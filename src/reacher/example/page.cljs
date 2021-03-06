
(ns reacher.example.page
  (:require [reacher.example.schema :as schema]
            [cljs.reader :refer [read-string]]
            [reacher.example.config :as config]
            [cumulo-util.build :refer [get-ip!]]
            ["react-dom/server" :refer [renderToString]]
            ["react" :as React]
            ["fs" :as fs]
            [reacher.core :refer [div html head body link style script title meta']])
  (:require-macros [clojure.core.strint :refer [<<]]))

(def base-info {:title (:title config/site), :icon (:icon config/site), :inline-html nil})

(defn make-page [info]
  (renderToString
   (html
    {}
    (head {} (title {} (:title info)) (meta' {:char-set "utf8"}))
    (body
     {}
     (div {:class-name "app"})
     (div {:class-name "meson-modal-container"})
     (->> (:scripts info) (map (fn [src] (script {:src src, :key src}))) (apply array))))))

(defn dev-page []
  (make-page
   (merge
    base-info
    {:styles [(<< "http://~(get-ip!):8100/main.css") "/entry/main.css"],
     :scripts ["/client.js"],
     :inline-styles []})))

(defn slurp [file-path] (fs/readFileSync file-path "utf8"))

(defn prod-page []
  (let [assets (read-string (slurp "dist/assets.edn"))
        cdn (if config/cdn? (:cdn-url config/site) "")
        prefix-cdn (fn [x] (str cdn x))]
    (make-page
     (merge
      base-info
      {:styles [(:release-ui config/site)],
       :scripts (map #(-> % :output-name prefix-cdn) assets),
       :inline-styles [(slurp "./entry/main.css")]}))))

(defn spit [file-path content] (fs/writeFileSync file-path content))

(defn main! []
  (println "Running mode:" (if config/dev? "dev" "release"))
  (if config/dev?
    (spit "target/index.html" (dev-page))
    (spit "dist/index.html" (prod-page))))

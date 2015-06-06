(set-env!
 :source-paths   #{"src/clj" "src/cljs"}
 :resource-paths #{"resources"}
 :dependencies '[[adzerk/boot-cljs      "0.0-2814-4"]
                 [adzerk/boot-cljs-repl "0.1.10-SNAPSHOT" :scope "test"]
                 [adzerk/boot-reload    "0.2.6"           :scope "test"]
                 [environ               "1.0.0"]
                 [danielsz/boot-environ "0.0.1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]

                 ;; server
                 [ring/ring-core        "1.3.2" ]
                 [ring/ring-devel       "1.3.2" ]
                 [ring/ring-defaults    "0.1.4"]
                 [org.danielsz/system   "0.1.8-SNAPSHOT"]
                 [liberator             "0.12.2"]
                 [http-kit              "2.1.16"]
                 [com.flyingmachine/liberator-unbound "0.1.1"]
                 [compojure             "1.3.3"]

                 ;; client
                 [org.omcljs/om "0.8.8" :exclusions [cljsjs/react]]
                 [om-sync "0.1.1"]
                 [cljs-ajax "0.3.11"]
                 [cljsjs/markdown "0.6.0-beta1-0"]
                 [cljsjs/react-with-addons "0.13.1-0"]
                 [boot-sassc "0.1.2"]])

(require
 '[boot.core             :as c]
 '[adzerk.boot-cljs      :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload    :refer [reload]]
 '[reloaded.repl         :refer [init start stop go reset]]
 '[magewars-deckbuilder.systems :refer [dev-system]]
 '[mathias.boot-sassc    :refer [sass]]
 '[system.boot           :refer [system]]
 '[magewars-deckbuilder.cards   :as cards]
 '[magewars-deckbuilder.mages   :as mages])

(deftask environ [e env FOO=BAR {kw edn} "The environment map"]
  (with-pre-wrap fileset
    (alter-var-root #'environ.core/env merge env)
    fileset))

(deftask dev
  "Run a restartable systemin the REPL"
  []
  (comp (environ :env {:http-port 3000})
        (watch :verbose true)
        (sass :sass-file "main.scss" :output-dir "stylesheets")
        (system :sys #'dev-system :hot-reload true :files ["handler.clj"])
        (reload)
        (cljs :compiler-options {:output-to "main.js"}
              :source-map)
        (repl :server true)))

(deftask build
  "Build for prod"
  []
  (comp (sass :sass-file "main.scss" :output-dir "stylesheets")
        (cljs :compiler-options {:output-to "main.js"}
              :optimizations :advanced)))

(deftask write-data
  "Write data to filesystem"
  []
  (let [dir (c/tmp-dir!)]
    (set-env! :target-path "src/cljs/magewars_deckbuilder/data")
    (with-pre-wrap fileset
      (let [data {:cards (cards/cards :core)
                  :mages mages/all}
            out (output-files fileset)]
        (spit (str (.getPath dir) "/all.cljs")
              (str "(ns magewars-deckbuilder.data.all)\n\n(def data "
                   data
                   ")"))
        (-> (rm fileset out)
            (c/add-resource dir)
            c/commit!)))))

(defproject org.vlacs/flare "0.1.0-SNAPSHOT"
  :description "TODO"
  :url "TODO"
  :license {:name "TODO: Choose a license"
            :url "http://choosealicense.com/"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-free "0.9.4766.11"]
                 [org.vlacs/hatch "0.2.0"]
                 [datomic-schematode "0.1.0-RC3"]
                 ;;;^{:voom {:repo "https://github.com/vlacs/helmsman"}}
                 [org.vlacs/helmsman "0.2.6"]
                 [liberator "0.10.0"]
                 [org.clojure/data.json "0.2.4"]
                 [http-kit "2.1.16"]
                 [com.taoensso/timbre "3.2.1"]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}})

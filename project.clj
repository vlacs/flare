(defproject org.vlacs/flare "0.1.0-SNAPSHOT"
  :description "TODO"
  :url "TODO"
  :license {:name "TODO: Choose a license"
            :url "http://choosealicense.com/"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [com.datomic/datomic-free "0.9.4766"]
                 [org.vlacs/hatch "0.1.2"]
                 [datomic-schematode "0.1.0-RC1"]
                 ^{:voom {:repo "https://github.com/vlacs/helmsman"}}
                 [org.vlacs/helmsman "0.2.4"]]
  :profiles {:dev {:plugins [[lein-voom "0.1.0-SNAPSHOT" :exclusions [org.clojure/clojure]]]
                   :dependencies [[org.clojure/tools.namespace "0.2.4"]
                                  ]
                   :source-paths ["dev"]}})

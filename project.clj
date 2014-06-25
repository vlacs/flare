(defproject org.vlacs/flare "0.1.0"
  :description "Flare is an event bus that uses Datomic to store events and
               data associated with those events so other applications can
               subscribe to events where Flare will asyncronously call the
               other applications API that they provide to Flare."
  :url "https://www.github.com/vlacs/flare"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.datomic/datomic-pro "0.9.4766.11"]
                 ^{:voom {:repo "https://github.com/vlacs/hatch"
                          :branch "development"}}
                 [org.vlacs/hatch "0.2.0-20140623_151558-gfe0fc67"]
                 [datomic-schematode "0.1.2"]
                 [org.vlacs/helmsman "0.2.6"]
                 [liberator "0.10.0"]
                 [org.clojure/data.json "0.2.4"]
                 [http-kit "2.1.16"]
                 [prismatic/schema "0.2.2"]
                 [com.taoensso/timbre "3.2.1"]]
  :repositories [["my.datomic.com" {:url "https://my.datomic.com/repo"
                                    :username :env/lein_datomic_repo_username
                                    :password :env/lein_datomic_repo_password}]]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]]
                   :source-paths ["dev"]}})

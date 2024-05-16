(defproject jrello "0.1.0"
  :description "Jrello helps tech savvy engineering managers analyse their trello boards"
  :url "https://github.com/aldiamond/jrello"
  :license {:name "EPL-2.0 WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"]
                 [org.clojure/data.json "2.4.0"]
                 [org.clojure/data.csv "1.1.0"]
                 [io.github.kit-clj/kit-core "1.0.6"]
                 [org.threeten/threeten-extra "1.7.2"]
                 [org.clojure/tools.cli "1.1.230"]]
  :main ^:skip-aot jrello.main
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})

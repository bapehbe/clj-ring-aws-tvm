(defproject clj-ring-aws-tvm "0.1.0-SNAPSHOT"
  :description "aws token vending machine ring handler"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.json "0.2.0"]
                 [com.amazonaws/aws-java-sdk "1.3.27"]]
  :profiles {:dev
             {:dependencies [[ring/ring-jetty-adapter "1.1.6"]
                             [ring/ring-devel "1.1.6"]
                             [clj-http "0.4.0"]]}})

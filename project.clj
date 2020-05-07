(defproject simple-server "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.10.0"]
                 ;; This is to run our basic HTTP server
                 [ring/ring-core "1.6.3"]
                 [ring/ring-jetty-adapter "1.6.3"]

                 ;; The libs we saw previously in training
                 [clj-time "0.15.2"]
                 [superstring "3.0.0"]
                 [funcool/cuerdas "RELEASE"]
                 [slingshot "0.12.2"]
                 [clj-http "3.10.1"]
                 [medley "1.3.0"]
                 [cheshire "5.9.0"]
                 [camel-snake-kebab "0.4.0"]

                 ;; Useful libraries for web development.
                 [compojure "1.6.1"]
                 [ring/ring-defaults "0.3.2"]
                 [hiccup "1.0.5"]

                 [ring/ring-mock "0.3.0"]
                 [ring/ring-devel "1.5.0"]]

  :repl-options {:init-ns simple-server.core})

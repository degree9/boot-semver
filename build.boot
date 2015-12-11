(set-env!
 :dependencies  '[[org.clojure/clojure "1.7.0"]
                  [boot/core           "2.4.2"]
                  [adzerk/bootlaces    "0.1.13"]
                  [hoplon/boot-hoplon  "0.1.10"]
                  [clojurewerkz/propertied "1.2.0"]
                  [grimradical/clj-semver "0.3.0"]]
 :resource-paths   #{"src"})

(require
 '[adzerk.bootlaces :refer :all]
 '[boot-semver.core :refer :all])


(auto-version! {:minor inc})

(task-options!
 pom {:project 'degree9/boot-semver
      :description ""
      :url         ""
      :scm {:url ""}})

(deftask dev
  "Build boot-semver for development"
  []
  (comp
   (watch)
   (version)
   (build-jar)))

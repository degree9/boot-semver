(set-env!
 :dependencies  '[[org.clojure/clojure     "1.7.0"]
                  [boot/core               "2.5.5"]
                  [adzerk/bootlaces        "0.1.13"]
                  [clojurewerkz/propertied "1.2.0"]
                  [grimradical/clj-semver  "0.3.0"]
                  [clj-time "0.11.0"]]
 :resource-paths   #{"src"})

(require
 '[adzerk.bootlaces :refer :all]
 '[boot-semver.core :refer :all])

(task-options!
 pom {:project 'degree9/boot-semver
      :description ""
      :url         ""
      :scm {:url ""}})

(deftask develop
  "Build boot-semver for development."
  []
  (comp
   (watch)
   (version :no-update true
            :minor 'inc
            :patch 'zero
            :pre-release 'snapshot)
   (build-jar)))

(deftask deploy
  "Build boot-semver and deploy to clojars."
  []
  (comp
   (version)
   (build-jar)
   (push-release)))

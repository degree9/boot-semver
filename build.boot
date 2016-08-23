(set-env!
 :dependencies  '[[org.clojure/clojure     "1.7.0"]
                  [boot/core               "2.6.0"]
                  [adzerk/bootlaces        "0.1.13" :scope "test"]
                  [clojurewerkz/propertied "1.2.0"]
                  [grimradical/clj-semver  "0.3.0"]
                  [clj-time                "0.11.0"]
                  [seancorfield/boot-new   "0.4.6"]]
 :resource-paths   #{"src"})

(require
 '[adzerk.bootlaces :refer :all]
 '[boot-semver.core :refer :all])

(task-options!
 pom {:project 'degree9/boot-semver
      :version (get-version)
      :description "Semantic versioning for boot projects."
      :url         "https://github.com/degree9/boot-semver"
      :scm {:url "https://github.com/degree9/boot-semver"}}
 target {:dir #{"target"}})

(deftask develop
  "Build boot-semver for development."
  []
  (comp
   (watch)
   (version :no-update true
            :minor 'inc
            :patch 'zero
            :pre-release 'snapshot
            :generate 'degree9.semver)
   (target)
   (build-jar)))

(deftask deploy
  "Build boot-semver and deploy to clojars."
  []
  (comp
   (version)
   (target)
   (build-jar)
   (push-release)))

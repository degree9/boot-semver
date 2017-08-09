(set-env!
 :dependencies  '[[org.clojure/clojure     "1.8.0"]
                  [grimradical/clj-semver  "0.3.0"]
                  [clj-time                "0.14.0"]]
 :resource-paths   #{"src"})

(require
 '[degree9.boot-semver :refer :all])

(task-options!
 pom {:project 'degree9/boot-semver
      :description "Semantic versioning for boot projects."
      :url         "https://github.com/degree9/boot-semver"
      :scm {:url "https://github.com/degree9/boot-semver"}}
 target {:dir #{"target"}})

(deftask develop
  "Build boot-semver for development."
  []
  (comp
   (version :develop true
            :minor 'inc
            :patch 'zero
            :pre-release 'snapshot)
   (watch)
   (target)
   (build-jar)
   (push-snapshot)))

(deftask deploy
  "Build boot-semver and deploy to clojars."
  []
  (comp
   (version)
   (target)
   (build-jar)
   (push-release)))

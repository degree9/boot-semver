(set-env!
 :dependencies  '[[org.clojure/clojure     "1.8.0"]
                  [adzerk/bootlaces        "0.1.13" :scope "test"]
                  [grimradical/clj-semver  "0.3.0"]
                  [clj-time                "0.13.0"]]
 :resource-paths   #{"src"})

(require
 '[adzerk.bootlaces :refer :all]
 '[degree9.boot-semver :refer :all])

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
   (version :develop true
            :minor 'inc
            :patch 'zero
            :pre-release 'snapshot
            :generate 'degree9.boot-semver.version)
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

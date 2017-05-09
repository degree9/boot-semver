(ns degree9.boot-semver
  (:require [boot.core          :as boot]
            [boot.git           :as git]
            [boot.task.built-in :as task]
            [boot.util          :as util]
            [clj-time.core      :as timec]
            [clj-time.format    :as timef]
            [degree9.boot-semver.impl :as impl]))

;; Boot SemVer Utils ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn alpha [& _] "alpha")

(defn beta [& _] "beta")

(defn snapshot [& _] "SNAPSHOT")

(defn zero  [& _] "0")

(defn one   [& _] "1")

(defn two   [& _] "2")

(defn three [& _] "3")

(defn four  [& _] "4")

(defn five  [& _] "5")

(defn six   [& _] "6")

(defn seven [& _] "7")

(defn eight [& _] "8")

(defn nine  [& _] "9")

(defn semver-date [& _] (timef/unparse (timef/formatter "yyyyMMdd") (timec/now)))

(defn semver-time [& _] (timef/unparse (timef/formatter "hhmmss") (timec/now)))

(defn semver-date-time [& [_ delim]] (clojure.string/join [(semver-date) (or delim ".") (semver-time)]))

(defn git-sha1-full [& _]
  (str (git/last-commit)))

(defn git-sha1 [& _]
  (subs (git-sha1-full) 0 7))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(boot/deftask version
  "Semantic Versioning for your project.

  You can also :refer or :use the following helper symbols in your build.boot:

  ;; Generic
  'zero 'one 'two ... 'nine

  ;; Pre-Release version helpers
  'alpha 'beta 'snapshot

  ;; Build version helpers
  'semver-date          ;; \"yyyyMMdd\"
  'semver-time          ;; \"hhmmss\"
  'semver-date-time     ;; \"yyyyMMdd-hhmmss\"
  'semver-date-dot-time ;; \"yyyyMMdd.hhmmss\"
  'git-sha1-full        ;; full git commit string
  'git-sha1             ;; short git commit string (7 chars)

  And then use them at the command line:

  boot version -r git-sha1
  Version in version.properties ...: 0.1.3
  Current Build Version ...: 0.1.3-1d7cfab"
  [x major       MAJ  sym  "Symbol of fn to apply to Major version."
   y minor       MIN  sym  "Symbol of fn to apply to Minor version."
   z patch       PAT  sym  "Symbol of fn to apply to Patch version."
   r pre-release PRE  sym  "Symbol of fn to apply to Pre-Release version."
   b build       BLD  sym  "Symbol of fn to apply to Build version."
   d develop          bool "Prevents writing to version.properties file."
   i include          bool "Includes version.properties file in out-files."
   g generate    GEN  sym  "Generate a namespace with version information."
   ]
  (let [fver    (impl/get-version impl/semver-file)
        include (:include *opts*)
        gen-ns  (:generate *opts*)]
    (util/info "Initial Project Version...: %s\n" fver)
    (reset! impl/+version+ fver)
    (cond-> (impl/version-impl fver *opts*)
      include (impl/version-file-fn)
      gen-ns  (impl/version-ns-fn gen-ns))))

(boot/deftask build-jar
  "Build and Install project jar with version information."
  []
  (comp (task/pom) (task/jar) (task/install)))

(boot/deftask push-snapshot
  "Deploy snapshot version to Clojars."
  [f file PATH str "The jar file to deploy."]
  (comp
    (impl/version-clojars)
    (task/push
      :file            file
      :ensure-snapshot true
      :repo            "version-clojars")))

(boot/deftask push-release
  "Deploy release version to Clojars."
  [f file PATH str "The jar file to deploy."]
  (comp
   (impl/version-clojars)
   (task/target)
   (task/push
     :file           file
     :tag            (boolean (git/last-commit))
     :ensure-release true
     :repo           "version-clojars")
     ))

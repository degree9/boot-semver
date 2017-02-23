(ns degree9.boot-semver
  (:require [boot.core          :as boot]
            [boot.task.built-in :as task]
            [boot.util          :as util]
            [boot.git           :as git]
            [clojure.java.io    :as io]
            [clj-semver.core    :as ver]
            [clj-time.core      :as timec]
            [clj-time.format    :as timef]))

(def semver-file "./version.properties")

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

(defn- str->num [str]
  (if (re-matches #"\d" str)
    (bigdec str) str))

(defn- update-version [vermap upmap]
  (let [res #(-> % symbol resolve)]
    (merge-with
      (fn [uv vv]
        (if (res uv)
          ((res uv) (if (string? vv)
                      (-> vv (clojure.string/replace #"[-+]" "") str->num)
                      (or vv 0)))
          (util/exit-error (util/fail "Unable to resolve symbol: %s \n" uv)))) upmap vermap)))

(defn git-sha1-full [& _]
  (str (git/last-commit)))

(defn git-sha1 [& _]
  (subs (git-sha1-full) 0 7))

(defn get-version
  ([] (get-version semver-file))
  ([file] (get-version semver-file "0.0.0"))
  ([file version] (if (.exists (io/as-file file))
                    (get (doto (java.util.Properties.)
                           (.load ^java.io.Reader (io/reader file)))
                         "VERSION"
                         version)
                    version)))

(defn set-version! [file version]
  (let [version (or version "0.1.0")]
    (doto (java.util.Properties.)
      (.setProperty "VERSION" version)
      (.store ^java.io.Writer (io/writer file) nil))))

(defn to-mavver [{:keys [major minor patch pre-release build]}]
  (clojure.string/join (cond-> []
                               major (into [major])
                               minor (into ["." minor])
                               patch (into ["." patch])
                               pre-release (into ["-" pre-release])
                               build (into ["+" build]))))

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
   d develop     bool      "Prevents writing to version.properties file."
   i include          bool "Includes version.properties file in out-files."
   g generate    GEN  sym  "Generate a namespace with version information."
   ]
  (let [curver       (get-version semver-file)
        version-orig (to-mavver (update-version (ver/version curver) *opts*))]
    (boot/task-options! task/pom  #(assoc-in % [:version] version-orig)
                        task/push #(assoc-in % [:ensure-version] version-orig))
    (boot/with-pre-wrap fs
        ;; Rebuild Version incase it was modified by another task or by
        ;; functions passed to the task which rely on a runtime value
        ;; (pom and push will be out of sync)
      (let [version (to-mavver (update-version (ver/version curver) *opts*))
            include (:include *opts*)
            gen-ns  (:generate *opts*)
            develop (:develop *opts*)
            tmp     (boot/tmp-dir!)
            file    (str (clojure.string/join "/" (clojure.string/split (clojure.string/replace (str gen-ns) #"-" "_") #"\.")) ".clj")
            ns-file (io/file tmp file)]
        (when (not= version-orig version)
          (util/dbug "Original Version ...: %s\n" version-orig)
          (util/warn "Version has been unexpectedly modified during task pipeline. \n"))
        (util/info "Version in version.properties ...: %s\n" curver)
        (util/info "Current Build Version ...: %s\n" version)
        (when (and (not (:develop *opts*)) (not= curver version))
          (util/info "Updating Project Version...: %s -> %s\n" curver version)
          (set-version! semver-file version))
        (when gen-ns
          (util/info "Generating Version Namespace...: %s\n" gen-ns)
          (util/dbug "Generating Namespace File...: %s\n" ns-file)
          (doto ns-file io/make-parents
            (spit (str "(ns " gen-ns ")\n" "(def version \"" version "\")\n"))))
        (prn (-> semver-file io/file .getParent io/file .getAbsolutePath))
        (cond-> fs
          include (-> (boot/add-resource (-> semver-file io/file .getParent io/file)
                        :include #{#"version.properties"}
                        :exclude #{#"\.*\version.properties"})
                      boot/commit!)
          gen-ns  (-> (boot/add-resource tmp)
                      boot/commit!))))))

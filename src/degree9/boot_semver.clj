(ns degree9.boot-semver
  (:require [boot.core          :as boot]
            [boot.task.built-in :as task]
            [boot.util          :as util]
            [boot.git           :as git]
            [boot.new           :as new]
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

(defn semver-git [& _]
  (str (git/last-commit)))

(defn semver-short-git [& _]
  (subs (semver-git) 0 7))

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
  'semver-git           ;; full git commit string
  'semver-short-git     ;; short git commit string (7 chars)

  And then use them at the command line:

  boot version -r semver-short-git
  Version in version.properties ...: 0.1.3
  Current Build Version ...: 0.1.3-1d7cfab"
  [x major       MAJ  sym  "Symbol of fn to apply to Major version."
   y minor       MIN  sym  "Symbol of fn to apply to Minor version."
   z patch       PAT  sym  "Symbol of fn to apply to Patch version."
   r pre-release PRE  sym  "Symbol of fn to apply to Pre-Release version."
   b build       BLD  sym  "Symbol of fn to apply to Build version."
   n no-update        bool "Prevents writing to version.properties file."
   i include          bool "Includes version.properties file in out-files."
   g generate    GEN  sym  "Generate a namespace with version information."
   ]
  (let [curver  (get-version semver-file)
        cursemver (ver/version curver)
        version (to-mavver (update-version (ver/version curver) *opts*))
        gen-ns (:generate *opts*)]
    (boot/task-options! task/pom  #(assoc-in % [:version] version)
                        task/push #(assoc-in % [:ensure-version] version))
    (cond->
      (boot/with-pre-wrap fs
        (util/info (clojure.string/join ["Version in version.properties ...: " curver "\n"]))
        (util/info (clojure.string/join ["Current Build Version ...: " version "\n"]))
        (when (and (nil? (:no-update *opts*)) (not= curver version))
          (util/info (clojure.string/join ["Updating Project Version...: " curver "->" version "\n"]))
          (set-version! semver-file version))
        (if (:include *opts*)
          (-> fs (boot/add-resource
                   (-> semver-file io/file .getParent io/file)
                   :include #{#"version.properties"})
                 boot/commit!)
          fs))
      gen-ns (comp (new/new :generate [(str "semver=" gen-ns "/VERSION" )] :args [(str (get-version))])))))

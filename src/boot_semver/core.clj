(ns boot-semver.core
  (:require [boot.core          :as boot]
            [boot.task.built-in :as task]
            [boot.util          :as util]
            [boot.git           :as git]
            [clojure.java.io    :as io]
            [clj-semver.core    :as ver]
            [clojurewerkz.propertied.properties :as prop]
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

;;TODO add pre-release inc/dec functions

(defn semver-date [& _] (timef/unparse (timef/formatter "yyyyMMdd") (timec/now)))

(defn semver-time [& _] (timef/unparse (timef/formatter "hhmmss") (timec/now)))

(defn semver-date-time [& [_ delim]] (clojure.string/join [(semver-date) (or delim "-") (semver-time)]))

(defn semver-date-dot-time [& _] (semver-date-time nil "."))

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
  (subs (semver-git) 0 5))

(defn get-version
  ([] (get-version semver-file))
  ([file] (get-version semver-file "0.0.0"))
  ([file version] (if (.exists (io/as-file file))
                    (or (:VERSION (prop/properties->map (prop/load-from (io/file file)) true)) version)
                    version)))

(defn set-version! [file version]
  (let [version (or version "0.1.0")]
    (prop/store-to {"VERSION" version} (io/file file))))

(defn to-mavver [{:keys [major minor patch pre-release build]}]
  (clojure.string/join (cond-> []
                               major (into [major])
                               minor (into ["." minor])
                               patch (into ["." patch])
                               pre-release (into ["-" pre-release])
                               build (into ["+" build]))))

(boot/deftask version
  "Semantic Versioning for your project."
  [x major       MAJ  sym  "Symbol of fn to apply to Major version."
   y minor       MIN  sym  "Symbol of fn to apply to Minor version."
   z patch       PAT  sym  "Symbol of fn to apply to Patch version."
   r pre-release PRE  sym  "Symbol of fn to apply to Pre-Release version."
   b build       BLD  sym  "Symbol of fn to apply to Build version."
   n no-update        bool "Prevents writing to version.properties file."
   i include          bool "Includes version.properties file in out-files."
   ;g generate   GEN  sym  "Generate a namespace with version information."
   ]
  (let [curver  (get-version semver-file)
        cursemver (ver/version curver)
        version (to-mavver (update-version (ver/version curver) *opts*))]
    (boot/task-options! task/pom  #(assoc-in % [:version] version)
                        task/push #(assoc-in % [:ensure-version] version))
    (boot/with-pre-wrap [fs]
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
        fs))))

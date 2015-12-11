(ns boot-semver.core
  (:require [boot.core          :as boot]
            [boot.task.built-in :as task]
            [boot.util          :as util]
            [hoplon.boot-hoplon :as hoplon]
            [adzerk.bootlaces   :as bootlaces]
            [clojure.java.io    :as io]
            [clj-semver.core    :as ver]
            [clojurewerkz.propertied.properties :as prop]))

(defn snapshot [x] "SNAPSHOT")

(defn alpha [x] "alpha")

(defn beta [x] "beta")

(def semver-file "./version.properties")

(defn- update-version [vermap upmap]
  (merge-with (fn [uv vv] ((resolve uv) vv)) upmap vermap))

(defn get-semver
  ([] (get-semver semver-file))
  ([file] (get-semver semver-file "0.0.0"))
  ([file version] (if (.exists (io/as-file file))
                    (or (:VERSION (prop/properties->map (prop/load-from (io/file file)) true)) version)
                    version)))

(defn set-semver [file version]
  (let [version (or version "0.1.0")]
    (prop/store-to {"VERSION" version} (io/file file))))

(defn to-mavver [{:keys [major minor patch pre-release build]}]
  (clojure.string/join
   (cond-> []
           major (into [major])
           minor (into ["." minor])
           patch (into ["." patch])
           pre-release (into ["-" pre-release])
           build (into ["-" build]))))

(boot/deftask version
  ""
  [f file        FILE str "Version target file."
   x major       MAJ  sym "Symbol of fn to apply to Major version."
   y minor       MIN  sym "Symbol of fn to apply to Minor version."
   z patch       PAT  sym "Symbol of fn to apply to Patch version."
   r pre-release PRE  sym "Symbol of fn to apply to Pre-Release version."
   b build       BLD  sym "Symbol of fn to apply to Build version."]
  (let [curver (get-semver (or (:file *opts*) semver-file))
        version (to-mavver (update-version (ver/version curver) *opts*))]
    (boot/task-options!
     task/pom #(assoc-in % [:version] version))
    (boot/with-pre-wrap [fs]
      (if (not= curver version)
        (util/info (clojure.string/join ["Updating Project Version...: " curver "->" version "\n"]))
        (util/info (clojure.string/join ["Setting Project Version...: " version "\n"])))
      (set-semver semver-file version)
      fs)))

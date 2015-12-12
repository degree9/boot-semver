(ns boot-semver.core
  (:require [boot.core          :as boot]
            [boot.task.built-in :as task]
            [boot.util          :as util]
            [hoplon.boot-hoplon :as hoplon]
            [adzerk.bootlaces   :as bootlaces]
            [clojure.java.io    :as io]
            [clj-semver.core    :as ver]
            [clojurewerkz.propertied.properties :as prop]))

(defn alpha [x] "alpha")

(defn beta [x] "beta")

(defn snapshot [x] "SNAPSHOT")

(def semver-file "./version.properties")

(defn- update-version [vermap upmap]
  (merge-with (fn [uv vv] ((resolve uv) vv)) upmap vermap))

(defn get-version
  ([] (get-version semver-file))
  ([file] (get-version semver-file "0.0.0"))
  ([file version] (if (.exists (io/as-file file))
                    (or (:VERSION (prop/properties->map (prop/load-from (io/file file)) true)) version)
                    version)))

(defn set-version [file version]
  (let [version (or version "0.1.0")]
    (prop/store-to {"VERSION" version} (io/file file))))

(defn to-mavver [{:keys [major minor patch pre-release build]}]
  (clojure.string/join (cond-> []
                               major (into [major])
                               minor (into ["." minor])
                               patch (into ["." patch])
                               pre-release (into ["-" pre-release])
                               build (into ["-" build]))))

(boot/deftask version
  "Semantic Versioning for your project."
  [x major       MAJ  sym "Symbol of fn to apply to Major version."
   y minor       MIN  sym "Symbol of fn to apply to Minor version."
   z patch       PAT  sym "Symbol of fn to apply to Patch version."
   r pre-release PRE  sym "Symbol of fn to apply to Pre-Release version."
   b build       BLD  sym "Symbol of fn to apply to Build version."]
  (let [curver  (get-version semver-file)
        version (to-mavver (update-version (ver/version curver) *opts*))]
    (boot/task-options! task/pom  #(assoc-in % [:version] version)
                        task/push #(assoc-in % [:ensure-version] version))
    (boot/with-pre-wrap [fs]
      (util/info (clojure.string/join ["Current Project Version...: " curver "\n"]))
      (when (not= curver version)
        (let [] (util/info (clojure.string/join ["Updating Project Version...: " curver "->" version "\n"]))
          (set-version! semver-file version)))
      fs)))

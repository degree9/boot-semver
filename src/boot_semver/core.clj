(ns boot-semver.core
  (:require [boot.core          :as boot]
            [boot.task.built-in :as task]
            [boot.util          :as util]
            [hoplon.boot-hoplon :as hoplon]
            [adzerk.bootlaces   :as bootlaces]
            [clojure.java.io    :as io]
            [clj-semver.core    :as ver]
            [clojurewerkz.propertied.properties :as prop]))

(def semver-file "./version.properties")

(defn- update-version [upmap [k v]]
  (let [uv (k upmap)]
    (cond (and (string? uv) (ver/valid-format? uv)) [k uv]
          (integer? uv) [k (str uv)]
          (fn? uv) [k (uv v)]
          :else [k v])))

(defn get-semver
  ([] (get-semver semver-file))
  ([file] (get-semver semver-file "0.1.0"))
  ([file version] (if (.exists (io/as-file file))
                    (or (:VERSION (prop/properties->map (prop/load-from (io/file file)) true)) version)
                    version)))

(defn set-semver [io-file version]
  (let [version (or version "0.1.0")]
    (prop/store-to {"VERSION" version} io-file)))

(defn to-mavver [{:keys [major minor patch pre-release]}]
  (let [joinmap (cond-> []
                        major (into [major])
                        minor (into ["." minor])
                        patch (into ["." patch])
                        pre-release (into ["-" pre-release]))]
    (clojure.string/join joinmap)))

(boot/deftask version
  ""
  [f file        FILE str "version.properties target file."
   v ver         VER  str "New version to be set."
   x major       MAJ  str "Major version number (X = X.y.z)"
   y minor       MIN  str "Minor version number (Y = x.Y.z)"
   z patch       PAT  str "Patch version number (Z = x.y.Z)"
   p pre-release PRE  str "Major version number (P = x.y.z-P)"]
  (boot/with-pre-wrap [fs]
    (let [upmap (cond-> []
                        (:major *opts*) (into [(:major *opts*)])
                        (:minor *opts*) (into ["." (:minor *opts*)])
                        (:patch *opts*) (into ["." (:patch *opts*)])
                        (:pre-release *opts*) (into ["-" (:pre-release *opts*)]))
          oldver (:ver *opts*)
          newver (to-mavver (into {} (map (partial update-version upmap) (ver/version oldver))))]
      (if (not= oldver newver)
        (util/info (clojure.string/join ["Updating Project Version...: " oldver "->" newver "\n"]))
        (util/info (clojure.string/join ["Setting Project Version...: " newver "\n"])))
      (set-semver (io/file (:file *opts*)) newver))
    fs))

(defn auto-version! [& [verfile]]
  (let [verfile (or verfile semver-file)
        version (-> (get-semver verfile) ver/version to-mavver)]
    (boot/task-options!
     version #(assoc-in % [:ver] version)
     version #(assoc-in % [:file] (.getPath (io/as-file verfile)))
     task/pom #(assoc-in % [:version] version))))

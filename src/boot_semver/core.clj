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

(defn- update-version [vermap upmap]
  (merge-with (fn [uv vv] ((resolve uv) vv)) upmap vermap))

(defn get-semver
  ([] (get-semver semver-file))
  ([file] (get-semver semver-file "0.0.0"))
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
   v ver         VER  str "Version to be updated."
   x major       MAJ  sym "Symbol of fn to apply to Major."
   y minor       MIN  sym "Symbol of fn to apply to Minor."
   z patch       PAT  sym "Symbol of fn to apply to Patch."
   p pre-release PRE  sym "Symbol of fn to apply to Pre-Release."]
  (boot/with-pre-wrap [fs]
    (let [upmap (cond-> {}
                        (:major *opts*) (assoc-in [:major] (:major *opts*))
                        (:minor *opts*) (assoc-in [:minor] (:minor *opts*))
                        (:patch *opts*) (assoc-in [:patch] (:patch *opts*))
                        (:pre-release *opts*) (assoc-in [:pre-release] (:pre-release *opts*)))
          oldver (:ver *opts*)
          newver (to-mavver (update-version (ver/version oldver) upmap))]
      (if (not= oldver newver)
        (util/info (clojure.string/join ["Updating Project Version...: " oldver "->" newver "\n"]))
        (util/info (clojure.string/join ["Setting Project Version...: " newver "\n"])))
      (set-semver (io/file (:file *opts*)) newver))
    fs))

(defn version! [version & [verfile]]
  (let [verfile (or verfile semver-file)
        version  (get-semver verfile)]
    (boot/task-options!
     version {:ver version :file verfile}
     task/pom #(assoc-in % [:version] version))))

(defn auto-version! [& [{:as upmap :or {}} [verfile]]]
  (let [verfile (or verfile semver-file)
        oldver  (get-semver verfile)
        version (to-mavver (update-version (ver/version oldver) upmap))]
    (boot/task-options!
     version (merge upmap {:ver oldver :file verfile})
     task/pom #(assoc-in % [:version] version))))

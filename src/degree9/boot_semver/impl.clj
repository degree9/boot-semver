(ns degree9.boot-semver.impl
  (:require [boot.core          :as boot]
            [boot.git           :as git]
            [boot.gpg           :as gpg]
            [boot.pod           :as pod]
            [boot.util          :as util]
            [clojure.java.io    :as io]
            [clj-semver.core    :as ver]
            ))
;; Boot SemVer Helpers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- str->num [str]
  (if (re-matches #"\d" str)
    (bigdec str) str))

(defn- apply-version [vermap upmap]
  (let [res #(-> % symbol resolve)]
    (merge-with
      (fn [uv vv]
        (if (res uv)
          ((res uv) (if (string? vv)
                      (-> vv (clojure.string/replace #"[-+]" "") str->num)
                      (or vv 0)))
          (util/exit-error (util/fail "Unable to resolve symbol: %s \n" uv)))) upmap vermap)))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Version.properties Public API ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def semver-file "./version.properties")

(def +version+ (atom nil))

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
  (let [version (or version (get-version file))]
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

(defn update-version [ver opts]
  (-> ver ver/version (apply-version opts) to-mavver))

(defn version= [ver]
  (if (= @+version+ ver) ver @+version+))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Boot SemVer Task Impl ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn version-ns-fn [version namespace]
  (let [gen-ns  (or namespace 'app.version)
        tmp     (boot/tmp-dir!)]
    (util/info "Generating Version Namespace...: %s\n" gen-ns)
    (boot/with-pre-wrap fs
      (let [curver  (if version version @+version+)
            path    (str (clojure.string/join "/"
                           (clojure.string/split
                             (clojure.string/replace (str gen-ns) #"-" "_") #"\.")) ".clj")
            file    (io/file tmp path)
            spit-ns #(spit (str "(ns " %2 ")\n" "(def version \"" %3 "\")\n") %1)]
        (util/dbug "Generating Namespace File...: %s\n" file)
        (doto file io/make-parents (spit-ns gen-ns curver))
        (-> fs (boot/add-resource tmp) boot/commit!)))))


(boot/deftask version-ns
  "Generate a namespace containing the project version."
  [v version     VER  str  "Version string to be stored in namespace."
   n namespace   GEN  sym  "Generate a namespace with version information. (app.version)"]
  (version-ns-fn (:version *opts*) (:namespace *opts*)))


(defn version-file-fn [& _]
  (let [projdir (-> semver-file io/file .getParent io/file)]
    (boot/with-pre-wrap fs
      (if (.exists (io/file projdir "version.properties"))
        (util/info "Adding version.properties to fileset... %s\n")
        (util/warn "Could not find version.properties... %s\n"))
      (-> fs
          (boot/add-resource projdir :include #{#"^version.properties$"})
          boot/commit!))))

(boot/deftask version-file
  "Includes the version.properties file in the fileset."
  []
  (version-file-fn))

(defn version-impl [fver opts]
  (boot/with-pass-thru _
    (let [develop (:develop opts)
          curver  (version= fver)
          version (update-version curver opts)]
      (util/info "Current Build Version...: %s\n" version)
      (reset! +version+ version)
      (when (and (not develop) (not= fver version))
        (util/info "Updating Project Version...: %s -> %s\n" fver version)
        (set-version! semver-file version)))))

(boot/deftask version-pom
  "Create a versioned project pom.xml file."
  [p project      SYM        sym         "The project id (eg. foo/bar)."
   v version      VER        str         "The project version."
   d description  DESC       str         "The project description."
   c classifier   STR        str         "The project classifier."
   P packaging    STR        str         "The project packaging type, i.e. war, pom"
   u url          URL        str         "The project homepage url."
   s scm          KEY=VAL    {kw str}    "The project scm map (KEY is one of url, tag, connection, developerConnection)."
   l license      NAME:URL   {str str}   "The map {name url} of project licenses."
   o developers   NAME:EMAIL {str str}   "The map {name email} of project developers."
   D dependencies SYM:VER    [[sym str]] "The project dependencies vector (overrides boot env dependencies)."]
  (boot/with-pre-wrap fs
    (let [tgt       (boot/tmp-dir!)
          tag       (:tag scm (util/guard (git/last-commit)))
          scm       (when scm (assoc scm :tag tag))
          deps      (:dependencies (boot/get-env) dependencies)
          version   (:version *opts* @+version+)
          opts      (assoc *opts*
                        :version version
                        :scm scm
                        :dependencies deps
                        :developers developers
                        :classifier classifier
                        :packaging (or packaging "jar"))
          [gid aid] (util/extract-ids project)
          pomdir    (io/file tgt "META-INF" "maven" gid aid)
          xmlfile   (io/file pomdir "pom.xml")
          propfile  (io/file pomdir "pom.properties")]
        (when-not (and project version)
          (throw (Exception. "Project and Version are needed to create pom.xml")))
        (util/info "Writing %s and %s...\n" (.getName xmlfile) (.getName propfile))
        (pod/with-call-worker
          (boot.pom/spit-pom! ~(.getPath xmlfile) ~(.getPath propfile) ~opts))
        (-> fs (boot/add-resource tgt) boot/commit!))))

(boot/replace-task! [p boot.task.built-in/pom] (fn [& args] (apply version-pom args)))

(boot/deftask version-clojars
  "Collect CLOJARS_USER and CLOJARS_PASS from the user if they're not set."
  []
  (let [[user pass]   (mapv #(System/getenv %) ["CLOJARS_USER" "CLOJARS_PASS"])
        clojars-creds (atom {})
        set-creds!    (partial swap! clojars-creds assoc)
        env-repos     (boot/get-env :repositories)
        clojars-repo  (get (into {} env-repos) "clojars")]
    (set-creds! :username user :password pass)
    (boot/with-pass-thru _
      (when-not (and (:username @clojars-creds user) (:password @clojars-creds pass))
        (util/warn "%s and %s were not set; please enter your Clojars credentials.\n" "CLOJARS_USER" "CLOJARS_PASS")
        (print "Username: ")
        (#(set-creds! :username %) (read-line))
        (print "Password: ")
        (#(set-creds! :password %)
          (apply str (.readPassword (System/console))))
        (boot/set-env! :repositories (conj env-repos ["version-clojars" (merge clojars-repo @clojars-creds)]))
        ))))

(boot/deftask version-push
  "Deploy jar file to a Maven repository.
  If the file option is not specified the task will look for jar files
  created by the build pipeline. The jar file(s) must contain pom.xml
  entries.
  The repo option is required. The repo option is used to get repository
  map from Boot envinronment. Additional repo-map option can be used to
  add options, like credentials, or to provide complete repo-map if Boot
  envinronment doesn't hold the named repository."

  [f file PATH            str      "The jar file to deploy."
   P pom PATH             str      "The pom.xml file to use (see install task)."
   F file-regex MATCH     #{regex} "The set of regexes of paths to deploy."
   g gpg-sign             bool     "Sign jar using GPG private key."
   k gpg-user-id KEY      str      "The name or key-id used to select the signing key."
   ^{:deprecated "Check GPG help about changing GNUPGHOME."}
   K gpg-keyring PATH     str      "The path to secring.gpg file to use for signing."
   p gpg-passphrase PASS  str      "The passphrase to unlock GPG signing key."
   r repo NAME            str      "The name of the deploy repository."
   e repo-map REPO        edn      "The repository map of the deploy repository."
   t tag                  bool     "Create git tag for this version."
   B ensure-branch BRANCH str      "The required current git branch."
   C ensure-clean         bool     "Ensure that the project git repo is clean."
   R ensure-release       bool     "Ensure that the current version is not a snapshot."
   S ensure-snapshot      bool     "Ensure that the current version is a snapshot."
   T ensure-tag TAG       str      "The SHA1 of the commit the pom's scm tag must contain."
   V ensure-version VER   str      "The version the jar's pom must contain."]

  (let [tgt (boot/tmp-dir!)]
    (boot/with-pass-thru [fs]
      (boot/empty-dir! tgt)
      (let [jarfiles (or (and file [(io/file file)])
                         (->> (boot/output-files fs)
                              (boot/by-ext [".jar"])
                              ((if (seq file-regex) #(boot/by-re file-regex %) identity))
                              (map boot/tmp-file)))
            ; Get options from Boot env by repo name
            r        (get (->> (boot/get-env :repositories) (into {})) repo)
            repo-map (merge r (when repo-map ((boot/configure-repositories!) repo-map)))]
        (when-not (and repo-map (seq jarfiles))
          (throw (Exception. "missing jar file or repo not found")))
        (doseq [f jarfiles]
          (let [{{t :tag} :scm
                 v :version} (pod/pom-xml-map f pom)
                b            (util/guard (git/branch-current))
                commit       (util/guard (git/last-commit))
                tags         (util/guard (git/ls-tags))
                clean?       (util/guard (git/clean?))
                snapshot?    (.endsWith v "-SNAPSHOT")
                artifact-map (when gpg-sign
                               (util/info "Signing %s...\n" (.getName f))
                               (gpg/sign-jar tgt f pom {:gpg-key gpg-user-id
                                                        :gpg-passphrase gpg-passphrase}))]
            (assert (or (not ensure-branch) (= b ensure-branch))
                    (format "current git branch is %s but must be %s" b ensure-branch))
            (assert (or (not ensure-clean) clean?)
                    "project repo is not clean")
            (assert (or (not ensure-release) (not snapshot?))
                    (format "not a release version (%s)" v))
            (assert (or (not ensure-snapshot) snapshot?)
                    (format "not a snapshot version (%s)" v))
            (assert (or (not ensure-tag) (not t) (= t ensure-tag))
                    (format "scm tag in pom doesn't match (%s, %s)" t ensure-tag))
            (when (and ensure-tag (not t))
              (util/warn "The --ensure-tag option was specified but scm info is missing from pom.xml\n"))
            (assert (or (= v @+version+) (= v ensure-version))
                    (format "jar version doesn't match project version (%s, %s)" v ensure-version))
            (util/info "Deploying %s...\n" (.getName f))
            ;(pod/with-call-worker
            ;  (boot.aether/deploy
            ;    ~(boot/get-env) ~[repo repo-map] ~(.getPath f) ~pom ~artifact-map))
            (pod/with-call-in
              (pod/make-pod {:dependencies (boot/template [[boot/worker ~boot/*boot-version*]])})
              (boot.aether/deploy
                ~(boot/get-env) ~[repo repo-map] ~(.getPath f) ~pom ~artifact-map))
            (when tag
              (if (and tags (= commit (get tags tag)))
                (util/info "Tag %s already created for %s\n" tag commit)
                (do (util/info "Creating tag %s...\n" v)
                    (git/tag v "release"))))))))))

(boot/replace-task! [p boot.task.built-in/push] (fn [& args] (apply version-push args)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

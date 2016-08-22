# boot-semver

[](dependency)
```clojure
[degree9/boot-semver "1.3.1"] ;; latest release
```
[](/dependency)

Semantic Versioning task for [boot-clj][1].

* Provides `version` task
* Parses a `version.properties` file to read the current project version. Example content:
  `VERSION=0.1.1-SNAPSHOT`
* Writes a new (if changed) [Maven compatible][2] version string to `version.properties`.

> The following outlines basic usage of the task, extensive testing has not been done.
> Please submit issues and pull requests!

## Usage

Add `boot-semver` to your `build.boot` dependencies and `require` the namespace:

```clj
(set-env! :dependencies '[[degree9/boot-semver "X.Y.Z" :scope "test"]])
(require '[boot-semver.core :refer :all])
```

Get the current project version:

```bash
boot version
```

Use the `version` task when pushing to clojars:

```bash
boot version -y inc build-jar push-release
```

Or use the `version` task in a deployment process:

```clojure
(deftask deploy
  "Build boot-semver and deploy to clojars."
  []
  (comp
   (version :minor 'inc)
   (build-jar)
   (push-release)))
```

##Task Options

The `version` task exposes a bunch of options for modifying the project version during a build pipeline.
Each option takes a quoted symbol (ex. `'inc`) which should resolve to an available function. This function will be applied to the current value of the project version component.

```clojure
x major       MAJ  sym  "Symbol of fn to apply to Major version."
y minor       MIN  sym  "Symbol of fn to apply to Minor version."
z patch       PAT  sym  "Symbol of fn to apply to Patch version."
r pre-release PRE  sym  "Symbol of fn to apply to Pre-Release version."
b build       BLD  sym  "Symbol of fn to apply to Build version."
n no-update        bool "Prevents writing to version.properties file."
```

The `:no-update` option is provided for development tasks. These tasks will modify the project version number however, this version number will not be written back to the filesystem.

```clojure
(deftask dev
  "Build boot-semver for development."
  []
  (comp
   (watch)
   (version :no-update true
            :minor 'inc
            :pre-release 'snapshot)
   (build-jar)))
```

##Helpers

A few helper functions are provided to be used with the version task.

```clojure
;; Pre-Release version helpers
'alpha    ;; "alpha"
'beta     ;; "beta"
'snapshot ;; "SNAPSHOT"

;; Build version helpers
'semver-date          ;; "yyyyMMdd"
'semver-time          ;; "hhmmss"
'semver-date-time     ;; "yyyyMMdd-hhmmss"
'semver-date-dot-time ;; "yyyyMMdd.hhmmss"
```

[1]: https://github.com/boot-clj/boot
[2]: https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm

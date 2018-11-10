# boot-semver

[![Clojars Project](https://img.shields.io/clojars/v/degree9/boot-semver.svg)](https://clojars.org/degree9/boot-semver)
[![Dependencies Status](https://versions.deps.co/degree9/boot-semver/status.svg)](https://versions.deps.co/degree9/boot-semver)
[![Downloads](https://versions.deps.co/degree9/boot-semver/downloads.svg)](https://versions.deps.co/degree9/boot-semver)
<!--- [![CircleCI](https://circleci.com/gh/degree9/boot-semver.svg?style=svg)](https://circleci.com/gh/degree9/boot-semver)
[![gitcheese.com](https://api.gitcheese.com/v1/projects/83cde58b-907d-4cd9-ba61-405b78f7b8f4/badges?type=1&size=xs)](https://www.gitcheese.com/app/#/projects/83cde58b-907d-4cd9-ba61-405b78f7b8f4/pledges/create) --->

Semantic Versioning task for [boot-clj][1].

---

<p align="center">
  <a href="https://degree9.io" align="center">
    <img width="135" src="http://degree9.io/images/degree9.png">
  </a>
  <br>
  <b>boot-semver is developed and maintained by Degree9</b>
</p>

---

* Provides `version` task
* Parses a `version.properties` file to read the current project version. Example content:
  `VERSION=0.1.1-SNAPSHOT`
* Writes a new (if changed) [Maven compatible][2] version string to `version.properties`.
* Optionally includes the `version.properties` file in target output.
* Optionally generates a namespace containing version information.

> The following outlines basic usage of the task, extensive testing has not been done.
> Please submit issues and pull requests!

## Usage ##

Add `boot-semver` to your `build.boot` dependencies and `require` the namespace:

```clj
(set-env! :dependencies '[[degree9/boot-semver "X.Y.Z" :scope "test"]])
(require '[degree9.boot-semver :refer :all])
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

## Task Options ##

The `version` task exposes a bunch of options for modifying the project version during a build pipeline.
Each option takes a quoted symbol (ex. `'inc`) which should resolve to an available function. This function will be applied to the current value of the project version component.

```clojure
x major       MAJ  sym  "Symbol of fn to apply to Major version."
y minor       MIN  sym  "Symbol of fn to apply to Minor version."
z patch       PAT  sym  "Symbol of fn to apply to Patch version."
r pre-release PRE  sym  "Symbol of fn to apply to Pre-Release version."
b build       BLD  sym  "Symbol of fn to apply to Build version."
d develop          bool "Prevents writing to version.properties file."
i include          bool "Includes version.properties file in out-files."
g generate    GEN  sym  "Generate a namespace with version information."
```

The `:develop` option is provided for development tasks. These tasks will modify the project version number however, this version number will not be written back to the filesystem.

```clojure
(deftask dev
  "Build boot-semver for development."
  []
  (comp
   (version :develop true
            :minor 'inc
            :pre-release 'snapshot)
   (watch)
   (build-jar)))
```

The `:include` option is provided for adding the `version.properties` file to the output directory.

```clojure
(deftask dev
  "Build boot-semver for development."
  []
  (comp
   (version :develop true
            :minor 'inc
            :pre-release 'snapshot
            :include true)
   (watch)
   (build-jar)))
```

The `:generate` option is provided for building a namespace containing a single variable `version` which will contain the the current version, it takes the namespace to be generated as input.

```clojure
(deftask dev
  "Build boot-semver for development."
  []
  (comp
   (version :develop true
            :minor 'inc
            :pre-release 'snapshot
            :generate 'degree9.boot-semver.version)
   (watch)
   (build-jar)))
```
```clojure
(ns degree9.boot-semver.version)

(def version "1.4.3")
```

## Continuous Deployment ##

The latest version of `boot-semver` now supports continuous versioning and deployment. This allows you to place the `version` task after the `watch` task to support updating the project version each time the files are changed. Additionally `build-jar`, `push-snapshot` and `push-release` are provided which support the new `version` task.

```clj
(deftask dev
  "Continuously build and deploy snapshot."
  []
  (comp
   (watch)
   (version :develop true
            :pre-release 'snapshot
            :generate 'degree9.boot-semver.version)
   (build-jar)
   (push-snapshot)))
```


## Helpers ##

A few helper functions are provided to be used with the version task.

```clojure
;; Generic
'zero 'one 'two ... 'nine

;; Pre-Release version helpers
'alpha    ;; "alpha"
'beta     ;; "beta"
'snapshot ;; "SNAPSHOT"

;; Build version helpers
'semver-date          ;; "yyyyMMdd"
'semver-time          ;; "hhmmss"
'semver-date-time     ;; "yyyyMMdd-hhmmss"
'semver-date-dot-time ;; "yyyyMMdd.hhmmss"
'git-sha1-full        ;; full git commit string
'git-sha1             ;; short git commit string (7 chars)
```

---

<p align="center">
  <a href="https://www.patreon.com/degree9" align="center">
    <img src="https://c5.patreon.com/external/logo/become_a_patron_button@2x.png" width="160" alt="Patreon">
  </a>
  <br>
  <b>Support this and other open-source projects on Patreon!</b>
</p>

---

[1]: https://github.com/boot-clj/boot
[2]: https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm

# boot-semver

[](dependency)
```clojure
[degree9/boot-semver "0.6.0"] ;; latest release
```
[](/dependency)

Semantic Versioning task for [boot-clj][1].

* Provides `version` task
* Parses a `version.properties` file to read the current project version.
* Writes a new (if changed) [Maven compatible][2] version string to `version.properties`.

> The following outlines basic usage of the task, extensive testing has not been done.
> Please submit issues and pull requests!

## Usage

Add `boot-semver` to your `build.boot` dependencies and `require` the namespace:

```clj
(set-env! :dependencies '[[degree9/boot-semver "X.Y.Z" :scope "test"]])
(require '[boot-semver.core :refer :all])
```

Then use the `version` task when pushing to clojars:

```bash
boot version -y inc build-jar push-release
```

[1]: https://github.com/boot-clj/boot
[2]: https://docs.oracle.com/middleware/1212/core/MAVEN/maven_version.htm

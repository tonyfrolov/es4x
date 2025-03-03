# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [0.12.0] - 2020-05-21
- Upgraded vert.x to 3.9.1
- Upgraded graalvm to 20.1.0
- PM can generate `security.policy` files to sandbox an application
- Allow `.d.ts` generation from plain java classes
- Added interop for `byte` type
- Docgen fixed `enum` to use proper enums, not string values
- Added interop with `Set`
- Fixed overrides on `pg-sqlclient`
- Added support for graphql vert.x module
- Replaced complex console code (which didn't work properly) with a logger
- All logging is now happening with loggers, so they can be enabled or disabled

## [0.11.0] - 2020-03-30
- Upgraded vert.x to 3.9.0
- Added `typedef` to generated js files to better hint IDE support
- Correctly identify graal 20 so `jvmci` hiperf compile is activated
- Implemented vert.x `docgen` to allow generate manuals for modules
- Created simple React SSR example
- Bumped graalvm to 20.0.0
- Graal FS Resolver will allow other files than just js (require for graal 20)
- Fixed interop with `Buffer` (Java <=> JS)
- exposed java `Throwable` as a type to `JS` alias for non instantiable `Error`

## [0.10.1] - 2020-01-24
- fix resolver with credentials to process data as UTF8
- VM version added to the `versions` command
- New utility for proxies ASM
- generated code moved to `target`
- Update graal to 19.3
- Removed promise polyfill (not needed anymore)
- Update vert.x to 3.8.5

## [0.10.0] - 2019-11-22
- Upgraded Graal to 19.3.0
- Upgraded Vert.x to 3.8.4
- Many fixes on module generation reported by `tsc`
- Some modules still fail with `tsc` (but fixes have been pushed upstream)
- `es4x` script will also take `JAVA_OPTS` into consideration when running
- Build now confirmed to work on `aarch64`

## [0.9.5] - 2019-10-18
- Moved `runtime.d.ts` to `es4x.d.ts` and integrated the missing internal modules too.
- The internal modules can now also be used from `.mjs`
- Bump `vertx` to 3.8.3
- Fixed jlink split package warning failing the build (ignored for now)

## [0.9.4] - 2019-10-11
- Added row override for sql-client (will fix TS errors on next vert.x release)
- Fix npm package license (SPDX format)
- Upgrade build plugin versions
- Added a stack package to allow usage from maven/gradle
- Bumped GSON

## [0.9.3] - 2019-10-07
- Fix Multi Thread regression on 0.9.2

## [0.9.2] - 2019-10-05
- Reduced PM size from 11MB to 4MB
- PM package doesn't depend on GraalVM to run
- Reduced code
- Bumped Vertx to 3.8.2
- Bumped Graal to 19.2.0.1
- Fix script issues with PM
- Fix TypeScript definitions that overlapped due to same class name

## [0.9.1] - 2019-09-12
- Fix issue with `pm` launcher that doesn't add itself to the classpath
- Fix issue with `pm` lancher that picks the wrong java if `GRAALVM_HOME`
  was defined
- Moved `esm`, `polyglot`, etc... run flags to jvm flags in order to be
  usable from any command
- Updated codegen to support more temporal types as they are supported
  by graal itself
- Added a few more tests to ES4X
- Instrumented `JsonObject` to be a Graal `ProxyObject` (this allows
  using it as a native object)
- Instrumented `JsonArray` to be a Graal `ProxyArray` (this is a
  preparation for hanlding it as a native array)

## [0.9.0] - 2019-08-23
- Upgrade to Graal 19.2.0
- Upgrade to Vert.x 3.8.1
- New website that allow multi language
- es4x command can now merge the classpath of the project
  allowing sharing commands during development
- ES4X commands are now properly registered using the service loader
- A new flag `--polyglot` enables Graal `Polyglot` access
- A new flag `--prefix` allows usage from outside the `CWD`
- Generator fixes to allow easier builds
- Fix broken redeploy/start command.

## [0.8.2] - 2019-08-16
- Fixed missing pom from maven central

## [0.8.1] - 2019-08-15
- Fixed start scripts for Windows
- Fixed es4x npm installer for Windows
- Added support for vendor jars
- Started removing abstraction code needed during nashorn time
- Added support for vert.x Future/Promise as JS Thenable

## [0.8.0] - 2019-05-27
- Simple ESM module app boots
- VSCode command generates a working debug config
- Debugger is working again
- Ugrade to Vert.x 3.7.1
- Avoid calling graaljs APIs from the EventBus codec
- Global log formatter used across graal and vert.x
- Fixed EventBus codec for Graal objects
- Removed Nashorn
- Update to Graal 19.0.0
- Verticle events are now triggered from the `process.on` event emitter.
- Updated to Graal rc16
- Removed `es4x-cli` as it has been replaced with `es4x-pm`
- ESM resolver can resolve npm modules that declare a `module` property.

## [0.7.3] - 2019-03-22
- Fixes bash script for debian like systems
- Added a `version` command to list all versions
- Added a `jlink` command to create slim packages
- Updated dependencies
- Added `vscode` command to generate launcher scripts
- Update to graal rc14

## [0.7.2] - 2019-01-31
- Added support for TypeScript

## [0.7.1] - 2019-01-10
- Fixed pm to work with symbolic links

## [0.7.0] - 2019-01-10
- new PM sub project to replace `es4x-cli`

## [0.6.1] - 2018-12-17
- upgrade vertx to 3.6.2
- Fixed issue loading JMVCI when missing compiler jar
- fixed typpo locating the process id
- add a package flag to build a docker image
- new website and docs

## [0.6.0] - 2018-11-29
- Upgraded to Vert.x 3.6.0
- Code gen extracted to own module

## [0.5.6] - 2018-10-07
- Added a package command to package either fat jar or a JVMCI fat jar.
- Fix issue preventing GraalJS running on OpenJ9
- Allow specifying absolute path as start module
- implemented module aliases
- fixed node inspector paths for debugging
- fixed runtime definitions
- `es4x-cli` has less dependencies and can be used by `npx`
- `vertx-scripts` has been replaced by `es4x-cli`
- Decoupled Codec from the engine (avoids calls on the wrong context)
- Several clean ups

## [0.5.5] - Demo release
- Small fixes in order to get demos running

## [0.5.4] - 2018-09-17
- Bump `pg-reactive-client` to 0.10.3
- Fix `parsePaths` to properly convert slashes on windows.
- Unit tests are now fully async and do not lead file descriptors.
- Added a shortcut (`engine`) to the `process` object with the engine name.
- Fix `util.promisify` on Graal when dealing with Java Objects.
- `AsyncError` helper to stitch asynchronous exception stacktraces.

## [0.5.3] - 2018-07-20
- Added docs about Worker and Graal
- Code clean ups
- `vertx-scripts` will install `mvnw` by default (for Graal users, this means there are no extra
  dependencies needed to run or develop ES4X)
- `Worker` API following the Ecma Interface will off load to worker verticles.
- Bump Vert.x dependencies to 3.5.3
- Bump `pg-reactive-client` to 0.9.0
- Fix build to always target `JDK8` making it possible to use on `JDK>=8` and `GraalVM 1.0.0`

## [0.5.2] - 2018-07-13
- `vertx-scripts` can generate basic native images
- Added `pg-reactive-client@0.8.0`
- Bump Vert.x dependencies to 3.5.2

## [0.5.1] - 2018-07-03
- REPL support for both Nashorn and Graal
- Added `Dynalink` to any `DataObject`
- Type definition for `process`
- Loader updated to switch from `Nashorn`/`GraalVM` depending on the runtime
- Fix Console `format` to not crash when handling unsupported types
- Added a Changelog
- Implemented `util/promisify` similar to node
- Added `process.nextTick` wrapper
- Added support to mix manual content to the typedoc/npm main page
- Codecov support to the build system


## [0.5.1] - 2018-03-22
- Initial Release

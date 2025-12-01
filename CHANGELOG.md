# SetupBuilder Changelog

## 8.4.23 (Unreleased)

### Changed
* **Version Management**: Simplified version management - version now read from `gradle.properties` instead of hardcoded `buildVersion` variable
  * Version format: `<major>.<minor>.<build>` or `<major>.<minor>.<build>-SNAPSHOT`
  * Version compatibility with Gradle version is automatically validated and adjusted if needed
* **Build Process**: Removed conditional JPP (Java Preprocessor) execution - now always processes source files for Gradle version compatibility
  * Source files are always preprocessed to `buildDir/preparedSrc-${gradleVersion}`
  * Ensures consistent build behavior across all Gradle versions
* **Publishing**: Replaced local `../repo` publishing with `mavenLocal()` for local development
  * Use `./gradlew publishToMavenLocal` for local development
  * Snapshots are published to Sonatype snapshot repository
  * Releases are published to Maven Central via Sonatype
* **CI/CD**: Migrated from Travis CI to GitHub Actions
  * CI workflow tests with multiple Gradle versions (7.6, 8.4, 8.14.2, 9.2.1) across multiple platforms (Ubuntu, Windows, macOS)
  * Release workflow automatically publishes to Sonatype and Gradle Plugin Portal

### Added
* **Sonatype Publishing**: Added full Sonatype/Maven Central publishing support
  * Snapshots automatically published to `https://oss.sonatype.org/content/repositories/snapshots/`
  * Releases go through staging → close → release process
  * Signing support for both snapshots and releases
* **Configuration Files**:
  * Created `gradle.properties` for centralized version management
  * Added GitHub Actions workflows (`.github/workflows/ci.yml` and `release.yml`)
* **Documentation**: Added "Using Snapshots" section to README.md
  * Instructions for adding Sonatype snapshot repository
  * Instructions for using `mavenLocal()` for local development

### Fixed
* Fixed Gradle 8.4 compatibility issues with `CopyProcessingSpec` interface methods
  * `getDirMode()`, `setDirMode()`, `getFileMode()`, `setFileMode()` methods properly handled
* Fixed syntax error in `nexusPublishing` configuration block
* Updated test builds to use `mavenLocal()` and Sonatype snapshot repository instead of `../../repo`

## 8.4.22 (v22)

### Changed
* Gradle 9 now requires Java 17
* Adaptations for Gradle 9 compatibility
* Current version of `com.gradle.plugin-publish` plugin only compatible with Gradle >= 7.6
  * Removed support for older Gradle versions
* Made Gradle script fit for Gradle 9
  * Replaced missing `VersionNumber` with own implementation
  * Updated `com.gradle.plugin-publish` plugin

### Fixed
* `java.net.URL` is not allowed as `@Input` anymore (#132)
* With Gradle 9 only the file name `build.gradle` is supported
* Replaced internal API `ConfigureUtil` with public API
* Removed reference to internal class `ProjectInternal`
* Made compatible with Gradle 8.12/8.13 through a more generic solution (#132)
* Fixed `gradlePlugin` settings
* Added hack for Eclipse `.classpath` file

## 8.4.21 (v21)

### Changed
* Build for Gradle version 8.4 because there are API changes since Gradle 8.0 (#125)
* Build also version 8.0 of SetupBuilder (#125)

### Added
* Added getter and setter methods for compression to use for Debian package
* Default compression is now xzip on newer Debian systems
* Can change compression for older systems (e.g., `compression="gzip"`)

### Fixed
* Debian package: skip Recommends when empty (#128)
* MSI installer: Does not use HKLM registry keys for installer with perUser scope (#127)
* RTF file must not be quoted

## 8.4.20 (v20)

### Changed
* Updated plugin dependencies
* Library updates

## 8.4.18 (v18)

### Changed
* Updates for Gradle 7

## 8.4.17 (v17)

### Changed
* Library updates

## 8.4.16 (v16)

### Changed
* Library updates

## 8.4.15 (v15)

### Changed
* Library updates

## 8.4.13 (v13)

### Changed
* Library updates

## 8.4.12 (v12)

### Changed
* Library updates

## 8.4.11 (v11)

### Changed
* Initial version tracking

---

**Note:** Version history prior to v11 may not be fully documented. This changelog tracks significant changes from version 8.4.11 onwards.


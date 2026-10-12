# libxposed API 102 migration

This branch is rebuilt from the public upstream source on GitHub, not uploaded from locally encrypted files. The master branch is unchanged.

- Modern API and service dependencies: 102.0.0.
- Modern module entry, interceptor-chain hooks, and META-INF/xposed metadata.
- Launcher / POCO / SystemUI scopes; system server uses the virtual package `system`.
- Local private preferences are mirrored through the Xposed service. Open Settings once after enabling the module so the configuration is published, then restart the target processes. System server changes require reboot.
- Compile SDK 37.0, Gradle 8.13, AGP 8.13.2, JDK 17. Target SDK remains 34 and min SDK 26. Original UI submodules remain pinned and are checked out recursively by Actions.
- Eight JVM regression tests cover early return, argument changes, result changes, exception recovery and callback failure rollback.

## Build with GitHub Actions

Actions workflow `Build API102 APK` runs on pushes to this branch and supports manual dispatch. It checks out submodules, runs tests and builds a signed Debug APK. No signing secrets or Telegram configuration are required.

Download `Fuck_Home-API102-debug` in the run's Artifacts after a successful build. Debug signing differs from the upstream release signing; uninstalling the old APK may be required after backing up settings. Debug keys may change between clean CI runners, so cross-run installation updates are not guaranteed.

This migration has undergone source review, not a successful build or device test at the time of this document. CI is the build-verification authority; inspect failed logs and fix before treating it as a working release. No guarantee is made for newer MIUI/HyperOS launcher method names or custom-font cross-UID broadcasts.
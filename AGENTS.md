# AGENTS.md

Guidance for humans and AI coding agents working on **The Hideout: Tarkov Sidekick**, an unofficial open-source Android companion app for Escape from Tarkov (flea prices, quests, hideout, barters, crafts, maps, ammo/weapon tools). Package: `com.austinhodak.thehideout`. App version in `app/build.gradle.kts` (currently 1.6.7).

> The project is a snapshot from early 2023 (AGP 7.4.0, Kotlin 1.7.20, compileSdk 33). Most versions below are outdated; see "Known upgrade debt".

## Modules

| Module | Purpose |
|---|---|
| `:app` | The application: Compose + legacy View UI, Hilt DI, Realm cache, Firebase, sync workers. Product flavors `beta` and `full`. |
| `:tarkovapi` | Library: legacy data layer (Room DB with bundled `hideout_database_*.db` assets), Apollo GraphQL for older screens, models, API helpers. |

Both modules have their own Apollo service and their own `schema.json` + `.graphql` files:
- `app/src/main/graphql/com/austinhodak/thehideout/` (package `com.austinhodak.thehideout`, response-based codegen) is the newer "v2" data path.
- `tarkovapi/src/main/graphql/com/austinhodak/tarkovapi/` (package `com.austinhodak.tarkovapi`) is the older path (Room-based, still used by maps, server status, trader restock timers, barters/crafts/quests updaters).

## Stack (as pinned in the repo)

| Area | Choice | Version |
|---|---|---|
| Language | Kotlin (+ a little Java) | 1.7.20 |
| Build | Android Gradle Plugin / Gradle wrapper | 7.4.0 / 7.6 |
| SDK | minSdk / targetSdk / compileSdk | 24 (23 in `:tarkovapi`) / 33 / 33 |
| JVM | source/target + toolchain | Java 11 |
| UI | Jetpack Compose (BOM 2023.01.00, compiler 1.3.2), Material 3 (1.0.1) + Material 2, ViewBinding/DataBinding for legacy screens, Accompanist 0.28.0 | |
| Navigation | AndroidX Navigation 2.5.3, Mavericks 3.0.1 (state/ViewModels), mikepenz MaterialDrawer 8.4.5 | |
| DI | Hilt / Dagger 2.44.2 (via **kapt**) | |
| Networking | Apollo Kotlin 3.7.4 (GraphQL), Retrofit 2.9.0 + OkHttp 4.10.0 + Gson | |
| Local storage | Realm Kotlin 1.6.0 (`library-sync`) in `:app`; Room 2.5.0 in `:tarkovapi`; Apollo SQL normalized cache (currently disabled in `ApolloModule`) | |
| Backend services | Firebase (BoM 31.2.0: Auth, Firestore, Messaging, Crashlytics, Perf, AppCheck, Functions), Sentry 6.12.1, Qonversion 4.1.3 (premium), Google Maps (maps-compose 2.9.1), ML Kit text recognition (item scanner), CameraX | |
| Images/other | Coil 2.2.2, Lottie, MPAndroidChart, kotlinx-datetime, Timber | |
| Quality | ktlint plugin 11.1.0 (applied to all subprojects), Maestro UI flows in `.maestro/` | |
| CI/CD | GitHub Actions in `.github/workflows/` (package, beta, release, secrets decode, emulator + Maestro), Fastlane in `fastlane/` | |

Version catalogs: `gradle/libs.versions.toml` (`libs.*`) and `gradle/androidx.versions.toml` (`androidx.*`, registered in `settings.gradle.kts`). Several dependencies in `app/build.gradle.kts` are still inline strings. Note `androidx.versions.toml` declares `compose-compiler = "1.4.0"`, but the build actually uses `kotlinCompilerExtensionVersion = "1.3.2"` in `app/build.gradle.kts`.

## Architecture in one paragraph

On launch, `Application.kt` initialises Firebase App Check, Mavericks, Qonversion, notification channels and the legacy static helpers (maps, traders, rigs, skills). `data/sync/` (WorkManager `SyncWorker`, `Sync`, `SyncStatusMonitor`) pulls data from the tarkov.dev GraphQL API through `apollo/TarkovApiRepository` and writes it into Realm via `realm/RealmRepository` and the converters in `realm/converters/`. Screens under `features/<name>/` (Compose screens and Activities, each with `viewmodels/`) read from Realm/Room and the API. Per-user state (quest/hideout progress, favorites, price alerts, teams, weapon builds) lives in Firebase (`firebase/`). DI modules are in `di/`.

## tarkov.dev API

- Endpoint: `https://api.tarkov.dev/graphql` (set in `app/.../di/ApolloModule.kt`; the `.graphqlconfig` files also point there). Images come from `https://assets.tarkov.dev/<itemId>-<variant>`.
- Queries run against the **live** API at runtime; the checked-in `schema.json` is only used by Apollo codegen. A stale `schema.json` therefore hides breakage until the app runs.
- Upstream schema source (useful for review and diffs): https://github.com/the-hideout/tarkov-api (`schema-static.mjs`).
- Refresh the local schema (verify task name for your Apollo version):
  `gradlew.bat :app:downloadApolloSchema --endpoint=https://api.tarkov.dev/graphql --schema=app/src/main/graphql/com/austinhodak/thehideout/schema.json`
  and the same for `:tarkovapi` with its own schema path. Then rebuild to regenerate Apollo classes.
- Newer API features not used yet: the `gameMode` argument (regular vs PVE) on most queries, and new root queries (`achievements`, `skills`, `mastering`, `prestige`, `goonReports`, `lootContainers`, `stationaryWeapons`).

### Known mismatches with the live API (found by validating the repo's `.graphql` files against upstream `schema-static.mjs`, commit 92f40c6, 2026-08-13)

1. **`app/.../Items.graphql` is invalid.** It declares variables of type `ItemCategoryName` and `HandbookCategoryName`; upstream `items(...)` now takes `categoryNames: [String]` and `handbookCategoryNames: [String]`. The server rejects the whole `Items` query, which feeds item sync and flea market screens. Fix: change both variable types to `[String]`, refresh `schema.json`, rebuild.
2. Deprecated (not yet removed) fields still used in `:app` queries: `Task.neededKeys`, `Task.traderLevelRequirements`, `RequirementTrader.level`, `TaskObjectiveItem.item`, `TaskObjectiveShoot.target`, `ItemPropertiesArmorAttachment.headZones`.
3. `:tarkovapi` queries rely heavily on deprecated API: root `quests`, `traderResetTimes`, `itemsByName`, plus `Quest.*`, `Item.imageLink`, `Barter.source`, `Craft.source`, `ItemPrice.source/requirements`, `BossSpawn.name`, `BossEscort.name`. These can be removed upstream at any time; migrate to `tasks`, `traders { resetTime }`, `items(name:)`, etc.

## Build and run

- Open the repository root in Android Studio (the project uses `settings.gradle.kts`; Gradle JDK is the `#GRADLE_LOCAL_JAVA_HOME` macro).
- `local.properties` must contain `sdk.dir` (created by Android Studio; git-ignored).
- Build variants are `<flavor><BuildType>`: `betaDebug`, `fullDebug`, `betaRelease`, `fullRelease`. Debug builds need no signing. Release signing reads `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, `KEYSTORE_ALIAS_PASSWORD` from the environment (keystore `*.jks` is git-ignored).
- Command line (Windows): `gradlew.bat :app:assembleFullDebug`, `gradlew.bat :app:installFullDebug`. Version overrides: env `VERSION`, `VERSION_CODE`.
- Gradle 7.6 does not run on JDK 20+; AGP 8+ needs JDK 17+. Match the Gradle JDK to the Gradle/AGP versions when upgrading.
- Lint/format: `gradlew.bat ktlintCheck` / `ktlintFormat`.
- UI tests: Maestro flows in `.maestro/` (`main.yaml`, `wipe.yaml`). There are no unit tests in the repo.

## Secrets and external accounts

- `app/google-services.json` and the Sentry DSN, Google Maps API key (`AndroidManifest.xml`) and Qonversion project key (`Application.kt`) are committed and belong to the original author's accounts. Do not add new secrets to the repo; do not copy these values into docs or logs.
- Firebase-backed features (login, sync of progress, price alerts, teams, shared builds) depend on that Firebase project and App Check; a fork with a different signing key may not be authorised. API-driven screens (items, flea prices, tasks, traders, maps data) only need network access to tarkov.dev.
- Realm App Services id `android-quwic` is created in `Application.kt`. MongoDB's Realm/Atlas Device Sync products are end-of-life; the app only needs local Realm.

## Conventions

- Kotlin official style (`kotlin.code.style=official`), ktlint on every subproject.
- Feature code lives in `features/<feature>/` with `viewmodels/` beside the screens; shared UI in `ui/common`; themes in `ui/theme` (Material 2) and `ui/theme3` (Material 3). New UI should be Compose + Material 3.
- Opt-in annotations for experimental Compose/coroutines APIs are applied globally in the root `build.gradle.kts`; do not repeat them per file.
- Apollo models are generated, never edited. Realm models are in `realm/models`, mapped from Apollo fragments in `realm/converters`.

## Known upgrade debt (tablet / current-Android readiness)

- AGP 7.4.0, Gradle 7.6, Kotlin 1.7.20, Compose compiler 1.3.2 and Hilt 2.44.2 are tightly coupled; upgrade in steps (AGP + Gradle, then Kotlin + Compose compiler, then Hilt/KSP, then libraries). `kapt` is used for Hilt and Room; moving to KSP is part of that.
- compileSdk/targetSdk 33 (Android 13). Newer targets change notification, foreground-service, edge-to-edge and back-navigation behaviour.
- No adaptive layouts: there is no `WindowSizeClass`, `NavigationRail` or permanent drawer usage, and the manifest sets no orientation lock or `resizeableActivity`, so on a tablet the phone UI is stretched. The README promises tablet/foldable support.
- Deprecated or archived libraries: Accompanist pager/navigation-animation/swiperefresh/placeholder/insets (replaced by Compose foundation/navigation), Firebase `*-ktx` artifacts (merged into the main artifacts in newer Firebase BoMs) and `firebase-dynamic-links` (the Firebase Dynamic Links service was shut down in 2025), Realm Kotlin `library-sync`, afollestad material-dialogs 3.3.0, StfalconImageViewer, Android Jetifier is enabled (`android.enableJetifier=true`).
- `AndroidManifest.xml` uses `requestLegacyExternalStorage` and `allowBackup="true"`; `largeHeap="true"` is set.

# AGENTS.md

Guidance for humans and AI coding agents working on **The Hideout: Tarkov Sidekick**, an unofficial open-source Android companion app for Escape from Tarkov (flea prices, quests, hideout, barters, crafts, maps, ammo/weapon tools). Package: `com.austinhodak.thehideout`. App version in `app/build.gradle.kts` (currently 1.6.7).

> The project started as a snapshot from early 2023 and is being upgraded in phases (see PR #1 and "Known upgrade debt"). Toolchain, libraries, API usage and targetSdk are current; tablet support is partial (see below).

## Modules

| Module | Purpose |
|---|---|
| `:app` | The application: Compose + legacy View UI, Hilt DI, Realm cache, local user store. Product flavors `beta` and `full`. |
| `:tarkovapi` | Library: legacy data layer (Room DB with bundled `hideout_database_*.db` assets), Apollo GraphQL for older screens, models, API helpers. |

Both modules have their own Apollo service and their own `schema.graphqls` + `.graphql` files:
- `app/src/main/graphql/com/austinhodak/thehideout/` (package `com.austinhodak.thehideout`, response-based codegen) is the newer "v2" data path.
- `tarkovapi/src/main/graphql/com/austinhodak/tarkovapi/` (package `com.austinhodak.tarkovapi`) is the older path (Room-based, still used by maps, server status, trader restock timers, barters/crafts/quests updaters).

## Stack (as pinned in the repo)

| Area | Choice | Version |
|---|---|---|
| Language | Kotlin (+ a little Java) | 2.4.21 (built-in Kotlin via AGP 9) |
| Build | Android Gradle Plugin / Gradle wrapper | 9.5.1 / 9.9.1 |
| SDK | minSdk / targetSdk / compileSdk | 24 (23 in `:tarkovapi`) / 36 / 37 |
| JVM | source/target + toolchain | Java 17 |
| UI | Jetpack Compose (BOM 2026.09.00, compiler = Kotlin Compose plugin), Material 3 + Material 2 from the BOM, ViewBinding for legacy screens. Pager, FlowRow, insets and pull-to-refresh come from Compose foundation/Material 3; no Accompanist | |
| Navigation | AndroidX Navigation 2.10.2, Mavericks 3.1.1 (state/ViewModels), mikepenz MaterialDrawer 8.4.5 | |
| DI | Hilt / Dagger 2.60.1 (via **KSP**), androidx.hilt 1.4.0 | |
| Networking | Apollo Kotlin 5.2.0 (GraphQL), Retrofit 3.0.0 + OkHttp 5.5.1 + Gson | |
| Local storage | krdb 3.3.7 (community fork of Realm Kotlin, package `io.github.xilinjia.krdb`, local only) in `:app`; Room 2.8.5 (KSP) in `:tarkovapi`; Apollo SQL normalized cache (currently disabled in `ApolloModule`) | |
| Backend services | Firebase (BoM 35.2.0, main artifacts, no `-ktx`: Analytics, Messaging, Crashlytics, Perf, Remote Config; no Auth/Firestore/Realtime Database any more), Sentry 8.64.0, Google Maps (maps-compose 9.0.1), ML Kit text recognition (item scanner), CameraX | |
| Images/other | Coil 2.7.0, Lottie, MPAndroidChart, kotlinx-datetime, Timber | |
| Quality | ktlint Gradle plugin 14.2.0 with per-module baselines in `config/ktlint/` (existing violations are baselined; new code must pass), Maestro UI flows in `.maestro/` | |
| CI/CD | GitHub Actions in `.github/workflows/` (package, beta, release, secrets decode, emulator + Maestro), Fastlane in `fastlane/` | |

Version catalogs: `gradle/libs.versions.toml` (`libs.*`) and `gradle/androidx.versions.toml` (`androidx.*`, registered in `settings.gradle.kts`). Several dependencies in `app/build.gradle.kts` are still inline strings. All Gradle plugins are declared in `libs.versions.toml` and applied with `alias(...)`. `android.nonTransitiveRClass=false` is set because `:app` code references `:tarkovapi` drawables through its own `R`.

## Architecture in one paragraph

On launch, `Application.kt` initialises the local store, Mavericks, notification channels and the legacy static helpers (maps, traders, rigs, skills). The `PriceUpdateWorker` pulls data from the tarkov.dev GraphQL API into Room. The Realm models and converters in `realm/` are no longer fed by any sync job. Screens under `features/<name>/` (Compose screens and Activities, each with `viewmodels/`) read from Realm/Room and the API. Per-user state lives on the device only (no account, no login): quest/hideout progress and keys, needed items and cart, weapon builds and custom map markers are in `local/LocalStore` (one JSON tree in `filesDir/local_store.json`: `user`, `tracker`, `loadouts`, `markers`), favorites and settings stay in DataStore. Settings > Export/Import progress writes and restores that JSON tree (favorites and settings are not part of it). The `firebase/` package only holds the data classes (`FSUser`, `User`, `WeaponBuildFirestore`) that Gson maps the tree onto; keep their field names stable (R8 keeps them). DI modules are in `di/`.

## tarkov.dev API

- Endpoint: `https://api.tarkov.dev/graphql` (set in `app/.../di/ApolloModule.kt`; the `.graphqlconfig` files also point there). Images come from `https://assets.tarkov.dev/<itemId>-<variant>`.
- Queries run against the **live** API at runtime; the checked-in `schema.graphqls` is only used by Apollo codegen. A stale schema therefore hides breakage until the app runs.
- Upstream schema source (useful for review and diffs): https://github.com/the-hideout/tarkov-api (`schema-static.mjs`).
- Refresh the local schemas: both modules have an Apollo `introspection {}` block, so run `gradlew.bat downloadServiceApolloSchemaFromIntrospection` (writes `schema.graphqls` in each module), then rebuild. If the endpoint is unreachable, print the SDL from upstream `schema-static.mjs` with graphql-js `printSchema` instead (that is how the current files were made).
- Game profiles (Settings > Display > Game profile, `UserSettingsModel.gameProfile`, enum `GameProfile`): PvP zone (default), PvE zone and PvP season. `GameModeInterceptor` (`di/`) adds `gameMode: "pve"` to every operation that declares `$gameMode` when the PvE zone is active, so new root queries that accept `gameMode` should declare `$gameMode: GameMode` and pass it. tarkov.dev only knows regular and PvE, so PvP season uses regular data. Progress is stored per profile in the `user` node of the local store (`progress`/`keys` for PvP zone, `progressPve`/`keysPve`, `progressSeason`/`keysSeason`): write with `progressField` / `keysField` and read `FSUser` through `forActiveMode()` (all in `utils/GameMode.kt`). Switching between regular and PvE API data empties the Room price, quest, barter and craft tables and refreshes them (`applyGameModeToCache`). The Realm cache and `data/sync` path is not wired into the running app and was not changed. The `tracker` node (needed items, `cart`) is shared across profiles. `local/Compat.kt` keeps the old call shapes (`FieldValue.delete()`, `ServerValue.increment()`, `userRefTracker(...)`, `userFirestore()`) so call sites did not change.
- Newer API features not used yet: new root queries (`achievements`, `skills`, `mastering`, `prestige`, `goonReports`, `lootContainers`, `stationaryWeapons`).

### Deprecated API usage

None: Apollo codegen reports zero deprecated-field warnings in either module (check with `gradlew.bat generateApolloSources --rerun-tasks` and look for "Use of deprecated field"). Keep it that way. How the legacy `:tarkovapi` data was moved off the deprecated API:

- `Quests.graphql` aliases `quests: tasks` and `RoomExtensions.toQuest()` maps each Task onto the legacy Room `Quest` (id = `tarkovDataId`, which matches the ids stored in the local progress; objective types are translated to the old names such as `find`, `collect`, `kill` or `place`).
- `ItemPrice.vendor` is turned back into the old `source` + `requirements` (`loyaltyLevel`, `questCompleted`, `playerLevel`) in `toBuySell()`. `ItemSourceName` and `RequirementType` are kept through `alwaysGenerateTypesMatching` in `tarkovapi/build.gradle`.
- `Barter.source` / `Craft.source` strings are rebuilt as "<Trader> LL<n>" / "<Station> level <n>". Trader restock timers alias `traders { normalizedName resetTime }`. Boss and escort names come from `boss.name` via the extension properties in `tarkovapi/models/MapsQueryExtensions.kt`.
- In `:app`, task keys come from `requiredKeys` on the objectives, trader levels from `traderRequirements` (`requirementType == "level"`, `value`), item objectives from `items`, shoot targets from `targetNames`, and armour zones from `zones`.

## Build and run

- Open the repository root in Android Studio (the project uses `settings.gradle.kts`; Gradle JDK is the `#GRADLE_LOCAL_JAVA_HOME` macro).
- `local.properties` must contain `sdk.dir` (created by Android Studio; git-ignored).
- Build variants are `<flavor><BuildType>`: `betaDebug`, `fullDebug`, `betaRelease`, `fullRelease`. Debug builds need no signing. Release signing reads `KEYSTORE_PATH`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, `KEYSTORE_ALIAS_PASSWORD` from the environment (keystore `*.jks` is git-ignored).
- Command line (Windows): `gradlew.bat :app:assembleFullDebug`, `gradlew.bat :app:installFullDebug`. Version overrides: env `VERSION`, `VERSION_CODE`.
- Use JDK 17 or newer for Gradle (AGP 9 / Gradle 9 require it). Install SDK platform 37 (`compileSdk = 37`, required by krdb).
- Lint/format: `gradlew.bat ktlintCheck` / `ktlintFormat`. Do not regenerate the baselines to hide new violations; fix them.
- UI tests: Maestro flows in `.maestro/` (`main.yaml`, `wipe.yaml`). There are no unit tests in the repo.

## Secrets and external accounts

- `app/google-services.json` and the Sentry DSN, Google Maps API key (`AndroidManifest.xml`) are committed and belong to the original author's accounts. Do not add new secrets to the repo; do not copy these values into docs or logs.
- Removed in the local-storage change: login, cloud sync of progress, teams and invite links, price alerts, the price-history charts and the Bitcoin screen (they needed the original author's Firebase project and Cloud Functions). Still pointing at the original author's services, to remove next: `app/google-services.json` (Analytics, Crashlytics, Perf, Messaging topics, Remote Config), Qonversion (premium), Sentry DSN and the Maps key. API-driven screens (items, flea prices, tasks, traders, maps data) only need network access to tarkov.dev.
- MongoDB Realm is end-of-life; the app uses the krdb fork for its local cache only (no sync). The Realm cache is deleted on schema mismatch (`deleteRealmIfMigrationNeeded`) and re-synced.

## Conventions

- Kotlin official style (`kotlin.code.style=official`), ktlint on every subproject.
- Feature code lives in `features/<feature>/` with `viewmodels/` beside the screens; shared UI in `ui/common`; themes in `ui/theme` (Material 2) and `ui/theme3` (Material 3). New UI should be Compose + Material 3.
- Opt-in annotations for experimental Compose/coroutines APIs are applied globally in the root `build.gradle.kts`; do not repeat them per file.
- Apollo models are generated, never edited. Realm models are in `realm/models`, mapped from Apollo fragments in `realm/converters`.

## Known upgrade debt (tablet / current-Android readiness)

- Toolchain and libraries are done (AGP 9.5, Gradle 9.9, Kotlin 2.4, KSP, Compose BOM 2026.09, Firebase BoM 35).
- targetSdk 36 is done. Edge-to-edge is on for every activity (`EdgeToEdgeCallbacks` in `Application.kt`), and `HideoutTheme` / `HideoutTheme3` wrap content in `ui/common/SystemBarsBox`, which pads `WindowInsets.safeDrawing` and paints the bar areas. Screens that want to draw behind the bars must opt out of that box deliberately; nested inset modifiers add nothing because the box consumes the insets. Do not use `window.statusBarColor` or `setDecorFitsSystemWindows`.
- Back handling: `onBackPressed()` overrides are not called at targetSdk 36 (predictive back). Use `onBackPressedDispatcher.addCallback` or Compose `BackHandler`; in `NavActivity` the `BackHandler` sits after `NavHost` so it runs first.
- Native libraries are 16 KB page aligned (checked on the APK's arm64/x86_64 `.so` files).
- Tablets: at expanded width (>= 840dp, `PERMANENT_DRAWER_MIN_WIDTH_DP` in `NavActivity`) the main drawer is shown permanently beside the content. The hamburger buttons are hidden then (`ui/common/DrawerMenuButton`, use it for any new top bar). Still missing: list-detail panes (item/quest/hideout details are separate Activities, so lists and details cannot sit side by side yet) and width limits for detail screens.  The README promises tablet/foldable support.
- Deprecated or archived libraries: afollestad material-dialogs 3.3.0, StfalconImageViewer. Jetifier is off.
- `AndroidManifest.xml` still sets `allowBackup="true"` (no backup rules) and `largeHeap="true"`, and many internal activities are `exported="true"` without needing it.

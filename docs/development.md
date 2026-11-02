# Run, test, and change the app

## Local setup

Use `main` for the developed app. The repository's default branch currently points to the earlier scaffold; an ordinary clone may therefore open the wrong version.

1. Open the repository in Android Studio and install Android SDK Platform 37. The app targets SDK 37 and supports API 26+.
2. Use the pinned Gradle 9.5.0 distribution. The wrapper JAR is deliberately untracked. If absent, the repository provides `powershell -ExecutionPolicy Bypass -File scripts/bootstrap-gradle-wrapper.ps1`; alternatively, with Gradle 9.5.0 installed, run `gradle wrapper`. The bootstrap download was not exercised during this documentation work.
3. Check the two Java requirements separately: [daemon criteria](../gradle/gradle-daemon-jvm.properties) request JetBrains JDK 25; JVM modules request a Java 17 toolchain and Android modules target Java 17. The Foojay resolver is configured in [settings](../settings.gradle.kts). Gradle may need network access to provision a missing toolchain.
4. Copy [local.properties.example](../local.properties.example) to ignored `local.properties` and set `sdk.dir`. Add `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`, or supply them as environment variables. [App configuration](../app/build.gradle.kts) checks local properties first and fails during configuration if either auth setting is absent. The publishable key is bundled into the APK; a server secret does not belong here.
5. Sync, then run the `app` configuration on an emulator/device. Dependency versions live in [the version catalog](../gradle/libs.versions.toml).

PowerShell build commands, run from the repository root after setup:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:testDebugUnitTest :feature:opportunities:opportunities-impl:testDebugUnitTest :data:opportunities:opportunities-impl:testDebugUnitTest :feature:details:details-api:test
```

On macOS/Linux, use `sh ./gradlew` for the same tasks (the wrapper is not tracked as executable). Build/test-only runs can use placeholder auth values; interactive sign-in requires a configured Supabase project. A successful APK build does not establish that the live services are reachable.

## Service contracts

Authentication uses the shared [Supabase client](../app/src/main/kotlin/com/example/problemfinder/app/di/SupabaseModule.kt). Recovery emails request `com.codingwithjoseph.immensity://auth/reset-password`; configure the corresponding redirect in the Supabase project. The app's auth callback root is `com.codingwithjoseph.immensity://auth`. Opening a bare URI does not establish a valid session.

The separate opportunities API base URL is currently hardcoded to `https://api.useimmensity.com` in [OpportunitiesNetworkModule](../data/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/data/opportunities/impl/di/OpportunitiesNetworkModule.kt); changing Supabase settings does not redirect those requests. All requests below carry the captured session's bearer token.

| Operation | Request | Response expected by the client |
|---|---|---|
| Discovery | `GET /clusters/discovery` | Nullable `featured`, plus category `collections` |
| Browse | `GET /clusters/browse/page` with category, sort, limit (20 by default), optional opaque cursor | `clusters` and nullable `nextCursor` |
| Add/watch | `POST /pipeline/watch`, JSON `{"cluster_id":"123"}` | `is_watched: true` |
| Remove/unwatch | `DELETE /pipeline/watch/123` | `is_watched: false` |

The [remote sources](../data/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/data/opportunities/impl/remote) and [DTOs](../data/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/data/opportunities/impl/model/dto) are the client contract, not a guarantee about the running backend. Ktor applies 30-second request/connect/socket timeouts and ignores additional JSON keys. Mapping still rejects malformed required fields, unknown category keys, and invalid model values.

## Tests and their limits

| Location | What it checks |
|---|---|
| [App unit tests](../app/src/test/kotlin/com/example/problemfinder/app) | Identity restoration/retry, sign-out, auth root policy |
| [Opportunity feature unit tests](../feature/opportunities/opportunities-impl/src/test/kotlin/com/example/problemfinder/feature/opportunities/impl) | Cache/request state, cancellation, stale results, pagination, watch actions, date labels |
| [Opportunity data unit tests](../data/opportunities/opportunities-impl/src/test/kotlin/com/example/problemfinder/data/opportunities/impl) | Ktor request contracts with mock engines; response/cache mapping |
| [Detail API unit tests](../feature/details/details-api/src/test) | Positive-ID route construction |
| [Data instrumented tests](../data/opportunities/opportunities-impl/src/androidTest) | Real Room behavior: transactions, ordering, account isolation, paging, shared watch updates |
| [Compose instrumented tests](../feature/opportunities/opportunities-impl/src/androidTest) | Independent card actions, filters, refresh, semantics, large text and dark theme |

With a device/emulator connected:

```powershell
.\gradlew.bat :data:opportunities:opportunities-impl:connectedDebugAndroidTest :feature:opportunities:opportunities-impl:connectedDebugAndroidTest
```

The comments-layer validation ran 110 tests from the four unit-test locations above and assembled debug successfully, using cached Gradle and placeholder auth configuration. Instrumented tests and live auth/backend journeys were not run in that pass. The `core:model` and `core:common` test files are scaffold placeholders; dedicated auth form/repository tests and a checked-in CI workflow are absent.

## Where to make a change

The workspace implementation adds domain/data workspaces API/implementation modules and core:preferences. Analyze remains in the projects feature modules. Shared Details is now feature:details:{details-api,details-impl}; the feature package is feature.details. Details/Search operation work remains intentionally unfinished.

Useful checks for the completed workspace flows are :data:workspaces:workspaces-impl:testDebugUnitTest, :feature:projects:projects-impl:testDebugUnitTest, :feature:builds:builds-impl:testDebugUnitTest and :feature:settings:settings-impl:testDebugUnitTest. Device suites cover workspace persistence, actual appearance persistence, labeled assumption interaction, conversation submit, and large-text task/document forms. They use test doubles or isolated local storage and do not run live AI generation. The two JSON fixtures in workspace data tests were exported from the backend Pydantic contracts.

Screen previews use the debug variant. Details, Search, Analyze, Build and the newer full Discovery/Browse/startup/shell/confirmation examples live under their module's src/debug/kotlin directories. Existing auth previews and the Settings default preview sit beside the production composables. Operation previews cover generation inputs, task verification, document conflicts, unsaved changes, analysis proposals and saved appearance overrides. Preview samples never enter production repository flows.

| Desired change | Start here |
|---|---|
| Root redirect, tab behavior, back stack | [App navigation](../app/src/main/kotlin/com/example/problemfinder/app/navigation) |
| Auth validation or fields | [Authentication feature](../feature/authentication/auth-impl/src/main/kotlin/com/example/problemfinder/feature/authentication/impl) |
| Browse/Discovery rendering or request state | [Opportunity feature](../feature/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/feature/opportunities/impl) |
| Payload fields, endpoint, cache structure | [Opportunity data implementation](../data/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/data/opportunities/impl) |
| Cross-screen colors, typography, icons | [Design system](../core/designsystem/src/main) |
| New business rule | Appropriate domain use-case implementation; keep I/O in the repository |

For stale content, inspect account ID, `cacheReady`, `hasSynced`, request errors, and cursor separately. For an unexpected redirect, reproduce the input combination in `AuthNavigationPolicyTest`. When changing Room entities, account for existing database version/schema compatibility; the current builder does not configure a destructive fallback. Follow [CONTRIBUTING](../CONTRIBUTING.md), and add tests at the boundary whose behavior changed.

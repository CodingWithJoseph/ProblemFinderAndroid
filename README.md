# Problem Finder Android scaffold

This repository is an intentionally incomplete Android foundation. It sets up module boundaries, MVI contracts, Hilt, Jetpack Compose, Kotlin, coroutines, and Flow without implementing the product behavior.

## Requirements

- Android Studio with JDK 17 or newer
- Android SDK Platform 37
- Android SDK Build Tools 36.0.0+
- Gradle 9.4.1 (the wrapper metadata is pinned; if `gradle/wrapper/gradle-wrapper.jar` is absent, run `powershell -ExecutionPolicy Bypass -File scripts/bootstrap-gradle-wrapper.ps1` or `gradle wrapper` once)

## Modules

```text
app                         Composition root and top-level navigation
core:model                  Shared, dependency-free domain models
core:common                 Cross-cutting contracts such as dispatcher policy
core:mvi                    Reusable MVI contracts and lifecycle plumbing
core:designsystem           Compose theme and reusable UI primitives
data:problems-api           Repository abstraction
data:problems-impl          Data implementation seam and Hilt binding
domain:problems-api         Use-case abstractions consumed by features
domain:problems-impl        Use-case implementation seam and Hilt binding
feature:problemfinder-api   Public route/navigation contract
feature:problemfinder-impl  MVI contract, ViewModel, and Compose entry point
```

Dependency direction is inward toward contracts:

```text
app -> feature impl -> feature api
                  \-> domain api <- domain impl -> data api <- data impl
                  \-> core modules
```

The `app` module is allowed to know implementation modules because it is the composition root. Feature modules consume domain APIs, never data implementations. API modules remain small and stable so implementations can be replaced without forcing unrelated callers to change.

## Start here

1. Read [`docs/architecture.md`](docs/architecture.md).
2. Work through [`docs/implementation-checklist.md`](docs/implementation-checklist.md).
3. Find all intentional gaps with `rg "TODO\\("` and `rg "TODO:"`.
4. Add tests beside each behavior as you implement it.

The current app can render the placeholder route. Any real intent or data operation intentionally reaches a `TODO(...)` until you implement it.

## Toolchain choices

Versions are centralized in `gradle/libs.versions.toml`. Android modules use AGP 9's built-in Kotlin support; the obsolete `org.jetbrains.kotlin.android` plugin is intentionally absent. Compose dependencies use the official BOM.

Useful references:

- [Guide to app architecture](https://developer.android.com/topic/architecture)
- [Modularization guide](https://developer.android.com/topic/modularization)
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- [Compose setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
- [StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)

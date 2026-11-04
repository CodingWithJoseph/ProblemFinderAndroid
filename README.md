# Problem Finder Android

Problem Finder, branded **Immensity** in the app, is an Android client for discovering problem clusters and adding opportunities to a pipeline. It uses Kotlin, Jetpack Compose, MVI-style ViewModels, Hilt, Supabase authentication, Ktor, and Room.

**Use `main` for the developed app.** The repository's default branch currently points to the older `scaffold/problem-finder-foundation` branch.

## What works

- Sign-in, two-step registration, recovery-email requests, password reset through auth links, and sign-out.
- Account-aware startup and a signed-in tab shell.
- Discovery dashboard with featured and category collections.
- Browse filters, sorting, cursor pagination, refresh, and account-scoped cached content.
- Server-confirmed add/remove watch actions reflected across Discovery and Browse.

Analyze and Build include account-scoped project lists, saved workspaces, generation progress, analysis actions/conversations, build approval, task verification, document editing, and ZIP export. Settings saves System/Light/Dark appearance and uses the existing sign-out flow. Details and Search have full screen/previews/navigation scaffolds; their remaining operation logic is intentionally unfinished. Cached content can remain readable without a usable network session; server writes require a session and are not queued offline. See [feature behavior](docs/navigation-and-features.md) for scope and backend prerequisites.

## Start reading

The [documentation map](docs/README.md) gives a short reading order. Each layer answers a different question:

| Question | Guide |
|---|---|
| Who calls whom, and how does data return? | [Runtime flows](docs/runtime-flows.md) |
| How do screens, Back, and auth redirects work? | [Navigation and features](docs/navigation-and-features.md) |
| Why these boundaries and patterns? | [Architecture and tradeoffs](docs/architecture-and-decisions.md) |
| How do I run, test, debug, or change it? | [Development](docs/development.md) |
| Can I explain the implementation myself? | [Study guide](docs/study-guide.md) |

KDoc and inline comments explain meaningful contracts and non-obvious behavior next to the source. The guides link to that code instead of duplicating it.

## Build and contribute

Open in Android Studio with SDK 37 and the pinned Gradle 9.5.0 toolchain. Configure the local SDK path plus `SUPABASE_URL` and `SUPABASE_PUBLISHABLE_KEY`; read [setup and test commands](docs/development.md) for wrapper bootstrap and the distinct Java daemon/compilation requirements.

The 32 modules are listed in [settings.gradle.kts](settings.gradle.kts) and grouped in the architecture guide. Follow [CONTRIBUTING](CONTRIBUTING.md). Feature status is recorded in [navigation and features](docs/navigation-and-features.md); populated previews do not establish working business operations.

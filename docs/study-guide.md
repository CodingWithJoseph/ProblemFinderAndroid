# Explain the project from the code

Use these as short exercises, not lines to memorize. For each answer, open the named implementation, trace one example, and explain one limitation. The goal is to confidently distinguish what you understand, what the code does, and what remains unfinished.

## A project explanation to build toward

“Problem Finder, branded Immensity in the app, is an Android client for discovering problem clusters and adding opportunities to a pipeline. It uses Compose, feature ViewModels, domain contracts, Supabase authentication, Ktor, and a Room cache. Browse and Discovery observe the same account-scoped opportunity rows, so confirmed watch changes propagate across both screens.”

Adapt that description to your own understanding. The current Search, Projects, Builds, Settings, and detail screens are placeholders. Cached reading is implemented; queued offline mutations and automatic background synchronization are not.

## Six walkthroughs

| Question | What to demonstrate | Code to open |
|---|---|---|
| What happens after a tap? | Screen callback → intent → ViewModel → use case → repository; Room emission → state → recomposition. Explain why request completion does not supply the rows. | [BrowseViewModel](../feature/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/feature/opportunities/impl/browse/BrowseViewModel.kt) |
| Why have both state and effects? | Loading/error/content survive collection changes through StateFlow. Navigation/messages use a buffered channel. Explain that effects are not persisted or broadcast. | [BaseMVIViewModel](../core/mvi/src/main/kotlin/com/example/problemfinder/core/mvi/BaseMVIViewModel.kt) |
| Can the app open while offline? | Cached identity can allow local content during initialization/refresh failure; a usable token is separately required for network work. Walk through explicit sign-out versus refresh failure. | [AuthNavigationPolicy](../app/src/main/kotlin/com/example/problemfinder/app/navigation/AuthNavigationPolicy.kt) and [session provider](../data/authentication/auth-impl/src/main/kotlin/com/example/problemfinder/data/authentication/impl/SupabaseAuthSessionProvider.kt) |
| How do you avoid stale results? | Change filter or account while work is pending. Identify cancelled jobs, generation checks, account checkpoints, and cursor checks; explain what each protects. | [BrowseViewModel](../feature/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/feature/opportunities/impl/browse/BrowseViewModel.kt) and [BrowseRepositoryImpl](../data/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/data/opportunities/impl/BrowseRepositoryImpl.kt) |
| Why does a watch change appear in two screens? | Both queries join shared opportunity rows for the same account. The server confirms the change before the cache updates; one mutex orders mutations with refreshes. Network and Room are still separate transactions. | [OpportunityActionsRepositoryImpl](../data/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/data/opportunities/impl/OpportunityActionsRepositoryImpl.kt) |
| Why so many modules? | Show an API/implementation pair and its Hilt binding. Explain substitution/testing benefits alongside build/configuration overhead. Some domain implementations currently only delegate. | [BrowseDomainModule](../domain/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/domain/opportunities/impl/di/BrowseDomainModule.kt) and [app dependencies](../app/build.gradle.kts) |

## Three small experiments

1. Step through “refresh after loading two pages.” Predict the cache membership and cursor, then read `refreshRemovesPreviouslyLoadedPagesAndSuccessfulEmptyResponseIsSynced` in [BrowseRepositoryTest](../data/opportunities/opportunities-impl/src/androidTest/kotlin/com/example/problemfinder/data/opportunities/impl/BrowseRepositoryTest.kt).
2. Explain why a failed page requires explicit retry while existing rows remain visible. Find both the ViewModel guard and the footer action before reading [BrowseViewModelTest](../feature/opportunities/opportunities-impl/src/test/kotlin/com/example/problemfinder/feature/opportunities/impl/browse/BrowseViewModelTest.kt).
3. Sketch a real details feature: route ID → new use-case/repository contract → cached/detail state → screen. List loading, missing record, and error behavior before writing code. The current placeholder provides navigation only.

For interviews or a resume, describe AI assistance plainly and claim decisions/testing you can substantiate. A useful next contribution is to implement one missing feature yourself, add focused tests, and explain the tradeoff you chose. The [development guide](development.md) identifies current test coverage and gaps; the [architecture guide](architecture-and-decisions.md) supplies alternatives to discuss.

# Implementation checklist

The ordering below keeps decisions local and testable.

## Product contract

- [ ] Define what a "problem" represents and extend `Problem` accordingly.
- [ ] Decide search/filter/sort semantics and validation rules.
- [ ] Decide which UI conditions are durable state and which are one-time effects.
- [ ] Define loading, empty, partial, offline, and failure behavior.

## Data

- [ ] Choose local and/or remote data sources inside `data:problems-impl`.
- [ ] Add implementation-private DTO/entity models.
- [ ] Map implementation models to `core:model` at the boundary.
- [ ] Implement `DefaultProblemRepository` and its unit tests.
- [ ] Add Retrofit/Room/DataStore only when a concrete requirement needs one.

## Domain

- [ ] Implement `DefaultSearchProblems`.
- [ ] Add domain validation and error types only when their behavior is known.
- [ ] Test the use case against a fake `ProblemRepository`.

## Feature

- [ ] Implement every branch in `ProblemFinderViewModel.onIntent`.
- [ ] Define cancellation, debounce, and stale-result behavior.
- [ ] Replace the placeholder `ProblemFinderScreen` with stateless UI.
- [ ] Handle effects in `ProblemFinderRoute`.
- [ ] Add ViewModel and Compose UI tests.

## Production readiness

- [ ] Add structured logging and crash reporting behind abstractions.
- [ ] Add accessibility labels and test large font/display sizes.
- [ ] Define analytics events behind a stable API if analytics is required.
- [ ] Add baseline profiles and macrobenchmarks after real user journeys exist.
- [ ] Add CI for lint, unit tests, and debug assembly.
- [ ] Review R8 rules and release signing outside source control.


# Architecture

## Rules

1. A feature depends on domain contracts, not repository implementations.
2. Domain implementations depend on repository contracts, not Android UI.
3. Data implementations may depend on network, persistence, sensors, or other external systems; their API module must not.
4. The app module owns assembly: implementation modules, navigation host, and application-wide DI.
5. UI state is immutable and is the single render input.
6. UI actions enter through intents. One-off outputs leave through effects. Durable UI facts belong in state.
7. `Flow` is exposed read-only across boundaries. Mutable streams stay private to their owner.
8. Domain and data APIs contain no Compose or Android framework types.

## MVI loop

```text
Compose UI --Intent--> ViewModel --use case--> domain/data boundary
     ^                     |
     |                     +--Effect--> transient UI action
     +------StateFlow------+
```

The scaffold supplies the types and stream plumbing. You still need to decide intent semantics, cancellation/debouncing policy, reducer behavior, error mapping, persistence, and navigation behavior.

## API versus implementation modules

Every replaceable area is split into `*-api` and `*-impl` modules. Keep API modules limited to contracts and stable types. A replacement implementation should require changes only in the composition root and its own module.

Do not add DTOs, database entities, HTTP models, or vendor SDK types to an API module. Map those at the implementation boundary.

## Adding a feature

Create `feature:<name>-api` and `feature:<name>-impl`. Put routes and cross-feature navigation contracts in the API. Put the ViewModel, Compose screen, and internal models in the implementation. Depend only on required domain APIs and core modules.

## Testing seams

- Test ViewModels with fake use-case interfaces and a controlled coroutine dispatcher.
- Test use-case implementations with fake repositories.
- Test repository implementations with fake local/remote sources.
- Test composables with immutable state and callback assertions; do not require a real ViewModel.
- Reserve Hilt tests for graph/integration coverage. Plain constructor-based unit tests should be the default.


# Implementation status and next decisions

This replaces the original scaffold checklist, whose Problem/search repository classes no longer exist. The current product model is `Opportunity`, representing a backend cluster. See [navigation and features](navigation-and-features.md) for behavior and [development](development.md) for tests.

## Implemented foundations

- [x] Authentication commands, recovery callback handling, session-driven root navigation, and sign-out retry.
- [x] Discovery and Browse with separate cache observation and network request state.
- [x] Room account/query isolation, transactional membership writes, cursor paging, and shared watch state.
- [x] ViewModel, mapping, request-contract, route-policy, Room, and Compose test sources. The development guide distinguishes tests run from tests merely present.
- [x] Shared theme, typography, accessibility semantics, and preview variants for key opportunity controls.

## Open work, not a committed roadmap

- [ ] Define and implement real opportunity details, Search, Projects, Builds, and Settings behavior. Their destinations exist, but the product workflows do not.
- [ ] Add focused authentication form/repository tests and integration coverage for actual sign-in/recovery callbacks. Existing root-policy tests do not exercise email delivery or the SDK end to end.
- [ ] Replace the stale `core:model/ProblemTest` and `core:common/AppDispatchersTest` placeholders with meaningful coverage, or remove unused scaffolding in a separate code change.
- [ ] Add CI for build, unit tests, lint, and an appropriate device-test lane; no workflow is checked in.
- [ ] Establish Room migration/schema testing before changing persisted entities, plus a deliberate cache retention/cleanup policy.
- [ ] Decide whether offline writes, background sync, automatic retries, or configurable API environments are product requirements before adding infrastructure for them.
- [ ] Define release signing, minification/R8 requirements, observability, and performance measurement before claiming production readiness.

For the next feature, write down its inputs, loading/empty/error behavior, account ownership, navigation, and tests first. Keep that specification short and link it to the implementation.


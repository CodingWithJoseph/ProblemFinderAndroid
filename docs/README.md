# Documentation map

These guides describe the developed app on `main`, starting from commit `01e3f50` (Discovery dashboard). They explain implemented behavior; proposed work and inferred design rationale are labelled separately.

| Layer | Read this to answer | Approximate read |
|---|---|---:|
| Code comments | What responsibility, contract, or tricky behavior does this class/function have? Start at [AppNavigation](../app/src/main/kotlin/com/example/problemfinder/app/navigation/AppNavigation.kt) or [BrowseViewModel](../feature/opportunities/opportunities-impl/src/main/kotlin/com/example/problemfinder/feature/opportunities/impl/browse/BrowseViewModel.kt). | As needed |
| [Runtime flows](runtime-flows.md) | Who calls whom, which work is asynchronous, and where does UI data come from? | 7 min |
| [Navigation and features](navigation-and-features.md) | What screens exist, what happens on Back, and which features are placeholders? | 6 min |
| [Architecture and decisions](architecture-and-decisions.md) | How do the 27 modules fit together, and what are the tradeoffs? | 6 min |
| [Development](development.md) | How do I configure, build, test, and find the right change location? | 7 min |
| [Study guide](study-guide.md) | Can I trace and explain the project without memorizing descriptions? | 5 min + exercises |

For a first pass, read runtime flows → navigation → architecture. Follow only the code links for a journey you want to understand deeply. Use development as a reference and the study guide to check your understanding.

When behavior changes, update the nearby comments and the relevant guide in the same PR. Keep generic patterns in architecture, call order in runtime flows, and visible behavior in navigation. Avoid copying full source listings or maintaining an exhaustive framework call graph.

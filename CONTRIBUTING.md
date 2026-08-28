# Working agreements

- Keep public contracts in `*-api` modules small and implementation-independent.
- Prefer constructor injection and immutable state.
- Keep mutable `StateFlow`/`SharedFlow`/`Channel` instances private.
- Add a focused test for every behavior before adding integration coverage.
- Do not commit `local.properties`, signing material, generated output, or secrets.
- Keep dependency versions in `gradle/libs.versions.toml`.
- Use `rg "TODO"` before handing off work so intentional scaffolding is visible.


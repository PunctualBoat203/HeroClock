# Validation

Local pure-Java regression tests cover deadline arithmetic, legacy key normalization, unchanged immutable values versus mutable values, queue ordering, capacity, coalescing, cooperative time budgets, exceptions, cancellation, continuations and thread confinement.

Forge-dependent tests exercise the actual timer API with mocked world/entity objects: compressed NBT round trips, dimension changes, block persistence dirtiness, unsupported access, overflow and non-mutating reads. These run in CI with the Forge development classpath.

A production release still needs the target modpack to verify:

1. Launch on Forge 47.x both with and without Palladium 4.5.9; check all mixins apply and no duplicate addon registrations occur.
2. Save/reload, chunk unload/reload, player death/respawn and dimension travel preserve expected remaining timer ticks.
3. Known temporary helpers clean up in every dimension, including partial block cleanup across unload/reload and saturated work queues.
4. Property changes reach clients; repeated equal scalar values do not send redundant packets; mutable properties continue syncing.
5. Datapack reload and world reconnect rebuild command caches correctly.
6. Compare tick-time percentiles, packet counts and client frame times on the same representative addon workload before/after the mod change.

No live modpack benchmark or in-game validation is claimed by these tests. The 1 ms work budget limits starting additional steps; it cannot bound an individual callback's execution time.

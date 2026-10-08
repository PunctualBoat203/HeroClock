# Compatibility contracts

HeroClock authorizes each optional code optimization separately by the method bodies and field layout it uses. A mod version string is context, not permission to patch. An unchanged audited target can work in a newer version; a changed target in the same version is rejected.

`compatibility/contracts.json` contains SHA-256 fingerprints of normalized executable instructions and required field contracts. Debug lines, source filenames, frames and constant-pool layout do not affect the result. Opcodes, constants, referenced members, branches, exception handlers, declared exceptions, descriptors and access flags do. Minecraft development names are normalized through Forge mappings; production and development target checks share one contract.

Checks run before optional mixins are selected. Missing mods, missing contracts, changed methods/fields, inspection errors and explicit disable switches leave the original target behavior in place. Each decision is available through `/heroclock status` and `HeroIntegrationAPI.compatibility()`. A changed target does not disable unrelated optimizations. These are conservative compatibility checks, not proof that every interaction with other mods is safe; materially changed code needs review and a new audited contract.

The former Satsu sentinel redirect is removed in 2.2.23. Matching the function body did not preserve synchronous execution, executed-command counts or direct mutations of the exposed entity tag set. Satsu functions now execute through Minecraft unchanged; no deferred sentinel kills are scheduled. Historical resource overrides remain a separate concern.

Audited code inputs for the initial contracts:

- Palladium 4.5.9: supplied binary SHA-256 `af99a7ba746404c9774cd737dcc1db2d1f6fb7963fdbfa1bbee6b5b9832e29c9`.
- Curios 5.14.1+1.20.1: official Maven binary SHA-256 `6d77ae8ad532fdf303390f404b0081b3b4ac4f61e7f1f4b4d4a9077e132dae4f`. This differs from the older supplied artifact's whole-JAR hash recorded in the archived 2.2.15 handoff. The contract concerns the inspected `getCurios` method and `curios` field, rather than claiming those entire artifacts are identical.

`tools/java/GenerateContracts.java` rebuilds the initial manifest from these inputs using the same fingerprint implementation. Do not automatically learn or accept fingerprints from unknown installed code: that would defeat the guard. The disable switches `heroclock.disablePalladiumOptimizations` and `heroclock.disableCuriosOptimizations` remain available.

Static historical resource overrides are not made automatically compatible with arbitrary addon versions by these Java guards. Their validation boundaries remain documented separately.

KubeJS and Rhino targets use the same independent contracts and have separate `heroclock.disableKubeJSOptimizations` and `heroclock.disableRhinoOptimizations` switches. These also disable their optional telemetry redirects. The embedded scripting API remains available even when an optional patch is rejected. See [SCRIPTING.md](SCRIPTING.md) for exact inputs.

PalladiumCore's Architectury registry snapshot allocation is also independently code-guarded in 2.2.24. The patch retains a bounded numeric capacity hint, not registry values. It uses the existing Palladium disable switch. See [the supplied-mod audit](MOD_AUDIT_2026-10-07.md) for the exact nested JAR and behavior boundary.

## Companion power resources

In 2.2.25, the seven audited OmniOptimizer 1.8.2 AlienEvo power files have independent whole-file SHA-256 contracts in `OmniPowerResources`. Matching bytes work regardless of the companion version label. HeroClock yields only its own matching paths, leaving the companion and user datapacks untouched. Changed formatting also fails this conservative byte check; missing, unreadable or larger-than-1-MiB files retain normal resource selection. No installed resource is automatically learned as trusted.

The optional Forge factory hook matches the exact supported method signature and filters the existing pack without changing its position. It leaves unmodified factory results untouched. Checks happen at pack creation and are reported in the startup/resource-loading log; they are separate from the Java optimization decisions exposed by `HeroIntegrationAPI`. Client assets, unrelated server data, pack metadata and the mergeable `minecraft:load` tag are preserved. Other historical resource overrides are not covered by these seven contracts.

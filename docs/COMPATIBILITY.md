# Compatibility contracts

HeroClock 2.2.16 authorizes each optional code optimization separately by the method bodies and field layout it uses. A mod version string is context, not permission to patch. An unchanged audited target can work in a newer version; a changed target in the same version is rejected.

`compatibility/contracts.json` contains SHA-256 fingerprints of normalized executable instructions and required field contracts. Debug lines, source filenames, frames and constant-pool layout do not affect the result. Opcodes, constants, referenced members, branches, exception handlers, descriptors and access flags do. Minecraft development names are normalized through Forge mappings; production and development target checks share one contract.

Checks run before optional mixins are selected. Missing mods, missing contracts, changed methods/fields, inspection errors and explicit disable switches leave the original target behavior in place. Each decision is available through `/heroclock status` and `HeroIntegrationAPI.compatibility()`. A changed target does not disable unrelated optimizations. These are conservative compatibility checks, not proof that every interaction with other mods is safe; materially changed code needs review and a new audited contract.

Satsu uses a runtime resource contract: the effective loaded `satsu_iron_man_addon:tick` must contain exactly one vanilla command entry, `kill @e[tag=sentinel_kill]`. Function object changes after reload are rechecked. Added/replaced commands retain the stock function. Already queued sentinel cleanup rechecks the current function before acting. Queue pressure also retains stock execution.

Audited code inputs for the initial contracts:

- Palladium 4.5.9: supplied binary SHA-256 `af99a7ba746404c9774cd737dcc1db2d1f6fb7963fdbfa1bbee6b5b9832e29c9`.
- Curios 5.14.1+1.20.1: official Maven binary SHA-256 `6d77ae8ad532fdf303390f404b0081b3b4ac4f61e7f1f4b4d4a9077e132dae4f`. This differs from the older supplied artifact's whole-JAR hash recorded in HANDOFF.md. The contract concerns the inspected `getCurios` method and `curios` field, rather than claiming those entire artifacts are identical.

`tools/java/GenerateContracts.java` rebuilds the initial manifest from these inputs using the same fingerprint implementation. Do not automatically learn or accept fingerprints from unknown installed code: that would defeat the guard. The disable switches `heroclock.disablePalladiumOptimizations` and `heroclock.disableCuriosOptimizations` remain available.

Static historical resource overrides are not made automatically compatible with arbitrary addon versions by these Java guards. Their validation boundaries remain documented separately.

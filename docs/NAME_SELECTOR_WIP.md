# Paused name-selector prototype

This branch is a checkpoint, not a validated release. The user requested a safe pause on 2026-10-08. Do not merge or distribute this prototype until the checks below pass. The version remains 2.2.25; no new runtime build has been validated.

The subsequent [server capture with 2.2.25](PROFILE_2026-10-08.md) confirms the existing registry/property hooks are executing and that command, NBT, suit and name-selector costs remain. It supports continued investigation of this prototype but does not validate it or establish its performance gain.

The current tested release is HeroClock 2.2.25, validated by [CI run 37698770315](https://github.com/PunctualBoat203/HeroClock/actions/runs/37698770315). Its artifact is [HeroClock-2.2.25](https://github.com/PunctualBoat203/HeroClock/actions/runs/37698770315/artifacts/11516489394). The runtime implementation was committed as `b3af6fe206c5e72e288bd879bbd4ee322246cb64`; final handoff documentation followed in `db7b4a43df17eb0137d4b0a3b9f4a036445b356d`.

The `improve-clock-runtime` branch was left at audit-only commit `32364fe7b567ccab712c0867b53fc76995553818`. [CI run 37724599626](https://github.com/PunctualBoat203/HeroClock/actions/runs/37724599626) passed for that audit checkpoint. This WIP branch contains the subsequent, uncompiled production changes.

## Proposed change

Minecraft's name-selector predicate currently calls `entity.getName().getString().equals(name) != inverted`. The audited 1.20.1 implementation creates a StringBuilder and a flattened string for each comparison. The prototype streams component fragments into a comparison while retaining full traversal, including callbacks and exceptions after a mismatch. It does not cache names or entity results.

- `runtime/NameMatcher.java`: comparison for exact MutableComponent roots; custom component implementations and overridden getString behavior fall back to the original call.
- `compat/NameSelectorContract.java` and `compatibility/name-selector.json`: proposed bytecode contracts for the predicate, Component/FormattedText getString methods, and the append-fragment lambda. Member names are normalized from the compiled helper's references.
- `compat/MethodFingerprint.java`: canonical-name overload; existing callers retain the original behavior.
- `mixin/NameSelectorMixin.java`, `PalladiumPlugin.java`, `CompatibilityGate.java`, and the mixin manifest: proposed guarded injection, diagnostics, and `heroclock.disableSelectorOptimizations` switch.

The four JSON hashes were calculated manually from audited instructions. They have NOT been verified against actual ASM fingerprints. The predicate's development name is `lambda$bootStrap$5`, descriptor `(Ljava/lang/String;ZLnet/minecraft/world/entity/Entity;)Z`; its observed production name is `m_175206_`.

## Required before release

1. Compile and verify the stored fingerprints against real Minecraft classes. Test unchanged and debug-only changes, altered method bodies or exception declarations, missing dependencies, and production name normalization. Never accept unknown code by learning a new hash at runtime.
2. Add differential tests against getString().equals: literal, sibling, styled and translated components; Unicode; null fragments/targets; live name or sibling mutations; custom root overrides; and callbacks or exceptions after an early mismatch.
3. Verify the generated production refmap targets the correct predicate and that the injection actually applies when the contract passes. Add selector GameTests for positive/negated names, live rename, ordering/limits/distance, and fallback with the optimization disabled.
4. Review effective transformed bytecode and other-mixin interactions. The current gate checks provider bytecode; it does not yet revalidate the target in preApply. Do not claim protection against every later transformer.
5. Remove the temporary `SelectorBytecodeAuditTest`, the `auditSelectorCode` Gradle logging/property, and the workflow's `-PauditSelectorCode` flag. These belong to the preceding audit-only checkpoint.
6. After successful tests, update the version, CI expectations, handoff and PR. No TPS/FPS improvement has been measured for this prototype.

Local Java currently cannot load libjli.so; validation has used GitHub CI. No CI run was requested for this WIP checkpoint.

The uploaded mod archive, extracted mod JARs and profiler files were removed by workspace cleanup. Exact Ninjago/FSang resource patches still require those bytes to be attached again or otherwise made available through an authorized source. Existing audit findings remain in the repository docs. HeroClock's scope stays clock/work infrastructure and narrow guarded optimizations; the custom KubeJS/Rhino replacement belongs to Mantis. Author attribution remains PunctualBoat.

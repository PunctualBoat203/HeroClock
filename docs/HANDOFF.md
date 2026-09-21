# HeroClock development handoff

This file is the resume point for future HeroClock work, especially when Astra 6 access is available again.

## Current branch and intent

Development branch: `improve-clock-runtime`

Target development version: **2.2.15**

Design contract:

- Preserve the improvements already added in the 2.2.15 branch: direct mapped deadlines, persistent timer semantics, bounded/coalesced server work, lifecycle cleanup, Palladium 4.5.9 sync/cache optimizations, tests and recovery behavior.
- Preserve released 2.2.14 behavior unless a replacement is intentionally documented and verified.
- Prefer **stock addon JAR + HeroClock**. Historical patched addon JARs were server-specific optimization vehicles and should not become a required public deployment model.
- Where a stock addon performs a known expensive operation, prefer a version-gated HeroClock hook/adapter that redirects redundant work through HeroClock internals while preserving the same gameplay result.
- Do not globally throttle arbitrary Palladium/KubeJS ticks. Only suppress work that is demonstrably redundant, or use an addon/version-specific adapter whose cadence tradeoff has been verified.
- If a target mod/version is unknown, leave it alone instead of guessing.
- Mods/JARs not listed below were intentionally out of scope for this pass.

## Verified HeroClock release reference

Supplied released artifact:

`HeroClock-2.2.14-forge-1.20.1.jar`

SHA-256:

`68549ce622e07d0b2b8c53086ad1b8e240de67a636c84d492a20d97a7cef9897`

This exactly matches the previously recorded 2.2.14 hash. The binary is now available as the release-regression reference. The editable 2.2.15 source was still originally reconstructed from 2.2.10.

The 2.2.14 binary confirms that `patches/**` entries were inert reference/server-patching payloads; `PatchManager.apply()` only reports that runtime JAR rewriting is disabled.

## Supplied stock baselines

The following are the only addon artifacts considered verified inputs for this handoff. Hashes are SHA-256 of the supplied files. “Embedded version” means the version declared by the mod's own metadata, which may differ from the filename.

| Supplied artifact | Primary mod id | Embedded version | SHA-256 |
| --- | --- | --- | --- |
| `AlienEvo-1.1.3-forge.jar` | `alienevo` | `1.1.2` | `115a9b6526c8ea93ed22dda90e171a3a0bb88b1a3af69667224aea75df22dae0` |
| `infinity-7.2.jar` | `infinity` | `7.1` | `1ad6c1b8d29237592f61cc1f178c4812e903e4846e5e1a28833bd30832411276` |
| `infintrix-2.2.jar` | `infintrix` | `2.2` | `ac45c6bd2c9d60dffca2c874925c5cc002c7776263769ef6cc2d06ee0c3f39ab` |
| `Satsu_iron_man_addon-3.5.3.jar` | `satsu_iron_man_addon` | `3.5.3` | `fea4ae8d9fba8527f6b96099fd222dc35236e7f2afcc99cfca033f9ea1788df5` |
| `omni_evo_1.0.6.jar` | `omni_evo` | `1.0.0` | `919da280a21327dff4087362f1ca9fccd2e2d7e9d7d3fb066156b8f97245a122` |
| `IntoTheOmniverse-v1.0.6.jar` | `aeo` | `1.0.6` | `a0667c504f40b9f68928e7cb1cdc8838329a1a10c23c331227b1afe939f75a45` |
| `celestialsapien-1.0.6.1.jar` | `mypowers` | `1.0.6` | `3f5a610389f9444215a28568a5375e79472738c8f09521ef266e105aa9221823` |
| `powerborne-heroes-1.20.1-0.5.1.jar` | `powerborne` | `0.5.1` | `531d88e5e915ab0055027477df995d4c29a0f89775ac0a2b60b63e6be5d20fc2` |
| `saiyan-3.0.jar` | `saiyan` | `0.1` | `c736737d9b5bf8771db201fbefc46d1130ef9f4631233c531db40f908f5c7ef2` |
| `pantheonsent-1.1.1+1.20.1-forge.jar` | `pantheonsent` | `1.1.1` | `3a323241a3bfe91afc0151c98779bef818d127f4816d71e9c15e630ff1639a02` |
| `OmniOptimizer-1.8.0.jar` | `omnioptimizer` | `1.8.0` | `43fee44ddfc8c44df816d74e1dc89075b22237f9d2b4549bf7866e1b9cfc3e1f` |

The filename/metadata mismatches are intentional facts of the supplied artifacts and must not be “corrected” by guessing. Runtime compatibility gating should use Forge-loaded metadata or an explicit artifact/profile check, not filename parsing.

## Findings that matter for future work

### Released 2.2.14 versus stock JARs

The released HeroClock 2.2.14 binary contains active resource overrides for several stock addons and also contains historical patch payloads under `patches/**`.

Direct stock-resource intersections observed against the supplied artifacts include:

- AlienEvo: targeted Palladium power/function overrides, including lightning/ice/helper-related resources.
- Infinity: targeted Infinity Gauntlet, Soul Stone and Power Stone resources.
- Satsu: a large set of Iron Man beam/power JSON overrides.
- Omni Evo: targeted tick/resource overlap.
- Shared `minecraft:load` integration resources where applicable.

The historical Satsu/Celestial KubeJS files under `patches/**` are not active runtime scripts in HeroClock itself. Do not simply promote those into the root addon tree: earlier project notes already document duplicate-registration problems from redundant addon-script overlays.

### Generic stock-mod benefit already present in 2.2.15

The current Palladium 4.5.9 mixins are intentionally generic and therefore benefit untouched Palladium addons:

- unchanged immutable scalar property updates can skip redundant sync work;
- the read-only power-holder map wrapper is reused instead of recreated;
- cached command functions are invalidated when the command dispatcher changes.

The timer/work/cleanup systems also provide the internal mechanisms future adapters should use.

### OmniOptimizer 1.8.0 coordination

The supplied OmniOptimizer 1.8.0 JAR exposes `com.openai.omnioptimizer.api.OmniOptimizerAPI` and a companion registration API. It also has optional integrations for several of the same ecosystem mods. HeroClock must coordinate rather than duplicate overlapping responsibilities.

HeroClock currently advertises `deadlines`, `bounded_work`, and `palladium_optimizations` to OmniOptimizer. Re-check this handshake whenever either mod's capability contract changes.

## Recommended adapter direction

Treat each optimization as one of three levels:

1. **Generic runtime optimization** — safe for all compatible callers. Example: equal scalar sync deduplication.
2. **Version-gated addon adapter** — enabled only for a supplied/verified mod profile and redirects a known hotspot through HeroClock deadlines, bounded work, lifecycle cleanup, or caching.
3. **Server-specific patch/reference** — keep under `patches/**` when the behavior depends on a particular server build or cannot be made safe as a transparent runtime adapter.

Do not make stock addons depend on HeroClock for correctness. If an adapter is disabled or the version is unsupported, original addon behavior should continue.

## Known follow-up items

- The current `VersionGuard` profile expectations should be reconciled with the **embedded** versions above, not filename labels. In particular, the supplied AlienEvo, Infinity, Omni Evo, CelestialSapien/MyPowers and Saiyan artifacts report versions different from their filenames.
- Keep investigating only the supplied mod set until new artifacts are explicitly added to scope.
- Do not blindly restore old KubeJS cadence throttles globally. Where historical server patches reduced 20 Hz polling, first determine whether the generic Palladium layer already eliminates the expensive side effect. Add a mod-specific cadence adapter only if runtime testing confirms the behavior remains correct.
- Preserve the released 2.2.14 artifact as a regression reference while developing newer versions.
- Revisit the suspicious Satsu animation resource path and the AlienEvo one-shot cleanup spike during target-pack testing; do not rename/delete behavior-sensitive resources without confirming the owning loader.
- Build/CI results still need an independently observed successful run on the final commit.

## Astra 6 resume checklist

When Astra 6 access is restored:

1. Use the exact supplied stock JAR baselines above first. Do not substitute a newer file silently.
2. Run once without HeroClock to capture baseline server tick percentiles, helper-entity counts, packet/sync behavior and representative client frame times.
3. Run the same scenario with the current HeroClock candidate and no server-patched addon copies.
4. Exercise timer persistence across save/reload, chunk unload, death/respawn and dimension changes.
5. Exercise each installed supplied addon’s normal combat/transform/ability paths, especially helper entities, temporary blocks, lightning/ice, beam powers, property-heavy equipment and datapack reload.
6. Confirm no duplicate Palladium/KubeJS ability registrations or command-function errors.
7. Confirm OmniOptimizer + HeroClock do not perform duplicate cleanup/integration work.
8. Capture any hotspot that remains. Prefer implementing it as a narrow version-gated adapter rather than editing the stock addon JAR.
9. Re-run the same measurement and compare before/after.
10. Update this handoff with the tested Astra 6 pack state, exact JAR hashes, results and any newly supported adapter profiles.

If development moves beyond 2.2.15, update this file before changing branches or version numbers so the next pass can distinguish verified runtime behavior from design intent.

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
| `OmniOptimizer-1.8.0.jar` | `omnioptimizer` | `1.8.0` | `43fee44ddfc8c44df816d74e1dc89075b22237f9d2b4549bf7866e1b9cfc3e1f` |\n| `OmniOptimizer-1.8.1.jar` | `omnioptimizer` | `1.8.0` | `5677f1ae0266e6e29e3162a32713ee8a5cff01dbf5c82025335a5eb184a454ac` |

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

### Initial supplied-JAR hotspot audit

Static inspection of the supplied artifacts identified these concrete runtime patterns. These are observations, not permission to change cadence blindly:

- **AlienEvo**: its root `afomni:tick` function is empty in the supplied artifact, but the pack contains many ability scripts, helper entities, scoreboard-driven resources and temporary block/entity mechanics. The existing HeroClock lifecycle/deadline cleanup remains the safest generic layer; deeper cadence changes need Astra 6 gameplay validation.
- **Infinity**: the supplied `infinity:tick` already throttles maps, stone cleanup and sitting-wolf tagging to once per second, while gauntlet ownership and snap logic remain per tick. Keep its existing targeted HeroClock resources and do not duplicate the already-present throttle.
- **Infintrix**: no global tick function tag was found. It is primarily a Palladium/data-driven target and should benefit from the generic Palladium layer before any bespoke adapter is considered.
- **Satsu Iron Man Addon**: the supplied global tick function is exactly `kill @e[tag=sentinel_kill]`. This is a strong candidate for a future verified-profile adapter: tag/lifecycle-triggered removal can replace a global 20 Hz selector. Do not suppress the original tick function until Astra 6 confirms sentinel death/removal semantics and datapack override behavior.
- **Omni Evo**: the supplied stock tick function repeatedly creates objectives, writes constants, adds a power and decrements recalibration scoreboards. Released HeroClock 2.2.14 already carries a targeted replacement that removes repeated objective creation while preserving the timer behavior. Further timer migration should be treated as a separate adapter and tested against OmniOptimizer overlap.
- **IntoTheOmniverse/AEO**: its tick calls `aeo:anvil_transform` every tick. OmniOptimizer 1.8.0 already ships an optional AEO integration module, so HeroClock should coordinate rather than duplicate that work.
- **CelestialSapien/MyPowers**: its tick function repeatedly runs `superpower add mypowers:dummy @a`. This looks redundant after the power is present, but changing it to join/event-driven behavior needs runtime verification before activation.
- **Powerborne Heroes**: its tick function is narrowly scoped to Herobrine sentinel facing in the mod's void dimension. No broad replacement is justified from static inspection alone.
- **Saiyan**: the supplied `saiyan:tick` function is empty. Its remaining behavior is resource/script driven; no global function optimization is needed from HeroClock at this point.
- **PantheonSent**: no global function tick or KubeJS layer was found in the supplied artifact. Treat it as a generic Palladium/Forge compatibility target unless profiling identifies a concrete hotspot.
- **OmniOptimizer**: already owns optional integrations for overlapping ecosystem mods. HeroClock should advertise capabilities and yield/coordinate rather than implement duplicate fixes.

No new cadence-changing redirect from this list should be promoted solely from static analysis. The current branch keeps generic optimizations active and records the specific candidates for Astra 6 verification.

### OmniOptimizer 1.8.x coordination

The supplied OmniOptimizer 1.8.0 JAR exposes `com.openai.omnioptimizer.api.OmniOptimizerAPI` and a companion registration API. It also has optional integrations for several of the same ecosystem mods. HeroClock must coordinate rather than duplicate overlapping responsibilities.

HeroClock currently advertises `deadlines`, `bounded_work`, and `palladium_optimizations` to OmniOptimizer. Re-check this handshake whenever either mod's capability contract changes.

## Recommended adapter direction

Treat each optimization as one of three levels:

1. **Generic runtime optimization** — safe for all compatible callers. Example: equal scalar sync deduplication.
2. **Version-gated addon adapter** — enabled only for a supplied/verified mod profile and redirects a known hotspot through HeroClock deadlines, bounded work, lifecycle cleanup, or caching.
3. **Server-specific patch/reference** — keep under `patches/**` when the behavior depends on a particular server build or cannot be made safe as a transparent runtime adapter.

Do not make stock addons depend on HeroClock for correctness. If an adapter is disabled or the version is unsupported, original addon behavior should continue.

## Known follow-up items

- `VersionGuard` uses the **embedded** versions above and exposes `isVerifiedProfile(modId)` for future version-gated adapters. OmniOptimizer is a special case: both supplied 1.8.0 and filename-labeled 1.8.1 artifacts advertise embedded version `1.8.0`, so artifact-level changes must be tracked by hash/API inspection rather than Forge version metadata alone.
- Keep investigating only the supplied mod set until new artifacts are explicitly added to scope.
- Do not blindly restore old KubeJS cadence throttles globally. Where historical server patches reduced 20 Hz polling, first determine whether the generic Palladium layer already eliminates the expensive side effect. Add a mod-specific cadence adapter only if runtime testing confirms the behavior remains correct.
- Preserve the released 2.2.14 artifact as a regression reference while developing newer versions.
- Revisit the suspicious Satsu animation resource path and the AlienEvo one-shot cleanup spike during target-pack testing; do not rename/delete behavior-sensitive resources without confirming the owning loader.
- Build/CI results still need an independently observed successful run on the final commit.

## 2026-09-21 real-pack validation checkpoint

Test build: `HeroClock-2.2.15-dev-6f57c0b.jar` from commit `6f57c0b7c2f2292c66052399f602d5a65087fbfb`.

The exact GitHub Actions build for this commit passed Gradle tests/build, base Forge GameTests and the Palladium 4.5.9 runtime tests before the JAR was tested in the pack.

A roughly 16m20s Spark Java-engine profile was captured in the real integrated-server pack. Important steady-state observations:

- Spark's ending statistics were effectively 20 TPS over the last 1 and 5 minutes. Last-1m MSPT was about 17.50 ms mean / 15.45 ms median / 24.37 ms p95. Last-5m MSPT was about 19.39 ms mean / 16.76 ms median / 33.46 ms p95. The profile contained isolated large spikes, so this is a checkpoint rather than a final benchmark.
- HeroClock itself was not a meaningful hot path in this sample. Summed exclusive HeroClock-source samples inside the active integrated-server tick subtree were about 228 ms, roughly 0.05% of that sampled tick subtree. `ServerRuntime.tick` and `TemporaryEntities` were individually tiny.
- The actionable hot area is Palladium + Rhino/KubeJS execution. Palladium's power/ability tick path accounted for a large share of active tick samples. `PropertyManager.getPropertyByName` accumulated about 19.7 s of exclusive sampled time across occurrences; condition evaluation and scriptable condition/ability paths were also substantial.
- Rhino carried about 16% of exclusive sampled active-tick work and Palladium about 7.7%. Interpreted addon scripts appear under Rhino rather than under each addon JAR's source name, so this profile alone cannot safely attribute all Rhino time to Satsu, Omni Evo, MyPowers, etc.
- The safest first per-mod redirect remains Satsu's verified `sentinel_kill` global selector -> lifecycle-triggered cleanup. The profile also justifies investigating a generic/version-gated Palladium property-name lookup/cache optimization, which may yield more than a single addon redirect.
- Do not globally throttle ScriptableCondition/ScriptableAbility callbacks from this profile. Add diagnostics or narrow adapters first so gameplay-sensitive 20 Hz behavior is not changed blindly.

Log interpretation matters for this capture:

- Severe `Can't keep up` messages occurred during world startup before the Spark capture settled.
- Two low-TPS windows inside the capture align with explicit singleplayer `Saving and pausing game...` events across many dimensions; do not treat those windows as steady-state server load.
- HeroClock 2.2.15 loaded successfully and no HeroClock mixin application crash was observed in the supplied logs.
- The pack had updated OmniOptimizer from 1.8.0 to the filename-labeled **1.8.1** artifact before this run. That exact JAR was subsequently supplied and hash-verified. Its HeroClock handshake failed because OmniOptimizer uses `Class.getMethod(\"handshake\")` while HeroClock's bridge was package-private; the branch now fixes that visibility contract. The separate OmniOptimizer optional integration-pack registration failure remains an OmniOptimizer-side issue.
- `minecraft_mobs_pack` advancement/resource errors are present. The failing HeroClock-tagged advancement chain is inherited from the released 2.2.14 compatibility resources, while the log also reports missing `minecraft_mobs` item IDs. Treat this as stale/missing external compatibility content rather than evidence that the new 2.2.15 Java runtime introduced the failure.

This checkpoint is the before/after reference for the next adapter build. Keep exact behavior changes isolated so future Spark captures can show whether each redirect actually reduces the Palladium/Rhino hot paths.

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

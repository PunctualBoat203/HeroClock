# HeroClock — Development Backup & Handoff

HeroClock is a Minecraft **Forge 1.20.1** performance, stability, timing, and compatibility framework for complex superhero modpacks.

## Backup status

This repository was created as a durable handoff/backup on 2026-09-20.

Backed-up build line:
- **2.2.10** — last build explicitly confirmed in-game by the user to launch cleanly and pass the core persistence tests.
- **2.2.12** — later deep-audited build.
- **2.2.14** — latest development JAR preserved from the workspace; treat as the latest candidate snapshot rather than claiming the same runtime verification as 2.2.10.

SHA-256:
- 2.2.10: `4d1b7ee500ca46caadee407a23a3e4815ff230f91247fef0eccd4e43420ad53f`
- 2.2.12: `818f7612de17512a77b29f7e3487ebe33e70a9c56424130f62e1da2d58bb27df`
- 2.2.14: `68549ce622e07d0b2b8c53086ad1b8e240de67a636c84d492a20d97a7cef9897`

## Core design decisions

- Shared timing API intended to reduce scoreboard-as-clock abuse. Scoreboards remain valid for actual gameplay state.
- Failure strategy: **prevent first → targeted cleanup failsafe → restart/chunk-load recovery**.
- Avoid giant global `@e` scans.
- Compatibility integrations should be optional; HeroClock should not require hero mods just to load.
- Minecraft 1.20.1 / Forge 47.x.
- OmniOptimizer integration is optional.
- Avoid duplicate active KubeJS overlay scripts because Palladium/addon loaders can execute both copies.

## Work completed across the project

The development line covered timer/deadline APIs, persistence/recovery guards, optional companion integration, version/compatibility guards, and prepatched compatibility resources for superhero addons. Later JARs contain compatibility resources for multiple addon namespaces, including AlienEvo, Infinity/Power Stone content, Omni Evo, Minecraft Mobs Pack, and Satsu Iron Man content.

The 2.2.14 JAR reports itself as loading the timer API plus guarded compatibility layer. Runtime JAR rewriting is disabled in that build because it is a prepatched distribution.

The optional OmniOptimizer companion bridge is reflection-based so OmniOptimizer is not a hard dependency.

## Important historical testing

The strongest confirmed runtime checkpoint is **2.2.10**: it launched without issues and the core persistence tests worked.

2.2.12 was subsequently deep-audited. 2.2.14 is the newest preserved artifact and contains the latest compatibility/resource state available in this workspace.

Do not silently call 2.2.14 “fully tested” unless it is re-tested in the target pack.

## Source recovery note

The original editable Java source tree for 2.2.14 was not present in the current workspace when this backup repository was created. To avoid losing the implementation state, this repo includes a **recovered 2.2.14 source/resource snapshot** made directly from the authoritative JAR:
- complete unpacked JAR contents/resources;
- compiled HeroClock classes;
- `javap -p -c` output for the HeroClock Java classes.

That recovered snapshot is useful for reconstruction and auditing, but it is **not claimed to be byte-for-byte original Java source**.

## Recommended continuation

1. Keep 2.2.10 as the known-good rollback build.
2. Treat 2.2.14 as the latest development baseline.
3. Reconstruct/restore a normal Gradle Java source tree from the recovered snapshot before substantial new Java changes.
4. Re-test persistence/restart/chunk-load behavior after reconstruction.
5. Keep compatibility patches targeted and optional.
6. Preserve the no-global-scan performance rule.

## Files

- `releases/HeroClock-2.2.10-forge-1.20.1.jar` — confirmed working rollback.
- `releases/HeroClock-2.2.12-forge-1.20.1.jar` — audited intermediate.
- `releases/HeroClock-2.2.14-forge-1.20.1.jar` — latest preserved candidate.
- `source-backup/HeroClock-2.2.14-source-snapshot.zip` — recovered source/resource snapshot.

## Version target

If development resumes, increment from **2.2.14** rather than overwriting an existing build.

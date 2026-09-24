# Recovery provenance

## Original reconstruction baseline

The editable 2.2.15 source tree was originally reconstructed from the supplied **HeroClock 2.2.10** JAR.

SHA-256:

```
4d1b7ee500ca46caadee407a23a3e4815ff230f91247fef0eccd4e43420ad53f
```

Java was reconstructed with CFR 0.152 and resources were extracted unchanged. At the time of that recovery, the remote repository did not contain the 2.2.12/2.2.14 binaries or the source snapshot referenced by its earlier README, and no GitHub releases containing those artifacts were available. Development therefore moved to version 2.2.15 rather than reusing a historical number.

## Released 2.2.14 artifact recovered later

The actual released **HeroClock-2.2.14-forge-1.20.1.jar** was later supplied directly and verified.

SHA-256:

```
68549ce622e07d0b2b8c53086ad1b8e240de67a636c84d492a20d97a7cef9897
```

This exactly matches the 2.2.14 hash already recorded in the project history. The binary contains the 2.2.14 Forge entrypoint, timer API, guarded compatibility layer, active compatibility resources, and inert historical patch payloads under `patches/`.

The 2.2.14 binary is now the **release-behavior and regression reference** for the 2.2.15 branch. It does not replace the recovered editable source provenance: the current source was still reconstructed from 2.2.10 and then developed forward.

Important architectural finding from the recovered 2.2.14 binary: its historical `patches/**` payloads are reference/server-patching material and are not a runtime JAR-rewriter. Future optimization should prefer stock addon JARs plus HeroClock runtime hooks/adapters rather than requiring patched addon distributions.

See [HANDOFF.md](HANDOFF.md) for the exact supplied addon baselines and future-resume notes.

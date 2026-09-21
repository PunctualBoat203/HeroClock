# Recovered Java API surface — HeroClock 2.2.14

Recovered from the compiled 2.2.14 JAR with `javap`. This documents the callable surface while the original editable source tree is being reconstructed.

```java
package com.heroclock.api;

public final class HeroClockAPI {
    public static long now(Object target);
    public static long set(Object target, String key, long ticks);
    public static long setSeconds(Object target, String key, double seconds);
    public static long setDeadline(Object target, String key, long deadline);
    public static long add(Object target, String key, long ticks);
    public static long remaining(Object target, String key);
    public static double remainingSeconds(Object target, String key);
    public static boolean active(Object target, String key);
    public static boolean expired(Object target, String key);
    public static long deadline(Object target, String key);
    public static void clear(Object target, String key);
    public static void clearAll(Object target);
}
```

Other preserved compiled classes:
```
com.heroclock.HeroClock
com.heroclock.CompanionBridge
com.heroclock.PatchManager
com.heroclock.VersionGuard
```

Implementation facts recovered from bytecode:
- HeroClock startup calls `VersionGuard.scan()`, `PatchManager.apply()`, then `CompanionBridge.handshake()`.
- PatchManager reports that runtime JAR rewriting is disabled for the prepatched distribution.
- CompanionBridge discovers OmniOptimizer by reflection rather than a hard Java linkage.
- VersionGuard scans installed mod JAR metadata and warns on unsupported versions.

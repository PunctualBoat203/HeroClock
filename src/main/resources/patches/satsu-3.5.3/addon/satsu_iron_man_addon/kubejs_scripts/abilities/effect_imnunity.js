StartupEvents.registry("palladium:abilities", (event) => {
  event
    .create("satsu_iron_man_addon:effect_immunity")
    .icon(palladium.createItemIcon("minecraft:diamond"))
    .addProperty(
      "effect",
      "string",
      "minecraft:poison",
      "The effect the entity is immune to"
    )
    .tick((entity, entry, holder, enabled) => {
      if (!enabled) return;
      // SAFEOPT-BALANCED: max 0.5s cleanup latency; avoids 90% of polling work.
      if (entity.age % 10 !== 0) return;

      const effect = entry.getPropertyByName("effect");
      if (entity.hasEffect(effect)) {
        entity.removeEffect(effect);
      }
    });
});
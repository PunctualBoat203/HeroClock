global.sound = (entity, soundId, volume, pitch, shift) => {
    entity.level.runCommandSilent(`playsound ${soundId} player @p ${entity.x} ${entity.y} ${entity.z} ${volume} ${pitch - shift/2 + Math.random()*shift}`);
};

global.applyKnockback = (entity, target, strength) => {
    let dx = target.x - entity.x;
    let dz = target.z - entity.z;
    let distance = Math.sqrt(dx * dx + dz * dz);

    if (distance > 0) {
        let multiplier = strength / distance;
        target.addMotion(
            dx * multiplier,
            0.4,
            dz * multiplier
        );
    }
};

// HeroClock integration: event-driven cooldown deadline with original persistentData fallback.
// If HeroClock is absent/incompatible, behavior falls back to the addon's original implementation.
let $HeroClock = null;
try { $HeroClock = Java.loadClass('com.heroclock.api.HeroClockAPI'); } catch (ignored) {}
const HERO_CLOCK_KEY = 'omni_evo.recal_failsafe';

function getCooldownTime(entity) {
    if ($HeroClock) {
        try { return Number($HeroClock.deadline(entity, HERO_CLOCK_KEY)); } catch (ignored) {}
    }
    return entity.persistentData.getInt('totemCooldown') || 0;
}

function setCooldownTime(entity, time) {
    if ($HeroClock) {
        try {
            $HeroClock.setDeadline(entity, HERO_CLOCK_KEY, Number(time));
            return;
        } catch (ignored) {}
    }
    entity.persistentData.putInt('totemCooldown', time);
}

function isOnCooldown(entity) {
    if ($HeroClock) {
        try { return $HeroClock.active(entity, HERO_CLOCK_KEY); } catch (ignored) {}
    }
    let currentTime = entity.level.time;
    let cooldownTime = getCooldownTime(entity);
    return currentTime < cooldownTime;
}

function getRemainingCooldown(entity) {
    if ($HeroClock) {
        try { return Math.max(0, Math.ceil(Number($HeroClock.remaining(entity, HERO_CLOCK_KEY)) / 20)); } catch (ignored) {}
    }
    let currentTime = entity.level.time;
    let cooldownTime = getCooldownTime(entity);
    let remainingTicks = cooldownTime - currentTime;
    return Math.max(0, Math.ceil(remainingTicks / 20));
}


let users = ['minecraft:player'];

users.forEach(key => {
    EntityEvents.death(key, e=> {
        let bypasses = ['outOfWorld'];
        let knockbackRadius = 10;
        let knockbackStrength = 3;

        let {entity, source} = e;

        if (isOnCooldown(entity)) {
            return;
        }

        if (!abilityUtil.hasPower(entity, "alienevo:recal_omnitrix")) {
            return;
        }

        if (!palladium.abilities.isUnlocked(entity, new ResourceLocation("alienevo:recal_omnitrix"), "failsafe_unlock")) {
            return;
        }

        if (bypasses.includes(source.type)) {
            return;
        }

        let cooldownDuration = 18000;
        setCooldownTime(entity, entity.level.time + cooldownDuration);

        palladium.scoreboard.setScore(entity, "AlienEvo.Timer", 2);

        let box = AABB.ofSize(entity.position(), knockbackRadius * 2, knockbackRadius * 2, knockbackRadius * 2);
        let nearbyEntities = entity.level.getEntities(entity, box);

        nearbyEntities.forEach(target => {
            if (target !== entity && target.type !== "minecraft:item" &&
                target.type !== "minecraft:item_frame" &&
                target.type !== "minecraft:glow_item_frame" &&
                target.type !== "minecraft:armor_stand") {
                global.applyKnockback(entity, target, knockbackStrength);
            }
        });

        superpowerUtil.addSuperpower(entity, new ResourceLocation(`alienevo:transform_bubble`));
        global.sound(entity, 'item.totem.use', 0.3, 1.8, 0.2);
        global.sound(entity, 'alienevo:prototype_failsafe', 1.0, 1.0, 0.0);
        entity.extinguish()
        entity.setHealth(1.0);
        e.cancel();
    })
});
# Lightning Rod leftovers are invalid in HeroClock's impact-driven implementation.
kill @e[type=palladium:custom_projectile,tag=lightning]

# Overworld DNA temporary ice/web markers.
scoreboard players add @e[tag=ice_timer] iceTime 20
scoreboard players add @e[tag=blue_ice_timer] iceTime 20
execute as @e[tag=ice_timer,scores={iceTime=200..}] at @s run fill ~-7 ~-7 ~-7 ~7 ~7 ~7 minecraft:air replace minecraft:blue_ice
execute as @e[tag=blue_ice_timer,scores={iceTime=400..}] at @s run fill ~-15 ~-15 ~-15 ~15 ~15 ~15 minecraft:air replace minecraft:blue_ice
kill @e[tag=ice_timer,scores={iceTime=200..}]
kill @e[tag=blue_ice_timer,scores={iceTime=400..}]
scoreboard players add @e[tag=web_timer] hcTempAge 20
execute as @e[tag=web_timer,scores={hcTempAge=400..}] at @s run fill ~-1 ~ ~-1 ~1 ~2 ~1 minecraft:air replace minecraft:cobweb
kill @e[tag=web_timer,scores={hcTempAge=400..}]

# AlienEvo/AFOmni temporary ice now ages independently of a transformed player.
scoreboard players add @e[tag=a.ice] afomni.lifetime 20
execute as @e[tag=a.ice,scores={afomni.lifetime=500..}] at @s run function afomni:iceberg/break_ice

# Conservative orphan guards. Normal ability cleanup should remove these first.
scoreboard players add @e[tag=IceSpikes] hcTempAge 20
scoreboard players add @e[tag=ice_spike] hcTempAge 20
scoreboard players add @e[tag=ice_spike_target] hcTempAge 20
scoreboard players add @e[tag=earth_spike] hcTempAge 20
scoreboard players add @e[tag=earth_spike_target] hcTempAge 20
scoreboard players add @e[tag=diamond_spike] hcTempAge 20
kill @e[tag=IceSpikes,scores={hcTempAge=200..}]
kill @e[tag=ice_spike,scores={hcTempAge=200..}]
kill @e[tag=ice_spike_target,scores={hcTempAge=200..}]
kill @e[tag=earth_spike,scores={hcTempAge=200..}]
kill @e[tag=earth_spike_target,scores={hcTempAge=200..}]
kill @e[tag=diamond_spike,scores={hcTempAge=200..}]

scoreboard players add @e[tag=a.iceberg] hcTempAge 20
scoreboard players add @e[tag=a.flying_block] hcTempAge 20
scoreboard players add @e[tag=a.slime_rain] hcTempAge 20
scoreboard players add @e[tag=slime_puddle] hcTempAge 20
scoreboard players add @e[tag=a.root] hcTempAge 20
scoreboard players add @e[tag=a.branch] hcTempAge 20
scoreboard players add @e[tag=a.tree] hcTempAge 20
scoreboard players add @e[tag=drago.ball] hcTempAge 20
scoreboard players add @e[tag=sound] hcTempAge 20
kill @e[tag=a.iceberg,scores={hcTempAge=600..}]
kill @e[tag=a.flying_block,scores={hcTempAge=600..}]
kill @e[tag=a.slime_rain,scores={hcTempAge=600..}]
kill @e[tag=slime_puddle,scores={hcTempAge=400..}]
kill @e[tag=a.root,scores={hcTempAge=600..}]
kill @e[tag=a.branch,scores={hcTempAge=600..}]
kill @e[tag=a.tree,scores={hcTempAge=600..}]
kill @e[tag=drago.ball,scores={hcTempAge=200..}]
kill @e[tag=sound,scores={hcTempAge=400..}]

# Powerborne Sentry shadow field: its JS tracking set is memory-only, so recover cleanup after restart.
execute as @e[type=minecraft:area_effect_cloud,tag=powerborne.shadow_field_marker] store result score @s hcTempAge run data get entity @s Age 1
execute as @e[type=minecraft:area_effect_cloud,tag=powerborne.shadow_field_marker,scores={hcTempAge=1160..}] at @s run function heroclock:shadow_field_cleanup
kill @e[type=minecraft:area_effect_cloud,tag=powerborne.shadow_field_marker,scores={hcTempAge=1160..}]

schedule function heroclock:persistence_guard 1s replace

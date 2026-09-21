# Safe player-only snap: each non-snapper player has the original 50% predicate applied.
tag @s add snapper
execute at @s run particle flash ~ ~1 ~ 0 0 0 0 1 force
execute at @s run playsound entity.firework_rocket.blast master @a ~ ~ ~ 1 2
execute as @a[tag=!snapper] if predicate infinity:true_random run kill @s
tag @s remove snapper

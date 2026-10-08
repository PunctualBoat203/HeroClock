#Mark the ray as having found a block.

scoreboard players set #hit afomni.IceRay 1

#Running custom commands since the block was found.

setblock ~ ~ ~ afomni:ice
playsound minecraft:block.glass.break block @a[distance=..20] ~ ~ ~ 0.75 1.4 0.025
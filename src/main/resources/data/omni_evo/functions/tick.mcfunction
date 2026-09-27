scoreboard players set @a Recal.Base 1
scoreboard players set @a Recal.Base2 2
scoreboard players set @a Recal.Max 5500
superpower add omni_evo:recal_ability @a
execute as @a if score @s Recaling.Timer = @s Recal.Base run stopsound @s * alienevo:first_recal
execute as @a if score @s Recaling.Timer = @s Recal.Base run superpower add alienevo:recal_omnitrix
execute as @a if score @s Recaling.Timer = @s Recal.Base run tag @s add recalibrated_once
execute as @a if score @s Recaling.Timer = @s Recal.Base run alienautoadd @s aeroflame:aerophibian
execute as @a if score @s Recaling.Timer = @s Recal.Base run alienautoadd @s aeroflame:methanosian
execute as @a if score @s Recaling.Timer = @s Recal.Base run alienautoadd @s chromastone:crystalsapien
execute as @a if score @s Recaling.Timer = @s Recal.Base run alienautoadd @s omni_evo_aliens:rath
execute as @a if score @s Recaling.Timer = @s Recal.Base run alienautoadd @s omni_evo_aliens:goop
execute as @a if score @s Recaling.Timer = @s Recal.Base2 if entity @s[tag=AlienEvo.Transformation] run kill
execute as @a if score @s Recaling.Timer = @s Recal.Base run scoreboard players set @s AlienEvo.Timer 2
execute as @a if score @s Recaling.Timer <= @s Recal.Base run scoreboard players set @s Recaling.Timer 1 
execute as @a if score @s Recaling.Timer <= @s Recal.Max run scoreboard players remove @s Recaling.Timer 1
execute as @a if score @s Recaling.Timer_Extra <= @s Recal.Base run scoreboard players set @s Recaling.Timer_Extra 1 
execute as @a if score @s Recaling.Timer_Extra <= @s Recal.Max run scoreboard players remove @s Recaling.Timer_Extra 1
execute as @a if score @s Recaling.Timer_Extra_Extra <= @s Recal.Base run scoreboard players set @s Recaling.Timer_Extra_Extra 1 
execute as @a if score @s Recaling.Timer_Extra_Extra <= @s Recal.Max run scoreboard players remove @s Recaling.Timer_Extra_Extra 1
execute as @a if score @s Recaling.Timer = @s Recal.Base2 run scoreboard players set @s AlienEvo.Tubes 1


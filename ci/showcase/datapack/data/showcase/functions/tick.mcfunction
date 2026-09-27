execute unless score started showcase matches 1 if entity @a run scoreboard players set t showcase 0
execute unless score started showcase matches 1 if entity @a run scoreboard players set started showcase 1
scoreboard players add t showcase 1
gamemode spectator @a[gamemode=!spectator]
execute if score t showcase matches ..399 run tp @a 0.5 -41 -6 0 28
execute if score t showcase matches 400..799 run tp @a 41 -43.5 -1 0 16
execute if score t showcase matches 800..1199 run tp @a -46 -33 -10 0 32
execute if score t showcase matches 1200 run weather rain 1000000
execute if score t showcase matches 1200..1599 run tp @a -46 -33 -10 0 32
execute if score t showcase matches 1600.. run tp @a 0.5 -41 -6 0 28

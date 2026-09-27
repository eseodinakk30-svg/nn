execute unless score started showcase matches 1 if entity @a run scoreboard players set t showcase 0
execute unless score started showcase matches 1 if entity @a run scoreboard players set started showcase 1
scoreboard players add t showcase 1
gamemode spectator @a[gamemode=!spectator]
execute if score t showcase matches ..399 run tp @a 0.5 -41 -6 0 28
execute if score t showcase matches 400..799 run tp @a 41 -43.5 -1 0 16
execute if score t showcase matches 800..1199 run tp @a -46 -33 -10 0 32
execute if score t showcase matches 1200 run weather rain 1000000
execute if score t showcase matches 1200..1599 run tp @a -46 -33 -10 0 32
execute if score t showcase matches 200 run place structure dunesrelics:volcano -200 -47 0
execute if score t showcase matches 1600..1999 run tp @a 0.5 -41 -6 0 28
execute if score t showcase matches 2000 run weather clear 1000000
execute if score t showcase matches 2000..2399 run tp @a 0.5 -41 -46 0 28
execute if score t showcase matches 2400..2799 run tp @a 42 -43 -42 0 14
execute if score t showcase matches 2800..3199 run tp @a -122 -14 8.5 90 10
execute if score t showcase matches 3200 run effect give @a minecraft:night_vision infinite 0 true
execute if score t showcase matches 3200..3599 run tp @a -194.5 -52 8.5 90 32
execute if score t showcase matches 3600 run effect clear @a minecraft:night_vision
execute if score t showcase matches 3600 run weather thunder 1000000
execute if score t showcase matches 3600.. run tp @a 0.5 -40 -48 0 22

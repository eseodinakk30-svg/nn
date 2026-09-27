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
execute if score t showcase matches 3200..3599 run tp @a -194.5 -49.5 8.5 90 30
execute if score t showcase matches 3600 run effect clear @a minecraft:night_vision
execute if score t showcase matches 3600 run weather thunder 1000000
execute if score t showcase matches 3600..3999 run tp @a 0.5 -40 -48 0 22
execute if score t showcase matches 4000 run weather clear 1000000
execute if score t showcase matches 4000..4399 run tp @a -48 -42 -88 -43 16
execute if score t showcase matches 4400 positioned -80 -47 -60 store success score hamlet showcase run livingworld hamlet
execute if score t showcase matches 4402 run summon dunesrelics:lumberjack -76 -47 -50 {HomeBell:{X:-77,Y:-46,Z:-53}}
execute if score t showcase matches 4402 run summon dunesrelics:quarryman -78 -47 -50 {HomeBell:{X:-77,Y:-46,Z:-53}}
execute if score t showcase matches 4401 if score hamlet showcase matches 1 run say showcase_result hamlet_founded
execute if score t showcase matches 4401 unless score hamlet showcase matches 1 run say showcase_result hamlet_failed
execute if score t showcase matches 4400..4799 run tp @a 0.5 -41 -78 0 28
execute if score t showcase matches 4800 run time set 18000
execute if score t showcase matches 4800 run effect give @a minecraft:night_vision infinite 0 true
execute if score t showcase matches 4800 run summon dunesrelics:shade 48.5 -47 -70.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}
execute if score t showcase matches 4800 run fill 212 -47 -228 228 -40 -212 minecraft:stone_bricks hollow
execute if score t showcase matches 4800 positioned 220 -47 -220 run livingworld stage 4
execute if score t showcase matches 4801 positioned 220 -47 -220 run livingworld react 12
execute if score t showcase matches 4800..5199 run tp @a 40.5 -43 -83 0 14
execute if score t showcase matches 4810 run summon dunesrelics:pirate_sloop 31 -48.4 -111 {Home:{X:31,Y:-48,Z:-111},Rotation:[60f,0f]}
execute if score t showcase matches 5200 run time set 5000
execute if score t showcase matches 5200 run effect clear @a minecraft:night_vision
execute if score t showcase matches 5200..5599 run tp @a -72 -42 -40 173 17
execute if score t showcase matches 5600..5999 run tp @a 250 -5 -250 61 30
execute if score t showcase matches 6000.. run tp @a 44 -41 -84 150 18
execute if score t showcase matches 300 run say showcase_shot 01_blocks
execute if score t showcase matches 700 run say showcase_shot 02_mobs
execute if score t showcase matches 1100 run say showcase_shot 03_ruins_oasis
execute if score t showcase matches 1500 run say showcase_shot 04_sandstorm
execute if score t showcase matches 1900 run say showcase_shot 05_sandstorm_blocks
execute if score t showcase matches 2300 run say showcase_shot 06_volcanic_blocks
execute if score t showcase matches 2700 run say showcase_shot 07_volcanic_mobs
execute if score t showcase matches 3100 run say showcase_shot 08_volcano
execute if score t showcase matches 3500 run say showcase_shot 09_magma_chamber
execute if score t showcase matches 3900 run say showcase_shot 10_ashfall_eruption
execute if score t showcase matches 4300 run say showcase_shot 11_mill
execute if score t showcase matches 4700 run say showcase_shot 12_living_blocks
execute if score t showcase matches 5100 run say showcase_shot 13_living_mobs
execute if score t showcase matches 5150 positioned -77 -46 -53 run livingworld village
execute if score t showcase matches 5500 run say showcase_shot 14_hamlet
execute if score t showcase matches 5900 run say showcase_shot 15_world_memory
execute if score t showcase matches 6300 run say showcase_shot 16_pirate_sloop

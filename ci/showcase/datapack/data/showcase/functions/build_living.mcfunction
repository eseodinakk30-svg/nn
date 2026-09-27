# ---- 11: an undershot water wheel in a stream turning a millstone; a cistern and troughs watering a wheat field
fill -46 -48 -78 -38 -48 -78 minecraft:air
setblock -46 -48 -78 minecraft:water
setblock -40 -47 -78 dunesrelics:water_wheel[axis=z]
setblock -40 -47 -77 dunesrelics:millstone{Items:[{Slot:0b,id:"minecraft:wheat",Count:64b}]}
setblock -40 -46 -77 minecraft:hopper[facing=down]{Items:[{Slot:0b,id:"minecraft:wheat",Count:32b}]}
setblock -39 -47 -77 minecraft:barrel[facing=up]
setblock -31 -47 -72 minecraft:stone_bricks
setblock -32 -47 -73 minecraft:stone_bricks
setblock -32 -47 -71 minecraft:stone_bricks
setblock -31 -47 -73 minecraft:stone_brick_wall
setblock -31 -47 -71 minecraft:stone_brick_wall
setblock -32 -47 -72 minecraft:water
fill -37 -47 -72 -33 -47 -72 dunesrelics:water_trough
fill -37 -48 -74 -33 -48 -73 minecraft:farmland[moisture=7]
fill -37 -47 -74 -33 -47 -73 minecraft:wheat[age=7]
fill -37 -48 -71 -33 -48 -70 minecraft:farmland[moisture=7]
fill -37 -47 -71 -35 -47 -70 minecraft:wheat[age=5]
fill -34 -47 -71 -33 -47 -70 minecraft:carrots[age=7]

# ---- 12: the new blocks, and the new items in frames
setblock -1 -47 -66 dunesrelics:wet_sand
setblock 0 -47 -66 dunesrelics:seashell[variant=0]
setblock 1 -47 -66 dunesrelics:seashell[variant=1]
setblock 2 -47 -66 dunesrelics:seashell[variant=2]
setblock 3 -47 -66 dunesrelics:seashell[variant=3]
setblock 4 -47 -66 dunesrelics:clam
setblock 6 -47 -66 dunesrelics:millstone
setblock 7 -47 -66 dunesrelics:water_trough
setblock 8 -47 -66 dunesrelics:water_trough
setblock 11 -46 -66 dunesrelics:water_wheel[axis=z]
setblock 14 -47 -66 dunesrelics:cannon[facing=north,loaded=true]
setblock 16 -47 -65 minecraft:dark_oak_fence
setblock 16 -46 -65 minecraft:dark_oak_fence
setblock 16 -45 -65 minecraft:dark_oak_fence
setblock 16 -44 -65 minecraft:dark_oak_fence
setblock 16 -44 -66 minecraft:dark_oak_fence
setblock 16 -45 -66 dunesrelics:dreamcatcher
fill -12 -47 -63 -3 -44 -63 minecraft:spruce_planks
summon item_frame -12 -45 -64 {Facing:2b,Fixed:1b,Item:{id:"dunesrelics:flour",Count:1b}}
summon item_frame -11 -45 -64 {Facing:2b,Fixed:1b,Item:{id:"dunesrelics:dough",Count:1b}}
summon item_frame -10 -45 -64 {Facing:2b,Fixed:1b,Item:{id:"dunesrelics:pearl",Count:1b}}
summon item_frame -9 -45 -64 {Facing:2b,Fixed:1b,Item:{id:"dunesrelics:message_in_a_bottle",Count:1b}}
summon item_frame -8 -45 -64 {Facing:2b,Fixed:1b,Item:{id:"dunesrelics:tide_clock",Count:1b}}
summon item_frame -7 -45 -64 {Facing:2b,Fixed:1b,Item:{id:"dunesrelics:world_chronicle",Count:1b}}
summon item_frame -6 -45 -64 {Facing:2b,Fixed:1b,Item:{id:"dunesrelics:cannonball",Count:1b}}
summon item_frame -5 -45 -64 {Facing:2b,Fixed:1b,Item:{id:"dunesrelics:gloom_dust",Count:1b}}
summon item_frame -4 -45 -64 {Facing:2b,Fixed:1b,Item:{id:"dunesrelics:cutlass",Count:1b}}
summon item_frame -3 -45 -64 {Facing:2b,Fixed:1b,Item:{id:"dunesrelics:captain_hat",Count:1b}}
summon armor_stand -7.5 -47 -65.5 {ShowArms:1b,NoBasePlate:1b,Invulnerable:1b,Rotation:[180f,0f],ArmorItems:[{},{},{id:"minecraft:leather_chestplate",Count:1b,tag:{Trim:{material:"dunesrelics:pearl",pattern:"minecraft:coast"}}},{id:"dunesrelics:captain_hat",Count:1b}],HandItems:[{id:"dunesrelics:cutlass",Count:1b},{id:"dunesrelics:tide_clock",Count:1b}]}

# ---- 13: the new mobs (the Shade is summoned at night by the tick function)
summon dunesrelics:pirate 32.5 -47 -70.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],HandItems:[{id:"dunesrelics:cutlass",Count:1b},{}]}
summon dunesrelics:pirate_gunner 36.5 -47 -70.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],HandItems:[{id:"minecraft:crossbow",Count:1b},{}]}
summon dunesrelics:pirate_captain 40.5 -47 -70.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f],HandItems:[{id:"dunesrelics:cutlass",Count:1b},{}]}
summon dunesrelics:traveler 44.5 -47 -70.5 {NoAI:1b,PersistenceRequired:1b,Rotation:[180f,0f]}
setblock 40 -47 -67 dunesrelics:cannon[facing=north,loaded=true]
setblock 36 -47 -67 minecraft:barrel[facing=up]
setblock 37 -47 -67 minecraft:barrel[facing=up]

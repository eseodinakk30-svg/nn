#!/usr/bin/env bash
# Living world visit: generates a normal world, finds a pirate ship and a village with /locate, checks the ship's crew,
# then plays a client visit (boarding the ship, the village growing and a pirate landing, the world reacting to a
# keep built near spawn, and a night) and saves screenshots, errors and any crash report to ci/living.
# Used by the CI "living" step.
set -uo pipefail
cd "$(dirname "$0")/.."
OUT=ci/living
mkdir -p run "$OUT"
rm -rf "$OUT"/*

# 1. A dedicated server generates the world; RCON finds the ship and a village, and counts the ship's crew.
echo "eula=true" > run/eula.txt
cat > run/server.properties <<PROPS
level-name=world
level-seed=${LIVING_SEED:-8412}
level-type=minecraft:normal
online-mode=false
spawn-protection=0
enable-rcon=true
rcon.port=25575
rcon.password=soak
PROPS
rm -rf run/world
./gradlew runServer --no-daemon > server.log 2>&1 &
for i in $(seq 1 240); do
  grep -q "Done (" server.log && break
  grep -q "Exception in server tick loop\|Failed to start the minecraft server" server.log && break
  sleep 2
done
python3 ci/rcon.py "locate structure dunesrelics:pirate_ship" | tee "$OUT/locate.txt"
python3 ci/rcon.py "locate structure #minecraft:village" | tee -a "$OUT/locate.txt"
read -r SX SZ VX VZ < <(python3 - <<'PY'
import re
found = re.findall(r"\[(-?\d+), ~, (-?\d+)\]", open("ci/living/locate.txt").read())
ship = found[0] if len(found) > 0 else ("0", "0")
village = found[1] if len(found) > 1 else ("0", "0")
print(*ship, *village)
PY
)
echo "ship: $SX $SZ  village: $VX $VZ" | tee -a "$OUT/locate.txt"
python3 ci/rcon.py "forceload add $((SX - 32)) $((SZ - 32)) $((SX + 32)) $((SZ + 32))" > /dev/null
sleep 20
for type in pirate pirate_gunner pirate_captain; do
  echo "$type: $(python3 ci/rcon.py "execute if entity @e[type=dunesrelics:$type,x=$SX,y=64,z=$SZ,distance=..64]")" \
    | tee -a "$OUT/locate.txt"
done
python3 ci/rcon.py "stop" > /dev/null
for i in $(seq 1 90); do pgrep -f forgeserveruserdev > /dev/null || break; sleep 1; done
wait || true
grep -nE "ERROR|Exception|Caused by" server.log | head -80 > "$OUT/server_errors.txt" || true

# 2. Singleplayer save with a datapack that drives the visit.
rm -rf run/saves/living && mkdir -p run/saves && cp -r run/world run/saves/living
DP=run/saves/living/datapacks/living/data
mkdir -p "$DP/living/functions" "$DP/minecraft/tags/functions"
echo '{"pack":{"pack_format":15,"description":"living"}}' > run/saves/living/datapacks/living/pack.mcmeta
echo '{"values":["living:load"]}' > "$DP/minecraft/tags/functions/load.json"
echo '{"values":["living:tick"]}' > "$DP/minecraft/tags/functions/tick.json"
cat > "$DP/living/functions/load.mcfunction" <<FN
gamerule sendCommandFeedback false
gamerule doDaylightCycle false
time set 5000
weather clear 1000000
scoreboard objectives add living dummy
scoreboard players set started living 0
FN
cat > "$DP/living/functions/tick.mcfunction" <<FN
execute unless score started living matches 1 if entity @a run scoreboard players set t living 0
execute unless score started living matches 1 if entity @a run scoreboard players set started living 1
scoreboard players add t living 1
effect give @a minecraft:resistance infinite 4 true
effect give @a minecraft:regeneration infinite 4 true
effect give @a minecraft:saturation infinite 0 true
effect give @a minecraft:water_breathing infinite 0 true
execute if score t living matches 1..399 run gamemode spectator @a
execute if score t living matches 1..399 run tp @a $((SX + 30)) 84 $SZ 90 32
execute if score t living matches 300 run say living_shot 01_pirate_ship
execute if score t living matches 400 run gamemode survival @a
execute if score t living matches 400 run tp @a $SX 70 $SZ
execute if score t living matches 700 run say living_shot 02_boarding
execute if score t living matches 800 run gamemode spectator @a
execute if score t living matches 800 run tp @a $((VX + 24)) 110 $((VZ + 24)) 135 38
execute if score t living matches 820 positioned $VX 64 $VZ run livingworld grow
execute if score t living matches 840 positioned $VX 64 $VZ run livingworld grow
execute if score t living matches 860 positioned $VX 64 $VZ run livingworld grow
execute if score t living matches 880 positioned $VX 64 $VZ store success score pirates living run livingworld pirates
execute if score t living matches 881 if score pirates living matches 1 run say living_result pirates_landed
execute if score t living matches 881 unless score pirates living matches 1 run say living_result no_coastal_village
execute if score t living matches 1200 run say living_shot 03_village
execute if score t living matches 1300 run gamemode survival @a
execute if score t living matches 1300 run spreadplayers $VX $VZ 0 6 false @a
execute if score t living matches 1600 run say living_shot 04_village_life
execute if score t living matches 1700 run gamemode spectator @a
execute if score t living matches 1700 run spreadplayers 0 0 0 4 false @a
execute if score t living matches 1720 at @p run fill ~-7 ~ ~-7 ~7 ~6 ~7 minecraft:stone_bricks hollow
execute if score t living matches 1721 at @p run livingworld stage 4
execute if score t living matches 1722 at @p store success score reacted living run livingworld react 10
execute if score t living matches 1723 if score reacted living matches 1 run say living_result world_reacted
execute if score t living matches 1730 at @p run tp @p ~28 ~48 ~28 135 42
execute if score t living matches 2100 run say living_shot 05_world_memory
execute if score t living matches 2200 run time set 18000
execute if score t living matches 2200 run gamemode survival @a
execute if score t living matches 2200 at @p run spreadplayers ~ ~ 0 4 false @a
execute if score t living matches 2210 at @p run summon dunesrelics:shade ^ ^ ^8
execute if score t living matches 2500 run say living_shot 06_night
execute if score t living matches 2600 run say living_done
FN
cp ci/showcase/options.txt run/options.txt

# 3. The client plays the visit on a virtual display.
Xvfb :99 -screen 0 1280x720x24 > /dev/null 2>&1 &
export DISPLAY=:99
export LIBGL_ALWAYS_SOFTWARE=1
./gradlew runClient -PquickPlay=living --no-daemon > client.log 2>&1 &
for i in $(seq 1 300); do
  grep -q "joined the game" client.log && break
  grep -q "Crash report\|#@!@#" client.log && break
  sleep 2
done
sleep 3
xdotool key F1 || true
for view in 01_pirate_ship 02_boarding 03_village 04_village_life 05_world_memory 06_night done; do
  for i in $(seq 1 240); do
    grep -q "living_shot $view\|living_$view" client.log && break
    grep -q "Crash report\|#@!@#\|Stopping!" client.log && break 2
    sleep 1
  done
  [ "$view" = done ] && break
  sleep 1
  import -window root -display :99 "$OUT/$view.png" && echo "captured $view"
done
sleep 2
pkill -f forgeclientuserdev || true
sleep 5
grep -nE "ERROR|Exception|Caused by|at com\.dunesrelics" client.log | head -150 > "$OUT/client_errors.txt" || true
grep -n "\[CHAT\]" client.log | tail -60 > "$OUT/client_chat.txt" || true
cp run/crash-reports/*.txt "$OUT/" 2>/dev/null || true
tail -60 client.log > "$OUT/client_tail.txt"
ls -la "$OUT"

#!/usr/bin/env bash
# Volcano soak test: generates a normal world, finds a real volcano with /locate, then flies the client there,
# walks its flanks, visits the crater and the magma chamber during ashfall and an eruption, fights the Magma Titan,
# and saves screenshots, errors and any crash report to ci/soak. Used by the CI "soak" step.
set -uo pipefail
cd "$(dirname "$0")/.."
OUT=ci/soak
mkdir -p run "$OUT"
rm -rf "$OUT"/*

# 1. A dedicated server generates the world; RCON finds the nearest volcano and the heart inside it.
echo "eula=true" > run/eula.txt
cat > run/server.properties <<PROPS
level-name=world
level-seed=${SOAK_SEED:-8412}
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
python3 ci/rcon.py "locate structure dunesrelics:volcano" | tee "$OUT/locate.txt"
read -r VX VZ < <(python3 - <<'PY'
import re
text = open("ci/soak/locate.txt").read()
m = re.search(r"\[(-?\d+), ~, (-?\d+)\]", text)
print(*(m.groups() if m else ("", "")))
PY
)
if [ -z "$VX" ]; then echo "no volcano found"; python3 ci/rcon.py "stop"; sleep 20; exit 1; fi
# The volcano is centred on the middle of the located chunk.
CX=$(( (VX >> 4) * 16 + 8 )); CZ=$(( (VZ >> 4) * 16 + 8 ))
echo "volcano centre: $CX $CZ"
python3 ci/rcon.py "forceload add $((CX - 64)) $((CZ - 64)) $((CX + 64)) $((CZ + 64))" > /dev/null
HY=""
for attempt in $(seq 1 40); do
  HY=$(python3 - "$CX" "$CZ" <<'PY'
import sys
sys.path.insert(0, "ci")
import rcon
cx, cz = sys.argv[1], sys.argv[2]
s = rcon.connect()
for y in range(40, 200):
    r = rcon.packet(s, 7, 2, "execute if block %s %d %s dunesrelics:heart_of_the_volcano" % (cx, y, cz))[1]
    if "passed" in r:
        print(y)
        break
    if "not loaded" in r.lower():
        break
PY
)
  [ -n "$HY" ] && break
  sleep 5
done
echo "heart at y=$HY" | tee -a "$OUT/locate.txt"
python3 ci/rcon.py "stop" > /dev/null
for i in $(seq 1 90); do pgrep -f forgeserveruserdev > /dev/null || break; sleep 1; done
wait || true
grep -nE "ERROR|Exception|Caused by" server.log | head -80 > "$OUT/server_errors.txt" || true
[ -n "$HY" ] || HY=80

# 2. Singleplayer save with a datapack that drives the visit.
rm -rf run/saves/soak && mkdir -p run/saves && cp -r run/world run/saves/soak
DP=run/saves/soak/datapacks/soak/data
mkdir -p "$DP/soak/functions" "$DP/minecraft/tags/functions"
echo '{"pack":{"pack_format":15,"description":"soak"}}' > run/saves/soak/datapacks/soak/pack.mcmeta
echo '{"values":["soak:load"]}' > "$DP/minecraft/tags/functions/load.json"
echo '{"values":["soak:tick"]}' > "$DP/minecraft/tags/functions/tick.json"
cat > "$DP/soak/functions/load.mcfunction" <<FN
gamerule sendCommandFeedback false
gamerule doDaylightCycle false
time set 6000
scoreboard objectives add soak dummy
scoreboard players set started soak 0
FN
cat > "$DP/soak/functions/tick.mcfunction" <<FN
execute unless score started soak matches 1 if entity @a run scoreboard players set t soak 0
execute unless score started soak matches 1 if entity @a run scoreboard players set started soak 1
scoreboard players add t soak 1
gamemode survival @a[gamemode=!survival]
effect give @a minecraft:resistance infinite 4 true
effect give @a minecraft:fire_resistance infinite 0 true
effect give @a minecraft:regeneration infinite 4 true
effect give @a minecraft:saturation infinite 0 true
effect give @a minecraft:water_breathing infinite 0 true
execute if score t soak matches 1 run weather rain 1000000
execute if score t soak matches 1 run spreadplayers $((CX + 40)) $CZ 0 6 false @a
execute if score t soak matches 400 run say soak_shot 01_flank
execute if score t soak matches 600 run spreadplayers $CX $CZ 6 14 false @a
execute if score t soak matches 1000 run say soak_shot 02_crater
execute if score t soak matches 1200 run tp @a $((CX + 2)) $((HY - 1)) $((CZ + 2)) -135 20
execute if score t soak matches 1600 run say soak_shot 03_chamber
execute if score t soak matches 1700 run weather thunder 1000000
execute if score t soak matches 1700 run summon dunesrelics:magma_titan $((CX - 2)) $HY $((CZ - 2))
execute if score t soak matches 2300 run say soak_shot 04_titan
execute if score t soak matches 2400 run kill @e[type=dunesrelics:magma_titan]
execute if score t soak matches 2400 run spreadplayers $((CX - 45)) $CZ 0 8 false @a
execute if score t soak matches 3000 run say soak_shot 05_eruption
execute if score t soak matches 3200 run say soak_done
FN
cp ci/showcase/options.txt run/options.txt

# 3. The client plays the visit on a virtual display.
Xvfb :99 -screen 0 1280x720x24 > /dev/null 2>&1 &
export DISPLAY=:99
export LIBGL_ALWAYS_SOFTWARE=1
./gradlew runClient -PquickPlay=soak --no-daemon > client.log 2>&1 &
for i in $(seq 1 300); do
  grep -q "joined the game" client.log && break
  grep -q "Crash report\|#@!@#" client.log && break
  sleep 2
done
sleep 3
xdotool key F1 || true
for view in 01_flank 02_crater 03_chamber 04_titan 05_eruption done; do
  for i in $(seq 1 240); do
    grep -q "soak_shot $view\|soak_$view" client.log && break
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
cp run/crash-reports/*.txt "$OUT/" 2>/dev/null || true
cp run/saves/soak/../../crash-reports/*.txt "$OUT/" 2>/dev/null || true
tail -60 client.log > "$OUT/client_tail.txt"
ls -la "$OUT"

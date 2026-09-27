#!/usr/bin/env bash
# Starts Minecraft with the mod in a virtual display, opens a flat Ancient Dunes world with a showcase
# of every block and mob, and saves screenshots to ci/screenshots. Used by the CI "showcase" step.
set -uo pipefail
cd "$(dirname "$0")/.."
mkdir -p run ci/screenshots
rm -f ci/screenshots/*.png

# 1. Let a dedicated server create the world (flat Ancient Dunes), then stop it cleanly.
echo "eula=true" > run/eula.txt
cp ci/showcase/server.properties run/server.properties
rm -rf run/world
./gradlew runServer --no-daemon > server.log 2>&1 &
for i in $(seq 1 180); do
  grep -q "Done (" server.log && break
  grep -q "Exception in server tick loop\|Failed to start the minecraft server" server.log && break
  sleep 2
done
grep -E "Done \(|ERROR" server.log | tail -5
pkill -TERM -f forgeserveruserdev || true
for i in $(seq 1 60); do pgrep -f forgeserveruserdev > /dev/null || break; sleep 1; done
wait || true
ls run/world || { echo "no world was created"; exit 1; }

# 2. Turn it into a singleplayer save with the showcase datapack.
rm -rf run/saves/showcase
mkdir -p run/saves
cp -r run/world run/saves/showcase
mkdir -p run/saves/showcase/datapacks
cp -r ci/showcase/datapack run/saves/showcase/datapacks/showcase
cp ci/showcase/options.txt run/options.txt

# 3. Start the client on a virtual display and join the world.
Xvfb :99 -screen 0 1280x720x24 > /dev/null 2>&1 &
export DISPLAY=:99
export LIBGL_ALWAYS_SOFTWARE=1
./gradlew runClient -PquickPlay=showcase --no-daemon > client.log 2>&1 &
joined=0
for i in $(seq 1 300); do
  if grep -q "joined the game" client.log; then joined=1; break; fi
  if grep -q "Crash report\|#@!@#" client.log; then break; fi
  sleep 2
done
if [ "$joined" != 1 ]; then
  echo "client did not join the world"; tail -80 client.log
  import -window root -display :99 ci/screenshots/failed.png || true
  pkill -f forgeclientuserdev || true
  exit 1
fi
sleep 3
xdotool key F1 || true   # hide the HUD

shot() { sleep "$1"; import -window root -display :99 "ci/screenshots/$2.png" && echo "captured $2"; }
shot 14 01_blocks
shot 20 02_mobs
shot 20 03_ruins_oasis
shot 22 04_sandstorm
shot 20 05_sandstorm_blocks
shot 20 06_volcanic_blocks
shot 20 07_volcanic_mobs
shot 20 08_volcano
shot 20 09_magma_chamber
shot 24 10_ashfall_eruption

echo "---- client warnings and errors mentioning the mod ----"
grep -nE "WARN|ERROR" client.log | grep -iE "dunesrelics|missing|unable|failed to load|exception" | head -60 || true
pkill -f forgeclientuserdev || true
sleep 5

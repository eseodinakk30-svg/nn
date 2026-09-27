"""JSON data for the "Living World" update: models, loot, recipes (with the millstone's), tags, the pirate ship,
spawns, advancements and translations. Called from generate_data.py."""
import os

from generate_data import (ASSETS, DATA, MOD, m, write, vanilla_json, blockstate, block_model, item_model, tex,
                           simple_block_state, cube_all, loot, item_entry, pool, SURVIVES, SILK, self_drop, recipe,
                           shaped, shapeless, cooking, tag, worldgen)

SPRUCE = "minecraft:block/spruce_planks"
LOG = "minecraft:block/stripped_spruce_log"
DARK_OAK = "minecraft:block/dark_oak_planks"


def face(texture, uv=None, tint=False, cull=None):
    f = {"texture": texture}
    if uv:
        f["uv"] = uv
    if tint:
        f["tintindex"] = 0
    if cull:
        f["cullface"] = cull
    return f


def box(frm, to, texture, faces=("north", "south", "east", "west", "up", "down"), rotation=None):
    element = {"from": frm, "to": to, "faces": {f: {"texture": texture} for f in faces}}
    if any(c < 0 or c > 16 for c in frm + to):
        # Outside the block the game would take the texture coordinates from the position and sample the
        # neighbouring sprites in the atlas; map each face onto the texture explicitly instead.
        dx, dy, dz = (abs(b - a) for a, b in zip(frm, to))
        size = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}
        for f in faces:
            w, h = size[f]
            element["faces"][f]["uv"] = [0, 0, min(w, 16), min(h, 16)]
    if rotation:
        element["rotation"] = rotation
    return element


# ================================================================================================ block assets

def water_wheel_models():
    """The wheel spins around the X axis (the block entity renderer turns it); the block itself is only the hub."""
    block_model("water_wheel", {"parent": "minecraft:block/block", "textures": {"log": "#end", "end": LOG, "particle": SPRUCE},
                                "elements": [box([0, 6, 6], [16, 10, 10], "#end"), box([4, 5, 5], [12, 11, 11], "#end")]})
    blockstate("water_wheel", {"variants": {
        "axis=%s,spinning=%s" % (a, s): ({"model": m("block/water_wheel"), "y": 90} if a == "z" else {"model": m("block/water_wheel")})
        for a in ("x", "z") for s in ("false", "true")}})
    elements = []
    rot = lambda angle: {"origin": [8, 8, 8], "axis": "x", "angle": angle}
    rims = [([5, 26, 0], [11, 28, 16]), ([5, -12, 0], [11, -10, 16]), ([5, 0, -12], [11, 16, -10]), ([5, 0, 26], [11, 16, 28])]
    for frm, to in rims:
        elements.append(box(frm, to, "#planks"))
    for frm, to in rims[:2]:
        for angle in (45, -45):
            elements.append(box(frm, to, "#planks", rotation=rot(angle)))
    spokes = [([7, -10, 7], [9, 26, 9]), ([7, 7, -10], [9, 9, 26])]
    for frm, to in spokes:
        elements.append(box(frm, to, "#log"))
        elements.append(box(frm, to, "#log", rotation=rot(45)))
    paddles = [([2, 28, 7], [14, 32, 9]), ([2, -16, 7], [14, -12, 9]), ([2, 7, -16], [14, 9, -12]), ([2, 7, 28], [14, 9, 32])]
    for frm, to in paddles:
        elements.append(box(frm, to, "#planks"))
    for frm, to in paddles[:2]:
        for angle in (45, -45):
            elements.append(box(frm, to, "#planks", rotation=rot(angle)))
    elements.append(box([4, 5, 5], [12, 11, 11], "#log"))
    block_model("water_wheel_wheel", {"parent": "minecraft:block/block", "textures": {"planks": SPRUCE, "log": LOG, "particle": SPRUCE},
                                      "elements": elements})
    item_model("water_wheel", {"parent": m("block/water_wheel_wheel"), "display": {
        "gui": {"rotation": [0, 90, 0], "scale": [0.36, 0.36, 0.36], "translation": [0, 0, 0]},
        "ground": {"scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 90, 0], "scale": [0.4, 0.4, 0.4]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.3, 0.3, 0.3], "translation": [0, 2.5, 0]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.3, 0.3, 0.3]}}})


def windmill_sails_models():
    """The hub on the tower (block model, facing north) and the blades the block entity renderer turns around it.
    The blades are drawn at half size (radius 1.5 blocks) and scaled up twice by the renderer."""
    block_model("windmill_sails", {"parent": "minecraft:block/block", "textures": {"log": LOG, "wood": SPRUCE, "particle": SPRUCE},
                                   "elements": [box([7, 7, 3], [9, 9, 16], "#log"), box([5.5, 5.5, 1], [10.5, 10.5, 3.5], "#wood")]})
    blockstate("windmill_sails", {"variants": {
        "facing=%s,wind=%d" % (f, w): ({"model": m("block/windmill_sails"), "y": r} if r else {"model": m("block/windmill_sails")})
        for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for w in range(4)}})

    def turned(frm, to, quarter):
        """A box rotated a quarter turn at a time around the hub (8, 8) in the plane of the sails."""
        (x1, y1, z1), (x2, y2, z2) = frm, to
        for _ in range(quarter):
            x1, y1, x2, y2 = 16 - y2, x1, 16 - y1, x2
        return [x1, y1, z1], [x2, y2, z2]

    arm = [(([7.25, 8, 1.25], [8.75, 32, 2.75]), "#spar")]
    for bar in (12, 18.5, 25, 31):
        arm.append((([8.75, bar, 1.5], [14, bar + 1, 2.5]), "#frame"))
    arm.append((([13, 12, 1.5], [14, 32, 2.5]), "#frame"))
    arm.append((([8.75, 12.5, 1.9], [13, 31.5, 2.1]), "#cloth"))
    elements = [box([6, 6, 0.5], [10, 10, 3], "#spar")]
    for quarter in range(4):
        for (frm, to), texture in arm:
            a, b = turned(frm, to, quarter)
            elements.append(box(a, b, texture))
    block_model("windmill_sails_blades", {"parent": "minecraft:block/block",
                                          "textures": {"spar": LOG, "frame": SPRUCE, "cloth": "minecraft:block/white_wool",
                                                       "particle": "minecraft:block/white_wool"},
                                          "elements": elements})
    item_model("windmill_sails", {"parent": m("block/windmill_sails_blades"), "display": {
        "gui": {"rotation": [0, 0, 0], "scale": [0.4, 0.4, 0.4], "translation": [0, 0, 0]},
        "ground": {"scale": [0.25, 0.25, 0.25]},
        "fixed": {"rotation": [0, 180, 0], "scale": [0.45, 0.45, 0.45]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "scale": [0.3, 0.3, 0.3], "translation": [0, 2.5, 0]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.3, 0.3, 0.3]}}})


def surf_models():
    """The flood's sheet of water: the still-water texture, tinted like the sea around it, drawn see-through."""
    block_model("surf", {"parent": "minecraft:block/block", "render_type": "minecraft:translucent",
                         "textures": {"water": "minecraft:block/water_still", "particle": "minecraft:block/water_still"},
                         "elements": [{"from": [0, 0, 0], "to": [16, 1.8, 16], "shade": False, "faces": {
                             "up": face("#water", [0, 0, 16, 16], tint=True),
                             "north": face("#water", [0, 0, 16, 2], tint=True, cull="north"),
                             "south": face("#water", [0, 0, 16, 2], tint=True, cull="south"),
                             "east": face("#water", [0, 0, 16, 2], tint=True, cull="east"),
                             "west": face("#water", [0, 0, 16, 2], tint=True, cull="west")}}]})
    blockstate("surf", {"variants": {"": {"model": m("block/surf")}}})


def generate_block_assets():
    water_wheel_models()
    windmill_sails_models()
    surf_models()

    # millstone: a stone base (block model) and the runner stone (turned by the block entity renderer)
    base = {"parent": "minecraft:block/block",
            "textures": {"top": "minecraft:block/smooth_stone", "side": "minecraft:block/smooth_stone_slab_side",
                         "particle": "minecraft:block/smooth_stone"},
            "elements": [{"from": [0, 0, 0], "to": [16, 10, 16], "faces": {
                "north": face("#side", [0, 6, 16, 16], cull="north"), "south": face("#side", [0, 6, 16, 16], cull="south"),
                "east": face("#side", [0, 6, 16, 16], cull="east"), "west": face("#side", [0, 6, 16, 16], cull="west"),
                "up": face("#top"), "down": face("#top", cull="down")}},
                box([7, 10, 7], [9, 15, 9], "#top")]}
    block_model("millstone", base)
    runner = {"parent": "minecraft:block/block",
              "textures": {"top": tex("millstone_top"), "side": tex("millstone_side"), "particle": tex("millstone_side")},
              "elements": [{"from": [1, 10, 1], "to": [15, 14, 15], "faces": {
                  "north": face("#side", [1, 6, 15, 10]), "south": face("#side", [1, 6, 15, 10]),
                  "east": face("#side", [1, 6, 15, 10]), "west": face("#side", [1, 6, 15, 10]),
                  "up": face("#top", [1, 1, 15, 15]), "down": face("#top", [1, 1, 15, 15])}}]}
    block_model("millstone_runner", runner)
    blockstate("millstone", {"variants": {"active=false": {"model": m("block/millstone")},
                                          "active=true": {"model": m("block/millstone")}}})
    item_model("millstone", {"parent": "minecraft:block/block", "textures": dict(runner["textures"], top2="minecraft:block/smooth_stone",
                                                                               side2="minecraft:block/smooth_stone_slab_side"),
                              "elements": [
                                  {"from": [0, 0, 0], "to": [16, 10, 16], "faces": {d: face("#side2", [0, 6, 16, 16]) for d in ("north", "south", "east", "west")}
                                   | {"up": face("#top2"), "down": face("#top2")}},
                                  runner["elements"][0]],
                              "display": vanilla_json("assets", "models/block/block", [])["display"]})

    # water trough: walls on the unconnected sides, water inside when filled
    walls = {"north": box([0, 0, 0], [16, 8, 2], "#wood"), "south": box([0, 0, 14], [16, 8, 16], "#wood"),
             "west": box([0, 0, 0], [2, 8, 16], "#wood"), "east": box([14, 0, 0], [16, 8, 16], "#wood")}
    block_model("water_trough_floor", {"parent": "minecraft:block/block", "textures": {"wood": SPRUCE, "particle": SPRUCE},
                                       "elements": [box([0, 0, 0], [16, 2, 16], "#wood")]})
    for side, element in walls.items():
        block_model("water_trough_" + side, {"textures": {"wood": SPRUCE, "particle": SPRUCE}, "elements": [element]})
    block_model("water_trough_water", {"textures": {"water": "minecraft:block/water_still", "particle": SPRUCE},
                                       "elements": [{"from": [0, 2, 0], "to": [16, 6.5, 16], "faces": {
                                           "up": face("#water", tint=True)}}], "render_type": "minecraft:translucent"})
    multipart = [{"apply": {"model": m("block/water_trough_floor")}}]
    for side in walls:
        multipart.append({"when": {side: "false"}, "apply": {"model": m("block/water_trough_" + side)}})
    multipart.append({"when": {"water": "1|2|3|4|5|6|7"}, "apply": {"model": m("block/water_trough_water")}})
    blockstate("water_trough", {"multipart": multipart})
    item_model("water_trough", {"parent": "minecraft:block/block", "textures": {"wood": SPRUCE, "particle": SPRUCE},
                                "elements": [box([0, 0, 0], [16, 2, 16], "#wood")] + list(walls.values()),
                                "display": vanilla_json("assets", "models/block/block", [])["display"]})

    # shore
    cube_all("wet_sand")
    for v in range(4):
        block_model("seashell_%d" % v, {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                        "textures": {"shell": tex("seashell_%d" % v), "particle": tex("seashell_%d" % v)},
                                        "elements": [{"from": [0, 0.1, 0], "to": [16, 0.1, 16], "faces": {
                                            "up": face("#shell"), "down": face("#shell")}},
                                            {"from": [5, 0, 5], "to": [11, 1.5, 11], "faces": {
                                                "up": face("#shell", [5, 5, 11, 11])}}]})
    blockstate("seashell", {"variants": {
        "natural=%s,variant=%d" % (n, v): [{"model": m("block/seashell_%d" % v), "y": y} if y else {"model": m("block/seashell_%d" % v)}
                                           for y in (0, 90, 180, 270)]
        for n in ("false", "true") for v in range(4)}})
    item_model("seashell", {"parent": "minecraft:item/generated", "textures": {"layer0": tex("seashell_0")}})
    block_model("clam", {"parent": "minecraft:block/block", "textures": {"top": tex("clam_top"), "side": tex("clam_side"),
                                                                        "particle": tex("clam_side")},
                         "elements": [{"from": [3, 0, 3], "to": [13, 4, 13], "faces": {
                             "north": face("#side", [3, 6, 13, 10]), "south": face("#side", [3, 6, 13, 10]),
                             "east": face("#side", [3, 6, 13, 10]), "west": face("#side", [3, 6, 13, 10]),
                             "up": face("#top", [3, 3, 13, 13]), "down": face("#top", [3, 3, 13, 13])}}]})
    blockstate("clam", {"variants": {"natural=false": {"model": m("block/clam")}, "natural=true": {"model": m("block/clam")}}})
    item_model("clam", {"parent": m("block/clam")})

    # cannon: a wooden carriage on four wheels with an iron barrel pointing north
    wheel = "minecraft:block/dark_oak_log_top"
    elements = [box([2, 1, 3], [14, 5, 13], "#wood"), box([1, 0, 3], [3, 4, 7], "#wheel"), box([13, 0, 3], [15, 4, 7], "#wheel"),
                box([1, 0, 9], [3, 4, 13], "#wheel"), box([13, 0, 9], [15, 4, 13], "#wheel"),
                {"from": [5, 5, -2], "to": [11, 11, 14], "faces": {
                    "north": face("#muzzle"), "south": face("#barrel", [5, 5, 11, 11]), "east": face("#barrel", [0, 5, 16, 11]),
                    "west": face("#barrel", [0, 5, 16, 11]), "up": face("#barrel", [5, 0, 11, 16]),
                    "down": face("#barrel", [5, 0, 11, 16])}},
                box([6.5, 6.5, 14], [9.5, 9.5, 16], "#barrel")]
    block_model("cannon", {"parent": "minecraft:block/block", "textures": {"wood": DARK_OAK, "wheel": wheel,
                                                                           "barrel": tex("cannon_barrel"), "muzzle": tex("cannon_muzzle"),
                                                                           "particle": tex("cannon_barrel")}, "elements": elements})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    blockstate("cannon", {"variants": {
        "facing=%s,loaded=%s,powered=%s" % (f, l, p): ({"model": m("block/cannon"), "y": r} if r else {"model": m("block/cannon")})
        for f, r in rot.items() for l in ("false", "true") for p in ("false", "true")}})
    item_model("cannon", {"parent": m("block/cannon")})

    # dreamcatcher: a flat hanging disc
    block_model("dreamcatcher", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                                 "textures": {"catcher": tex("dreamcatcher"), "particle": tex("dreamcatcher")},
                                 "elements": [{"from": [0, 0, 8], "to": [16, 16, 8], "faces": {
                                     "north": face("#catcher"), "south": face("#catcher")}}]})
    blockstate("dreamcatcher", {"variants": {
        "facing=%s" % f: ({"model": m("block/dreamcatcher"), "y": r} if r else {"model": m("block/dreamcatcher")})
        for f, r in rot.items()}})
    item_model("dreamcatcher", {"parent": "minecraft:item/generated", "textures": {"layer0": tex("dreamcatcher")}})


def generate_item_models():
    generated = ["flour", "dough", "pearl", "message_in_a_bottle", "cannonball", "captain_hat", "gloom_dust", "world_chronicle",
                 "request_note"]
    for name in generated:
        item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": m("item/" + name)}})
    item_model("cutlass", {"parent": "minecraft:item/handheld", "textures": {"layer0": m("item/cutlass")}})
    overrides = []
    for f in range(8):
        item_model("tide_clock_%02d" % f, {"parent": "minecraft:item/generated", "textures": {"layer0": m("item/tide_clock_%02d" % f)}})
        overrides.append({"predicate": {m("tide"): f / 8.0}, "model": m("item/tide_clock_%02d" % f)})
    item_model("tide_clock", {"parent": "minecraft:item/generated", "textures": {"layer0": m("item/tide_clock_04")},
                              "overrides": overrides})
    for mob in ("pirate", "pirate_gunner", "pirate_captain", "traveler", "shade", "village_builder", "lumberjack", "quarryman"):
        item_model(mob + "_spawn_egg", {"parent": "minecraft:item/template_spawn_egg"})


# ================================================================================================ loot

def generate_loot():
    for name in ("water_wheel", "millstone", "water_trough", "cannon", "dreamcatcher", "seashell", "windmill_sails"):
        self_drop(name)
    loot("blocks/wet_sand", {"type": "minecraft:block", "pools": [pool([{
        "type": "minecraft:alternatives", "children": [item_entry(m("wet_sand"), conditions=[SILK]),
                                                       item_entry("minecraft:sand")]}], conditions=[SURVIVES])],
        "random_sequence": m("blocks/wet_sand")})
    loot("blocks/clam", {"type": "minecraft:block", "pools": [
        pool([{"type": "minecraft:alternatives", "children": [
            item_entry(m("clam"), conditions=[SILK]),
            item_entry(m("pearl"), conditions=[{"condition": "minecraft:table_bonus", "enchantment": "minecraft:fortune",
                                                "chances": [0.3, 0.4, 0.5, 0.65]}])]}])],
        "random_sequence": m("blocks/clam")})

    looting = lambda lo, hi: {"function": "minecraft:looting_enchant",
                              "count": {"type": "minecraft:uniform", "min": float(lo), "max": float(hi)}}
    by_player = {"condition": "minecraft:killed_by_player"}
    treasure_map = {"function": "minecraft:exploration_map", "destination": "minecraft:on_treasure_maps",
                    "decoration": "red_x", "zoom": 1, "skip_existing_chunks": False}
    ship_map = {"function": "minecraft:exploration_map", "destination": m("on_pirate_maps"), "decoration": "red_x",
                "zoom": 1, "skip_existing_chunks": False}
    loot("entities/pirate", {"type": "minecraft:entity", "pools": [
        pool([item_entry("minecraft:gold_nugget", (1, 3), functions=[looting(0, 1)])]),
        pool([item_entry(m("cannonball"), (0, 1), functions=[looting(0, 1)])]),
        pool([item_entry("minecraft:emerald")], conditions=[by_player, {"condition": "minecraft:random_chance_with_looting",
                                                                          "chance": 0.08, "looting_multiplier": 0.02}]),
    ], "random_sequence": m("entities/pirate")})
    loot("entities/pirate_gunner", {"type": "minecraft:entity", "pools": [
        pool([item_entry("minecraft:arrow", (0, 3), functions=[looting(0, 1)])]),
        pool([item_entry("minecraft:gunpowder", (0, 2), functions=[looting(0, 1)])]),
        pool([item_entry("minecraft:gold_nugget", (0, 2))]),
    ], "random_sequence": m("entities/pirate_gunner")})
    loot("entities/pirate_captain", {"type": "minecraft:entity", "pools": [
        pool([item_entry(m("captain_hat"))]),
        pool([item_entry(m("cutlass"))]),
        pool([item_entry("minecraft:gold_ingot", (3, 6))]),
        pool([item_entry("minecraft:emerald", (2, 5))]),
        pool([item_entry("minecraft:map", functions=[treasure_map])]),
    ], "random_sequence": m("entities/pirate_captain")})
    loot("entities/shade", {"type": "minecraft:entity", "pools": [
        pool([item_entry(m("gloom_dust"), (1, 2), functions=[looting(0, 1)])]),
    ], "random_sequence": m("entities/shade")})
    loot("entities/traveler", {"type": "minecraft:entity", "pools": [], "random_sequence": m("entities/traveler")})

    enchant = {"function": "minecraft:enchant_randomly"}
    loot("chests/pirate_hold", {"type": "minecraft:chest", "pools": [
        pool([
            item_entry("minecraft:gunpowder", (2, 6), weight=15),
            item_entry(m("cannonball"), (2, 6), weight=15),
            item_entry("minecraft:gold_nugget", (3, 9), weight=14),
            item_entry("minecraft:iron_ingot", (1, 4), weight=12),
            item_entry("minecraft:bread", (2, 5), weight=10),
            item_entry("minecraft:cooked_cod", (2, 5), weight=10),
            item_entry("minecraft:lead", (1, 2), weight=6),
            item_entry("minecraft:emerald", (1, 3), weight=6),
            item_entry(m("pearl"), (1, 2), weight=4),
            item_entry(m("message_in_a_bottle"), weight=3),
        ], rolls={"type": "minecraft:uniform", "min": 3.0, "max": 6.0}),
    ], "random_sequence": m("chests/pirate_hold")})
    loot("chests/pirate_captain", {"type": "minecraft:chest", "pools": [
        pool([
            item_entry("minecraft:gold_ingot", (2, 6), weight=15),
            item_entry("minecraft:emerald", (2, 6), weight=15),
            item_entry(m("pearl"), (1, 3), weight=10),
            item_entry("minecraft:diamond", (1, 2), weight=5),
            item_entry("minecraft:golden_apple", weight=4),
            item_entry(m("message_in_a_bottle"), weight=4),
            item_entry("minecraft:book", weight=5, functions=[enchant]),
            item_entry(m("tide_clock"), weight=3),
        ], rolls={"type": "minecraft:uniform", "min": 3.0, "max": 5.0}),
        pool([item_entry("minecraft:map", functions=[treasure_map])]),
    ], "random_sequence": m("chests/pirate_captain")})
    loot("gameplay/message_in_a_bottle", {"type": "minecraft:chest", "pools": [
        pool([
            item_entry("minecraft:map", weight=25, functions=[treasure_map]),
            item_entry("minecraft:map", weight=15, functions=[ship_map]),
            item_entry(m("pearl"), weight=15),
            item_entry("minecraft:emerald", (1, 4), weight=20),
            item_entry("minecraft:gold_nugget", (2, 6), weight=15),
            item_entry(m("seashell"), (1, 3), weight=10),
        ]),
    ], "random_sequence": m("gameplay/message_in_a_bottle")})


# ================================================================================================ recipes

def milling(name, ingredient, result, count=1, time=100):
    recipe("milling/" + name, {"type": m("milling"), "ingredient": {"tag": ingredient[1:]} if ingredient.startswith("#")
                               else {"item": ingredient}, "result": {"item": result, "count": count}, "time": time})


def generate_recipes():
    shaped("windmill_sails", ["WSW", "SPS", "WSW"], {"W": "#minecraft:wool", "S": "minecraft:stick", "P": "#minecraft:planks"},
           m("windmill_sails"), 1, "redstone")
    shaped("water_wheel", ["PSP", "SLS", "PSP"], {"P": "#minecraft:planks", "S": "minecraft:stick", "L": "#minecraft:logs"},
           m("water_wheel"), 1, "redstone")
    shaped("millstone", ["TTT", "CIC", "CCC"], {"T": "minecraft:smooth_stone_slab", "C": "minecraft:cobblestone",
                                                "I": "minecraft:iron_ingot"}, m("millstone"), 1, "misc")
    shaped("water_trough", ["P P", "PPP"], {"P": "#minecraft:planks"}, m("water_trough"), 3, "misc")
    shaped("cannon", ["III", "IGI", "LLL"], {"I": "minecraft:iron_ingot", "G": "minecraft:gunpowder", "L": "#minecraft:logs"},
           m("cannon"), 1, "redstone")
    shaped("cannonball", [" N ", "NIN", " N "], {"N": "minecraft:iron_nugget", "I": "minecraft:iron_ingot"},
           m("cannonball"), 4, "equipment")
    shaped("cutlass", [" I", "I ", "SG"], {"I": "minecraft:iron_ingot", "S": "minecraft:stick", "G": "minecraft:gold_nugget"},
           m("cutlass"), 1, "equipment")
    shaped("tide_clock", [" C ", "CKC", " C "], {"C": "minecraft:copper_ingot", "K": "minecraft:clock"}, m("tide_clock"), 1, "equipment")
    shaped("dreamcatcher", ["TST", "SGS", "F F"], {"T": "minecraft:string", "S": "minecraft:stick", "G": m("gloom_dust"),
                                                   "F": "minecraft:feather"}, m("dreamcatcher"), 1, "misc")
    shapeless("world_chronicle", ["minecraft:book", "minecraft:map", "minecraft:feather"], m("world_chronicle"), 1)
    shapeless("dough", [m("flour"), m("flour"), m("flour"), "minecraft:water_bucket"], m("dough"), 3)
    cooking("bread_from_dough", m("dough"), "minecraft:bread", 0.35, ("smelting", "smoking", "campfire_cooking"),
            time=200, category="food")
    shapeless("pink_dye_from_seashell", [m("seashell")], "minecraft:pink_dye", 1)
    milling("flour", "minecraft:wheat", m("flour"), 1, 80)
    milling("bone_meal", "minecraft:bone", "minecraft:bone_meal", 5, 100)
    milling("bone_meal_from_seashell", m("seashell"), "minecraft:bone_meal", 2, 80)
    milling("gravel", "minecraft:cobblestone", "minecraft:gravel", 1, 120)
    milling("sand", "minecraft:gravel", "minecraft:sand", 1, 120)
    milling("sugar", "minecraft:sugar_cane", "minecraft:sugar", 2, 60)
    milling("sugar_from_dates", m("dates"), "minecraft:sugar", 2, 60)
    milling("red_dye_from_beetroot", "minecraft:beetroot", "minecraft:red_dye", 2, 60)
    milling("red_dye_from_fire_pepper", m("fire_pepper"), "minecraft:red_dye", 2, 60)
    milling("volcanic_ash_from_pumice", m("pumice"), m("volcanic_ash"), 2, 100)
    milling("gravel_from_scoria", m("scoria"), "minecraft:gravel", 1, 100)
    milling("glowstone_dust", "minecraft:glowstone", "minecraft:glowstone_dust", 4, 100)


# ================================================================================================ tags & worldgen

def generate_tags():
    mm = lambda names: [m(n) for n in names]
    tag("blocks", "minecraft", "mineable/pickaxe", mm(["millstone", "cannon"]))
    tag("blocks", "minecraft", "mineable/axe", mm(["water_wheel", "water_trough", "windmill_sails"]))
    tag("blocks", "minecraft", "mineable/shovel", mm(["wet_sand"]))
    tag("blocks", "minecraft", "needs_stone_tool", mm(["cannon"]))
    tag("items", MOD, "villager_gifts", ["#minecraft:small_flowers", "minecraft:bread", "minecraft:cake", "minecraft:cookie",
                                          "minecraft:pumpkin_pie", "minecraft:emerald", "minecraft:honey_bottle",
                                          "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:apple",
                                          m("dates"), m("honeyed_dates"), m("pearl"), m("seashell")])
    tag("items", "minecraft", "swords", mm(["cutlass"]))
    tag("items", "minecraft", "trim_materials", mm(["pearl"]))
    tag("items", "minecraft", "trimmable_armor", mm(["captain_hat"]))
    tag("items", "minecraft", "bookshelf_books", mm(["world_chronicle"]))
    tag("items", "minecraft", "lectern_books", [])
    tag("entity_types", "minecraft", "raiders", mm(["pirate", "pirate_gunner", "pirate_captain"]))
    tag("entity_types", "minecraft", "illager", mm(["pirate", "pirate_gunner", "pirate_captain"]))
    tag("worldgen/biome", MOD, "has_tides", ["#minecraft:is_beach", "#minecraft:is_ocean"])
    tag("worldgen/biome", MOD, "has_currents", ["#minecraft:is_ocean", "#minecraft:is_river"])
    tag("worldgen/biome", MOD, "has_structure/pirate_ship", [
        "minecraft:ocean", "minecraft:deep_ocean", "minecraft:lukewarm_ocean", "minecraft:deep_lukewarm_ocean",
        "minecraft:warm_ocean", "minecraft:cold_ocean", "minecraft:deep_cold_ocean"])
    tag("worldgen/biome", MOD, "spawns_shades", ["#minecraft:is_forest", "#minecraft:is_taiga", "#minecraft:is_savanna",
                                                  "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow",
                                                  "minecraft:swamp", "minecraft:mangrove_swamp"])
    tag("worldgen/structure", MOD, "on_traveler_maps/volcano", [m("volcano")])
    tag("worldgen/structure", MOD, "on_traveler_maps/pirate_ship", [m("pirate_ship")])
    tag("worldgen/structure", MOD, "on_pirate_maps", [m("pirate_ship")])
    tag("point_of_interest_type", "minecraft", "acquirable_job_site", [])


def generate_worldgen():
    spawn = lambda t, w, lo, hi: {"type": t, "weight": w, "minCount": lo, "maxCount": hi}
    worldgen("structure", "pirate_ship", {
        "type": m("pirate_ship"), "biomes": "#" + m("has_structure/pirate_ship"), "step": "surface_structures",
        "terrain_adaptation": "none",
        "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": [
            spawn(m("pirate"), 10, 1, 2), spawn(m("pirate_gunner"), 5, 1, 1)]}}})
    worldgen("structure_set", "pirate_ship", {
        "structures": [{"structure": m("pirate_ship"), "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "salt": 918273645, "spacing": 36, "separation": 14}})
    write(os.path.join(DATA, MOD, "forge/biome_modifier/shades.json"), {
        "type": "forge:add_spawns", "biomes": "#" + m("spawns_shades"), "spawners": [spawn(m("shade"), 6, 1, 1)]})


# ================================================================================================ misc

def generate_particles():
    write(os.path.join(ASSETS, "particles/foam.json"), {"textures": [m("foam_%d" % i) for i in range(3)]})


def generate_misc_data():
    write(os.path.join(DATA, MOD, "trim_material/pearl.json"), {
        "asset_name": "pearl", "description": {"color": "#E8E0F0", "translate": "trim_material.dunesrelics.pearl"},
        "ingredient": m("pearl"), "item_model_index": 0.15})

    def adv(name, parent, icon, criteria, frame="task", rewards=None, requirements=None):
        obj = {"display": {"icon": {"item": icon}, "title": {"translate": "advancements.dunesrelics.%s.title" % name},
                           "description": {"translate": "advancements.dunesrelics.%s.description" % name},
                           "frame": frame, "show_toast": parent is not None, "announce_to_chat": parent is not None,
                           "hidden": False}, "criteria": criteria}
        if parent:
            obj["parent"] = m(parent)
        else:
            obj["display"]["background"] = "minecraft:textures/block/spruce_planks.png"
        if rewards:
            obj["rewards"] = rewards
        if requirements:
            obj["requirements"] = requirements
        write(os.path.join(DATA, MOD, "advancements", name + ".json"), obj)

    has = lambda item: {"has": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [item]}]}}}
    entity = lambda t: [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": t}}]
    placed = lambda block: {"placed": {"trigger": "minecraft:placed_block", "conditions": {"location": [
        {"condition": "minecraft:location_check", "predicate": {"block": {"blocks": [block]}}}]}}}
    adv("living_root", None, m("water_wheel"), {"tick": {"trigger": "minecraft:tick"}})
    adv("chronicle", "living_root", m("world_chronicle"), has(m("world_chronicle")))
    adv("mill", "living_root", m("flour"), has(m("flour")))
    adv("irrigation", "mill", m("water_trough"), placed(m("water_trough")))
    adv("villager_gift", "living_root", "minecraft:poppy", {"gift": {"trigger": "minecraft:player_interacted_with_entity",
                                                                     "conditions": {"item": {"tag": m("villager_gifts")},
                                                                                    "entity": entity("minecraft:villager")}}})
    adv("traveler", "villager_gift", "minecraft:map", {"trade": {"trigger": "minecraft:villager_trade", "conditions": {
        "villager": entity(m("traveler"))}}})
    adv("pearl", "living_root", m("pearl"), has(m("pearl")))
    adv("message_in_a_bottle", "pearl", m("message_in_a_bottle"), has(m("message_in_a_bottle")))
    adv("cannon", "living_root", m("cannon"), placed(m("cannon")))
    adv("pirate_ship", "cannon", m("cutlass"), {"looted": {"trigger": "minecraft:player_generates_container_loot",
                                                          "conditions": {"loot_table": m("chests/pirate_captain")}}}, frame="goal")
    adv("pirate_captain", "pirate_ship", m("captain_hat"), {"killed": {"trigger": "minecraft:player_killed_entity",
                                                                       "conditions": {"entity": entity(m("pirate_captain"))}}},
        frame="challenge", rewards={"experience": 100})
    adv("shade", "living_root", m("gloom_dust"), {"killed": {"trigger": "minecraft:player_killed_entity",
                                                             "conditions": {"entity": entity(m("shade"))}}})
    adv("dreamcatcher", "shade", m("dreamcatcher"), placed(m("dreamcatcher")))


# ================================================================================================ translations

BLOCKS = {
    "water_wheel": ("Water Wheel", "Водяное колесо"),
    "windmill_sails": ("Windmill Sails", "Крылья ветряной мельницы"),
    "surf": ("Surf", "Прибой"),
    "millstone": ("Millstone", "Жернова"),
    "water_trough": ("Water Trough", "Жёлоб для воды"),
    "wet_sand": ("Wet Sand", "Мокрый песок"),
    "seashell": ("Seashell", "Ракушка"),
    "clam": ("Clam", "Моллюск"),
    "cannon": ("Cannon", "Пушка"),
    "dreamcatcher": ("Dreamcatcher", "Ловец снов"),
}

ITEMS = {
    "flour": ("Flour", "Мука"),
    "dough": ("Dough", "Тесто"),
    "pearl": ("Pearl", "Жемчужина"),
    "message_in_a_bottle": ("Message in a Bottle", "Послание в бутылке"),
    "tide_clock": ("Tide Clock", "Приливные часы"),
    "cutlass": ("Cutlass", "Абордажная сабля"),
    "cannonball": ("Cannonball", "Пушечное ядро"),
    "captain_hat": ("Captain's Hat", "Капитанская треуголка"),
    "gloom_dust": ("Gloom Dust", "Сумрачная пыль"),
    "world_chronicle": ("World Chronicle", "Летопись мира"),
    "request_note": ("Villager's Request", "Просьба жителя"),
    "pirate_spawn_egg": ("Pirate Spawn Egg", "Яйцо призыва пирата"),
    "pirate_gunner_spawn_egg": ("Pirate Gunner Spawn Egg", "Яйцо призыва пирата-стрелка"),
    "pirate_captain_spawn_egg": ("Pirate Captain Spawn Egg", "Яйцо призыва капитана пиратов"),
    "traveler_spawn_egg": ("Traveler Spawn Egg", "Яйцо призыва путника"),
    "shade_spawn_egg": ("Shade Spawn Egg", "Яйцо призыва Тени"),
    "village_builder_spawn_egg": ("Builder Spawn Egg", "Яйцо призыва строителя"),
    "lumberjack_spawn_egg": ("Lumberjack Spawn Egg", "Яйцо призыва лесоруба"),
    "quarryman_spawn_egg": ("Quarryman Spawn Egg", "Яйцо призыва каменотёса"),
}

ENTITIES = {
    "pirate": ("Pirate", "Пират"),
    "pirate_gunner": ("Pirate Gunner", "Пират-стрелок"),
    "pirate_captain": ("Pirate Captain", "Капитан пиратов"),
    "traveler": ("Traveler", "Путник"),
    "shade": ("Shade", "Тень"),
    "cannonball": ("Cannonball", "Пушечное ядро"),
    "village_builder": ("Builder", "Строитель"),
    "lumberjack": ("Lumberjack", "Лесоруб"),
    "quarryman": ("Quarryman", "Каменотёс"),
}

ADVANCEMENTS = {
    "living_root": (("Living World", "The world remembers those who live in it"),
                    ("Живой мир", "Мир помнит тех, кто в нём живёт")),
    "chronicle": (("Dear Diary", "Make a World Chronicle and read what the world remembers"),
                  ("Дорогой дневник", "Сделайте летопись мира и прочтите, что помнит мир")),
    "mill": (("Grist for the Mill", "Grind flour with a water-driven millstone"),
             ("Всё перемелется", "Смелите муку на жерновах с водяным колесом")),
    "irrigation": (("Aqueduct", "Place a water trough to carry water to your fields"),
                   ("Акведук", "Поставьте жёлоб, чтобы провести воду к полям")),
    "villager_gift": (("Small Kindness", "Give a villager a present (sneak and use it on them)"),
                      ("Маленькая радость", "Подарите жителю подарок (присядьте и используйте на нём предмет)")),
    "traveler": (("Tales from Afar", "Trade with a traveller who came down the road to your home"),
                 ("Вести издалека", "Поторгуйте с путником, пришедшим по дороге к вашему дому")),
    "pearl": (("Low Tide", "Find a pearl in a clam washed up by the tide"),
              ("Отлив", "Найдите жемчужину в моллюске, выброшенном приливом")),
    "message_in_a_bottle": (("Message Received", "Find a message in a bottle on the shore"),
                            ("Послание получено", "Найдите на берегу бутылку с посланием")),
    "cannon": (("Fire in the Hole!", "Place a cannon"), ("Пли!", "Поставьте пушку")),
    "pirate_ship": (("Boarding Party", "Loot the captain's chest aboard a pirate ship"),
                    ("На абордаж!", "Обыщите сундук капитана на пиратском корабле")),
    "pirate_captain": (("Dead Men Tell No Tales", "Defeat a Pirate Captain"),
                       ("Мёртвые не рассказывают сказки", "Победите капитана пиратов")),
    "shade": (("Afraid of the Dark", "Defeat a Shade"), ("Боязнь темноты", "Победите Тень")),
    "dreamcatcher": (("Sweet Dreams", "Hang up a dreamcatcher to keep the night away"),
                     ("Сладких снов", "Повесьте ловец снов, чтобы ночь обходила вас стороной")),
}

VILLAGER_NAMES = [("Anselm", "Ансельм"), ("Brigid", "Бригитта"), ("Corin", "Корин"), ("Dorran", "Доран"),
                  ("Elka", "Элька"), ("Fenn", "Фенн"), ("Greta", "Грета"), ("Hollis", "Холлис"), ("Ivo", "Иво"),
                  ("Jora", "Йора"), ("Kasimir", "Казимир"), ("Lenna", "Ленна"), ("Milo", "Мило"), ("Nessa", "Несса"),
                  ("Oswin", "Освин"), ("Petra", "Петра"), ("Quill", "Квилл"), ("Rowan", "Рован"), ("Sela", "Села"),
                  ("Tobin", "Тобин"), ("Una", "Уна"), ("Vesna", "Весна"), ("Wendel", "Вендел"), ("Yara", "Яра"),
                  ("Zlata", "Злата"), ("Aldo", "Альдо"), ("Bertha", "Берта"), ("Cyril", "Кирилл"), ("Dagny", "Дагни"),
                  ("Edwin", "Эдвин"), ("Frida", "Фрида"), ("Gunnar", "Гуннар"), ("Hilde", "Хильда"), ("Ingram", "Ингрэм"),
                  ("Juno", "Юнона"), ("Klaus", "Клаус"), ("Liesel", "Лизель"), ("Matvey", "Матвей"), ("Nora", "Нора"),
                  ("Olaf", "Олаф"), ("Polina", "Полина"), ("Ragnar", "Рагнар"), ("Sonya", "Соня"), ("Timur", "Тимур"),
                  ("Ulla", "Улла"), ("Vadim", "Вадим"), ("Wilma", "Вильма"), ("Yaroslav", "Ярослав")]

VILLAGE_NAMES = [("Willowbrook", "Ивовый Ручей"), ("Stonemere", "Каменное Озеро"), ("Ashford", "Ясеневый Брод"),
                 ("Oakhollow", "Дубовая Лощина"), ("Millbank", "Мельничий Берег"), ("Fernside", "Папоротниково"),
                 ("Brackenridge", "Орляковый Гребень"), ("Kettlewick", "Котелково"), ("Harrowgate", "Бороновые Ворота"),
                 ("Thistledown", "Чертополохово"), ("Larkspur", "Живокость"), ("Dunmore", "Дюнмор"),
                 ("Elderglen", "Бузинная Долина"), ("Foxmeadow", "Лисий Луг"), ("Greyhaven", "Серая Гавань"),
                 ("Hazelford", "Ореховый Брод"), ("Ivybridge", "Плющевой Мост"), ("Juniper Hill", "Можжевеловый Холм"),
                 ("Kingsfold", "Королевский Двор"), ("Lindenfeld", "Липовое Поле"), ("Mossbury", "Моховое"),
                 ("Nettlebed", "Крапивное"), ("Oxbow", "Старица"), ("Pinecrest", "Сосновый Гребень"),
                 ("Quarryside", "Каменоломня"), ("Rookwood", "Грачёвник"), ("Saltmarsh", "Солёное Болото"),
                 ("Tidewater", "Приливное"), ("Upton", "Верховье"), ("Violetvale", "Фиалковая Долина"),
                 ("Windmere", "Ветреное Озеро"), ("Yarrowfield", "Тысячелистник")]

TRAITS = ["friendly", "grumpy", "greedy", "brave"]
GREETINGS = {
    ("hostile", "friendly"): ("...I'd rather you kept your distance, %s.", "...Лучше держись от меня подальше, %s."),
    ("hostile", "grumpy"): ("You again. Clear off.", "Опять ты. Проваливай."),
    ("hostile", "greedy"): ("Not a single emerald for you, thief.", "Ни одного изумруда тебе, воришка."),
    ("hostile", "brave"): ("I'm watching you, %s.", "Я слежу за тобой, %s."),
    ("neutral", "friendly"): ("Good day to you, stranger!", "Доброго дня, путник!"),
    ("neutral", "grumpy"): ("Hmph.", "Хм."),
    ("neutral", "greedy"): ("Buying or selling?", "Покупаешь или продаёшь?"),
    ("neutral", "brave"): ("Keep safe out there.", "Береги себя там, за околицей."),
    ("friendly", "friendly"): ("%s! So good to see you!", "%s! Как же я рад тебя видеть!"),
    ("friendly", "grumpy"): ("Oh, it's you. ...Good.", "А, это ты. ...Хорошо."),
    ("friendly", "greedy"): ("For you, a special price. Almost.", "Для тебя особая цена. Почти."),
    ("friendly", "brave"): ("Good to have you around, %s.", "Хорошо, что ты рядом, %s."),
    ("hero", "friendly"): ("Everyone, look! %s is here!", "Все сюда! %s пришёл!"),
    ("hero", "grumpy"): ("...Thank you. For everything.", "...Спасибо. За всё."),
    ("hero", "greedy"): ("Take whatever you need. Well, nearly.", "Бери, что нужно. Ну, почти."),
    ("hero", "brave"): ("If there's trouble, I'm with you, %s.", "Если будет беда, я с тобой, %s."),
}

EXTRA = {
    "itemGroup.dunesrelics.ash_and_ember": ("Ash & Ember", "Пепел и Угли"),
    "itemGroup.dunesrelics.living_world": ("Living World", "Живой мир"),
    "trim_material.dunesrelics.pearl": ("Pearl Material", "Материал: жемчуг"),
    "block.dunesrelics.cannon.empty": ("Load it with gunpowder and a cannonball first.", "Сначала зарядите пушку порохом и ядром."),
    "filled_map.dunesrelics.volcano": ("Volcano Map", "Карта к вулкану"),
    "filled_map.dunesrelics.pirate_ship": ("Pirate Ship Map", "Карта к пиратскому кораблю"),
    "filled_map.dunesrelics.village": ("Village Map", "Карта к деревне"),
    "item.dunesrelics.world_chronicle.cover": ("World Chronicle\n\nWhat the world remembers.",
                                               "Летопись мира\n\nВсё, что помнит мир."),
    "item.dunesrelics.world_chronicle.empty": ("Nothing has happened yet. Live here a while.",
                                               "Пока ничего не произошло. Поживите здесь немного."),
    "item.dunesrelics.world_chronicle.day": ("Day %s", "День %s"),
    "item.dunesrelics.world_chronicle.entries": ("%s entries", "Записей: %s"),
    "item.dunesrelics.tide_clock.high": ("High tide. It turns in about %s min.", "Прилив. Сменится примерно через %s мин."),
    "item.dunesrelics.tide_clock.low": ("Low tide: the beaches are bare. It turns in about %s min.",
                                        "Отлив: берега обнажились. Сменится примерно через %s мин."),
    "item.dunesrelics.tide_clock.rising": ("The tide is coming in (%s min).", "Вода прибывает (%s мин)."),
    "item.dunesrelics.tide_clock.falling": ("The tide is going out (%s min).", "Вода уходит (%s мин)."),
    "villager.dunesrelics.says": ("%s (%s): %s", "%s (%s): %s"),
    "villager.dunesrelics.missed": ("%s! We haven't seen you in days!", "%s! Тебя не было несколько дней!"),
    "villager.dunesrelics.enough": ("Thank you, but that's plenty for today.", "Спасибо, но на сегодня хватит."),
    "villager.dunesrelics.cheer": ("Hooray! You drove them off!", "Ура! Ты их прогнал!"),
    "chronicle.dunesrelics.stage.1": ("Around your home, nature is coming back: saplings take root and animals wander in.",
                                      "Вокруг твоего дома оживает природа: пускают корни саженцы, приходят звери."),
    "chronicle.dunesrelics.stage.2": ("Word of your home has spread. A trail has been worn to it.",
                                      "Слухи о твоём доме разошлись. К нему протоптали тропу."),
    "chronicle.dunesrelics.stage.3": ("Lamps now light the road to your home, and people have come to farm nearby.",
                                      "Дорогу к твоему дому осветили фонари, неподалёку поселились фермеры."),
    "chronicle.dunesrelics.stage.4": ("Your home has become a landmark. Settlers are looking for land nearby.",
                                      "Твой дом стал приметным местом. Поселенцы ищут землю поблизости."),
    "chronicle.dunesrelics.trail_joined": ("The trail from your home now reaches the village of %s.",
                                           "Тропа от твоего дома дошла до деревни %s."),
    "chronicle.dunesrelics.farm": ("A family set up a farm by the road to your home.",
                                   "У дороги к твоему дому семья разбила ферму."),
    "chronicle.dunesrelics.hamlet": ("Settlers founded the hamlet of %s near your home.",
                                     "Неподалёку от твоего дома поселенцы основали хутор %s."),
    "chronicle.dunesrelics.traveler": ("A traveller named %s came down the road.", "По дороге пришёл путник по имени %s."),
    "chronicle.dunesrelics.pirates": ("Pirates landed near %s!", "У деревни %s высадились пираты!"),
    "chronicle.dunesrelics.village.house": ("%s built a new house.", "В деревне %s построили новый дом."),
    "chronicle.dunesrelics.village.lamp": ("%s put up a new lamp post.", "В деревне %s поставили новый фонарь."),
    "chronicle.dunesrelics.village.field": ("%s ploughed a new field.", "В деревне %s распахали новое поле."),
    "chronicle.dunesrelics.village.well": ("%s dug a well.", "В деревне %s выкопали колодец."),
    "chronicle.dunesrelics.village.stall": ("%s opened a market stall.", "В деревне %s открылась рыночная лавка."),
    "chronicle.dunesrelics.village.windmill": ("%s built a windmill.", "В деревне %s построили ветряную мельницу."),
    "chronicle.dunesrelics.village.watchtower": ("%s put up a watchtower.", "В деревне %s поставили сторожевую башню."),
    "chronicle.dunesrelics.village.benches": ("%s put benches round the bell.", "В деревне %s поставили лавочки у колокола."),
    "chronicle.dunesrelics.village.flowers": ("%s planted a flower bed.", "В деревне %s разбили клумбу."),
    "chronicle.dunesrelics.village.repaired": ("%s rebuilt what had been destroyed.", "Деревня %s отстроила разрушенное."),
    "project.dunesrelics.house": ("a house", "дом"),
    "project.dunesrelics.lamp": ("a lamp post", "фонарь"),
    "project.dunesrelics.well": ("a well", "колодец"),
    "project.dunesrelics.stall": ("a market stall", "рыночный прилавок"),
    "project.dunesrelics.field": ("a field", "поле"),
    "project.dunesrelics.windmill": ("a windmill", "ветряная мельница"),
    "project.dunesrelics.watchtower": ("a watchtower", "сторожевая башня"),
    "project.dunesrelics.benches": ("benches", "лавочки"),
    "project.dunesrelics.flowers": ("a flower bed", "клумба"),
    "project.dunesrelics.hamlet": ("a hamlet", "хутор"),
    "entity.dunesrelics.village_worker.idle": ("Looking for a village that needs a pair of hands.",
                                                "Ищу деревню, где нужны рабочие руки."),
    "entity.dunesrelics.village_worker.repairing": ("Putting back what was destroyed: %s blocks to go.",
                                                     "Чиню разрушенное: осталось блоков — %s."),
    "entity.dunesrelics.village_worker.building": ("Building %s for %s: %s blocks to go.",
                                                    "Строю для деревни %2$s: %1$s, осталось блоков — %3$s."),
    "entity.dunesrelics.village_worker.resting": ("%s has all it needs for now.", "Сейчас у деревни %s всё есть."),
    "entity.dunesrelics.village_worker.waiting_wood": ("Waiting for the lumberjacks: we are out of wood.",
                                                        "Жду лесорубов: кончилось дерево."),
    "entity.dunesrelics.village_worker.waiting_stone": ("Waiting for the quarrymen: we are out of stone.",
                                                         "Жду каменотёсов: кончился камень."),
    "entity.dunesrelics.village_worker.lumber": ("The village has %s wood in store.", "На складе деревни дерева: %s."),
    "entity.dunesrelics.village_worker.quarry": ("The village has %s stone in store.", "На складе деревни камня: %s."),
    "chronicle.dunesrelics.worker_arrived.builder": ("The builder %s settled in %s.", "В деревню %2$s пришёл строитель %1$s."),
    "chronicle.dunesrelics.worker_arrived.lumberjack": ("The lumberjack %s settled in %s.", "В деревню %2$s пришёл лесоруб %1$s."),
    "chronicle.dunesrelics.worker_arrived.quarryman": ("The quarryman %s settled in %s.", "В деревню %2$s пришёл каменотёс %1$s."),
    "chronicle.dunesrelics.village.quarry": ("The quarrymen of %s opened a quarry.", "Каменотёсы деревни %s открыли карьер."),
    "chronicle.dunesrelics.village.storehouse": ("%s built a storehouse.", "В деревне %s построили склад."),
    "chronicle.dunesrelics.village.smithy": ("%s built a smithy.", "В деревне %s построили кузницу."),
    "chronicle.dunesrelics.village.chapel": ("%s built a chapel.", "В деревне %s построили часовню."),
    "chronicle.dunesrelics.daughter": ("Settlers from %s founded the hamlet of %s.", "Переселенцы из деревни %s основали хутор %s."),
    "chronicle.dunesrelics.road": ("A road now joins %s and %s.", "Деревни %s и %s соединила дорога."),
    "project.dunesrelics.storehouse": ("a storehouse", "склад"),
    "project.dunesrelics.smithy": ("a smithy", "кузница"),
    "project.dunesrelics.chapel": ("a chapel", "часовня"),
    "sign.dunesrelics.milestone.distance": ("%s m", "%s м"),
    "item.dunesrelics.request_note.from": ("From %s of %s", "От: %s, деревня %s"),
    "item.dunesrelics.request_note.bring": ("Bring %s × %s", "Принести: %2$s × %1$s"),
    "item.dunesrelics.request_note.hunt": ("Drive off the monsters round the village: %s of %s",
                                           "Прогнать монстров у деревни: %s из %s"),
    "item.dunesrelics.request_note.find": ("Find the cow that wandered off and bring her back",
                                           "Найти пропавшую корову и привести её обратно"),
    "item.dunesrelics.request_note.reward": ("Reward: %s emeralds", "Награда: изумрудов — %s"),
    "item.dunesrelics.request_note.done": ("Done! Go back to the villager for your reward.",
                                           "Готово! Вернитесь к жителю за наградой."),
    "villager.dunesrelics.request.bring": ("Could you help me out? I need a few things. I wrote them down for you.",
                                           "Не выручишь? Мне кое-что нужно, я записал."),
    "villager.dunesrelics.request.hunt": ("Monsters prowl round here at night. Could you drive them off?",
                                          "По ночам тут бродят чудища. Прогонишь их?"),
    "villager.dunesrelics.request.find": ("My cow has wandered off! Could you find her and bring her back?",
                                          "Моя корова ушла! Найдёшь её и приведёшь назад?"),
    "villager.dunesrelics.request.waiting": ("Not yet? I'll wait.", "Ещё не всё? Я подожду."),
    "villager.dunesrelics.request.thanks": ("Thank you! Here, %s emeralds.", "Спасибо! Держи изумруды: %s."),
    "villager.dunesrelics.news": ("Have you heard? %s", "Слыхал? %s"),
    "chronicle.dunesrelics.wedding": ("A wedding in %s: %s and %s.", "Свадьба в деревне %s: %s и %s."),
    "chronicle.dunesrelics.birth": ("A new little one, %s, in %s.", "Пополнение в деревне %2$s: малыш %1$s."),
    "name.dunesrelics.cow.0": ("Daisy", "Бурёнка"),
    "name.dunesrelics.cow.1": ("Buttercup", "Зорька"),
    "name.dunesrelics.cow.2": ("Bessie", "Пеструшка"),
    "name.dunesrelics.cow.3": ("Clover", "Ночка"),
    "name.dunesrelics.cow.4": ("Rosie", "Милка"),
    "name.dunesrelics.cow.5": ("Maisie", "Звёздочка"),
    "name.dunesrelics.cow.6": ("Bluebell", "Ромашка"),
    "name.dunesrelics.cow.7": ("Marigold", "Марта"),
    "commands.dunesrelics.livingworld.status": (
        "Region: %s blocks placed, %s days here, stage %s; trail %s blocks, farm %s, hamlet %s",
        "Регион: поставлено блоков %s, дней здесь %s, стадия %s; тропа %s блоков, ферма %s, хутор %s"),
    "commands.dunesrelics.livingworld.tide": ("Tide: %s (-1 low water, 1 high water)", "Прилив: %s (-1 отлив, 1 прилив)"),
    "commands.dunesrelics.livingworld.reacted": ("The world lived through %s more mornings.", "Прошло утр: %s."),
    "commands.dunesrelics.livingworld.hamlet": ("A hamlet was founded here: %s.", "Здесь основан хутор: %s."),
    "commands.dunesrelics.livingworld.no_room": ("There is no room for a hamlet here (it needs flat, natural, unbuilt ground).",
                                                 "Здесь нет места для хутора (нужна ровная нетронутая земля без построек)."),
    "commands.dunesrelics.livingworld.stage": ("This region now counts as stage %s.", "Этот регион теперь на стадии %s."),
    "commands.dunesrelics.livingworld.grew": ("The villages nearby took on a project.", "Ближайшие деревни взялись за стройку."),
    "commands.dunesrelics.livingworld.pirates": ("Pirates are landing!", "Высаживаются пираты!"),
    "commands.dunesrelics.livingworld.no_village": ("No village nearby.", "Поблизости нет деревни."),
    "commands.dunesrelics.livingworld.no_sea": ("That village is not by the sea.", "Эта деревня не у моря."),
    "gamerule.doWorldMemory": ("World memory (the world grows around players' homes)",
                               "Память мира (мир меняется вокруг домов игроков)"),
    "gamerule.doVillageGrowth": ("Villages grow", "Деревни растут"),
    "gamerule.doPirateRaids": ("Pirate raids", "Набеги пиратов"),
    "gamerule.doTides": ("Tides and currents", "Приливы и течения"),
}


def lang_tables():
    """Merges the update's translations into the four tables of lang.py (block, item, entity, extra)."""
    extra = dict(EXTRA)
    for i, (en, ru) in enumerate(VILLAGER_NAMES):
        extra["name.dunesrelics.villager.%d" % i] = (en, ru)
    for i, (en, ru) in enumerate(VILLAGE_NAMES):
        extra["name.dunesrelics.village.%d" % i] = (en, ru)
    for (tier, trait), texts in GREETINGS.items():
        extra["villager.dunesrelics.greet.%s.%s" % (tier, trait)] = texts
    per_trait = {
        "gift": [("Here, I saved this for you.", "Держи, я это для тебя отложил."),
                 ("Take it. Don't make a fuss.", "Бери. И не спорь."),
                 ("A gift. Free. Don't get used to it.", "Подарок. Бесплатно. Не привыкай."),
                 ("For the road. You'll need it.", "Тебе в дорогу. Пригодится.")],
        "thanks": [("Oh, how lovely! Thank you!", "Ой, какая прелесть! Спасибо!"),
                   ("...Fine. Thank you.", "...Ладно. Спасибо."),
                   ("Now that's what I call a gift!", "Вот это я понимаю подарок!"),
                   ("I'll remember this, friend.", "Я этого не забуду, друг.")],
        "angry": [("Why would you break that?!", "Зачем ты это сломал?!"),
                  ("Hey! That's ours!", "Эй! Это наше!"),
                  ("You'll pay for that!", "Ты за это заплатишь!"),
                  ("Stop right there!", "А ну стой!")],
    }
    for kind, lines in per_trait.items():
        for trait, texts in zip(TRAITS, lines):
            extra["villager.dunesrelics.%s.%s" % (kind, trait)] = texts
    notes = [("The note is faded: \"...the treasure lies where the palms...\"", "Записка выцвела: «...клад лежит там, где пальмы...»"),
             ("\"If you find this, we are safe. The sea was kind.\"", "«Если ты это нашёл, мы спаслись. Море было милостиво»."),
             ("A child's drawing of a ship with black sails.", "Детский рисунок корабля с чёрными парусами."),
             ("\"Beware the captain with the golden hat.\"", "«Берегись капитана в треуголке с золотом»."),
             ("\"The tide keeps its secrets for those who wait.\"", "«Прилив хранит тайны для тех, кто умеет ждать»."),
             ("Just one word: \"Home.\"", "Лишь одно слово: «Дом»."),]
    for i, texts in enumerate(notes):
        extra["item.dunesrelics.message_in_a_bottle.note.%d" % i] = texts
    return BLOCKS, ITEMS, ENTITIES, ADVANCEMENTS, extra

"""Generates every JSON file of Dunes & Relics: blockstates, block/item models, loot tables, recipes,
tags, worldgen, biome modifiers, advancements, trims, language files and the game test arena.

Vanilla blockstate/loot files are used as templates for the complex blocks (stairs, doors, walls...);
they are downloaded once from the misode/mcmeta mirror into tools/.cache.

Run from the repository root:  python3 tools/generate_data.py
"""
import gzip
import io
import json
import os
import shutil
import struct
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src/main/resources")
ASSETS = os.path.join(RES, "assets/dunesrelics")
DATA = os.path.join(RES, "data")
CACHE = os.path.join(ROOT, "tools/.cache/vanilla")
MOD = "dunesrelics"
MCMETA = "https://raw.githubusercontent.com/misode/mcmeta/1.20.1-{kind}/{kind}/minecraft/{path}.json"


def m(name):
    return "%s:%s" % (MOD, name)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as out:
        json.dump(obj, out, indent=2, ensure_ascii=False)
        out.write("\n")


def vanilla(kind, path):
    """kind is 'assets' or 'data'; path is relative to the minecraft namespace, without .json."""
    local = os.path.join(CACHE, path + ".json")
    if not os.path.exists(local):
        os.makedirs(os.path.dirname(local), exist_ok=True)
        url = MCMETA.format(kind=kind, path=path)
        with urllib.request.urlopen(url) as response, open(local, "wb") as out:
            out.write(response.read())
    with open(local, encoding="utf-8") as f:
        return f.read()


def vanilla_json(kind, path, replacements):
    text = vanilla(kind, path)
    for old, new in replacements:
        text = text.replace(old, new)
    return json.loads(text)


# ================================================================================================ block models

def blockstate(name, obj):
    write(os.path.join(ASSETS, "blockstates", name + ".json"), obj)


def block_model(name, obj):
    write(os.path.join(ASSETS, "models/block", name + ".json"), obj)


def item_model(name, obj):
    write(os.path.join(ASSETS, "models/item", name + ".json"), obj)


def tex(name):
    return m("block/" + name)


def simple_block_state(name, model=None):
    blockstate(name, {"variants": {"": {"model": model or m("block/" + name)}}})


def cube_all(name, texture=None, render_type=None):
    model = {"parent": "minecraft:block/cube_all", "textures": {"all": tex(texture or name)}}
    if render_type:
        model["render_type"] = render_type
    block_model(name, model)
    simple_block_state(name)
    item_model(name, {"parent": m("block/" + name)})


def pillar(name, side, end, horizontal=True):
    block_model(name, {"parent": "minecraft:block/cube_column", "textures": {"end": tex(end), "side": tex(side)}})
    if horizontal:
        block_model(name + "_horizontal", {"parent": "minecraft:block/cube_column_horizontal",
                                           "textures": {"end": tex(end), "side": tex(side)}})
    h = m("block/" + name + ("_horizontal" if horizontal else ""))
    blockstate(name, {"variants": {
        "axis=x": {"model": h, "x": 90, "y": 90},
        "axis=y": {"model": m("block/" + name)},
        "axis=z": {"model": h, "x": 90},
    }})
    item_model(name, {"parent": m("block/" + name)})


def stairs(name, texture, top=None, bottom=None):
    textures = {"bottom": tex(bottom or texture), "top": tex(top or texture), "side": tex(texture)}
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        block_model(name + suffix, {"parent": "minecraft:block/" + parent, "textures": textures})
    blockstate(name, vanilla_json("assets", "blockstates/oak_stairs", [("minecraft:block/oak_stairs", m("block/" + name))]))
    item_model(name, {"parent": m("block/" + name)})


def slab(name, texture, full_model):
    textures = {"bottom": tex(texture), "top": tex(texture), "side": tex(texture)}
    block_model(name, {"parent": "minecraft:block/slab", "textures": textures})
    block_model(name + "_top", {"parent": "minecraft:block/slab_top", "textures": textures})
    blockstate(name, {"variants": {
        "type=bottom": {"model": m("block/" + name)},
        "type=double": {"model": m("block/" + full_model)},
        "type=top": {"model": m("block/" + name + "_top")},
    }})
    item_model(name, {"parent": m("block/" + name)})


def wall(name, texture):
    t = {"wall": tex(texture)}
    block_model(name + "_post", {"parent": "minecraft:block/template_wall_post", "textures": t})
    block_model(name + "_side", {"parent": "minecraft:block/template_wall_side", "textures": t})
    block_model(name + "_side_tall", {"parent": "minecraft:block/template_wall_side_tall", "textures": t})
    block_model(name + "_inventory", {"parent": "minecraft:block/wall_inventory", "textures": t})
    blockstate(name, vanilla_json("assets", "blockstates/cobblestone_wall",
                                  [("minecraft:block/cobblestone_wall", m("block/" + name))]))
    item_model(name, {"parent": m("block/" + name + "_inventory")})


def fence(name, texture):
    t = {"texture": tex(texture)}
    block_model(name + "_post", {"parent": "minecraft:block/fence_post", "textures": t})
    block_model(name + "_side", {"parent": "minecraft:block/fence_side", "textures": t})
    block_model(name + "_inventory", {"parent": "minecraft:block/fence_inventory", "textures": t})
    blockstate(name, vanilla_json("assets", "blockstates/oak_fence", [("minecraft:block/oak_fence", m("block/" + name))]))
    item_model(name, {"parent": m("block/" + name + "_inventory")})


def fence_gate(name, texture):
    t = {"texture": tex(texture)}
    for suffix, parent in (("", "template_fence_gate"), ("_open", "template_fence_gate_open"),
                           ("_wall", "template_fence_gate_wall"), ("_wall_open", "template_fence_gate_wall_open")):
        block_model(name + suffix, {"parent": "minecraft:block/" + parent, "textures": t})
    blockstate(name, vanilla_json("assets", "blockstates/oak_fence_gate",
                                  [("minecraft:block/oak_fence_gate", m("block/" + name))]))
    item_model(name, {"parent": m("block/" + name)})


def door(name):
    t = {"bottom": tex(name + "_bottom"), "top": tex(name + "_top")}
    for half in ("bottom", "top"):
        for hinge in ("left", "right"):
            for opened in ("", "_open"):
                suffix = "_%s_%s%s" % (half, hinge, opened)
                block_model(name + suffix, {"parent": "minecraft:block/door" + suffix, "textures": t,
                                            "render_type": "minecraft:cutout"})
    blockstate(name, vanilla_json("assets", "blockstates/oak_door", [("minecraft:block/oak_door", m("block/" + name))]))
    item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": m("item/" + name)}})


def trapdoor(name):
    t = {"texture": tex(name)}
    for suffix in ("bottom", "top", "open"):
        block_model(name + "_" + suffix, {"parent": "minecraft:block/template_orientable_trapdoor_" + suffix,
                                          "textures": t, "render_type": "minecraft:cutout"})
    blockstate(name, vanilla_json("assets", "blockstates/oak_trapdoor", [("minecraft:block/oak_trapdoor", m("block/" + name))]))
    item_model(name, {"parent": m("block/" + name + "_bottom")})


def button(name, texture):
    t = {"texture": tex(texture)}
    block_model(name, {"parent": "minecraft:block/button", "textures": t})
    block_model(name + "_pressed", {"parent": "minecraft:block/button_pressed", "textures": t})
    block_model(name + "_inventory", {"parent": "minecraft:block/button_inventory", "textures": t})
    blockstate(name, vanilla_json("assets", "blockstates/oak_button", [("minecraft:block/oak_button", m("block/" + name))]))
    item_model(name, {"parent": m("block/" + name + "_inventory")})


def pressure_plate(name, texture):
    t = {"texture": tex(texture)}
    block_model(name, {"parent": "minecraft:block/pressure_plate_up", "textures": t})
    block_model(name + "_down", {"parent": "minecraft:block/pressure_plate_down", "textures": t})
    blockstate(name, vanilla_json("assets", "blockstates/oak_pressure_plate",
                                  [("minecraft:block/oak_pressure_plate", m("block/" + name))]))
    item_model(name, {"parent": m("block/" + name)})


def cross(name, texture=None, item=True):
    block_model(name, {"parent": "minecraft:block/cross", "textures": {"cross": tex(texture or name)},
                       "render_type": "minecraft:cutout"})
    if item:
        item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": tex(texture or name)}})


def potted(name, plant_texture):
    block_model(name, {"parent": "minecraft:block/flower_pot_cross", "textures": {"plant": tex(plant_texture)},
                       "render_type": "minecraft:cutout"})
    simple_block_state(name)


def generate_block_assets():
    # limestone
    cube_all("limestone")
    stairs("limestone_stairs", "limestone")
    slab("limestone_slab", "limestone", "limestone")
    wall("limestone_wall", "limestone")
    cube_all("polished_limestone")
    stairs("polished_limestone_stairs", "polished_limestone")
    slab("polished_limestone_slab", "polished_limestone", "polished_limestone")
    wall("polished_limestone_wall", "polished_limestone")
    cube_all("limestone_bricks")
    stairs("limestone_brick_stairs", "limestone_bricks")
    slab("limestone_brick_slab", "limestone_bricks", "limestone_bricks")
    wall("limestone_brick_wall", "limestone_bricks")
    cube_all("cracked_limestone_bricks")
    cube_all("chiseled_limestone_bricks")
    pillar("limestone_pillar", "limestone_pillar", "limestone_pillar_top")
    cube_all("gilded_limestone")

    # amber & bronze
    cube_all("amber_ore")
    cube_all("amber_block", render_type="minecraft:translucent")
    cube_all("amber_lamp")
    cube_all("bronze_block")

    # palm
    pillar("palm_log", "palm_log", "palm_log_top")
    pillar("stripped_palm_log", "stripped_palm_log", "stripped_palm_log_top")
    pillar("palm_wood", "palm_log", "palm_log", horizontal=False)
    pillar("stripped_palm_wood", "stripped_palm_log", "stripped_palm_log", horizontal=False)
    cube_all("palm_planks")
    stairs("palm_stairs", "palm_planks")
    slab("palm_slab", "palm_planks", "palm_planks")
    fence("palm_fence", "palm_planks")
    fence_gate("palm_fence_gate", "palm_planks")
    door("palm_door")
    trapdoor("palm_trapdoor")
    button("palm_button", "palm_planks")
    pressure_plate("palm_pressure_plate", "palm_planks")
    block_model("palm_leaves", {"parent": "minecraft:block/leaves", "textures": {"all": tex("palm_leaves")},
                                "render_type": "minecraft:cutout_mipped"})
    simple_block_state("palm_leaves")
    item_model("palm_leaves", {"parent": m("block/palm_leaves")})
    cross("palm_sapling")
    simple_block_state("palm_sapling")
    potted("potted_palm_sapling", "palm_sapling")

    # plants
    cross("dune_grass")
    simple_block_state("dune_grass")
    cross("desert_rose")
    simple_block_state("desert_rose")
    potted("potted_desert_rose", "desert_rose")
    for stage in range(4):
        cross("aloe_vera_stage%d" % stage, item=False)
    blockstate("aloe_vera", {"variants": {"age=%d" % a: {"model": m("block/aloe_vera_stage%d" % a)} for a in range(4)}})
    item_model("aloe_vera", {"parent": "minecraft:item/generated", "textures": {"layer0": tex("aloe_vera_stage2")}})
    cross("cattail_bottom", item=False)
    cross("cattail_top", item=False)
    blockstate("cattail", {"variants": {"half=lower": {"model": m("block/cattail_bottom")},
                                        "half=upper": {"model": m("block/cattail_top")}}})
    item_model("cattail", {"parent": "minecraft:item/generated", "textures": {"layer0": m("item/cattail")}})

    # quicksand
    cube_all("quicksand")

    # ancient urn: a round-bellied jar with a narrow neck and a rim
    side, top = tex("ancient_urn_side"), tex("ancient_urn_top")

    def box(frm, to, uv_side, uv_top, uv_bottom=None):
        faces = {d: {"uv": uv_side, "texture": "#side"} for d in ("north", "south", "east", "west")}
        faces["up"] = {"uv": uv_top, "texture": "#top"}
        faces["down"] = {"uv": uv_bottom or uv_top, "texture": "#top"}
        return {"from": frm, "to": to, "faces": faces}

    block_model("ancient_urn", {
        "parent": "minecraft:block/block",
        "textures": {"side": side, "top": top, "particle": side},
        "elements": [
            box([3, 0, 3], [13, 11, 13], [3, 4, 13, 15], [3, 3, 13, 13]),
            box([5, 11, 5], [11, 13, 11], [5, 1, 11, 3], [5, 5, 11, 11]),
            box([4, 13, 4], [12, 15, 12], [4, 0, 12, 2], [4, 4, 12, 12]),
        ],
    })
    blockstate("ancient_urn", {"variants": {"sealed=false": {"model": m("block/ancient_urn")},
                                            "sealed=true": {"model": m("block/ancient_urn")}}})
    item_model("ancient_urn", {"parent": m("block/ancient_urn")})

    # sarcophagus: a stone coffin with a gilded lid; the lid's head points to FACING
    block_model("sarcophagus", {
        "parent": "minecraft:block/block",
        "textures": {"top": tex("sarcophagus_top"), "side": tex("sarcophagus_side"),
                     "bottom": tex("sarcophagus_bottom"), "particle": tex("sarcophagus_side")},
        "elements": [
            {"from": [1, 0, 1], "to": [15, 11, 15], "faces": {
                "north": {"uv": [1, 5, 15, 16], "texture": "#side"},
                "south": {"uv": [1, 5, 15, 16], "texture": "#side"},
                "east": {"uv": [1, 5, 15, 16], "texture": "#side"},
                "west": {"uv": [1, 5, 15, 16], "texture": "#side"},
                "down": {"uv": [1, 1, 15, 15], "texture": "#bottom", "cullface": "down"}}},
            {"from": [0.5, 11, 0.5], "to": [15.5, 13, 15.5], "faces": {
                "north": {"uv": [0, 0, 16, 2], "texture": "#side"},
                "south": {"uv": [0, 0, 16, 2], "texture": "#side"},
                "east": {"uv": [0, 0, 16, 2], "texture": "#side"},
                "west": {"uv": [0, 0, 16, 2], "texture": "#side"},
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "down": {"uv": [0, 0, 16, 16], "texture": "#bottom"}}},
        ],
    })
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    blockstate("sarcophagus", {"variants": {
        "facing=%s,sealed=%s" % (f, s): ({"model": m("block/sarcophagus"), "y": r} if r else {"model": m("block/sarcophagus")})
        for f, r in rot.items() for s in ("false", "true")}})
    item_model("sarcophagus", {"parent": m("block/sarcophagus")})


def generate_item_models():
    generated = ["amber", "bronze_ingot", "bronze_nugget", "linen", "scorpion_stinger", "vulture_feather",
                 "bronze_helmet", "bronze_chestplate", "bronze_leggings", "bronze_boots", "amber_goggles",
                 "dates", "honeyed_dates", "flatbread", "aloe_leaf", "bandage", "pharaoh_armor_trim_smithing_template"]
    handheld = ["bronze_khopesh", "bronze_shovel", "bronze_pickaxe", "bronze_axe", "bronze_hoe", "scepter_of_sands"]
    for name in generated:
        item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": m("item/" + name)}})
    for name in handheld:
        item_model(name, {"parent": "minecraft:item/handheld", "textures": {"layer0": m("item/" + name)}})
    for mob in ("mummy", "pharaoh", "scorpion", "scarab", "meerkat", "vulture"):
        item_model(mob + "_spawn_egg", {"parent": "minecraft:item/template_spawn_egg"})


# ================================================================================================ loot tables

def loot(path, obj):
    write(os.path.join(DATA, MOD, "loot_tables", path + ".json"), obj)


def item_entry(name, count=None, weight=None, functions=(), conditions=()):
    entry = {"type": "minecraft:item", "name": name}
    fns = list(functions)
    if count is not None:
        if isinstance(count, tuple):
            fns.insert(0, {"function": "minecraft:set_count",
                           "count": {"type": "minecraft:uniform", "min": float(count[0]), "max": float(count[1])},
                           "add": False})
        else:
            fns.insert(0, {"function": "minecraft:set_count", "count": float(count), "add": False})
    if fns:
        entry["functions"] = fns
    if weight is not None:
        entry["weight"] = weight
    if conditions:
        entry["conditions"] = list(conditions)
    return entry


def pool(entries, rolls=1.0, conditions=()):
    p = {"rolls": rolls, "bonus_rolls": 0.0, "entries": entries}
    if conditions:
        p["conditions"] = list(conditions)
    return p


SURVIVES = {"condition": "minecraft:survives_explosion"}
SHEARS = {"condition": "minecraft:match_tool", "predicate": {"items": ["minecraft:shears"]}}
SILK = {"condition": "minecraft:match_tool", "predicate": {"enchantments": [{"enchantment": "minecraft:silk_touch",
                                                                              "levels": {"min": 1}}]}}


def self_drop(name):
    loot("blocks/" + name, {"type": "minecraft:block", "pools": [pool([item_entry(m(name))], conditions=[SURVIVES])],
                            "random_sequence": m("blocks/" + name)})


def generate_loot():
    simple = ["limestone", "limestone_stairs", "limestone_wall", "polished_limestone", "polished_limestone_stairs",
              "polished_limestone_wall", "limestone_bricks", "limestone_brick_stairs", "limestone_brick_wall",
              "cracked_limestone_bricks", "chiseled_limestone_bricks", "limestone_pillar", "gilded_limestone",
              "amber_block", "amber_lamp", "bronze_block", "palm_log", "palm_wood", "stripped_palm_log",
              "stripped_palm_wood", "palm_planks", "palm_stairs", "palm_fence", "palm_fence_gate", "palm_trapdoor",
              "palm_button", "palm_pressure_plate", "palm_sapling", "desert_rose", "quicksand", "sarcophagus"]
    for name in simple:
        self_drop(name)
    for name in ("limestone_slab", "polished_limestone_slab", "limestone_brick_slab", "palm_slab"):
        loot("blocks/" + name, vanilla_json("data", "loot_tables/blocks/oak_slab",
                                            [("minecraft:oak_slab", m(name)), ("minecraft:blocks/oak_slab", m("blocks/" + name))]))
    loot("blocks/palm_door", vanilla_json("data", "loot_tables/blocks/oak_door",
                                          [("minecraft:oak_door", m("palm_door")), ("minecraft:blocks/oak_door", m("blocks/palm_door"))]))
    leaves = vanilla_json("data", "loot_tables/blocks/oak_leaves",
                          [("minecraft:oak_leaves", m("palm_leaves")), ("minecraft:oak_sapling", m("palm_sapling")),
                           ("minecraft:apple", m("dates")), ("minecraft:blocks/oak_leaves", m("blocks/palm_leaves"))])
    # Dates are far more common than apples.
    for p in leaves["pools"]:
        for entry in p["entries"]:
            if entry.get("name") == m("dates"):
                for c in entry["conditions"]:
                    if c["condition"] == "minecraft:table_bonus":
                        c["chances"] = [0.04, 0.05, 0.0625, 0.08, 0.12]
    loot("blocks/palm_leaves", leaves)
    ore = vanilla_json("data", "loot_tables/blocks/diamond_ore",
                       [("minecraft:diamond_ore", m("amber_ore")), ("minecraft:diamond", m("amber")),
                        ("minecraft:blocks/diamond_ore", m("blocks/amber_ore"))])
    # One or two amber per ore, before fortune.
    for p in ore["pools"]:
        for alt in p["entries"]:
            for child in alt.get("children", []):
                if child.get("name") == m("amber"):
                    child["functions"].insert(0, {"function": "minecraft:set_count", "add": False,
                                                  "count": {"type": "minecraft:uniform", "min": 1.0, "max": 2.0}})
    loot("blocks/amber_ore", ore)
    for name, plant in (("potted_palm_sapling", "palm_sapling"), ("potted_desert_rose", "desert_rose")):
        loot("blocks/" + name, vanilla_json("data", "loot_tables/blocks/potted_oak_sapling",
                                            [("minecraft:oak_sapling", m(plant)),
                                             ("minecraft:blocks/potted_oak_sapling", m("blocks/" + name))]))
    loot("blocks/cattail", vanilla_json("data", "loot_tables/blocks/sunflower",
                                        [("minecraft:sunflower", m("cattail")), ("minecraft:blocks/sunflower", m("blocks/cattail"))]))
    loot("blocks/dune_grass", {"type": "minecraft:block", "pools": [
        pool([item_entry(m("dune_grass"))], conditions=[SHEARS])], "random_sequence": m("blocks/dune_grass")})
    ripe = {"condition": "minecraft:block_state_property", "block": m("aloe_vera"), "properties": {"age": "3"}}
    loot("blocks/aloe_vera", {"type": "minecraft:block", "pools": [
        pool([item_entry(m("aloe_vera"))], conditions=[SURVIVES]),
        pool([item_entry(m("aloe_leaf"), (1, 2), functions=[{"function": "minecraft:explosion_decay"}])], conditions=[ripe]),
    ], "random_sequence": m("blocks/aloe_vera")})

    sealed = lambda v: {"condition": "minecraft:block_state_property", "block": m("ancient_urn"), "properties": {"sealed": v}}
    loot("blocks/ancient_urn", {"type": "minecraft:block", "pools": [
        pool([item_entry(m("ancient_urn"))], conditions=[sealed("false"), SURVIVES]),
        pool([
            item_entry("minecraft:gold_nugget", (2, 6), weight=20),
            item_entry(m("amber"), (1, 2), weight=14),
            item_entry(m("bronze_nugget"), (2, 5), weight=12),
            item_entry(m("dates"), (1, 3), weight=12),
            item_entry("minecraft:bone", (1, 3), weight=12),
            item_entry(m("linen"), (1, 2), weight=8),
            item_entry("minecraft:arrow", (2, 6), weight=8),
            item_entry("minecraft:emerald", 1, weight=4),
            item_entry("minecraft:gold_ingot", 1, weight=3),
        ], rolls={"type": "minecraft:uniform", "min": 1.0, "max": 3.0}, conditions=[sealed("true")]),
    ], "random_sequence": m("blocks/ancient_urn")})

    looting = lambda lo, hi: {"function": "minecraft:looting_enchant", "count": {"type": "minecraft:uniform", "min": float(lo), "max": float(hi)}}
    by_player = {"condition": "minecraft:killed_by_player"}
    loot("entities/mummy", {"type": "minecraft:entity", "pools": [
        pool([item_entry("minecraft:rotten_flesh", (0, 2), functions=[looting(0, 1)])]),
        pool([item_entry(m("linen"), (0, 2), functions=[looting(0, 1)])]),
        pool([item_entry("minecraft:gold_nugget", (1, 3)), item_entry(m("amber"))],
             conditions=[by_player, {"condition": "minecraft:random_chance_with_looting", "chance": 0.05,
                                     "looting_multiplier": 0.02}]),
    ], "random_sequence": m("entities/mummy")})
    loot("entities/pharaoh", {"type": "minecraft:entity", "pools": [
        pool([item_entry(m("scepter_of_sands"))]),
        pool([item_entry(m("pharaoh_armor_trim_smithing_template"))]),
        pool([item_entry("minecraft:gold_ingot", (3, 6))]),
        pool([item_entry(m("amber"), (3, 6))]),
        pool([item_entry(m("linen"), (4, 8))]),
    ], "random_sequence": m("entities/pharaoh")})
    loot("entities/scorpion", {"type": "minecraft:entity", "pools": [
        pool([item_entry(m("scorpion_stinger"), (0, 1), functions=[looting(0, 1)])]),
    ], "random_sequence": m("entities/scorpion")})
    loot("entities/scarab", {"type": "minecraft:entity", "pools": [
        pool([item_entry("minecraft:gold_nugget", (1, 2))],
             conditions=[{"condition": "minecraft:random_chance_with_looting", "chance": 0.2, "looting_multiplier": 0.05}]),
    ], "random_sequence": m("entities/scarab")})
    loot("entities/meerkat", {"type": "minecraft:entity", "pools": [], "random_sequence": m("entities/meerkat")})
    loot("entities/vulture", {"type": "minecraft:entity", "pools": [
        pool([item_entry("minecraft:feather", (1, 3), functions=[looting(0, 1)])]),
        pool([item_entry(m("vulture_feather"), (0, 1), functions=[looting(0, 1)])]),
    ], "random_sequence": m("entities/vulture")})

    enchant = {"function": "minecraft:enchant_randomly"}
    loot("chests/ancient_ruins", {"type": "minecraft:chest", "pools": [
        pool([
            item_entry(m("amber"), (1, 3), weight=20),
            item_entry("minecraft:gold_nugget", (2, 7), weight=20),
            item_entry(m("dates"), (2, 5), weight=15),
            item_entry("minecraft:bone", (1, 4), weight=15),
            item_entry(m("linen"), (1, 3), weight=12),
            item_entry(m("bronze_nugget"), (2, 6), weight=12),
            item_entry(m("bronze_ingot"), (1, 3), weight=10),
            item_entry("minecraft:arrow", (2, 8), weight=10),
            item_entry(m("bandage"), (1, 2), weight=6),
            item_entry("minecraft:gold_ingot", (1, 2), weight=5),
            item_entry("minecraft:emerald", (1, 2), weight=4),
        ], rolls={"type": "minecraft:uniform", "min": 3.0, "max": 6.0}),
        pool([
            {"type": "minecraft:empty", "weight": 10},
            item_entry(m("bronze_khopesh"), weight=3),
            item_entry(m("bronze_pickaxe"), weight=2),
            item_entry(m("amber_goggles"), weight=2),
            item_entry("minecraft:book", weight=2, functions=[enchant]),
            item_entry("minecraft:golden_apple", weight=1),
            item_entry(m("pharaoh_armor_trim_smithing_template"), weight=1),
        ]),
    ], "random_sequence": m("chests/ancient_ruins")})
    loot("chests/ancient_tomb", {"type": "minecraft:chest", "pools": [
        pool([
            item_entry("minecraft:gold_ingot", (2, 5), weight=15),
            item_entry(m("amber"), (2, 5), weight=15),
            item_entry(m("bronze_ingot"), (2, 5), weight=12),
            item_entry(m("linen"), (2, 4), weight=10),
            item_entry(m("dates"), (2, 5), weight=10),
            item_entry(m("bandage"), (1, 3), weight=8),
            item_entry("minecraft:emerald", (1, 3), weight=8),
            item_entry(m("honeyed_dates"), (1, 3), weight=6),
            item_entry("minecraft:golden_carrot", (1, 3), weight=4),
            item_entry("minecraft:diamond", (1, 2), weight=4),
        ], rolls={"type": "minecraft:uniform", "min": 4.0, "max": 7.0}),
        pool([
            item_entry(m("bronze_helmet"), weight=3),
            item_entry(m("bronze_chestplate"), weight=3),
            item_entry(m("bronze_leggings"), weight=3),
            item_entry(m("bronze_boots"), weight=3),
            item_entry("minecraft:book", weight=4, functions=[enchant]),
            item_entry("minecraft:golden_apple", weight=3),
            item_entry(m("pharaoh_armor_trim_smithing_template"), weight=3),
            item_entry(m("amber_goggles"), weight=2),
            item_entry("minecraft:saddle", weight=2),
            item_entry("minecraft:name_tag", weight=2),
            item_entry("minecraft:enchanted_golden_apple", weight=1),
        ], rolls={"type": "minecraft:uniform", "min": 1.0, "max": 2.0}),
    ], "random_sequence": m("chests/ancient_tomb")})


# ================================================================================================ recipes

def recipe(name, obj):
    write(os.path.join(DATA, MOD, "recipes", name + ".json"), obj)


def ing(value):
    return {"tag": value[1:]} if value.startswith("#") else {"item": value}


def shaped(name, pattern, key, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
                  "key": {k: ing(v) for k, v in key.items()}, "result": {"item": result, "count": count}})


def shapeless(name, ingredients, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category,
                  "ingredients": [ing(i) for i in ingredients], "result": {"item": result, "count": count}})


def cooking(name, ingredient, result, xp, kinds=("smelting",), time=200, category="misc"):
    for kind in kinds:
        suffix = "" if kind == "smelting" else "_from_" + kind
        recipe(name + suffix, {"type": "minecraft:" + kind, "category": category, "ingredient": ing(ingredient),
                               "result": result, "experience": xp,
                               "cookingtime": time if kind in ("smelting", "campfire_cooking") else time // 2})


def stonecut(source, result, count=1):
    recipe("%s_from_%s_stonecutting" % (result.split(":")[1], source.split(":")[1]),
           {"type": "minecraft:stonecutting", "ingredient": ing(source), "result": result, "count": count})


def generate_recipes():
    B = "building"
    # limestone family
    families = [("limestone", "limestone_stairs", "limestone_slab", "limestone_wall"),
                ("polished_limestone", "polished_limestone_stairs", "polished_limestone_slab", "polished_limestone_wall"),
                ("limestone_bricks", "limestone_brick_stairs", "limestone_brick_slab", "limestone_brick_wall")]
    for base, st, sl, wa in families:
        shaped(st, ["#  ", "## ", "###"], {"#": m(base)}, m(st), 4, B)
        shaped(sl, ["###"], {"#": m(base)}, m(sl), 6, B)
        shaped(wa, ["###", "###"], {"#": m(base)}, m(wa), 6, B)
    shaped("polished_limestone", ["##", "##"], {"#": m("limestone")}, m("polished_limestone"), 4, B)
    shaped("limestone_bricks", ["##", "##"], {"#": m("polished_limestone")}, m("limestone_bricks"), 4, B)
    shaped("chiseled_limestone_bricks", ["#", "#"], {"#": m("limestone_brick_slab")}, m("chiseled_limestone_bricks"), 1, B)
    shaped("limestone_pillar", ["#", "#"], {"#": m("polished_limestone")}, m("limestone_pillar"), 2, B)
    shaped("gilded_limestone", ["###", "#G#", "###"], {"#": m("polished_limestone"), "G": "minecraft:gold_ingot"},
           m("gilded_limestone"), 8, B)
    cooking("cracked_limestone_bricks", m("limestone_bricks"), m("cracked_limestone_bricks"), 0.1, category="blocks")
    cutting = {
        "limestone": ["limestone_stairs", ("limestone_slab", 2), "limestone_wall", "polished_limestone",
                      "polished_limestone_stairs", ("polished_limestone_slab", 2), "polished_limestone_wall",
                      "limestone_bricks", "limestone_brick_stairs", ("limestone_brick_slab", 2), "limestone_brick_wall",
                      "chiseled_limestone_bricks", "limestone_pillar"],
        "polished_limestone": ["polished_limestone_stairs", ("polished_limestone_slab", 2), "polished_limestone_wall",
                               "limestone_bricks", "limestone_brick_stairs", ("limestone_brick_slab", 2),
                               "limestone_brick_wall", "chiseled_limestone_bricks", "limestone_pillar"],
        "limestone_bricks": ["limestone_brick_stairs", ("limestone_brick_slab", 2), "limestone_brick_wall",
                             "chiseled_limestone_bricks"],
    }
    for source, results in cutting.items():
        for r in results:
            name, count = (r, 1) if isinstance(r, str) else r
            stonecut(m(source), m(name), count)

    # palm wood
    shapeless("palm_planks", ["#" + m("palm_logs")], m("palm_planks"), 4, B)
    shaped("palm_wood", ["##", "##"], {"#": m("palm_log")}, m("palm_wood"), 3, B)
    shaped("stripped_palm_wood", ["##", "##"], {"#": m("stripped_palm_log")}, m("stripped_palm_wood"), 3, B)
    P = m("palm_planks")
    shaped("palm_stairs", ["#  ", "## ", "###"], {"#": P}, m("palm_stairs"), 4, B)
    shaped("palm_slab", ["###"], {"#": P}, m("palm_slab"), 6, B)
    shaped("palm_fence", ["W#W", "W#W"], {"W": P, "#": "minecraft:stick"}, m("palm_fence"), 3)
    shaped("palm_fence_gate", ["#W#", "#W#"], {"W": P, "#": "minecraft:stick"}, m("palm_fence_gate"), 1, "redstone")
    shaped("palm_door", ["##", "##", "##"], {"#": P}, m("palm_door"), 3, "redstone")
    shaped("palm_trapdoor", ["###", "###"], {"#": P}, m("palm_trapdoor"), 2, "redstone")
    shapeless("palm_button", [P], m("palm_button"), 1, "redstone")
    shaped("palm_pressure_plate", ["##"], {"#": P}, m("palm_pressure_plate"), 1, "redstone")

    # amber
    shaped("amber_block", ["###", "###", "###"], {"#": m("amber")}, m("amber_block"), 1, B)
    shapeless("amber_from_amber_block", [m("amber_block")], m("amber"), 9)
    shaped("amber_lamp", [" A ", "ATA", " A "], {"A": m("amber"), "T": "minecraft:torch"}, m("amber_lamp"), 1, "redstone")
    cooking("amber_from_smelting_amber_ore", m("amber_ore"), m("amber"), 0.7, ("smelting", "blasting"))
    shaped("amber_goggles", ["LLL", "A A"], {"L": "minecraft:leather", "A": m("amber")}, m("amber_goggles"), 1, "equipment")

    # bronze
    shapeless("bronze_ingot", ["minecraft:copper_ingot", "minecraft:copper_ingot", "minecraft:copper_ingot", m("amber")],
              m("bronze_ingot"), 2)
    shaped("bronze_block", ["###", "###", "###"], {"#": m("bronze_ingot")}, m("bronze_block"), 1, B)
    shapeless("bronze_ingot_from_bronze_block", [m("bronze_block")], m("bronze_ingot"), 9)
    shaped("bronze_ingot_from_nuggets", ["###", "###", "###"], {"#": m("bronze_nugget")}, m("bronze_ingot"), 1)
    shapeless("bronze_nugget", [m("bronze_ingot")], m("bronze_nugget"), 9)
    I, S = m("bronze_ingot"), "minecraft:stick"
    E = "equipment"
    shaped("bronze_khopesh", ["II", " I", " S"], {"I": I, "S": S}, m("bronze_khopesh"), 1, E)
    shaped("bronze_shovel", ["I", "S", "S"], {"I": I, "S": S}, m("bronze_shovel"), 1, E)
    shaped("bronze_pickaxe", ["III", " S ", " S "], {"I": I, "S": S}, m("bronze_pickaxe"), 1, E)
    shaped("bronze_axe", ["II", "IS", " S"], {"I": I, "S": S}, m("bronze_axe"), 1, E)
    shaped("bronze_hoe", ["II", " S", " S"], {"I": I, "S": S}, m("bronze_hoe"), 1, E)
    shaped("bronze_helmet", ["III", "I I"], {"I": I}, m("bronze_helmet"), 1, E)
    shaped("bronze_chestplate", ["I I", "III", "III"], {"I": I}, m("bronze_chestplate"), 1, E)
    shaped("bronze_leggings", ["III", "I I", "I I"], {"I": I}, m("bronze_leggings"), 1, E)
    shaped("bronze_boots", ["I I", "I I"], {"I": I}, m("bronze_boots"), 1, E)
    for gear in ("khopesh", "shovel", "pickaxe", "axe", "hoe", "helmet", "chestplate", "leggings", "boots"):
        cooking("bronze_nugget_from_bronze_" + gear, m("bronze_" + gear), m("bronze_nugget"), 0.1, ("smelting", "blasting"))

    # food & remedies
    shapeless("honeyed_dates", [m("dates"), m("dates"), m("dates"), "minecraft:honey_bottle"], m("honeyed_dates"), 3)
    shapeless("flatbread", ["minecraft:wheat", "minecraft:wheat", m("dates")], m("flatbread"), 1)
    shapeless("bandage", [m("linen"), m("linen"), m("aloe_leaf")], m("bandage"), 2)

    # misc
    shapeless("quicksand", ["minecraft:sand", "minecraft:sand", "minecraft:sand", "minecraft:sand",
                            "minecraft:water_bucket"], m("quicksand"), 4, B)
    shaped("ancient_urn", ["T T", "T T", " T "], {"T": "minecraft:terracotta"}, m("ancient_urn"), 1, "building")
    shaped("sarcophagus", ["GPG", "PLP", "PPP"], {"G": "minecraft:gold_ingot", "P": m("polished_limestone"),
                                                 "L": m("linen")}, m("sarcophagus"), 1, "building")
    shapeless("pink_dye_from_desert_rose", [m("desert_rose")], "minecraft:pink_dye", 1)
    shapeless("brown_dye_from_cattail", [m("cattail")], "minecraft:brown_dye", 1)
    shaped("pharaoh_armor_trim_smithing_template", ["DTD", "DLD", "DDD"],
           {"D": "minecraft:diamond", "T": m("pharaoh_armor_trim_smithing_template"), "L": m("limestone")},
           m("pharaoh_armor_trim_smithing_template"), 2)
    recipe("pharaoh_armor_trim_smithing_template_smithing_trim", {
        "type": "minecraft:smithing_trim",
        "template": {"item": m("pharaoh_armor_trim_smithing_template")},
        "base": {"tag": "minecraft:trimmable_armor"},
        "addition": {"tag": "minecraft:trim_materials"}})


# ================================================================================================ tags

def tag(registry, namespace, name, values, replace=False):
    """Writes a tag file; calling it again for the same tag appends to it (both updates share vanilla tags)."""
    path = os.path.join(DATA, namespace, "tags", registry, name + ".json")
    if os.path.exists(path):
        with open(path, encoding="utf-8") as f:
            old = json.load(f)["values"]
        values = old + [v for v in values if v not in old]
    write(path, {"replace": replace, "values": values})


LIMESTONE_BLOCKS = ["limestone", "limestone_stairs", "limestone_slab", "limestone_wall", "polished_limestone",
                    "polished_limestone_stairs", "polished_limestone_slab", "polished_limestone_wall",
                    "limestone_bricks", "limestone_brick_stairs", "limestone_brick_slab", "limestone_brick_wall",
                    "cracked_limestone_bricks", "chiseled_limestone_bricks", "limestone_pillar", "gilded_limestone"]
PALM_BLOCKS = ["palm_log", "palm_wood", "stripped_palm_log", "stripped_palm_wood", "palm_planks", "palm_stairs",
               "palm_slab", "palm_fence", "palm_fence_gate", "palm_door", "palm_trapdoor", "palm_button",
               "palm_pressure_plate"]


def generate_tags():
    mm = lambda names: [m(n) for n in names]
    both = {
        ("minecraft", "planks"): ["palm_planks"],
        ("minecraft", "wooden_stairs"): ["palm_stairs"],
        ("minecraft", "wooden_slabs"): ["palm_slab"],
        ("minecraft", "wooden_fences"): ["palm_fence"],
        ("minecraft", "fence_gates"): ["palm_fence_gate"],
        ("minecraft", "wooden_doors"): ["palm_door"],
        ("minecraft", "wooden_trapdoors"): ["palm_trapdoor"],
        ("minecraft", "wooden_buttons"): ["palm_button"],
        ("minecraft", "wooden_pressure_plates"): ["palm_pressure_plate"],
        ("minecraft", "leaves"): ["palm_leaves"],
        ("minecraft", "saplings"): ["palm_sapling"],
        ("minecraft", "small_flowers"): ["desert_rose"],
        ("minecraft", "stairs"): ["limestone_stairs", "polished_limestone_stairs", "limestone_brick_stairs"],
        ("minecraft", "slabs"): ["limestone_slab", "polished_limestone_slab", "limestone_brick_slab"],
        ("minecraft", "walls"): ["limestone_wall", "polished_limestone_wall", "limestone_brick_wall"],
        (MOD, "palm_logs"): ["palm_log", "palm_wood", "stripped_palm_log", "stripped_palm_wood"],
        ("forge", "storage_blocks/bronze"): ["bronze_block"],
        ("forge", "ores/amber"): ["amber_ore"],
    }
    for (ns, name), values in both.items():
        for registry in ("blocks", "items"):
            tag(registry, ns, name, mm(values))
    for registry in ("blocks", "items"):
        tag(registry, "minecraft", "logs_that_burn", ["#" + m("palm_logs")])
        tag(registry, "forge", "storage_blocks", ["#forge:storage_blocks/bronze"])
        tag(registry, "forge", "ores", ["#forge:ores/amber"])

    tag("blocks", "minecraft", "mineable/pickaxe", mm(LIMESTONE_BLOCKS + ["amber_ore", "amber_block", "amber_lamp",
                                                                        "bronze_block", "sarcophagus", "ancient_urn"]))
    tag("blocks", "minecraft", "mineable/axe", mm(PALM_BLOCKS))
    tag("blocks", "minecraft", "mineable/shovel", mm(["quicksand"]))
    tag("blocks", "minecraft", "mineable/hoe", mm(["palm_leaves"]))
    tag("blocks", "minecraft", "needs_stone_tool", mm(["amber_ore", "bronze_block"]))
    tag("blocks", "minecraft", "flower_pots", mm(["potted_palm_sapling", "potted_desert_rose"]))
    tag("blocks", "minecraft", "replaceable_by_trees", mm(["dune_grass", "cattail"]))
    tag("blocks", "minecraft", "base_stone_overworld", mm(["limestone"]))
    tag("blocks", "minecraft", "overworld_carver_replaceables", mm(["limestone"]))
    tag("blocks", "minecraft", "beacon_base_blocks", mm(["bronze_block"]))

    tag("items", "minecraft", "trimmable_armor", mm(["bronze_helmet", "bronze_chestplate", "bronze_leggings", "bronze_boots"]))
    tag("items", "minecraft", "trim_materials", mm(["amber"]))
    tag("items", "minecraft", "trim_templates", mm(["pharaoh_armor_trim_smithing_template"]))
    tag("items", "minecraft", "beacon_payment_items", mm(["bronze_ingot"]))
    tag("items", "minecraft", "stone_tool_materials", mm(["limestone"]))
    tag("items", "minecraft", "stone_crafting_materials", mm(["limestone"]))
    tag("items", "minecraft", "swords", mm(["bronze_khopesh"]))
    tag("items", "minecraft", "shovels", mm(["bronze_shovel"]))
    tag("items", "minecraft", "pickaxes", mm(["bronze_pickaxe"]))
    tag("items", "minecraft", "axes", mm(["bronze_axe"]))
    tag("items", "minecraft", "hoes", mm(["bronze_hoe"]))
    tag("items", "forge", "ingots/bronze", mm(["bronze_ingot"]))
    tag("items", "forge", "ingots", ["#forge:ingots/bronze"])
    tag("items", "forge", "nuggets/bronze", mm(["bronze_nugget"]))
    tag("items", "forge", "nuggets", ["#forge:nuggets/bronze"])
    tag("items", "forge", "gems/amber", mm(["amber"]))
    tag("items", "forge", "gems", ["#forge:gems/amber"])

    tag("entity_types", MOD, "quicksand_walkers", mm(["mummy", "pharaoh", "scorpion", "scarab", "meerkat"])
        + ["minecraft:husk", "minecraft:camel"])

    tag("damage_type", "minecraft", "bypasses_armor", [m("quicksand")])
    tag("damage_type", "minecraft", "no_knockback", [m("quicksand")])

    dunes = m("ancient_dunes")
    tag("worldgen/biome", MOD, "has_sandstorms", ["minecraft:desert", dunes])
    for name in ("is_overworld", "has_structure/desert_pyramid", "has_structure/village_desert",
                 "has_structure/mineshaft", "has_structure/ruined_portal_desert", "has_structure/pillager_outpost",
                 "stronghold_biased_to", "spawns_gold_rabbits", "snow_golem_melts"):
        tag("worldgen/biome", "minecraft", name, [dunes])
    for name in ("is_desert", "is_hot", "is_hot/overworld", "is_dry", "is_dry/overworld", "is_sandy"):
        tag("worldgen/biome", "forge", name, [dunes])


# ================================================================================================ worldgen

def worldgen(kind, name, obj):
    write(os.path.join(DATA, MOD, "worldgen", kind, name + ".json"), obj)


def placement(*modifiers):
    return list(modifiers)


def rarity(chance):
    return {"type": "minecraft:rarity_filter", "chance": chance}


def count(n):
    return {"type": "minecraft:count", "count": n}


IN_SQUARE = {"type": "minecraft:in_square"}
BIOME = {"type": "minecraft:biome"}


def heightmap(kind):
    return {"type": "minecraft:heightmap", "heightmap": kind}


def patch(block_state, tries, spread):
    return {"type": "minecraft:random_patch", "config": {
        "tries": tries, "xz_spread": spread, "y_spread": 3,
        "feature": {
            "feature": {"type": "minecraft:simple_block",
                        "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": block_state}}},
            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {
                "type": "minecraft:all_of", "predicates": [
                    {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                    {"type": "minecraft:would_survive", "state": block_state}]}}]}}}


def ore(targets, size):
    return {"type": "minecraft:ore", "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": targets}}


def generate_worldgen():
    none = {"config": {}}
    for feature in ("palm_tree", "ancient_ruin", "obelisk", "oasis"):
        worldgen("configured_feature", feature, {"type": m(feature), **none})
    worldgen("configured_feature", "ore_limestone", ore([
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:base_stone_overworld"},
         "state": {"Name": m("limestone")}}], 48))
    worldgen("configured_feature", "ore_amber", ore([
        {"target": {"predicate_type": "minecraft:block_match", "block": m("limestone")}, "state": {"Name": m("amber_ore")}},
        {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:sandstone"}, "state": {"Name": m("amber_ore")}},
    ], 5))
    worldgen("configured_feature", "quicksand_patch", {"type": "minecraft:disk", "config": {
        "state_provider": {"fallback": {"type": "minecraft:simple_state_provider", "state": {"Name": m("quicksand")}},
                           "rules": []},
        "target": {"type": "minecraft:matching_blocks", "blocks": ["minecraft:sand"]},
        "radius": {"type": "minecraft:uniform", "value": {"min_inclusive": 2, "max_inclusive": 4}},
        "half_height": 3}})
    worldgen("configured_feature", "patch_dune_grass", patch({"Name": m("dune_grass")}, 24, 6))
    worldgen("configured_feature", "patch_aloe_vera", patch({"Name": m("aloe_vera"), "Properties": {"age": "3"}}, 10, 4))
    worldgen("configured_feature", "flower_desert_rose", patch({"Name": m("desert_rose")}, 12, 5))

    surface = heightmap("WORLD_SURFACE_WG")
    motion = heightmap("MOTION_BLOCKING")
    placed = {
        "ore_limestone": ("ore_limestone", [count(3), IN_SQUARE, {"type": "minecraft:height_range", "height": {
            "type": "minecraft:uniform", "min_inclusive": {"absolute": 0}, "max_inclusive": {"absolute": 120}}}, BIOME]),
        "ore_amber": ("ore_amber", [count(14), IN_SQUARE, {"type": "minecraft:height_range", "height": {
            "type": "minecraft:uniform", "min_inclusive": {"absolute": 0}, "max_inclusive": {"absolute": 128}}}, BIOME]),
        "quicksand_patch": ("quicksand_patch", [rarity(3), IN_SQUARE, surface, BIOME]),
        "palm_tree_sparse": ("palm_tree", [rarity(7), IN_SQUARE, surface, BIOME]),
        "patch_dune_grass": ("patch_dune_grass", [count(3), IN_SQUARE, motion, BIOME]),
        "patch_aloe_vera": ("patch_aloe_vera", [rarity(4), IN_SQUARE, motion, BIOME]),
        "flower_desert_rose": ("flower_desert_rose", [rarity(6), IN_SQUARE, motion, BIOME]),
        "ancient_ruin": ("ancient_ruin", [rarity(28), IN_SQUARE, surface, BIOME]),
        "ancient_ruin_rare": ("ancient_ruin", [rarity(110), IN_SQUARE, surface, BIOME]),
        "obelisk": ("obelisk", [rarity(45), IN_SQUARE, surface, BIOME]),
        "oasis": ("oasis", [rarity(22), IN_SQUARE, surface, BIOME]),
        "oasis_rare": ("oasis", [rarity(70), IN_SQUARE, surface, BIOME]),
    }
    for name, (feature, modifiers) in placed.items():
        worldgen("placed_feature", name, {"feature": m(feature), "placement": modifiers})

    desert = json.loads(vanilla("data", "worldgen/biome/desert"))
    features = desert["features"]
    features[1] = features[1] + [m("oasis")]
    features[2] = features[2] + [m("quicksand_patch")]
    features[4] = features[4] + [m("ancient_ruin"), m("obelisk")]
    features[6] = features[6] + [m("ore_limestone"), m("ore_amber")]
    features[9] = [f for f in features[9] if f not in ("minecraft:flower_default", "minecraft:patch_pumpkin")] \
        + [m("palm_tree_sparse"), m("patch_dune_grass"), m("patch_aloe_vera"), m("flower_desert_rose")]
    spawn = lambda t, w, lo, hi: {"type": t, "weight": w, "minCount": lo, "maxCount": hi}
    biome = {
        "has_precipitation": False,
        "temperature": 2.0,
        "downfall": 0.0,
        "effects": {
            "sky_color": 0xC9D2E6,
            "fog_color": 0xE6D3A8,
            "water_color": 0x3FC7C0,
            "water_fog_color": 0x0C3F4A,
            "grass_color": 0xB6B05A,
            "foliage_color": 0xA89E3C,
            "mood_sound": desert["effects"]["mood_sound"],
            "music": desert["effects"]["music"],
        },
        "carvers": desert["carvers"],
        "features": features,
        "spawn_costs": {},
        "spawners": {
            "ambient": [spawn("minecraft:bat", 10, 8, 8)],
            "axolotls": [],
            "creature": [spawn("minecraft:rabbit", 4, 2, 3), spawn(m("meerkat"), 8, 2, 4), spawn(m("vulture"), 3, 1, 2)],
            "misc": [],
            "monster": [spawn("minecraft:spider", 60, 2, 3), spawn("minecraft:skeleton", 80, 2, 4),
                        spawn("minecraft:creeper", 80, 2, 4), spawn("minecraft:enderman", 10, 1, 4),
                        spawn("minecraft:witch", 5, 1, 1), spawn("minecraft:husk", 50, 2, 4),
                        spawn(m("mummy"), 70, 1, 3), spawn(m("scorpion"), 80, 1, 2)],
            "underground_water_creature": [spawn("minecraft:glow_squid", 10, 4, 6)],
            "water_ambient": [],
            "water_creature": [],
        },
    }
    worldgen("biome", "ancient_dunes", biome)

    modifier = lambda name, obj: write(os.path.join(DATA, MOD, "forge/biome_modifier", name + ".json"), obj)
    add = lambda features, step: {"type": "forge:add_features", "biomes": "minecraft:desert",
                                  "features": [m(f) for f in features], "step": step}
    modifier("desert_limestone_and_amber", add(["ore_limestone", "ore_amber"], "underground_ores"))
    modifier("desert_ruins", add(["ancient_ruin_rare"], "surface_structures"))
    modifier("desert_oases", add(["oasis_rare"], "lakes"))
    modifier("desert_plants", add(["patch_aloe_vera", "flower_desert_rose"], "vegetal_decoration"))
    modifier("desert_mobs", {"type": "forge:add_spawns", "biomes": "minecraft:desert", "spawners": [
        spawn(m("scorpion"), 40, 1, 2), spawn(m("meerkat"), 4, 2, 3), spawn(m("vulture"), 2, 1, 1)]})


# ================================================================================================ misc data

def generate_misc_data():
    write(os.path.join(DATA, MOD, "trim_pattern/pharaoh.json"), {
        "asset_id": m("pharaoh"), "description": {"translate": "trim_pattern.dunesrelics.pharaoh"},
        "template_item": m("pharaoh_armor_trim_smithing_template")})
    write(os.path.join(DATA, MOD, "trim_material/amber.json"), {
        "asset_name": "amber", "description": {"color": "#E8962C", "translate": "trim_material.dunesrelics.amber"},
        "ingredient": m("amber"), "item_model_index": 0.55})
    write(os.path.join(DATA, MOD, "damage_type/quicksand.json"),
          {"exhaustion": 0.0, "message_id": "dunesrelics.quicksand", "scaling": "never"})

    lm = lambda name, table, item, chance: write(os.path.join(DATA, MOD, "loot_modifiers", name + ".json"), {
        "type": m("add_item"), "item": item, "conditions": [
            {"condition": "forge:loot_table_id", "loot_table_id": table},
            {"condition": "minecraft:random_chance", "chance": chance}]})
    lm("pharaoh_trim_in_desert_pyramid", "minecraft:chests/desert_pyramid", m("pharaoh_armor_trim_smithing_template"), 0.15)
    lm("amber_in_desert_pyramid", "minecraft:chests/desert_pyramid", m("amber"), 0.5)
    lm("dates_in_desert_village", "minecraft:chests/village/village_desert_house", m("dates"), 0.4)
    write(os.path.join(DATA, "forge/loot_modifiers/global_loot_modifiers.json"), {"replace": False, "entries": [
        m("pharaoh_trim_in_desert_pyramid"), m("amber_in_desert_pyramid"), m("dates_in_desert_village")]})

    generate_advancements()
    write_arena()


def generate_advancements():
    def adv(name, parent, icon, criteria, frame="task", hidden=False, rewards=None, toast=True):
        obj = {"display": {"icon": {"item": icon}, "title": {"translate": "advancements.dunesrelics.%s.title" % name},
                           "description": {"translate": "advancements.dunesrelics.%s.description" % name},
                           "frame": frame, "show_toast": toast, "announce_to_chat": toast, "hidden": hidden},
               "criteria": criteria}
        if parent:
            obj["parent"] = m(parent)
        else:
            obj["display"]["background"] = "minecraft:textures/block/sandstone_top.png"
        if rewards:
            obj["rewards"] = rewards
        write(os.path.join(DATA, MOD, "advancements", name + ".json"), obj)

    entity = lambda t: [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": t}}]
    adv("root", None, m("chiseled_limestone_bricks"), {"tick": {"trigger": "minecraft:tick"}}, toast=False)
    adv("ancient_dunes", "root", "minecraft:sand", {"entered": {"trigger": "minecraft:location", "conditions": {
        "player": [{"condition": "minecraft:entity_properties", "entity": "this",
                    "predicate": {"location": {"biome": m("ancient_dunes")}}}]}}})
    adv("quicksand", "ancient_dunes", m("quicksand"), {"sank": {"trigger": "minecraft:enter_block",
                                                                "conditions": {"block": m("quicksand")}}})
    adv("mummy", "ancient_dunes", m("linen"), {"killed": {"trigger": "minecraft:player_killed_entity",
                                                          "conditions": {"entity": entity(m("mummy"))}}})
    adv("meerkats", "ancient_dunes", m("dates"), {"bred": {"trigger": "minecraft:bred_animals",
                                                          "conditions": {"child": entity(m("meerkat"))}}})
    adv("ruins", "ancient_dunes", m("ancient_urn"), {"looted": {"trigger": "minecraft:player_generates_container_loot",
                                                               "conditions": {"loot_table": m("chests/ancient_ruins")}}})
    adv("tomb", "ruins", m("sarcophagus"), {"looted": {"trigger": "minecraft:player_generates_container_loot",
                                                      "conditions": {"loot_table": m("chests/ancient_tomb")}}}, frame="goal")
    adv("pharaoh", "tomb", m("scepter_of_sands"), {"killed": {"trigger": "minecraft:player_killed_entity",
                                                             "conditions": {"entity": entity(m("pharaoh"))}}},
        frame="challenge", rewards={"experience": 100})
    adv("bronze", "root", m("bronze_ingot"), {"has": {"trigger": "minecraft:inventory_changed",
                                                     "conditions": {"items": [{"items": [m("bronze_ingot")]}]}}})
    adv("goggles", "bronze", m("amber_goggles"), {"has": {"trigger": "minecraft:inventory_changed",
                                                         "conditions": {"items": [{"items": [m("amber_goggles")]}]}}})


# ------------------------------------------------------------------------------------------------ NBT arena

def nbt_string(s):
    data = s.encode("utf-8")
    return struct.pack(">H", len(data)) + data


def write_arena():
    """An empty 24x24x24 structure used as the game test arena."""
    body = b""
    body += b"\x03" + nbt_string("DataVersion") + struct.pack(">i", 3465)
    body += b"\x09" + nbt_string("size") + b"\x03" + struct.pack(">i", 3) + struct.pack(">iii", 24, 24, 24)
    for name in ("palette", "blocks", "entities"):
        body += b"\x09" + nbt_string(name) + b"\x00" + struct.pack(">i", 0)
    root = b"\x0a" + nbt_string("") + body + b"\x00"
    path = os.path.join(DATA, MOD, "structures/arena.nbt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    buffer = io.BytesIO()
    with gzip.GzipFile(fileobj=buffer, mode="wb", mtime=0) as gz:
        gz.write(root)
    with open(path, "wb") as out:
        out.write(buffer.getvalue())


# ================================================================================================ client assets

def generate_client_assets():
    write(os.path.join(ASSETS, "particles/sand_gust.json"),
          {"textures": [m("sand_gust_%d" % i) for i in range(4)]})
    permutations = json.loads(vanilla("assets", "atlases/armor_trims"))["sources"][0]["permutations"]
    vanilla_textures = json.loads(vanilla("assets", "atlases/armor_trims"))["sources"][0]["textures"]
    all_permutations = dict(permutations)
    mod_materials = {"amber": m("trims/color_palettes/amber"), "fire_opal": m("trims/color_palettes/fire_opal"),
                     "pearl": m("trims/color_palettes/pearl")}
    all_permutations.update(mod_materials)
    write(os.path.join(RES, "assets/minecraft/atlases/armor_trims.json"), {"sources": [
        {"type": "paletted_permutations",
         "textures": [m("trims/models/armor/" + t) for t in ("pharaoh", "pharaoh_leggings", "titan", "titan_leggings")],
         "palette_key": "trims/color_palettes/trim_palette", "permutations": all_permutations},
        {"type": "paletted_permutations", "textures": vanilla_textures,
         "palette_key": "trims/color_palettes/trim_palette", "permutations": mod_materials},
    ]})


def main():
    for folder in (os.path.join(ASSETS, "blockstates"), os.path.join(ASSETS, "models"), os.path.join(DATA, MOD)):
        if os.path.isdir(folder):
            shutil.rmtree(folder)
    generate_block_assets()
    generate_item_models()
    generate_loot()
    generate_recipes()
    generate_tags()
    generate_worldgen()
    generate_misc_data()
    import volcanic_data
    volcanic_data.generate_block_assets()
    volcanic_data.generate_item_models()
    volcanic_data.generate_loot()
    volcanic_data.generate_recipes()
    volcanic_data.generate_tags()
    volcanic_data.generate_worldgen()
    volcanic_data.generate_misc_data()
    import world_data
    world_data.generate_block_assets()
    world_data.generate_item_models()
    world_data.generate_loot()
    world_data.generate_recipes()
    world_data.generate_tags()
    world_data.generate_worldgen()
    world_data.generate_misc_data()
    world_data.generate_particles()
    generate_client_assets()
    import lang
    lang.generate(ASSETS)
    print("data generated")


if __name__ == "__main__":
    main()

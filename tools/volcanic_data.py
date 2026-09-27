"""JSON data for the volcanic update ("Ash & Ember"): blockstates, models, loot, recipes, tags, three biomes,
features, the volcano structure, advancements, trims and translations.

Called from generate_data.py; it reuses that module's helpers.
"""
import json
import os

from generate_data import (ASSETS, DATA, MOD, m, write, vanilla, vanilla_json, blockstate, block_model, item_model,
                           tex, simple_block_state, cube_all, pillar, stairs, slab, wall, fence, fence_gate, door,
                           trapdoor, button, pressure_plate, cross, potted, loot, item_entry, pool, SURVIVES, SHEARS,
                           SILK, self_drop, recipe, shaped, shapeless, cooking, stonecut, tag, worldgen, rarity, count,
                           IN_SQUARE, BIOME, heightmap, patch, ore)

SCORIA_BLOCKS = ["scoria", "scoria_stairs", "scoria_slab", "scoria_wall", "polished_scoria", "polished_scoria_stairs",
                 "polished_scoria_slab", "polished_scoria_wall", "scoria_bricks", "scoria_brick_stairs",
                 "scoria_brick_slab", "scoria_brick_wall", "cracked_scoria_bricks", "chiseled_scoria_bricks",
                 "molten_scoria"]
PUMICE_BLOCKS = ["pumice", "pumice_bricks", "pumice_brick_stairs", "pumice_brick_slab", "pumice_sponge",
                 "molten_pumice_sponge"]
OBSIDIAN_BLOCKS = ["obsidian_bricks", "obsidian_brick_stairs", "obsidian_brick_slab", "obsidian_brick_wall",
                   "chiseled_obsidian"]
EMBER_BLOCKS = ["ember_log", "ember_wood", "stripped_ember_log", "stripped_ember_wood", "ember_planks", "ember_stairs",
                "ember_slab", "ember_fence", "ember_fence_gate", "ember_door", "ember_trapdoor", "ember_button",
                "ember_pressure_plate"]
MOBS = ["magma_titan", "salamander", "lava_crab", "magmaling", "cinder_wraith"]


# ================================================================================================ block assets

def forge_models():
    """The volcanic forge: a squat anvil-like base with a basin on top that fills with lava in four steps."""
    def element(frm, to, inner=(), skip=("down",), front=False):
        faces = {}
        for face in ("north", "south", "east", "west", "up", "down"):
            if face in skip:
                continue
            texture = "#top" if face in ("up", "down") else "#inner" if face in inner else "#side"
            if front and face == "north":
                texture = "#front"
            faces[face] = {"texture": texture}
        return {"from": frm, "to": to, "faces": faces}

    base = [
        element([0, 0, 0], [16, 4, 16], skip=()),
        element([2, 4, 2], [14, 10, 14], skip=("down", "up"), front=True),
        element([0, 10, 0], [16, 14, 2], inner=("south",)),
        element([0, 10, 14], [16, 14, 16], inner=("north",)),
        element([0, 10, 2], [2, 14, 14], inner=("east",), skip=("down", "north", "south")),
        element([14, 10, 2], [16, 14, 14], inner=("west",), skip=("down", "north", "south")),
        {"from": [2, 10, 2], "to": [14, 11, 14], "faces": {"up": {"texture": "#inner"}, "down": {"texture": "#top"}}},
    ]
    textures = {"side": tex("volcanic_forge_side"), "front": tex("volcanic_forge_front"),
                "top": tex("volcanic_forge_top"), "inner": tex("volcanic_forge_inner"),
                "lava": "minecraft:block/lava_still", "particle": tex("volcanic_forge_side")}
    for level in range(5):
        elements = list(base)
        if level:
            height = [0, 11.75, 12.5, 13.25, 13.9][level]
            elements.append({"from": [2, 11, 2], "to": [14, height, 14],
                             "faces": {"up": {"uv": [2, 2, 14, 14], "texture": "#lava"}}})
        block_model("volcanic_forge_lava%d" % level, {"parent": "minecraft:block/block", "textures": textures,
                                                      "elements": elements})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    variants = {}
    for facing, y in rot.items():
        for level in range(5):
            v = {"model": m("block/volcanic_forge_lava%d" % level)}
            if y:
                v["y"] = y
            variants["facing=%s,lava=%d" % (facing, level)] = v
    blockstate("volcanic_forge", {"variants": variants})
    item_model("volcanic_forge", {"parent": m("block/volcanic_forge_lava3")})


def generate_block_assets():
    # scoria
    for base, st, sl, wa in (("scoria", "scoria_stairs", "scoria_slab", "scoria_wall"),
                             ("polished_scoria", "polished_scoria_stairs", "polished_scoria_slab", "polished_scoria_wall"),
                             ("scoria_bricks", "scoria_brick_stairs", "scoria_brick_slab", "scoria_brick_wall")):
        cube_all(base)
        stairs(st, base)
        slab(sl, base, base)
        wall(wa, base)
    for name in ("cracked_scoria_bricks", "chiseled_scoria_bricks", "molten_scoria"):
        cube_all(name)

    # pumice
    cube_all("pumice")
    cube_all("pumice_bricks")
    stairs("pumice_brick_stairs", "pumice_bricks")
    slab("pumice_brick_slab", "pumice_bricks", "pumice_bricks")
    cube_all("pumice_sponge")
    cube_all("molten_pumice_sponge")

    # obsidian & glass
    cube_all("obsidian_bricks")
    stairs("obsidian_brick_stairs", "obsidian_bricks")
    slab("obsidian_brick_slab", "obsidian_bricks", "obsidian_bricks")
    wall("obsidian_brick_wall", "obsidian_bricks")
    cube_all("chiseled_obsidian")
    cube_all("volcanic_glass", render_type="minecraft:translucent")
    for part in ("post", "side", "side_alt", "noside", "noside_alt"):
        block_model("volcanic_glass_pane_" + part, {
            "parent": "minecraft:block/template_glass_pane_" + part, "render_type": "minecraft:translucent",
            "textures": {"pane": tex("volcanic_glass"), "edge": tex("volcanic_glass_pane_top")}})
    blockstate("volcanic_glass_pane", vanilla_json("assets", "blockstates/glass_pane",
                                                   [("minecraft:block/glass_pane", m("block/volcanic_glass_pane"))]))
    item_model("volcanic_glass_pane", {"parent": "minecraft:item/generated", "textures": {"layer0": tex("volcanic_glass")}})

    # ash & sand
    cube_all("ash_block")
    cube_all("black_sand")
    for height in range(2, 16, 2):
        block_model("ash_layer_height%d" % height, vanilla_json(
            "assets", "models/block/snow_height%d" % height, [('"block/snow"', '"%s"' % tex("ash_block"))]))
    blockstate("ash_layer", vanilla_json("assets", "blockstates/snow", [
        ("minecraft:block/snow_height", m("block/ash_layer_height")), ("minecraft:block/snow_block", m("block/ash_block"))]))
    item_model("ash_layer", {"parent": m("block/ash_layer_height2")})

    # ores & minerals
    for name in ("fire_opal_ore", "deepslate_fire_opal_ore", "fire_opal_block", "sulfur_ore", "sulfur_block"):
        cube_all(name)
    cross("sulfur_cluster")
    simple_block_state("sulfur_cluster")

    # emberwood
    pillar("ember_log", "ember_log", "ember_log_top")
    pillar("stripped_ember_log", "stripped_ember_log", "stripped_ember_log_top")
    pillar("ember_wood", "ember_log", "ember_log", horizontal=False)
    pillar("stripped_ember_wood", "stripped_ember_log", "stripped_ember_log", horizontal=False)
    cube_all("ember_planks")
    stairs("ember_stairs", "ember_planks")
    slab("ember_slab", "ember_planks", "ember_planks")
    fence("ember_fence", "ember_planks")
    fence_gate("ember_fence_gate", "ember_planks")
    door("ember_door")
    trapdoor("ember_trapdoor")
    button("ember_button", "ember_planks")
    pressure_plate("ember_pressure_plate", "ember_planks")
    block_model("ember_leaves", {"parent": "minecraft:block/leaves", "textures": {"all": tex("ember_leaves")},
                                 "render_type": "minecraft:cutout_mipped"})
    simple_block_state("ember_leaves")
    item_model("ember_leaves", {"parent": m("block/ember_leaves")})
    cross("ember_sapling")
    simple_block_state("ember_sapling")
    potted("potted_ember_sapling", "ember_sapling")

    # plants
    cross("ash_grass")
    simple_block_state("ash_grass")
    cross("fireblossom")
    simple_block_state("fireblossom")
    potted("potted_fireblossom", "fireblossom")
    lily = json.loads(vanilla("assets", "models/block/lily_pad").replace('"block/lily_pad"', '"%s"' % tex("lava_lily")))
    lily["render_type"] = "minecraft:cutout"
    block_model("lava_lily", lily)
    blockstate("lava_lily", {"variants": {"": [{"model": m("block/lava_lily"), "y": y} if y else {"model": m("block/lava_lily")}
                                               for y in (0, 90, 180, 270)]}})
    item_model("lava_lily", {"parent": "minecraft:item/generated", "textures": {"layer0": tex("lava_lily")}})
    for stage in range(4):
        cross("fire_pepper_bush_stage%d" % stage, item=False)
    blockstate("fire_pepper_bush", {"variants": {"age=%d" % a: {"model": m("block/fire_pepper_bush_stage%d" % a)}
                                                 for a in range(4)}})

    # mechanics
    for active, suffix in (("false", ""), ("true", "_active")):
        block_model("steam_vent" + suffix, {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": tex("steam_vent_top" + suffix), "side": tex("steam_vent_side"), "bottom": tex("scoria")}})
    blockstate("steam_vent", {"variants": {"active=false": {"model": m("block/steam_vent")},
                                           "active=true": {"model": m("block/steam_vent_active")}}})
    item_model("steam_vent", {"parent": m("block/steam_vent")})
    forge_models()
    for name in ("heart_of_the_volcano", "heart_of_the_volcano_dormant"):
        block_model(name, {"parent": "minecraft:block/cube_all", "textures": {"all": tex(name)}})
    blockstate("heart_of_the_volcano", {"variants": {
        "dormant=false": {"model": m("block/heart_of_the_volcano")},
        "dormant=true": {"model": m("block/heart_of_the_volcano_dormant")}}})
    item_model("heart_of_the_volcano", {"parent": m("block/heart_of_the_volcano")})
    for age in range(4):
        block_model("cooled_lava_crust_%d" % age, {"parent": "minecraft:block/cube_all",
                                                   "textures": {"all": tex("cooled_lava_crust_%d" % age)}})
    blockstate("cooled_lava_crust", {"variants": {"age=%d" % a: {"model": m("block/cooled_lava_crust_%d" % a)}
                                                  for a in range(4)}})


def generate_item_models():
    generated = ["fire_opal", "sulfur", "volcanic_ash", "salamander_scale", "ember_core", "lava_crab_bucket",
                 "fire_opal_helmet", "fire_opal_chestplate", "fire_opal_leggings", "fire_opal_boots",
                 "salamander_boots", "fire_pepper", "crab_meat", "cooked_crab_meat", "spicy_stew",
                 "titan_armor_trim_smithing_template", "scorpion_venom"]
    handheld = ["fire_opal_sword", "fire_opal_shovel", "fire_opal_pickaxe", "fire_opal_axe", "fire_opal_hoe",
                "magma_hammer"]
    for name in generated:
        item_model(name, {"parent": "minecraft:item/generated", "textures": {"layer0": m("item/" + name)}})
    for name in handheld:
        item_model(name, {"parent": "minecraft:item/handheld", "textures": {"layer0": m("item/" + name)}})
    for mob in MOBS:
        item_model(mob + "_spawn_egg", {"parent": "minecraft:item/template_spawn_egg"})


# ================================================================================================ loot tables

def template_loot(name, template, replacements):
    loot("blocks/" + name, vanilla_json("data", "loot_tables/blocks/" + template,
                                        replacements + [("minecraft:blocks/" + template, m("blocks/" + name))]))


def ore_loot(name, drop, count_range=None):
    table = vanilla_json("data", "loot_tables/blocks/diamond_ore",
                         [("minecraft:diamond_ore", m(name)), ("minecraft:diamond", drop),
                          ("minecraft:blocks/diamond_ore", m("blocks/" + name))])
    if count_range:
        for p in table["pools"]:
            for alt in p["entries"]:
                for child in alt.get("children", []):
                    if child.get("name") == drop:
                        child["functions"].insert(0, {"function": "minecraft:set_count", "add": False, "count": {
                            "type": "minecraft:uniform", "min": float(count_range[0]), "max": float(count_range[1])}})
    loot("blocks/" + name, table)


def generate_loot():
    simple = [b for b in SCORIA_BLOCKS + PUMICE_BLOCKS + OBSIDIAN_BLOCKS + EMBER_BLOCKS
              if not b.endswith("_slab") and b != "ember_door"]
    simple += ["ash_block", "black_sand", "fire_opal_block", "sulfur_block", "ember_sapling", "fireblossom",
               "lava_lily", "steam_vent", "volcanic_forge", "heart_of_the_volcano"]
    for name in simple:
        self_drop(name)
    for name in ("scoria_slab", "polished_scoria_slab", "scoria_brick_slab", "pumice_brick_slab", "obsidian_brick_slab",
                 "ember_slab"):
        template_loot(name, "oak_slab", [("minecraft:oak_slab", m(name))])
    template_loot("ember_door", "oak_door", [("minecraft:oak_door", m("ember_door"))])
    template_loot("ember_leaves", "birch_leaves", [("minecraft:birch_leaves", m("ember_leaves")),
                                                   ("minecraft:birch_sapling", m("ember_sapling"))])
    template_loot("volcanic_glass", "glass", [('"minecraft:glass"', '"%s"' % m("volcanic_glass"))])
    template_loot("volcanic_glass_pane", "glass_pane", [("minecraft:glass_pane", m("volcanic_glass_pane"))])
    template_loot("ash_layer", "snow", [('"minecraft:snowball"', '"%s"' % m("volcanic_ash")),
                                        ('"minecraft:snow"', '"%s"' % m("ash_layer"))])
    for name, plant in (("potted_ember_sapling", "ember_sapling"), ("potted_fireblossom", "fireblossom")):
        template_loot(name, "potted_oak_sapling", [("minecraft:oak_sapling", m(plant)),
                                                   ("minecraft:potted_oak_sapling", m(name))])
    ore_loot("fire_opal_ore", m("fire_opal"))
    ore_loot("deepslate_fire_opal_ore", m("fire_opal"))
    ore_loot("sulfur_ore", m("sulfur"), (2, 4))

    loot("blocks/sulfur_cluster", {"type": "minecraft:block", "pools": [pool([{
        "type": "minecraft:alternatives", "children": [
            item_entry(m("sulfur_cluster"), conditions=[SILK]),
            item_entry(m("sulfur"), (1, 2), functions=[{"function": "minecraft:explosion_decay"}])]}])],
        "random_sequence": m("blocks/sulfur_cluster")})
    loot("blocks/ash_grass", {"type": "minecraft:block", "pools": [pool([{
        "type": "minecraft:alternatives", "children": [
            item_entry(m("ash_grass"), conditions=[SHEARS]),
            item_entry(m("volcanic_ash"), conditions=[{"condition": "minecraft:random_chance", "chance": 0.125}])]}])],
        "random_sequence": m("blocks/ash_grass")})
    bush = vanilla_json("data", "loot_tables/blocks/sweet_berry_bush",
                        [("minecraft:sweet_berry_bush", m("fire_pepper_bush")), ("minecraft:sweet_berries", m("fire_pepper")),
                         ("minecraft:blocks/sweet_berry_bush", m("blocks/fire_pepper_bush"))])
    # an unripe bush still gives its pepper back, like replanting a seed
    for age in ("0", "1"):
        bush["pools"].append(pool([item_entry(m("fire_pepper"))], conditions=[{
            "condition": "minecraft:block_state_property", "block": m("fire_pepper_bush"), "properties": {"age": age}}]))
    loot("blocks/fire_pepper_bush", bush)

    looting = lambda lo, hi: {"function": "minecraft:looting_enchant",
                              "count": {"type": "minecraft:uniform", "min": float(lo), "max": float(hi)}}
    by_player = {"condition": "minecraft:killed_by_player"}
    loot("entities/magma_titan", {"type": "minecraft:entity", "pools": [
        pool([item_entry(m("magma_hammer"))]),
        pool([item_entry(m("ember_core"))]),
        pool([item_entry(m("titan_armor_trim_smithing_template"))]),
        pool([item_entry(m("fire_opal"), (4, 8))]),
        pool([item_entry("minecraft:magma_cream", (3, 6))]),
        pool([item_entry(m("obsidian_bricks"), (4, 10))]),
    ], "random_sequence": m("entities/magma_titan")})
    loot("entities/salamander", {"type": "minecraft:entity", "pools": [], "random_sequence": m("entities/salamander")})
    on_fire = {"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"flags": {"is_on_fire": True}}}
    loot("entities/lava_crab", {"type": "minecraft:entity", "pools": [
        pool([item_entry(m("crab_meat"), (1, 2), functions=[
            {"function": "minecraft:furnace_smelt", "conditions": [on_fire]}, looting(0, 1)])]),
    ], "random_sequence": m("entities/lava_crab")})
    loot("entities/magmaling", {"type": "minecraft:entity", "pools": [
        pool([item_entry(m("sulfur"), (0, 2), functions=[looting(0, 1)])]),
        pool([item_entry("minecraft:magma_cream", (0, 1), functions=[looting(0, 1)])]),
    ], "random_sequence": m("entities/magmaling")})
    loot("entities/cinder_wraith", {"type": "minecraft:entity", "pools": [
        pool([item_entry(m("volcanic_ash"), (1, 3), functions=[looting(0, 1)])]),
        pool([item_entry("minecraft:blaze_powder", (0, 1), functions=[looting(0, 1)])], conditions=[by_player]),
    ], "random_sequence": m("entities/cinder_wraith")})

    enchant = {"function": "minecraft:enchant_randomly"}
    enchant_levels = {"function": "minecraft:enchant_with_levels", "levels": {"type": "minecraft:uniform", "min": 20.0,
                                                                                "max": 39.0}, "treasure": True}
    loot("chests/volcano_lair", {"type": "minecraft:chest", "pools": [
        pool([
            item_entry(m("fire_opal"), (2, 5), weight=18),
            item_entry("minecraft:gold_ingot", (2, 6), weight=15),
            item_entry(m("sulfur"), (3, 8), weight=15),
            item_entry("minecraft:obsidian", (2, 5), weight=12),
            item_entry("minecraft:magma_cream", (1, 4), weight=10),
            item_entry(m("fire_pepper"), (2, 5), weight=10),
            item_entry(m("cooked_crab_meat"), (2, 4), weight=8),
            item_entry("minecraft:diamond", (1, 3), weight=6),
            item_entry("minecraft:blaze_rod", (1, 3), weight=5),
            item_entry("minecraft:fire_charge", (2, 5), weight=5),
        ], rolls={"type": "minecraft:uniform", "min": 4.0, "max": 7.0}),
        pool([
            {"type": "minecraft:empty", "weight": 4},
            item_entry(m("fire_opal_sword"), weight=3),
            item_entry(m("fire_opal_pickaxe"), weight=3),
            item_entry(m("fire_opal_chestplate"), weight=2),
            item_entry("minecraft:book", weight=4, functions=[enchant_levels]),
            item_entry("minecraft:golden_apple", weight=3),
            item_entry(m("titan_armor_trim_smithing_template"), weight=2),
            item_entry(m("pumice_sponge"), weight=2),
            item_entry("minecraft:netherite_scrap", weight=1),
            item_entry(m("ember_core"), weight=1),
        ], rolls={"type": "minecraft:uniform", "min": 1.0, "max": 2.0}),
    ], "random_sequence": m("chests/volcano_lair")})
    loot("chests/ruined_forge", {"type": "minecraft:chest", "pools": [
        pool([
            item_entry("minecraft:iron_ingot", (1, 4), weight=18),
            item_entry("minecraft:iron_nugget", (3, 9), weight=16),
            item_entry("minecraft:coal", (2, 7), weight=15),
            item_entry(m("sulfur"), (2, 5), weight=14),
            item_entry("minecraft:gold_nugget", (2, 7), weight=10),
            item_entry(m("fire_opal"), (1, 2), weight=6),
            item_entry("minecraft:lava_bucket", weight=4),
            item_entry(m("fire_pepper"), (1, 3), weight=8),
        ], rolls={"type": "minecraft:uniform", "min": 3.0, "max": 6.0}),
        pool([
            {"type": "minecraft:empty", "weight": 8},
            item_entry("minecraft:iron_pickaxe", weight=3, functions=[enchant]),
            item_entry("minecraft:iron_sword", weight=3, functions=[enchant]),
            item_entry(m("fire_opal_sword"), weight=1),
            item_entry(m("salamander_boots"), weight=1),
            item_entry(m("titan_armor_trim_smithing_template"), weight=1),
        ]),
    ], "random_sequence": m("chests/ruined_forge")})


# ================================================================================================ recipes

def generate_recipes():
    B, E, R = "building", "equipment", "redstone"
    families = [("scoria", "scoria_stairs", "scoria_slab", "scoria_wall"),
                ("polished_scoria", "polished_scoria_stairs", "polished_scoria_slab", "polished_scoria_wall"),
                ("scoria_bricks", "scoria_brick_stairs", "scoria_brick_slab", "scoria_brick_wall"),
                ("pumice_bricks", "pumice_brick_stairs", "pumice_brick_slab", None),
                ("obsidian_bricks", "obsidian_brick_stairs", "obsidian_brick_slab", "obsidian_brick_wall")]
    for base, st, sl, wa in families:
        shaped(st, ["#  ", "## ", "###"], {"#": m(base)}, m(st), 4, B)
        shaped(sl, ["###"], {"#": m(base)}, m(sl), 6, B)
        if wa:
            shaped(wa, ["###", "###"], {"#": m(base)}, m(wa), 6, B)
    shaped("polished_scoria", ["##", "##"], {"#": m("scoria")}, m("polished_scoria"), 4, B)
    shaped("scoria_bricks", ["##", "##"], {"#": m("polished_scoria")}, m("scoria_bricks"), 4, B)
    shaped("chiseled_scoria_bricks", ["#", "#"], {"#": m("scoria_brick_slab")}, m("chiseled_scoria_bricks"), 1, B)
    cooking("cracked_scoria_bricks", m("scoria_bricks"), m("cracked_scoria_bricks"), 0.1, category="blocks")
    shaped("molten_scoria", ["##", "##"], {"#": "minecraft:magma_block"}, m("molten_scoria"), 4, B)
    shaped("pumice_bricks", ["##", "##"], {"#": m("pumice")}, m("pumice_bricks"), 4, B)
    shaped("obsidian_bricks", ["##", "##"], {"#": "minecraft:obsidian"}, m("obsidian_bricks"), 4, B)
    shaped("chiseled_obsidian", ["#", "#"], {"#": m("obsidian_brick_slab")}, m("chiseled_obsidian"), 1, B)
    cutting = {
        "scoria": ["scoria_stairs", ("scoria_slab", 2), "scoria_wall", "polished_scoria", "polished_scoria_stairs",
                   ("polished_scoria_slab", 2), "polished_scoria_wall", "scoria_bricks", "scoria_brick_stairs",
                   ("scoria_brick_slab", 2), "scoria_brick_wall", "chiseled_scoria_bricks"],
        "polished_scoria": ["polished_scoria_stairs", ("polished_scoria_slab", 2), "polished_scoria_wall",
                            "scoria_bricks", "scoria_brick_stairs", ("scoria_brick_slab", 2), "scoria_brick_wall",
                            "chiseled_scoria_bricks"],
        "scoria_bricks": ["scoria_brick_stairs", ("scoria_brick_slab", 2), "scoria_brick_wall", "chiseled_scoria_bricks"],
        "pumice": ["pumice_bricks", "pumice_brick_stairs", ("pumice_brick_slab", 2)],
        "pumice_bricks": ["pumice_brick_stairs", ("pumice_brick_slab", 2)],
        "obsidian_bricks": ["obsidian_brick_stairs", ("obsidian_brick_slab", 2), "obsidian_brick_wall",
                            "chiseled_obsidian"],
    }
    for source, results in cutting.items():
        for r in results:
            name, n = (r, 1) if isinstance(r, str) else r
            stonecut(m(source), m(name), n)
    for r in ("obsidian_bricks", "obsidian_brick_stairs", ("obsidian_brick_slab", 2), "obsidian_brick_wall",
              "chiseled_obsidian"):
        name, n = (r, 1) if isinstance(r, str) else r
        stonecut("minecraft:obsidian", m(name), n)

    # glass, ash, sand
    cooking("volcanic_glass", m("black_sand"), m("volcanic_glass"), 0.1, category="blocks")
    shaped("volcanic_glass_pane", ["###", "###"], {"#": m("volcanic_glass")}, m("volcanic_glass_pane"), 16, B)
    shaped("ash_block", ["##", "##"], {"#": m("volcanic_ash")}, m("ash_block"), 1, B)
    shapeless("volcanic_ash_from_ash_block", [m("ash_block")], m("volcanic_ash"), 4)
    shaped("ash_layer", ["###"], {"#": m("ash_block")}, m("ash_layer"), 6, B)
    shapeless("black_sand", ["minecraft:sand", "minecraft:sand", "minecraft:sand", "minecraft:sand", m("volcanic_ash")],
              m("black_sand"), 4, B)

    # minerals
    cooking("fire_opal_from_smelting_fire_opal_ore", m("fire_opal_ore"), m("fire_opal"), 1.0, ("smelting", "blasting"))
    cooking("fire_opal_from_smelting_deepslate_fire_opal_ore", m("deepslate_fire_opal_ore"), m("fire_opal"), 1.0,
            ("smelting", "blasting"))
    cooking("sulfur_from_smelting_sulfur_ore", m("sulfur_ore"), m("sulfur"), 0.5, ("smelting", "blasting"))
    shaped("fire_opal_block", ["###", "###", "###"], {"#": m("fire_opal")}, m("fire_opal_block"), 1, B)
    shapeless("fire_opal_from_fire_opal_block", [m("fire_opal_block")], m("fire_opal"), 9)
    shaped("sulfur_block", ["###", "###", "###"], {"#": m("sulfur")}, m("sulfur_block"), 1, B)
    shapeless("sulfur_from_sulfur_block", [m("sulfur_block")], m("sulfur"), 9)
    shapeless("gunpowder_from_sulfur", [m("sulfur"), m("sulfur"), "#minecraft:coals"], "minecraft:gunpowder", 3)
    shapeless("fire_charge_from_sulfur", [m("sulfur"), "minecraft:blaze_powder", "#minecraft:coals"],
              "minecraft:fire_charge", 3)
    shapeless("yellow_dye_from_sulfur", [m("sulfur")], "minecraft:yellow_dye", 1)
    shapeless("orange_dye_from_fireblossom", [m("fireblossom")], "minecraft:orange_dye", 1)
    shapeless("gray_dye_from_volcanic_ash", [m("volcanic_ash")], "minecraft:gray_dye", 1)

    # emberwood
    shapeless("ember_planks", ["#" + m("ember_logs")], m("ember_planks"), 4, B)
    shaped("ember_wood", ["##", "##"], {"#": m("ember_log")}, m("ember_wood"), 3, B)
    shaped("stripped_ember_wood", ["##", "##"], {"#": m("stripped_ember_log")}, m("stripped_ember_wood"), 3, B)
    P = m("ember_planks")
    shaped("ember_stairs", ["#  ", "## ", "###"], {"#": P}, m("ember_stairs"), 4, B)
    shaped("ember_slab", ["###"], {"#": P}, m("ember_slab"), 6, B)
    shaped("ember_fence", ["W#W", "W#W"], {"W": P, "#": "minecraft:stick"}, m("ember_fence"), 3)
    shaped("ember_fence_gate", ["#W#", "#W#"], {"W": P, "#": "minecraft:stick"}, m("ember_fence_gate"), 1, R)
    shaped("ember_door", ["##", "##", "##"], {"#": P}, m("ember_door"), 3, R)
    shaped("ember_trapdoor", ["###", "###"], {"#": P}, m("ember_trapdoor"), 2, R)
    shapeless("ember_button", [P], m("ember_button"), 1, R)
    shaped("ember_pressure_plate", ["##"], {"#": P}, m("ember_pressure_plate"), 1, R)

    # fire opal gear
    O, S = m("fire_opal"), "minecraft:stick"
    shaped("fire_opal_sword", ["O", "O", "S"], {"O": O, "S": S}, m("fire_opal_sword"), 1, E)
    shaped("fire_opal_shovel", ["O", "S", "S"], {"O": O, "S": S}, m("fire_opal_shovel"), 1, E)
    shaped("fire_opal_pickaxe", ["OOO", " S ", " S "], {"O": O, "S": S}, m("fire_opal_pickaxe"), 1, E)
    shaped("fire_opal_axe", ["OO", "OS", " S"], {"O": O, "S": S}, m("fire_opal_axe"), 1, E)
    shaped("fire_opal_hoe", ["OO", " S", " S"], {"O": O, "S": S}, m("fire_opal_hoe"), 1, E)
    shaped("fire_opal_helmet", ["OOO", "O O"], {"O": O}, m("fire_opal_helmet"), 1, E)
    shaped("fire_opal_chestplate", ["O O", "OOO", "OOO"], {"O": O}, m("fire_opal_chestplate"), 1, E)
    shaped("fire_opal_leggings", ["OOO", "O O", "O O"], {"O": O}, m("fire_opal_leggings"), 1, E)
    shaped("fire_opal_boots", ["O O", "O O"], {"O": O}, m("fire_opal_boots"), 1, E)
    shaped("salamander_boots", ["S S", "S S"], {"S": m("salamander_scale")}, m("salamander_boots"), 1, E)

    # mechanics
    shaped("volcanic_forge", ["O O", "OFO", "SSS"], {"O": m("obsidian_bricks"), "F": m("fire_opal"),
                                                     "S": m("polished_scoria")}, m("volcanic_forge"), 1, "misc")
    shaped("pumice_sponge", ["PAP", "AMA", "PAP"], {"P": m("pumice"), "A": m("volcanic_ash"),
                                                    "M": "minecraft:magma_cream"}, m("pumice_sponge"), 1, B)
    shaped("steam_vent", ["S S", "SWS", "SSS"], {"S": m("scoria"), "W": "minecraft:water_bucket"}, m("steam_vent"), 1, R)
    shaped("heart_of_the_volcano", ["MMM", "MEM", "MMM"], {"M": "minecraft:magma_block", "E": m("ember_core")},
           m("heart_of_the_volcano"), 1, B)

    # food & brewing leftovers
    cooking("cooked_crab_meat", m("crab_meat"), m("cooked_crab_meat"), 0.35,
            ("smelting", "smoking", "campfire_cooking"), time=200, category="food")
    shapeless("spicy_stew", ["minecraft:bowl", m("fire_pepper"), m("fire_pepper"), m("cooked_crab_meat")],
              m("spicy_stew"), 1)
    recipe("poison_arrows_from_scorpion_venom", {
        "type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["AAA", "AVA", "AAA"],
        "key": {"A": {"item": "minecraft:arrow"}, "V": {"item": m("scorpion_venom")}},
        "result": {"item": "minecraft:tipped_arrow", "count": 8, "nbt": "{Potion:\"minecraft:poison\"}"}})

    # trims
    shaped("titan_armor_trim_smithing_template", ["DTD", "DSD", "DDD"],
           {"D": "minecraft:diamond", "T": m("titan_armor_trim_smithing_template"), "S": m("scoria")},
           m("titan_armor_trim_smithing_template"), 2)
    recipe("titan_armor_trim_smithing_template_smithing_trim", {
        "type": "minecraft:smithing_trim",
        "template": {"item": m("titan_armor_trim_smithing_template")},
        "base": {"tag": "minecraft:trimmable_armor"},
        "addition": {"tag": "minecraft:trim_materials"}})


# ================================================================================================ tags

def generate_tags():
    mm = lambda names: [m(n) for n in names]
    both = {
        ("minecraft", "planks"): ["ember_planks"],
        ("minecraft", "wooden_stairs"): ["ember_stairs"],
        ("minecraft", "wooden_slabs"): ["ember_slab"],
        ("minecraft", "wooden_fences"): ["ember_fence"],
        ("minecraft", "fence_gates"): ["ember_fence_gate"],
        ("minecraft", "wooden_doors"): ["ember_door"],
        ("minecraft", "wooden_trapdoors"): ["ember_trapdoor"],
        ("minecraft", "wooden_buttons"): ["ember_button"],
        ("minecraft", "wooden_pressure_plates"): ["ember_pressure_plate"],
        ("minecraft", "leaves"): ["ember_leaves"],
        ("minecraft", "saplings"): ["ember_sapling"],
        ("minecraft", "small_flowers"): ["fireblossom"],
        ("minecraft", "sand"): ["black_sand"],
        ("minecraft", "stairs"): ["scoria_stairs", "polished_scoria_stairs", "scoria_brick_stairs",
                                  "pumice_brick_stairs", "obsidian_brick_stairs"],
        ("minecraft", "slabs"): ["scoria_slab", "polished_scoria_slab", "scoria_brick_slab", "pumice_brick_slab",
                                 "obsidian_brick_slab"],
        ("minecraft", "walls"): ["scoria_wall", "polished_scoria_wall", "scoria_brick_wall", "obsidian_brick_wall"],
        (MOD, "ember_logs"): ["ember_log", "ember_wood", "stripped_ember_log", "stripped_ember_wood"],
        ("forge", "storage_blocks/fire_opal"): ["fire_opal_block"],
        ("forge", "storage_blocks/sulfur"): ["sulfur_block"],
        ("forge", "ores/fire_opal"): ["fire_opal_ore", "deepslate_fire_opal_ore"],
        ("forge", "ores/sulfur"): ["sulfur_ore"],
        ("forge", "glass"): ["volcanic_glass"],
        ("forge", "glass_panes"): ["volcanic_glass_pane"],
    }
    for (ns, name), values in both.items():
        for registry in ("blocks", "items"):
            tag(registry, ns, name, mm(values))
    for registry in ("blocks", "items"):
        # emberwood does not burn, so it is a log but not one of the logs_that_burn
        tag(registry, "minecraft", "logs", ["#" + m("ember_logs")])
        tag(registry, "forge", "storage_blocks", ["#forge:storage_blocks/fire_opal", "#forge:storage_blocks/sulfur"])
        tag(registry, "forge", "ores", ["#forge:ores/fire_opal", "#forge:ores/sulfur"])

    pickaxe = SCORIA_BLOCKS + PUMICE_BLOCKS + OBSIDIAN_BLOCKS + [
        "fire_opal_ore", "deepslate_fire_opal_ore", "fire_opal_block", "sulfur_ore", "sulfur_block", "steam_vent",
        "volcanic_forge", "heart_of_the_volcano"]
    tag("blocks", "minecraft", "mineable/pickaxe", mm(pickaxe))
    tag("blocks", "minecraft", "mineable/axe", mm(EMBER_BLOCKS + ["fire_pepper_bush"]))
    tag("blocks", "minecraft", "mineable/shovel", mm(["ash_block", "ash_layer", "black_sand", "cooled_lava_crust"]))
    tag("blocks", "minecraft", "mineable/hoe", mm(["ember_leaves"]))
    tag("blocks", "minecraft", "needs_iron_tool", mm(["fire_opal_ore", "deepslate_fire_opal_ore", "fire_opal_block",
                                                      "heart_of_the_volcano"]))
    tag("blocks", "minecraft", "needs_stone_tool", mm(["sulfur_ore"]))
    tag("blocks", "minecraft", "needs_diamond_tool", mm(OBSIDIAN_BLOCKS))
    tag("blocks", "minecraft", "flower_pots", mm(["potted_ember_sapling", "potted_fireblossom"]))
    tag("blocks", "minecraft", "replaceable_by_trees", mm(["ash_grass", "ash_layer", "sulfur_cluster"]))
    tag("blocks", "minecraft", "impermeable", mm(["volcanic_glass"]))
    tag("blocks", "minecraft", "dragon_immune", mm(OBSIDIAN_BLOCKS + ["heart_of_the_volcano"]))
    tag("blocks", "minecraft", "wither_immune", mm(["heart_of_the_volcano"]))
    tag("blocks", "minecraft", "infiniburn_overworld", mm(["sulfur_block"]))
    tag("blocks", "minecraft", "overworld_carver_replaceables", mm(["scoria", "ash_block", "black_sand", "pumice"]))
    tag("blocks", "minecraft", "dead_bush_may_place_on", mm(["ash_block", "black_sand"]))
    tag("blocks", "minecraft", "beacon_base_blocks", mm(["fire_opal_block"]))
    tag("blocks", "minecraft", "sword_efficient", mm(["fire_pepper_bush", "ember_leaves"]))

    tag("items", "minecraft", "trimmable_armor", mm(["fire_opal_helmet", "fire_opal_chestplate", "fire_opal_leggings",
                                                     "fire_opal_boots", "salamander_boots"]))
    tag("items", "minecraft", "trim_materials", mm(["fire_opal"]))
    tag("items", "minecraft", "trim_templates", mm(["titan_armor_trim_smithing_template"]))
    tag("items", "minecraft", "beacon_payment_items", mm(["fire_opal"]))
    tag("items", "minecraft", "stone_tool_materials", mm(["scoria"]))
    tag("items", "minecraft", "stone_crafting_materials", mm(["scoria"]))
    tag("items", "minecraft", "swords", mm(["fire_opal_sword", "magma_hammer"]))
    tag("items", "minecraft", "shovels", mm(["fire_opal_shovel"]))
    tag("items", "minecraft", "pickaxes", mm(["fire_opal_pickaxe"]))
    tag("items", "minecraft", "axes", mm(["fire_opal_axe"]))
    tag("items", "minecraft", "hoes", mm(["fire_opal_hoe"]))
    tag("items", "minecraft", "piglin_loved", mm(["fire_opal", "fire_opal_block"]))
    tag("items", "forge", "gems/fire_opal", mm(["fire_opal"]))
    tag("items", "forge", "gems", ["#forge:gems/fire_opal"])
    tag("items", "forge", "dusts/sulfur", mm(["sulfur"]))
    tag("items", "forge", "dusts", ["#forge:dusts/sulfur"])
    tag("items", "forge", "crops/fire_pepper", mm(["fire_pepper"]))
    tag("items", "forge", "crops", ["#forge:crops/fire_pepper"])

    tag("entity_types", "minecraft", "fall_damage_immune", mm(["cinder_wraith", "magma_titan"]))
    tag("entity_types", "minecraft", "frog_food", mm(["magmaling"]))

    wastes, isles, caverns = m("ashen_wastes"), m("volcanic_archipelago"), m("magma_caverns")
    tag("worldgen/biome", MOD, "has_ashfall", [wastes, isles])
    tag("worldgen/biome", MOD, "has_structure/volcano", [wastes, isles])
    tag("worldgen/biome", "minecraft", "is_overworld", [wastes, isles, caverns])
    tag("worldgen/biome", "minecraft", "is_ocean", [isles])
    for name in ("has_structure/mineshaft", "has_structure/ruined_portal_standard", "stronghold_biased_to"):
        tag("worldgen/biome", "minecraft", name, [wastes])
    tag("worldgen/biome", "minecraft", "has_structure/mineshaft", [caverns])
    tag("worldgen/biome", "minecraft", "stronghold_biased_to", [caverns])
    for name in ("has_structure/ruined_portal_ocean", "has_structure/shipwreck", "has_structure/ocean_ruin_warm"):
        tag("worldgen/biome", "minecraft", name, [isles])
    tag("worldgen/biome", "minecraft", "snow_golem_melts", [wastes, isles, caverns])
    tag("worldgen/biome", "minecraft", "spawns_warm_variant_frogs", [wastes, isles, caverns])
    tag("worldgen/biome", "minecraft", "increased_fire_burnout", [wastes])
    for name in ("is_hot", "is_hot/overworld"):
        tag("worldgen/biome", "forge", name, [wastes, isles, caverns])
    for name in ("is_dry", "is_dry/overworld", "is_wasteland"):
        tag("worldgen/biome", "forge", name, [wastes])
    tag("worldgen/biome", "forge", "is_underground", [caverns])


# ================================================================================================ worldgen

FEATURE_ORDER = ["ore_scoria", "ore_fire_opal", "ore_sulfur", "hot_spring", "basalt_pillars", "fumarole",
                 "ruined_forge", "magma_floor", "magma_ceiling", "ember_trees", "ember_trees_sparse", "patch_ash_grass",
                 "flower_fireblossom", "patch_fire_pepper", "patch_sulfur_cluster"]


def append(features, step, names):
    """Adds mod features at the end of a step, always in FEATURE_ORDER so biomes never disagree on ordering."""
    names = sorted(names, key=FEATURE_ORDER.index)
    features[step] = features[step] + [m(n) for n in names]


def env_scan(direction):
    return {"type": "minecraft:environment_scan", "direction_of_search": direction, "max_steps": 12,
            "target_condition": {"type": "minecraft:solid"},
            "allowed_search_condition": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}}


def weighted(entries):
    return {"type": "minecraft:weighted_state_provider",
            "entries": [{"data": state, "weight": w} for state, w in entries]}


def scorched_patch(surface, vegetation_chance):
    basalt = {"Name": "minecraft:basalt", "Properties": {"axis": "y"}}
    ground = [({"Name": m("scoria")}, 5), (basalt, 4), ({"Name": "minecraft:magma_block"}, 3),
              ({"Name": m("molten_scoria")}, 2)]
    config = {
        "depth": {"type": "minecraft:uniform", "value": {"min_inclusive": 1, "max_inclusive": 2}},
        "extra_bottom_block_chance": 0.0, "extra_edge_column_chance": 0.3,
        "ground_state": weighted(ground), "replaceable": "#minecraft:base_stone_overworld", "surface": surface,
        "vegetation_chance": vegetation_chance, "vertical_range": 5,
        "xz_radius": {"type": "minecraft:uniform", "value": {"min_inclusive": 3, "max_inclusive": 7}},
        "vegetation_feature": {"feature": {"type": "minecraft:simple_block", "config": {"to_place": weighted(
            [({"Name": m("sulfur_cluster")}, 3), ({"Name": m("ash_grass")}, 2)])}}, "placement": []},
    }
    return {"type": "minecraft:vegetation_patch", "config": config}


def generate_worldgen():
    none = {"config": {}}
    for feature in ("basalt_pillars", "fumarole", "hot_spring", "ruined_forge"):
        worldgen("configured_feature", feature, {"type": m(feature), **none})
    worldgen("configured_feature", "ember_tree", vanilla_json("data", "worldgen/configured_feature/acacia", [
        ("minecraft:acacia_log", m("ember_log")), ("minecraft:acacia_leaves", m("ember_leaves")),
        ('"minecraft:dirt"', '"%s"' % m("ash_block"))]))
    worldgen("configured_feature", "ore_scoria", ore([
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:base_stone_overworld"},
         "state": {"Name": m("scoria")}}], 48))
    worldgen("configured_feature", "ore_fire_opal", ore([
        {"target": {"predicate_type": "minecraft:block_match", "block": m("scoria")}, "state": {"Name": m("fire_opal_ore")}},
        {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:basalt"},
         "state": {"Name": m("fire_opal_ore")}},
        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
         "state": {"Name": m("deepslate_fire_opal_ore")}}], 5))
    worldgen("configured_feature", "ore_sulfur", ore([
        {"target": {"predicate_type": "minecraft:block_match", "block": m("scoria")}, "state": {"Name": m("sulfur_ore")}},
        {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:basalt"},
         "state": {"Name": m("sulfur_ore")}},
        {"target": {"predicate_type": "minecraft:block_match", "block": "minecraft:tuff"},
         "state": {"Name": m("sulfur_ore")}}], 9))
    worldgen("configured_feature", "patch_ash_grass", patch({"Name": m("ash_grass")}, 32, 7))
    worldgen("configured_feature", "flower_fireblossom", patch({"Name": m("fireblossom")}, 16, 5))
    worldgen("configured_feature", "patch_fire_pepper", patch({"Name": m("fire_pepper_bush"),
                                                               "Properties": {"age": "3"}}, 12, 4))
    worldgen("configured_feature", "patch_sulfur_cluster", patch({"Name": m("sulfur_cluster")}, 14, 5))
    worldgen("configured_feature", "magma_floor", scorched_patch("floor", 0.12))
    worldgen("configured_feature", "magma_ceiling", scorched_patch("ceiling", 0.0))

    surface = heightmap("WORLD_SURFACE_WG")
    motion = heightmap("MOTION_BLOCKING")
    ocean_floor = heightmap("OCEAN_FLOOR")
    sapling = {"type": "minecraft:block_predicate_filter", "predicate": {
        "type": "minecraft:would_survive", "state": {"Name": m("ember_sapling"), "Properties": {"stage": "0"}}}}
    dry = {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0}
    height = lambda lo, hi: {"type": "minecraft:height_range", "height": {
        "type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}
    cave_floor = [count(70), IN_SQUARE, height({"above_bottom": 0}, {"absolute": 60}), env_scan("down"),
                  {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": 1}, BIOME]
    cave_ceiling = [count(35), IN_SQUARE, height({"above_bottom": 0}, {"absolute": 60}), env_scan("up"),
                    {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": -1}, BIOME]
    placed = {
        "ore_scoria": ("ore_scoria", [count(6), IN_SQUARE, height({"absolute": 0}, {"absolute": 110}), BIOME]),
        "ore_fire_opal": ("ore_fire_opal", [count(10), IN_SQUARE, height({"absolute": -48}, {"absolute": 110}), BIOME]),
        "ore_sulfur": ("ore_sulfur", [count(16), IN_SQUARE, height({"absolute": -16}, {"absolute": 128}), BIOME]),
        "hot_spring": ("hot_spring", [rarity(9), IN_SQUARE, surface, BIOME]),
        "basalt_pillars": ("basalt_pillars", [rarity(5), IN_SQUARE, surface, BIOME]),
        "fumarole": ("fumarole", [rarity(3), IN_SQUARE, surface, BIOME]),
        "ruined_forge": ("ruined_forge", [rarity(45), IN_SQUARE, surface, BIOME]),
        "magma_floor": ("magma_floor", cave_floor),
        "magma_ceiling": ("magma_ceiling", cave_ceiling),
        "ember_trees": ("ember_tree", [count(1), rarity(2), IN_SQUARE, dry, ocean_floor, BIOME, sapling]),
        "ember_trees_sparse": ("ember_tree", [rarity(7), IN_SQUARE, dry, ocean_floor, BIOME, sapling]),
        "patch_ash_grass": ("patch_ash_grass", [count(4), IN_SQUARE, motion, BIOME]),
        "flower_fireblossom": ("flower_fireblossom", [rarity(3), IN_SQUARE, motion, BIOME]),
        "patch_fire_pepper": ("patch_fire_pepper", [rarity(10), IN_SQUARE, motion, BIOME]),
        "patch_sulfur_cluster": ("patch_sulfur_cluster", [rarity(3), IN_SQUARE, motion, BIOME]),
    }
    for name, (feature, modifiers) in placed.items():
        worldgen("placed_feature", name, {"feature": m(feature), "placement": modifiers})

    spawn = lambda t, w, lo, hi: {"type": t, "weight": w, "minCount": lo, "maxCount": hi}
    basalt_music = {"sound": "minecraft:music.nether.basalt_deltas", "min_delay": 12000, "max_delay": 24000,
                    "replace_current_music": False}

    # Ashen Wastes: savanna turned to ash
    savanna = json.loads(vanilla("data", "worldgen/biome/savanna"))
    f = savanna["features"]
    f[9] = [x for x in f[9] if x in ("minecraft:glow_lichen", "minecraft:brown_mushroom_normal",
                                      "minecraft:red_mushroom_normal")]
    append(f, 1, ["hot_spring"])
    append(f, 4, ["basalt_pillars", "fumarole", "ruined_forge"])
    append(f, 6, ["ore_scoria", "ore_fire_opal", "ore_sulfur"])
    append(f, 9, ["ember_trees", "patch_ash_grass", "flower_fireblossom", "patch_fire_pepper", "patch_sulfur_cluster"])
    worldgen("biome", "ashen_wastes", {
        "has_precipitation": False, "temperature": 2.0, "downfall": 0.0,
        "effects": {"sky_color": 0x9C8C84, "fog_color": 0x847670, "water_color": 0x4E8C8A, "water_fog_color": 0x1E3434,
                    "grass_color": 0x77705E, "foliage_color": 0x6E5E4C, "mood_sound": savanna["effects"]["mood_sound"],
                    "music": basalt_music,
                    "particle": {"options": {"type": "minecraft:white_ash"}, "probability": 0.006}},
        "carvers": savanna["carvers"], "features": f, "spawn_costs": {},
        "spawners": {
            "ambient": [spawn("minecraft:bat", 10, 8, 8)], "axolotls": [],
            "creature": [spawn(m("salamander"), 10, 1, 3), spawn(m("lava_crab"), 6, 2, 4)],
            "misc": [],
            "monster": [spawn("minecraft:spider", 80, 2, 3), spawn("minecraft:zombie", 80, 2, 4),
                        spawn("minecraft:skeleton", 100, 2, 4), spawn("minecraft:creeper", 90, 2, 4),
                        spawn("minecraft:enderman", 10, 1, 4), spawn("minecraft:witch", 5, 1, 1),
                        spawn(m("magmaling"), 70, 1, 3), spawn(m("cinder_wraith"), 30, 1, 2)],
            "underground_water_creature": [spawn("minecraft:glow_squid", 10, 4, 6)],
            "water_ambient": [], "water_creature": []}})

    # Volcanic Archipelago: warm seas with black sand islands and volcanoes
    ocean = json.loads(vanilla("data", "worldgen/biome/lukewarm_ocean"))
    f = ocean["features"]
    f[9] = [x for x in f[9] if x not in ("minecraft:trees_water", "minecraft:flower_default",
                                          "minecraft:patch_grass_badlands", "minecraft:patch_sugar_cane",
                                          "minecraft:patch_pumpkin")]
    append(f, 4, ["basalt_pillars", "fumarole"])
    append(f, 6, ["ore_scoria", "ore_fire_opal", "ore_sulfur"])
    append(f, 9, ["ember_trees_sparse", "patch_ash_grass", "flower_fireblossom", "patch_sulfur_cluster"])
    spawners = ocean["spawners"]
    spawners["creature"] = [spawn(m("lava_crab"), 12, 2, 4), spawn(m("salamander"), 4, 1, 2)]
    spawners["monster"] = [s for s in spawners["monster"] if s["type"] != "minecraft:slime"] + [spawn(m("magmaling"), 40, 1, 2)]
    spawners["water_ambient"] = [spawn("minecraft:tropical_fish", 25, 8, 8), spawn("minecraft:pufferfish", 15, 1, 3)]
    worldgen("biome", "volcanic_archipelago", {
        "has_precipitation": False, "temperature": 1.5, "downfall": 0.0,
        "effects": {"sky_color": 0x8FA4C4, "fog_color": 0xA89A92, "water_color": 0x2FB8B0, "water_fog_color": 0x0A3438,
                    "grass_color": 0x6E8A4A, "foliage_color": 0x5E7A3A, "mood_sound": ocean["effects"]["mood_sound"]},
        "carvers": ocean["carvers"], "features": f, "spawn_costs": {}, "spawners": spawners})

    # Magma Caverns: the underground under a volcano
    caves = json.loads(vanilla("data", "worldgen/biome/dripstone_caves"))
    f = caves["features"]
    f[2] = [x for x in f[2] if x != "minecraft:large_dripstone"]
    f[7] = [x for x in f[7] if x not in ("minecraft:dripstone_cluster", "minecraft:pointed_dripstone")]
    append(f, 6, ["ore_scoria", "ore_fire_opal", "ore_sulfur"])
    append(f, 7, ["magma_floor", "magma_ceiling"])
    spawners = caves["spawners"]
    spawners["monster"] = [s for s in spawners["monster"] if s["type"] not in ("minecraft:drowned", "minecraft:slime")] \
        + [spawn(m("magmaling"), 100, 1, 3), spawn(m("cinder_wraith"), 40, 1, 2), spawn("minecraft:magma_cube", 30, 1, 2)]
    worldgen("biome", "magma_caverns", {
        "has_precipitation": True, "temperature": 1.2, "downfall": 0.2,
        "effects": {"sky_color": 0x9C7C6C, "fog_color": 0x6A3422, "water_color": 0x3F76E4, "water_fog_color": 0x050533,
                    "mood_sound": caves["effects"]["mood_sound"], "music": basalt_music,
                    "particle": {"options": {"type": "minecraft:ash"}, "probability": 0.004}},
        "carvers": caves["carvers"], "features": f, "spawn_costs": {}, "spawners": spawners})

    # The volcano
    worldgen("structure", "volcano", {
        "type": m("volcano"), "biomes": "#" + m("has_structure/volcano"), "step": "surface_structures",
        "terrain_adaptation": "none",
        "spawn_overrides": {"monster": {"bounding_box": "piece", "spawns": [
            spawn(m("magmaling"), 10, 1, 3), spawn(m("cinder_wraith"), 3, 1, 1)]}}})
    worldgen("structure_set", "volcano", {
        "structures": [{"structure": m("volcano"), "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "salt": 571930213, "spacing": 24, "separation": 10}})


# ================================================================================================ misc

def generate_misc_data():
    write(os.path.join(DATA, MOD, "trim_pattern/titan.json"), {
        "asset_id": m("titan"), "description": {"translate": "trim_pattern.dunesrelics.titan"},
        "template_item": m("titan_armor_trim_smithing_template")})
    write(os.path.join(DATA, MOD, "trim_material/fire_opal.json"), {
        "asset_name": "fire_opal", "description": {"color": "#F06A22", "translate": "trim_material.dunesrelics.fire_opal"},
        "ingredient": m("fire_opal"), "item_model_index": 0.45})
    generate_advancements()


def generate_advancements():
    def adv(name, parent, icon, criteria, frame="task", rewards=None, toast=True, requirements=None):
        obj = {"display": {"icon": {"item": icon}, "title": {"translate": "advancements.dunesrelics.%s.title" % name},
                           "description": {"translate": "advancements.dunesrelics.%s.description" % name},
                           "frame": frame, "show_toast": toast, "announce_to_chat": toast, "hidden": False},
               "criteria": criteria}
        if parent:
            obj["parent"] = m(parent)
        else:
            obj["display"]["background"] = "dunesrelics:textures/block/scoria.png"
        if rewards:
            obj["rewards"] = rewards
        if requirements:
            obj["requirements"] = requirements
        write(os.path.join(DATA, MOD, "advancements", name + ".json"), obj)

    entity = lambda t: [{"condition": "minecraft:entity_properties", "entity": "this", "predicate": {"type": t}}]
    has = lambda item, nbt=None: {"has": {"trigger": "minecraft:inventory_changed", "conditions": {
        "items": [dict({"items": [item]}, **({"nbt": nbt} if nbt else {}))]}}}
    biome = lambda b: {"trigger": "minecraft:location", "conditions": {"player": [{
        "condition": "minecraft:entity_properties", "entity": "this", "predicate": {"location": {"biome": m(b)}}}]}}

    adv("volcanic_root", None, m("heart_of_the_volcano"), {"tick": {"trigger": "minecraft:tick"}}, toast=False)
    adv("ashen_lands", "volcanic_root", m("ash_block"),
        {"wastes": biome("ashen_wastes"), "isles": biome("volcanic_archipelago"), "caverns": biome("magma_caverns")},
        requirements=[["wastes", "isles", "caverns"]])
    adv("fire_opal", "ashen_lands", m("fire_opal"), has(m("fire_opal")))
    adv("tempered", "fire_opal", m("volcanic_forge"), {
        "tempered": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [
            {"nbt": "{dunesrelics_tempered:1b}"}]}}}, frame="goal")
    adv("salamander", "ashen_lands", m("fire_pepper"), {"tamed": {"trigger": "minecraft:tame_animal", "conditions": {
        "entity": entity(m("salamander"))}}})
    adv("salamander_boots", "salamander", m("salamander_boots"), has(m("salamander_boots")))
    adv("crab_bucket", "ashen_lands", m("lava_crab_bucket"), {"filled": {"trigger": "minecraft:filled_bucket",
                                                                        "conditions": {"item": {"items": [m("lava_crab_bucket")]}}}})
    adv("molten_sponge", "ashen_lands", m("pumice_sponge"), has(m("molten_pumice_sponge")))
    adv("spicy_stew", "salamander", m("spicy_stew"), {"ate": {"trigger": "minecraft:consume_item", "conditions": {
        "item": {"items": [m("spicy_stew")]}}}})
    adv("ruined_forge", "ashen_lands", m("scoria_bricks"), {"looted": {
        "trigger": "minecraft:player_generates_container_loot", "conditions": {"loot_table": m("chests/ruined_forge")}}})
    adv("volcano", "ashen_lands", m("chiseled_obsidian"), {"looted": {
        "trigger": "minecraft:player_generates_container_loot", "conditions": {"loot_table": m("chests/volcano_lair")}}},
        frame="goal")
    adv("magma_titan", "volcano", m("magma_hammer"), {"killed": {"trigger": "minecraft:player_killed_entity",
                                                                 "conditions": {"entity": entity(m("magma_titan"))}}},
        frame="challenge", rewards={"experience": 150})
    adv("scorpion_venom", "ancient_dunes", m("scorpion_venom"), has(m("scorpion_venom")))


# ================================================================================================ translations

BLOCKS = {
    "scoria": ("Scoria", "Шлак"),
    "scoria_stairs": ("Scoria Stairs", "Шлаковые ступеньки"),
    "scoria_slab": ("Scoria Slab", "Шлаковая плита"),
    "scoria_wall": ("Scoria Wall", "Шлаковая ограда"),
    "polished_scoria": ("Polished Scoria", "Полированный шлак"),
    "polished_scoria_stairs": ("Polished Scoria Stairs", "Ступеньки из полированного шлака"),
    "polished_scoria_slab": ("Polished Scoria Slab", "Плита из полированного шлака"),
    "polished_scoria_wall": ("Polished Scoria Wall", "Ограда из полированного шлака"),
    "scoria_bricks": ("Scoria Bricks", "Шлаковые кирпичи"),
    "scoria_brick_stairs": ("Scoria Brick Stairs", "Ступеньки из шлаковых кирпичей"),
    "scoria_brick_slab": ("Scoria Brick Slab", "Плита из шлаковых кирпичей"),
    "scoria_brick_wall": ("Scoria Brick Wall", "Ограда из шлаковых кирпичей"),
    "cracked_scoria_bricks": ("Cracked Scoria Bricks", "Потрескавшиеся шлаковые кирпичи"),
    "chiseled_scoria_bricks": ("Chiseled Scoria Bricks", "Резные шлаковые кирпичи"),
    "molten_scoria": ("Molten Scoria", "Раскалённый шлак"),
    "pumice": ("Pumice", "Пемза"),
    "pumice_bricks": ("Pumice Bricks", "Пемзовые кирпичи"),
    "pumice_brick_stairs": ("Pumice Brick Stairs", "Ступеньки из пемзовых кирпичей"),
    "pumice_brick_slab": ("Pumice Brick Slab", "Плита из пемзовых кирпичей"),
    "pumice_sponge": ("Pumice Sponge", "Пемзовая губка"),
    "molten_pumice_sponge": ("Molten Pumice Sponge", "Раскалённая пемзовая губка"),
    "obsidian_bricks": ("Obsidian Bricks", "Обсидиановые кирпичи"),
    "obsidian_brick_stairs": ("Obsidian Brick Stairs", "Ступеньки из обсидиановых кирпичей"),
    "obsidian_brick_slab": ("Obsidian Brick Slab", "Плита из обсидиановых кирпичей"),
    "obsidian_brick_wall": ("Obsidian Brick Wall", "Ограда из обсидиановых кирпичей"),
    "chiseled_obsidian": ("Chiseled Obsidian", "Резной обсидиан"),
    "volcanic_glass": ("Volcanic Glass", "Вулканическое стекло"),
    "volcanic_glass_pane": ("Volcanic Glass Pane", "Панель из вулканического стекла"),
    "ash_block": ("Block of Ash", "Блок пепла"),
    "ash_layer": ("Ash", "Пепел"),
    "black_sand": ("Black Sand", "Чёрный песок"),
    "fire_opal_ore": ("Fire Opal Ore", "Руда огненного опала"),
    "deepslate_fire_opal_ore": ("Deepslate Fire Opal Ore", "Глубинная руда огненного опала"),
    "fire_opal_block": ("Block of Fire Opal", "Блок огненного опала"),
    "sulfur_ore": ("Sulfur Ore", "Серная руда"),
    "sulfur_block": ("Block of Sulfur", "Блок серы"),
    "sulfur_cluster": ("Sulfur Cluster", "Друза серы"),
    "ember_log": ("Emberwood Log", "Бревно тлеющего дерева"),
    "ember_wood": ("Emberwood", "Тлеющая древесина"),
    "stripped_ember_log": ("Stripped Emberwood Log", "Обтёсанное бревно тлеющего дерева"),
    "stripped_ember_wood": ("Stripped Emberwood", "Обтёсанная тлеющая древесина"),
    "ember_planks": ("Emberwood Planks", "Доски тлеющего дерева"),
    "ember_stairs": ("Emberwood Stairs", "Ступеньки из тлеющего дерева"),
    "ember_slab": ("Emberwood Slab", "Плита из тлеющего дерева"),
    "ember_fence": ("Emberwood Fence", "Забор из тлеющего дерева"),
    "ember_fence_gate": ("Emberwood Fence Gate", "Калитка из тлеющего дерева"),
    "ember_door": ("Emberwood Door", "Дверь из тлеющего дерева"),
    "ember_trapdoor": ("Emberwood Trapdoor", "Люк из тлеющего дерева"),
    "ember_button": ("Emberwood Button", "Кнопка из тлеющего дерева"),
    "ember_pressure_plate": ("Emberwood Pressure Plate", "Нажимная плита из тлеющего дерева"),
    "ember_leaves": ("Ember Leaves", "Тлеющая листва"),
    "ember_sapling": ("Emberwood Sapling", "Саженец тлеющего дерева"),
    "potted_ember_sapling": ("Potted Emberwood Sapling", "Саженец тлеющего дерева в горшке"),
    "ash_grass": ("Ash Grass", "Пепельная трава"),
    "fireblossom": ("Fireblossom", "Огнецвет"),
    "potted_fireblossom": ("Potted Fireblossom", "Огнецвет в горшке"),
    "lava_lily": ("Lava Lily", "Лавовая кувшинка"),
    "fire_pepper_bush": ("Fire Pepper Bush", "Куст огненного перца"),
    "steam_vent": ("Steam Vent", "Паровой гейзер"),
    "volcanic_forge": ("Volcanic Forge", "Вулканическая кузня"),
    "heart_of_the_volcano": ("Heart of the Volcano", "Сердце вулкана"),
    "cooled_lava_crust": ("Cooled Lava Crust", "Застывшая лавовая корка"),
}

ITEMS = {
    "fire_opal": ("Fire Opal", "Огненный опал"),
    "sulfur": ("Sulfur", "Сера"),
    "volcanic_ash": ("Volcanic Ash", "Вулканический пепел"),
    "salamander_scale": ("Salamander Scale", "Чешуя саламандры"),
    "ember_core": ("Ember Core", "Тлеющее ядро"),
    "lava_crab_bucket": ("Bucket of Lava Crab", "Лавовый краб в ведре"),
    "fire_opal_sword": ("Fire Opal Sword", "Меч из огненного опала"),
    "fire_opal_shovel": ("Fire Opal Shovel", "Лопата из огненного опала"),
    "fire_opal_pickaxe": ("Fire Opal Pickaxe", "Кирка из огненного опала"),
    "fire_opal_axe": ("Fire Opal Axe", "Топор из огненного опала"),
    "fire_opal_hoe": ("Fire Opal Hoe", "Мотыга из огненного опала"),
    "fire_opal_helmet": ("Fire Opal Helmet", "Шлем из огненного опала"),
    "fire_opal_chestplate": ("Fire Opal Chestplate", "Нагрудник из огненного опала"),
    "fire_opal_leggings": ("Fire Opal Leggings", "Поножи из огненного опала"),
    "fire_opal_boots": ("Fire Opal Boots", "Ботинки из огненного опала"),
    "salamander_boots": ("Salamander Boots", "Сапоги саламандры"),
    "magma_hammer": ("Magma Hammer", "Магмовый молот"),
    "fire_pepper": ("Fire Pepper", "Огненный перец"),
    "crab_meat": ("Raw Crab Meat", "Сырое крабовое мясо"),
    "cooked_crab_meat": ("Cooked Crab Meat", "Жареное крабовое мясо"),
    "spicy_stew": ("Spicy Stew", "Острое рагу"),
    "titan_armor_trim_smithing_template": ("Smithing Template", "Кузнечный шаблон"),
    "scorpion_venom": ("Scorpion Venom", "Яд скорпиона"),
    "magma_titan_spawn_egg": ("Magma Titan Spawn Egg", "Яйцо призыва магмового титана"),
    "salamander_spawn_egg": ("Salamander Spawn Egg", "Яйцо призыва саламандры"),
    "lava_crab_spawn_egg": ("Lava Crab Spawn Egg", "Яйцо призыва лавового краба"),
    "magmaling_spawn_egg": ("Magmaling Spawn Egg", "Яйцо призыва магмыша"),
    "cinder_wraith_spawn_egg": ("Cinder Wraith Spawn Egg", "Яйцо призыва пепельного призрака"),
}

ENTITIES = {
    "magma_titan": ("Magma Titan", "Магмовый титан"),
    "salamander": ("Salamander", "Саламандра"),
    "lava_crab": ("Lava Crab", "Лавовый краб"),
    "magmaling": ("Magmaling", "Магмыш"),
    "cinder_wraith": ("Cinder Wraith", "Пепельный призрак"),
    "volcanic_bomb": ("Volcanic Bomb", "Вулканическая бомба"),
}

ADVANCEMENTS = {
    "volcanic_root": (("Ash & Ember", "Where the ground is still being made"),
                      ("Пепел и Угли", "Там, где земля всё ещё рождается")),
    "ashen_lands": (("Hot Ground", "Reach the Ashen Wastes, the Volcanic Archipelago or the Magma Caverns"),
                    ("Горячая земля", "Доберитесь до Пепельных пустошей, Вулканического архипелага или Магмовых пещер")),
    "fire_opal": (("Play of Colour", "Mine a Fire Opal"), ("Игра цвета", "Добудьте огненный опал")),
    "tempered": (("Forged in Fire", "Temper a tool or armor piece in a Volcanic Forge"),
                 ("Закалённое в огне", "Закалите инструмент или броню в вулканической кузне")),
    "salamander": (("Hot-Blooded Friend", "Tame a Salamander with a Fire Pepper"),
                   ("Горячий друг", "Приручите саламандру огненным перцем")),
    "salamander_boots": (("Lava Walker", "Make Salamander Boots from shed scales"),
                         ("Ходок по лаве", "Сделайте сапоги саламандры из сброшенной чешуи")),
    "crab_bucket": (("Hot Catch", "Scoop up a Lava Crab with a lava bucket"),
                    ("Горячий улов", "Поймайте лавового краба ведром лавы")),
    "molten_sponge": (("Soaked in Magma", "Let a Pumice Sponge soak up lava"),
                      ("Пропитано магмой", "Дайте пемзовой губке впитать лаву")),
    "spicy_stew": (("Too Hot to Handle", "Eat a Spicy Stew"), ("Остренько!", "Съешьте острое рагу")),
    "ruined_forge": (("Cold Anvils", "Loot the chest of a ruined forge"),
                     ("Остывшие наковальни", "Обыщите сундук в разрушенной кузне")),
    "volcano": (("Into the Caldera", "Reach the magma chamber deep inside a volcano and loot its treasure"),
                ("В кальдеру", "Доберитесь до магматической камеры в глубине вулкана и заберите сокровища")),
    "magma_titan": (("Titanomachy", "Awaken the Magma Titan... and defeat it"),
                    ("Титаномахия", "Пробудите магмового титана... и одолейте его")),
    "scorpion_venom": (("Handle with Care", "Milk a Scorpion with a glass bottle"),
                       ("Осторожно, яд!", "Сцедите яд скорпиона в стеклянный пузырёк")),
}

EXTRA = {
    "biome.dunesrelics.ashen_wastes": ("Ashen Wastes", "Пепельные пустоши"),
    "biome.dunesrelics.volcanic_archipelago": ("Volcanic Archipelago", "Вулканический архипелаг"),
    "biome.dunesrelics.magma_caverns": ("Magma Caverns", "Магмовые пещеры"),
    "trim_pattern.dunesrelics.titan": ("Titan Armor Trim", "Отделка «Титан»"),
    "trim_material.dunesrelics.fire_opal": ("Fire Opal Material", "Материал: огненный опал"),
    "block.dunesrelics.heart_of_the_volcano.peaceful": ("The heart beats, but nothing answers... in peace.",
                                                        "Сердце бьётся, но никто не отзывается... в мирном режиме."),
    "entity.dunesrelics.magma_titan.enraged": ("The Magma Titan erupts with rage!", "Магмовый титан извергается от ярости!"),
    "block.dunesrelics.volcanic_forge.no_lava": ("The forge needs lava. Pour in a lava bucket.",
                                                 "Кузне нужна лава. Вылейте в неё ведро лавы."),
    "block.dunesrelics.volcanic_forge.no_opal": ("Tempering needs a Fire Opal in your inventory.",
                                                 "Для закалки нужен огненный опал в инвентаре."),
    "block.dunesrelics.volcanic_forge.already": ("This item is already tempered.", "Этот предмет уже закалён."),
    "block.dunesrelics.volcanic_forge.tempered": ("Tempered!", "Закалено!"),
    "item.dunesrelics.tempered": ("Tempered", "Закалённое"),
    "item.dunesrelics.magma_hammer.desc": ("Use: slam the ground, scorching everything around",
                                           "Использование: удар оземь, опаляющий всё вокруг"),
}

"""Pixel art for the volcanic update ("Ash & Ember"): scoria, pumice, obsidian bricks, ash, emberwood,
fire opal gear, the volcanic forge, the Heart of the Volcano and friends.

Called from textures.py (python3 tools/textures.py) but can also be run on its own.
"""
import json
import math
import os
import random

from PIL import Image

from pixels import Palette, shade, mix, hex_rgb
from textures import (TEX, CLEAR, new, put, get, save, value_noise, from_field, bricks_base, sprite, fill_faces,
                      HEAD, BODY, ARM, LEG, TOOL_SPRITES, ARMOR_SPRITES, STICK)

SCORIA = Palette("#2e1714", "#3f201b", "#512a22", "#62352a", "#744033", "#874d3c")
SCORIA_BRICK = Palette("#3a1d18", "#4b261f", "#5c3027", "#6d3a2f", "#7f4638", "#925443")
PUMICE = Palette("#8f887c", "#a19a8e", "#b2ac9f", "#c2bcb0", "#d1ccc1", "#e0dcd2")
ASH = Palette("#403d3a", "#4c4946", "#585551", "#65615d", "#726e69", "#827e78")
BLACK_SAND = Palette("#141211", "#1c1917", "#24201e", "#2d2825", "#37312d", "#443c37")
OBSIDIAN = Palette("#0b0812", "#140e20", "#1d152d", "#281d3c", "#35274f", "#473668")
DEEPSLATE = Palette("#26262b", "#303035", "#3a3a40", "#45454b", "#515157", "#5e5e64")
BASALT = Palette("#2a2a2d", "#353538", "#404044", "#4c4c50", "#58585c", "#66666a")
LAVA = Palette("#5a1000", "#9a2400", "#d44a00", "#f47414", "#ffa434", "#ffd466", "#fff2b0")
OPAL = Palette("#5c1406", "#962a0c", "#cc4616", "#ee6a22", "#ff9640", "#ffc47a", "#ffe8b8")
OPAL_FLECKS = [hex_rgb("#5ef0b0"), hex_rgb("#58b8ff"), hex_rgb("#ff5a8a"), hex_rgb("#fff05a")]
SULFUR = Palette("#7a6a0c", "#a69314", "#cdb920", "#e8d63c", "#f8ea6a", "#fff8aa")
CHAR = Palette("#0f0b0a", "#181211", "#221a17", "#2d231e", "#3a2d26", "#4a3a30")
EMBER_WOOD = Palette("#5e1e0c", "#7c2c12", "#9a3c18", "#b44e20", "#cc622a", "#e27c38")
EMBER_LEAF = Palette("#6e160a", "#9a220c", "#c43812", "#e2561a", "#f47c26", "#ffaa3c", "#ffd068")
ASH_PLANT = Palette("#3e3c38", "#57544f", "#716d67", "#8c8780", "#a8a39b")
STEEL = Palette("#1a1716", "#262120", "#332c2a", "#423936", "#544844", "#6a5b55")
SCALE = Palette("#0e0a09", "#1c1614", "#2a2220", "#3a302c", "#4c403a")
SAL_ORANGE = Palette("#a04a0c", "#d0661a", "#f08a1c", "#ffb04a")


def save_anim(frames, path, frametime, interpolate=True):
    """Stacks frames vertically and writes the matching .mcmeta."""
    w, h = frames[0].size
    strip = new(w, h * len(frames))
    for i, frame in enumerate(frames):
        strip.paste(frame, (0, i * h))
    save(strip, path)
    with open(os.path.join(TEX, path + ".png.mcmeta"), "w", encoding="utf-8") as out:
        json.dump({"animation": {"frametime": frametime, "interpolate": interpolate}}, out, indent=2)
        out.write("\n")


def outline(img, color):
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            if img.getpixel((x, y))[3] == 0 and any(
                    0 <= x + dx < img.width and 0 <= y + dy < img.height and img.getpixel((x + dx, y + dy))[3] > 0
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                put(out, x, y, color)
    return out


def recolor(img, mapping):
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            c = img.getpixel((x, y))
            if c[3] and c[:3] in mapping:
                put(out, x, y, mapping[c[:3]])
    return out


def crack_walk(img, rng, starts, color, steps=(4, 8), glow=None):
    for start in starts:
        x, y = start
        for _ in range(rng.randint(*steps)):
            put(img, x % 16, y % 16, color)
            if glow is not None and rng.random() < 0.5:
                put(img, (x + 1) % 16, y % 16, glow)
            x += rng.choice([-1, 0, 1])
            y += rng.choice([0, 1, 1])


def vesicles(img, rng, count, pal, big=False):
    """Gas bubbles frozen in the rock: a dark pit with a lit lower rim."""
    for _ in range(count):
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, pal.c(0))
        if big or rng.random() < 0.4:
            put(img, (x + 1) % 16, y, pal.c(0))
            put(img, (x + 1) % 16, (y + 1) % 16, pal.c(len(pal) - 1))
        put(img, x, (y + 1) % 16, pal.c(len(pal) - 2))


# ------------------------------------------------------------------------------------------ scoria

def scoria(seed=601):
    field = value_noise(16, 16, seed, scale=8, octaves=3)
    img = from_field([[0.15 + v * 0.75 for v in row] for row in field], SCORIA, 0.3, seed + 1)
    vesicles(img, random.Random(seed + 2), 14, SCORIA)
    return img


def polished_scoria():
    field = value_noise(16, 16, 611, scale=8, octaves=2)
    pal = Palette(*SCORIA.colors[1:5])
    img = from_field([[0.3 + v * 0.45 for v in row] for row in field], pal, 0.12, 612)
    for i in range(16):
        put(img, i, 0, SCORIA.c(5))
        put(img, 0, i, SCORIA.c(5))
        put(img, i, 15, SCORIA.c(0))
        put(img, 15, i, SCORIA.c(0))
    rng = random.Random(613)
    for _ in range(3):
        put(img, rng.randint(2, 13), rng.randint(2, 13), SCORIA.c(1))
    return img


def scoria_bricks():
    img = bricks_base(621, SCORIA_BRICK, hex_rgb("#1e0f0c"))
    vesicles(img, random.Random(622), 5, SCORIA_BRICK)
    return img


def cracked_scoria_bricks():
    img = scoria_bricks()
    crack_walk(img, random.Random(631), ((3, 1), (11, 6), (6, 10), (13, 13)), hex_rgb("#160a08"))
    return img


def chiseled_scoria_bricks():
    """A carved flame between two bands."""
    img = new()
    field = value_noise(16, 16, 641, scale=8, octaves=2)
    for y in range(16):
        for x in range(16):
            put(img, x, y, SCORIA_BRICK.c(2 + int(field[y][x] * 2)))
    frame = hex_rgb("#1e0f0c")
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(img, x, y, frame)
        for (x, y) in ((i, 1), (1, i)):
            if 1 <= i <= 14:
                put(img, x, y, SCORIA_BRICK.c(5))
        for (x, y) in ((i, 14), (14, i)):
            if 1 <= i <= 14:
                put(img, x, y, SCORIA_BRICK.c(0))
    flame = [
        "......#.",
        ".....##.",
        "....###.",
        "...####.",
        "..##.###",
        "..#..###",
        ".##...##",
        ".#....##",
        ".#.#..#.",
        ".##..##.",
        "..####..",
    ]
    for y, row in enumerate(flame):
        for x, ch in enumerate(row):
            if ch == "#":
                put(img, x + 4, y + 3, SCORIA_BRICK.c(0))
                put(img, x + 5, y + 3, SCORIA_BRICK.c(4)) if x + 1 < 8 and row[x + 1] == "." else None
    return img


def molten_frame(base, t, seed):
    """Scoria whose cracks glow; t in 0..1 drives the brightness so frames can pulse."""
    img = base.copy()
    rng = random.Random(seed)
    heat = value_noise(16, 16, seed, scale=4, octaves=2)
    for y in range(16):
        for x in range(16):
            v = heat[y][x]
            if v > 0.62:
                level = int((v - 0.62) / 0.38 * 4 + t * 2)
                put(img, x, y, LAVA.c(min(6, 2 + level)))
            elif v > 0.55:
                put(img, x, y, LAVA.c(1 + int(t * 1.5)))
    for _ in range(4):
        put(img, rng.randrange(16), rng.randrange(16), LAVA.c(5))
    return img


def molten_scoria():
    base = scoria(651)
    return [molten_frame(base, t, 652) for t in (0.0, 0.5, 1.0, 0.5)]


# ------------------------------------------------------------------------------------------ pumice

def pumice():
    field = value_noise(16, 16, 661, scale=4, octaves=2)
    img = from_field([[0.25 + v * 0.7 for v in row] for row in field], PUMICE, 0.25, 662)
    rng = random.Random(663)
    for _ in range(26):
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, PUMICE.c(0) if rng.random() < 0.6 else PUMICE.c(1))
        put(img, x, (y + 1) % 16, PUMICE.c(5))
    return img


def pumice_bricks():
    img = bricks_base(671, PUMICE, hex_rgb("#7a7368"))
    rng = random.Random(672)
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        if get(img, x, y)[:3] != hex_rgb("#7a7368"):
            put(img, x, y, PUMICE.c(1))
    return img


def sponge_holes(img, holes, dark, rim):
    for (x, y, r) in holes:
        for dy in range(-2, 3):
            for dx in range(-2, 3):
                d = math.hypot(dx, dy)
                if d <= r:
                    put(img, (x + dx) % 16, (y + dy) % 16, dark)
                elif d <= r + 0.9 and dy > 0:
                    put(img, (x + dx) % 16, (y + dy) % 16, rim)


SPONGE_HOLES = [(3, 3, 1.3), (10, 2, 0.9), (13, 8, 1.4), (6, 8, 1.0), (2, 12, 0.9), (9, 13, 1.3), (14, 14, 0.7),
                (7, 4, 0.6), (11, 10, 0.6)]


def pumice_sponge():
    field = value_noise(16, 16, 681, scale=4, octaves=2)
    pal = Palette("#b8a888", "#c8b996", "#d6c8a4", "#e2d5b2", "#ece1c0")
    img = from_field([[0.2 + v * 0.7 for v in row] for row in field], pal, 0.2, 682)
    sponge_holes(img, SPONGE_HOLES, hex_rgb("#6e6250"), hex_rgb("#f4ebd0"))
    return img


def molten_pumice_sponge():
    field = value_noise(16, 16, 691, scale=4, octaves=2)
    pal = Palette("#8a2a0a", "#b8400e", "#d85a16", "#ee7a24", "#fa9a38")
    img = from_field([[0.2 + v * 0.7 for v in row] for row in field], pal, 0.2, 692)
    sponge_holes(img, SPONGE_HOLES, LAVA.c(5), LAVA.c(6))
    return img


# ------------------------------------------------------------------------------------------ obsidian, glass

def obsidian_bricks():
    img = bricks_base(701, OBSIDIAN, hex_rgb("#050308"))
    rng = random.Random(702)
    for _ in range(6):
        x, y = rng.randrange(16), rng.randrange(16)
        if get(img, x, y)[:3] != hex_rgb("#050308"):
            put(img, x, y, hex_rgb("#6a4a9a"))  # a glint of purple
    return img


def chiseled_obsidian():
    img = new()
    field = value_noise(16, 16, 711, scale=8, octaves=2)
    for y in range(16):
        for x in range(16):
            put(img, x, y, OBSIDIAN.c(1 + int(field[y][x] * 2)))
    frame = hex_rgb("#050308")
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(img, x, y, frame)
        if 1 <= i <= 14:
            put(img, i, 1, OBSIDIAN.c(4))
            put(img, 1, i, OBSIDIAN.c(4))
    # a diamond-shaped rune with a crying-obsidian-like violet core
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5) + abs(y - 7.5)
            if 4.5 <= d <= 5.5:
                put(img, x, y, OBSIDIAN.c(5))
            elif 5.5 < d <= 6.5 and x + y > 15:
                put(img, x, y, OBSIDIAN.c(0))
            elif d <= 1.5:
                put(img, x, y, hex_rgb("#8a4ad0") if d <= 0.5 else hex_rgb("#5a2a9a"))
    return img


def volcanic_glass():
    img = new()
    rng = random.Random(721)
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                put(img, x, y, (22, 18, 20, 255))
            else:
                a = 120 + rng.randrange(20)
                put(img, x, y, (34, 26, 30, a))
    # conchoidal streaks
    for (x0, y0, n) in ((3, 3, 4), (9, 7, 3), (5, 11, 3)):
        for k in range(n):
            put(img, x0 + k, y0 - k // 2, (120, 96, 120, 170))
    put(img, 2, 2, (170, 150, 170, 200))
    return img


def volcanic_glass_pane_top():
    img = new()
    for y in range(16):
        for x in range(7, 9):
            put(img, x, y, (22, 18, 20, 255))
    return img


# ------------------------------------------------------------------------------------------ ash, sand

def ash_block():
    field = value_noise(16, 16, 731, scale=4, octaves=3)
    img = from_field([[0.2 + v * 0.65 for v in row] for row in field], ASH, 0.3, 732)
    rng = random.Random(733)
    for _ in range(9):
        put(img, rng.randrange(16), rng.randrange(16), hex_rgb("#a8a49c"))
    for _ in range(2):
        put(img, rng.randrange(16), rng.randrange(16), hex_rgb("#c0501a"))
    return img


def black_sand():
    field = value_noise(16, 16, 741, scale=4, octaves=2)
    img = from_field([[0.2 + v * 0.7 for v in row] for row in field], BLACK_SAND, 0.35, 742)
    rng = random.Random(743)
    for _ in range(6):
        put(img, rng.randrange(16), rng.randrange(16), hex_rgb("#6a6260"))
    for _ in range(3):
        put(img, rng.randrange(16), rng.randrange(16), hex_rgb("#9aa0a8"))  # glinting olivine and glass
    return img


# ------------------------------------------------------------------------------------------ ores & minerals

def deepslate():
    img = new()
    rng = random.Random(751)
    for y in range(16):
        for x in range(16):
            band = (y + (x // 5)) % 4
            c = DEEPSLATE.c(2 + (band == 0) - (band == 2) + rng.choice([0, 0, 1, -1]))
            put(img, x, y, c)
    return img


def opal_gems(img, spots, seed):
    rng = random.Random(seed)
    for (cx, cy) in spots:
        shape = [(0, 0), (1, 0), (0, 1), (1, 1)]
        if rng.random() < 0.6:
            shape.append((2, 1))
        if rng.random() < 0.6:
            shape.append((1, 2))
        for (dx, dy) in shape:
            tone = 5 if (dx, dy) == (0, 0) else 3 if dy == 0 else 2
            put(img, cx + dx, cy + dy, OPAL.c(tone))
        put(img, cx + 1, cy + 1, OPAL_FLECKS[rng.randrange(len(OPAL_FLECKS))])
        put(img, cx - 1, cy + 1, OPAL.c(0))
    return img


def fire_opal_ore():
    return opal_gems(scoria(761), [(3, 3), (10, 2), (6, 8), (12, 11), (2, 12)], 762)


def deepslate_fire_opal_ore():
    return opal_gems(deepslate(), [(2, 2), (11, 3), (7, 9), (13, 12), (3, 12)], 772)


def fire_opal_block():
    img = new()
    rng = random.Random(781)
    for y in range(16):
        for x in range(16):
            facet = ((x // 4) + (y // 4)) % 3
            c = OPAL.c(2 + facet + rng.choice([0, 0, 0, 1]))
            if (x % 4 == 0) or (y % 4 == 0):
                c = OPAL.c(4 + (facet == 1))
            put(img, x, y, c)
    for _ in range(10):
        put(img, rng.randrange(16), rng.randrange(16), OPAL_FLECKS[rng.randrange(4)])
    for i in range(16):
        put(img, i, 15, OPAL.c(1))
        put(img, 15, i, OPAL.c(1))
        put(img, i, 0, OPAL.c(6))
        put(img, 0, i, OPAL.c(6))
    return img


def sulfur_ore():
    img = scoria(791)
    rng = random.Random(792)
    for (cx, cy) in ((2, 2), (9, 3), (5, 8), (12, 9), (3, 13), (11, 14)):
        for (dx, dy) in ((0, 0), (1, 0), (0, 1), (1, 1), (rng.choice([-1, 2]), rng.choice([0, 1]))):
            put(img, cx + dx, cy + dy, SULFUR.c(3 if dy == 0 else 2))
        put(img, cx, cy, SULFUR.c(5))
    return img


def sulfur_block():
    field = value_noise(16, 16, 801, scale=4, octaves=2)
    img = from_field([[0.2 + v * 0.7 for v in row] for row in field], SULFUR, 0.3, 802)
    rng = random.Random(803)
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, SULFUR.c(5))
        put(img, x, (y + 1) % 16, SULFUR.c(1))
    return img


SULFUR_CLUSTER = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......Y........",
    "......YyY.......",
    "..Y...Yyd...Y...",
    ".YyY..Yyd..YyY..",
    ".Yyd..Yyd..Yyd..",
    ".Yyd.YYydY.Yyd..",
    "..Yd.Yyydd.Ydd..",
    "..YyYyyyyddYyd..",
    "..dyyyyyyyyydd..",
    "...dddddddddd...",
    "................",
]


def sulfur_cluster():
    return sprite(SULFUR_CLUSTER, {"Y": SULFUR.c(5), "y": SULFUR.c(3), "d": SULFUR.c(1)})


# ------------------------------------------------------------------------------------------ emberwood

def ember_log():
    """Charred bark split by glowing seams."""
    img = new()
    rng = random.Random(811)
    for x in range(16):
        base = rng.choice([1, 2, 2, 3])
        for y in range(16):
            put(img, x, y, CHAR.c(base + rng.choice([0, 0, 1, -1])))
    for x0 in (2, 7, 12):
        x = x0
        for y in range(16):
            if rng.random() < 0.75:
                put(img, x % 16, y, LAVA.c(3 if rng.random() < 0.7 else 4))
            if rng.random() < 0.3:
                x += rng.choice([-1, 1])
    return img


def ember_log_top():
    img = new()
    rng = random.Random(821)
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                c = CHAR.c(1 + rng.randrange(2))
            elif ring == 1:
                c = LAVA.c(2)
            else:
                c = EMBER_WOOD.c(1 + ring % 2 * 2 + rng.choice([0, 0, 1]))
                if ring >= 6:
                    c = LAVA.c(4)
            put(img, x, y, c)
    return img


def stripped_ember_log():
    img = new()
    rng = random.Random(831)
    for x in range(16):
        base = rng.choice([2, 3, 3, 4])
        for y in range(16):
            c = EMBER_WOOD.c(base + rng.choice([0, 0, 0, 1, -1]))
            if x % 5 == 1 and rng.random() < 0.6:
                c = EMBER_WOOD.c(0)
            put(img, x, y, c)
    return img


def stripped_ember_log_top():
    img = ember_log_top()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(img, x, y, EMBER_WOOD.c(1))
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
            if 1 <= i <= 14:
                put(img, x, y, EMBER_WOOD.c(4))
    return img


def ember_planks():
    img = new()
    rng = random.Random(841)
    pal = EMBER_WOOD
    seam = hex_rgb("#3a1206")
    joints = [4, 12, 7, 1]
    for board in range(4):
        tone = [2, 3, 2, 3][board]
        for dy in range(4):
            y = board * 4 + dy
            for x in range(16):
                if dy == 3 or x == joints[board]:
                    c = seam
                elif dy == 0:
                    c = pal.c(tone + 1)
                else:
                    c = pal.c(tone + rng.choice([-1, 0, 0, 0, 0, 1]))
                put(img, x, y, c)
        for _ in range(2):
            gx = rng.randrange(16)
            gy = board * 4 + rng.choice([1, 2])
            for k in range(rng.randint(2, 4)):
                if (gx + k) % 16 != joints[board]:
                    put(img, (gx + k) % 16, gy, pal.c(tone - 1))
    return img


def ember_door(top):
    img = new()
    rng = random.Random(851 if top else 852)
    frame = EMBER_WOOD.c(0)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or (top and y == 0) or (not top and y == 15):
                c = frame
            elif x in (1, 14):
                c = EMBER_WOOD.c(1)
            else:
                c = EMBER_WOOD.c(2 + ((x // 3) % 2) + rng.choice([0, 0, 0, 1]))
            put(img, x, y, c)
    if top:
        # a window of volcanic glass with a glowing lattice
        for y in range(3, 11):
            for x in range(3, 13):
                if x in (7, 8) or y == 6 or y == 7:
                    put(img, x, y, LAVA.c(3) if (x in (7, 8) and y in (6, 7)) else CHAR.c(2))
                else:
                    put(img, x, y, (34, 26, 30, 170))
        for x in range(3, 13):
            put(img, x, 2, frame)
            put(img, x, 11, frame)
        for y in range(2, 12):
            put(img, 2, y, frame)
            put(img, 13, y, frame)
    else:
        for y in (3, 12):
            for x in range(2, 14):
                put(img, x, y, CHAR.c(2))
        put(img, 12, 1, LAVA.c(4))  # glowing handle
        put(img, 12, 2, LAVA.c(3))
    return img


def ember_door_item():
    top, bottom = ember_door(True), ember_door(False)
    img = new()
    for y in range(8):
        for x in range(4, 12):
            put(img, x, y, get(top, 2 + (x - 4) * 12 // 8, y * 2))
            put(img, x, y + 8, get(bottom, 2 + (x - 4) * 12 // 8, y * 2))
    return outline(img, EMBER_WOOD.c(0))


def ember_trapdoor():
    img = new()
    rng = random.Random(861)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = EMBER_WOOD.c(0)
            elif (x in (4, 5, 10, 11)) and 3 <= y <= 12:
                c = CLEAR if y % 3 else CHAR.c(2)
            else:
                c = EMBER_WOOD.c(2 + rng.choice([0, 0, 1]))
            put(img, x, y, c)
    return img


def ember_leaves():
    img = new()
    rng = random.Random(871)
    field = value_noise(16, 16, 872, scale=4, octaves=2)
    for y in range(16):
        for x in range(16):
            v = field[y][x] + (rng.random() - 0.5) * 0.4
            if v < 0.25:
                continue
            put(img, x, y, EMBER_LEAF.c(max(0, min(6, int(v * 6)))))
    for _ in range(6):
        put(img, rng.randrange(16), rng.randrange(16), EMBER_LEAF.c(6))
    return img


EMBER_SAPLING = [
    "................",
    "................",
    "......y.........",
    ".....yo...o.....",
    "....yoOo.oOy....",
    "....oOOoyOOo....",
    ".....oOCOOo.....",
    "......oCo.......",
    ".......C..y.....",
    "..y....C.oOy....",
    ".oOo...CoOo.....",
    "..oOo..CC.......",
    "....ooCC........",
    ".......C........",
    ".......C........",
    "......cCc.......",
]


def ember_sapling():
    return sprite(EMBER_SAPLING, {"y": EMBER_LEAF.c(6), "O": EMBER_LEAF.c(4), "o": EMBER_LEAF.c(2),
                                  "C": CHAR.c(3), "c": CHAR.c(1)})


# ------------------------------------------------------------------------------------------ plants

ASH_GRASS = [
    "................",
    "................",
    "................",
    "................",
    "...........l....",
    "..l........m....",
    "..m....l..mm....",
    "...m...m..m..l..",
    "...m..mm..m..m..",
    "...mm.m...m.mm..",
    "....m.m..mm.m...",
    "....mdm..m.mm.l.",
    ".l..mdm.dm.md.m.",
    ".mm.dmd.dd.dd.m.",
    "..mddmddmdddmmd.",
    "..ddddddddddddd.",
]


def ash_grass():
    return sprite(ASH_GRASS, {"l": ASH_PLANT.c(4), "m": ASH_PLANT.c(2), "d": ASH_PLANT.c(1)})


FIREBLOSSOM = [
    "................",
    ".......y........",
    "......yOy.......",
    "...y..yOy..y....",
    "...OyyORRyyO....",
    "....ORRWRRO.....",
    ".....RWWWR......",
    "....ORRWRRO.....",
    "...OR.ROR.RO....",
    "...O...R...O....",
    ".......g........",
    "......Gg...Gg...",
    ".Gg....g..Gg....",
    "..Ggg..g.gG.....",
    "....Gggggg......",
    ".......g........",
]


def fireblossom():
    return sprite(FIREBLOSSOM, {"y": hex_rgb("#ffe066"), "O": hex_rgb("#ff8a1c"), "R": hex_rgb("#e0341a"),
                                "W": hex_rgb("#fff6c0"), "g": hex_rgb("#3a3a24"), "G": hex_rgb("#5c5a36")})


def lava_lily():
    """Seen from above: a charred round pad with a notch and a glowing bloom."""
    img = new()
    rng = random.Random(881)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            angle = math.atan2(y - 7.5, x - 7.5)
            if d > 7.2 or (-0.35 < angle < 0.35 and d > 1.5):
                continue
            vein = abs(math.sin(angle * 4)) < 0.2
            c = CHAR.c(4) if d > 6.2 else CHAR.c(2 + rng.choice([0, 1])) if not vein else hex_rgb("#6a2a14")
            put(img, x, y, c)
    for (x, y, c) in ((6, 6, 4), (7, 6, 5), (8, 6, 4), (6, 7, 5), (7, 7, 6), (8, 7, 5), (6, 8, 3), (7, 8, 4),
                      (8, 8, 3), (5, 7, 3), (9, 7, 3), (7, 5, 3), (7, 9, 2)):
        put(img, x, y, LAVA.c(c))
    return img


PEPPER_STAGES = [
    [
        "................", "................", "................", "................",
        "................", "................", "................", "................",
        "................", "................", "......G.........", "...G..g..G......",
        "....g.g.g.......", ".....ggg........", "......g.........", "......g.........",
    ],
    [
        "................", "................", "................", "................",
        "................", "................", "........G.......", "...G....g..G....",
        "....g..G.g.g....", "..G..g.g.gg.....", "...g..ggg...G...", "....g..g...g....",
        ".....g.g..g.....", "......ggg.......", ".......g........", ".......g........",
    ],
    [
        "................", "................", "................", "......G...G.....",
        "...G...g.gg.....", "....g.GgG...G...", "..G..ggg...Gg...", "...gg.Pg.gg.....",
        "....g.ggg.P.G...", ".G..ggg.ggg..g..", "..gg..ggg...g...", "....g..g..gg....",
        ".....g.g.g......", "......ggg.......", ".......g........", ".......g........",
    ],
    [
        "................", "................", "....G....G......", "..G..g.GgG..G...",
        "...g.gg.g..gg...", ".G..RgGgGgR..G..", "..gRRggg.gRRg...", "...gRR.g.gRr....",
        "..G.rg.gR.gr.G..", ".Gg.ggRgRr.gg...", "...gg.rRr.gg....", "....gg.gRg..R...",
        "..R..g.g.g.RR...", "..RR..ggg..r....", "...r...g........", ".......g........",
    ],
]


def fire_pepper_bush(stage):
    return sprite(PEPPER_STAGES[stage], {"G": hex_rgb("#6a8a2c"), "g": hex_rgb("#3e5a1c"), "P": hex_rgb("#e8e0a0"),
                                         "R": hex_rgb("#e8281a"), "r": hex_rgb("#9a0e0a")})


# ------------------------------------------------------------------------------------------ mechanics

def steam_vent_side():
    img = new()
    rng = random.Random(891)
    for y in range(16):
        for x in range(16):
            put(img, x, y, BASALT.c(2 + ((x + (y // 4)) % 3 == 0) + rng.choice([0, 0, -1])))
    for x in range(16):
        put(img, x, 0, SULFUR.c(2) if rng.random() < 0.5 else BASALT.c(4))
        if rng.random() < 0.4:
            put(img, x, 1, SULFUR.c(1))
    return img


def steam_vent_top(active):
    img = new()
    rng = random.Random(901)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.6:
                c = (hex_rgb("#e8ecef") if d < 1.4 else hex_rgb("#9aa4aa")) if active else hex_rgb("#0e0c0c")
            elif d < 3.6:
                c = hex_rgb("#1e1a18") if not active else hex_rgb("#5a5e60")
            elif d < 5.5:
                c = SULFUR.c(2 + rng.choice([0, 1, 0, -1])) if rng.random() < 0.55 else BASALT.c(3)
            else:
                c = BASALT.c(2 + rng.choice([0, 0, 1, -1]))
            put(img, x, y, c)
    return img


def forge_stone(seed):
    img = new()
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            put(img, x, y, STEEL.c(2 + rng.choice([0, 0, 1, -1])))
    return img


def volcanic_forge_side():
    img = forge_stone(911)
    for x in range(16):
        for y in (0, 15):
            put(img, x, y, STEEL.c(5))
        put(img, x, 1, STEEL.c(0))
        put(img, x, 14, STEEL.c(0))
    for y in range(16):
        put(img, 0, y, STEEL.c(4))
        put(img, 15, y, STEEL.c(0))
    for (x, y) in ((3, 4), (12, 4), (3, 11), (12, 11)):
        put(img, x, y, STEEL.c(5))  # rivets
        put(img, x + 1, y + 1, STEEL.c(0))
    return img


def volcanic_forge_front():
    img = volcanic_forge_side()
    for y in range(5, 12):
        for x in range(4, 12):
            glow = 6 - int(math.hypot(x - 7.5, (y - 11) * 1.4) * 0.9)
            put(img, x, y, LAVA.c(max(1, min(6, glow))))
    for x in range(4, 12):
        put(img, x, 4, STEEL.c(5))
    for y in range(5, 12):
        put(img, 3, y, STEEL.c(5))
        put(img, 12, y, STEEL.c(0))
    return img


def volcanic_forge_top():
    img = forge_stone(921)
    for i in range(16):
        for (x, y) in ((i, 0), (0, i)):
            put(img, x, y, STEEL.c(5))
        for (x, y) in ((i, 15), (15, i)):
            put(img, x, y, STEEL.c(0))
    return img


def volcanic_forge_inner():
    img = forge_stone(931)
    rng = random.Random(932)
    for _ in range(14):
        put(img, rng.randrange(16), rng.randrange(16), hex_rgb("#5a2a14"))  # scorch marks
    return img


def heart_frame(t, dormant):
    img = new()
    rng = random.Random(941)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            facet = (int(math.atan2(y - 7.5, x - 7.5) * 3) + int(d)) % 2
            if d < 3.2:
                level = 6 if d < 1.6 else 5
            elif d < 6.0:
                level = 3 + facet + (1 if rng.random() < 0.15 else 0)
            else:
                level = 2 + facet
            if dormant:
                level = max(1, level - 1 + int(t * 2))
            put(img, x, y, LAVA.c(min(6, level)))
    # an obsidian cage over the core
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i), (i, i), (i, 15 - i)):
            if not (6 <= i <= 9 and (x, y) in ((i, i), (i, 15 - i))):
                put(img, x, y, OBSIDIAN.c(2) if (x + y) % 3 else OBSIDIAN.c(4))
    return img


def cooled_lava_crust(age):
    """Age 0 is freshly frozen; each age the crust gets thinner and the lava shows through more."""
    img = new()
    rng = random.Random(951)
    heat = value_noise(16, 16, 952, scale=4, octaves=2)
    threshold = [0.78, 0.66, 0.55, 0.42][age]
    for y in range(16):
        for x in range(16):
            v = heat[y][x] + (rng.random() - 0.5) * 0.12
            if v > threshold:
                put(img, x, y, LAVA.c(min(6, 2 + int((v - threshold) * 12) + age // 2)))
            else:
                put(img, x, y, CHAR.c(1 + int(v * 4)))
    return img


# ------------------------------------------------------------------------------------------ items

ITEM_SPRITES = {
    "fire_opal": ([
        "................",
        "................",
        ".......oo.......",
        ".....ooHHoo.....",
        "....oHhhhHho....",
        "...oHhhGhhhdo...",
        "...ohhhhhBhdo...",
        "..ohhPhhhhhddo..",
        "..ohhhhhhGhddo..",
        "...ohhhBhhddo...",
        "...odhhhhhddo...",
        "....oddhhddo....",
        ".....odddoo.....",
        ".......oo.......",
        "................",
        "................",
    ], {"o": OPAL.c(0), "H": OPAL.c(6), "h": OPAL.c(3), "d": OPAL.c(2), "G": OPAL_FLECKS[0], "B": OPAL_FLECKS[1],
        "P": OPAL_FLECKS[2]}),
    "sulfur": ([
        "................",
        "................",
        "................",
        ".......o........",
        "......oYo..o....",
        ".....oYyo.oYo...",
        "..o..oYyooYyo...",
        ".oYo.oyyoYyyo...",
        ".oYyooyyYyyyo...",
        ".oyyyyyyyyydo...",
        "..oyyyyyyydo....",
        "..oddyyyyddo....",
        "...oodddddo.....",
        ".....ooooo......",
        "................",
        "................",
    ], {"o": SULFUR.c(0), "Y": SULFUR.c(5), "y": SULFUR.c(3), "d": SULFUR.c(1)}),
    "volcanic_ash": ([
        "................",
        "................",
        "................",
        "................",
        "................",
        ".......a........",
        ".....aAaa.......",
        "....aAAaaAa.....",
        "...aaAaAaeaa....",
        "..aAaaAaaAaaa...",
        "..aaAeaaaaaAa...",
        ".aaaaAaaAaaaaa..",
        ".aAaaaaaaaAaea..",
        "..dddaAadddddd..",
        "................",
        "................",
    ], {"a": ASH.c(3), "A": ASH.c(5), "d": ASH.c(1), "e": hex_rgb("#e0601a")}),
    "salamander_scale": ([
        "................",
        "................",
        ".....oooooo.....",
        "....oHHHHHho....",
        "...oHhhhhhhdo...",
        "...ohhOOOOhdo...",
        "...ohOQQQQOdo...",
        "...ohhOOOOhdo...",
        "...ohhhhhhhdo...",
        "....ohhOOhdo....",
        "....ohOQQOdo....",
        ".....ohOOdo.....",
        "......ohdo......",
        ".......oo.......",
        "................",
        "................",
    ], {"o": SCALE.c(0), "H": SCALE.c(4), "h": SCALE.c(2), "d": SCALE.c(1), "O": SAL_ORANGE.c(2),
        "Q": SAL_ORANGE.c(3)}),
    "ember_core": ([
        "................",
        "................",
        "......oooo......",
        "....oo6556oo....",
        "...o65YYYY56o...",
        "...o5YWWYYY6o...",
        "..o65WWYYYYY6o..",
        "..o6YYYYYYYO6o..",
        "..o6YYYYYYOO5o..",
        "..o65YYYYOOO6o..",
        "...o6OOYOOO5o...",
        "...o66OOOO66o...",
        "....oo6556oo....",
        "......oooo......",
        "................",
        "................",
    ], {"o": hex_rgb("#140c0a"), "6": CHAR.c(4), "5": LAVA.c(2), "W": LAVA.c(6), "Y": LAVA.c(5),
        "O": LAVA.c(3)}),
    "fire_pepper": ([
        "................",
        "................",
        "...........gg...",
        "..........gG....",
        ".........oGGo...",
        "........oRrRo...",
        ".......oRrrRo...",
        "......oRrrrdo...",
        ".....oRrrrdo....",
        "....oRrrrdo.....",
        "...oRrrddo......",
        "..oRrddo........",
        "..oyddo.........",
        "...oo...........",
        "................",
        "................",
    ], {"o": hex_rgb("#4a0604"), "R": hex_rgb("#ff5a3a"), "r": hex_rgb("#d8201a"), "d": hex_rgb("#8a0e0a"),
        "g": hex_rgb("#2e5a1a"), "G": hex_rgb("#5a9a2a"), "y": hex_rgb("#ffb02a")}),
    "spicy_stew": ([
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "..oooooooooooo..",
        ".oSsSRsSSsgSsSo.",
        ".oBSsSgsSRsSsBo.",
        ".obBBBBBBBBBBbo.",
        "..obBBBBBBBBbo..",
        "..obbBBBBBBbbo..",
        "...obbbbbbbbo...",
        "....oooooooo....",
        "................",
        "................",
    ], {"o": hex_rgb("#3a2410"), "S": hex_rgb("#e8601a"), "s": hex_rgb("#c03a10"), "R": hex_rgb("#ff2a1a"),
        "g": hex_rgb("#5a9a2a"), "B": hex_rgb("#8a5a2a"), "b": hex_rgb("#5a3a1a")}),
    "scorpion_venom": ([
        "................",
        "................",
        "......oooo......",
        "......oCco......",
        "......oooo......",
        ".......GG.......",
        "......oGGo......",
        ".....oGVVGo.....",
        "....oGVVvVGo....",
        "...oGVvVVVVdo...",
        "...oGVVVVVvdo...",
        "...oGVVVvVVdo...",
        "....oVVVVVdo....",
        ".....oddddo.....",
        "......oooo......",
        "................",
    ], {"o": hex_rgb("#1e2a2a"), "C": hex_rgb("#a0764a"), "c": hex_rgb("#6e4a26"), "G": hex_rgb("#d8eef0"),
        "V": hex_rgb("#8ac43a"), "v": hex_rgb("#d0f47a"), "d": hex_rgb("#4a7a1a")}),
    "titan_armor_trim_smithing_template": ([
        "................",
        ".....oooooo.....",
        "....oSSSSSSo....",
        "...oSsLLLLsSo...",
        "..oSSLOYYOLSSo..",
        "..oSSLYWWYLSSo..",
        "..oSSLOYYOLSSo..",
        "..oSSsLOOLsSSo..",
        "..oSSSsLLsSSSo..",
        "..oSsSSSSSSsSo..",
        "..oSSSsSSsSSSo..",
        "..oSSSSSSSSSSo..",
        "...oSSsSSsSSo...",
        "....oSSSSSSo....",
        ".....oooooo.....",
        "................",
    ], {"o": hex_rgb("#1a0c0a"), "S": SCORIA.c(4), "s": SCORIA.c(2), "L": OBSIDIAN.c(3), "O": LAVA.c(3),
        "Y": LAVA.c(5), "W": LAVA.c(6)}),
}


SWORD = [
    "................",
    ".............ooo",
    "............oHHo",
    "...........oHhdo",
    "..........oHhdo.",
    ".........oHhdo..",
    "........oHhdo...",
    "...oo..oHhdo....",
    "...ogooHhdo.....",
    "....ogHhdo......",
    ".....ogdo.......",
    "....oSogo.......",
    "...oSo.ogo......",
    "..oSo...oo......",
    ".oSo............",
    ".oo.............",
]


AXE = [
    "................",
    "......ooo.......",
    ".....oHHHo......",
    "....oHhhhHo.oo..",
    "...oHhhhhhooHdo.",
    "...oHhhhhhsSdo..",
    "...ohhhhhsSoo...",
    "....ohhdsSo.....",
    ".....oosSo......",
    "......sSo.......",
    ".....sS.........",
    "....sS..........",
    "...sS...........",
    "..sS............",
    ".sS.............",
    "................",
]


def opal_tool(name):
    # the unused classic-shaped bronze sprites double as templates for the fire opal tools
    template = {"sword": SWORD, "pickaxe": TOOL_SPRITES["bronze_pickaxe"], "axe": AXE,
                "shovel": TOOL_SPRITES["bronze_shovel"], "hoe": TOOL_SPRITES["bronze_hoe"]}[name]
    img = sprite(template, {"H": OPAL.c(5), "h": OPAL.c(3), "d": OPAL.c(2), "o": OPAL.c(0),
                            "s": STICK.c(3), "S": STICK.c(1), "g": STEEL.c(4)})
    # one play-of-colour fleck on the head
    body = [(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[:3] == OPAL.c(3)]
    if body:
        x, y = body[len(body) // 2]
        put(img, x, y, OPAL_FLECKS[(x + y) % 4])
    # the pickaxe and hoe templates have no outline of their own
    return outline(img, OPAL.c(0)) if name in ("pickaxe", "hoe") else img


def opal_armor_icon(name):
    img = sprite(ARMOR_SPRITES["bronze_" + name], {"H": OPAL.c(5), "h": OPAL.c(3), "d": OPAL.c(1), "o": OPAL.c(0)})
    count = 0
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[:3] == OPAL.c(3) and (x * 5 + y * 3) % 13 == 0 and count < 2:
                put(img, x, y, OPAL_FLECKS[count])
                count += 1
    return img


def salamander_boots_icon():
    img = sprite(ARMOR_SPRITES["bronze_boots"], {"H": SAL_ORANGE.c(3), "h": SCALE.c(3), "d": SCALE.c(1),
                                                 "o": SCALE.c(0)})
    for (x, y) in ((4, 9), (11, 9), (3, 11), (12, 11), (5, 11), (13, 11)):
        put(img, x, y, SAL_ORANGE.c(2))
    return img


def magma_hammer():
    """A block of obsidian veined with magma on a charred haft, held diagonally."""
    img = new()
    ux, uy = 0.7071, -0.7071  # handle direction (towards the head)
    vx, vy = 0.7071, 0.7071   # across the handle: the head's long axis
    cx, cy = 10.2, 5.2
    rng = random.Random(961)
    for y in range(16):
        for x in range(16):
            px, py = x - cx, y - cy
            along = px * vx + py * vy
            across = px * ux + py * uy
            if abs(along) <= 5.6 and abs(across) <= 2.8:
                edge = abs(along) > 4.7 or abs(across) > 2.0
                c = OBSIDIAN.c(4) if edge and along < 0 else OBSIDIAN.c(1) if edge else OBSIDIAN.c(2 + rng.choice([0, 1]))
                if not edge and abs(across - along * 0.35) < 0.6:
                    c = LAVA.c(4)
                put(img, x, y, c)
    for k in range(10):
        x, y = 1 + k, 14 - k
        if img.getpixel((x, y))[3] == 0:
            put(img, x, y, CHAR.c(4))
            put(img, x + 1, y, CHAR.c(2)) if img.getpixel((min(15, x + 1), y))[3] == 0 else None
    put(img, 1, 14, LAVA.c(3))
    return outline(img, hex_rgb("#08050a"))


def crab_claw(raw):
    rows = [
        "................",
        "................",
        "......oo........",
        ".....oPPo..oo...",
        "....oPpppooPPo..",
        "....oPpdo.oPpdo.",
        "....oppo..oppdo.",
        "....oPpo.oPppdo.",
        ".....oPpooPpddo.",
        ".....oPppppddo..",
        "......oppppdo...",
        ".....oWWpddo....",
        "....oWwWoo......",
        "....oWwo........",
        ".....oo.........",
        "................",
    ]
    if raw:
        colors = {"o": hex_rgb("#5a1e18"), "P": hex_rgb("#f0a898"), "p": hex_rgb("#d8766a"), "d": hex_rgb("#a84a40"),
                  "W": hex_rgb("#f6e8e0"), "w": hex_rgb("#e0c6bc")}
    else:
        colors = {"o": hex_rgb("#4a140a"), "P": hex_rgb("#ff9a52"), "p": hex_rgb("#e4562a"), "d": hex_rgb("#a82e16"),
                  "W": hex_rgb("#fff6ea"), "w": hex_rgb("#f0dcc4")}
    return sprite(rows, colors)


def lava_crab_bucket():
    img = new()
    iron = Palette("#3a3a3a", "#5a5a5a", "#7a7a7a", "#9c9c9c", "#c6c6c6", "#e8e8e8")
    for y in range(4, 15):
        half = 5.5 - (y - 4) * 0.22
        for x in range(16):
            dx = x - 7.5
            if abs(dx) <= half:
                if abs(dx) > half - 1:
                    c = iron.c(0)
                elif dx < -half + 2.2:
                    c = iron.c(4)
                elif dx > half - 2.2:
                    c = iron.c(1)
                else:
                    c = iron.c(3 if y % 4 else 2)
                put(img, x, y, c)
    for x in range(2, 14):
        put(img, x, 14, iron.c(0))
    # lava surface and a peeking crab
    for x in range(3, 13):
        put(img, x, 4, iron.c(0) if x in (3, 12) else LAVA.c(4 if x % 3 else 5))
        put(img, x, 5, iron.c(0) if x in (3, 12) else LAVA.c(3))
    for (x, y, c) in ((5, 3, hex_rgb("#c83a1a")), (6, 3, hex_rgb("#e0501c")), (7, 3, hex_rgb("#e0501c")),
                      (8, 3, hex_rgb("#e0501c")), (9, 3, hex_rgb("#c83a1a")), (6, 2, hex_rgb("#1a1414")),
                      (8, 2, hex_rgb("#1a1414")), (4, 2, hex_rgb("#e0501c")), (10, 2, hex_rgb("#e0501c"))):
        put(img, x, y, c)
    put(img, 2, 3, iron.c(1))
    put(img, 13, 3, iron.c(1))
    return img


# ------------------------------------------------------------------------------------------ armor layers

def gem_plate(i, j, w, h, rng, pal):
    if i == 0 or j == 0:
        return pal.c(5)
    if i == w - 1 or j == h - 1:
        return pal.c(1)
    if rng.random() < 0.04:
        return OPAL_FLECKS[rng.randrange(4)]
    return pal.c(2 + rng.choice([0, 1, 1]))


def fire_opal_layer_1():
    img = new(64, 32)
    rng = random.Random(971)

    def helmet(face, i, j, w, h):
        if face == "bottom":
            return None
        if face == "front" and 3 <= j <= 4 and 1 <= i <= w - 2:
            return STEEL.c(1) if j == 4 else STEEL.c(3)  # visor slit
        if face == "front" and j >= 5 and 2 <= i <= w - 3:
            return None
        return gem_plate(i, j, w, h, rng, OPAL)

    fill_faces(img, HEAD, helmet)

    def chest(face, i, j, w, h):
        if face in ("top", "bottom"):
            return OPAL.c(2)
        if face == "front" and 3 <= i <= 4 and 3 <= j <= 4:
            return OPAL.c(6) if (i, j) == (3, 3) else OPAL_FLECKS[(i + j) % 4]
        if j in (6, 7):
            return STEEL.c(2) if j == 7 else STEEL.c(4)
        return gem_plate(i, j, w, h, rng, OPAL)

    fill_faces(img, BODY, chest)

    def pauldron(face, i, j, w, h):
        if face == "bottom" or j > 5:
            return None
        return gem_plate(i, j, w, h, rng, OPAL)

    fill_faces(img, ARM, pauldron)

    def boot(face, i, j, w, h):
        if face == "top":
            return None
        if face != "bottom" and j < 8:
            return None
        return gem_plate(i, j - 8 if face != "bottom" else j, w, h, rng, OPAL)

    fill_faces(img, LEG, boot)
    return img


def fire_opal_layer_2():
    img = new(64, 32)
    rng = random.Random(972)

    def belt(face, i, j, w, h):
        if face in ("top", "bottom"):
            return None if face == "top" else OPAL.c(2)
        if j < 7:
            return None
        if j == 7:
            return STEEL.c(4)
        return gem_plate(i, j - 7, w, h, rng, OPAL)

    fill_faces(img, BODY, belt)

    def legs(face, i, j, w, h):
        if face == "bottom":
            return None
        if face != "top" and j >= 10:
            return None
        if face != "top" and j in (4, 8):
            return STEEL.c(3)
        return gem_plate(i, j, w, h, rng, OPAL)

    fill_faces(img, LEG, legs)
    return img


def salamander_layer_1():
    img = new(64, 32)

    def boot(face, i, j, w, h):
        if face == "top":
            return None
        if face != "bottom" and j < 7:
            return None
        if face != "bottom" and j == 7:
            return SAL_ORANGE.c(2)
        k = (i + 2 * j) % 4
        if face != "bottom" and (i + j) % 5 == 0:
            return SAL_ORANGE.c(1 + (j % 2))
        return SCALE.c(1 + (k == 0) + (k == 1) * 2)

    fill_faces(img, LEG, boot)
    return img


def titan_trim(leggings):
    """Grayscale trim: magma fissures running over the armor."""
    light, mid, dark = (224, 224, 224), (192, 192, 192), (160, 160, 160)
    img = new(64, 32)
    if not leggings:
        def horns(face, i, j, w, h):
            if face in ("top", "bottom"):
                return None
            if face == "front" and j == 1:
                return light if i in (0, 1, 6, 7) else mid if i in (2, 5) else None
            if face == "front" and j == 0 and i in (0, 7):
                return light
            if face in ("left", "right") and j in (1, 2) and i >= 4:
                return mid
            return None

        fill_faces(img, HEAD, horns)

        def fissure(face, i, j, w, h):
            if face in ("top", "bottom"):
                return None
            if face in ("front", "back"):
                path = [3, 3, 4, 4, 3, 2, 2, 3, 4, 5, 5, 4]
                if i == path[j]:
                    return light
                if (i == path[j] + 1 or i == path[j] - 1) and j % 3 == 0:
                    return dark
                return None
            if j in (0, 5):
                return mid
            return None

        fill_faces(img, BODY, fissure)

        def cuffs(face, i, j, w, h):
            if face in ("top", "bottom"):
                return None
            if j in (0, 1):
                return light if j == 0 else dark
            if j == 2 and i % 2 == 0:
                return mid
            return None

        fill_faces(img, ARM, cuffs)

        def greaves(face, i, j, w, h):
            if face in ("top", "bottom"):
                return None
            if j == 8:
                return light
            if j == 9 and (i + (face == "front")) % 2:
                return dark
            return None

        fill_faces(img, LEG, greaves)
    else:
        def belt(face, i, j, w, h):
            if face in ("top", "bottom"):
                return None
            if j == 8:
                return mid
            if j == 9 and face == "front" and i in (3, 4):
                return light
            return None

        fill_faces(img, BODY, belt)

        def veins(face, i, j, w, h):
            if face in ("top", "bottom"):
                return None
            path = [1, 1, 2, 2, 1, 1, 2, 2, 1]
            if j < len(path) and i == path[j]:
                return light if j % 2 == 0 else mid
            return None

        fill_faces(img, LEG, veins)
    return img


def fire_opal_trim_palette():
    img = new(8, 1)
    for x, c in enumerate(["#ffe2b0", "#ffb866", "#ff8c3a", "#f06a22", "#d24c16", "#a8360e", "#80260a", "#5a1806"]):
        put(img, x, 0, hex_rgb(c))
    return img


# ------------------------------------------------------------------------------------------ main

def main():
    blocks = {
        "scoria": scoria(), "polished_scoria": polished_scoria(), "scoria_bricks": scoria_bricks(),
        "cracked_scoria_bricks": cracked_scoria_bricks(), "chiseled_scoria_bricks": chiseled_scoria_bricks(),
        "pumice": pumice(), "pumice_bricks": pumice_bricks(), "pumice_sponge": pumice_sponge(),
        "molten_pumice_sponge": molten_pumice_sponge(), "obsidian_bricks": obsidian_bricks(),
        "chiseled_obsidian": chiseled_obsidian(), "volcanic_glass": volcanic_glass(),
        "volcanic_glass_pane_top": volcanic_glass_pane_top(), "ash_block": ash_block(), "black_sand": black_sand(),
        "fire_opal_ore": fire_opal_ore(), "deepslate_fire_opal_ore": deepslate_fire_opal_ore(),
        "fire_opal_block": fire_opal_block(), "sulfur_ore": sulfur_ore(), "sulfur_block": sulfur_block(),
        "sulfur_cluster": sulfur_cluster(), "ember_log": ember_log(), "ember_log_top": ember_log_top(),
        "stripped_ember_log": stripped_ember_log(), "stripped_ember_log_top": stripped_ember_log_top(),
        "ember_planks": ember_planks(), "ember_door_top": ember_door(True), "ember_door_bottom": ember_door(False),
        "ember_trapdoor": ember_trapdoor(), "ember_leaves": ember_leaves(), "ember_sapling": ember_sapling(),
        "ash_grass": ash_grass(), "fireblossom": fireblossom(), "lava_lily": lava_lily(),
        "steam_vent_side": steam_vent_side(), "steam_vent_top": steam_vent_top(False),
        "steam_vent_top_active": steam_vent_top(True), "volcanic_forge_side": volcanic_forge_side(),
        "volcanic_forge_front": volcanic_forge_front(), "volcanic_forge_top": volcanic_forge_top(),
        "volcanic_forge_inner": volcanic_forge_inner(), "heart_of_the_volcano": heart_frame(1.0, False),
    }
    for stage in range(4):
        blocks["fire_pepper_bush_stage%d" % stage] = fire_pepper_bush(stage)
    for age in range(4):
        blocks["cooled_lava_crust_%d" % age] = cooled_lava_crust(age)
    for name, img in blocks.items():
        save(img, "block/" + name)
    save_anim(molten_scoria(), "block/molten_scoria", 20)
    save_anim([heart_frame(t, True) for t in (0.0, 0.5, 1.0, 0.5)], "block/heart_of_the_volcano_dormant", 12)

    items = {name: sprite(rows, colors) for name, (rows, colors) in ITEM_SPRITES.items()}
    for tool in ("sword", "pickaxe", "axe", "shovel", "hoe"):
        items["fire_opal_" + tool] = opal_tool(tool)
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        items["fire_opal_" + piece] = opal_armor_icon(piece)
    items["salamander_boots"] = salamander_boots_icon()
    items["magma_hammer"] = magma_hammer()
    items["crab_meat"] = crab_claw(True)
    items["cooked_crab_meat"] = crab_claw(False)
    items["lava_crab_bucket"] = lava_crab_bucket()
    items["ember_door"] = ember_door_item()
    for name, img in items.items():
        save(img, "item/" + name)

    save(fire_opal_layer_1(), "models/armor/fire_opal_layer_1")
    save(fire_opal_layer_2(), "models/armor/fire_opal_layer_2")
    save(salamander_layer_1(), "models/armor/salamander_layer_1")
    save(new(64, 32), "models/armor/salamander_layer_2")
    save(titan_trim(False), "trims/models/armor/titan")
    save(titan_trim(True), "trims/models/armor/titan_leggings")
    save(fire_opal_trim_palette(), "trims/color_palettes/fire_opal")
    print("volcanic textures: %d blocks, %d items" % (len(blocks) + 2, len(items)))


if __name__ == "__main__":
    main()

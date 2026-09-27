"""Pixel art for Dunes & Relics: every block, item, armor, particle and humanoid mob texture.

All textures are drawn procedurally (with fixed seeds, so the output is stable) or from the
small ASCII sprites below. Run from the repository root:  python3 tools/textures.py
"""
import math
import os
import random

from PIL import Image

from pixels import Palette, shade, mix, hex_rgb

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEX = os.path.join(ROOT, "src/main/resources/assets/dunesrelics/textures")
CLEAR = (0, 0, 0, 0)


def new(w=16, h=16, fill=CLEAR):
    return Image.new("RGBA", (w, h), fill)


def put(img, x, y, color):
    if 0 <= x < img.width and 0 <= y < img.height and color is not None:
        img.putpixel((x, y), color if len(color) == 4 else (*color, 255))


def get(img, x, y):
    return img.getpixel((x % img.width, y % img.height))


def save(img, path):
    full = os.path.join(TEX, path + ".png")
    os.makedirs(os.path.dirname(full), exist_ok=True)
    img.save(full)


# ------------------------------------------------------------------------------------------ noise

def value_noise(w, h, seed, scale=4, octaves=2, wrap=True):
    """Tileable smooth noise in 0..1."""
    rng = random.Random(seed)
    out = [[0.0] * w for _ in range(h)]
    amp_total = 0.0
    amp = 1.0
    cell = scale
    for _ in range(octaves):
        gw, gh = max(1, w // cell), max(1, h // cell)
        grid = [[rng.random() for _ in range(gw)] for _ in range(gh)]
        for y in range(h):
            for x in range(w):
                fx, fy = x / cell, y / cell
                x0, y0 = int(fx), int(fy)
                tx, ty = fx - x0, fy - y0
                tx = tx * tx * (3 - 2 * tx)
                ty = ty * ty * (3 - 2 * ty)
                a = grid[y0 % gh][x0 % gw]
                b = grid[y0 % gh][(x0 + 1) % gw]
                c = grid[(y0 + 1) % gh][x0 % gw]
                d = grid[(y0 + 1) % gh][(x0 + 1) % gw]
                out[y][x] += amp * ((a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty)
        amp_total += amp
        amp *= 0.5
        cell = max(1, cell // 2)
    return [[v / amp_total for v in row] for row in out]


def from_field(field, pal, jitter=0.18, seed=0):
    rng = random.Random(seed)
    h, w = len(field), len(field[0])
    img = new(w, h)
    n = len(pal)
    for y in range(h):
        for x in range(w):
            v = field[y][x] + (rng.random() - 0.5) * jitter
            i = max(0, min(n - 1, int(v * n)))
            put(img, x, y, pal.c(i))
    return img


# ------------------------------------------------------------------------------------------ palettes

LIMESTONE = Palette("#a8946b", "#b9a67c", "#c7b58c", "#d3c39b", "#ddcfaa", "#e7dbb9")
LIME_DARK = hex_rgb("#8c7a55")
GOLD = Palette("#8f5f14", "#b8801e", "#d9a12c", "#f0c545", "#fbe386")
BRONZE = Palette("#5a3414", "#83501f", "#a86a2e", "#c98a45", "#e3ad68", "#f3cf96")
AMBER = Palette("#7a3c08", "#a9570f", "#d0781a", "#e8962c", "#f5b646", "#fde08a")
PALM_BARK = Palette("#4a3928", "#5a4632", "#6b553d", "#7d6648", "#8e7755", "#a08a66")
PALM_WOOD = Palette("#8f6838", "#a97f45", "#b88c50", "#c79a5c", "#d4a96a", "#dfb87a")
PALM_INNER = Palette("#a8844e", "#bb9660", "#c9a56d", "#d6b47c", "#e1c28c")
LEAF = Palette("#2a5220", "#356a28", "#437f30", "#54963a", "#68ab47", "#82c05a")
SAND = Palette("#c9b17a", "#d3bc86", "#dcc793", "#e4d29f", "#ecdcad")
QUICK = Palette("#a38a5a", "#b09766", "#bda673", "#c9b381", "#d4c08e")
DRY_GRASS = Palette("#6f6a2c", "#8b8538", "#a39d48", "#bdb55c", "#d6cd78")
AGAVE = Palette("#2f5e46", "#3c7658", "#4b8e6a", "#62a882", "#84c49c", "#b5e0c4")
TERRACOTTA = Palette("#7e3a1e", "#9a4b28", "#ae5a32", "#bf6b3f", "#cf7f52", "#dc9669")
LAPIS = Palette("#142a66", "#1d3a86", "#2a4ea6", "#3c66c0", "#5a86d6")
STICK = Palette("#28190c", "#4a311a", "#6b4a28", "#896033")
LINEN = Palette("#9c8a66", "#b5a37c", "#cbbb93", "#ddd0ab", "#ebe2c4")


# ------------------------------------------------------------------------------------------ limestone family

def limestone():
    field = value_noise(16, 16, 11, scale=8, octaves=3)
    # faint sedimentary bedding
    for y in range(16):
        band = 0.07 * math.sin(y * 1.3 + 0.8) + 0.05 * math.sin(y * 0.55)
        for x in range(16):
            field[y][x] = min(1.0, max(0.0, field[y][x] * 0.8 + 0.12 + band))
    img = from_field(field, LIMESTONE, 0.22, 12)
    rng = random.Random(13)
    for _ in range(7):
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, LIME_DARK)
        if rng.random() < 0.5:
            put(img, (x + 1) % 16, y, LIMESTONE.c(0))
    return img


def polished_limestone():
    field = value_noise(16, 16, 21, scale=8, octaves=2)
    pal = Palette(*LIMESTONE.colors[1:5])
    img = from_field([[0.3 + v * 0.45 for v in row] for row in field], pal, 0.1, 22)
    for i in range(16):
        put(img, i, 0, LIMESTONE.c(5))
        put(img, 0, i, LIMESTONE.c(5))
        put(img, i, 15, LIMESTONE.c(1))
        put(img, 15, i, LIMESTONE.c(1))
    put(img, 0, 15, LIMESTONE.c(3))
    put(img, 15, 0, LIMESTONE.c(3))
    return img


def bricks_base(seed, pal=LIMESTONE, mortar=hex_rgb("#958259")):
    rng = random.Random(seed)
    img = new()
    for row in range(4):
        offset = 0 if row % 2 == 0 else 4
        y0 = row * 4
        for bx in range(-1, 3):
            x0 = bx * 8 + offset
            tone = rng.choice([1, 2, 2, 3])
            for dy in range(4):
                for dx in range(8):
                    x, y = x0 + dx, y0 + dy
                    if not (0 <= x < 16):
                        continue
                    if dy == 3 or dx == 7:
                        c = mortar
                    elif dy == 0:
                        c = pal.c(min(len(pal) - 1, tone + 2))
                    elif dx == 0:
                        c = pal.c(tone + 1)
                    else:
                        c = pal.c(tone + rng.choice([-1, 0, 0, 0, 1]))
                    put(img, x, y, c)
    return img


def limestone_bricks():
    return bricks_base(31)


def cracked_limestone_bricks():
    img = bricks_base(31)
    rng = random.Random(41)
    crack = hex_rgb("#6f5f41")
    for start in ((3, 1), (11, 6), (6, 10), (13, 13)):
        x, y = start
        for _ in range(rng.randint(4, 7)):
            put(img, x, y, crack)
            x += rng.choice([-1, 0, 1])
            y += rng.choice([0, 1, 1])
            if not (0 <= x < 16 and 0 <= y < 16):
                break
    return img


ANKH = [
    "......###.......",
    ".....#...#......",
    ".....#...#......",
    "......#.#.......",
    "...#########....",
    ".......#........",
    ".......#........",
    ".......#........",
    ".......#........",
]


def chiseled_limestone_bricks():
    img = new()
    pal = LIMESTONE
    field = value_noise(16, 16, 51, scale=8, octaves=2)
    for y in range(16):
        for x in range(16):
            put(img, x, y, pal.c(2 + int(field[y][x] * 2)))
    frame = hex_rgb("#958259")
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(img, x, y, frame)
        if 1 <= i <= 14:
            put(img, i, 1, pal.c(5))
            put(img, 1, i, pal.c(5))
            put(img, i, 14, pal.c(1))
            put(img, 14, i, pal.c(1))
    carve = hex_rgb("#8a7650")
    lip = pal.c(5)
    for gy, row in enumerate(ANKH):
        for gx, ch in enumerate(row):
            if ch == "#":
                x, y = gx, gy + 3
                put(img, x, y, carve)
                if row[gx + 1:gx + 2] != "#" if gx + 1 < len(row) else True:
                    put(img, x + 1, y, lip)
    return img


def limestone_pillar():
    img = new()
    pal = LIMESTONE
    field = value_noise(16, 16, 61, scale=8, octaves=2)
    for y in range(16):
        for x in range(16):
            groove = x % 4
            if groove == 0:
                c = pal.c(1)
            elif groove == 1:
                c = pal.c(4)
            else:
                c = pal.c(2 + int(field[y][x] * 2))
            put(img, x, y, c)
    for x in range(16):
        put(img, x, 0, pal.c(5))
        put(img, x, 1, pal.c(3))
        put(img, x, 2, pal.c(1))
        put(img, x, 13, pal.c(4))
        put(img, x, 14, pal.c(3))
        put(img, x, 15, pal.c(1))
    return img


def limestone_pillar_top():
    img = new()
    pal = LIMESTONE
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            c = [pal.c(1), pal.c(4), pal.c(3), pal.c(2), pal.c(4), pal.c(3), pal.c(2), pal.c(1)][ring]
            put(img, x, y, c)
    return img


def gilded_limestone():
    img = polished_limestone()
    for i in range(2, 14):
        for (x, y) in ((i, 2), (i, 13), (2, i), (13, i)):
            put(img, x, y, GOLD.c(2))
    for i in range(3, 13):
        put(img, i, 3, GOLD.c(4) if i % 2 else GOLD.c(3))
        put(img, 3, i, GOLD.c(3))
    # a gilded sun disc with rays
    cx, cy = 7.5, 7.5
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - cx, y - cy)
            if d < 2.2:
                put(img, x, y, GOLD.c(4) if d < 1.2 else GOLD.c(3))
            elif d < 3.6 and (x == 7 or x == 8 or y == 7 or y == 8):
                put(img, x, y, GOLD.c(1))
    for (x, y) in ((5, 5), (10, 5), (5, 10), (10, 10)):
        put(img, x, y, GOLD.c(2))
    return img


# ------------------------------------------------------------------------------------------ palm wood

def palm_log():
    """Crosshatched bark made of the stubs of old fronds."""
    img = new()
    pal = PALM_BARK
    rng = random.Random(71)
    for y in range(16):
        for x in range(16):
            u = (x + y) % 8
            v = (x - y) % 8
            if u == 0 or v == 0:
                c = pal.c(1)
            elif u == 1 or v == 7:
                c = pal.c(4 + (rng.random() < 0.25))
            else:
                c = pal.c(2 + (rng.random() < 0.45))
            put(img, x, y, c)
    return img


def palm_log_top():
    img = new()
    rng = random.Random(81)
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                c = PALM_BARK.c(1 + rng.randrange(2))
            elif ring == 1:
                c = PALM_BARK.c(3)
            else:
                # palm wood has scattered fibre bundles instead of growth rings
                c = PALM_INNER.c(2 + rng.randrange(2))
                if rng.random() < 0.18:
                    c = PALM_INNER.c(0)
                if ring >= 6 and rng.random() < 0.3:
                    c = PALM_INNER.c(1)
            put(img, x, y, c)
    return img


def stripped_palm_log():
    img = new()
    rng = random.Random(91)
    pal = PALM_INNER
    for x in range(16):
        base = rng.choice([1, 2, 2, 3])
        for y in range(16):
            c = pal.c(base + rng.choice([0, 0, 0, 1, -1]))
            if x % 5 == 2 and rng.random() < 0.7:
                c = pal.c(0)
            put(img, x, y, c)
    return img


def stripped_palm_log_top():
    img = palm_log_top()
    for i in range(16):
        for (x, y) in ((i, 0), (i, 15), (0, i), (15, i)):
            put(img, x, y, PALM_INNER.c(1))
        for (x, y) in ((i, 1), (i, 14), (1, i), (14, i)):
            if 1 <= i <= 14:
                put(img, x, y, PALM_INNER.c(3))
    return img


def palm_planks():
    img = new()
    rng = random.Random(101)
    pal = PALM_WOOD
    seam = hex_rgb("#7a5630")
    joints = [5, 11, 2, 9]
    for board in range(4):
        tone = [2, 3, 2, 3][board]
        for dy in range(4):
            y = board * 4 + dy
            for x in range(16):
                if dy == 3:
                    c = seam
                elif x == joints[board]:
                    c = seam
                elif dy == 0:
                    c = pal.c(tone + 1)
                else:
                    c = pal.c(tone + rng.choice([-1, 0, 0, 0, 0, 1]))
                put(img, x, y, c)
        # grain streaks
        for _ in range(2):
            gx = rng.randrange(16)
            gy = board * 4 + rng.choice([1, 2])
            for k in range(rng.randint(2, 4)):
                if (gx + k) % 16 != joints[board]:
                    put(img, (gx + k) % 16, gy, pal.c(tone - 1))
    return img


def palm_door(top):
    img = new()
    pal = PALM_WOOD
    frame = pal.c(0)
    rng = random.Random(111 if top else 112)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or (top and y == 0) or (not top and y == 15):
                c = frame
            elif x in (5, 10):
                c = pal.c(1)
            else:
                c = pal.c(2 + rng.choice([0, 0, 1]))
            put(img, x, y, c)
    if top:
        # four small panes in a lattice frame
        for y in range(3, 11):
            for x in range(3, 13):
                bar = x in (3, 7, 8, 12) or y in (3, 6, 7, 10)
                put(img, x, y, pal.c(1) if bar else CLEAR)
        for x in range(1, 15):
            put(img, x, 12, pal.c(0))
    else:
        for x in range(1, 15):
            put(img, x, 1, pal.c(0))
            put(img, x, 8, pal.c(0))
        # diamond carving
        for y in range(3, 7):
            half = 3 - abs(y - 4.5) + 0.5
            for x in range(int(7.5 - half), int(8.5 + half)):
                put(img, x, y, pal.c(4))
        for y in range(10, 14):
            half = 3 - abs(y - 11.5) + 0.5
            for x in range(int(7.5 - half), int(8.5 + half)):
                put(img, x, y, pal.c(4))
        put(img, 12, 8, hex_rgb("#c9a13a"))
        put(img, 12, 9, hex_rgb("#8f6a1c"))
    return img


def palm_door_item():
    top, bottom = palm_door(True), palm_door(False)
    img = new()
    for y in range(16):
        src = top if y < 8 else bottom
        sy = (y % 8) * 2
        for x in range(4, 12):
            put(img, x, y, src.getpixel(((x - 4) * 2, sy)))
    return img


def palm_trapdoor():
    img = new()
    pal = PALM_WOOD
    rng = random.Random(121)
    for y in range(16):
        for x in range(16):
            edge = x in (0, 1, 14, 15) or y in (0, 1, 14, 15)
            slat = x in (7, 8) or y in (7, 8)
            if edge:
                c = pal.c(1) if (x in (0, 15) or y in (0, 15)) else pal.c(3)
            elif slat:
                c = pal.c(2)
            elif (x % 2 == 0) != (y % 2 == 0):
                c = pal.c(2 + rng.randrange(2))
            else:
                c = CLEAR
            put(img, x, y, c)
    return img


def palm_leaves():
    img = new()
    rng = random.Random(131)
    pal = LEAF
    # long leaflets hanging diagonally from several midribs
    for rib in range(5):
        x = rng.randrange(16)
        y = rng.randrange(16)
        dx = rng.choice([1, -1])
        for step in range(22):
            px, py = (x + step * dx) % 16, (y + step // 2) % 16
            put(img, px, py, pal.c(1))
            for k in range(1, 4):
                lx, ly = (px - k * dx) % 16, (py + k) % 16
                if get(img, lx, ly)[3] == 0 or rng.random() < 0.5:
                    put(img, lx, ly, pal.c(min(5, 2 + k + rng.choice([-1, 0, 0]))))
    # a few holes so the canopy looks airy
    for _ in range(30):
        put(img, rng.randrange(16), rng.randrange(16), CLEAR)
    return img


SAPLING = [
    "................",
    "...GG.....gg....",
    "..G..GG.gg..g...",
    ".G....GgG....g..",
    "......gGGg......",
    "....gg.BB.GG....",
    "...g...B....G...",
    "..g....B.....G..",
    ".......B........",
    "......B.........",
    "......B.........",
    "......b.........",
    ".....B..........",
    ".....b..........",
    ".....B..........",
    "....bBb.........",
]


def sprite(rows, colors):
    img = new(len(rows[0]), len(rows))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colors:
                put(img, x, y, colors[ch])
    return img


def palm_sapling():
    return sprite(SAPLING, {"G": LEAF.c(3), "g": LEAF.c(5), "B": PALM_BARK.c(3), "b": PALM_BARK.c(1)})


# ------------------------------------------------------------------------------------------ amber, bronze

def amber_ore():
    img = limestone()
    rng = random.Random(141)
    spots = [(3, 3), (10, 2), (6, 8), (12, 10), (2, 12)]
    for (cx, cy) in spots:
        shape = [(0, 0), (1, 0), (0, 1), (1, 1)]
        if rng.random() < 0.6:
            shape.append((2, 1))
        if rng.random() < 0.6:
            shape.append((1, 2))
        for (dx, dy) in shape:
            x, y = cx + dx, cy + dy
            tone = 3 if (dx, dy) == (0, 0) else 2 if dy == 0 else 1
            put(img, x, y, AMBER.c(tone))
        put(img, cx, cy, AMBER.c(5))
        put(img, cx + 1, cy + 1, AMBER.c(0))
    return img


def amber_block():
    img = new()
    field = value_noise(16, 16, 151, scale=8, octaves=2)
    for y in range(16):
        for x in range(16):
            flow = 0.5 + 0.5 * math.sin((x + y * 0.6) * 0.7 + field[y][x] * 3)
            tone = 1 + int(flow * 3)
            c = AMBER.c(tone)
            put(img, x, y, (*c, 215))
    for i in range(16):
        put(img, i, 0, (*AMBER.c(4), 230))
        put(img, 0, i, (*AMBER.c(4), 230))
        put(img, i, 15, (*AMBER.c(0), 235))
        put(img, 15, i, (*AMBER.c(0), 235))
    # bubbles and a tiny trapped insect
    for (x, y) in ((4, 4), (11, 9), (9, 3)):
        put(img, x, y, (*AMBER.c(5), 240))
    bug = [(7, 10), (8, 10), (8, 11), (9, 11), (6, 9), (10, 12), (7, 12), (9, 9)]
    for (x, y) in bug:
        put(img, x, y, (58, 30, 10, 245))
    return img


def amber_lamp():
    img = new()
    for y in range(16):
        for x in range(16):
            ring = min(x, y, 15 - x, 15 - y)
            if ring == 0:
                c = BRONZE.c(1)
            elif ring == 1:
                c = BRONZE.c(3)
            elif x in (7, 8) or y in (7, 8):
                c = BRONZE.c(2)
            else:
                d = math.hypot(x - 7.5, y - 7.5)
                c = AMBER.c(5 if d < 3 else 4 if d < 5 else 3)
            put(img, x, y, c)
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(img, x, y, BRONZE.c(5))
    return img


def bronze_block():
    img = new()
    rng = random.Random(161)
    for y in range(16):
        for x in range(16):
            qx, qy = x % 8, y % 8
            if qx == 7 or qy == 7:
                c = BRONZE.c(1)
            elif qx == 0 or qy == 0:
                c = BRONZE.c(4)
            else:
                c = BRONZE.c(2 + rng.choice([0, 1, 1, 1, 2]))
            put(img, x, y, c)
    for (x, y) in ((1, 1), (5, 1), (1, 5), (5, 5), (9, 1), (13, 1), (9, 5), (13, 5),
                   (1, 9), (5, 9), (1, 13), (5, 13), (9, 9), (13, 9), (9, 13), (13, 13)):
        put(img, x, y, BRONZE.c(5))
        put(img, x + 1, y + 1, BRONZE.c(0))
    return img


# ------------------------------------------------------------------------------------------ sand, plants

def quicksand():
    field = value_noise(16, 16, 171, scale=8, octaves=2)
    img = new()
    rng = random.Random(172)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, (y - 7.5) * 1.3)
            ripple = 0.5 + 0.5 * math.sin(r * 1.35 + field[y][x] * 2.2)
            v = 0.55 * field[y][x] + 0.45 * ripple + (rng.random() - 0.5) * 0.2
            put(img, x, y, QUICK.c(max(0, min(4, int(v * 5)))))
    return img


def dune_grass():
    img = new()
    rng = random.Random(181)
    blades = [(2, 8), (4, 12), (6, 9), (7, 14), (9, 11), (11, 13), (13, 9), (14, 6)]
    for (x0, height) in blades:
        lean = rng.choice([-1, 0, 1])
        for k in range(height):
            y = 15 - k
            x = x0 + (lean if k > height * 0.6 else 0)
            tone = 1 + int(3 * k / height)
            put(img, x, y, DRY_GRASS.c(tone))
        put(img, x0 + lean, 15 - height, DRY_GRASS.c(4))
    return img


def desert_rose():
    rows = [
        "................",
        "................",
        "......rRr.......",
        ".....rPpPr......",
        "....RpYyYpR.....",
        "....rPyYyPr.....",
        ".....rPpPr......",
        "......rRr.......",
        ".......S........",
        ".......S..LL....",
        "...LL..S.L......",
        ".....L.SL.......",
        "......LS........",
        ".......S........",
        ".......S........",
        ".......S........",
    ]
    return sprite(rows, {"r": hex_rgb("#9c2a3c"), "R": hex_rgb("#b83a4c"), "P": hex_rgb("#e0607a"),
                         "p": hex_rgb("#f28aa0"), "Y": hex_rgb("#f4d05a"), "y": hex_rgb("#e0a83a"),
                         "S": hex_rgb("#4e7a2a"), "L": hex_rgb("#6a9a3a")})


def aloe(stage):
    img = new()
    rng = random.Random(191 + stage)
    height = [5, 8, 11, 13][stage]
    spread = [2, 4, 6, 7][stage]
    leaves = [(-1.0, 1.0), (-0.55, 1.0), (-0.15, 1.0), (0.2, 1.0), (0.6, 1.0), (1.0, 1.0)]
    for lean, _ in leaves[: 2 + stage + (stage > 1)]:
        tip_x = 7.5 + lean * spread
        tip_y = 15 - height * (1.0 - abs(lean) * 0.35)
        steps = 24
        for s in range(steps + 1):
            t = s / steps
            x = 7.5 + (tip_x - 7.5) * (t ** 1.4)
            y = 15 - (15 - tip_y) * t
            width = max(0.5, (1 - t) * (1.2 + stage * 0.35))
            for w in range(-int(width), int(width) + 1):
                px, py = int(round(x + w)), int(round(y))
                tone = 3 + (w > 0) - (t > 0.85)
                put(img, px, py, AGAVE.c(tone))
            if stage >= 1 and rng.random() < 0.08:
                put(img, int(round(x)), int(round(y)), AGAVE.c(5))
        put(img, int(round(tip_x)), int(round(tip_y)), AGAVE.c(1))
    if stage == 3:
        # the flower spike of a mature aloe
        for y in range(0, 9):
            put(img, 9, y, AGAVE.c(1))
        for (x, y) in ((10, 1), (8, 2), (10, 3), (8, 4), (10, 5), (9, 0)):
            put(img, x, y, hex_rgb("#e8622a"))
        for (x, y) in ((10, 0), (8, 1), (10, 2)):
            put(img, x, y, hex_rgb("#f5a13a"))
    return img


def cattail(top):
    img = new()
    stalk = Palette("#3f6a26", "#4d7a2c", "#5f9236", "#78ab48")
    for (x, lean) in ((4, 0), (8, 1), (11, 0)):
        for y in range(16):
            put(img, x + (lean if y < 8 and not top else 0), y, stalk.c(1 + (y % 5 == 0)))
    blades = [(2, 6), (6, 10), (13, 7)] if not top else [(2, 12), (13, 14)]
    for (x, h) in blades:
        for k in range(h):
            put(img, x + (1 if k > h * 0.7 else 0), 15 - k, stalk.c(2 + (k > h / 2)))
    if top:
        head = Palette("#4a2e14", "#5a3a1c", "#6f4a26", "#86592e")
        for (x, y0) in ((4, 4), (8, 2), (11, 6)):
            put(img, x, y0 - 3, stalk.c(3))
            put(img, x, y0 - 2, stalk.c(2))
            for y in range(y0 - 1, y0 + 5):
                put(img, x, y, head.c(2 + (y % 2)))
                put(img, x + 1, y, head.c(1))
                put(img, x - 1, y, head.c(0) if y in (y0 - 1, y0 + 4) else None)
            put(img, x, y0 - 1, head.c(3))
    return img


# ------------------------------------------------------------------------------------------ urn, sarcophagus

def urn_side():
    img = new()
    rng = random.Random(201)
    for y in range(16):
        for x in range(16):
            c = TERRACOTTA.c(2 + rng.choice([0, 0, 1, -1]) + (x < 3) - (x > 12))
            put(img, x, y, c)
    band = hex_rgb("#2e1a10")
    for x in range(16):
        put(img, x, 3, band)
        put(img, x, 9, band)
        put(img, x, 10, band)
        if x % 4 in (0, 1):
            put(img, x, 6, GOLD.c(3))
        if x % 4 == 2:
            put(img, x, 5, GOLD.c(2))
            put(img, x, 7, GOLD.c(2))
        if x % 2 == 0:
            put(img, x, 12, band)
    return img


def urn_top():
    img = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 2.2:
                c = hex_rgb("#6b3a1c")  # clay stopper
            elif d < 3.2:
                c = hex_rgb("#2a1810")
            elif d < 4.6:
                c = TERRACOTTA.c(4)
            else:
                c = TERRACOTTA.c(2)
            put(img, x, y, c)
    put(img, 7, 7, hex_rgb("#8a5028"))
    return img


def sarcophagus_top():
    img = new()
    # body: gold with lapis bands, face mask at the head end (top rows), crossed arms
    for y in range(16):
        for x in range(16):
            c = GOLD.c(2)
            if y >= 9 and y % 2 == 1:
                c = LAPIS.c(2)
            if x in (0, 15) or y in (0, 15):
                c = GOLD.c(0)
            put(img, x, y, c)
    # nemes headdress
    for y in range(1, 7):
        for x in range(3, 13):
            stripe = LAPIS.c(1) if (x + (y > 3)) % 2 == 0 else GOLD.c(3)
            put(img, x, y, stripe)
    # face
    for y in range(2, 6):
        for x in range(5, 11):
            put(img, x, y, GOLD.c(4) if y < 5 else GOLD.c(3))
    for (x, y) in ((6, 3), (9, 3)):
        put(img, x, y, LAPIS.c(0))
    put(img, 7, 4, GOLD.c(1))
    put(img, 8, 4, GOLD.c(1))
    for x in range(7, 9):
        put(img, x, 6, LAPIS.c(1))  # beard
        put(img, x, 7, LAPIS.c(1))
    # crossed arms holding crook and flail
    for i in range(6):
        put(img, 4 + i, 8 + i // 2, GOLD.c(4))
        put(img, 11 - i, 8 + i // 2, GOLD.c(4))
    put(img, 4, 7, hex_rgb("#d0781a"))
    put(img, 11, 7, hex_rgb("#d0781a"))
    return img


def sarcophagus_side():
    img = new()
    for y in range(16):
        for x in range(16):
            if y < 3:
                c = GOLD.c(0) if y == 0 else GOLD.c(3)
            elif y in (3, 15):
                c = GOLD.c(1)
            elif y == 9:
                c = LAPIS.c(2)
            else:
                c = LIMESTONE.c(3 + ((x + y) % 5 == 0))
            put(img, x, y, c)
    # hieroglyph band
    glyphs = [(2, 5), (2, 6), (3, 6), (6, 5), (6, 7), (7, 6), (10, 5), (11, 5), (11, 6), (11, 7), (14, 6)]
    for (x, y) in glyphs:
        put(img, x, y, LAPIS.c(1))
    for x in range(1, 15, 3):
        put(img, x, 12, GOLD.c(2))
    return img


def sarcophagus_bottom():
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, LIMESTONE.c(1 + ((x * 7 + y * 3) % 3)))
    return img


# ------------------------------------------------------------------------------------------ items

ITEM_SPRITES = {
    "amber": ([
        "................",
        "................",
        "........oo......",
        "......ooHHo.....",
        ".....oHhhHHo....",
        "....oOhhHhHOo...",
        "....oOOhhhOOo...",
        "...oOOOOhOOOo...",
        "...oOOOOOOOdo...",
        "...oOOOOOOddo...",
        "...oOOOOOdddo...",
        "....oOOOdddo....",
        ".....oddddo.....",
        "......oooo......",
        "................",
        "................",
    ], {"o": AMBER.c(0), "O": AMBER.c(3), "d": AMBER.c(2), "h": AMBER.c(4), "H": AMBER.c(5)}),
    "bronze_ingot": ([
        "................",
        "................",
        "................",
        "................",
        "................",
        "......ooooooo...",
        ".....oHHHHHHho..",
        "....oHhhhhhhdo..",
        "...oHhhhhhhhdo..",
        "..ohhhhhhhhddo..",
        "..oddddddddddo..",
        "..oddddddddddo..",
        "..oooooooooooo..",
        "................",
        "................",
        "................",
    ], {"o": BRONZE.c(0), "H": BRONZE.c(5), "h": BRONZE.c(3), "d": BRONZE.c(2)}),
    "bronze_nugget": ([
        "................",
        "................",
        "................",
        "................",
        "................",
        "......oo........",
        ".....oHho.......",
        ".....ohdo.oo....",
        "......oo.oHho...",
        ".........ohdo...",
        "....oo....oo....",
        "...oHho.........",
        "...ohdo.........",
        "....oo..........",
        "................",
        "................",
    ], {"o": BRONZE.c(0), "H": BRONZE.c(5), "h": BRONZE.c(3), "d": BRONZE.c(2)}),
    "linen": ([
        "................",
        "................",
        "................",
        "................",
        "...oooooooooo...",
        "..oHHHHHHHHHHo..",
        "..ohhhhhhhhhho..",
        "..odddddddddso..",
        "..oHHHHHHHHHHo..",
        "..ohhhhhhhhhho..",
        "..odddddddddso..",
        "..oHHHHHHHHHso..",
        "...oooooooooso..",
        "............o...",
        "................",
        "................",
    ], {"o": LINEN.c(0), "H": LINEN.c(4), "h": LINEN.c(3), "d": LINEN.c(2), "s": LINEN.c(1)}),
    "scorpion_stinger": ([
        "................",
        "................",
        "................",
        "............o...",
        "...........oSo..",
        "...........oSo..",
        "..........oSso..",
        ".....oooooSso...",
        "....oHHhhdSo....",
        "...oHhhhhhdo....",
        "...ohhhhhhdo....",
        "...ohhhhhddo....",
        "....odddddo.....",
        ".....ooooo......",
        "................",
        "................",
    ], {"o": hex_rgb("#2c1a0c"), "H": hex_rgb("#dcaa5c"), "h": hex_rgb("#c28a42"), "d": hex_rgb("#8a5c2c"),
        "S": hex_rgb("#4a2e16"), "s": hex_rgb("#7a4a22")}),
    "vulture_feather": ([
        "................",
        "............oo..",
        "...........oddo.",
        "..........oddso.",
        ".........odddso.",
        "........oddwdo..",
        ".......oddwddo..",
        "......oddwddo...",
        ".....oddwddo....",
        "....oddwdso.....",
        "....odwdso......",
        "...oswdso.......",
        "...oswso........",
        "...ow.o.........",
        "..w.............",
        ".w..............",
    ], {"o": hex_rgb("#1c1614"), "d": hex_rgb("#3d322b"), "s": hex_rgb("#5a4a3e"), "w": hex_rgb("#d8d0c4")}),
    "dates": ([
        "................",
        ".........s......",
        "........s.......",
        ".......sS.......",
        "......s..S......",
        ".....ooo..ooo...",
        "....oHho.oHho...",
        "....ohdo.ohdo...",
        "....odd..odd....",
        "....oo.ooo.oo...",
        "......oHho......",
        "......ohdo......",
        "......oddo......",
        ".......oo.......",
        "................",
        "................",
    ], {"o": hex_rgb("#3a1c0c"), "H": hex_rgb("#c97a3a"), "h": hex_rgb("#9a5226"), "d": hex_rgb("#6e3618"),
        "s": hex_rgb("#c9a13a"), "S": hex_rgb("#a07a2a")}),
    "honeyed_dates": ([
        "................",
        ".........s......",
        "........s.......",
        ".......sS.......",
        "......s..S......",
        ".....ooo..ooo...",
        "....oYho.oYho...",
        "....ohyo.ohyo...",
        "....oyd..oyd....",
        "....oo.ooo.oo...",
        "......oYho......",
        "......ohyo......",
        "......oyyo......",
        ".......yy.......",
        ".......y........",
        "................",
    ], {"o": hex_rgb("#3a1c0c"), "Y": hex_rgb("#ffe08a"), "h": hex_rgb("#b86a2e"), "d": hex_rgb("#6e3618"),
        "y": hex_rgb("#f2a91e"), "s": hex_rgb("#c9a13a"), "S": hex_rgb("#a07a2a")}),
    "flatbread": ([
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "...ooHHHhhhoo...",
        "..oHHhhhhhhhdo..",
        ".oHhhchhhhchhdo.",
        ".ohhhhhhchhhhdo.",
        ".ohchhhhhhhhhdo.",
        ".ohhhhhchhhchdo.",
        ".odhhhhhhhhhddo.",
        "..oddhhhchhddo..",
        "...oodddddoo....",
        ".....oooooo.....",
        "................",
        "................",
    ], {"o": hex_rgb("#7a4a1e"), "H": hex_rgb("#f2d59a"), "h": hex_rgb("#e0b870"), "d": hex_rgb("#b88a48"),
        "c": hex_rgb("#6e3e16")}),
    "aloe_leaf": ([
        "................",
        "............oo..",
        "...........oHo..",
        "..........oHho..",
        ".........oHhso..",
        "........oHhsho..",
        ".......oHhhhdo..",
        "......oHshhdo...",
        ".....oHhhhsdo...",
        "....oHhhhhddo...",
        "...oHhshhddo....",
        "...ohhhhddo.....",
        "..ohhhhddo......",
        "..odddddo.......",
        "..oooooo........",
        "................",
    ], {"o": AGAVE.c(0), "H": AGAVE.c(5), "h": AGAVE.c(3), "d": AGAVE.c(2), "s": hex_rgb("#d8f0dc")}),
    "bandage": ([
        "................",
        "................",
        "......oooo......",
        "....ooHHHHoo....",
        "...oHHhhhhHHo...",
        "..oHhhoooohhHo..",
        "..oHhoHHHohhdo..",
        "..oHhoHooohhdo..",
        "..oHhoHhGhhhdo..",
        "..oHhhooooGhdo..",
        "...oHhhhhhhdo...",
        "....oddddddHHo..",
        "......oooooHhdo.",
        "...........oHdo.",
        "............oo..",
        "................",
    ], {"o": LINEN.c(0), "H": LINEN.c(4), "h": LINEN.c(3), "d": LINEN.c(1), "G": AGAVE.c(3)}),
    "scepter_of_sands": ([
        "..........ooo...",
        ".........oGGGo..",
        "........oG.AoG..",
        "........oGAAGo..",
        "........ooGGo...",
        ".......oGGGGGo..",
        "......oLo.oo....",
        ".....oGo........",
        "....oGo.........",
        "...oLo..........",
        "..oGo...........",
        ".oGo............",
        ".oo.............",
        "................",
        "................",
        "................",
    ], {"o": hex_rgb("#5a3a0c"), "G": GOLD.c(3), "L": LAPIS.c(2), "A": AMBER.c(4)}),
    "pharaoh_armor_trim_smithing_template": ([
        "................",
        ".....oooooo.....",
        "....oSSSSSSo....",
        "...oSLGGGGLSo...",
        "..oSSLGffGLSSo..",
        "..oSSLGeeGLSSo..",
        "..oSSLGffGLSSo..",
        "..oSSSLGGLSSSo..",
        "..oSSSSGGSSSSo..",
        "..oSSSSSSSSSSo..",
        "..oSsSSsSSsSSo..",
        "..oSSSSSSSSSSo..",
        "...oSSSSSSSSo...",
        "....oSSSSSSo....",
        ".....oooooo.....",
        "................",
    ], {"o": hex_rgb("#5e4a2c"), "S": hex_rgb("#c9b07c"), "s": hex_rgb("#a38a5a"), "L": LAPIS.c(2),
        "G": GOLD.c(3), "f": GOLD.c(4), "e": LAPIS.c(0)}),
    "amber_goggles": ([
        "................",
        "................",
        "................",
        "................",
        "................",
        "..oooo....oooo..",
        ".oBBBBo..oBBBBo.",
        "oBAHAABooBAHAABo",
        "sBAAAABssBAAAABs",
        "sBAAAdBooBAAAdBs",
        ".oBBBBo..oBBBBo.",
        "..oooo....oooo..",
        "................",
        "................",
        "................",
        "................",
    ], {"o": BRONZE.c(0), "B": BRONZE.c(3), "A": AMBER.c(3), "H": AMBER.c(5), "d": AMBER.c(1),
        "s": hex_rgb("#5a3a1c")}),
}

TOOL_SPRITES = {
    "bronze_pickaxe": [
        "................",
        "....HHHHHH......",
        "...HhhhhhhHH....",
        "..Hhdooo.hhHH...",
        "..hdo..sS.dhH...",
        "..do..sS...dh...",
        ".....sS.....d...",
        "....sS..........",
        "...sS...........",
        "..sS............",
        ".sS.............",
        "sS..............",
        "S...............",
        "................",
        "................",
        "................",
    ],
    "bronze_axe": [
        "................",
        "........oo......",
        ".......oHHo.....",
        "......oHhhHo....",
        ".....oHhhhhdo...",
        ".....ohhhhsSo...",
        "......ohhsSdo...",
        "......odsShdo...",
        ".......sSoddo...",
        "......sS..oo....",
        ".....sS.........",
        "....sS..........",
        "...sS...........",
        "..sS............",
        ".sS.............",
        "................",
    ],
    "bronze_shovel": [
        "................",
        "...........oo...",
        "..........oHHo..",
        ".........oHhhHo.",
        "........oHhhhhdo",
        "........ohhhhhdo",
        ".........ohhhdo.",
        "........sSoddo..",
        ".......sS..oo...",
        "......sS........",
        ".....sS.........",
        "....sS..........",
        "...sS...........",
        "..sS............",
        ".sS.............",
        "................",
    ],
    "bronze_hoe": [
        "................",
        "......HHHHH.....",
        ".....Hhhhhhh....",
        "....ddoosShdo...",
        ".......sS.do....",
        "......sS........",
        ".....sS.........",
        "....sS..........",
        "...sS...........",
        "..sS............",
        ".sS.............",
        "sS..............",
        "S...............",
        "................",
        "................",
        "................",
    ],
    "bronze_khopesh": [
        "................",
        ".....HHHH.......",
        "...HHhhhhH......",
        "..Hhhd..Hh......",
        "..hd.....hH.....",
        "..d......hhH....",
        ".........dhh....",
        "..........hhd...",
        "..........hdo...",
        ".........ohdo...",
        "........gggo....",
        ".......sS.......",
        "......sS........",
        ".....sS.........",
        "....ss..........",
        "................",
    ],
}


def epsilon_axe():
    """A bronze-age crescent ("epsilon") axe: a half-disc blade lashed to the side of the haft."""
    img = new()
    cx, cy = 8.0, 7.0
    for y in range(16):
        for x in range(16):
            if x + y > 13:
                continue
            d = math.hypot(x - cx, y - cy)
            if d < 5.6:
                if d > 4.6:
                    c = BRONZE.c(5) if x + y < 11 else BRONZE.c(0)
                elif d > 3.6:
                    c = BRONZE.c(3)
                elif x + y > 11:
                    c = BRONZE.c(1)
                else:
                    c = BRONZE.c(2)
                put(img, x, y, c)
    for x in range(1, 13):
        put(img, x, 15 - x, STICK.c(1))
        put(img, x, 14 - x, STICK.c(3))
    for (x, y) in ((9, 5), (10, 5), (8, 6)):
        put(img, x, y, GOLD.c(2))  # lashing
    return img


def spade():
    img = new()
    ux, uy = 0.7071, -0.7071
    for y in range(16):
        for x in range(16):
            px, py = x - 11.0, y - 4.5
            along = px * ux + py * uy
            across = -px * uy + py * ux
            if (along / 4.3) ** 2 + (across / 2.9) ** 2 <= 1.0:
                if across < -1.6:
                    c = BRONZE.c(5)
                elif across > 1.6:
                    c = BRONZE.c(0)
                elif along > 2.2:
                    c = BRONZE.c(4)
                else:
                    c = BRONZE.c(3 if across < 0 else 2)
                put(img, x, y, c)
    for x in range(1, 9):
        put(img, x, 15 - x, STICK.c(1))
        put(img, x, 14 - x, STICK.c(3))
    put(img, 8, 7, BRONZE.c(1))
    put(img, 9, 6, BRONZE.c(1))
    return img


def stinger():
    """A curved, tapering scorpion telson: a venom bulb ending in a dark hooked barb."""
    img = new()
    cx, cy, radius = 11.5, 11.5, 7.0
    steps = 60
    for k in range(steps + 1):
        t = k / steps
        angle = math.pi * 0.85 + t * math.pi * 0.75
        x = cx + math.cos(angle) * radius
        y = cy + math.sin(angle) * radius
        width = 2.1 * (1 - t) ** 0.9 + 0.3
        for oy in range(-3, 4):
            for ox in range(-3, 4):
                if ox * ox + oy * oy <= width * width:
                    px, py = int(round(x + ox)), int(round(y + oy))
                    if t < 0.55:
                        c = hex_rgb("#dcaa5c") if oy < 0 and ox < 1 else hex_rgb("#c28a42") if oy < 1 else hex_rgb("#8a5c2c")
                    else:
                        c = hex_rgb("#4a2e16") if oy < 0 else hex_rgb("#2c1a0c")
                    put(img, px, py, c)
    # outline
    out = img.copy()
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] == 0 and any(
                    0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3] > 0
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                put(out, x, y, hex_rgb("#24150a"))
    return out


def tool(name):
    if name == "bronze_axe":
        return epsilon_axe()
    if name == "bronze_shovel":
        return spade()
    rows = TOOL_SPRITES[name]
    return sprite(rows, {"H": BRONZE.c(5), "h": BRONZE.c(3), "d": BRONZE.c(1), "o": BRONZE.c(0),
                         "s": STICK.c(3), "S": STICK.c(1), "g": GOLD.c(2)})


ARMOR_SPRITES = {
    "bronze_helmet": [
        "................",
        "................",
        "................",
        "....oooooooo....",
        "...oHHHHHHHho...",
        "..oHhhhhhhhhdo..",
        "..ohhhhhhhhhdo..",
        "..ohdooooooddo..",
        "..odo......odo..",
        "..oo........oo..",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ],
    "bronze_chestplate": [
        "................",
        "..ooo......ooo..",
        ".oHho......oHho.",
        ".ohhdooooooohdo.",
        ".ohhhHHHHHHhhdo.",
        ".odhhhhhhhhhhdo.",
        "..ooohhhhhhdooo.",
        "....ohhhhhhdo...",
        "....ohhhhhhdo...",
        "....ohhHHhhdo...",
        "....ohhhhhhdo...",
        "....ohhhhhhdo...",
        "....odddddddo...",
        "....ooooooooo...",
        "................",
        "................",
    ],
    "bronze_leggings": [
        "................",
        "................",
        "....ooooooooo...",
        "....oHHHHHHHo...",
        "....ohhhhhhdo...",
        "....ohhdohhdo...",
        "....ohhdohhdo...",
        "....ohhdohhdo...",
        "....ohhdohhdo...",
        "....ohdooohdo...",
        "....ohdo.ohdo...",
        "....ohdo.ohdo...",
        "....oooo.oooo...",
        "................",
        "................",
        "................",
    ],
    "bronze_boots": [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...oooo..oooo...",
        "...oHho..oHho...",
        "...ohdo..ohdo...",
        "...ohdo..ohdo...",
        "..ohhdo..ohhdo..",
        ".ohhhdo..ohhhdo.",
        ".ooooooo.ooooooo",
        "................",
        "................",
        "................",
    ],
}


def armor_icon(name):
    return sprite(ARMOR_SPRITES[name], {"H": BRONZE.c(5), "h": BRONZE.c(3), "d": BRONZE.c(1), "o": BRONZE.c(0)})


# ------------------------------------------------------------------------------------------ armor layers

def humanoid_faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def fill_faces(img, box, painter, faces=None):
    for face, (fx, fy, fw, fh) in humanoid_faces(*box).items():
        if faces and face not in faces:
            continue
        for j in range(fh):
            for i in range(fw):
                c = painter(face, i, j, fw, fh)
                if c is not None:
                    put(img, fx + i, fy + j, c)


HEAD = (0, 0, 8, 8, 8)
HAT = (32, 0, 8, 8, 8)
BODY = (16, 16, 8, 12, 4)
ARM = (40, 16, 4, 12, 4)
LEG = (0, 16, 4, 12, 4)


def plate(face, i, j, w, h, rng, pal=BRONZE):
    if i == 0 or j == 0:
        return pal.c(4)
    if i == w - 1 or j == h - 1:
        return pal.c(1)
    return pal.c(2 + rng.choice([0, 1, 1]))


def bronze_layer_1():
    img = new(64, 32)
    rng = random.Random(301)

    def helmet(face, i, j, w, h):
        if face == "front" and 2 <= j and 1 <= i <= w - 2:
            return BRONZE.c(1) if j == 2 else None  # open face with a brow ridge
        if face == "bottom":
            return None
        if face in ("left", "right") and j >= 6 and 3 <= i:
            return None
        return plate(face, i, j, w, h, rng)

    fill_faces(img, HEAD, helmet)

    def chest(face, i, j, w, h):
        if face in ("top", "bottom"):
            return BRONZE.c(2)
        if j == 5 or j == 6:
            return BRONZE.c(1) if j == 6 else BRONZE.c(4)
        if face == "front" and j < 5 and i in (3, 4):
            return BRONZE.c(4)
        return plate(face, i, j, w, h, rng)

    fill_faces(img, BODY, chest)

    def pauldron(face, i, j, w, h):
        if face == "bottom" or j > 4:
            return None
        return plate(face, i, j, w, h, rng)

    fill_faces(img, ARM, pauldron)

    def boot(face, i, j, w, h):
        if face == "top":
            return None
        if face != "bottom" and j < 8:
            return None
        return plate(face, i, j - 8 if face != "bottom" else j, w, h, rng)

    fill_faces(img, LEG, boot)
    return img


def bronze_layer_2():
    img = new(64, 32)
    rng = random.Random(302)

    def belt(face, i, j, w, h):
        if face in ("top", "bottom"):
            return None if face == "top" else BRONZE.c(2)
        if j < 7:
            return None
        if j == 7:
            return BRONZE.c(4)
        return plate(face, i, j - 7, w, h, rng)

    fill_faces(img, BODY, belt)

    def legs(face, i, j, w, h):
        if face == "bottom":
            return None
        if face != "top" and j >= 9:
            return None
        if face != "top" and j in (3, 7):
            return BRONZE.c(1)
        return plate(face, i, j, w, h, rng)

    fill_faces(img, LEG, legs)
    return img


def amber_goggles_layer():
    img = new(64, 32)

    def strap(face, i, j, w, h):
        if face in ("top", "bottom"):
            return None
        if face == "front":
            if j in (3, 4, 5) and i in (1, 2, 5, 6):
                if j == 3 or j == 5:
                    return BRONZE.c(1)
                return AMBER.c(4) if i in (1, 5) else AMBER.c(3)
            if j in (3, 4, 5) and i in (0, 3, 4, 7):
                return BRONZE.c(3) if j == 4 else BRONZE.c(1)
            return None
        if j == 4:
            return hex_rgb("#5a3a1c")
        return None

    fill_faces(img, HEAD, strap)
    return img


def trim_pattern(leggings):
    """Grayscale trim textures (they use the vanilla trim palette key so any trim material recolours them)."""
    light, mid, dark = (224, 224, 224), (192, 192, 192), (160, 160, 160)
    img = new(64, 32)
    if not leggings:
        def collar(face, i, j, w, h):
            # a broad Egyptian collar (usekh) on the chest and shoulders
            if face in ("top", "bottom"):
                return None
            if face == "front":
                if j <= 3 - abs(i - 3.5) * 0.0 and j <= 3:
                    return light if j == 0 else mid if j % 2 else dark
                if j == 4 and 1 <= i <= w - 2:
                    return dark
                if j == 5 and 2 <= i <= w - 3:
                    return mid
                return None
            if j <= 1:
                return mid if j == 0 else dark
            return None

        fill_faces(img, BODY, collar)

        def crown(face, i, j, w, h):
            # a band with a cobra (uraeus) at the brow
            if face == "top":
                return None
            if j == 1:
                return light if face == "front" else mid
            if face == "front" and i in (3, 4) and j in (0, 2):
                return light
            return None

        fill_faces(img, HEAD, crown)

        def bracers(face, i, j, w, h):
            if face in ("top", "bottom"):
                return None
            if j in (0, 1):
                return mid if j == 0 else dark
            return None

        fill_faces(img, ARM, bracers)

        def anklets(face, i, j, w, h):
            if face in ("top", "bottom"):
                return None
            if j == 9:
                return mid
            return None

        fill_faces(img, LEG, anklets)
    else:
        def sash(face, i, j, w, h):
            if face in ("top", "bottom"):
                return None
            if j == 8:
                return light
            if face == "front" and j > 8 and i in (3, 4):
                return mid if j % 2 else dark
            return None

        fill_faces(img, BODY, sash)

        def kilt(face, i, j, w, h):
            if face in ("top", "bottom"):
                return None
            if j < 4 and (i + j) % 2 == 0:
                return mid
            if j == 4:
                return dark
            return None

        fill_faces(img, LEG, kilt)
    return img


def amber_trim_palette():
    img = new(8, 1)
    colors = ["#ffe9a6", "#f8c65a", "#eea23a", "#d9801f", "#b85f12", "#96470b", "#7a3808", "#5c2905"]
    for x, c in enumerate(colors):
        put(img, x, 0, hex_rgb(c))
    return img


# ------------------------------------------------------------------------------------------ humanoid mobs

def mummy(pharaoh=False):
    img = new(64, 64)
    rng = random.Random(401 if not pharaoh else 402)

    def wraps(face, i, j, w, h, offset=0):
        """Linen bandages wound around the body in slanted strips, with thin seams and the odd dark gap."""
        if face in ("top", "bottom"):
            return LINEN.c(2 + ((i + j + offset) % 3 == 0))
        slant = i // 3 if face in ("front", "back") else (i + 1) // 3
        k = (j + offset + slant) % 3
        if k == 0:
            # seam between two strips; now and then the wrapping has slipped, showing the dark body
            return hex_rgb("#4a3a2a") if rng.random() < 0.12 else LINEN.c(1)
        if k == 1:
            return LINEN.c(4) if rng.random() < 0.8 else LINEN.c(3)
        return LINEN.c(3) if rng.random() < 0.75 else LINEN.c(2)

    def head(face, i, j, w, h):
        if pharaoh and face == "front":
            # golden death mask
            c = GOLD.c(3) if j < 6 else GOLD.c(2)
            if j == 3 and i in (1, 2, 5, 6):
                return LAPIS.c(0) if i in (1, 6) else hex_rgb("#62e0f0")
            if j == 2 and 1 <= i <= 6 and i not in (3, 4):
                return LAPIS.c(1)
            if j == 5 and i in (3, 4):
                return GOLD.c(1)
            if j == 7 and i in (3, 4):
                return LAPIS.c(1)
            return c
        if face == "front":
            if j == 3 and i in (1, 2, 5, 6):
                return hex_rgb("#1c140e") if i in (1, 6) else hex_rgb("#d9e08a")
            if j == 6 and 2 <= i <= 5:
                return hex_rgb("#2a1e16") if i in (3, 4) else LINEN.c(1)
        return wraps(face, i, j, w, h)

    fill_faces(img, HEAD, head)

    def hat(face, i, j, w, h):
        if not pharaoh:
            # a loose, trailing bandage
            if face == "back" and i == 2 and j >= 3:
                return LINEN.c(3)
            if face in ("left", "right") and j == 1:
                return LINEN.c(4) if i % 3 else None
            return None
        # nemes headdress: lapis and gold stripes
        if face == "front":
            if j == 0:
                return GOLD.c(3)
            if i in (0, 7) and j >= 1:
                return LAPIS.c(1) if j % 2 else GOLD.c(3)
            if j == 1 and i in (3, 4):
                return hex_rgb("#e8622a")  # cobra at the brow
            return None
        if face == "bottom":
            return None
        return LAPIS.c(1) if (j + (i if face == "top" else 0)) % 2 else GOLD.c(3)

    fill_faces(img, HAT, hat)

    def body(face, i, j, w, h):
        if pharaoh:
            if face == "front" and j <= 3:
                return [GOLD.c(4), LAPIS.c(2), GOLD.c(3), hex_rgb("#d0781a")][j]  # usekh collar
            if face in ("left", "right", "back") and j <= 1:
                return GOLD.c(3)
            if j in (8, 9):
                return GOLD.c(3) if j == 8 else GOLD.c(2)
            if j >= 10:
                return hex_rgb("#efe9d8") if face != "back" else hex_rgb("#d8d0bc")
        return wraps(face, i, j, w, h, 1)

    fill_faces(img, BODY, body)

    def arm(face, i, j, w, h):
        if pharaoh and j in (6, 7) and face not in ("top", "bottom"):
            return GOLD.c(3) if j == 6 else LAPIS.c(1)
        if j == h - 1 and face != "top":
            return LINEN.c(1)
        return wraps(face, i, j, w, h, 2)

    fill_faces(img, ARM, arm)

    def leg(face, i, j, w, h):
        if pharaoh and j <= 3 and face not in ("top", "bottom"):
            return hex_rgb("#efe9d8") if (i + j) % 3 else hex_rgb("#d8d0bc")
        if pharaoh and j == 10 and face not in ("top", "bottom"):
            return GOLD.c(3)
        return wraps(face, i, j, w, h, 3)

    fill_faces(img, LEG, leg)
    return img


# ------------------------------------------------------------------------------------------ particles, logo

def sand_gust(n):
    img = new(8, 8)
    rng = random.Random(501 + n)
    grains = [[(3, 3)], [(3, 3), (4, 3)], [(3, 3), (4, 4), (3, 4)], [(2, 3), (3, 3), (4, 4), (5, 4)]][n]
    for (x, y) in grains:
        put(img, x, y, SAND.c(rng.randrange(5)))
    return img


def logo():
    small = new(32, 32)
    for y in range(32):
        for x in range(32):
            t = y / 31
            put(small, x, y, mix(hex_rgb("#f2c97a"), hex_rgb("#e07a3a"), t) if y < 20 else SAND.c(2 + (x + y) % 3))
    for y in range(4, 12):
        for x in range(18, 28):
            if math.hypot(x - 22.5, y - 7.5) < 4:
                put(small, x, y, hex_rgb("#fff1b0"))
    for y in range(8, 21):
        half = (y - 8) * 0.9
        for x in range(int(10 - half), int(10 + half) + 1):
            if 0 <= x < 32:
                put(small, x, y, LIMESTONE.c(3) if x < 10 else LIMESTONE.c(1))
    for y in range(10, 21):
        put(small, 25 + (1 if y < 14 else 0), y, PALM_BARK.c(2))
    for (dx, dy) in ((-3, 1), (-2, 0), (-1, -1), (1, -1), (2, 0), (3, 1), (0, -2)):
        put(small, 26 + dx, 10 + dy, LEAF.c(3))
    return small.resize((128, 128), Image.NEAREST)


# ------------------------------------------------------------------------------------------ main

def main():
    blocks = {
        "limestone": limestone(), "polished_limestone": polished_limestone(),
        "limestone_bricks": limestone_bricks(), "cracked_limestone_bricks": cracked_limestone_bricks(),
        "chiseled_limestone_bricks": chiseled_limestone_bricks(), "limestone_pillar": limestone_pillar(),
        "limestone_pillar_top": limestone_pillar_top(), "gilded_limestone": gilded_limestone(),
        "palm_log": palm_log(), "palm_log_top": palm_log_top(), "stripped_palm_log": stripped_palm_log(),
        "stripped_palm_log_top": stripped_palm_log_top(), "palm_planks": palm_planks(),
        "palm_door_top": palm_door(True), "palm_door_bottom": palm_door(False), "palm_trapdoor": palm_trapdoor(),
        "palm_leaves": palm_leaves(), "palm_sapling": palm_sapling(),
        "amber_ore": amber_ore(), "amber_block": amber_block(), "amber_lamp": amber_lamp(),
        "bronze_block": bronze_block(), "quicksand": quicksand(), "dune_grass": dune_grass(),
        "desert_rose": desert_rose(), "cattail_bottom": cattail(False), "cattail_top": cattail(True),
        "ancient_urn_side": urn_side(), "ancient_urn_top": urn_top(),
        "sarcophagus_top": sarcophagus_top(), "sarcophagus_side": sarcophagus_side(),
        "sarcophagus_bottom": sarcophagus_bottom(),
    }
    for stage in range(4):
        blocks["aloe_vera_stage%d" % stage] = aloe(stage)
    for name, img in blocks.items():
        save(img, "block/" + name)

    items = {name: sprite(rows, colors) for name, (rows, colors) in ITEM_SPRITES.items()}
    items["scorpion_stinger"] = sprite([
        "................",
        "................",
        ".......ooooo....",
        "......oSSSSSo...",
        ".....oSsoooSSo..",
        ".....oSo...oSo..",
        "....oHho....oSo.",
        "....oHho.....oo.",
        "...oHhhdo.......",
        "...oHhhdo.......",
        "..oHhhhhdo......",
        "..ohhhhhdo......",
        "..ohhhhddo......",
        "...odddoo.......",
        "....ooo.........",
        "................",
    ], {"o": hex_rgb("#24150a"), "H": hex_rgb("#dcaa5c"), "h": hex_rgb("#c28a42"), "d": hex_rgb("#8a5c2c"),
        "S": hex_rgb("#4a2e16"), "s": hex_rgb("#7a4a22")})
    for name in TOOL_SPRITES:
        items[name] = tool(name)
    for name in ARMOR_SPRITES:
        items[name] = armor_icon(name)
    items["palm_door"] = palm_door_item()
    items["cattail"] = cattail(True)
    for name, img in items.items():
        save(img, "item/" + name)

    save(bronze_layer_1(), "models/armor/bronze_layer_1")
    save(bronze_layer_2(), "models/armor/bronze_layer_2")
    save(amber_goggles_layer(), "models/armor/amber_goggles_layer_1")
    save(new(64, 32), "models/armor/amber_goggles_layer_2")
    save(trim_pattern(False), "trims/models/armor/pharaoh")
    save(trim_pattern(True), "trims/models/armor/pharaoh_leggings")
    save(amber_trim_palette(), "trims/color_palettes/amber")
    save(mummy(False), "entity/mummy")
    save(mummy(True), "entity/pharaoh")
    for n in range(4):
        save(sand_gust(n), "particle/sand_gust_%d" % n)
    logo().save(os.path.join(ROOT, "src/main/resources/logo.png"))
    print("textures: %d blocks, %d items" % (len(blocks), len(items)))
    import volcanic_textures
    volcanic_textures.main()
    import world_textures
    world_textures.main()


if __name__ == "__main__":
    main()

"""Pixel art for the "Living World" update: shore finds, the mill, the cannon, the dreamcatcher and the new items.
Called from textures.py; can also be run on its own."""
import math
import random

from pixels import Palette, hex_rgb, mix
from textures import new, put, get, save, value_noise, from_field, sprite, SAND

WET = Palette("#8f7a52", "#9c875e", "#a8936a", "#b39e75", "#bea980")
STONE = Palette("#6e6e6e", "#7c7c7c", "#8a8a8a", "#989898", "#a6a6a6", "#b4b4b4")
IRON = Palette("#1a1a1e", "#26262c", "#32323a", "#40404a", "#50505c", "#686874")
PAPER = Palette("#c8b690", "#d8c8a0", "#e8dab4", "#f4ead0")
GLASS = (200, 230, 240, 110)
SEA = Palette("#1e3e7a", "#2a5294", "#3a68ae", "#4c80c6", "#6a9ad8")


def outline(img, color):
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            if img.getpixel((x, y))[3] == 0 and any(
                    0 <= x + dx < img.width and 0 <= y + dy < img.height and img.getpixel((x + dx, y + dy))[3] > 0
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                put(out, x, y, color)
    return out


# ------------------------------------------------------------------------------------------ blocks

def wet_sand():
    field = value_noise(16, 16, 1101, scale=4, octaves=2)
    img = from_field([[0.15 + v * 0.7 for v in row] for row in field], WET, 0.25, 1102)
    rng = random.Random(1103)
    for _ in range(5):
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, hex_rgb("#d8c89e"))  # glints of water
    for x in range(16):
        # ripple marks left by the ebbing tide
        y = int(4 + math.sin(x * 0.8) * 1.2)
        put(img, x, y, WET.c(0))
        put(img, x, (y + 8) % 16, WET.c(0))
    return img


SHELL_COLORS = [("#f4d8d0", "#e0a8a0", "#b87870"), ("#f8f0e0", "#e0d0b0", "#a89068"),
                ("#f8c890", "#e89850", "#b06828"), ("#e8e0f0", "#b8a8d0", "#7a6a9a")]


def seashell(variant):
    light, mid, dark = (hex_rgb(c) for c in SHELL_COLORS[variant])
    img = new()
    if variant in (0, 1):
        # scallop: a fan of ridges with a hinge
        for y in range(4, 13):
            for x in range(3, 13):
                dx, dy = x - 7.5, y - 12.0
                d = math.hypot(dx, dy)
                if d < 8.2 and dy < 0.5:
                    angle = math.atan2(dy, dx)
                    ridge = int((angle + math.pi) * 5) % 2
                    put(img, x, y, light if ridge else mid)
                    if d > 7.2:
                        put(img, x, y, dark)
        for x in range(6, 10):
            put(img, x, 12, dark)
    elif variant == 2:
        # conch: a spiral cone
        for y in range(3, 14):
            for x in range(3, 13):
                t = (y - 3) / 10.0
                half = 1.5 + t * 4.0
                if abs(x - 7.5 + t * 1.5) <= half:
                    band = int((y + x * 0.5)) % 3
                    put(img, x, y, (light, mid, dark)[band])
        put(img, 7, 13, dark)
    else:
        # snail shell: a spiral
        for y in range(4, 13):
            for x in range(4, 13):
                dx, dy = x - 8.0, y - 8.5
                d = math.hypot(dx, dy)
                if d < 4.5:
                    angle = math.atan2(dy, dx)
                    spiral = (d - angle * 0.7) % 1.6
                    put(img, x, y, dark if spiral < 0.4 else light if spiral > 1.0 else mid)
    return img


def clam(top):
    img = new()
    rng = random.Random(1200 + top)
    shell = Palette("#6a6258", "#7e766a", "#928a7c", "#a69e90", "#bab2a4")
    for y in range(16):
        for x in range(16):
            if top:
                d = math.hypot(x - 7.5, (y - 8.5) * 1.2)
                if d < 7.5:
                    ring = int(d) % 2
                    put(img, x, y, shell.c(2 + ring + rng.choice([0, 0, -1])))
            else:
                ring = (y // 2) % 2
                put(img, x, y, shell.c(1 + ring + rng.choice([0, 0, 1])))
    if not top:
        for x in range(16):
            put(img, x, 8, hex_rgb("#3e3830"))  # the seam where it opens
    return img


def millstone_top():
    """The runner stone, with the radial grooves that grind the grain and a hole in the middle."""
    img = new()
    rng = random.Random(1301)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            angle = math.atan2(dy, dx)
            c = STONE.c(2 + rng.choice([0, 0, 1, -1]))
            if 2.2 < d < 7.2 and int((angle + math.pi) / (math.pi / 4)) % 2 == 0 and abs(math.sin(angle * 4)) < 0.25:
                c = STONE.c(0)
            if d <= 1.6:
                c = hex_rgb("#3a2a1a")  # the eye, where grain goes in
            if d > 7.2:
                c = STONE.c(4)
            put(img, x, y, c)
    return img


def millstone_side():
    img = new()
    rng = random.Random(1302)
    for y in range(16):
        for x in range(16):
            put(img, x, y, STONE.c(2 + rng.choice([0, 0, 1, -1])))
    for x in range(16):
        put(img, x, 0, STONE.c(5))
        put(img, x, 15, STONE.c(0))
        if x % 4 == 0:
            for y in range(2, 14):
                put(img, x, y, STONE.c(1))
    return img


def cannon_barrel():
    img = new()
    rng = random.Random(1401)
    for y in range(16):
        for x in range(16):
            c = IRON.c(2 + (x in (3, 12)) - (x in (0, 15)) + rng.choice([0, 0, 1]))
            if y in (0, 1, 14, 15):
                c = IRON.c(4)  # reinforcing rings
            put(img, x, y, c)
    return img


def cannon_muzzle():
    img = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            put(img, x, y, IRON.c(0) if d < 4.5 else IRON.c(4) if d < 6 else IRON.c(2))
    return img


def dreamcatcher():
    """A willow hoop with a woven web, a bead in the middle and feathers hanging below."""
    img = new()
    hoop = hex_rgb("#8a5a2e")
    web = (232, 226, 210)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 5.5)
            if 4.6 <= d <= 5.6:
                put(img, x, y, hoop)
            elif d < 4.6:
                angle = math.atan2(y - 5.5, x - 7.5)
                if abs(math.sin(angle * 3)) < 0.16 or abs(d - 2.5) < 0.35:
                    put(img, x, y, web)
    put(img, 7, 5, hex_rgb("#5aa0d8"))
    put(img, 8, 5, hex_rgb("#3a78b0"))
    put(img, 7, 0, hoop)
    put(img, 8, 0, hoop)
    for fx, color in ((4, "#f0f0f0"), (7, "#c83a2a"), (11, "#f0f0f0")):
        for y in range(11, 16):
            put(img, fx, y, hex_rgb(color) if y > 11 else web)
        put(img, fx + 1, 14, hex_rgb(color))
    return img


# ------------------------------------------------------------------------------------------ items

ITEMS = {
    "flour": ([
        "................",
        "................",
        "......oooo......",
        ".....oTTTTo.....",
        "......oTTo......",
        ".....oSSSSo.....",
        "....oSsSSSSo....",
        "...oSSSWWSSSo...",
        "...oSSWWWWSso...",
        "...oSsWWWWSSo...",
        "...oSSSWWSsSo...",
        "...oSSSSSSSSo...",
        "....oSsSSSSo....",
        ".....oooooo.....",
        "................",
        "................",
    ], {"o": hex_rgb("#6a5a3a"), "T": hex_rgb("#a08a5a"), "S": hex_rgb("#d8c8a0"), "s": hex_rgb("#bca878"),
        "W": hex_rgb("#fafaf4")}),
    "dough": ([
        "................",
        "................",
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "....oDDDdDDo....",
        "...oDdDDDDDDo...",
        "..oDDDDDdDDDDo..",
        "..oDDDDDDDDdDo..",
        "..osDDdDDDDDso..",
        "...ossDDDDsso...",
        "....oosssso.....",
        "......oooo......",
        "................",
        "................",
    ], {"o": hex_rgb("#8a6e44"), "D": hex_rgb("#f0dcb0"), "d": hex_rgb("#e0c898"), "s": hex_rgb("#c8aa78")}),
    "pearl": ([
        "................",
        "................",
        "................",
        "................",
        "................",
        "......oooo......",
        ".....oWHPPo.....",
        "....oPHHPPPo....",
        "....oPPPPPPo....",
        "....oPPPPPdo....",
        "....oPPPPddo....",
        ".....oPddddo....",
        "......oooo......",
        "................",
        "................",
        "................",
    ], {"o": hex_rgb("#8a8494"), "W": hex_rgb("#ffffff"), "H": hex_rgb("#f8f4fc"), "P": hex_rgb("#e4dcec"),
        "d": hex_rgb("#c0b4cc")}),
    "message_in_a_bottle": ([
        "................",
        "................",
        "...........cc...",
        "..........cCco..",
        ".........oGGo...",
        "........oGGGGo..",
        ".......oGPPPGo..",
        "......oGPPpPGo..",
        ".....oGPPpPGo...",
        "....oGPPpPGo....",
        "...oGPpPPGo.....",
        "..oGPPPPGo......",
        "..oGGGGGo.......",
        "...ooooo........",
        "................",
        "................",
    ], {"o": hex_rgb("#3a5a5a"), "G": (190, 225, 230), "P": hex_rgb("#e8dab4"), "p": hex_rgb("#a08860"),
        "c": hex_rgb("#a0764a"), "C": hex_rgb("#6e4a26")}),
    "cannonball": ([
        "................",
        "................",
        "................",
        "................",
        "......oooo......",
        "....ooIIIIoo....",
        "...oIHHIIIIIo...",
        "...oIHIIIIIIo...",
        "..oIIIIIIIIIdo..",
        "..oIIIIIIIIIdo..",
        "...oIIIIIIIddo..",
        "...oIIIIIIddo...",
        "....ooIdddoo....",
        "......oooo......",
        "................",
        "................",
    ], {"o": IRON.c(0), "I": IRON.c(3), "H": IRON.c(5), "d": IRON.c(1)}),
    "gloom_dust": ([
        "................",
        "................",
        "................",
        "................",
        "......w.........",
        ".......a....w...",
        ".....aAa........",
        "....aAAaaA......",
        "...aaAaAaaa..w..",
        "..aAaaAaaAaa....",
        "..aaAaaaaaaAa...",
        ".aaaaAaaAaaaaa..",
        ".aAaaawaaaAaaa..",
        "..dddaAadddddd..",
        "................",
        "................",
    ], {"a": hex_rgb("#1a1a24"), "A": hex_rgb("#2c2c3c"), "d": hex_rgb("#0a0a10"), "w": hex_rgb("#d8d8f0")}),
    "world_chronicle": ([
        "................",
        "..oooooooooooo..",
        "..oLLLLLLLLLLpo.",
        "..oLGGGGGGGGLpo.",
        "..oLGLLLLLLGLpo.",
        "..oLGLLCCLLGLpo.",
        "..oLGLCssCLGLpo.",
        "..oLGLCssCLGLpo.",
        "..oLGLLCCLLGLpo.",
        "..oLGLLLLLLGLpo.",
        "..oLGGGGGGGGLpo.",
        "..oLLLLLLLLLLpo.",
        "..oLLLLLLLLLLpo.",
        "..ooooooooooopo.",
        "...pppppppppppo.",
        "....oooooooooo..",
    ], {"o": hex_rgb("#1e2a14"), "L": hex_rgb("#3e5a2a"), "G": hex_rgb("#d4a83a"), "C": hex_rgb("#6a9ad8"),
        "s": hex_rgb("#2a5294"), "p": hex_rgb("#e8dab4")}),
    "captain_hat": ([
        "................",
        "................",
        "................",
        "................",
        ".......ooo.W....",
        "......oHHHoW....",
        ".....oHhhhhoW...",
        ".G..oHhhhhhhoG..",
        ".GoohhhhhhhhhoG.",
        "..GhhhhYYhhhhG..",
        "...GhhhhhhhhG...",
        "....GGGGGGGG....",
        "................",
        "................",
        "................",
        "................",
    ], {"o": hex_rgb("#08080a"), "H": hex_rgb("#34343c"), "h": hex_rgb("#1c1c22"), "G": hex_rgb("#e8c040"),
        "Y": hex_rgb("#e8c040"), "W": hex_rgb("#f8f8f0")}),
}


CUTLASS = [
    "................",
    "..........oo....",
    ".........oHHo...",
    "........oHhho...",
    ".......oHhho....",
    "......oHhho.....",
    ".....oHhho......",
    "....oHhdo.......",
    "...oHhdo........",
    "..ggHddo........",
    ".gGGgoo.........",
    "..gSGg..........",
    "..oSo.g.........",
    ".oSo............",
    ".oo.............",
    "................",
]


def cutlass():
    return sprite(CUTLASS, {"o": hex_rgb("#202428"), "H": hex_rgb("#f0f4f8"), "h": hex_rgb("#c0c8d0"),
                            "d": hex_rgb("#8890a0"), "g": hex_rgb("#b08820"), "G": hex_rgb("#e8c040"),
                            "S": hex_rgb("#5a3a1c")})


def tide_clock(frame):
    """A copper dial showing the sea: the water rises from low (frame 0) to high (frame 7)."""
    img = new()
    copper = Palette("#6a3a1e", "#9a5a2e", "#c07a44", "#e0a060")
    level = 11 - frame
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 7.3:
                continue
            if d > 6.2:
                put(img, x, y, copper.c(3 if y < 8 else 1))
            elif y >= level:
                wave = (x + y) % 4 == 0
                put(img, x, y, SEA.c(4) if wave and y == level else SEA.c(2 if y < level + 3 else 1))
            else:
                put(img, x, y, hex_rgb("#d8ecf8") if y < 5 else hex_rgb("#b8d8f0"))
    # needle
    angle = math.pi * (1.0 - frame / 7.0)
    for r in range(0, 6):
        put(img, int(round(7.5 + math.cos(angle) * r)), int(round(7.5 - math.sin(angle) * r)), hex_rgb("#2a1a10"))
    return img


def pearl_trim_palette():
    img = new(8, 1)
    for x, c in enumerate(["#ffffff", "#f6f2fa", "#ece4f2", "#e0d6ea", "#d0c4dc", "#bcaeca", "#a494b4", "#8a7a9c"]):
        put(img, x, 0, hex_rgb(c))
    return img


def foam(n):
    """A fleck of foam: a soft white blob, a little different in each frame."""
    img = new(8, 8)
    px = img.load()
    cx, cy = 3.5 + (n - 1) * 0.5, 3.5
    radius = 1.6 + n * 0.7
    for y in range(8):
        for x in range(8):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= radius:
                edge = d > radius - 1.0
                px[x, y] = (230, 242, 250, 150) if edge else (250, 253, 255, 230)
    return img


def main():
    blocks = {"wet_sand": wet_sand(), "clam_top": clam(True), "clam_side": clam(False),
              "millstone_top": millstone_top(), "millstone_side": millstone_side(),
              "cannon_barrel": cannon_barrel(), "cannon_muzzle": cannon_muzzle(), "dreamcatcher": dreamcatcher()}
    for v in range(4):
        blocks["seashell_%d" % v] = seashell(v)
    for name, img in blocks.items():
        save(img, "block/" + name)
    items = {name: sprite(rows, colors) for name, (rows, colors) in ITEMS.items()}
    items["cutlass"] = cutlass()
    for f in range(8):
        items["tide_clock_%02d" % f] = tide_clock(f)
    for name, img in items.items():
        save(img, "item/" + name)
    save(pearl_trim_palette(), "trims/color_palettes/pearl")
    for n in range(3):
        save(foam(n), "particle/foam_%d" % n)
    print("living world textures: %d blocks, %d items" % (len(blocks), len(items)))


if __name__ == "__main__":
    main()

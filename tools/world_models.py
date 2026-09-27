"""Entity models and skins of the "Living World" update.

* The Shade gets its own model (Java class + texture), like the other mobs of the mod.
* Pirates, the pirate gunner, the captain and the traveller reuse the vanilla illager and villager models, so only
  their skins are painted here, on the vanilla UV layouts; the tricorn hat has its own small texture.
"""
import os

from PIL import Image

from entity_models import Cube, Part, paint, TEX_DIR
from pixels import Palette, hex_rgb


def hashed(*values):
    h = 0
    for v in values:
        h = (h * 1000003 + v * 2654435761 + 0x9E3779B9) & 0xFFFFFFFF
    return (h % 1000) / 1000.0


# ----------------------------------------------------------------------------------------------- the shade

SHADOW = Palette("#07070b", "#0c0c12", "#12121a", "#191924", "#22222e")


def shade():
    def skin(face, i, j, w, h):
        n = hashed(i, j, len(face), 7)
        tone = 1 + int(n * 2.5)
        if face == "top":
            tone += 1
        return (*SHADOW.c(tone), 255)

    def head(face, i, j, w, h):
        if face == "front":
            if j == 3 and i in (1, 4):
                return (230, 232, 240)
            if j == 4 and i in (1, 4):
                return (120, 122, 140)
            if j == 6 and 2 <= i <= 3:
                return (2, 2, 4)
        return skin(face, i, j, w, h)

    def eyes(face, i, j, w, h):
        if face == "front" and j == 3 and i in (1, 4):
            return (240, 242, 255)
        return None

    def limb(face, i, j, w, h):
        # long fingers at the end of the arms
        if j >= h - 3 and face != "top" and i % 2 == 0:
            return (3, 3, 6)
        return skin(face, i, j, w, h)

    parts = [
        Part("body", None, (0, -8, 0), cubes=[Cube(0, 16, -3, 0, -1.5, 6, 14, 3, skin)]),
        Part("head", None, (0, -8, 0), cubes=[Cube(0, 0, -3, -8, -3, 6, 8, 6, head, glow=eyes)]),
        Part("right_arm", None, (-4, -7, 0), cubes=[Cube(24, 0, -1.5, 0, -1, 2, 20, 2, limb)]),
        Part("left_arm", None, (4, -7, 0), cubes=[Cube(24, 0, -0.5, 0, -1, 2, 20, 2, limb, mirror=True)]),
        Part("right_leg", None, (-1.5, 6, 0), cubes=[Cube(32, 0, -1, 0, -1, 2, 18, 2, skin)]),
        Part("left_leg", None, (1.5, 6, 0), cubes=[Cube(32, 0, -1, 0, -1, 2, 18, 2, skin, mirror=True)]),
    ]
    anim = """        if (entity.isWatched()) {
            // Frozen mid-step the moment it is seen, head cocked to one side.
            this.head.zRot = 0.4F;
            this.head.xRot = -0.1F;
            this.rightArm.xRot = -0.25F;
            this.leftArm.xRot = -0.15F;
            this.rightArm.zRot = 0.08F;
            this.leftArm.zRot = -0.08F;
            return;
        }
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.head.zRot = Mth.sin(ageInTicks * 0.9F) * 0.03F;
        float swing = limbSwing * 0.5F;
        this.rightLeg.xRot = Mth.cos(swing) * 0.9F * limbSwingAmount;
        this.leftLeg.xRot = Mth.cos(swing + Mth.PI) * 0.9F * limbSwingAmount;
        this.rightArm.xRot = Mth.cos(swing + Mth.PI) * 0.35F * limbSwingAmount;
        this.leftArm.xRot = Mth.cos(swing) * 0.35F * limbSwingAmount;
        this.rightArm.zRot = 0.05F + Mth.sin(ageInTicks * 0.05F) * 0.03F;
        this.leftArm.zRot = -0.05F - Mth.sin(ageInTicks * 0.05F) * 0.03F;"""
    return "ShadeModel", ("shade", "com.dunesrelics.entity.world.Shade"), parts, 64, 64, \
        "Shade: a tall, thin figure of darkness with two pale eyes.", anim


MODELS = (shade,)


# ----------------------------------------------------------------------------------------------- skins on vanilla layouts

def faces(u, v, w, h, d):
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def fill(img, box, painter):
    px = img.load()
    for face, (fx, fy, fw, fh) in faces(*box).items():
        for j in range(fh):
            for i in range(fw):
                c = painter(face, i, j, fw, fh)
                if c is not None:
                    px[fx + i, fy + j] = c if len(c) == 4 else (*c, 255)


def noisy(pal, seed, spread=2):
    def painter(face, i, j, w, h):
        n = hashed(i, j, len(face), seed)
        return pal.c(1 + int(n * spread))
    return painter


# illager boxes: (u, v, w, h, d)
ILL_HEAD = (0, 0, 8, 10, 8)
ILL_NOSE = (24, 0, 2, 4, 2)
ILL_BODY = (16, 20, 8, 12, 6)
ILL_ROBE = (0, 38, 8, 20, 6)
ILL_ARMS_SIDE = (44, 22, 4, 8, 4)
ILL_ARMS_MID = (40, 38, 8, 4, 4)
ILL_LEG = (0, 22, 4, 12, 4)
ILL_ARM = (40, 46, 4, 12, 4)

SKIN = Palette("#8e6a52", "#a07a5e", "#b08a6a", "#bf9a78")
ILLAGER_SKIN = Palette("#7d8584", "#8c9493", "#9aa2a0", "#a8b0ae")


def illager_skin(kind):
    """kind: pirate, gunner or captain."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    beard = {"pirate": hex_rgb("#2a1a10"), "gunner": hex_rgb("#6a6a6a"), "captain": hex_rgb("#141010")}[kind]
    shirt_a, shirt_b = {"pirate": ("#e8e4dc", "#2a3a6a"), "gunner": ("#d8d0bc", "#8a2a22"),
                        "captain": ("#f0ece4", "#f0ece4")}[kind]
    coat = {"pirate": Palette("#3a2618", "#4a3220", "#5a3e28", "#6a4a30"),
            "gunner": Palette("#1c2a4a", "#24345a", "#2c3e6a", "#34487a"),
            "captain": Palette("#5a0e10", "#7a1418", "#961c20", "#b0282a")}[kind]
    pants = Palette("#2a2420", "#3a322a", "#4a4036") if kind != "gunner" else Palette("#8a7a5a", "#9a8a68", "#aa9a78")
    gold = hex_rgb("#e8c040")

    def head(face, i, j, w, h):
        base = ILLAGER_SKIN.c(1 + int(hashed(i, j, len(face), 3) * 2))
        if face == "front":
            if j == 4 and i in (1, 2, 5, 6):
                if kind == "pirate" and i in (5, 6):
                    return (20, 16, 14)  # eyepatch
                return (240, 240, 240) if i in (2, 5) else (30, 60, 40)
            if j == 3 and 1 <= i <= 6:
                return (40, 36, 34)  # heavy brow
            if kind == "pirate" and 1 <= j <= 3 and i == 5 + (j - 1) // 2:
                return (20, 16, 14)  # eyepatch strap
            if j >= 7:
                return beard if (i + j) % 5 else (60, 40, 30)
            if j == 6 and i in (1, 6):
                return beard
        if face in ("left", "right") and j >= 7 and (i < 3 if face == "right" else i > 4):
            return beard
        if kind == "pirate" and (face == "top" or j <= 2 and face != "bottom"):
            # a red bandana tied at the back
            return (170, 30, 30) if (i + j) % 3 else (120, 20, 20)
        if kind == "captain" and face == "right" and j == 5 and i == 3:
            return gold  # earring
        return base

    fill(img, ILL_HEAD, head)
    fill(img, ILL_NOSE, lambda f, i, j, w, h: ILLAGER_SKIN.c(2) if j < 3 else beard)

    def body(face, i, j, w, h):
        if face in ("front", "back", "left", "right") and j < h:
            stripe = (j // 2) % 2 == 0
            return hex_rgb(shirt_a) if stripe else hex_rgb(shirt_b)
        return hex_rgb(shirt_a)

    fill(img, ILL_BODY, body)

    def robe(face, i, j, w, h):
        if face == "top" or face == "bottom":
            return None
        if kind == "captain":
            if face == "front" and 2 <= i <= 5 and j < 11:
                return None  # open coat showing the shirt
            if face == "front" and i in (1, 6) and j % 3 == 1 and j < 12:
                return gold  # buttons
            if j == 10:
                return (30, 20, 16)  # belt
            if j == 11 and face == "front" and i in (3, 4):
                return gold
            return coat.c(1 + int(hashed(i, j, 5, len(face)) * 3))
        if kind == "gunner":
            if face == "front" and 3 <= i <= 4 and j < 10:
                return None
            if j == 9:
                return (40, 28, 18)
            return coat.c(1 + int(hashed(i, j, 6, len(face)) * 3))
        # the pirate: a short leather vest, open at the front, over the striped shirt
        if j >= 12:
            return None
        if face == "front" and 2 <= i <= 5:
            return None
        if j == 10:
            return (20, 14, 10)
        return coat.c(1 + int(hashed(i, j, 7, len(face)) * 3))

    fill(img, ILL_ROBE, robe)

    def arms(face, i, j, w, h):
        stripe = (j // 2) % 2 == 0
        if kind == "captain":
            return coat.c(2) if j < h - 2 else ILLAGER_SKIN.c(2)
        if kind == "gunner":
            return coat.c(2) if j < h - 2 else ILLAGER_SKIN.c(2)
        return hex_rgb(shirt_a) if stripe else hex_rgb(shirt_b)

    fill(img, ILL_ARMS_SIDE, arms)
    fill(img, ILL_ARMS_MID, lambda f, i, j, w, h: ILLAGER_SKIN.c(2) if f in ("front", "top") else arms(f, i, j, w, h))

    def arm(face, i, j, w, h):
        if j >= h - 3:
            return ILLAGER_SKIN.c(2)
        return arms(face, i, j, w, h)

    fill(img, ILL_ARM, arm)

    def leg(face, i, j, w, h):
        if j >= h - 4 or face == "bottom":
            return (26, 20, 18) if kind != "gunner" else (60, 40, 24)  # boots
        return pants.c(1 + int(hashed(i, j, 9, len(face)) * 2))

    fill(img, ILL_LEG, leg)
    return img


# villager boxes
VIL_HEAD = (0, 0, 8, 10, 8)
VIL_HAT = (32, 0, 8, 10, 8)
VIL_NOSE = (24, 0, 2, 4, 2)
VIL_BODY = (16, 20, 8, 12, 6)
VIL_ROBE = (0, 38, 8, 20, 6)
VIL_ARMS_SIDE = (44, 22, 4, 8, 4)
VIL_ARMS_MID = (40, 38, 8, 4, 4)
VIL_LEG = (0, 22, 4, 12, 4)


def traveler_skin():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    cloak = Palette("#3e4a2a", "#4a5832", "#56663a", "#627242")
    hat = Palette("#5a4028", "#6a4c30", "#7a5838")
    skin = Palette("#9a6e52", "#aa7c5e", "#b8896a")

    def head(face, i, j, w, h):
        if face == "front":
            if j == 4 and i in (1, 2, 5, 6):
                return (240, 240, 240) if i in (1, 6) else (40, 90, 60)
            if j == 3 and 1 <= i <= 6:
                return (70, 50, 36)
            if j >= 7 and 1 <= i <= 6:
                return (120, 110, 100) if (i + j) % 3 else (90, 80, 72)  # grey stubble
        return skin.c(1 + int(hashed(i, j, len(face), 11) * 2))

    fill(img, VIL_HEAD, head)
    fill(img, VIL_NOSE, lambda f, i, j, w, h: skin.c(1))

    def hat_band(face, i, j, w, h):
        # only the crown of the travel hat: the top rows of the hat layer
        if face == "top":
            return hat.c(1 + int(hashed(i, j, 12, 1) * 2))
        if face != "bottom" and j <= 2:
            return (40, 28, 20) if j == 2 else hat.c(1 + (i + j) % 2)
        return None

    fill(img, VIL_HAT, hat_band)
    # the wide brim (16 x 16 x 1 box at 30, 47)
    px = img.load()
    for x in range(30, 30 + 16 + 16 + 2):
        for y in range(47, 47 + 1 + 16):
            if x < 64 and y < 64:
                u = x - 31
                v = y - 48
                if 0 <= u < 16 and 0 <= v < 16:
                    d = ((u - 7.5) ** 2 + (v - 7.5) ** 2) ** 0.5
                    if d <= 7.8 and not (5 <= u <= 10 and 5 <= v <= 10):
                        px[x, y] = (*hat.c(1 + int(hashed(u, v, 13, 2) * 2)), 255)
    fill(img, VIL_BODY, lambda f, i, j, w, h: (170, 150, 120) if (i + j) % 4 else (150, 130, 100))

    def robe(face, i, j, w, h):
        if face in ("top", "bottom"):
            return None
        if j == 11:
            return (60, 40, 24)  # belt
        if face == "back" and 2 <= i <= 5 and 1 <= j <= 7:
            return (110, 80, 50) if j in (1, 7) or i in (2, 5) else (130, 96, 60)  # backpack patch
        return cloak.c(1 + int(hashed(i, j, 14, len(face)) * 3))

    fill(img, VIL_ROBE, robe)
    fill(img, VIL_ARMS_SIDE, lambda f, i, j, w, h: cloak.c(2) if j < h - 2 else skin.c(2))
    fill(img, VIL_ARMS_MID, lambda f, i, j, w, h: skin.c(2) if f in ("front", "top") else cloak.c(2))
    fill(img, VIL_LEG, lambda f, i, j, w, h: (60, 44, 30) if j >= h - 3 else (90, 80, 60))
    return img


def tricorn(captain):
    """Texture of TricornModel: crown (0,16) 9x3x9, brim (0,28) 13x1x13, back (0,42) 13x3x1, flaps (30,42) 10x3x1,
    feather (54,16) 1x6x3."""
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    felt = Palette("#101012", "#18181c", "#202026", "#2a2a30")
    gold = (232, 192, 64)

    def crown(face, i, j, w, h):
        if face not in ("top", "bottom") and j == h - 1:
            return gold if captain else (140, 24, 24)  # hat band
        return felt.c(1 + int(hashed(i, j, len(face), 21) * 3))

    def brim(face, i, j, w, h):
        edge = face == "top" and (i in (0, w - 1) or j in (0, h - 1))
        if edge and captain:
            return gold
        return felt.c(1 + int(hashed(i, j, len(face), 22) * 2))

    def flap(face, i, j, w, h):
        if captain and j == 0 and face in ("front", "back", "top"):
            return gold
        return felt.c(1 + int(hashed(i, j, len(face), 23) * 2))

    fill(img, (0, 16, 9, 3, 9), crown)
    fill(img, (0, 28, 13, 1, 13), brim)
    fill(img, (0, 42, 13, 3, 1), flap)
    fill(img, (30, 42, 10, 3, 1), flap)
    if captain:
        fill(img, (54, 16, 1, 6, 3), lambda f, i, j, w, h: (250, 250, 245) if j < h - 1 else (200, 200, 190))
    return img


def main():
    import entity_models
    for build in MODELS:
        class_name, layer, parts, tw, th, doc, anim = build()[:7]
        with open(os.path.join(entity_models.JAVA_DIR, class_name + ".java"), "w") as out:
            out.write(entity_models.java_model(class_name, layer, parts, tw, th, doc, anim, ["import " + layer[1] + ";"]))
        paint(parts, tw, th).save(os.path.join(TEX_DIR, layer[0] + ".png"))
        paint(parts, tw, th, glow=True).save(os.path.join(TEX_DIR, layer[0] + "_glow.png"))
        print("generated", class_name)
    illager_skin("pirate").save(os.path.join(TEX_DIR, "pirate.png"))
    illager_skin("gunner").save(os.path.join(TEX_DIR, "pirate_gunner.png"))
    illager_skin("captain").save(os.path.join(TEX_DIR, "pirate_captain.png"))
    traveler_skin().save(os.path.join(TEX_DIR, "traveler.png"))
    tricorn(False).save(os.path.join(TEX_DIR, "pirate_gunner_hat.png"))
    tricorn(True).save(os.path.join(TEX_DIR, "pirate_captain_hat.png"))
    armor = os.path.join(os.path.dirname(TEX_DIR), "models/armor")
    os.makedirs(armor, exist_ok=True)
    tricorn(True).save(os.path.join(armor, "captain_hat.png"))
    print("generated living world skins")


if __name__ == "__main__":
    main()

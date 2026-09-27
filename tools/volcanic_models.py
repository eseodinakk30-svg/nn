"""Entity models of the "Ash & Ember" update (see entity_models.py for how models are generated)."""
from entity_models import Cube, Part
from pixels import Palette, hex_rgb

BASALT = Palette("#1c1918", "#262221", "#302b29", "#3b3432", "#4a423e")
EMBER = Palette("#b8400c", "#e0641a", "#ff8a1c", "#ffb030", "#ffd86a")
ASH = Palette("#4a4644", "#5e5a57", "#6f6a67", "#86807c", "#a39d98")


def hashed(*values):
    """A stable pseudo-random number in 0..1 for a pixel, so textures do not change between runs."""
    a, b, c, d = values
    return ((a * 73856093) ^ (b * 19349663) ^ (c * 83492791) ^ (d * 2654435761)) % 1000 / 1000.0


def crack_mask(seed):
    """Veins of glowing magma running across a face."""
    def is_crack(face, i, j, w, h):
        f = {"top": 1, "bottom": 2, "left": 3, "right": 4, "front": 5, "back": 6}[face]
        # diagonal veins with a little jitter
        a = (i + j * 2 + seed + f * 3) % 11 == 0
        b = (i * 2 - j + seed * 5 + f) % 13 == 0 and hashed(i // 3, j // 3, f, seed) < 0.5
        return (a or b) and hashed(i, j, f, seed) < 0.92
    return is_crack


def rock(seed, pal=BASALT, glow_pal=EMBER, crack_density=True):
    cracks = crack_mask(seed)

    def base(face, i, j, w, h):
        if crack_density and cracks(face, i, j, w, h):
            return glow_pal.c(1)
        n = hashed(i, j, seed, len(face))
        tone = 1 + int(n * 3)
        if face == "top":
            tone += 1
        elif face == "bottom":
            tone -= 1
        return pal.c(tone)

    def glow(face, i, j, w, h):
        if crack_density and cracks(face, i, j, w, h):
            return glow_pal.c(3 if hashed(i, j, seed, 7) < 0.5 else 2)
        return None

    return base, glow


# ----------------------------------------------------------------------------------------------- magma titan

def magma_titan():
    body, body_glow = rock(1)
    limb, limb_glow = rock(2)
    fist, fist_glow = rock(3)
    leg, leg_glow = rock(4)
    stone, _ = rock(5, crack_density=False)

    def head(face, i, j, w, h):
        if face == "front" and j in (3, 4) and i in (1, 2, 5, 6):
            return EMBER.c(4)
        if face == "front" and j == 6 and 2 <= i <= 5:
            return EMBER.c(1)
        return BASALT.c(2 + (i + j) % 2)

    def head_glow(face, i, j, w, h):
        if face == "front" and j in (3, 4) and i in (1, 2, 5, 6):
            return EMBER.c(4)
        if face == "front" and j == 6 and 2 <= i <= 5:
            return EMBER.c(2)
        return None

    def core(face, i, j, w, h):
        return EMBER.c(3 + (i + j) % 2)

    parts = [
        Part("body", None, (0, 6, 0), cubes=[Cube(0, 0, -10, -20, -6, 20, 20, 12, body, glow=body_glow)]),
        Part("core", "body", (0, -12, -6), cubes=[Cube(96, 20, -3, -3, -1, 6, 6, 1, core, glow=core)]),
        Part("head", "body", (0, -20, -2), cubes=[Cube(64, 0, -4, -7, -4, 8, 7, 8, head, glow=head_glow)]),
        Part("right_rock", "body", (-7, -20, 1), (0.2, 0, -0.3), [Cube(96, 0, -2.5, -4, -3, 5, 4, 6, stone)]),
        Part("left_rock", "body", (7, -20, 2), (-0.2, 0, 0.35), [Cube(96, 10, -2, -3, -2, 4, 3, 4, stone)]),
        Part("right_arm", None, (-12, -11, 0), cubes=[Cube(0, 32, -7, -3, -4, 7, 22, 8, limb, glow=limb_glow)]),
        Part("right_fist", "right_arm", (-3.5, 19, 0), cubes=[Cube(60, 32, -4.5, 0, -4.5, 9, 8, 9, fist, glow=fist_glow)]),
        Part("left_arm", None, (12, -11, 0), cubes=[Cube(30, 32, 0, -3, -4, 7, 22, 8, limb, mirror=True, glow=limb_glow)]),
        Part("left_fist", "left_arm", (3.5, 19, 0), cubes=[Cube(60, 32, -4.5, 0, -4.5, 9, 8, 9, fist, mirror=True, glow=fist_glow)]),
        Part("right_leg", None, (-5, 6, 0), cubes=[Cube(0, 64, -3.5, 0, -3.5, 7, 18, 7, leg, glow=leg_glow)]),
        Part("left_leg", None, (5, 6, 0), cubes=[Cube(28, 64, -3.5, 0, -3.5, 7, 18, 7, leg, mirror=True, glow=leg_glow)]),
    ]

    anim = """        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.6F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.5F;

        float walk = limbSwing * 0.5F;
        this.rightLeg.xRot = Mth.cos(walk) * 0.9F * limbSwingAmount;
        this.leftLeg.xRot = -Mth.cos(walk) * 0.9F * limbSwingAmount;
        this.rightArm.xRot = -Mth.cos(walk) * 0.5F * limbSwingAmount;
        this.leftArm.xRot = Mth.cos(walk) * 0.5F * limbSwingAmount;
        this.body.zRot = Mth.cos(walk) * 0.04F * limbSwingAmount;
        this.body.xRot = 0.08F + Mth.sin(ageInTicks * 0.05F) * 0.02F;
        this.core.xScale = this.core.yScale = 1.0F + Mth.sin(ageInTicks * 0.2F) * 0.08F;

        // Punch with the right fist.
        float punch = Mth.sin(this.attackTime * Mth.PI);
        this.rightArm.xRot -= punch * 1.6F;

        // Ground slam: both fists raised overhead, then brought down.
        float partialTick = ageInTicks - entity.tickCount;
        int slam = entity.getSlamAnim();
        if (slam > 0) {
            float progress = 1.0F - (slam - partialTick) / com.dunesrelics.entity.volcanic.MagmaTitan.SLAM_DURATION;
            float lift = progress < 0.5F ? progress * 2.0F : 1.0F - (progress - 0.5F) * 2.0F;
            this.rightArm.xRot = -2.8F * lift;
            this.leftArm.xRot = -2.8F * lift;
            this.body.xRot = -0.25F * lift + 0.3F * (1.0F - lift) * (progress > 0.5F ? 1.0F : 0.0F);
        }"""
    return "MagmaTitanModel", ("magma_titan", "com.dunesrelics.entity.volcanic.MagmaTitan"), parts, 128, 128, \
        "Magma Titan: a colossus of basalt with glowing magma veins, a burning core and enormous fists.", anim


# ----------------------------------------------------------------------------------------------- salamander

def salamander():
    black = Palette("#141110", "#1c1816", "#262120", "#302a28")
    spots = Palette("#e06a14", "#f08a1c", "#ffb030", "#ffd24a")

    def spotted(seed):
        def is_spot(face, i, j, w, h):
            return face in ("top", "left", "right") and hashed(i // 2, j // 2, seed, len(face)) < 0.28

        def base(face, i, j, w, h):
            if is_spot(face, i, j, w, h):
                return spots.c(1 + (i + j) % 2)
            if face == "bottom":
                return black.c(3)
            return black.c((i + j) % 3)

        def glow(face, i, j, w, h):
            return spots.c(2) if is_spot(face, i, j, w, h) else None

        return base, glow

    body, body_glow = spotted(11)
    tail, tail_glow = spotted(12)
    leg = lambda face, i, j, w, h: black.c(1 + (i + j) % 2)

    def head(face, i, j, w, h):
        if face in ("left", "right") and j == 0 and i == 1:
            return spots.c(3)
        if face == "top" and (i in (0, w - 1)) and j < 2:
            return spots.c(1)
        return black.c((i + j) % 3)

    def head_glow(face, i, j, w, h):
        if face in ("left", "right") and j == 0 and i == 1:
            return spots.c(3)
        if face == "top" and (i in (0, w - 1)) and j < 2:
            return spots.c(2)
        return None

    parts = [
        Part("body", None, (0, 21, 0), cubes=[Cube(0, 0, -2.5, -1.5, -5, 5, 3, 10, body, glow=body_glow)]),
        Part("head", "body", (0, 0, -5), cubes=[Cube(30, 0, -2.5, -1.5, -4, 5, 3, 4, head, glow=head_glow)]),
        Part("tail", "body", (0, -0.5, 5), cubes=[Cube(0, 13, -1.5, -1, 0, 3, 2, 6, tail, glow=tail_glow)]),
        Part("tail_tip", "tail", (0, 0, 6), cubes=[Cube(18, 13, -1, -0.5, 0, 2, 1, 6, tail, glow=tail_glow)]),
    ]
    for side, x, sign in (("right", -2.5, -1), ("left", 2.5, 1)):
        for end, z in (("front", -3.0), ("hind", 3.0)):
            ux = -3 if sign < 0 else 0
            fx = -3 if sign < 0 else 2
            parts.append(Part("%s_%s_leg" % (side, end), None, (x, 21.5, z),
                              cubes=[Cube(34, 13, ux, -0.5, -1, 3, 1, 2, leg, mirror=sign > 0),
                                     Cube(44, 13, fx, 0.5, -1, 1, 2, 2, leg, mirror=sign > 0)]))

    anim = """        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.5F;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD * 0.3F;
        if (entity.isInSittingPose()) {
            // Lying flat, tail curled around.
            this.tail.yRot = 0.9F;
            this.tailTip.yRot = 1.2F;
            this.rightFrontLeg.yRot = -0.5F;
            this.leftFrontLeg.yRot = 0.5F;
            this.rightHindLeg.yRot = 0.5F;
            this.leftHindLeg.yRot = -0.5F;
            return;
        }
        // A lizard's gait: diagonal legs move together while the body and tail wriggle.
        float walk = limbSwing * 1.2F;
        float swing = Mth.cos(walk) * 0.8F * limbSwingAmount;
        this.rightFrontLeg.yRot = swing;
        this.leftHindLeg.yRot = swing;
        this.leftFrontLeg.yRot = swing;
        this.rightHindLeg.yRot = swing;
        this.body.yRot = Mth.sin(walk) * 0.12F * limbSwingAmount;
        this.tail.yRot = -Mth.sin(walk) * 0.35F * limbSwingAmount + Mth.sin(ageInTicks * 0.1F) * 0.1F;
        this.tailTip.yRot = -Mth.sin(walk - 0.8F) * 0.4F * limbSwingAmount + Mth.sin(ageInTicks * 0.1F - 0.5F) * 0.15F;"""
    return "SalamanderModel", ("salamander", "com.dunesrelics.entity.volcanic.Salamander"), parts, 64, 32, \
        "Fire salamander: a black lizard with glowing ember spots and a long wriggling tail.", anim


# ----------------------------------------------------------------------------------------------- lava crab

def lava_crab():
    shell = Palette("#2c1f1c", "#3a2a26", "#4a3630", "#5c443a")
    seam = Palette("#b8400c", "#e0501c", "#ff7a2a")

    def body(face, i, j, w, h):
        if face == "top" and (j == 1 or i in (1, w - 2)):
            return seam.c(1)
        if face in ("front", "back") and j == 0:
            return seam.c(0)
        return shell.c(1 + (i * 3 + j) % 3)

    def body_glow(face, i, j, w, h):
        if face == "top" and (j == 1 or i in (1, w - 2)):
            return seam.c(2)
        return None

    def claw(face, i, j, w, h):
        if face == "front":
            return seam.c(1) if j == h - 1 else shell.c(3)
        return shell.c(2 + (i + j) % 2)

    eye = lambda face, i, j, w, h: (255, 200, 60) if (face == "top" or j == 0) else shell.c(1)
    eye_glow = lambda face, i, j, w, h: (255, 200, 60) if (face == "top" or j == 0) else None
    leg = lambda face, i, j, w, h: shell.c(1 + (i % 2)) if i % 3 else seam.c(0)

    parts = [
        Part("crab", None, (0, 24, 0), (0, 1.5708, 0)),
        Part("body", "crab", (0, -4, 0), cubes=[Cube(0, 0, -4, -2, -3, 8, 3, 6, body, glow=body_glow)]),
        Part("right_eye", "body", (-1.5, -2, -2.5), cubes=[Cube(28, 0, -0.5, -2, -0.5, 1, 2, 1, eye, glow=eye_glow)]),
        Part("left_eye", "body", (1.5, -2, -2.5), cubes=[Cube(28, 0, -0.5, -2, -0.5, 1, 2, 1, eye, glow=eye_glow)]),
        Part("right_claw", "body", (-3.5, 0, -3), (0, 0.4, 0), [Cube(32, 0, -1.5, -1.5, -3, 3, 3, 3, claw)]),
        Part("left_claw", "body", (3.5, 0, -3), (0, -0.4, 0), [Cube(32, 0, -1.5, -1.5, -3, 3, 3, 3, claw, mirror=True)]),
    ]
    for n, z in (("front", -1.5), ("middle", 0.5), ("hind", 2.5)):
        parts.append(Part("right_%s_leg" % n, "body", (-4, 0.5, z), (0, 0, -0.7), [Cube(0, 9, -4, 0, -0.5, 4, 1, 1, leg)]))
        parts.append(Part("left_%s_leg" % n, "body", (4, 0.5, z), (0, 0, 0.7), [Cube(0, 9, 0, 0, -0.5, 4, 1, 1, leg, mirror=True)]))

    anim = """        // The whole crab is turned sideways, so walking forward is a scuttle.
        float walk = limbSwing * 1.6F;
        float a = Mth.cos(walk) * 0.5F * limbSwingAmount;
        float b = Mth.cos(walk + Mth.PI) * 0.5F * limbSwingAmount;
        this.rightFrontLeg.zRot += a;
        this.rightMiddleLeg.zRot += b;
        this.rightHindLeg.zRot += a;
        this.leftFrontLeg.zRot += b;
        this.leftMiddleLeg.zRot += a;
        this.leftHindLeg.zRot += b;
        this.body.y += Math.abs(Mth.sin(walk)) * 0.3F * limbSwingAmount;
        float snap = Mth.sin(this.attackTime * Mth.PI) * 0.6F + Mth.sin(ageInTicks * 0.1F) * 0.05F;
        this.rightClaw.yRot -= snap;
        this.leftClaw.yRot += snap;
        this.rightClaw.xRot = -snap * 0.5F;
        this.leftClaw.xRot = -snap * 0.5F;
        if (entity.isDancing()) {
            // Claws in the air!
            this.rightClaw.xRot = -1.2F + Mth.sin(ageInTicks * 0.8F) * 0.4F;
            this.leftClaw.xRot = -1.2F - Mth.sin(ageInTicks * 0.8F) * 0.4F;
            this.body.y += Mth.sin(ageInTicks * 0.8F) * 0.5F;
            this.body.zRot = Mth.sin(ageInTicks * 0.4F) * 0.15F;
        }"""
    return "LavaCrabModel", ("lava_crab", "com.dunesrelics.entity.volcanic.LavaCrab"), parts, 64, 32, \
        "Lava crab: a basalt-shelled crab with glowing seams that scuttles sideways.", anim


# ----------------------------------------------------------------------------------------------- magmaling

def magmaling():
    crust, crust_glow = rock(21, pal=Palette("#2a1208", "#3a1a0e", "#4a220f", "#5a2a12", "#6a3214"))

    def head(face, i, j, w, h):
        if face == "front" and j == 3 and i in (1, 2, 4, 5):
            return (255, 236, 150)
        if face == "front" and j == 5 and 2 <= i <= 4:
            return EMBER.c(3)
        return crust(face, i, j, w, h)

    def head_glow(face, i, j, w, h):
        if face == "front" and j == 3 and i in (1, 2, 4, 5):
            return (255, 236, 150)
        if face == "front" and j == 5 and 2 <= i <= 4:
            return EMBER.c(3)
        return crust_glow(face, i, j, w, h)

    parts = [
        Part("body", None, (0, 13, 0), cubes=[Cube(28, 0, -3, 0, -2, 6, 6, 4, crust, glow=crust_glow)]),
        Part("head", None, (0, 13, 0), cubes=[Cube(0, 0, -3.5, -7, -3.5, 7, 7, 7, head, glow=head_glow)]),
        Part("right_arm", None, (-3, 14, 0), cubes=[Cube(48, 0, -2, -1, -1, 2, 6, 2, crust, glow=crust_glow)]),
        Part("left_arm", None, (3, 14, 0), cubes=[Cube(48, 0, 0, -1, -1, 2, 6, 2, crust, mirror=True, glow=crust_glow)]),
        Part("right_leg", None, (-1.5, 19, 0), cubes=[Cube(0, 14, -1, 0, -1, 2, 5, 2, crust, glow=crust_glow)]),
        Part("left_leg", None, (1.5, 19, 0), cubes=[Cube(0, 14, -1, 0, -1, 2, 5, 2, crust, mirror=True, glow=crust_glow)]),
    ]
    anim = """        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        float walk = limbSwing * 0.6662F;
        this.rightLeg.xRot = Mth.cos(walk) * 1.2F * limbSwingAmount;
        this.leftLeg.xRot = -Mth.cos(walk) * 1.2F * limbSwingAmount;
        this.rightArm.xRot = -Mth.cos(walk) * 0.8F * limbSwingAmount;
        this.leftArm.xRot = Mth.cos(walk) * 0.8F * limbSwingAmount;
        this.head.y += Mth.sin(ageInTicks * 0.15F) * 0.4F;
        this.rightArm.zRot = 0.1F + Mth.sin(ageInTicks * 0.1F) * 0.05F;
        this.leftArm.zRot = -0.1F - Mth.sin(ageInTicks * 0.1F) * 0.05F;
        if (entity.isAggressive()) {
            this.rightArm.xRot = -1.9F + Mth.sin(ageInTicks * 0.3F) * 0.2F;
        }"""
    return "MagmalingModel", ("magmaling", "com.dunesrelics.entity.volcanic.Magmaling"), parts, 64, 32, \
        "Magmaling: a small walking lump of magma with a big glowing head.", anim


# ----------------------------------------------------------------------------------------------- cinder wraith

def cinder_wraith():
    def ashen(alpha_top=200, alpha_bottom=120):
        def painter(face, i, j, w, h):
            n = hashed(i, j, len(face), 31)
            base = ASH.c(1 + int(n * 3))
            if n > 0.93:
                return (*EMBER.c(2), 230)
            alpha = alpha_top if face != "bottom" else alpha_bottom
            return (*base, alpha)
        return painter

    def ember_specks(face, i, j, w, h):
        return EMBER.c(3) if hashed(i, j, len(face), 31) > 0.93 else None

    def hood(face, i, j, w, h):
        if face == "front":
            if 1 <= i <= 5 and 2 <= j <= 6:
                if j == 3 and i in (2, 4):
                    return (255, 140, 40, 255)
                return (18, 14, 14, 255)
        return ashen(220)(face, i, j, w, h)

    def hood_glow(face, i, j, w, h):
        if face == "front" and j == 3 and i in (2, 4):
            return (255, 150, 50)
        return ember_specks(face, i, j, w, h)

    parts = [
        Part("head", None, (0, 4, 0), cubes=[Cube(0, 0, -3.5, -7, -3.5, 7, 7, 7, hood, glow=hood_glow)]),
        Part("body", None, (0, 4, 0), cubes=[Cube(28, 0, -3, 0, -2, 6, 8, 4, ashen(200), glow=ember_specks)]),
        Part("robe", "body", (0, 8, 0), cubes=[Cube(0, 14, -2.5, 0, -1.5, 5, 5, 3, ashen(160, 100), glow=ember_specks)]),
        Part("tail", "robe", (0, 5, 0), cubes=[Cube(16, 14, -1.5, 0, -1, 3, 4, 2, ashen(110, 80))]),
        Part("right_arm", None, (-3, 5, 0), cubes=[Cube(48, 0, -2, 0, -1, 2, 7, 2, ashen(190))]),
        Part("left_arm", None, (3, 5, 0), cubes=[Cube(48, 0, 0, 0, -1, 2, 7, 2, ashen(190), mirror=True)]),
    ]
    anim = """        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        float bob = Mth.sin(ageInTicks * 0.1F);
        this.head.y += bob * 0.6F;
        this.body.y += bob * 0.6F;
        this.rightArm.y += bob * 0.6F;
        this.leftArm.y += bob * 0.6F;
        this.robe.xRot = 0.15F + Mth.sin(ageInTicks * 0.13F) * 0.1F;
        this.tail.xRot = 0.25F + Mth.sin(ageInTicks * 0.13F - 0.6F) * 0.2F;
        this.robe.zRot = Mth.sin(ageInTicks * 0.07F) * 0.06F;
        if (entity.isAggressive()) {
            // Reaching out with both arms.
            this.rightArm.xRot = -1.4F + Mth.sin(ageInTicks * 0.3F) * 0.1F;
            this.leftArm.xRot = -1.4F - Mth.sin(ageInTicks * 0.3F) * 0.1F;
        } else {
            this.rightArm.zRot = 0.15F + bob * 0.05F;
            this.leftArm.zRot = -0.15F - bob * 0.05F;
        }"""
    return "CinderWraithModel", ("cinder_wraith", "com.dunesrelics.entity.volcanic.CinderWraith"), parts, 64, 32, \
        "Cinder wraith: a hooded spirit of drifting ash with ember eyes.", anim, True


MODELS = (magma_titan, salamander, lava_crab, magmaling, cinder_wraith)

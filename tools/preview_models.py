"""Renders the entity models in their rest pose to PNGs (a quick visual check, not used by the build)."""
import math
import sys

from PIL import Image, ImageDraw

import entity_models as em


def rot_matrix(rx, ry, rz):
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mul(mul(Rz, Ry), Rx)


def mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def apply(m, v):
    return [sum(m[i][k] * v[k] for k in range(3)) for i in range(3)]


def render(build, out, yaw=0.6, pitch=0.45, scale=10, overrides=None):
    class_name, layer, parts, tw, th, doc, anim = build()
    texture = em.paint(parts, tw, th)
    by_name = {p.name: p for p in parts}
    overrides = overrides or {}
    transforms = {}

    def transform(p):
        if p.name in transforms:
            return transforms[p.name]
        rot = overrides.get(p.name, p.rot)
        local_r = rot_matrix(*rot)
        if p.parent is None:
            R, T = local_r, list(p.pivot)
        else:
            PR, PT = transform(by_name[p.parent])
            T = [a + b for a, b in zip(PT, apply(PR, p.pivot))]
            R = mul(PR, local_r)
        transforms[p.name] = (R, T)
        return R, T

    cam = mul(rot_matrix(pitch, 0, 0), rot_matrix(0, yaw, 0))
    quads = []
    for p in parts:
        R, T = transform(p)
        for c in p.cubes:
            x0, y0, z0 = c.x, c.y, c.z
            x1, y1, z1 = x0 + c.w, y0 + c.h, z0 + c.d
            v = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
            u, vv, w, h, d = c.u, c.v, c.w, c.h, c.d
            f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
            f10, f11, f12 = vv, vv + d, vv + d + h
            polys = [((5, 4, 0, 1), f5, f10, f6, f11), ((2, 3, 7, 6), f6, f11, f7, f10),
                     ((0, 4, 7, 3), f4, f11, f5, f12), ((1, 0, 3, 2), f5, f11, f6, f12),
                     ((5, 1, 2, 6), f6, f11, f8, f12), ((4, 5, 6, 7), f8, f11, f9, f12)]
            world = [apply(cam, [a + b for a, b in zip(apply(R, pt), T)]) for pt in v]
            for idx, u0, v0, u1, v1 in polys:
                c0, c1, c2 = world[idx[0]], world[idx[1]], world[idx[2]]
                nu, nv = int(round(abs(u1 - u0))), int(round(abs(v1 - v0)))
                for j in range(nv):
                    for i in range(nu):
                        s0, s1 = i / nu, (i + 1) / nu
                        t0, t1 = j / nv, (j + 1) / nv
                        tu = u0 + (u1 - u0) * (i + 0.5) / nu
                        tv = v0 + (v1 - v0) * (j + 0.5) / nv
                        tx, ty = int(tu), int(tv)
                        if not (0 <= tx < tw and 0 <= ty < th):
                            continue
                        color = texture.getpixel((tx, ty))
                        if color[3] == 0:
                            continue

                        def pt(s, t):
                            return [c1[k] + s * (c0[k] - c1[k]) + t * (c2[k] - c1[k]) for k in range(3)]

                        corners = [pt(s0, t0), pt(s1, t0), pt(s1, t1), pt(s0, t1)]
                        depth = sum(q[2] for q in corners) / 4
                        quads.append((depth, corners, color))
    quads.sort(key=lambda q: -q[0])
    size = 520
    img = Image.new("RGBA", (size, size), (70, 75, 90, 255))
    draw = ImageDraw.Draw(img)
    for depth, corners, color in quads:
        draw.polygon([(size / 2 - q[0] * scale, size * 0.3 + q[1] * scale) for q in corners], fill=color)
    img.save(out)


if __name__ == "__main__":
    out_dir = sys.argv[1]
    for build in (em.scorpion, em.scarab, em.meerkat, em.vulture):
        name = build.__name__
        render(build, "%s/%s.png" % (out_dir, name), scale=16 if name == "scarab" else 9)
    # the meerkat's lookout pose
    render(em.meerkat, out_dir + "/meerkat_standing.png", overrides={"body": (-1.35, 0, 0), "head": (1.35, 0, 0),
                                                                        "right_front_leg": (1.1, 0, 0), "left_front_leg": (1.1, 0, 0),
                                                                        "tail": (0.6, 0, 0)})

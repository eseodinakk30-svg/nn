"""Small pixel-art helpers shared by the texture generators."""
import random


def hex_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


class Palette:
    """An ordered list of colours, darkest first."""

    def __init__(self, *colors):
        self.colors = [hex_rgb(c) if isinstance(c, str) else tuple(c) for c in colors]

    def c(self, index):
        return self.colors[max(0, min(len(self.colors) - 1, index))]

    def __len__(self):
        return len(self.colors)

    def pick(self, rng):
        return self.colors[rng.randrange(len(self.colors))]


def clamp(v):
    return max(0, min(255, int(round(v))))


def shade(color, factor):
    rgb = tuple(clamp(c * factor) for c in color[:3])
    return rgb + tuple(color[3:])


def mix(a, b, t):
    rgb = tuple(clamp(a[i] * (1 - t) + b[i] * t) for i in range(3))
    return rgb + tuple(a[3:])


def noise_pick(pal, rng, speckle=0.35):
    """Mostly the middle tones of a palette, with occasional darker and lighter speckles."""
    n = len(pal)
    if n == 1:
        return pal.c(0)
    roll = rng.random()
    if roll < speckle / 2:
        return pal.c(0)
    if roll > 1 - speckle / 2:
        return pal.c(n - 1)
    return pal.c(1 + rng.randrange(max(1, n - 2))) if n > 2 else pal.c(rng.randrange(n))

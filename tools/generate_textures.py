"""
Generates all of the mod's textures (PNG images) as pixel art.

You do NOT need to run this to play or build the mod - the finished PNG files are already in
src/main/resources/assets/parakeetsandcapybaras/textures/. It is here so you can see how they were
made, tweak a colour below and regenerate them. You can also simply open the PNGs in any image
editor (for example https://www.blockbench.net or GIMP) and paint over them.

Run it from the project folder with:
    pip install pillow
    python tools/generate_textures.py

How Minecraft maps a box onto a texture ("box UV"): a box that is W wide, H tall and D deep, with
texOffs(u, v), takes up this area of the image:

           u    u+D     u+D+W   u+2D+W  u+2D+2W
    v           [ top  ][bottom]
    v+D  [side ][front ][ side ][ back  ]
    v+D+H
"""
import os
import random
from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "parakeetsandcapybaras", "textures")


def rgb(hex_value):
    return ((hex_value >> 16) & 255, (hex_value >> 8) & 255, hex_value & 255, 255)


def shade(color, amount):
    r, g, b, a = color
    clamp = lambda x: max(0, min(255, int(x)))
    return (clamp(r + amount), clamp(g + amount), clamp(b + amount), a)


class Box:
    """Helper that knows where each face of a model box lives on the texture."""

    def __init__(self, u, v, w, h, d):
        self.u, self.v, self.w, self.h, self.d = u, v, w, h, d

    def faces(self):
        u, v, w, h, d = self.u, self.v, self.w, self.h, self.d
        return {
            "top": (u + d, v, w, d),
            "bottom": (u + d + w, v, w, d),
            "right": (u, v + d, d, h),       # its right-hand column touches the front face
            "front": (u + d, v + d, w, h),
            "left": (u + d + w, v + d, d, h),  # its left-hand column touches the front face
            "back": (u + 2 * d + w, v + d, w, h),
        }


def fill(img, rect, color_fn):
    """color_fn(x, y, width, height) -> colour, where x/y are positions inside the face."""
    x0, y0, w, h = rect
    for y in range(h):
        for x in range(w):
            img.putpixel((x0 + x, y0 + y), color_fn(x, y, w, h))


# ---------------------------------------------------------------------------------------------
# Parakeets (uses the vanilla parrot model, 32x32 texture)
# ---------------------------------------------------------------------------------------------
PARAKEET_COLOURS = {
    #            body      dark      face      bars      cheek     tail      eye       cere
    "green":    (0x63C132, 0x4E9E28, 0xF4E04D, 0x1E1E1E, 0x5B4FCF, 0x2C4F9E, 0x111111, 0x5578D8),
    "sky_blue": (0x6EC3EE, 0x4FA2D3, 0xF5F5F5, 0x1E1E1E, 0x5B4FCF, 0x27428F, 0x111111, 0x5578D8),
    "yellow":   (0xF6E24B, 0xE2CB35, 0xFFF6A8, 0xE8D46A, 0xF4F1E6, 0xF2E7A0, 0xC0303A, 0xE8B9B0),
    "white":    (0xF6F6F2, 0xE0E0DA, 0xFFFFFF, 0xE4E4DE, 0xF0F0F0, 0xEDEDE8, 0xC0303A, 0xE8C0B8),
    "violet":   (0x8B6FD6, 0x7055BB, 0xF5F5F5, 0x1E1E1E, 0x5B4FCF, 0x47358F, 0x111111, 0x5578D8),
    "grey":     (0xA2A6AB, 0x878B91, 0xF2F2F2, 0x1E1E1E, 0x6F7480, 0x4E535C, 0x111111, 0x8A6A5A),
}
BEAK = rgb(0xE7C98A)
LEGS = rgb(0xA89A9E)


def make_parakeet(name, colours):
    body, dark, face, bars, cheek, tail, eye, cere = (rgb(c) for c in colours)
    rnd = random.Random(name)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

    def body_px(x, y, w, h):
        return shade(body, rnd.randint(-8, 8))

    def barred(x, y, w, h):
        # the thin black wavy stripes on the back of a budgie's head and wings
        return bars if y % 2 == 0 else face

    # Head (2,2) 2x3x2
    head = Box(2, 2, 2, 3, 2).faces()
    fill(img, head["top"], lambda x, y, w, h: face if y == h - 1 else barred(x, y, w, h))
    fill(img, head["bottom"], lambda *a: face)
    fill(img, head["front"], lambda *a: face)
    fill(img, head["back"], lambda x, y, w, h: bars if y % 2 == 0 else face)
    side = [[None, "eye"], ["bar", "face"], ["bar", "cheek"]]
    pick = {"eye": eye, "face": face, "bar": bars, "cheek": cheek}
    fill(img, head["right"], lambda x, y, w, h: pick[side[y][x]] if side[y][x] else barred(x, y, w, h))
    fill(img, head["left"], lambda x, y, w, h: pick[side[y][1 - x]] if side[y][1 - x] else barred(x, y, w, h))

    # Crown / forehead (10,0) 2x1x4
    crown = Box(10, 0, 2, 1, 4).faces()
    fill(img, crown["top"], lambda x, y, w, h: face if y >= 2 else barred(x, y, w, h))
    fill(img, crown["bottom"], lambda *a: face)
    fill(img, crown["front"], lambda *a: face)
    fill(img, crown["back"], lambda *a: bars)
    fill(img, crown["right"], lambda x, y, w, h: bars if x == 0 else face)
    fill(img, crown["left"], lambda x, y, w, h: bars if x == w - 1 else face)

    # Beak: lower part (11,7) and upper part with the cere (16,7)
    for f in Box(11, 7, 1, 2, 1).faces().values():
        fill(img, f, lambda *a: BEAK)
    for key, f in Box(16, 7, 1, 2, 1).faces().items():
        fill(img, f, lambda x, y, w, h, key=key: cere if (key == "top" or y == 0) else BEAK)

    # Body (2,8) 3x6x3: chest at the front, barred shoulders at the back
    b = Box(2, 8, 3, 6, 3).faces()
    fill(img, b["top"], lambda x, y, w, h: barred(x, y, w, h))
    fill(img, b["bottom"], lambda *a: shade(dark, rnd.randint(-6, 6)))
    fill(img, b["front"], body_px)
    fill(img, b["back"], lambda x, y, w, h: barred(x, y, w, h) if y < 2 else body_px(x, y, w, h))
    fill(img, b["right"], lambda x, y, w, h: barred(x, y, w, h) if y < 2 else body_px(x, y, w, h))
    fill(img, b["left"], lambda x, y, w, h: barred(x, y, w, h) if y < 2 else body_px(x, y, w, h))

    # Wings (19,8) 1x5x3: scalloped feathers with darker flight-feather tips
    wing = Box(19, 8, 1, 5, 3).faces()

    def wing_px(x, y, w, h):
        if y == h - 1:
            return shade(bars, 20) if bars[0] < 100 else shade(dark, -10)
        # rows of feathers: a light feather edged with a dark stripe, like real budgie wings
        if y % 2 == 1:
            return bars if x != 1 else shade(bars, 40) if bars[0] < 100 else bars
        return face if x != 2 else body

    fill(img, wing["right"], wing_px)
    fill(img, wing["left"], wing_px)
    for key in ("top", "bottom", "front", "back"):
        fill(img, wing[key], lambda *a: dark)

    # Tail (22,1) 3x4x1
    t = Box(22, 1, 3, 4, 1).faces()
    for f in t.values():
        fill(img, f, lambda x, y, w, h: shade(tail, -25) if y == h - 1 and h > 1 else shade(tail, rnd.randint(-6, 6)))

    # Legs (14,18) 1x2x1
    for f in Box(14, 18, 1, 2, 1).faces().values():
        fill(img, f, lambda *a: LEGS)

    # The parrot model's big head-feather (2,18) is left see-through: budgies don't have one.

    out = os.path.join(ROOT, "entity", "parakeet", f"parakeet_{name}.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)


# ---------------------------------------------------------------------------------------------
# Capybara (CapybaraModel, 64x64 texture)
# ---------------------------------------------------------------------------------------------
def make_capybara():
    rnd = random.Random("capybara")
    fur = rgb(0x9A6A3E)
    back = rgb(0x84592F)
    belly = rgb(0xB28657)
    face = rgb(0x8E6038)
    nose = rgb(0x4A3427)
    nostril = rgb(0x1E1410)
    eye = rgb(0x15100C)
    ear = rgb(0x5E3E26)
    ear_inner = rgb(0x42291A)
    foot = rgb(0x4A3424)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    def furry(base):
        def px(x, y, w, h):
            r = rnd.random()
            if r < 0.08:
                return shade(base, -22)  # dark hair streak
            if r < 0.14:
                return shade(base, 14)   # light hair
            return shade(base, rnd.randint(-6, 6))
        return px

    # Body (0,0) 10x9x16
    body = Box(0, 0, 10, 9, 16).faces()
    fill(img, body["top"], furry(back))
    fill(img, body["bottom"], furry(belly))
    for key in ("right", "left"):
        fill(img, body[key], lambda x, y, w, h: furry(back)(x, y, w, h) if y < 2 else (furry(belly)(x, y, w, h) if y >= h - 2 else furry(fur)(x, y, w, h)))
    fill(img, body["front"], furry(fur))
    fill(img, body["back"], furry(fur))

    # Head (0,25) 7x7x8 - eyes high up on the sides
    head = Box(0, 25, 7, 7, 8).faces()
    for key in ("top", "bottom", "front", "back", "right", "left"):
        fill(img, head[key], furry(face))
    hx, hy, hw, hh = head["right"]
    img.putpixel((hx + hw - 4, hy + 1), eye)
    hx, hy, hw, hh = head["left"]
    img.putpixel((hx + 3, hy + 1), eye)

    # Nose (32,25) 5x4x2 - big dark blunt snout with nostrils
    n = Box(32, 25, 5, 4, 2).faces()
    for key, f in n.items():
        fill(img, f, lambda x, y, w, h: shade(nose, rnd.randint(-5, 5)))
    nx, ny, nw, nh = n["front"]
    img.putpixel((nx + 1, ny), nostril)
    img.putpixel((nx + 3, ny), nostril)
    for x in range(nw):
        img.putpixel((nx + x, ny + nh - 1), shade(nose, -18))  # mouth line

    # Ears (32,31) and (38,31) 2x2x1
    for u in (32, 38):
        e = Box(u, 31, 2, 2, 1).faces()
        for key, f in e.items():
            fill(img, f, lambda x, y, w, h, key=key: ear_inner if key == "front" else ear)

    # Legs (0,41) 3x5x3 - darker feet at the bottom
    leg = Box(0, 41, 3, 5, 3).faces()
    for key, f in leg.items():
        fill(img, f, lambda x, y, w, h, key=key: foot if key == "bottom" or (key not in ("top",) and y >= h - 1) else furry(back)(x, y, w, h))

    out = os.path.join(ROOT, "entity", "capybara", "capybara.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)


# ---------------------------------------------------------------------------------------------
# Bird cages (16x16 block textures)
#   rows 0-11: metal for bars and plates, rows 12-13: wooden perch, rows 14-15: darker base metal
# ---------------------------------------------------------------------------------------------
CAGE_METALS = {
    #        light     mid       dark      darkest
    "gold": (0xFFF2A0, 0xF2C94C, 0xC8961E, 0x8C6410),
    "iron": (0xF4F4F4, 0xCACACA, 0x9C9C9C, 0x6A6A6A),
}


def make_cage(name, colours):
    light, mid, dark, darkest = (rgb(c) for c in colours)
    wood, wood_dark = rgb(0x9C7A48), rgb(0x6E5230)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            if y < 12:
                if x == 7:
                    c = light                      # shiny bar
                elif x in (0, 15) or y in (0, 11):
                    c = dark                       # plate edges
                else:
                    c = mid if (x + y) % 5 else light
            elif y < 14:
                c = wood if (x + y) % 4 else wood_dark
            else:
                c = darkest if x in (0, 15) or y == 15 else dark
            img.putpixel((x, y), c)
    out = os.path.join(ROOT, "block", f"{name}_bird_cage.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)


if __name__ == "__main__":
    for variant, colours in PARAKEET_COLOURS.items():
        make_parakeet(variant, colours)
    make_capybara()
    for metal, colours in CAGE_METALS.items():
        make_cage(metal, colours)
    print("Textures written to", os.path.abspath(ROOT))

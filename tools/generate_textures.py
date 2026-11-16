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
#
# Style: chunky budgie look - a solid body colour, a checkered face, a checkered "scalloped"
# wing pattern, a black eye with a dark cheek patch, a blue band (the cere) above a tan beak and
# grey legs.
# ---------------------------------------------------------------------------------------------
PARAKEET_COLOURS = {
    #            body      body_dark face      face_dark wing_lt   wing_dk   cheek     eye       cere
    "green":    (0x2FA03C, 0x1E7A2A, 0xC6C55C, 0x9A9636, 0xC9C760, 0x8A8235, 0x4D4C7A, 0x2A2A2A, 0x3A44A0),
    "sky_blue": (0x72B4C4, 0x4E8E9C, 0xE6E6E6, 0xB4B4B4, 0xD0D0D0, 0x7C7C7C, 0x25255A, 0x1A1A1A, 0x4A58B5),
    "yellow":   (0xE8D84A, 0xC9B630, 0xF4EE9A, 0xE0D670, 0xF2EA8C, 0xD2C452, 0xF2EEDC, 0xB0303A, 0xD8A8A0),
    "white":    (0xF0F0EC, 0xD2D2CC, 0xFFFFFF, 0xE2E2DE, 0xF4F4F0, 0xCDCDC8, 0xE8E8E8, 0xB0303A, 0xD8B4AC),
    "violet":   (0x8466C8, 0x634AA4, 0xE6E6E6, 0xB4B4B4, 0xD0D0D0, 0x7C7C7C, 0x4A3A8A, 0x1A1A1A, 0x4A58B5),
    "grey":     (0x9A9EA4, 0x767A80, 0xE6E6E6, 0xB4B4B4, 0xC8C8C8, 0x6E6E6E, 0x50545C, 0x1A1A1A, 0x6A5A88),
}
BEAK = rgb(0xA4994F)
BEAK_DARK = rgb(0x857A38)
LEGS = rgb(0x9A949C)


def make_parakeet(name, colours):
    body, body_dark, face, face_dark, wing_lt, wing_dk, cheek, eye, cere = (rgb(c) for c in colours)
    img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))

    def checker(light, dark):
        return lambda x, y, w, h: light if (x + y) % 2 == 0 else dark

    def body_shaded(x, y, w, h):
        # lighter at the top of the body, darker towards the bottom
        return shade(body, 10 - int(20 * y / max(1, h - 1)))

    # Head (2,2) 2x3x2: checkered face, eye in the middle, dark cheek patch at the front-bottom
    head = Box(2, 2, 2, 3, 2).faces()
    fill(img, head["top"], checker(face, face_dark))
    fill(img, head["bottom"], lambda *a: face)
    fill(img, head["front"], lambda *a: face)
    fill(img, head["back"], checker(face_dark, face))
    side = [["dark", "face"], ["eye", "face"], ["face", "cheek"]]  # [row][0 = back, 1 = front]
    pick = {"eye": eye, "face": face, "dark": face_dark, "cheek": cheek}
    fill(img, head["right"], lambda x, y, w, h: pick[side[y][x]])
    fill(img, head["left"], lambda x, y, w, h: pick[side[y][1 - x]])

    # Crown / forehead (10,0) 2x1x4
    crown = Box(10, 0, 2, 1, 4).faces()
    fill(img, crown["top"], checker(face, face_dark))
    fill(img, crown["bottom"], lambda *a: face)
    fill(img, crown["front"], lambda *a: face)
    fill(img, crown["back"], lambda *a: face_dark)
    fill(img, crown["right"], checker(face_dark, face))
    fill(img, crown["left"], checker(face, face_dark))

    # Beak: lower part (11,7), and the upper part (16,7) whose top is the blue cere band
    for f in Box(11, 7, 1, 2, 1).faces().values():
        fill(img, f, lambda x, y, w, h: BEAK_DARK if y == h - 1 and h > 1 else BEAK)
    for key, f in Box(16, 7, 1, 2, 1).faces().items():
        fill(img, f, lambda x, y, w, h, key=key: cere if (key == "top" or y == 0) else BEAK)

    # Body (2,8) 3x6x3: one solid colour with soft shading
    b = Box(2, 8, 3, 6, 3).faces()
    fill(img, b["top"], lambda *a: shade(body, 12))
    fill(img, b["bottom"], lambda *a: body_dark)
    for key in ("front", "right", "left"):
        fill(img, b[key], body_shaded)
    fill(img, b["back"], lambda x, y, w, h: shade(body_dark, 6 - int(12 * y / (h - 1))))

    # Wings (19,8) 1x5x3: big checkered "scalloped" pattern
    wing = Box(19, 8, 1, 5, 3).faces()
    fill(img, wing["right"], checker(wing_lt, wing_dk))
    fill(img, wing["left"], checker(wing_lt, wing_dk))
    for key in ("top", "bottom", "front", "back"):
        fill(img, wing[key], checker(wing_dk, wing_lt))

    # Tail (22,1) 3x4x1: dark body colour
    for f in Box(22, 1, 3, 4, 1).faces().values():
        fill(img, f, lambda x, y, w, h: shade(body_dark, -12) if y == h - 1 and h > 1 else body_dark)

    # Legs (14,18) 1x2x1
    for f in Box(14, 18, 1, 2, 1).faces().values():
        fill(img, f, lambda *a: LEGS)

    # The parrot model's big head-feather (2,18) is left see-through: budgies don't have one.

    out = os.path.join(ROOT, "entity", "parakeet", f"parakeet_{name}.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out)


# ---------------------------------------------------------------------------------------------
# Capybara (CapybaraModel, 64x64 texture)
#
# Style: dark, blotchy brown fur, a slightly lighter back, a muzzle that fades to almost black
# at the front, dark ears and dark feet.
# ---------------------------------------------------------------------------------------------
def make_capybara():
    rnd = random.Random("capybara")
    fur = rgb(0x573D27)
    back = rgb(0x634630)
    belly = rgb(0x45301E)
    muzzle = rgb(0x2A1C13)
    nose = rgb(0x1C140E)
    nostril = rgb(0x0D0805)
    eye = rgb(0x120908)
    ear = rgb(0x2E241B)
    ear_inner = rgb(0x1E1712)
    foot = rgb(0x28201A)
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    # A blotch map: big soft patches of lighter and darker fur, like the reference picture.
    blotch = {}

    def blotchy(base, strength=9):
        def px(x, y, w, h):
            key = (id(base), x // 2, y // 2, w, h)
            if key not in blotch:
                blotch[key] = rnd.randint(-strength, strength)
            return shade(base, blotch[key] + rnd.randint(-4, 4))
        return px

    # Body (0,0) 10x9x16
    body = Box(0, 0, 10, 9, 16).faces()
    fill(img, body["top"], blotchy(back))
    fill(img, body["bottom"], blotchy(belly, 6))
    for key in ("right", "left"):
        fill(img, body[key], lambda x, y, w, h: blotchy(back)(x, y, w, h) if y < 2 else blotchy(fur)(x, y, w, h))
    fill(img, body["front"], blotchy(fur))
    fill(img, body["back"], blotchy(fur))

    # Head (0,25) 7x7x8. The sides get darker towards the front (the muzzle).
    head = Box(0, 25, 7, 7, 8).faces()

    def side_px(front_is_right):
        def px(x, y, w, h):
            dist = (w - 1 - x) if front_is_right else x       # 0 = at the front
            base = muzzle if dist == 0 else (shade(fur, -14) if dist <= 2 else fur)
            return blotchy(base, 10)(x, y, w, h)
        return px

    fill(img, head["right"], side_px(True))
    fill(img, head["left"], side_px(False))
    fill(img, head["top"], lambda x, y, w, h: blotchy(muzzle if y == h - 1 else back, 10)(x, y, w, h))
    fill(img, head["bottom"], blotchy(shade(fur, -10), 6))
    fill(img, head["front"], blotchy(muzzle, 6))
    fill(img, head["back"], blotchy(fur, 10))
    hx, hy, hw, hh = head["right"]
    img.putpixel((hx + hw - 4, hy + 1), eye)
    img.putpixel((hx + hw - 3, hy + 1), eye)
    hx, hy, hw, hh = head["left"]
    img.putpixel((hx + 3, hy + 1), eye)
    img.putpixel((hx + 2, hy + 1), eye)

    # Nose (32,25) 5x4x2: the blunt, nearly black snout with nostrils
    n = Box(32, 25, 5, 4, 2).faces()
    for key, f in n.items():
        fill(img, f, lambda x, y, w, h: shade(nose, rnd.randint(-3, 5)))
    nx, ny, nw, nh = n["front"]
    img.putpixel((nx + 1, ny + 1), nostril)
    img.putpixel((nx + 3, ny + 1), nostril)

    # Ears (32,31) and (38,31) 2x2x1
    for u in (32, 38):
        for key, f in Box(u, 31, 2, 2, 1).faces().items():
            fill(img, f, lambda x, y, w, h, key=key: ear_inner if key == "front" else shade(ear, rnd.randint(-4, 4)))

    # Legs (0,41) 3x5x3: fur at the top, dark feet at the bottom
    leg = Box(0, 41, 3, 5, 3).faces()
    for key, f in leg.items():
        def leg_px(x, y, w, h, key=key):
            if key == "bottom" or y >= h - 2:
                return shade(foot, rnd.randint(-3, 3))
            if key == "top":
                return blotchy(fur, 8)(x, y, w, h)
            return blotchy(shade(fur, -10), 8)(x, y, w, h)
        fill(img, f, leg_px)

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

"""
Generates the capybara and bird cage textures (PNG images) as pixel art.

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


# Parakeet textures are NOT made here: they come from the budgie Blockbench models, see
# tools/convert_budgie_models.py.


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
            base = shade(fur, -16) if dist <= 1 else (shade(fur, -8) if dist <= 3 else fur)
            return blotchy(base, 10)(x, y, w, h)
        return px

    fill(img, head["right"], side_px(True))
    fill(img, head["left"], side_px(False))
    fill(img, head["top"], lambda x, y, w, h: blotchy(shade(fur, -12) if y == h - 1 else back, 10)(x, y, w, h))
    fill(img, head["bottom"], blotchy(shade(fur, -10), 6))
    fill(img, head["front"], blotchy(muzzle, 6))
    fill(img, head["back"], blotchy(fur, 10))
    hx, hy, hw, hh = head["right"]
    img.putpixel((hx + hw - 4, hy + 1), eye)
    img.putpixel((hx + hw - 3, hy + 1), eye)
    hx, hy, hw, hh = head["left"]
    img.putpixel((hx + 3, hy + 1), eye)
    img.putpixel((hx + 2, hy + 1), eye)

    # Snout (32,25) 5x5x4: long and blunt, getting darker towards the tip, with nostrils
    n = Box(32, 25, 5, 5, 4).faces()

    def snout_side(front_is_right):
        def px(x, y, w, h):
            dist = (w - 1 - x) if front_is_right else x   # 0 = at the tip
            base = nose if dist == 0 else (muzzle if dist == 1 else shade(fur, -16))
            return shade(base, rnd.randint(-3, 4))
        return px

    fill(img, n["right"], snout_side(True))
    fill(img, n["left"], snout_side(False))
    fill(img, n["top"], lambda x, y, w, h: shade(nose if y == h - 1 else (muzzle if y == h - 2 else shade(fur, -16)), rnd.randint(-3, 4)))
    fill(img, n["bottom"], lambda *a: shade(muzzle, rnd.randint(-3, 3)))
    fill(img, n["front"], lambda *a: shade(nose, rnd.randint(-3, 4)))
    fill(img, n["back"], lambda *a: muzzle)
    nx, ny, nw, nh = n["front"]
    img.putpixel((nx + 1, ny + 1), nostril)
    img.putpixel((nx + 3, ny + 1), nostril)
    for x in range(1, nw - 1):
        img.putpixel((nx + x, ny + nh - 2), shade(nose, -8))  # mouth line

    # Ears (52,25) and (52,29) 2x2x1
    for v in (25, 29):
        for key, f in Box(52, v, 2, 2, 1).faces().items():
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
    make_capybara()
    for metal, colours in CAGE_METALS.items():
        make_cage(metal, colours)
    print("Textures written to", os.path.abspath(ROOT))

"""
Generates the bird cage textures (PNG images) as pixel art.

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


# The capybara texture is NOT made here: it comes from the capybara Blockbench model, see
# tools/convert_capybara_model.py.


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
    for metal, colours in CAGE_METALS.items():
        make_cage(metal, colours)
    print("Textures written to", os.path.abspath(ROOT))

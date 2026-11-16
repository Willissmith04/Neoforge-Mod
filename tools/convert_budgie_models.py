"""
Converts the budgie Blockbench models (nm_budgie_*.bbmodel) into files the mod can use.

    python tools/convert_budgie_models.py <folder with the .bbmodel files>

It writes:
  * src/main/java/.../client/model/ParakeetGeometry.java
        the 3D model (bones and cubes). All colours share this model.
  * assets/parakeetsandcapybaras/textures/entity/parakeet/parakeet_<colour>.png
        one texture per colour, repacked into Minecraft's "box UV" layout at 4x resolution
        (so the small 0.25-pixel parts of the model stay sharp).
  * assets/parakeetsandcapybaras/neoforge/animations/entity/parakeet_<name>.json
        the animations (idle, walk, bite, dance, preen, hurt_1-3, death).

The shared conversion code is in bbmodel_converter.py.
"""
import glob
import os
import re
import sys

import bbmodel_converter as bb

TEX_UNITS = 64      # size of the texture as the model sees it (64 x 64 units)
SCALE = 4           # pixels per unit in the PNG (so the PNG is 256 x 256)

# Blockbench bone name -> name used in the mod (the root bone is called nm_budgie_<colour>)
BONE_NAMES = {"h_head": "head", "rightLeg": "right_leg", "leftLeg": "left_leg",
              "rightWing": "right_wing", "leftWing": "left_wing", "tailWing": "tail"}
SKIP_BONES = {"tag_name", "l_leash", "hitbox"}  # ModelEngine helper bones, not part of the bird


def bone_name(name):
    return "body" if name.startswith("nm_budgie") else BONE_NAMES.get(name, name)


def main():
    folder = sys.argv[1] if len(sys.argv) > 1 else "."
    files = sorted(glob.glob(os.path.join(folder, "*nm_budgie_*.bbmodel")))
    if not files:
        sys.exit("No nm_budgie_*.bbmodel files found in " + folder)

    first = bb.load(files[0])
    root = bb.build_parts(first, bone_name, SKIP_BONES)
    cubes = list(bb.all_cubes(root))
    spots = bb.pack(cubes, TEX_UNITS)
    bb.write_java(root, spots, "ParakeetGeometry",
                  "The budgie's 3D shape: bones (body, head, legs, wings, tail) and their cubes.",
                  "convert_budgie_models.py from the nm_budgie .bbmodel files", TEX_UNITS)
    bb.write_animations(first, "parakeet", bone_name, SKIP_BONES)

    tex_dir = os.path.join(bb.ASSETS, "textures", "entity", "parakeet")
    os.makedirs(tex_dir, exist_ok=True)
    for path in files:
        colour = re.search(r"nm_budgie_(.+)\.bbmodel$", os.path.basename(path)).group(1)
        model = bb.load(path)
        # every file has the same cubes in the same order; match them up by position in the list
        remap = dict(zip((e["uuid"] for e in first["elements"]), (e["uuid"] for e in model["elements"])))
        local_spots = {remap[k]: v for k, v in spots.items()}
        local_cubes = [{"uuid": remap[c["uuid"]]} for c in cubes]
        bb.write_texture(model, local_cubes, local_spots, os.path.join(tex_dir, f"parakeet_{colour}.png"), TEX_UNITS, SCALE)
        print("wrote", colour)
    print("Done:", len(cubes), "cubes,", len(first["animations"]), "animations")


if __name__ == "__main__":
    main()

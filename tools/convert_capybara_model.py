"""
Converts the capybara Blockbench model (nm_capybara.bbmodel) into files the mod can use.

    python tools/convert_capybara_model.py <path to nm_capybara.bbmodel>

It writes:
  * src/main/java/.../client/model/CapybaraGeometry.java      the 3D model (bones and cubes)
  * assets/parakeetsandcapybaras/textures/entity/capybara/capybara.png
        the texture, repacked into Minecraft's "box UV" layout
  * assets/parakeetsandcapybaras/neoforge/animations/entity/capybara_<name>.json
        the animations (idle, walk, sniff, death)

The shared conversion code is in bbmodel_converter.py.
"""
import os
import sys

import bbmodel_converter as bb

TEX_UNITS = 64   # texture size as the model sees it
SCALE = 1        # the capybara only uses whole pixels, so keep the original resolution

BONE_NAMES = {"h_head": "head", "leftArm": "left_front_leg", "rightArm": "right_front_leg",
              "leftLeg": "left_hind_leg", "rightLeg": "right_hind_leg"}
SKIP_BONES = {"tag_name", "hitbox"}  # ModelEngine helper bones, not part of the capybara


def bone_name(name):
    return BONE_NAMES.get(name, name)


def main():
    if len(sys.argv) < 2:
        sys.exit("Usage: python tools/convert_capybara_model.py <path to nm_capybara.bbmodel>")
    model = bb.load(sys.argv[1])
    root = bb.build_parts(model, bone_name, SKIP_BONES)
    cubes = list(bb.all_cubes(root))
    spots = bb.pack(cubes, TEX_UNITS)
    bb.write_java(root, spots, "CapybaraGeometry",
                  "The capybara's 3D shape: bones (base, torso, head, tail, four legs) and their cubes.",
                  "convert_capybara_model.py from nm_capybara.bbmodel", TEX_UNITS)
    bb.write_animations(model, "capybara", bone_name, SKIP_BONES)
    tex_dir = os.path.join(bb.ASSETS, "textures", "entity", "capybara")
    os.makedirs(tex_dir, exist_ok=True)
    bb.write_texture(model, cubes, spots, os.path.join(tex_dir, "capybara.png"), TEX_UNITS, SCALE)
    print("Done:", len(cubes), "cubes,", len(model["animations"]), "animations")


if __name__ == "__main__":
    main()

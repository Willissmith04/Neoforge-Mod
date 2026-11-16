"""
Converts the budgie Blockbench models (nm_budgie_*.bbmodel) into files the mod can use.

    python tools/convert_budgie_models.py <folder with the .bbmodel files>

It writes:
  * src/main/java/.../client/model/ParakeetGeometry.java
        the 3D model (bones and cubes) as Minecraft model code. All colours share this model.
  * src/main/resources/assets/parakeetsandcapybaras/textures/entity/parakeet/parakeet_<colour>.png
        one texture per colour, repacked into Minecraft's "box UV" layout (see below).
  * src/main/resources/assets/parakeetsandcapybaras/neoforge/animations/entity/parakeet_<name>.json
        the animations (idle, walk, bite, dance, preen, hurt_1-3, death) in NeoForge's
        JSON animation format.

Why the textures are repacked: the Blockbench models give every face of every cube its own
spot on the texture ("per-face UV"). Minecraft's entity models instead expect each cube's six
faces laid out next to each other in a fixed pattern ("box UV"). So for every pixel of every
face in the new layout, this script works out which point of the cube it belongs to and copies
the colour that the Blockbench model shows at that point. The new textures are 4x higher
resolution so that the small 0.25-pixel parts of the model stay sharp.

Coordinates: Blockbench uses y-up with the ground at y = 0. Minecraft entity models use y-down
with the ground at y = 24, and are mirrored left-right. So x -> -x and y -> 24 - y, and
rotations around x and y change sign.
"""
import base64
import glob
import io
import json
import math
import os
import re
import sys

from PIL import Image

PROJECT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(PROJECT, "src", "main", "resources", "assets", "parakeetsandcapybaras")
JAVA_OUT = os.path.join(PROJECT, "src", "main", "java", "com", "parakeetsandcapybaras", "client", "model", "ParakeetGeometry.java")

TEX_UNITS = 64      # size of the texture as the model sees it (64 x 64 units)
SCALE = 4           # pixels per unit in the PNG (so the PNG is 256 x 256)

# Blockbench bone name -> name used in the mod (the root bone is called nm_budgie_<colour>)
BONE_NAMES = {"h_head": "head", "rightLeg": "right_leg", "leftLeg": "left_leg",
              "rightWing": "right_wing", "leftWing": "left_wing", "tailWing": "tail"}
SKIP_BONES = {"tag_name", "l_leash", "hitbox"}  # ModelEngine helper bones, not part of the bird


def bone_name(name):
    return "body" if name.startswith("nm_budgie") else BONE_NAMES.get(name, name)


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


# --------------------------------------------------------------------------------------------
# Geometry
# --------------------------------------------------------------------------------------------
def to_model(p, origin):
    """A Blockbench point, relative to a pivot, in Minecraft model space."""
    return (-(p[0] - origin[0]), -(p[1] - origin[1]), p[2] - origin[2])


def build_parts(model):
    """Returns a tree of parts: {name, pivot(bb), rotation, cubes:[elements], children:[...]}."""
    elements = {e["uuid"]: e for e in model["elements"]}

    def group(node, parent_origin):
        part = {"name": bone_name(node["name"]), "origin": node["origin"], "parent_origin": parent_origin,
                "rotation": node.get("rotation") or [0, 0, 0], "cubes": [], "children": []}
        rotated = 0
        for child in node["children"]:
            if isinstance(child, str):
                e = elements[child]
                rot = e.get("rotation") or [0, 0, 0]
                if any(abs(r) > 1e-6 for r in rot):
                    # A rotated cube becomes its own little part that rotates around the cube's origin.
                    part["children"].append({"name": f"{part['name']}_piece{rotated}", "origin": e["origin"],
                                             "parent_origin": node["origin"], "rotation": rot,
                                             "cubes": [e], "children": []})
                    rotated += 1
                else:
                    part["cubes"].append(e)
            elif child["name"] not in SKIP_BONES:
                part["children"].append(group(child, node["origin"]))
        return part

    roots = [o for o in model["outliner"] if not isinstance(o, str)]
    assert len(roots) == 1
    return group(roots[0], None)


def all_cubes(part):
    yield from part["cubes"]
    for c in part["children"]:
        yield from all_cubes(c)


def size_of(e):
    return [round(e["to"][i] - e["from"][i], 4) for i in range(3)]


def pack(cubes):
    """Shelf-packs every cube's box-UV area (2*(d+w) wide, d+h tall). Returns {uuid: (u, v)}."""
    spots, x, y, row = {}, 0, 0, 0
    for e in sorted(cubes, key=lambda e: -(size_of(e)[2] + size_of(e)[1])):
        w, h, d = size_of(e)
        bw, bh = math.ceil(2 * (d + w)), math.ceil(d + h)
        if x + bw > TEX_UNITS:
            x, y, row = 0, y + row, 0
        spots[e["uuid"]] = (x, y)
        x += bw
        row = max(row, bh)
    assert y + row <= TEX_UNITS, "textures do not fit"
    return spots


def fmt(v):
    v = round(v, 4) + 0.0
    return f"{v:g}F"


def java_part(part, spots, parent_var, lines, indent="        "):
    if part["parent_origin"] is None:
        o = part["origin"]
        pivot = (-o[0], 24 - o[1], o[2])
    else:
        pivot = to_model(part["origin"], part["parent_origin"])
    rx, ry, rz = part["rotation"]
    rot = (-math.radians(rx), -math.radians(ry), math.radians(rz))
    var = part["name"].replace("-", "_")
    cubes = ""
    for e in part["cubes"]:
        w, h, d = size_of(e)
        mn = (-(e["to"][0] - part["origin"][0]), -(e["to"][1] - part["origin"][1]), e["from"][2] - part["origin"][2])
        u, v = spots[e["uuid"]]
        cubes += f".texOffs({u}, {v}).addBox({fmt(mn[0])}, {fmt(mn[1])}, {fmt(mn[2])}, {fmt(w)}, {fmt(h)}, {fmt(d)})"
    pose = (f"PartPose.offsetAndRotation({', '.join(fmt(p) for p in pivot)}, {', '.join(fmt(r) for r in rot)})"
            if any(abs(r) > 1e-9 for r in rot) else f"PartPose.offset({', '.join(fmt(p) for p in pivot)})")
    lines.append(f'{indent}PartDefinition {var} = {parent_var}.addOrReplaceChild("{part["name"]}", '
                 f'CubeListBuilder.create(){cubes}, {pose});')
    for child in part["children"]:
        java_part(child, spots, var, lines, indent)


def write_java(root, spots):
    lines = []
    java_part(root, spots, "root", lines)
    body = "\n".join(lines)
    code = f'''package com.parakeetsandcapybaras.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * The budgie's 3D shape: bones (body, head, legs, wings, tail) and their cubes.
 *
 * <p>GENERATED by tools/convert_budgie_models.py from the nm_budgie .bbmodel files. Don't edit
 * this by hand: change the model in Blockbench and run the script again.</p>
 */
@SuppressWarnings("unused")
public final class ParakeetGeometry {{
    private ParakeetGeometry() {{
    }}

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, {TEX_UNITS}, {TEX_UNITS});
    }}
}}
'''
    with open(JAVA_OUT, "w", encoding="utf-8") as f:
        f.write(code)


# --------------------------------------------------------------------------------------------
# Textures
# --------------------------------------------------------------------------------------------
# For each Minecraft face: (corner vertex for (u1,v1), vertex for (u2,v1), vertex for (u1,v2)),
# copied from ModelPart.Cube. Vertices are picked from the cube corners by (x, y, z) = min/max.
MC_FACES = {
    # direction: (region in box UV as offsets from texOffs: u1, v1, u2, v2),
    #            (cube corner at (u1, v1), corner at (u2, v1), corner at (u1, v2))
    # N = the minimum of that axis in model space, M = the maximum.
    "DOWN": (lambda w, h, d: (d, 0, d + w, d), ("xN yN zM", "xM yN zM", "xN yN zN")),
    "UP": (lambda w, h, d: (d + w, d, d + w + w, 0), ("xN yM zN", "xM yM zN", "xN yM zM")),
    "WEST": (lambda w, h, d: (0, d, d, d + h), ("xN yN zM", "xN yN zN", "xN yM zM")),
    "NORTH": (lambda w, h, d: (d, d, d + w, d + h), ("xN yN zN", "xM yN zN", "xN yM zN")),
    "EAST": (lambda w, h, d: (d + w, d, d + w + d, d + h), ("xM yN zN", "xM yN zM", "xM yM zN")),
    "SOUTH": (lambda w, h, d: (d + w + d, d, d + w + d + w, d + h), ("xM yN zM", "xN yN zM", "xM yM zM")),
}
# Minecraft model face -> the same physical face in Blockbench (x and y are mirrored)
MC_TO_BB = {"DOWN": "up", "UP": "down", "WEST": "east", "EAST": "west", "NORTH": "north", "SOUTH": "south"}


def corner(spec, mn, mx):
    """'xN yM zN' -> (min x, max y, min z) in model space."""
    out = []
    for axis, part in zip(range(3), spec.split()):
        out.append(mn[axis] if part[1] == "N" else mx[axis])
    return out


def bb_uv(face, uv, b, f, t):
    """Blockbench per-face UV of the point b on the given face of a cube from f to t."""
    u1, v1, u2, v2 = uv

    def frac(val, lo, hi):
        return 0.0 if abs(hi - lo) < 1e-9 else (val - lo) / (hi - lo)

    top = frac(t[1] - b[1] + f[1], f[1], t[1])  # 0 at the top edge, 1 at the bottom edge
    if face == "north":
        s, r = frac(t[0] - b[0] + f[0], f[0], t[0]), top
    elif face == "south":
        s, r = frac(b[0], f[0], t[0]), top
    elif face == "east":
        s, r = frac(t[2] - b[2] + f[2], f[2], t[2]), top
    elif face == "west":
        s, r = frac(b[2], f[2], t[2]), top
    elif face == "up":
        s, r = frac(b[0], f[0], t[0]), frac(b[2], f[2], t[2])
    else:  # down
        s, r = frac(b[0], f[0], t[0]), frac(t[2] - b[2] + f[2], f[2], t[2])
    return u1 + s * (u2 - u1), v1 + r * (v2 - v1)


def write_texture(model, cubes, spots, out_path):
    src_info = model["textures"][0]
    src = Image.open(io.BytesIO(base64.b64decode(src_info["source"].split(",", 1)[1]))).convert("RGBA")
    res_w, res_h = model["resolution"]["width"], model["resolution"]["height"]
    sx, sy = src.width / res_w, src.height / res_h
    out = Image.new("RGBA", (TEX_UNITS * SCALE, TEX_UNITS * SCALE), (0, 0, 0, 0))
    by_uuid = {e["uuid"]: e for e in model["elements"]}

    for shared in cubes:
        e = by_uuid[shared["uuid"]]           # this colour's version of the cube (its own UVs)
        f, t = e["from"], e["to"]
        w, h, d = size_of(e)
        # model-space box, relative to the cube's "from" corner: x and y are mirrored
        mn, mx = (-w, -h, 0.0), (0.0, 0.0, d)
        u0, v0 = spots[e["uuid"]]
        for mc_face, (region_fn, corners) in MC_FACES.items():
            face = e["faces"].get(MC_TO_BB[mc_face])
            if face is None or face.get("texture") is None:
                continue
            ru1, rv1, ru2, rv2 = region_fn(w, h, d)
            c11, c21, c12 = (corner(c, mn, mx) for c in corners)
            x_lo, x_hi = round((u0 + min(ru1, ru2)) * SCALE), round((u0 + max(ru1, ru2)) * SCALE)
            y_lo, y_hi = round((v0 + min(rv1, rv2)) * SCALE), round((v0 + max(rv1, rv2)) * SCALE)
            for py in range(y_lo, y_hi):
                for px in range(x_lo, x_hi):
                    # position inside the region, 0..1 from the (u1,v1) corner
                    U = (px + 0.5) / SCALE - u0
                    V = (py + 0.5) / SCALE - v0
                    s = (U - ru1) / (ru2 - ru1)
                    r = (V - rv1) / (rv2 - rv1)
                    m = [c11[i] + s * (c21[i] - c11[i]) + r * (c12[i] - c11[i]) for i in range(3)]
                    # model space -> Blockbench space
                    b = (f[0] - m[0], f[1] - m[1], f[2] + m[2])
                    u, v = bb_uv(MC_TO_BB[mc_face], face["uv"], b, f, t)
                    tx = min(src.width - 1, max(0, int(math.floor(u * sx))))
                    ty = min(src.height - 1, max(0, int(math.floor(v * sy))))
                    out.putpixel((px, py), src.getpixel((tx, ty)))
    out.save(out_path)


# --------------------------------------------------------------------------------------------
# Animations
# --------------------------------------------------------------------------------------------
def write_animations(model, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    for anim in model["animations"]:
        bones = []
        for animator in anim["animators"].values():
            name = bone_name(animator["name"])
            if animator["name"] in SKIP_BONES:
                continue
            for channel in ("rotation", "position", "scale"):
                frames = sorted((k for k in animator["keyframes"] if k["channel"] == channel), key=lambda k: k["time"])
                if not frames:
                    continue
                keyframes = []
                for k in frames:
                    p = k["data_points"][-1]
                    x, y, z = (float(p[a]) for a in "xyz")
                    if channel == "rotation":
                        target = [-x, -y, z]
                    elif channel == "position":
                        target = [-x, y, z]   # NeoForge flips y itself
                    else:
                        target = [x, y, z]
                    keyframes.append({"timestamp": round(float(k["time"]), 4),
                                      "target": [round(c, 4) + 0.0 for c in target],
                                      "interpolation": "minecraft:" + ("catmullrom" if k["interpolation"] == "catmullrom" else "linear")})
                bones.append({"bone": name, "target": "minecraft:" + channel, "keyframes": keyframes})
        data = {"length": anim["length"], "loop": anim["loop"] == "loop", "animations": bones}
        with open(os.path.join(out_dir, f"parakeet_{anim['name']}.json"), "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
            f.write("\n")


# --------------------------------------------------------------------------------------------
def main():
    folder = sys.argv[1] if len(sys.argv) > 1 else "."
    files = sorted(glob.glob(os.path.join(folder, "*nm_budgie_*.bbmodel")))
    if not files:
        sys.exit("No nm_budgie_*.bbmodel files found in " + folder)

    first = load(files[0])
    root = build_parts(first)
    cubes = list(all_cubes(root))
    spots = pack(cubes)
    write_java(root, spots)
    write_animations(first, os.path.join(ASSETS, "neoforge", "animations", "entity"))

    tex_dir = os.path.join(ASSETS, "textures", "entity", "parakeet")
    os.makedirs(tex_dir, exist_ok=True)
    for path in files:
        colour = re.search(r"nm_budgie_(.+)\.bbmodel$", os.path.basename(path)).group(1)
        model = load(path)
        # every file has the same cubes in the same order; match them up by position in the list
        ids = [e["uuid"] for e in model["elements"]]
        base_ids = [e["uuid"] for e in first["elements"]]
        remap = dict(zip(base_ids, ids))
        local_spots = {remap[k]: v for k, v in spots.items()}
        local_cubes = [{"uuid": remap[c["uuid"]]} for c in cubes]
        write_texture(model, local_cubes, local_spots, os.path.join(tex_dir, f"parakeet_{colour}.png"))
        print("wrote", colour)
    print("Done:", len(cubes), "cubes,", len(first["animations"]), "animations")


if __name__ == "__main__":
    main()

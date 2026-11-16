"""
Shared code for turning Blockbench models (.bbmodel, "free" format with per-face UVs) into files
the mod can use. Used by convert_budgie_models.py and convert_capybara_model.py.

What it makes:
  * a Java class with createBodyLayer(): the model's bones and cubes as Minecraft model code;
  * a texture repacked into Minecraft's "box UV" layout (see write_texture);
  * NeoForge JSON animation files (assets/<mod>/neoforge/animations/entity/*.json).

Coordinates: Blockbench uses y-up with the ground at y = 0. Minecraft entity models use y-down
with the ground at y = 24, and are mirrored left-right. So x -> -x and y -> 24 - y, and
rotations around x and y change sign.
"""
import base64
import io
import json
import math
import os

from PIL import Image

PROJECT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(PROJECT, "src", "main", "resources", "assets", "parakeetsandcapybaras")
JAVA_DIR = os.path.join(PROJECT, "src", "main", "java", "com", "parakeetsandcapybaras", "client", "model")
ANIMATION_DIR = os.path.join(ASSETS, "neoforge", "animations", "entity")


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


# --------------------------------------------------------------------------------------------
# Geometry
# --------------------------------------------------------------------------------------------
def to_model(p, origin):
    """A Blockbench point, relative to a pivot, in Minecraft model space."""
    return (-(p[0] - origin[0]), -(p[1] - origin[1]), p[2] - origin[2])


def build_parts(model, bone_name, skip_bones):
    """Returns a tree of parts: {name, origin, parent_origin, rotation, cubes, children}."""
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
            elif child["name"] not in skip_bones:
                part["children"].append(group(child, node["origin"]))
        return part

    roots = [o for o in model["outliner"] if not isinstance(o, str) and o["name"] not in skip_bones]
    assert len(roots) == 1, "expected exactly one root bone"
    return group(roots[0], None)


def all_cubes(part):
    yield from part["cubes"]
    for c in part["children"]:
        yield from all_cubes(c)


def size_of(e):
    return [round(e["to"][i] - e["from"][i], 4) for i in range(3)]


def pack(cubes, tex_units):
    """Shelf-packs every cube's box-UV area (2*(d+w) wide, d+h tall). Returns {uuid: (u, v)}."""
    spots, x, y, row = {}, 0, 0, 0
    for e in sorted(cubes, key=lambda e: -(size_of(e)[2] + size_of(e)[1])):
        w, h, d = size_of(e)
        bw, bh = math.ceil(2 * (d + w)), math.ceil(d + h)
        if x + bw > tex_units:
            x, y, row = 0, y + row, 0
        spots[e["uuid"]] = (x, y)
        x += bw
        row = max(row, bh)
    assert y + row <= tex_units, "textures do not fit"
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


def write_java(root, spots, class_name, description, script, tex_units):
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
 * {description}
 *
 * <p>GENERATED by tools/{script}.
 * Don't edit this by hand: change the model in Blockbench and run the script again.</p>
 */
@SuppressWarnings("unused")
public final class {class_name} {{
    private {class_name}() {{
    }}

    public static LayerDefinition createBodyLayer() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, {tex_units}, {tex_units});
    }}
}}
'''
    with open(os.path.join(JAVA_DIR, class_name + ".java"), "w", encoding="utf-8") as f:
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


def write_texture(model, cubes, spots, out_path, tex_units, scale):
    """Repacks the model's per-face texture into Minecraft's box-UV layout.

    For every pixel of every face in the new layout, works out which point of the cube it
    belongs to and copies the colour the Blockbench model shows at that point.
    """
    src_info = model["textures"][0]
    src = Image.open(io.BytesIO(base64.b64decode(src_info["source"].split(",", 1)[1]))).convert("RGBA")
    res_w, res_h = model["resolution"]["width"], model["resolution"]["height"]
    sx, sy = src.width / res_w, src.height / res_h
    out = Image.new("RGBA", (tex_units * scale, tex_units * scale), (0, 0, 0, 0))
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
            x_lo, x_hi = round((u0 + min(ru1, ru2)) * scale), round((u0 + max(ru1, ru2)) * scale)
            y_lo, y_hi = round((v0 + min(rv1, rv2)) * scale), round((v0 + max(rv1, rv2)) * scale)
            for py in range(y_lo, y_hi):
                for px in range(x_lo, x_hi):
                    # position inside the region, 0..1 from the (u1,v1) corner
                    U = (px + 0.5) / scale - u0
                    V = (py + 0.5) / scale - v0
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
def write_animations(model, prefix, bone_name, skip_bones, out_dir=ANIMATION_DIR):
    os.makedirs(out_dir, exist_ok=True)
    for anim in model["animations"]:
        bones = []
        for animator in anim["animators"].values():
            name = bone_name(animator["name"])
            if animator["name"] in skip_bones:
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
        with open(os.path.join(out_dir, f"{prefix}_{anim['name']}.json"), "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2)
            f.write("\n")

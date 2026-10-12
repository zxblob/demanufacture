#!/usr/bin/env python3
"""Build animation-ready Blockbench rigs (Bedrock format) from the Farmer's Delight / Kaleidoscope Cookery
block models in libs/.

Usage:  python3 tools/cooking_rigs.py            (run from the project root)

Output: planning/art/Cooking Rigs/<rig>.bbmodel  (textures are embedded)

What a rig contains
  root
    vessel      the block model (export), cube names come from the original model's element names
    contents    empty bone at the mouth of the vessel: where ingredients are drawn in game
    reference   (export:false) a 16 px counter or floor, so you see the real height of the block
    character   (export:false) full player (Steve proportions) standing one block back, facing the block,
                bones named head/body/right_arm/left_arm/right_leg/left_leg like Emotecraft / Player Animator;
                hidden alex_right_arm / alex_left_arm (3 px wide) to compare
  camera        locator: a guess at where the cook camera sits
Animations (empty stubs): idle, work, add_ingredient, serve, fail
"""
import io
import json
import re
import sys
import uuid
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
LIBS = ROOT / "libs"
OUT = ROOT / "planning" / "art" / "Cooking Rigs"

FD = next(LIBS.glob("FarmersDelight-*.jar"))
KC = next(LIBS.glob("kaleidoscopecookery-*.jar"))
JARS = {"farmersdelight": zipfile.ZipFile(FD), "kaleidoscope_cookery": zipfile.ZipFile(KC)}

# rig name -> (models to merge as groups {group: model id}, floor mode, where the contents bone sits (x, y, z))
RIGS = {
    "fd_cooking_pot": ({"vessel": "farmersdelight:block/cooking_pot"}, "counter", (0, 10, 0)),
    "fd_skillet": ({"vessel": "farmersdelight:block/skillet"}, "counter", (0, 3, 0)),
    "fd_cutting_board": ({"vessel": "farmersdelight:block/cutting_board"}, "counter", (0, 1, 0)),
    "fd_stove": ({"vessel": "farmersdelight:block/stove"}, "floor", (0, 16, 0)),
    "kc_stockpot": ({"vessel": "kaleidoscope_cookery:block/stockpot"}, "counter", (0, 10, 0)),
    "kc_wok": ({"vessel": "kaleidoscope_cookery:block/pot"}, "counter", (0, 4, 0)),
    "kc_chopping_board": ({"vessel": "kaleidoscope_cookery:block/chopping_board"}, "counter", (0, 1, 0)),
    "kc_stove": ({"vessel": "kaleidoscope_cookery:block/stove"}, "floor", (0, 16, 0)),
}

DIRS = ["north", "east", "south", "west", "up", "down"]


def _cube(**faces):
    full = {d: {"uv": [0, 0, 16, 16], "texture": "#" + t} for d, t in faces.items()}
    return [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": full}], {}


# the few vanilla parents the cook blocks use, as full cubes
VANILLA_CUBES = {
    "block/cube": _cube(north="north", east="east", south="south", west="west", up="up", down="down"),
    "block/orientable_with_bottom": _cube(north="front", east="side", south="side", west="side", up="top",
                                          down="bottom"),
    "block/cube_all": _cube(north="all", east="all", south="all", west="all", up="all", down="all"),
}


def split_id(rid):
    ns, _, path = rid.rpartition(":")
    return (ns or "minecraft"), path


def read_json(ns, rel):
    z = JARS.get(ns)
    if z is None:
        return None
    try:
        return json.loads(z.read(f"assets/{ns}/{rel}"))
    except KeyError:
        return None


def load_model(rid, depth=0):
    """Resolve a model with its parents: (elements, textures)."""
    ns, path = split_id(rid)
    if ns == "minecraft" and path in VANILLA_CUBES:
        return VANILLA_CUBES[path]
    data = read_json(ns, f"models/{path}.json")
    if data is None:
        if ns != "minecraft":
            print(f"  ! missing model {rid}", file=sys.stderr)
        return [], {}
    elements, textures = [], {}
    if "parent" in data and depth < 8:
        elements, textures = load_model(data["parent"], depth + 1)
    textures = {**textures, **data.get("textures", {})}
    if "elements" in data:
        elements = data["elements"]
    return elements, textures


def resolve_tex(textures, ref):
    seen = 0
    while ref and ref.startswith("#") and seen < 10:
        ref = textures.get(ref[1:])
        seen += 1
    return ref


def load_png(rid):
    ns, path = split_id(rid)
    z = JARS.get(ns)
    if z is None:
        return None
    try:
        im = Image.open(io.BytesIO(z.read(f"assets/{ns}/textures/{path}.png"))).convert("RGBA")
    except KeyError:
        return None
    if im.height > im.width and im.height % im.width == 0:  # animated strip -> first frame
        im = im.crop((0, 0, im.width, im.width))
    return im


class Atlas:
    """Stacks textures vertically; extra rows hold baked (rotated) face crops and the character colours."""

    def __init__(self):
        self.tex = {}      # id -> (image, y offset)
        self.baked = []    # (image, id)
        self.width = 16

    def add(self, rid):
        if rid in self.tex:
            return
        im = load_png(rid)
        if im is None:
            im = Image.new("RGBA", (16, 16), (255, 0, 255, 255))
            print(f"  ! missing texture {rid}", file=sys.stderr)
        self.tex[rid] = [im, 0]
        self.width = max(self.width, im.width)

    def bake(self, im):
        self.baked.append([im, 0])
        self.width = max(self.width, im.width)
        return len(self.baked) - 1

    def layout(self):
        y = 0
        for ent in self.tex.values():
            ent[1] = y
            y += ent[0].height
        for ent in self.baked:
            ent[1] = y
            y += ent[0].height
        self.char_y = y            # 4 colour strips of 8 x 4
        y += 4 * 4
        self.height = y

    def image(self):
        out = Image.new("RGBA", (self.width, self.height), (0, 0, 0, 0))
        for im, y in list(self.tex.values()) + self.baked:
            out.paste(im, (0, y))
        for i, col in enumerate(CHAR_COLOURS):
            out.paste(Image.new("RGBA", (8, 4), col), (0, self.char_y + i * 4))
        return out


# skin, hair, shirt, trousers
CHAR_COLOURS = [(198, 140, 108, 255), (74, 52, 34, 255), (58, 143, 183, 255), (61, 58, 140, 255)]


def uid():
    return str(uuid.uuid4())


def make_cube(name, frm, to, origin=None, rotation=None, faces=None, export=True):
    return {
        "name": name, "box_uv": False, "render_order": "default", "locked": False, "export": export,
        "scope": 0, "allow_mirror_modeling": True, "from": frm, "to": to, "autouv": 0, "color": 0,
        "origin": origin or [0, 0, 0], **({"rotation": rotation} if rotation else {}),
        "faces": faces, "type": "cube", "uuid": uid(),
    }


def convert_elements(elements, textures, atlas, prefix):
    cubes = []
    for i, el in enumerate(elements):
        frm = [el["from"][0] - 8, el["from"][1], el["from"][2] - 8]
        to = [el["to"][0] - 8, el["to"][1], el["to"][2] - 8]
        rot = origin = None
        if "rotation" in el:
            r = el["rotation"]
            ax = r["axis"]
            rot = [0, 0, 0]
            rot["xyz".index(ax)] = r["angle"]
            o = r["origin"]
            origin = [o[0] - 8, o[1], o[2] - 8]
        faces = {}
        for d in DIRS:
            f = el.get("faces", {}).get(d)
            if not f:
                faces[d] = {"uv": [0, 0, 0, 0], "texture": None}
                continue
            tid = resolve_tex(textures, f.get("texture"))
            if not tid:
                faces[d] = {"uv": [0, 0, 0, 0], "texture": None}
                continue
            atlas.add(tid)
            im, _ = atlas.tex[tid]
            uv = f.get("uv") or default_uv(el, d)
            x1, y1, x2, y2 = uv
            sx, sy = im.width / 16, im.height / 16
            rotn = f.get("rotation", 0) % 360
            if rotn:
                # Bedrock cubes cannot store per-face UV rotation: bake the rotated crop into an extra row
                crop = im.crop((round(min(x1, x2) * sx), round(min(y1, y2) * sy),
                                max(round(max(x1, x2) * sx), round(min(x1, x2) * sx) + 1),
                                max(round(max(y1, y2) * sy), round(min(y1, y2) * sy) + 1)))
                if x1 > x2:
                    crop = crop.transpose(Image.FLIP_LEFT_RIGHT)
                if y1 > y2:
                    crop = crop.transpose(Image.FLIP_TOP_BOTTOM)
                crop = crop.rotate(-rotn, expand=True)
                k = atlas.bake(crop)
                faces[d] = {"uv": ("baked", k, crop.width, crop.height), "texture": 0}
            else:
                # row offsets are only known after layout(): resolved in finish_uvs
                faces[d] = {"uv": ("tex", tid, x1 * sx, y1 * sy, x2 * sx, y2 * sy), "texture": 0}
        cubes.append(make_cube(f"{prefix}{el.get('name') or 'cube'}_{i}", frm, to, origin, rot, faces))
    return cubes


def default_uv(el, d):
    (x1, y1, z1), (x2, y2, z2) = el["from"], el["to"]
    return {
        "north": [x1, 16 - y2, x2, 16 - y1], "south": [x1, 16 - y2, x2, 16 - y1],
        "east": [z1, 16 - y2, z2, 16 - y1], "west": [z1, 16 - y2, z2, 16 - y1],
        "up": [x1, z1, x2, z2], "down": [x1, z1, x2, z2],
    }[d]


def finish_uvs(cubes, atlas):
    for c in cubes:
        for f in c["faces"].values():
            uv = f["uv"]
            if isinstance(uv, tuple) and uv[0] == "tex":
                _, tid, x1, y1, x2, y2 = uv
                oy = atlas.tex[tid][1]
                f["uv"] = [x1, y1 + oy, x2, y2 + oy]
            elif isinstance(uv, tuple):
                _, k, w, h = uv
                y = atlas.baked[k][1]
                f["uv"] = [0, y, w, y + h]


def character_cubes(floor_y, atlas, z=14):
    """Steve-proportioned player. bedrock bone origins; offset so feet stand on floor_y, back at z."""
    def box(name, frm, to, origin, skin_strip, front_strip=None):
        f = [frm[0], frm[1] + floor_y, frm[2] + z]
        t = [to[0], to[1] + floor_y, to[2] + z]
        o = [origin[0], origin[1] + floor_y, origin[2] + z]
        faces = {}
        for d in DIRS:
            s = front_strip if (d == "north" and front_strip is not None) else skin_strip
            y = atlas.char_y + s * 4
            faces[d] = {"uv": [0, y, 8, y + 4], "texture": 0}
        return make_cube(name, f, t, o, None, faces, export=False)
    return {
        "head": box("head", [-4, 24, -4], [4, 32, 4], [0, 24, 0], 1, 0),
        "body": box("body", [-4, 12, -2], [4, 24, 2], [0, 24, 0], 2),
        # the character faces -z (towards the block), so its right side is +x
        "right_arm": box("right_arm", [4, 12, -2], [8, 24, 2], [5, 22, 0], 0),
        "left_arm": box("left_arm", [-8, 12, -2], [-4, 24, 2], [-5, 22, 0], 0),
        "right_leg": box("right_leg", [0, 0, -2], [4, 12, 2], [1.9, 12, 0], 3),
        "left_leg": box("left_leg", [-4, 0, -2], [0, 12, 2], [-1.9, 12, 0], 3),
        "alex_right_arm": box("alex_right_arm", [4, 12, -2], [7, 24, 2], [5, 22, 0], 0),
        "alex_left_arm": box("alex_left_arm", [-7, 12, -2], [-4, 24, 2], [-5, 22, 0], 0),
    }


def group(name, origin, children, export=True, visible=True):
    return {
        "name": name, "uuid": uid(), "export": export, "locked": False, "scope": 0, "selected": False,
        "visibility": visible, "origin": origin, "rotation": [0, 0, 0], "bedrock_binding": "", "color": 0,
        "children": children, "reset": False, "shade": True, "mirror_uv": False, "autouv": 0, "isOpen": True,
        "primary_selected": False,
    }


def build(name, spec):
    parts, floor_mode, contents = spec
    atlas = Atlas()
    vessel_cubes = []
    for gname, mid in parts.items():
        elements, textures = load_model(mid)
        if not elements:
            print(f"  ! {name}: {mid} has no elements", file=sys.stderr)
        vessel_cubes += convert_elements(elements, textures, atlas, "")
    atlas.layout()
    floor_y = -16 if floor_mode == "counter" else 0
    char = character_cubes(floor_y, atlas)
    ref = []
    if floor_mode == "counter":
        faces = {d: {"uv": [0, atlas.char_y + 4 * 2 - 4, 8, atlas.char_y + 4 * 2], "texture": 0} for d in DIRS}
        ref.append(make_cube("counter", [-8, -16, -8], [8, 0, 8], [0, 0, 0], None, faces, export=False))
    faces = {d: {"uv": [0, atlas.char_y + 4 * 3, 8, atlas.char_y + 4 * 4], "texture": 0} for d in DIRS}
    ref.append(make_cube("floor", [-24, floor_y - 1, -24], [24, floor_y, 40], [0, 0, 0], None, faces, export=False))
    finish_uvs(vessel_cubes + ref + list(char.values()), atlas)

    locator = {"name": "camera", "type": "locator", "position": [10, 18, 40], "rotation": [0, 0, 0],
               "export": True, "locked": False, "uuid": uid(), "color": 0}
    g_vessel = group("vessel", [0, 0, 0], [c["uuid"] for c in vessel_cubes])
    g_contents = group("contents", list(contents), [])
    g_ref = group("reference", [0, 0, 0], [c["uuid"] for c in ref], export=False)
    char_groups = []
    for n, c in char.items():
        vis = not n.startswith("alex_")
        char_groups.append(group(n, c["origin"], [c["uuid"]], export=False, visible=vis))
        c["visibility"] = vis
    g_char = group("character", [0, 0, 0], char_groups, export=False)
    g_root = group("root", [0, 0, 0], [g_vessel, g_contents, g_ref, g_char, locator["uuid"]])

    # Blockbench keeps a flat `groups` list and nests them by uuid in `outliner`
    all_groups = []

    def walk(g):
        flat = {k: v for k, v in g.items() if k != "children"}
        flat["children"] = []
        all_groups.append(flat)
        return {"uuid": g["uuid"], "isOpen": True,
                "children": [walk(k) if isinstance(k, dict) else k for k in g["children"]]}

    outliner = [walk(g_root)]

    buf = io.BytesIO()
    atlas.image().save(buf, "PNG")
    import base64
    png = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode()

    def anim(n, length, loop):
        return {"uuid": uid(), "name": n, "loop": loop, "override": False, "length": length, "snapping": 24,
                "selected": False, "saved": False, "path": "", "scope": 0, "anim_time_update": "",
                "blend_weight": "", "start_delay": "", "loop_delay": "", "animators": {}}

    texture = {
        "name": f"{name}.png", "path": "", "folder": "", "namespace": "", "id": "0", "group": "", "scope": 0,
        "width": atlas.width, "height": atlas.height, "uv_width": atlas.width, "uv_height": atlas.height,
        "particle": False, "use_as_default": False, "layers_enabled": False, "sync_to_project": "",
        "file_format": "png", "render_mode": "default", "render_sides": "auto", "wrap_mode": "limited",
        "pbr_channel": "color", "fps": 7, "frame_time": 1, "frame_order_type": "loop", "frame_order": "",
        "frame_interpolate": False, "visible": True, "internal": True, "saved": False, "uuid": uid(),
        "source": png,
    }
    project = {
        "meta": {"format_version": "5.0", "model_format": "bedrock", "box_uv": False},
        "name": name, "model_identifier": "unknown", "visible_box": [1, 1, 0],
        "variable_placeholders": "", "multi_file_ruleset": "", "variable_placeholder_buttons": [],
        "bedrock_animation_mode": "entity", "timeline_setups": [], "unhandled_root_fields": {},
        "resolution": {"width": atlas.width, "height": atlas.height},
        "elements": vessel_cubes + ref + list(char.values()) + [locator],
        "groups": all_groups, "outliner": outliner, "textures": [texture],
        "animations": [anim("idle", 1.0, "loop"), anim("work", 1.0, "loop"),
                       anim("add_ingredient", 1.0, "once"), anim("serve", 1.0, "once"),
                       anim("fail", 1.0, "once")],
    }
    return project, len(vessel_cubes), (atlas.width, atlas.height)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, spec in RIGS.items():
        project, n, size = build(name, spec)
        (OUT / f"{name}.bbmodel").write_text(json.dumps(project, indent=1))
        print(f"{name}: {n} cubes, atlas {size[0]}x{size[1]}")


if __name__ == "__main__":
    main()

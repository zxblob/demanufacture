#!/usr/bin/env python3
"""Export the animations of a cooking rig (.bbmodel) to Player Animator's Bedrock/GeckoLib JSON.

Usage:  python3 tools/export_player_animations.py <rig.bbmodel> [rig_name]

Writes
  frozen/cooking-minigames/src/main/resources/assets/demanufacture/player_animations/<rig>.json   player bones only (what Player Animator plays)
  frozen/cooking-minigames/src/main/resources/assets/demanufacture/cook_props/<rig>.json          every other animated bone (spoon, lid...)

Animation ids become  <rig>.<animation name>  (Player Animator keys animations by name inside one namespace).
Value rules are Blockbench's own Bedrock export: rotation = [-x, -y, z], position = [-x, y, z].
Blockbench's autosaves are lz-compressed; decode them first (see the handoff, section 10d) or save the project.
"""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "frozen/cooking-minigames/src/main/resources/assets/demanufacture"
PLAYER_BONES = {"head", "body", "right_arm", "left_arm", "right_leg", "left_leg"}


def num(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        raise SystemExit(f"keyframe value {v!r} is an expression; only plain numbers are supported")


def fmt_time(t):
    return f"{round(float(t), 4):g}"


def channel(keys, kind):
    out = {}
    for k in sorted(keys, key=lambda k: k["time"]):
        pts = [[num(p["x"]), num(p["y"]), num(p["z"])] for p in k["data_points"]]
        if kind == "rotation":
            pts = [[-x, -y, z] for x, y, z in pts]
        elif kind == "position":
            pts = [[-x, y, z] for x, y, z in pts]
        entry = {}
        if len(pts) == 2:
            entry["pre"], entry["post"] = pts
        else:
            entry["vector"] = pts[0]
        # Always write the object form with an explicit lerp_mode: Player Animator reads a bare
        # [x, y, z] rotation as a CONSTANT (stepped) key, which is what made the first export choppy.
        entry["lerp_mode"] = k.get("interpolation", "linear") if k.get("interpolation") in ("linear", "catmullrom", "step") else "linear"
        out[fmt_time(k["time"])] = entry
    return out


def main():
    src = Path(sys.argv[1])
    rig = sys.argv[2] if len(sys.argv) > 2 else src.stem
    d = json.loads(src.read_text())
    player, props = {}, {}
    for a in d.get("animations", []):
        loop = a.get("loop", "once")
        body = {"loop": True if loop == "loop" else ("hold_on_last_frame" if loop == "hold" else False),
                "animation_length": a["length"]}
        pb, ob = {}, {}
        for an in a["animators"].values():
            if an.get("type") != "bone" or not an.get("keyframes"):
                continue
            chans = {}
            for kind in ("rotation", "position", "scale"):
                ks = [k for k in an["keyframes"] if k["channel"] == kind]
                if ks:
                    chans[kind] = channel(ks, kind)
            if chans:
                (pb if an["name"] in PLAYER_BONES else ob)[an["name"]] = chans
        name = f"{rig}.{a['name']}"
        if pb:
            player[name] = {**body, "bones": pb}
        if ob:
            props[name] = {**body, "bones": ob}
        print(f"{name}: {len(pb)} player bones, {len(ob)} other bones, {a['length']}s, loop={loop}")
    for sub, data in (("player_animations", player), ("cook_props", props)):
        if data:
            out = RES / sub / f"{rig}.json"
            out.parent.mkdir(parents=True, exist_ok=True)
            out.write_text(json.dumps({"format_version": "1.8.0", "animations": data}, indent=1))
            print("wrote", out.relative_to(ROOT))


if __name__ == "__main__":
    main()

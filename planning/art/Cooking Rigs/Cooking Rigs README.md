---
status: built
updated: 2026-10-12
---
# Cooking Rigs README

Blockbench projects (Bedrock format, textures embedded) generated from the Farmer's Delight and Kaleidoscope Cookery block models by `tools/cooking_rigs.py`. Open one with Blockbench, go to the Animate tab, and pose the character against the block.

| file | block | contents bone height |
| --- | --- | --- |
| `fd_cooking_pot.bbmodel` | FD Cooking Pot | 10 px |
| `kc_stockpot.bbmodel` | KC Stockpot | 10 px |
| `fd_skillet.bbmodel` | FD Skillet | 3 px |
| `kc_wok.bbmodel` | KC Wok (`kaleidoscope_cookery:pot`) | 4 px |
| `fd_cutting_board.bbmodel` | FD Cutting Board | 1 px |
| `kc_chopping_board.bbmodel` | KC Chopping Board | 1 px |
| `fd_stove.bbmodel`, `kc_stove.bbmodel` | the two stoves (plain cubes) | top of block |

## Layout of every file
- `root`
  - `vessel`: the block (exported). Cubes are named `cube_N` because KC's models carry no element names.
  - `contents`: empty bone where ingredients appear in game. Move its pivot if you want them elsewhere.
  - `reference` (not exported): a 16 px counter under the block (or a floor for the stoves).
  - `character` (not exported): full Steve-proportioned player one block back, facing the block. Bones are named `head`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg` (what Emotecraft and Player Animator expect). `alex_right_arm` / `alex_left_arm` are 3 px wide and hidden: toggle them to check slim skins.
  - `camera` locator: only a guess at the camera position, the real one is set in code.
- Animations are empty stubs: `idle`, `work` (the loop), `add_ingredient`, `serve`, `fail`.

## Rules for animating (so it works in game)
- **`work` is driven by the mouse**: time in the animation = progress of your stir / flip, so make it a clean loop that reads well forwards and at different speeds.
- Keep every bone returning to rest on the final keyframe.
- The block itself does **not** move in game (FD and KC own those models). Animate the **character**, the held tool and the ingredients; moving vessel parts (lid, pan tilt) will only be possible if we later replace the block model.
- Textures are the originals from the jars (see `THIRD_PARTY.md`); this is for posing, final ingredient and tool models are separate.

## Rebuild
`python3 tools/cooking_rigs.py` from the project root. It overwrites these files, so copy yours elsewhere first.

## Getting your animations into the game
1. In Blockbench: **File > Save** the project (any rig file in this folder).
2. Tell Claude, or run `python3 tools/export_player_animations.py "planning/art/Cooking Rigs/<rig>.bbmodel"`. It writes `player_animations/<rig>.json` (player bones: head, body, arms, legs) and `cook_props/<rig>.json` (any other bone you animated, e.g. the spoon).
3. In game: `/cookanim list`, `/cookanim play fd_cooking_pot.work`, `/cookanim stop`. Press F5 to see yourself. Animation ids are `<rig>.<animation name>`.
Player Animator ignores non-player bones, so keep custom bones (spoon, lid) in their own group and Claude will drive them from `cook_props`.

## Needed next (Claude)
Export path for the animations (Emotecraft / Player Animator JSON) and a tool model for the held spatula, ladle, knife. Player Animator 2.0.4 (`dev.kosmx.playerAnim`) is in the pack and has what we need: `PlayerAnimationAccess` (per-player layer stack), `ModifierLayer`, `KeyframeAnimationPlayer` (plays Emotecraft JSON) and the lower-level `IAnimation` for driving arm bones procedurally from the mouse. Still to do: add it to `libs/` as compileOnly and prove one animation on the player.

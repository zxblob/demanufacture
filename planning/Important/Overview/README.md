# Demanufacture planning vault

Open this folder (`planning/`) as a vault in Obsidian. Start at **Overview.canvas** - that's the mindmap.

**To Do - Compat Features.md** (in the vault root) is the one checklist of every compat idea. Updated 2026-10-09.

**Mind Maps/Mod Map.canvas** is the pack-wide radial mindmap of every installed mod (green arrow = similar and mergeable, red arrow = a new connection we could build; only connections we can implement are kept). **Mod Map - Similar.canvas** and **Mod Map - Connect.canvas** show only the mods that have at least one green / red arrow. Generated from the jars on 2026-10-07, pruned 2026-10-08; edit freely.

## How we use it together

- **Overview.canvas** is the big picture. Drag cards around, add new cards, draw arrows between them. Claude can read the whole thing (it's plain JSON).
- Each card that is a note links to a file in `systems/`, `minigames/`, `ideas/`, `art/` or `decisions/`.
- **Adding an idea:** either add a text card straight onto the canvas, or create a note in `ideas/`. Link notes to each other with `[[Note Name]]`.
- **Asking Claude to do something:** write a line starting with `TODO(claude):` anywhere. Claude greps for these at the start of a session.
- **Answering a question:** open questions live in [[Open Questions]]. Write your answer right under the question.
- **Sketches / screenshots:** drop them in `attachments/` and embed with `![[filename.png]]`.

## Status values (top of every note, `status:`)

| status | meaning |
| --- | --- |
| `built` | works in game, tested |
| `partial` | works, but with known gaps (listed in the note) |
| `planned` | designed, not started |
| `idea` | not decided / not designed yet |

## Canvas card colours

green = built, yellow = partial, cyan = planned, purple = idea, orange = needs art from you, red = open questions.

## Handoffs

`CC Handoffs/handoff 11 10 2026.md` (named by date; the newest one is current) is the full "where are we" document for a fresh Claude Code session (goal, current state, known issues, next steps, environment). Claude rewrites it when something big changes.

## Where the code lives

`../src/main/java/com/zxblob/demanufacture/` - package layout is described in `../CLAUDE.md`.

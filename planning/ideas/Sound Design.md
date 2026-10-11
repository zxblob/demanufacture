---
status: idea
---
# Sound Design

- You want a sound when you go too fast / too slow - **in the final version**, not now.
- It must not "ding ding ding": the raw on/off pace state flickers every frame. The [[Doneness and Pace]] meter was built to smooth that. A sound should key off the *meter crossing a threshold* (with a cooldown), never the instantaneous speed.
- Other likely sounds: ingredient dropped in, liquid poured, doneness reached, ruined, served, page turn (already built into vanilla's book buttons).
- Needs sound files from you - see [[Art Requests]]. Code side is a registered sound event per cue.

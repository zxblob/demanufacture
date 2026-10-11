---
status: built
---
# Doneness and Pace

Two meters under the pot in the minigame screens.

## Pace
- 0-100. Drains 18/s while off-pace, recovers 10/s on-pace. Speed is smoothed so it doesn't flicker (and later a sound cue must not "ding ding ding" - see [[Sound Design]]).
- [[Stirring]]: angular speed of the mouse around the pot, good band 0.6-6.0 rad/s.
- [[Scrambling]]: raw mouse travel speed, good band 400-2500 px/s.
- **Pace hits 0 -> the pot's ingredients are ruined** (destroyed). You chose this over "reset progress" or "close the screen".

## Doneness
- Only advances while pace >= 90 **and** the pot has an ingredient **and** a liquid ([[Liquid Flask]]).
- 30 seconds of good stirring (`COOK_DURATION_SECONDS`, placeholder number).
- At 100% every raw ingredient is converted using **vanilla smelting recipes** (Raw Beef -> Steak). Works for any raw/cooked pair FD/KC define, no hand-kept table.
- Progress resets when the pot becomes empty (served / ruined / emptied by hand).

## Trust model
Pace only exists on the client, so the server trusts the client's "finished" / "ruined" signals (it still checks cook mode and reach). Fine for a friends' SMP, not hardened. Noted in [[Decisions Log]].

## Tuning
All numbers are first guesses - tweak after playing. They are constants at the top of `StirringMinigameScreen` / `ScramblingMinigameScreen`.

# Demanufacture

NeoForge 1.21.1 compat mod for the New'O'Nautics modpack (later an SMP). A pack-wide harmoniser (since 2026-10-07). Mod id `demanufacture`, package `com.zxblob.demanufacture`. **As of 2026-10-12 `src/` is an empty shell:** the Overcooked-style cooking minigames are frozen in `frozen/cooking-minigames/` (see its README to unfreeze) and the electricity module (Electro-Zapper, Factory Light) was merged into **ArtificeCannons** (mod id `artifice_cannons`, built and deployed to the pack as `artifice_cannons-0.2.0.jar`).

## Roles
The user is creative direction and does **all art/assets**; Claude writes the code and asks for assets by name. Placeholders (vanilla textures) are fine until real art arrives.

## Read first
- `planning/CC Handoffs/handoff 12 10 2026.md` - the latest full handoff (goal, state, known issues, next steps). Start here in a fresh session.
- `planning/` is an Obsidian vault. Start with `planning/Overview/README.md`; the checklist is `planning/Important/To Do - Compat Features.md`; mind maps are in `planning/Mind Maps/`. Diff the pack's `mods/` folder against `planning/ideas/Mod List Snapshot.md` at the start of a session.
- `grep -rn "TODO(claude)" planning/` for things the user wants done.
- `planning/Important/Open Questions.md` for pending decisions - the user answers under each question.
- `planning/decisions/Technical Gotchas.md` before touching networking, containers or build config.
- Keep the vault current: when a feature changes, update its note's `status:` and gaps in the same session.

## Build / run
- `./gradlew compileJava` to check; `./gradlew runClient` to play from source (dev client has FD + KC loaded from `libs/`). Logs in `run/logs/latest.log`.
- Gradle runs on the local JDK 21 set in `gradle.properties` (system JDK 26 is too new for Gradle 8.14.2).
- The user playtests by running `runClient` themselves; Claude can't drive the game window, so ask for results and read the log.

## Code layout
The sections below describe the **frozen** cooking code (now under `frozen/cooking-minigames/src/main/java/com/zxblob/demanufacture/`). `src/main/java/com/zxblob/demanufacture/` only holds `Demanufacture.java`, an empty shell. Electricity code: `ArtificeCannons/src/main/java/com/zxblob/artificecannons/electricity/`.

- `Demanufacture.java` - registries: attachments (cook mode, pot ingredients, pot liquid, served dishes), cookbook item/block, cooked dish item.
- `cooking/` - server-side logic: `CookBlockInteractionHandler` (which block opens which minigame), `CookingIngredientHandler` (all server payload handlers), `BasketScanner`, `LiquidItems`, `DishNaming`, cookbook item/block, `CookModeUtil`.
- `network/` - payload records and `ModNetworking` registration.
- `client/` - screens (`StirringMinigameScreen`, `ScramblingMinigameScreen`, `CookbookMenuScreen`), openers and client network handlers. Keep client-only classes out of common code paths.
- **ArtificeCannons/** is a second, separate Gradle project (IANA addon, mod id artifice_cannons) living inside this repo; build it from its own folder.
- Dependencies FD and KC are jars in `libs/` (compileOnly + localRuntime).

## Conventions
- No mixins unless an event/capability can't do it.
- Server is authoritative; the client is only trusted for pace-derived signals (documented in the decisions log).
- Don't commit or push unless asked. The project is a git repo (`main`, remote `origin` over SSH); `libs/`, `build/` and `run/` are git-ignored.

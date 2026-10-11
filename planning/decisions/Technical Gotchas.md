---
status: built
---
# Technical Gotchas

Things that cost time once. Claude: read before touching the related code.

## Game / mod behaviour
- **Container contents never sync to clients.** FD's Basket (like a chest) only sends its items via the menu protocol or the chunk's first load. Reading it from the client level gives stale/empty data. Always scan on the server and send the result ([[Basket Integration]]).
- **Block interaction:** FD/KC cook blocks open their menu in `useItemOn`, which is also called with an empty hand in 1.21.1. `RightClickBlock` fires on both sides - cancel on both.
- **KC naming:** modId is `kaleidoscope_cookery` (not `kaleidoscopecookery`). The block called Wok is `PotBlock` / `ModBlocks.POT`; "Oil Can" is `OilPotBlock`. See [[Wok]].
- **Attachments on foreign block entities** (our pot ingredients on FD/KC block entities) work and sync via `setData` + `syncData`; list attachments must be replaced with a new immutable list, never mutated in place.
- **Loot tables** live in `data/<modid>/loot_table/` (singular) in 1.21.1.
- `PotionContents.is(Potions.WATER)` identifies water bottles; water buckets are a separate item.

## Build / tooling
- The system JDK is 26; Gradle 8.14.2 can't run on it. `gradle.properties` points `org.gradle.java.home` at a local JDK 21 in `~/.local/jdks/jdk-21.0.12.1+1`.
- Run the game from source with `./gradlew runClient` (not through CurseForge). Logs: `run/logs/latest.log`.
- `StreamCodec.map()` keeps the original buffer type (`ByteBuf`), so a payload built with it must declare `StreamCodec<ByteBuf, ...>`.
- `AttachmentType.builder(List::of)` is ambiguous (Supplier vs Function overload) - use an explicit lambda.
- `@EventBusSubscriber(bus = MOD)` shows a deprecation warning on this NeoForge version; harmless for now, revisit on a NeoForge bump.
- A class registered on the event bus with no `@SubscribeEvent` methods throws at load.

## Harmless log noise
- Narrator "Failed to load library flite" on Linux.
- KC `quark` tag reference errors (Quark isn't loaded in the dev run).

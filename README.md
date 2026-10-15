# MineBOT

Client-side Fabric mod. You set a mining job, press Start, and walk away. It only uses ordinary player actions — look, walk, jump, break, place, use, chest clicks — one per tick, with real break times. That is why it works on Paper. It does not send custom packets.

## Install

- Minecraft **26.3**
- Fabric Loader **0.19.5**
- Fabric API **0.163.0+26.3**
- Java **25**

`./gradlew build` writes the jar to `build/libs`. Put that jar in the client `mods` folder next to Fabric API. The server does not need the mod.

## Run a job

1. Place chests near the dig and fill them with supplies: torches, food, wood, planks, sticks, iron, and a pickaxe. Add any extra item ids you want kept to the keep list.
2. Press **K** (or run nothing yet — configure first).
3. Set the area by standing on the first corner and pressing **Corner A**, then the second corner, or type the coordinates. Any width and length.
4. Pick a depth: bedrock (bedrock is never broken), a Y level, or a layer count.
5. Stair off, or a clockwise border spiral: one block down per step, outer ring first, the tread column left solid to bedrock, two blocks of headroom.
6. Mine everything, or list block ids. The obstruction toggle digs other blocks when they are in the way.
7. Deposit list is what goes into chests. Keep list stays on you. Search radius, or mark a chest. If every chest is full the job pauses and the status line says `Paused: chests are full`.
8. Survival toggles: eat (hunger threshold), sleep when a bed is nearby at night, torch spacing, auto-craft a replacement pick. The pick tier is the best one the materials you are actually holding can make (wood, stone, iron, diamond, netherite).
9. Press **Start**, or run `minebot start`. `minebot stop` stops it. Named presets save and load. A job that was running resumes after you die or restart the client.

**Example quarry** is a built-in preset, not the only mode: 25×25 from the corner you are standing on, down to bedrock, stair on, mine everything, deposit cobble / deepslate / ores, keep torches, food, wood, and iron.

## Develop

```
./gradlew test
./gradlew build
```

The task runner lives in `com.minebot.core` and does not import Minecraft. `com.minebot.client` is the Fabric tick loop.

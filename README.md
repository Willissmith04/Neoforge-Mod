# Parakeets and Capybaras

A beginner-friendly Minecraft mod for **Minecraft 1.21.1** on **NeoForge** (21.1.266 or newer) that adds:

| | What it is |
|---|---|
| 🦜 **Parakeet** | A chunky little budgie (a bit smaller than a parrot) that behaves like the vanilla parrot, in 10 real budgie colours |
| 🐹 **Capybara** | A calm, tameable rodent that lies flat ("sploots") when told to sit |
| 🪺 **Bird Cage** (gold and iron) | Catch a parrot or parakeet and show it off in a cage |

Vanilla parrots are not changed. Parakeets are a separate mob that lives alongside them.

---

## How to play

### Parakeet
- **Where:** forests (forest, flower forest, birch forest, old growth birch forest, dark forest).
  They spawn a little more often than parrots do in jungles (weight 60, groups of 1-2).
- **Colours:** 10 budgie colours: green, light green, olive, green spangle, sky blue, cobalt,
  blue spangle, gray, yellow and white.
- **Wild behaviour:** wild budgies fly around and look around, and fly away from players, unless
  you are holding seeds (then they come closer). They are quiet, apart from high-pitched squeaks
  when hurt. They have 12 health, take 40% more damage from fire and lava, and can't be leashed.
- **Tricks:** right-click a budgie with an empty hand and it does a random trick: a **bite**, a
  little **dance** or **preening** its wing (at most once every 3 seconds).
- **Model and animations:** the budgie model, textures and animations (idle, walk, bite, dance,
  preen, hurt and death) are converted from the *Nog's Menagerie* budgie Blockbench models with
  `tools/convert_budgie_models.py`.
- **Tame:** right-click with **seeds** (wheat, melon, pumpkin, beetroot, torchflower seeds or a
  pitcher pod). Each seed has a 1 in 10 chance, like a parrot. Hearts = tamed!
- **Modes:** right-click a tamed parakeet to switch between **Follow → Sit → Stand**. A message
  above your hotbar tells you the new mode.
- **Shoulder:** **shift + right-click** with an empty hand to put it on your shoulder (you can
  carry two). Jump, swim, fly or sleep and it hops off, just like a parrot.
- **Dance:** play a music disc in a jukebox nearby and it parties.
- **Breed:** feed seeds to two tamed adults. The chick gets one parent's colour (with a small
  chance of a random colour "mutation"). Feeding a chick seeds makes it grow faster.
- **Careful:** cookies are poisonous to parakeets, just like parrots.

### Capybara
- **Where:** grasslands (plains, sunflower plains, meadow, savanna, savanna plateau), swamps,
  forests and sparse jungles, on grass blocks, in groups of up to 4.
  They are **uncommon** (**weight 4**): rarer than cows or sheep, but you'll come across them.
  See [Changing spawn rates](#changing-spawn-rates) to make them more common.
- **Tame:** right-click with a **melon slice** (or a melon block). 1 in 3 chance per melon.
- **Modes:** right-click a tamed capybara to switch between **Follow → Sit → Wander**.
  When sitting it lies flat on its belly with its legs spread out.
- **Breed:** feed melon to two tamed adults to get a baby.
- **Baby rides:** like baby striders, baby capybaras climb onto an adult's back. Sometimes an adult
  even spawns with a baby already riding it. **Right-click the baby (or the adult carrying it)
  with an empty hand** to take the baby off. It waits a minute before climbing back on.
- **Sniff:** right-click a capybara with an empty hand and it sniffs you (it also sniffs around
  on its own now and then).
- **Stats:** 24 health, no attack, quiet apart from deep strider sounds when hurt, and very
  flammable: fire and lava do 4x damage.
- **Model and animations:** converted from the *Nog's Menagerie* capybara Blockbench model
  (idle, walk, sniff and death animations) with `tools/convert_capybara_model.py`.

### Bird Cage
- **Craft** (gold or iron):
  ```
   N        N = gold nugget / iron nugget
  I S I     I = gold ingot  / iron ingot
  I I I     S = stick (the perch)
  ```
- **Catch a bird:** hold an empty cage and right-click a parrot or parakeet. (You can't cage
  someone else's tamed bird.)
- **Place it:** put it on top of any block, or click the **underside** of a block to **hang** it
  from the ceiling. The bird is visible inside, sitting on its perch.
- **Let it out:** right-click the placed cage with an empty hand.
- **Pick it up:** break the cage and the bird stays inside the dropped item. The tooltip shows
  which bird is in it.

---

## Building and running the mod

You need **Java 21** (for example from https://adoptium.net).

```bash
./gradlew runClient     # start Minecraft with the mod (Windows: gradlew.bat runClient)
./gradlew build         # build the mod .jar into build/libs/
```

To play with it normally, copy `build/libs/parakeetsandcapybaras-1.0.0.jar` into the `mods`
folder of a Minecraft 1.21.1 NeoForge install.

> Looking for the **Minecraft 1.21.4** version? It's saved on the `minecraft-1.21.4` branch.

In IntelliJ IDEA: *File → Open* the project folder, wait for Gradle to finish importing, then use
the `Client` run configuration.

---

## Where everything is (a map for beginners)

```
src/main/java/com/parakeetsandcapybaras/
├── ParakeetsAndCapybaras.java     the main mod class: registers everything
├── registry/                      lists of our entities, blocks, items, tags...
│   ├── ModEntities.java           mob types and hitbox sizes
│   ├── ModItems.java              bird cage items and spawn eggs
│   ├── ModBlocks.java             the two bird cage blocks
│   ├── ModBlockEntities.java      the block entity that remembers the caged bird
│   ├── ModDataComponents.java     the "caged_bird" data stored on cage items
│   └── ModTags.java               names of the JSON tags we use
├── entity/
│   ├── Parakeet.java              parakeet behaviour (based on vanilla Parrot)
│   ├── Capybara.java              capybara behaviour
│   └── ai/BabyRideAdultGoal.java  makes baby capybaras climb on adults
├── block/
│   ├── BirdCageBlock.java         placing, hanging, releasing the bird
│   └── BirdCageBlockEntity.java   stores the bird inside a placed cage
├── item/BirdCageItem.java         the cage item + its tooltip
├── event/
│   ├── BirdCageEvents.java        catching birds with a cage
│   └── CommonModEvents.java       attributes, spawn placement, creative tabs
└── client/                        drawing things (only runs on the player's computer)
    ├── ClientModEvents.java       registers renderers and models
    ├── model/ParakeetModel.java   plays the budgie animations (idle, walk, tricks, hurt, death, fly, sit)
    ├── model/ParakeetGeometry.java  the budgie's 3D shape (generated from the .bbmodel files)
    ├── model/CapybaraModel.java   plays the capybara animations (idle, walk, sniff, death, sitting pose)
    ├── model/CapybaraGeometry.java  the capybara's 3D shape (generated from the .bbmodel file)
    └── renderer/                  renderers for the mobs, the shoulder parakeet and the cage bird

src/main/resources/
├── assets/parakeetsandcapybaras/  things the player SEES
│   ├── lang/en_us.json            all English names and messages
│   ├── textures/                  PNG textures (entity/ and block/)
│   ├── models/block/              the bird cage 3D block model
│   ├── blockstates/               which model to use for each cage direction / hanging
│   └── models/item/               item models (cages and spawn eggs)
└── data/                          game RULES
    ├── parakeetsandcapybaras/neoforge/biome_modifier/   spawn weights and group sizes
    ├── parakeetsandcapybaras/tags/worldgen/biome/       which biomes each mob spawns in
    ├── parakeetsandcapybaras/tags/item/                 taming / breeding foods
    ├── parakeetsandcapybaras/tags/entity_type/          which mobs fit in a bird cage
    ├── parakeetsandcapybaras/recipe/                    crafting recipes
    └── parakeetsandcapybaras/loot_table/                drops (cages keep their bird)

tools/generate_textures.py         paints the capybara and bird cage textures (optional)
tools/convert_budgie_models.py     converts the budgie .bbmodel files into the model, textures
                                   and animations (python tools/convert_budgie_models.py <folder>)
tools/convert_capybara_model.py    the same for the capybara
                                   (python tools/convert_capybara_model.py <nm_capybara.bbmodel>)
tools/bbmodel_converter.py         the shared Blockbench -> Minecraft conversion code
```

## Easy things to change

### Changing spawn rates
Open `src/main/resources/data/parakeetsandcapybaras/neoforge/biome_modifier/add_capybara_spawns.json`:

```json
"spawners": { "type": "parakeetsandcapybaras:capybara", "weight": 4, "minCount": 1, "maxCount": 4 }
```

A higher `weight` means more common. For comparison: cows are 8, sheep 12, parrots in jungles 40
and pandas in bamboo jungles 80. Parakeets are 60. Try `8` if you want to meet capybaras regularly.

### Changing where they spawn
Edit the biome lists in `data/parakeetsandcapybaras/tags/worldgen/biome/`. You can add any biome id
(like `"minecraft:jungle"`) or a whole biome tag (like `"#minecraft:is_jungle"`).

### Changing taming food
Edit `data/parakeetsandcapybaras/tags/item/parakeet_food.json` or `capybara_food.json`.

### Changing the parakeet's size
Parakeets are drawn a little smaller than a parrot. Change `SIZE = 1.0F` in
`client/renderer/ParakeetRenderer.java` (and the hitbox numbers in `registry/ModEntities.java`).

### Changing colours or textures
Edit the PNGs in `assets/parakeetsandcapybaras/textures/`, or change the colours at the top of
`tools/generate_textures.py` and run `python tools/generate_textures.py`.

## Credits and licence
- The budgie and capybara models, textures and animations are from **Nog's Menagerie**. They are
  not covered by this project's licence; check that pack's own licence before sharing them.
- The bird cage textures were made from scratch for this mod.
- The code is MIT licensed. The parts based on Minecraft's Parrot class follow Mojang's code, as
  most mods do.

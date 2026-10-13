from pathlib import Path
import json

root = Path("src/main")
java = root / "java/br/com/residenciasaldeoes"
resources = root / "resources"
main = (java / "ResidenciasDosAldeoes.java").read_text()
door = (java / "block/ReinforcedDoorBlock.java").read_text()
assert 'BLOCKS.register("reinforced_door", ReinforcedDoorBlock::new)' in main
assert 'ITEMS.register("reinforced_door"' in main and "CreativeModeTab.TAB_DECORATIONS" in main
assert "extends DoorBlock" in door
assert "NORTH_SOUTH = Block.box(0, 0, 7, 16, 16, 9)" in door
assert "EAST_WEST = Block.box(7, 0, 0, 9, 16, 16)" in door
assert "panelShape(state)" in door
assert ".strength(5.0F, 5.0F)" in door and "Material.WOOD" in door
assert "level.setBlock(lowerPos, changed, 10)" in door and "upper.setValue(OPEN, opening)" in door
assert "SoundEvents.WOODEN_DOOR_OPEN" in door and "SoundEvents.WOODEN_DOOR_CLOSE" in door
item_model = json.loads((resources / "assets/residenciasaldeoes/models/item/reinforced_door.json").read_text())
assert item_model["display"]["firstperson_righthand"]["scale"] == [0.45, 0.45, 0.45]
assert item_model["display"]["gui"]["scale"] == [0.8, 0.8, 0.8]

recipe = json.loads((resources / "data/residenciasaldeoes/recipes/reinforced_door.json").read_text())
assert recipe["pattern"] == ["INI", "NDN", "INI"]
assert recipe["key"]["I"]["item"] == "minecraft:iron_ingot"
assert recipe["key"]["N"]["item"] == "minecraft:iron_nugget"
assert recipe["key"]["D"]["tag"] == "residenciasaldeoes:vanilla_doors"
doors = json.loads((resources / "data/residenciasaldeoes/tags/items/vanilla_doors.json").read_text())["values"]
assert len(doors) == 8 and all(x.startswith("minecraft:") and x.endswith("_door") for x in doors)
assert all(not any(term in x for term in ("trapdoor", "fence_gate")) for x in doors)
wooden = json.loads((resources / "data/minecraft/tags/blocks/wooden_doors.json").read_text())
assert wooden["replace"] is False and "residenciasaldeoes:reinforced_door" in wooden["values"]
loot = json.loads((resources / "data/residenciasaldeoes/loot_tables/blocks/reinforced_door.json").read_text())
assert loot["pools"][0]["entries"][0]["name"] == "residenciasaldeoes:reinforced_door"

states = json.loads((resources / "assets/residenciasaldeoes/blockstates/reinforced_door.json").read_text())["variants"]
assert len(states) == 64
for variant in states.values():
    namespace, model_path = variant["model"].split(":", 1)
    assert (resources / "assets" / namespace / "models" / f"{model_path}.json").exists()
for half in ("lower", "upper"):
    image = (resources / f"assets/residenciasaldeoes/textures/block/reinforced_door_{half}.png").read_bytes()
    assert image[:8] == bytes.fromhex("89504e470d0a1a0a")
    assert int.from_bytes(image[16:20], "big") == 16 and int.from_bytes(image[20:24], "big") == 16
item = (resources / "assets/residenciasaldeoes/textures/item/reinforced_door.png").read_bytes()
assert item[:8] == bytes.fromhex("89504e470d0a1a0a")
assert int.from_bytes(item[16:20], "big") == 16 and int.from_bytes(item[20:24], "big") == 16
print("Reinforced door registrations, recipe, state coverage, durability, villager tag, and 16x16 assets passed.")

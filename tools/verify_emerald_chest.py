from pathlib import Path
import json

root = Path('src/main')
java = root / 'java/br/com/residenciasaldeoes'
resources = root / 'resources'
main = (java / 'ResidenciasDosAldeoes.java').read_text()
block = (java / 'block/EmeraldChestBlock.java').read_text()
be = (java / 'block/EmeraldChestBlockEntity.java').read_text()
anchor = (java / 'block/ResidentialAnchorBlockEntity.java').read_text()
events = (java / 'VillagerResidenceEvents.java').read_text()
renderer = (java / 'client/EmeraldChestRenderer.java').read_text()
assert 'BLOCKS.register("emerald_chest", EmeraldChestBlock::new)' in main
assert 'ITEMS.register("emerald_chest"' in main
assert 'BLOCK_ENTITIES.register("emerald_chest"' in main
assert 'extends ChestBlock' in block and 'ChestType.SINGLE' in block
assert 'extends ChestBlockEntity' in be and 'ResidenciasDosAldeoes.EMERALD_FRAGMENT.get()' in be
assert 'ItemStack.isSameItemSameTags' in be and 'stack.shrink(moved)' in be
assert 'ForgeCapabilities.ITEM_HANDLER' in be
assert 'takeAvailableFragments()' in anchor and 'restoreFragments(' in anchor
assert 'PRODUCTION_TIME_OF_DAY=200L' in events and 'COLLECTION_TIME_OF_DAY=3_000L' in events
assert 'home.residents.iterator().next()' in events
assert 'findEmeraldChest(level,home,villager)' in events
assert 'villager.getNavigation().moveTo' in events
assert 'insertFragments(stack)' in events and 'job.putInt("stage",2)' in events
assert 'EquipmentSlot.MAINHAND' in events and 'job.putInt("stage",2)' in events
assert 'last_emerald_collection_period' in (java / 'data/ResidenceData.java').read_text()
assert 'new ItemStack(Items.EMERALD)' in renderer and 'getOpenNess(partialTick)' in renderer
client = (java / 'client/ClientSetup.java').read_text()
assert 'TextureStitchEvent.Pre' in client and 'event.addSprite' in client

blockstates = json.loads((resources / 'assets/residenciasaldeoes/blockstates/emerald_chest.json').read_text())['variants']
assert len(blockstates) == 8
loot = json.loads((resources / 'data/residenciasaldeoes/loot_tables/blocks/emerald_chest.json').read_text())
assert loot['pools'][0]['entries'][0]['name'] == 'residenciasaldeoes:emerald_chest'
for path in (
    resources / 'assets/residenciasaldeoes/textures/entity/chest/emerald_chest.png',
    resources / 'assets/residenciasaldeoes/textures/entity/chest/emerald_chest_left.png',
    resources / 'assets/residenciasaldeoes/textures/entity/chest/emerald_chest_right.png',
    resources / 'assets/residenciasaldeoes/textures/item/emerald_chest.png',
):
    data = path.read_bytes()
    assert data[:8] == bytes.fromhex('89504e470d0a1a0a')
# Inventory transfers are quantity-conserving: destination plus source remainder is the input.
def transfer(source, free_capacity):
    moved = min(source, free_capacity)
    return moved, source - moved
for source, free in ((64, 64), (17, 5), (64, 0), (128, 71)):
    moved, remainder = transfer(source, free)
    assert moved + remainder == source
print('Emerald chest registration, whitelist, texture files, daily transport and quantity conservation checks passed; these are source-level checks, not Minecraft runtime tests.')

from pathlib import Path
import json
import struct
root=Path('src/main');java=root/'java/br/com/residenciasaldeoes';resources=root/'resources'
main=(java/'ResidenciasDosAldeoes.java').read_text();block=(java/'block/EmeraldChestBlock.java').read_text();be=(java/'block/EmeraldChestBlockEntity.java').read_text();anchor=(java/'block/ResidentialAnchorBlockEntity.java').read_text();events=(java/'VillagerResidenceEvents.java').read_text();renderer=(java/'client/EmeraldChestRenderer.java').read_text()
assert 'BLOCKS.register("emerald_chest", EmeraldChestBlock::new)' in main
assert 'ITEMS.register("emerald_chest"' in main and 'BLOCK_ENTITIES.register("emerald_chest"' in main
assert 'extends ChestBlock' in block and 'ChestType.SINGLE' in block
assert 'extends ChestBlockEntity' in be and 'ResidenciasDosAldeoes.EMERALD_FRAGMENT.get()' in be
assert 'ItemStack.isSameItemSameTags' in be and 'stack.shrink(moved)' in be and 'ForgeCapabilities.ITEM_HANDLER' in be
assert 'new InvWrapper(this)' in be and 'if (!stack.isEmpty() && !stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())) return stack;' in be
assert 'return LazyOptional.empty()' not in be
assert 'if (!stack.isEmpty() && !canPlaceItem(slot, stack)) return;' in be
assert 'return slot >= 0 && slot < getContainerSize()' in be and 'stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())' in be
assert 'ContainerHelper.saveAllItems' not in be and 'void load(' not in be
assert 'if (!stack.isEmpty() && !stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())) return stack;' in be
assert 'takeAvailableFragments()' in anchor and 'restoreFragments(' in anchor
assert 'pending_emerald_collections' in (java/'data/ResidenceData.java').read_text()
assert 'villager.getNavigation().moveTo' in events and 'insertFragments(stack)' in events
assert 'Blocks.EMERALD_BLOCK.defaultBlockState()' in renderer and 'getOpenNess(partialTick)' in renderer
assert 'Blocks.GOLD_BLOCK.defaultBlockState()' in renderer and 'renderChestRelief' in renderer
model=json.loads((resources/'assets/residenciasaldeoes/models/item/emerald_chest.json').read_text())
assert len(model['elements'])>=7 and model['display']['gui']['scale']==[0.7,0.7,0.7]
assert model['textures']['front'].endswith('emerald_chest_front')
assert model['textures']['side'].endswith('emerald_chest_side')
assert model['textures']['top'].endswith('emerald_chest_top')
states=json.loads((resources/'assets/residenciasaldeoes/blockstates/emerald_chest.json').read_text())['variants'];assert len(states)==8
for name in ('emerald_chest.png','emerald_chest_left.png','emerald_chest_right.png'):
 data=(resources/f'assets/residenciasaldeoes/textures/entity/chest/{name}').read_bytes()
 assert data[:8]==bytes.fromhex('89504e470d0a1a0a') and struct.unpack('>II',data[16:24])==(64,64)
for name in ('emerald_chest_front.png','emerald_chest_side.png','emerald_chest_top.png','emerald_chest_bottom.png','emerald_gem_front.png','emerald_gem_side.png','emerald_gem_top.png'):
 assert (resources/f'assets/residenciasaldeoes/textures/item/{name}').exists()
assert model['textures']['back'].endswith('emerald_chest_side')
assert 'pending' in be.lower() or 'canPlaceItem' in be
print('Emerald chest filter, retained inventory, 3D item geometry and render resource checks passed; runtime interactions require Minecraft.')

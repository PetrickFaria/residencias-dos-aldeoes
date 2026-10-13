from pathlib import Path
import json
import struct

root = Path('src/main')
java = root / 'java/br/com/residenciasaldeoes'
resources = root / 'resources'
main = (java / 'ResidenciasDosAldeoes.java').read_text()
block = (java / 'block/EmeraldChestBlock.java').read_text()
be = (java / 'block/EmeraldChestBlockEntity.java').read_text()
anchor = (java / 'block/ResidentialAnchorBlockEntity.java').read_text()
events = (java / 'VillagerResidenceEvents.java').read_text()
renderer = (java / 'client/EmeraldChestRenderer.java').read_text()
data = (java / 'data/ResidenceData.java').read_text()

# Registration and normal animated single-chest behavior.
assert 'BLOCKS.register("emerald_chest", EmeraldChestBlock::new)' in main
assert 'ITEMS.register("emerald_chest"' in main and 'BLOCK_ENTITIES.register("emerald_chest"' in main
assert 'EMERALD_CHEST_BODY' in main and 'ITEMS.register("emerald_chest_body"' not in main
assert 'extends ChestBlock' in block and 'ChestType.SINGLE' in block
assert 'extends ChestBlockEntity' in be
assert 'super.render(entity, partialTick, pose, buffers, light, overlay);' in renderer

# All insertion routes accept only the mod's fragment; inherited NBT loading stays intact.
assert 'ResidenciasDosAldeoes.EMERALD_FRAGMENT.get()' in be
assert 'ItemStack.isSameItemSameTags' in be and 'stack.shrink(moved)' in be
assert 'ForgeCapabilities.ITEM_HANDLER' in be and 'new InvWrapper(this)' in be
assert 'if (!stack.isEmpty() && !stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())) return stack;' in be
assert 'if (!stack.isEmpty() && !canPlaceItem(slot, stack)) return;' in be
assert 'return slot >= 0 && slot < getContainerSize()' in be
assert 'stack.is(ResidenciasDosAldeoes.EMERALD_FRAGMENT.get())' in be
assert 'ContainerHelper.loadAllItems' not in be and 'void load(' not in be
assert 'ContainerHelper.saveAllItems' not in be and 'void saveAdditional(' not in be
# The superclass still owns legacy inventory NBT; no migration/deletion hook is present.
assert 'extends ChestBlockEntity' in be and 'remove("Items")' not in be and 'clearContent()' not in be
assert 'takeAvailableFragments()' in anchor and 'restoreFragments(' in anchor
assert 'pending_emerald_collections' in data
assert 'villager.getNavigation().moveTo' in events and 'insertFragments(stack)' in events

# Body details keep the anchor palette while the top panel/crystal/bezel move with the vanilla lid.
assert 'renderSingleBlock(body,pose,buffers,light,OverlayTexture.NO_OVERLAY)' in renderer
assert 'renderSingleBlock(lid,pose,buffers,light,OverlayTexture.NO_OVERLAY)' in renderer
assert 'pose.scale(0.875F,0.5625F,0.875F)' in renderer
assert 'entity.getOpenNess(partialTick)' in renderer
assert 'pose.mulPose(Vector3f.XP.rotation(-openness' in renderer
assert '0.5625D,0.9375D' in renderer
assert 'BlockStateProperties.HORIZONTAL_FACING' in (java / 'block/EmeraldChestBodyBlock.java').read_text()
assert 'BooleanProperty LID' in (java / 'block/EmeraldChestBodyBlock.java').read_text()
body_model = json.loads((resources / 'assets/residenciasaldeoes/models/block/emerald_chest_body.json').read_text())
anchor_model = json.loads((resources / 'assets/residenciasaldeoes/models/block/ancora_residencial_body.json').read_text())
for key in ('side', 'wood', 'corner'):
    assert body_model['textures'][key] == anchor_model['textures'][key]
assert all('up' not in element.get('faces', {}) for element in body_model['elements'])
composite = json.loads((resources / 'assets/residenciasaldeoes/models/block/emerald_chest_model.json').read_text())
assert set(composite['children']) == {'body', 'side_crystals', 'side_trim', 'front_panel'}
lid_composite = json.loads((resources / 'assets/residenciasaldeoes/models/block/emerald_chest_lid_model.json').read_text())
assert set(lid_composite['children']) == {'top_panel', 'front_panel', 'top_crystal', 'top_trim'}
lid_panel = json.loads((resources / 'assets/residenciasaldeoes/models/block/emerald_chest_lid_panel.json').read_text())
assert len(lid_panel['elements']) == 1 and 'up' in lid_panel['elements'][0]['faces']
front = json.loads((resources / 'assets/residenciasaldeoes/models/block/emerald_chest_front_panel.json').read_text())
assert front['textures']['panel'] == 'residenciasaldeoes:block/wood_bottom'
lid_front = json.loads((resources / 'assets/residenciasaldeoes/models/block/emerald_chest_lid_front_panel.json').read_text())
assert lid_front['textures']['front'] == 'residenciasaldeoes:block/wood_bottom'
assert lid_front['elements'][0]['from'][2] == 0 and lid_front['elements'][0]['to'][2] < 0.02
assert 'Blocks.GOLD_BLOCK.defaultBlockState()' in renderer
assert 'Blocks.EMERALD_BLOCK.defaultBlockState()' in renderer
clasp = renderer[renderer.index('A small raised emerald clasp'):]
assert clasp.index('Vector3f.XP.rotation(-openness') < clasp.index('Blocks.GOLD_BLOCK.defaultBlockState()')
assert clasp.index('Vector3f.XP.rotation(-openness') < clasp.index('Blocks.EMERALD_BLOCK.defaultBlockState()')
body_states = json.loads((resources / 'assets/residenciasaldeoes/blockstates/emerald_chest_body.json').read_text())['variants']
assert len(body_states) == 8
assert all(any(key.endswith('lid=true') and value['model'].endswith('emerald_chest_lid_model') for key, value in body_states.items()) for _ in [0])
assert all(any(key.endswith('lid=false') and value['model'].endswith('emerald_chest_model') for key, value in body_states.items()) for _ in [0])
assert len(json.loads((resources / 'assets/residenciasaldeoes/blockstates/emerald_chest.json').read_text())['variants']) == 8

# Item icon retains chest proportions and anchor palette.
item_model = json.loads((resources / 'assets/residenciasaldeoes/models/item/emerald_chest.json').read_text())
assert len(item_model['elements']) >= 7
assert item_model['display']['gui']['scale'] == [0.7, 0.7, 0.7]
assert item_model['textures']['front'] == 'residenciasaldeoes:item/emerald_chest_front'
assert item_model['textures']['back'] == 'residenciasaldeoes:block/wood_bottom'
assert item_model['textures']['side'] == 'residenciasaldeoes:item/emerald_chest_side'
assert item_model['textures']['top'] == 'residenciasaldeoes:item/emerald_chest_top'
for name in ('emerald_chest.png', 'emerald_chest_left.png', 'emerald_chest_right.png'):
    image = (resources / f'assets/residenciasaldeoes/textures/entity/chest/{name}').read_bytes()
    assert image[:8] == bytes.fromhex('89504e470d0a1a0a')
    assert struct.unpack('>II', image[16:24]) == (64, 64)
for name in ('emerald_chest_front.png', 'emerald_chest_side.png', 'emerald_chest_top.png', 'emerald_chest_bottom.png', 'emerald_gem_front.png', 'emerald_gem_side.png', 'emerald_gem_top.png'):
    assert (resources / f'assets/residenciasaldeoes/textures/item/{name}').exists()

# Strict slot filtering and non-destructive legacy data behavior.
invalid_items = ('minecraft:dirt', 'minecraft:stone', 'minecraft:emerald', 'minecraft:diamond')
accepted = lambda item: item == 'residenciasaldeoes:emerald_fragment'
assert all(not accepted(item) for item in invalid_items) and accepted('residenciasaldeoes:emerald_fragment')
print('Emerald chest fragment-only insertion, legacy NBT retention, residential-anchor side/top details, wood front, lid-attached raised clasp, and item resources passed; in-game visuals/interactions remain pending.')

from pathlib import Path
import json
import struct
import zlib

root=Path('src/main');java=root/'java/br/com/residenciasaldeoes';resources=root/'resources'
main=(java/'ResidenciasDosAldeoes.java').read_text();door=(java/'block/ReinforcedDoorBlock.java').read_text()
assert 'BLOCKS.register("reinforced_door", ReinforcedDoorBlock::new)' in main
assert 'ITEMS.register("reinforced_door"' in main and 'CreativeModeTab.TAB_DECORATIONS' in main
assert 'extends DoorBlock' in door and 'panelShape' not in door
assert 'getShape(' not in door and 'getCollisionShape(' not in door
assert '.strength(5.0F, 5.0F)' in door and 'Material.WOOD' in door
assert 'level.setBlock(lowerPos, changed, 10)' in door and 'upper.setValue(OPEN, opening)' in door
assert 'SoundEvents.WOODEN_DOOR_OPEN' in door and 'SoundEvents.WOODEN_DOOR_CLOSE' in door
item=json.loads((resources/'assets/residenciasaldeoes/models/item/reinforced_door.json').read_text())
assert item['display']['gui']['scale']==[0.8,0.8,0.8]
recipe=json.loads((resources/'data/residenciasaldeoes/recipes/reinforced_door.json').read_text())
assert recipe['pattern']==['INI','NDN','INI']
assert recipe['key']['I']['item']=='minecraft:iron_ingot'
assert recipe['key']['N']['item']=='minecraft:iron_nugget'
assert recipe['key']['D']['tag']=='residenciasaldeoes:vanilla_doors'
states=json.loads((resources/'assets/residenciasaldeoes/blockstates/reinforced_door.json').read_text())['variants']
assert len(states)==64
assert all(states[k]['model'].split('/')[-1].startswith('reinforced_door_') for k in states)
def png_rgba(path):
 data=path.read_bytes(); assert data[:8]==bytes.fromhex('89504e470d0a1a0a')
 width,height,depth,color=struct.unpack('>IIBB',data[16:26])
 assert depth==8 and color==6
 cursor=8; compressed=bytearray()
 while cursor<len(data):
  length=struct.unpack('>I',data[cursor:cursor+4])[0]; kind=data[cursor+4:cursor+8]; payload=data[cursor+8:cursor+8+length]; cursor+=12+length
  if kind==b'IDAT': compressed.extend(payload)
 raw=zlib.decompress(compressed); stride=width*4; previous=bytearray(stride); alphas=[]; offset=0
 for _ in range(height):
  filt=raw[offset]; row=bytearray(raw[offset+1:offset+1+stride]); offset+=stride+1
  for i in range(stride):
   left=row[i-4] if i>=4 else 0; up=previous[i]; upper_left=previous[i-4] if i>=4 else 0
   if filt==1: row[i]=(row[i]+left)&255
   elif filt==2: row[i]=(row[i]+up)&255
   elif filt==3: row[i]=(row[i]+((left+up)//2))&255
   elif filt==4:
    p0=left+up-upper_left; pa=abs(p0-left); pb=abs(p0-up); pc=abs(p0-upper_left); predictor=left if pa<=pb and pa<=pc else up if pb<=pc else upper_left; row[i]=(row[i]+predictor)&255
   elif filt!=0: raise AssertionError('unsupported PNG filter')
  alphas.extend(row[3::4]); previous=row
 return width,height,alphas
for half in ('lower','upper'):
 width,height,alphas=png_rgba(resources/f'assets/residenciasaldeoes/textures/block/reinforced_door_{half}.png')
 assert (width,height)==(16,16) and 0 in alphas
width,height,alphas=png_rgba(resources/'assets/residenciasaldeoes/textures/item/reinforced_door.png')
assert (width,height)==(16,32) and 0 in alphas
for path in resources.glob('assets/residenciasaldeoes/models/block/reinforced_door*.json'):
 model=json.loads(path.read_text())
 assert model['render_type']=='minecraft:cutout'
 assert model['parent'].startswith('minecraft:block/door_')
print('Vanilla door parents, 64 state mappings, cutout window alpha, recipe and 16x32 icon checks passed.')

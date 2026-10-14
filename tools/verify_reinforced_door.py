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
assert 'InteractionResult use(' not in door and 'super.use(' not in door
assert 'DoorBlock' in door, 'Use the inherited vanilla door state and interaction behavior.'
item=json.loads((resources/'assets/residenciasaldeoes/models/item/reinforced_door.json').read_text())
assert item['display']['gui']['scale']==[0.75,1.0,1.0]
recipe=json.loads((resources/'data/residenciasaldeoes/recipes/reinforced_door.json').read_text())
assert recipe['pattern']==['INI','NDN','INI']
assert recipe['key']['I']['item']=='minecraft:iron_ingot'
assert recipe['key']['N']['item']=='minecraft:iron_nugget'
assert recipe['key']['D']['tag']=='residenciasaldeoes:vanilla_doors'
states=json.loads((resources/'assets/residenciasaldeoes/blockstates/reinforced_door.json').read_text())['variants']
assert len(states)==64
assert all(states[k]['model'].split('/')[-1].startswith('reinforced_door_') for k in states)
# The base model rotations and hinge-dependent open rotations must match vanilla DoorBlock states.
facing_y={'north':270,'east':0,'south':90,'west':180}
for key,value in states.items():
    parts=dict(piece.split('=') for piece in key.split(','))
    expected=facing_y[parts['facing']]
    if parts['open']=='true':
        expected=(expected+(90 if parts['hinge']=='left' else -90))%360
    assert value.get('y',0)==expected,(key,value.get('y',0),expected)
    if parts['open']=='false':
        expected_model='reinforced_door_bottom' if parts['half']=='lower' and parts['hinge']=='left' else None
        if parts['half']=='upper' and parts['hinge']=='left': expected_model='reinforced_door_top'
        if parts['half']=='lower' and parts['hinge']=='right': expected_model='reinforced_door_bottom_closed_right'
        if parts['half']=='upper' and parts['hinge']=='right': expected_model='reinforced_door_top_closed_right'
        assert value['model'].endswith(expected_model)
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
 assert (width,height)==(64,64) and 0 in alphas
width,height,alphas=png_rgba(resources/'assets/residenciasaldeoes/textures/item/reinforced_door.png')
assert (width,height)==(64,128) and 0 in alphas
for path in resources.glob('assets/residenciasaldeoes/models/block/reinforced_door*.json'):
 model=json.loads(path.read_text())
 assert model['render_type']=='minecraft:cutout'
 assert model['parent'].startswith('minecraft:block/door_')
 assert len(model['elements'])>10
 assert model['elements'][0]['from']==[0,0,0] and model['elements'][0]['to']==[3,16,16]
 assert any(e['from'][0]<0 or e['to'][0]>3 for e in model['elements'][1:])
 assert any(e['faces'].get('north',{}).get('texture')=='#iron' and e['from'][2]<0 for e in model['elements'])
 assert any(e['faces'].get('south',{}).get('texture')=='#iron' and e['to'][2]>16 for e in model['elements'])
 name=path.name
 if 'bottom' in name:
  assert all(e['from'][1]>=-0.02 for e in model['elements'][1:])
  if 'bottom' in name:
   assert any(e['from'][1]==-0.02 for e in model['elements'][1:])
 if 'top' in name:
  assert all(e['to'][1]<=16.02 for e in model['elements'][1:])
  if 'top' in name:
   assert any(e['to'][1]==16.02 for e in model['elements'][1:])
print('Vanilla door states, 64 model mappings, cutout window alpha, subtle top/bottom metal relief, reference-sized GUI icon and recipe checks passed.')

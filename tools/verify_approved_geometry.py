from pathlib import Path
import json,hashlib
root=Path('src/main/resources');expected=json.loads(Path('tools/approved_geometry_1_0_46.json').read_text())
for rel,entry in expected.items():
    p=root/rel
    if 'json' in entry:
        data=json.loads(p.read_text());data.pop('textures',None)
        for child in data.get('children',{}).values():
            if 'parent' in child:child['parent']=child['parent'].replace('emerald_chest_finished_emerald_top_','emerald_top_')
        assert data==entry['json'],rel
    else:assert hashlib.sha256(p.read_bytes()).hexdigest()==entry['sha256'],rel
print('Approved element geometry, OBJ/MTL meshes, state rotations and other models remain identical to 1.0.46.')

for name in ['emerald_top_crystal','emerald_top_trim']:
    a=json.loads((root/'assets/residenciasaldeoes/models/block'/('emerald_chest_finished_'+name+'.json')).read_text());a.pop('textures',None)
    b=json.loads((root/'assets/residenciasaldeoes/models/block'/(name+'.json')).read_text());b.pop('textures',None)
    assert a==b,name

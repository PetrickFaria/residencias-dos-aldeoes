from pathlib import Path
from PIL import Image
import json,shutil

root=Path('src/main/resources/assets/residenciasaldeoes');tex=root/'textures/block';models=root/'models/block'
sources=Path('tools/texture_sources');sources.mkdir(exist_ok=True)
def polished(image,kind):
    source=image.convert('RGBA');out=source.copy();w,h=out.size
    for y in range(h):
        for x in range(w):
            r,g,b,a=source.getpixel((x,y))
            if a==0:continue
            above=source.getpixel((x,max(0,y-1)));difference=(r+g+b)-(above[0]+above[1]+above[2])
            edge=2 if difference>30 else -2 if difference<-30 else 0
            if kind=='wood':shade=edge+(1 if y%5==1 else -1 if y%5==0 else 0)
            elif kind=='green':shade=(1 if x+y<(w+h)//2 else -1)+edge
            else:shade=edge+(2 if (x==1 or y==1) else -1 if (x==w-2 or y==h-2) else 0)
            out.putpixel((x,y),(max(0,min(255,r+shade)),max(0,min(255,g+shade)),max(0,min(255,b+shade)),a))
    return out

for filename in ['reinforced_door_lower.png','reinforced_door_upper.png','reinforced_iron.png','reinforced_rivet.png']:
    original=sources/filename
    if not original.exists():shutil.copy2(tex/filename,original)
    polished(Image.open(original),'wood' if 'door_' in filename else 'metal').save(tex/filename)
original=sources/'reinforced_door_item.png'
if not original.exists():shutil.copy2(root/'textures/item/reinforced_door.png',original)
polished(Image.open(original),'wood').save(root/'textures/item/reinforced_door.png')

names=['wood_trim','corner_trim','wood_bottom','emerald_panel','emerald_top','emerald_facet','emerald_shade','emerald_crystal_light','emerald_crystal_mid','emerald_crystal_dark','emerald_chest_interior']
for name in names:
    kind='wood' if name in ['wood_bottom','emerald_chest_interior'] else 'metal' if name in ['wood_trim','corner_trim'] else 'green'
    polished(Image.open(tex/(name+'.png')),kind).save(tex/('chest_finish_'+name+'.png'))
for name in ['emerald_chest_body','emerald_chest_lid_panel','emerald_chest_clasp','emerald_chest_side_crystals','emerald_chest_side_trim']:
    p=models/(name+'.json');data=json.loads(p.read_text())
    for key,value in data.get('textures',{}).items():
        base=value.rsplit('/',1)[-1]
        if base in names:data['textures'][key]='residenciasaldeoes:block/chest_finish_'+base
    p.write_text(json.dumps(data,indent=2)+'\n')
for original in ['emerald_top_crystal','emerald_top_trim']:
    data=json.loads((models/(original+'.json')).read_text())
    for key,value in data.get('textures',{}).items():
        base=value.rsplit('/',1)[-1]
        if base in names:data['textures'][key]='residenciasaldeoes:block/chest_finish_'+base
    (models/('emerald_chest_finished_'+original+'.json')).write_text(json.dumps(data,indent=2)+'\n')
p=models/'emerald_chest_lid_model.json';data=json.loads(p.read_text())
data['children']['top_crystal']['parent']='residenciasaldeoes:block/emerald_chest_finished_emerald_top_crystal'
data['children']['top_trim']['parent']='residenciasaldeoes:block/emerald_chest_finished_emerald_top_trim'
p.write_text(json.dumps(data,indent=2)+'\n')
print('Subtle finish applied with unchanged sizes and alpha. Shared residential-anchor textures left intact.')

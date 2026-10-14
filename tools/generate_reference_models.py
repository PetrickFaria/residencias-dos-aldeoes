"""Build precise repo-native door relief and chest geometry from the supplied reference layout."""
from pathlib import Path
import json
from PIL import Image, ImageDraw

root=Path('src/main/resources/assets/residenciasaldeoes')
models=root/'models/block'; tex=root/'textures/block'

def write(name,data):
    (models/(name+'.json')).write_text(json.dumps(data,indent=2)+'\n')

def box(lo,hi,texture,faces=None):
    return {'from':lo,'to':hi,'faces':faces or {d:{'uv':[0,0,16,16],'texture':texture} for d in ['north','south','east','west','up','down']}}

# Two glass panes, warm timber, iron perimeter, four raised corner plates and square rivets.
im=Image.new('RGBA',(64,128));d=ImageDraw.Draw(im)
for y in range(128):
    for x in range(64):
        grain=((x//5+y//8)%3)*5+((x*7+y*3)%7)-3
        im.putpixel((x,y),(max(0,155+grain),max(0,91+grain),max(0,35+grain),255))
for y in [6,61,81,122]:d.line((8,y,55,y),fill=(119,65,26,255),width=2)
for rect in [(20,17,43,55),(20,83,43,112)]:
    x0,y0,x1,y1=rect
    d.rectangle((x0-2,y0-2,x1+2,y1+2),fill=(88,48,20,255))
    d.rectangle(rect,fill=(187,225,228,255))
    d.rectangle((x0+2,y0+2,x1-2,y1-2),fill=(0,0,0,0))
    for off in [-15,7,22]:
        for k in range(17):
            px=x0+3+k;py=y1-4-k*2+off
            if x0+2<=px<=x1-2 and y0+2<=py<=y1-2:
                d.rectangle((px,py,px+1,py+1),fill=(196,238,249,255))
d.rectangle((18,61,45,77),outline=(110,58,24,255),width=2)
d.rectangle((21,64,42,74),outline=(191,119,52,255),width=2)
for r in [(0,0,7,127),(56,0,63,127),(0,0,63,7),(0,120,63,127)]:
    d.rectangle(r,fill=(110,110,115,255));d.rectangle(r,outline=(158,157,161,255),width=1)
for x in [0,52]:
    for y in [0,116]:
        d.rectangle((x,y,x+11,y+11),fill=(81,81,87,255));d.rectangle((x+1,y+1,x+10,y+10),fill=(128,127,133,255))
        d.rectangle((x+4,y+4,x+7,y+7),fill=(226,225,225,255));d.line((x+4,y+7,x+7,y+7),fill=(80,79,81,255))
for x in [3,59]:
    for y in [44,92]:d.rectangle((x-1,y-1,x+1,y+1),fill=(225,225,227,255))
d.rectangle((9,59,15,67),fill=(58,58,63,255));d.rectangle((10,60,12,62),fill=(196,194,195,255))
im.crop((0,64,64,128)).save(tex/'reinforced_door_lower.png')
im.crop((0,0,64,64)).save(tex/'reinforced_door_upper.png')
im.save(root/'textures/item/reinforced_door.png')

iron=Image.new('RGBA',(16,16));di=ImageDraw.Draw(iron)
di.rectangle((0,0,15,15),fill=(112,112,118,255));di.rectangle((0,0,15,15),outline=(60,60,66,255))
di.line((1,1,14,1),fill=(182,181,185,255));di.line((1,1,1,14),fill=(155,154,158,255))
di.line((2,14,14,14),fill=(71,71,76,255));iron.save(tex/'reinforced_iron.png')
bolt=Image.new('RGBA',(16,16),(128,127,131,255));db=ImageDraw.Draw(bolt)
db.rectangle((0,0,15,15),outline=(65,64,69,255));db.rectangle((3,3,12,12),fill=(214,213,214,255))
db.line((3,3,12,3),fill=(246,244,242,255));db.line((3,12,12,12),fill=(97,96,100,255));bolt.save(tex/'reinforced_rivet.png')

# Preserve every vanilla state transform and the slab's original 0..3 X thickness.
for p in models.glob('reinforced_door*.json'):
    model=json.loads(p.read_text());parent=model['parent'];upper='top' in parent
    material='#top' if upper else '#bottom'
    elements=[box([0,0,0],[3,16,16],material)]
    # UVs of the two wide faces reproduce the original vanilla parent orientation.
    right='right' in parent;opened=parent.endswith('_open')
    mirrored_east=(not right and not opened) or (right and opened)
    elements[0]['faces']['east']['uv']=[16,0,0,16] if mirrored_east else [0,0,16,16]
    elements[0]['faces']['west']['uv']=[0,0,16,16] if mirrored_east else [16,0,0,16]
    relief=[]
    for z in [0,14.1]:relief.append(([-.18,0,z],[3.18,32,z+1.9],'#iron'))
    for y in [0,30]:relief.append(([-.2,y,0],[3.2,y+2,16],'#iron'))
    for y in [0,29]:
        for z in [0,13]:
            relief.append(([-.42,y,z],[3.42,y+3,z+3],'#iron'))
            for x0,x1 in [(-.62,-.42),(3.42,3.62)]:relief.append(([x0,y+.8,z+.8],[x1,y+2.2,z+2.2],'#bolt'))
    for y in [10.5,22]:
        for z in [.5,14.5]:
            for x0,x1 in [(-.3,-.18),(3.18,3.3)]:relief.append(([x0,y,z],[x1,y+1,z+1],'#bolt'))
    handle_z=12 if right else 2.5
    for x0,x1 in [(-.55,-.05),(3.05,3.55)]:relief.append(([x0,15,handle_z],[x1,17,handle_z+1.7],'#iron'))
    ybase=16 if upper else 0
    for lo,hi,t in relief:
        low=max(lo[1],ybase);high=min(hi[1],ybase+16)
        if high<=low:continue
        elements.append(box([lo[0],low-ybase,lo[2]],[hi[0],high-ybase,hi[2]],t))
    model['textures']['iron']='residenciasaldeoes:block/reinforced_iron'
    model['textures']['bolt']='residenciasaldeoes:block/reinforced_rivet'
    model['elements']=elements;model['ambientocclusion']=False
    p.write_text(json.dumps(model,indent=2)+'\n')

# Retain the approved green side surfaces/gold structures. Only front wood and cavity change.
inside=Image.new('RGBA',(16,16));di=ImageDraw.Draw(inside)
for y in range(16):
    for x in range(16):
        v=((x*5+y*3)%9)-4;inside.putpixel((x,y),(88+v,48+v,21+v,255))
for y in [0,5,10,15]:di.line((0,y,15,y),fill=(45,25,12,255))
inside.save(tex/'emerald_chest_interior.png')
body=json.loads((models/'emerald_chest_body.json').read_text())
body['textures']['inside']='residenciasaldeoes:block/emerald_chest_interior'
body['textures']['frontwood']='residenciasaldeoes:block/wood_bottom'
remaining=[e for e in body['elements'] if not any(f['texture'] in ('#side','#inside','#frontwood') for f in e['faces'].values())]
def wall(lo,hi,outside,inner):
    fs={d:{'uv':[0,0,16,16],'texture':'#side'} for d in ['north','south','east','west','up','down']}
    fs['up']['texture']='#wood';fs[inner]['texture']='#inside'
    if outside=='north':fs['north']['texture']='#frontwood'
    return box(lo,hi,'#side',fs)
floor=box([1,2,1],[15,3.5,15],'#inside');floor['faces']['down']['texture']='#bottom'
body['elements']=[floor,wall([1,3.5,1],[15,13,2.3],'north','south'),
                  wall([1,3.5,13.7],[15,13,15],'south','north'),
                  wall([1,3.5,2.3],[2.3,13,13.7],'west','east'),
                  wall([13.7,3.5,2.3],[15,13,13.7],'east','west')]+remaining
for y in [2.9,12.25]:body['elements'].append(box([2,y,.75],[14,y+.7,1],'#inside'))
write('emerald_chest_body',body)
lid=json.loads((models/'emerald_chest_lid_panel.json').read_text())
lid['textures']['inside']='residenciasaldeoes:block/emerald_chest_interior'
lid['textures']['frontwood']='residenciasaldeoes:block/wood_bottom'
lid['elements'][0]['faces']['north']['texture']='#frontwood'
lid['elements'][0]['faces']['down']['texture']='#inside'
write('emerald_chest_lid_panel',lid)

# A single rectangular faceted emerald and gold bezel, attached to the existing moving lid.
clasp={'parent':'minecraft:block/block','ambientocclusion':False,'textures':{
    'particle':'residenciasaldeoes:block/wood_trim','gold':'residenciasaldeoes:block/wood_trim',
    'shade':'residenciasaldeoes:block/corner_trim','green':'residenciasaldeoes:block/emerald_crystal_mid',
    'light':'residenciasaldeoes:block/emerald_crystal_light','dark':'residenciasaldeoes:block/emerald_crystal_dark'},'elements':[]}
clasp['elements'].append(box([6.6,10.3,-.25],[9.4,13.45,.65],'#shade'))
clasp['elements'].append(box([6.85,10.6,-.5],[9.15,13.2,-.25],'#gold'))
clasp['elements'].append(box([7.3,11.05,-.8],[8.7,12.75,-.5],'#green'))
for lo,hi,t in [([7.3,12.5,-.84],[8.7,12.75,-.8],'#light'),([7.3,11.05,-.84],[7.5,12.5,-.8],'#light'),([8.5,11.05,-.84],[8.7,12.5,-.8],'#dark')]:
    clasp['elements'].append(box(lo,hi,t))
write('emerald_chest_clasp',clasp)
print('Reference door relief, wood chest front, moving emerald clasp and actual open cavity generated.')

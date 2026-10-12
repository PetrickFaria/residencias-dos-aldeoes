from PIL import Image, ImageDraw
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SIZE = 64
# Each model cuboid gets an unmistakable color. Face borders are one atlas pixel
# so the rendered mob exposes where each UV island lands without introducing blur.
COLORS = {
    'head': (238, 56, 56, 255),       # red: base head
    'hat': (192, 72, 215, 255),        # violet: head overlay / hat shell
    'nose': (255, 225, 36, 255),       # yellow: nose
    'body': (45, 108, 245, 255),       # blue: torso
    'jacket': (24, 194, 185, 255),     # cyan: torso outer cube
    'arms': (67, 197, 59, 255),        # green: sleeves / arms
    'arm_bridge': (250, 147, 42, 255), # orange: folded arm connector
    'legs': (244, 83, 174, 255),       # pink: legs
    'hat_rim': (246, 246, 246, 255),   # white: rim accessory geometry
}

def cube_uv(draw, u, v, w, h, d, color, mirror=False):
    # CubeListBuilder's 1.19-era box atlas: top, bottom, and four side faces.
    patches = [
        (u+d,       v,       w, d), # top
        (u+d+w,     v,       w, d), # bottom
        (u,         v+d,     d, h), # right
        (u+d,       v+d,     w, h), # front
        (u+d+w,     v+d,     d, h), # left
        (u+2*d+w,   v+d,     w, h), # back
    ]
    if mirror:
        # Mirror is part-specific; it reverses U winding on the two side faces.
        patches[2], patches[4] = patches[4], patches[2]
    for x, y, width, height in patches:
        # Clipping the outer edge catches accidental UV overflow while retaining the
        # pixels that the model samples inside its actual 64x64 atlas.
        x0, y0, x1, y1 = x, y, x+width-1, y+height-1
        if x0 < 0 or y0 < 0 or x1 >= SIZE or y1 >= SIZE:
            raise ValueError(f'UV fora do atlas: {(x0,y0,x1,y1)}')
        draw.rectangle((x0,y0,x1,y1), fill=color)
        if width > 2 and height > 2:
            draw.rectangle((x0,y0,x1,y1), outline=(20,20,20,255))

im = Image.new('RGBA', (SIZE,SIZE), (0,0,0,0))
d = ImageDraw.Draw(im)
# VillagerModel UV/model cube definitions (vanilla 1.19.2 dimensions).
cube_uv(d, 0, 0, 8, 10, 8, COLORS['head'])
cube_uv(d, 32, 0, 8, 10, 8, COLORS['hat'])
cube_uv(d, 24, 0, 2, 4, 2, COLORS['nose'])
cube_uv(d, 16, 20, 8, 12, 6, COLORS['body'])
cube_uv(d, 0, 38, 8, 18, 6, COLORS['jacket'])
cube_uv(d, 44, 22, 4, 8, 4, COLORS['arms'])
cube_uv(d, 44, 22, 4, 8, 4, COLORS['arms'], mirror=True)
cube_uv(d, 40, 38, 8, 4, 4, COLORS['arm_bridge'])
cube_uv(d, 0, 22, 4, 12, 4, COLORS['legs'])
cube_uv(d, 0, 22, 4, 12, 4, COLORS['legs'], mirror=True)
# The model's separate hat rim UV island (16x16x1).
cube_uv(d, 30, 47, 16, 16, 1, COLORS['hat_rim'])
im.save(ROOT/'docs/villager_uv_diagnostic_64.png')
im.resize((256,256), Image.Resampling.NEAREST).save(ROOT/'docs/villager_uv_diagnostic_256.png')

# Validate the provided skeleton texture is a square PNG in one of the supported
# dimensions and report alpha coverage for comparison in the release notes.
source = Path('/workspace/scratch/fdeb4b9555f1/upload/skeleton-villager-custom(1).png')
if source.exists():
    test = Image.open(source)
    if test.size != (64,64) or test.mode not in ('RGBA','RGB'):
        raise SystemExit(f'Skeleton test texture invalid: size={test.size}, mode={test.mode}')
    print('skeleton-test', test.size, test.mode, 'alpha-extrema=', test.getchannel('A').getextrema() if test.mode=='RGBA' else None)
print('generated', (ROOT/'docs/villager_uv_diagnostic_64.png').stat().st_size, (ROOT/'docs/villager_uv_diagnostic_256.png').stat().st_size)

#!/usr/bin/env python3
"""Build exact vanilla-villager UV references and transparent blank templates."""
from argparse import ArgumentParser
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs/skins"
SCALE = 16  # 64 UV pixels -> 1024 atlas pixels

# Half-open UV boxes on VillagerModel's 64x64 atlas; dimensions are derived from
# the CubeListBuilder texOffs/addBox calls used by the vanilla villager renderer.
REGIONS = [
    ("Cabeça: topo", (16, 0, 24, 8), (82, 168, 118)),
    ("Cabeça: base", (8, 0, 16, 8), (82, 168, 118)),
    ("Cabeça: lado esquerdo", (0, 8, 8, 18), (82, 168, 118)),
    ("Rosto/frente da cabeça", (8, 8, 16, 18), (82, 168, 118)),
    ("Cabeça: lado direito", (16, 8, 24, 18), (82, 168, 118)),
    ("Cabeça: costas", (24, 8, 32, 18), (82, 168, 118)),
    ("Nariz: caixa UV", (24, 0, 32, 6), (240, 166, 90)),
    ("Sobreposição de cabeça/chapéu (deixar transparente sem acessório)", (32, 0, 64, 18), (226, 83, 73)),
    ("Corpo base (8×12×6)", (16, 20, 44, 38), (74, 140, 222)),
    ("Pernas (4×12×4; UV espelhada compartilhada)", (0, 22, 16, 38), (167, 111, 220)),
    ("Jaqueta/manto externo (8×20×6)", (0, 38, 28, 64), (231, 181, 72)),
    ("Braços cruzados (8×4×4)", (40, 38, 64, 46), (87, 200, 197)),
    ("Braços laterais (4×8×4; UV espelhada compartilhada)", (44, 22, 60, 34), (121, 204, 138)),
    ("Aba larga do chapéu (deixar transparente sem acessório)", (30, 47, 64, 64), (226, 83, 73)),
]


def fonts():
    base = Path("/usr/share/fonts/truetype/dejavu")
    try:
        return (ImageFont.truetype(str(base / "DejaVuSans.ttf"), 16),
                ImageFont.truetype(str(base / "DejaVuSans.ttf"), 14),
                ImageFont.truetype(str(base / "DejaVuSans-Bold.ttf"), 26))
    except OSError:
        f = ImageFont.load_default()
        return f, f, f


def render_guide():
    font, small, title_font = fonts()
    atlas = Image.new("RGBA", (1024, 1024), (24, 27, 28, 255))
    ad = ImageDraw.Draw(atlas, "RGBA")
    for i in range(0, 65, 4):
        p = i * SCALE
        color = (74, 80, 82, 130) if i % 8 == 0 else (55, 61, 63, 85)
        ad.line((p, 0, p, 1024), fill=color, width=1)
        ad.line((0, p, 1024, p), fill=color, width=1)
    for name, (x0, y0, x1, y1), rgb in REGIONS:
        alpha = 75 if "deixar transparente" in name else 110
        ad.rectangle((x0*SCALE, y0*SCALE, x1*SCALE-1, y1*SCALE-1),
                     fill=(*rgb, alpha), outline=(*rgb, 235), width=2)
    for label, box, _ in REGIONS:
        x0, y0, x1, y1 = box
        if label == "Cabeça: topo": caption = "TOPO"
        elif label == "Cabeça: base": caption = "BASE"
        elif label == "Cabeça: lado esquerdo" or label == "Cabeça: lado direito": caption = "LADO"
        elif label.startswith("Rosto/"): caption = "ROSTO / FRENTE"
        elif label == "Cabeça: costas": caption = "COSTAS"
        elif label == "Nariz: caixa UV": caption = "NARIZ"
        else: continue
        ad.text((x0*SCALE+3, y0*SCALE+4), caption, font=small, fill=(255,255,255,245),
                stroke_width=1, stroke_fill=(0,0,0,240))
    # This face occupies a sub-rectangle within the nose cube's full UV net.
    ad.rectangle((26*SCALE, 2*SCALE, 28*SCALE-1, 6*SCALE-1),
                 outline=(255, 224, 132, 255), width=3)
    atlas.save(OUTPUT / "villager_uv_guide_1024.png", optimize=True)

    w, h, ox, oy = 1900, 1190, 48, 100
    sheet = Image.new("RGBA", (w, h), (27, 31, 34, 255))
    d = ImageDraw.Draw(sheet, "RGBA")
    d.text((32, 18), "Mapa UV — aldeão vanilla Java 1.19.2", font=title_font, fill=(245,245,238,255))
    d.text((32, 54), "Base 64×64; 256×256 = escala ×4; 1024×1024 = escala ×16.", font=font, fill=(215,220,222,255))
    sheet.alpha_composite(atlas, (ox, oy))
    d = ImageDraw.Draw(sheet, "RGBA")
    y = 100
    for label, _box, rgb in REGIONS:
        d.rectangle((1120,y+2,1138,y+20), fill=(*rgb,255), outline=(245,245,245,220), width=1)
        d.text((1148,y),label,font=font,fill=(240,242,239,255))
        y += 31
    d.line((32,h-78,w-32,h-78),fill=(110,120,122,220),width=1)
    d.text((32,h-66),"Áreas vermelhas de chapéu/aba devem ficar totalmente transparentes quando a skin não tiver esse acessório.",font=font,fill=(255,208,165,255))
    d.text((32,h-38),"As faces coloridas seguem as caixas UV do modelo. EMF/Fresh Animations pode substituir modelo e UVs.",font=small,fill=(207,215,216,255))
    sheet.convert("RGB").save(OUTPUT / "villager_uv_regions_guide.png", quality=95)


def clear_unwanted_hat(source: Path):
    skin = Image.open(source).convert("RGBA")
    if skin.size != (256, 256):
        raise ValueError(f"A textura Creeper precisa ter 256×256; recebida {skin.size}")
    for x0, y0, x1, y1 in ((32,0,64,18),(30,47,64,64)):
        for y in range(y0*4, y1*4):
            for x in range(x0*4, x1*4):
                r,g,b,_ = skin.getpixel((x,y))
                skin.putpixel((x,y),(r,g,b,0))
    skin.save(OUTPUT / "aldeao_creeper_256_corrigido.png", optimize=True)


def main():
    parser = ArgumentParser()
    parser.add_argument("--creeper", type=Path, help="input 256x256 Creeper villager atlas")
    args = parser.parse_args()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for side in (256, 1024):
        Image.new("RGBA", (side,side), (0,0,0,0)).save(OUTPUT / f"villager_skin_template_{side}.png", optimize=True)
    render_guide()
    if args.creeper:
        clear_unwanted_hat(args.creeper)


if __name__ == "__main__":
    main()

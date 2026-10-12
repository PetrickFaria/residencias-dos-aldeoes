#!/usr/bin/env python3
"""Inspect key VillagerModel UV islands in a 64x64 reference PNG."""
from pathlib import Path
import argparse
from PIL import Image

parser = argparse.ArgumentParser()
parser.add_argument("png", nargs="?", default="examples/skeleton-villager-custom-validation.png")
args = parser.parse_args()
image = Image.open(Path(args.png)).convert("RGBA")
if image.size != (64, 64):
    raise SystemExit(f"Expected 64x64, found {image.size}")

regions = {
    "face_front": (8, 8, 16, 18),
    "nose_front": (26, 2, 28, 6),
    "torso_front": (22, 26, 30, 38),
    "coat_front": (6, 44, 14, 62),
}
for name, (x0, y0, x1, y1) in regions.items():
    pixels = [image.getpixel((x, y)) for y in range(y0, y1) for x in range(x0, x1)]
    opaque = sum(alpha > 0 for _, _, _, alpha in pixels)
    print(f"{name}: {opaque}/{len(pixels)} pixels with alpha > 0")

# These are the islands directly relevant to the supplied screenshot: a fully
# opaque face can be overdrawn by the final pass, while the torso has holes.
face = regions["face_front"]
face_pixels = [image.getpixel((x, y)) for y in range(face[1], face[3]) for x in range(face[0], face[2])]
if not all(pixel[3] > 0 for pixel in face_pixels):
    raise SystemExit("Face-front island contains alpha holes; order alone cannot replace every face pixel")
if not any(pixel[3] == 0 for pixel in [image.getpixel((x, y)) for y in range(26, 38) for x in range(22, 30)]):
    raise SystemExit("Expected transparent torso pixels were not found")
print("Static UV check passed: opaque face can cover earlier pixels; transparent torso still reveals prior rendering.")

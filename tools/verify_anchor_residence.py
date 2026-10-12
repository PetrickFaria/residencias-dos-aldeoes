#!/usr/bin/env python3
"""Guard the approved anchor geometry and new material/resident-return rules."""

from hashlib import sha256
import json
from pathlib import Path
import struct

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/residenciasaldeoes"
BLOCK = ASSETS / "models/block"
TEXTURES = ASSETS / "textures/block"

# These OBJ files define the approved shape. Texture/material work must not move vertices.
GEOMETRY_SHA256 = {
    "emerald_side_crystals.obj": "eca3f3002e74e884152b679c0a5522fc00397a05fc7d54cb18ffd6a0c5b8fda7",
    "emerald_sides.obj": "8d472f1db91b1ef58832e5efda60482ab39c9841f85ab548787fa755909ce30f",
    "emerald_top.obj": "9d82ce6e1fa5f37d2884ac4112b2a7c612c597482fc338b623cdb3a634ba8137",
    "emerald_top_trim.obj": "a81793fabee7fad1ded4307dd0e54595a5873fd41d1f537771da18e62a192210",
}


def png_size(path: Path) -> tuple[int, int]:
    data = path.read_bytes()
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        raise AssertionError(f"Invalid PNG header: {path}")
    return struct.unpack(">II", data[16:24])


for name, expected in GEOMETRY_SHA256.items():
    actual = sha256((BLOCK / name).read_bytes()).hexdigest()
    assert actual == expected, f"Approved model geometry changed: {name} ({actual})"

for name in ("emerald_side_trim.json", "emerald_top_trim.json"):
    model = json.loads((BLOCK / name).read_text(encoding="utf-8"))
    assert model["shade_quads"] is False, f"Directional black shading still enabled: {name}"
    assert model["emissive_ambient"] is True, f"Localized ambient light missing: {name}"

for name in ("emerald_sides.mtl", "emerald_top_trim.mtl"):
    materials = (BLOCK / name).read_text(encoding="utf-8")
    assert "Ka 0.55 0.55 0.55" in materials
    assert "Ka 0.36 0.36 0.36" in materials

side = TEXTURES / "emerald_panel.png"
top = TEXTURES / "emerald_top.png"
assert png_size(side) == (16, 16) and png_size(top) == (16, 16)
assert side.read_bytes() == top.read_bytes(), "Top and side panels must use the same pixel pattern and palette"
for name in ("wood_trim.png", "corner_trim.png"):
    assert png_size(TEXTURES / name) == (16, 16), f"Unexpected texture dimensions: {name}"

events = (ROOT / "src/main/java/br/com/residenciasaldeoes/VillagerResidenceEvents.java").read_text(encoding="utf-8")
anchor = (ROOT / "src/main/java/br/com/residenciasaldeoes/block/ResidentialAnchorBlockEntity.java").read_text(encoding="utf-8")
assert "RETURN_TIMEOUT_TICKS=160L" in events and "RETURN_RETRY_TICKS=20L" in events
assert "elapsed>=RETURN_TIMEOUT_TICKS" in events
assert "level.noCollision(villager" in events and "floor.isFaceSturdy" in events
assert "at.getFluidState().isEmpty()" in events and "above.getFluidState().isEmpty()" in events
assert "onResidenceSizeChanged(sl,home,data)" in anchor
assert "villager.stopSleeping()" in events and "data.releaseBed(id)" in events
assert "addRegionTicket" not in events, "Resident recovery must not force-load chunks"

print("Anchor checks passed: geometry hashes, green panel parity, gold trim lighting, safe return timer, bed wake-up, and no forced chunk loading.")

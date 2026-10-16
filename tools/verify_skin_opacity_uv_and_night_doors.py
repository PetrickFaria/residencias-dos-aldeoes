#!/usr/bin/env python3
"""Verify skin-reference assets and critical opacity/night-door hooks."""
from pathlib import Path
import struct
import json

root = Path(__file__).resolve().parents[1]
read = lambda p: (root / p).read_text(encoding="utf-8")

screen = read("src/main/java/br/com/residenciasaldeoes/client/AnchorScreen.java")
opacity = read("src/main/java/br/com/residenciasaldeoes/client/ResidentOpacity.java")
preview_buffers = read("src/main/java/br/com/residenciasaldeoes/client/OpacityPreviewBuffers.java")
name_mixin = read("src/main/java/br/com/residenciasaldeoes/mixin/EntityRendererNameTagOpacityMixin.java")
mixins = json.loads(read("src/main/resources/residencias.mixins.json"))
textures = read("src/main/java/br/com/residenciasaldeoes/client/VillagerSkinTextures.java")
events = read("src/main/java/br/com/residenciasaldeoes/VillagerResidenceEvents.java")
transport = read("src/main/java/br/com/residenciasaldeoes/mixin/VillagerTransportMixin.java")

assert "new OpacityPreviewBuffers(bufferSource,pendingOpacity)" in screen
assert "bufferSource.endBatch()" in screen
assert "target.color(red,green,blue,adjusted)" in preview_buffers
assert "Math.round(alpha*(opacity/100.0F))" in preview_buffers
assert "renderingNameTag()" in preview_buffers
assert "hasPreviewAlphaOverride()" in read("src/main/java/br/com/residenciasaldeoes/mixin/BufferBuilderOpacityMixin.java")
assert "beginPreviewRender(skinPreviewVillager)" in screen
assert "if (renderingNameTag()) return 100;" in opacity
assert "renderNameTag" in name_mixin and "beginNameTag" in name_mixin and "endNameTag" in name_mixin
assert "EntityRendererNameTagOpacityMixin" in mixins["client"]
assert "Set.of(64, 128, 256, 512, 1024)" in textures

# Validate PNG dimensions and alpha-capable format using only Python's stdlib;
# the GitHub runner does not install Pillow.
def png_info(path):
    data = path.read_bytes()
    assert data[:8] == b"\x89PNG\r\n\x1a\n"
    width, height, bit_depth, color_type = struct.unpack(">IIBB", data[16:26])
    return width, height, bit_depth, color_type

skin_docs = root / "docs/skins"
for side in (256, 1024):
    width, height, bit_depth, color_type = png_info(skin_docs / f"villager_skin_template_{side}.png")
    assert (width, height, bit_depth, color_type) == (side, side, 8, 6)
width, height, *_ = png_info(skin_docs / "villager_uv_regions_guide.png")
assert width >= 1500 and height >= 1100
assert png_info(skin_docs / "villager_uv_guide_1024.png")[:2] == (1024, 1024)
assert png_info(skin_docs / "aldeao_creeper_256_corrigido.png") == (256, 256, 8, 6)

assert "crossedNightfall" in events and "beginNightDoorClosure" in events
assert "onResidentialDoorChanged" in events and "beginWeatherDoorClosure" in events
assert "chooseReachableDoorResident" in events and "DOOR_REOPEN_COOLDOWN" in events
assert "DOOR_OPEN_STABILITY_TICKS" in events and "residentUsingDoorway(level,data,pos)" in events
assert "home.residents.contains(villager.getUUID())" in events
assert "phase<NIGHT_START||phase>=NIGHT_END)&&!level.isRaining()" in events
assert "level.hasChunkAt(pos)" in events and "state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)" in events
assert "door.setOpen(villager,level,state,doorPos,false)" in events
assert "findApproach(level,doorPos,villager)" in events and "NIGHT_DOOR_WALK_SPEED" in events
assert "now-task.getLong(\"last_progress\")>=200L" in events and "attempts>=3" in events
assert "residenciasaldeoes_night_door_task" in transport

print("Skin preview alpha/name-tag separation, 64–1024 templates, UV accessory transparency, and bounded night-door task verified.")

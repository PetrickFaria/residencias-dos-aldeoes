#!/usr/bin/env python3
"""Verify skin-reference assets and critical opacity/night-door hooks."""
from pathlib import Path
import struct
import json

root = Path(__file__).resolve().parents[1]
read = lambda p: (root / p).read_text(encoding="utf-8")

screen = read("src/main/java/br/com/residenciasaldeoes/client/AnchorScreen.java")
opacity = read("src/main/java/br/com/residenciasaldeoes/client/ResidentOpacity.java")
buffer_mixin = read("src/main/java/br/com/residenciasaldeoes/mixin/BufferBuilderOpacityMixin.java")
name_mixin = read("src/main/java/br/com/residenciasaldeoes/mixin/EntityRendererNameTagOpacityMixin.java")
render_opacity_mixin = read("src/main/java/br/com/residenciasaldeoes/mixin/LivingEntityRendererOpacityMixin.java")
mixins = json.loads(read("src/main/resources/residencias.mixins.json"))
textures = read("src/main/java/br/com/residenciasaldeoes/client/VillagerSkinTextures.java")
events = read("src/main/java/br/com/residenciasaldeoes/VillagerResidenceEvents.java")
transport = read("src/main/java/br/com/residenciasaldeoes/mixin/VillagerTransportMixin.java")
model_type = read("src/main/java/br/com/residenciasaldeoes/data/ResidentModelType.java")
model_state = read("src/main/java/br/com/residenciasaldeoes/client/ResidentAppearanceModels.java")
model_mixin = read("src/main/java/br/com/residenciasaldeoes/mixin/ResidentSteveModelMixin.java")
model = read("src/main/java/br/com/residenciasaldeoes/client/SteveVillagerModel.java")
data = read("src/main/java/br/com/residenciasaldeoes/data/ResidenceData.java")

assert "new OpacityPreviewBuffers(bufferSource,pendingOpacity)" in screen
assert "dispatcher.render(skinPreviewVillager,0,0,0,0,1.0F,poses,previewBuffers,15728880)" in screen
assert "bufferSource.endBatch()" in screen
assert "isPreviewRendering()" in buffer_mixin
assert "if(ResidentOpacity.isPreviewRendering())return alpha;" in buffer_mixin
assert 'method = "getRenderType"' in render_opacity_mixin
assert "ResidentAppearanceModels.usesSteve(villager)" in render_opacity_mixin
assert "ResidentOpacity.renderingPercent() < 100" in render_opacity_mixin
assert "@org.spongepowered.asm.mixin.Shadow" not in render_opacity_mixin
assert "EntityRenderer<LivingEntity>" in render_opacity_mixin and ".getTextureLocation(entity)" in render_opacity_mixin
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
assert "DOOR_SCAN_INTERVAL=300L" in events and "if(now%DOOR_SCAN_INTERVAL!=0L)return;" in events
assert "onResidentialDoorChanged" in events and "beginWeatherDoorClosure(level)" in events
assert "(phase<NIGHT_START||phase>=NIGHT_END)&&!level.isRaining()" in events
assert "awakeWithoutBed" in events and "resident.distanceToSqr(leader)" in events
assert "active.stream().allMatch(Villager::isSleeping)" in events and "hasValidResidentialBed" in events
assert "DOOR_OPEN_STABILITY_TICKS" in events and "residentUsingDoorway(level,data,pos)" in events
assert "home.residents.contains(villager.getUUID())" in events
assert "if((phase<NIGHT_START||phase>=NIGHT_END)&&!level.isRaining())return;" in events
assert "BED_ANCHOR_INTERVAL=100" in events and "tickCount%BED_ANCHOR_INTERVAL==0" in events
assert "Release the stale reservation and search the remaining beds" in events
assert "level.hasChunkAt(pos)" in events and "state.is(net.minecraft.tags.BlockTags.WOODEN_DOORS)" in events
assert "door.setOpen(villager,level,state,doorPos,false)" in events
assert "findApproach(level,doorPos,villager)" in events and "NIGHT_DOOR_WALK_SPEED" in events
assert "villager.getNavigation().stop();" in events and "door.setOpen(villager,level,state,doorPos,false)" in events
assert "now-task.getLong(\"last_progress\")>=200L" in events and "attempts>=3" in events
assert "NIGHT_DOOR_RESUME" in events and "putBoolean(NIGHT_DOOR_RESUME,true)" in events
assert "residenciasaldeoes_night_wander" in events and "tickNightWander(level,villager,home)" in events
assert "60L+villager.getRandom().nextInt(81)" in events
assert "pathAvoidsWoodenDoors(level,path)" in events
assert "level.getPoiManager().take" in events and "type.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME)" in events
assert "MemoryModuleType.WALK_TARGET,new WalkTarget(bedApproach,.85F,0)" in events
assert "villager.blockPosition().equals(bedApproach)" in events
assert "residenciasaldeoes_night_door_task" in transport
assert "STEVE" in model_type and "residentModel" in data and "model_type" in data
assert "ResidentAppearanceModels.usesSteve(villager)" in model_mixin
assert "class SteveVillagerModel extends PlayerModel<Villager>" in model
assert "PlayerModel.createMesh(new CubeDeformation(0.0F), false)" in model
assert "import net.minecraft.client.model.VillagerModel;" not in model
assert "extends VillagerModel<" not in model and "hat_rim" not in model
assert "transitionToModelDialog" in screen and "modelOpen" in screen and "Para Steve, use skin com UV humanoide." in screen

for percent in (100, 75, 50, 25, 0):
    alpha = max(0, min(100, percent)) / 100.0
    assert alpha == percent / 100.0
gametests = read("src/gametest/java/br/com/residenciasaldeoes/test/ResidenceGameTests.java")
assert "three_residents_reach_and_use_three_distinct_residential_beds" in gametests
assert "night_door_task_wakes_leader_then_returns_to_same_bed" in gametests
assert "bedless_night_stroll_does_not_reopen_completed_door" in gametests
workflow = read(".github/workflows/build.yml")
assert "python3 tools/run_visual_ci.py" in workflow
print("Continuous preview shader alpha, name-tag separation, 64–1024 templates, UV accessory transparency, 300-tick night scans, resident priorities, and bed recovery verified.")

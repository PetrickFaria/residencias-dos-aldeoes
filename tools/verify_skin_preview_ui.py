from pathlib import Path
import math

screen = Path("src/main/java/br/com/residenciasaldeoes/client/AnchorScreen.java").read_text()
residence = Path("src/main/java/br/com/residenciasaldeoes/data/ResidenceData.java").read_text()

# Skin selection has one mode and does not share the residence-unlink command.
for obsolete in ("skinModeButtons", "rebuildSkinModeButtons", "pendingSkinMode"):
    assert obsolete not in screen, f"Obsolete mode control/state remains: {obsolete}"
confirm = screen.split("private void confirmSkin(){", 1)[1].split("\n    private void closeSkinDialog()", 1)[0]
assert "ModNetwork.assignSkin(" in confirm
assert "ModNetwork.unlinkResident(" not in confirm
assert "ModNetwork.unlinkAll(" not in confirm

# The modal consumes clicks and dispatches only its own controls.
modal = screen.split("if(skinOpen){", 1)[1].split("\n        if(button==0&&hasScrollbar()", 1)[0]
assert "dispatchSkinDialogClick(x,y,button)" in modal
assert "return super.mouseClicked(x,y,button)" not in modal
assert "skinPreviewPitch=Math.max(-90.0F,Math.min(90.0F" in screen
assert "skinPreviewYaw=(skinPreviewYaw+" in screen
assert "setYRot(180)" in screen
assert "resetSkinPreviewView(){skinPreviewYaw=0.0F;skinPreviewPitch=0.0F;}" in screen
assert "VillagerSkinTextures.receive(skinPreviewVillager.getUUID(),pendingSkin,png,SkinApplicationMode.COMPLETE)" in screen
assert "new ResidentSkin(skinKey,payload,SkinApplicationMode.COMPLETE)" in residence

# The projected bounding-box fit remains inside the 59x89 preview at every yaw/pitch.
vw, vh = 59, 89
model_w, model_h = 0.8, 1.9
base_scale = min(36, (vw - 8) / model_w, (vh - 12) / model_h)
for yaw_deg in range(-360, 361, 5):
    y = math.radians(yaw_deg)
    half_w = half_d = model_w / 2
    projected_half_w = abs(math.cos(y))*half_w + abs(math.sin(y))*half_d
    projected_depth = abs(math.sin(y))*half_w + abs(math.cos(y))*half_d
    for pitch_deg in range(-90, 91, 5):
        p = math.radians(pitch_deg)
        projected_half_h = abs(math.cos(p))*model_h/2 + abs(math.sin(p))*projected_depth
        scale = max(1, min(base_scale, math.floor(min((vw-8)/(2*projected_half_w), (vh-12)/(2*projected_half_h)))))
        assert 2*projected_half_w*scale <= vw - 8 + 1e-6
        assert 2*projected_half_h*scale <= vh - 12 + 1e-6

print("Skin UI source invariants and 360°/vertical viewport-fit checks passed.")

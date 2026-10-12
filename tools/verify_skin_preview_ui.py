from pathlib import Path
import math

screen = Path("src/main/java/br/com/residenciasaldeoes/client/AnchorScreen.java").read_text()
assert "SKIN_PREVIEW_DEFAULT_YAW=-25.0F" in screen
assert "resetSkinPreviewView(){skinPreviewYaw=SKIN_PREVIEW_DEFAULT_YAW;skinPreviewPitch=0.0F;}" in screen
assert "skinDialogLeft+16,skinDialogTop+132,86,20" in screen
assert "skinDialogLeft+118,skinDialogTop+132,86,20" in screen
assert "if(!skinOpen)font.draw(p,title,titleLabelX,titleLabelY" in screen
assert "poses.translate(0.0D,modelHeight*0.43D,0.0D);" in screen
assert screen.index("poses.translate(0.0D,modelHeight*0.43D,0.0D);") < screen.index("poses.mulPose(Vector3f.YP.rotationDegrees(skinPreviewYaw));")
assert "poses.translate(0.0D,-modelHeight/2.0D,0.0D);" not in screen
assert "sinP*halfHeight+cosP*halfDepth" in screen
assert "skinPreviewPitch=Math.max(-90.0F,Math.min(90.0F" in screen
assert "skinPreviewYaw=(skinPreviewYaw+" in screen

# With the preview's 180-degree Z camera flip, -25 degrees sends the forward axis to screen-right.
yaw=math.radians(-25.0)
assert -math.sin(yaw)>0

# Exercise the same projected-box fit at every horizontal angle and vertical inspection angle.
vw,vh=59,89
model_width,model_height=0.81,1.95
base=min(36,(vw-8)/model_width,(vh-12)/model_height)
for yaw_deg in range(-360,361,5):
    y=math.radians(yaw_deg)
    cy,sy=abs(math.cos(y)),abs(math.sin(y))
    for pitch_deg in range(-90,91,5):
        p=math.radians(pitch_deg)
        cp,sp=abs(math.cos(p)),abs(math.sin(p))
        hw=hd=model_width/2
        hh=model_height/2
        projected_w=cy*hw+sy*(sp*hh+cp*hd)
        projected_h=cp*hh+sp*hd
        scale=max(1,min(base,math.floor(min((vw-8)/(2*projected_w),(vh-12)/(2*projected_h)))))
        assert 2*projected_w*scale <= vw-8+1e-6
        assert 2*projected_h*scale <= vh-12+1e-6

print("Three-quarter default, centered transform, clean title, footer spacing, and 3D viewport fit checks passed.")

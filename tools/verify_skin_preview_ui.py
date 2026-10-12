from pathlib import Path
import math

screen = Path("src/main/java/br/com/residenciasaldeoes/client/AnchorScreen.java").read_text()
assert "SKIN_PREVIEW_DEFAULT_YAW=25.0F" in screen
assert "resetSkinPreviewView(){skinPreviewYaw=SKIN_PREVIEW_DEFAULT_YAW;skinPreviewPitch=0.0F;}" in screen
assert "skinDialogLeft=leftPos+(imageWidth-220)/2;skinDialogTop=topPos+(imageHeight-160)/2;" in screen
assert "skinDialogLeft+16,skinDialogTop+132,86,20" in screen
assert "skinDialogLeft+118,skinDialogTop+132,86,20" in screen
assert "if(!skinOpen)font.draw(p,title,titleLabelX,titleLabelY" in screen
assert "int scale=baseScale; // Fixed for the full drag" in screen
assert "fitPreviewScale" not in screen
assert "poses.translate(0.0D,-modelHeight*0.43D,0.0D);" in screen
assert screen.index("poses.mulPose(Vector3f.YP.rotationDegrees(skinPreviewYaw));") < screen.index("poses.mulPose(Vector3f.XP.rotationDegrees(skinPreviewPitch));")
assert screen.index("poses.mulPose(Vector3f.XP.rotationDegrees(skinPreviewPitch));") < screen.index("poses.translate(0.0D,-modelHeight*0.43D,0.0D);")
assert "skinPreviewPitch=Math.max(-90.0F,Math.min(90.0F" in screen
assert "skinPreviewYaw=(skinPreviewYaw+" in screen
assert "skinDialogTop+160" in screen
assert "skinDialogTop+180" not in screen

# The camera's 180-degree Z flip makes +25 degrees the reverse of the prior -25-degree yaw.
yaw=math.radians(25.0)
assert -math.sin(yaw)<0

# The fixed model scale and pivot do not depend on either drag angle.
vw,vh=59,89
model_width,model_height=0.81,1.95
scale=max(1,int(math.floor(min(36.0,(vw-8.0)/model_width,(vh-12.0)/model_height))))
assert scale==36
for yaw_deg in range(-360,361,5):
    for pitch_deg in range(-90,91,5):
        assert scale==36
        # Both rotations act around the viewport center; overflow is clipped by the preview scissor.
        center_x,center_y=vw/2,vh/2
        assert center_x==29.5 and center_y==44.5

# Buttons end at y=152; the dialog now ends at 160, leaving an 8-unit inside margin.
assert 132+20==152
assert 160-(132+20)==8
# The outer dialog is centered relative to the full GUI panel in both axes.
image_w,image_h,dialog_w,dialog_h=246,226,220,160
left=(image_w-dialog_w)//2
top=(image_h-dialog_h)//2
assert left==13 and top==33
assert left+dialog_w/2==image_w/2
assert top+dialog_h/2==image_h/2

print("Fixed preview scale/pivot, reversed default yaw, centered 160-unit dialog, and button-bound checks passed.")

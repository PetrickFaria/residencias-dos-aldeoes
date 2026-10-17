#!/usr/bin/env python3
"""Guard the current skin-dialog layout and its live mouse/render paths."""
from pathlib import Path
import math

screen=Path("src/main/java/br/com/residenciasaldeoes/client/AnchorScreen.java").read_text(encoding="utf-8")
opacity=Path("src/main/java/br/com/residenciasaldeoes/client/ResidentOpacity.java").read_text(encoding="utf-8")
buffer_mixin=Path("src/main/java/br/com/residenciasaldeoes/mixin/BufferBuilderOpacityMixin.java").read_text(encoding="utf-8")
preview_buffers=Path("src/main/java/br/com/residenciasaldeoes/client/OpacityPreviewBuffers.java").read_text(encoding="utf-8")

assert "skinDialogLeft=leftPos+(imageWidth-220)/2;skinDialogTop=topPos+(imageHeight-190)/2;" in screen
assert "skinOpacitySlider=new OpacitySlider(skinDialogLeft+25,skinDialogTop+134);" in screen
assert "super(x,y,170,20,Component.empty(),pendingOpacity/100.0D)" in screen
assert "skinDialogLeft+16,skinDialogTop+164,86,20" in screen
assert "skinDialogLeft+118,skinDialogTop+164,86,20" in screen
assert "skinDialogLeft+153,skinDialogTop+112,60,14" in screen

click=screen[screen.index("@Override public boolean mouseClicked(double x,double y,int button)"):screen.index("private boolean dispatchSkinDialogClick")]
drag=screen[screen.index("@Override public boolean mouseDragged"):screen.index("@Override public boolean mouseScrolled")]
assert click.index("draggingSkinPreview=true") < click.index("skinOpacitySlider.mouseClicked")
assert "draggingOpacitySlider=true" in click
assert "if(draggingSkinPreview&&button==0)" in drag
assert drag.index("draggingSkinPreview&&button==0") < drag.index("draggingOpacitySlider")
assert "skinPreviewYaw=(skinPreviewYaw+" in drag
assert "skinPreviewPitch=Math.max(-90.0F,Math.min(90.0F" in drag
assert "skinOpacitySlider.mouseDragged(x,y,button,dx,dy)" in drag

assert "ResidentOpacity.set(skinPreviewVillager.getUUID(),pendingOpacity)" in screen
assert "new OpacityPreviewBuffers(bufferSource,pendingOpacity)" in screen
assert "adjusted=Math.max(0,Math.min(255,Math.round(alpha*(clamped/100.0F))))" in preview_buffers
assert "isPreviewRendering()" in buffer_mixin
assert "if(ResidentOpacity.isPreviewRendering())return alpha;" in buffer_mixin
assert "bufferSource.endBatch()" in screen
assert "ResidentOpacity.beginPreviewRender(skinPreviewVillager)" in screen
assert "ResidentOpacity.endPreviewRender()" in screen
assert "private static final ThreadLocal<Integer> PREVIEW_RENDERING" in opacity
assert "if (preview != null) return preview;" in opacity
assert "skinDialogLeft+SKIN_PREVIEW_LEFT" in click

# The preview viewport still uses one fixed scale and clips the rotated mesh.
assert "int scale=baseScale; // Fixed for the full drag" in screen
assert "RenderSystem.enableScissor(sx,minecraft.getWindow().getHeight()-ey,ex-sx,ey-sy);" in screen
assert "fitPreviewScale" not in screen
image_w,image_h,dialog_w,dialog_h=246,226,220,190
assert (image_w-dialog_w)//2==13 and (image_h-dialog_h)//2==18
assert (image_w-dialog_w)//2+dialog_w/2==image_w/2
assert (image_h-dialog_h)//2+dialog_h/2==image_h/2

print("Skin dialog placement, slider drag capture, preview rotation, and live alpha render path verified.")

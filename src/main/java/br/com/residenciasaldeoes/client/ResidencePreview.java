package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.world.phys.AABB;
import java.util.List;

@Mod.EventBusSubscriber(modid="residenciasaldeoes",value=Dist.CLIENT)
public final class ResidencePreview {
    private static List<ModNetwork.PreviewEntry> entries=List.of();
    private ResidencePreview(){}
    public static void setEntries(List<ModNetwork.PreviewEntry> next){entries=List.copyOf(next);}
    static boolean shouldRender(boolean permanentEnabled,boolean playerInside){return permanentEnabled||playerInside;}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||entries.isEmpty())return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();RenderSystem.lineWidth(3.0F);
        event.getPoseStack().pushPose();var cam=event.getCamera().getPosition();event.getPoseStack().translate(-cam.x,-cam.y,-cam.z);
        RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableCull();RenderSystem.depthMask(false);RenderSystem.setShader(GameRenderer::getPositionColorShader);
        for(var e:entries){
            if(!mc.level.dimension().location().equals(e.dimension()))continue;
            BlockPos pos=e.pos();int r=e.size()/2;
            AABB box=new AABB(pos.getX()-r,pos.getY()+1,pos.getZ()-r,pos.getX()+r+1,pos.getY()+1+e.size(),pos.getZ()+r+1);
            double renderDistance=mc.options.renderDistance().get()*16.0+16.0;
            if(cam.distanceToSqr(pos.getX()+.5,pos.getY()+1+e.size()*.5,pos.getZ()+.5)>renderDistance*renderDistance)continue;
            boolean playerInside=mc.player.getX()>=box.minX&&mc.player.getX()<box.maxX&&mc.player.getY()>=box.minY&&mc.player.getY()<box.maxY&&mc.player.getZ()>=box.minZ&&mc.player.getZ()<box.maxZ;
            if(!shouldRender(e.permanent(),playerInside))continue;
            BufferBuilder fill=Tesselator.getInstance().getBuilder();fill.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION_COLOR);var matrix=event.getPoseStack().last().pose();float alpha=0.035F;
            planeXZ(fill,matrix,box.minY,box.minX,box.maxX,box.minZ,box.maxZ,alpha);planeXZ(fill,matrix,box.maxY,box.minX,box.maxX,box.minZ,box.maxZ,alpha);
            planeYZ(fill,matrix,box.minX,box.minY,box.maxY,box.minZ,box.maxZ,alpha);planeYZ(fill,matrix,box.maxX,box.minY,box.maxY,box.minZ,box.maxZ,alpha);
            planeXY(fill,matrix,box.minZ,box.minX,box.maxX,box.minY,box.maxY,alpha);planeXY(fill,matrix,box.maxZ,box.minX,box.maxX,box.minY,box.maxY,alpha);Tesselator.getInstance().end();
            LevelRenderer.renderLineBox(event.getPoseStack(),buffers.getBuffer(RenderType.lines()),box,0.08F,0.88F,0.42F,0.95F);
        }
        buffers.endBatch(RenderType.lines());RenderSystem.depthMask(true);RenderSystem.enableCull();RenderSystem.disableBlend();event.getPoseStack().popPose();RenderSystem.lineWidth(1.0F);
    }
    private static void planeXZ(com.mojang.blaze3d.vertex.VertexConsumer o,Matrix4f m,double y,double x0,double x1,double z0,double z1,float a){vertex(o,m,x0,y,z0,a);vertex(o,m,x1,y,z0,a);vertex(o,m,x1,y,z1,a);vertex(o,m,x0,y,z1,a);}
    private static void planeYZ(com.mojang.blaze3d.vertex.VertexConsumer o,Matrix4f m,double x,double y0,double y1,double z0,double z1,float a){vertex(o,m,x,y0,z0,a);vertex(o,m,x,y1,z0,a);vertex(o,m,x,y1,z1,a);vertex(o,m,x,y0,z1,a);}
    private static void planeXY(com.mojang.blaze3d.vertex.VertexConsumer o,Matrix4f m,double z,double x0,double x1,double y0,double y1,float a){vertex(o,m,x0,y0,z,a);vertex(o,m,x1,y0,z,a);vertex(o,m,x1,y1,z,a);vertex(o,m,x0,y1,z,a);}
    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,Matrix4f pose,double x,double y,double z,float a){out.vertex(pose,(float)x,(float)y,(float)z).color(0.08F,0.88F,0.42F,a).endVertex();}
}

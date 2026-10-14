package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
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
        MultiBufferSource.BufferSource buffers=mc.renderBuffers().bufferSource();
        var pose=event.getPoseStack();var cam=event.getCamera().getPosition();
        for(var e:entries){
            if(!mc.level.dimension().location().equals(e.dimension()))continue;
            BlockPos pos=e.pos();int r=e.size()/2;
            double renderDistance=mc.options.renderDistance().get()*16.0+16.0;
            if(cam.distanceToSqr(pos.getX()+.5,pos.getY()+1+e.size()*.5,pos.getZ()+.5)>renderDistance*renderDistance)continue;
            pose.pushPose();
            pose.translate(pos.getX()-cam.x,pos.getY()-cam.y,pos.getZ()-cam.z);
            // Local bounds and a private pose scope prevent one residence from
            // inheriting another entry's world/camera transform.
            AABB box=new AABB(-r,1,-r,r+1,1+e.size(),r+1);
            double playerX=mc.player.getX()-pos.getX(),playerY=mc.player.getY()-pos.getY(),playerZ=mc.player.getZ()-pos.getZ();
            boolean playerInside=playerX>=box.minX&&playerX<box.maxX&&playerY>=box.minY&&playerY<box.maxY&&playerZ>=box.minZ&&playerZ<box.maxZ;
            if(shouldRender(e.permanent(),playerInside)){
                var matrix=pose.last().pose();float alpha=0.035F;
                var fill=buffers.getBuffer(RenderType.debugQuads());
                planeXZ(fill,matrix,box.minY,box.minX,box.maxX,box.minZ,box.maxZ,alpha);planeXZ(fill,matrix,box.maxY,box.minX,box.maxX,box.minZ,box.maxZ,alpha);
                planeYZ(fill,matrix,box.minX,box.minY,box.maxY,box.minZ,box.maxZ,alpha);planeYZ(fill,matrix,box.maxX,box.minY,box.maxY,box.minZ,box.maxZ,alpha);
                planeXY(fill,matrix,box.minZ,box.minX,box.maxX,box.minY,box.maxY,alpha);planeXY(fill,matrix,box.maxZ,box.minX,box.maxX,box.minY,box.maxY,alpha);
                LevelRenderer.renderLineBox(pose,buffers.getBuffer(RenderType.lines()),box,0.08F,0.88F,0.42F,0.95F);
            }
            pose.popPose();
        }
        buffers.endBatch(RenderType.debugQuads());buffers.endBatch(RenderType.lines());
    }
    private static void planeXZ(com.mojang.blaze3d.vertex.VertexConsumer o,Matrix4f m,double y,double x0,double x1,double z0,double z1,float a){vertex(o,m,x0,y,z0,a);vertex(o,m,x1,y,z0,a);vertex(o,m,x1,y,z1,a);vertex(o,m,x0,y,z1,a);}
    private static void planeYZ(com.mojang.blaze3d.vertex.VertexConsumer o,Matrix4f m,double x,double y0,double y1,double z0,double z1,float a){vertex(o,m,x,y0,z0,a);vertex(o,m,x,y1,z0,a);vertex(o,m,x,y1,z1,a);vertex(o,m,x,y0,z1,a);}
    private static void planeXY(com.mojang.blaze3d.vertex.VertexConsumer o,Matrix4f m,double z,double x0,double x1,double y0,double y1,float a){vertex(o,m,x0,y0,z,a);vertex(o,m,x1,y0,z,a);vertex(o,m,x1,y1,z,a);vertex(o,m,x0,y1,z,a);}
    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,Matrix4f pose,double x,double y,double z,float a){out.vertex(pose,(float)x,(float)y,(float)z).color(0.08F,0.88F,0.42F,a).endVertex();}
}

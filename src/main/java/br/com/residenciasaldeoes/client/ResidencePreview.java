package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.ModList;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.world.phys.AABB;
import java.util.List;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.client.renderer.RenderStateShard;

@Mod.EventBusSubscriber(modid="residenciasaldeoes",value=Dist.CLIENT)
public final class ResidencePreview {
    private static final RenderType FILL = RenderType.create("residenciasaldeoes:residence_preview_fill",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
            PreviewRenderStates.fill());
    private static final class PreviewRenderStates extends RenderStateShard {
        private PreviewRenderStates() { super("residence_preview", () -> {}, () -> {}); }
        private static RenderType.CompositeState fill() {
            return RenderType.CompositeState.builder()
                    .setShaderState(POSITION_COLOR_SHADER)
                    .setTextureState(NO_TEXTURE)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setCullState(NO_CULL)
                    .setLightmapState(NO_LIGHTMAP)
                    .setOverlayState(NO_OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false);
        }
    }
    private static List<ModNetwork.PreviewEntry> entries=List.of();
    private ResidencePreview(){}
    public static void setEntries(List<ModNetwork.PreviewEntry> next){entries=List.copyOf(next);}
    static boolean shouldRender(boolean permanentEnabled,boolean playerInside){return permanentEnabled||playerInside;}
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS||entries.isEmpty()||isOculusShadowPass())return;
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        // Separate private per-frame sources keep position/color fill vertices isolated
        // from line vertices. A shared BufferSource reuses one BufferBuilder across
        // RenderTypes, invalidating a retained consumer when the type switches.
        // Private buffers also remain isolated from the world renderer and listeners.
        MultiBufferSource.BufferSource fillBuffers=MultiBufferSource.immediate(new BufferBuilder(4096));
        MultiBufferSource.BufferSource lineBuffers=MultiBufferSource.immediate(new BufferBuilder(4096));
        var pose=event.getPoseStack();var cam=event.getCamera().getPosition();
        RenderSystem.depthMask(false);
        VertexConsumer fill=null;
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
                if(fill==null)fill=buffers.getBuffer(FILL);
                var matrix=pose.last().pose();float alpha=0.035F;
                planeXZ(fill,matrix,box.minY,box.minX,box.maxX,box.minZ,box.maxZ,alpha);planeXZ(fill,matrix,box.maxY,box.minX,box.maxX,box.minZ,box.maxZ,alpha);
                planeYZ(fill,matrix,box.minX,box.minY,box.maxY,box.minZ,box.maxZ,alpha);planeYZ(fill,matrix,box.maxX,box.minY,box.maxY,box.minZ,box.maxZ,alpha);
                planeXY(fill,matrix,box.minZ,box.minX,box.maxX,box.minY,box.maxY,alpha);planeXY(fill,matrix,box.maxZ,box.minX,box.maxX,box.minY,box.maxY,alpha);
                LevelRenderer.renderLineBox(pose,lineBuffers.getBuffer(RenderType.lines()),box,0.08F,0.88F,0.42F,0.95F);
            }
            pose.popPose();
        }
        if(fill!=null)fillBuffers.endBatch(FILL);
        RenderSystem.depthMask(false);RenderSystem.lineWidth(3.0F);
        lineBuffers.endBatch(RenderType.lines());
        RenderSystem.depthMask(true);RenderSystem.lineWidth(1.0F);
    }
    private static boolean isOculusShadowPass(){
        if(!ModList.get().isLoaded("oculus"))return false;
        try{
            Class<?> state=Class.forName("net.coderbot.iris.shadows.ShadowRenderingState",false,ResidencePreview.class.getClassLoader());
            return (Boolean)state.getMethod("areShadowsCurrentlyBeingRendered").invoke(null);
        }catch(ReflectiveOperationException|LinkageError unavailable){
            // If Oculus is present but its shadow state cannot be queried, fail closed:
            // do not submit preview vertices into an unknown shader vertex format.
            return true;
        }
    }
    private static void planeXZ(com.mojang.blaze3d.vertex.VertexConsumer o,Matrix4f m,double y,double x0,double x1,double z0,double z1,float a){vertex(o,m,x0,y,z0,a);vertex(o,m,x1,y,z0,a);vertex(o,m,x1,y,z1,a);vertex(o,m,x0,y,z1,a);}
    private static void planeYZ(com.mojang.blaze3d.vertex.VertexConsumer o,Matrix4f m,double x,double y0,double y1,double z0,double z1,float a){vertex(o,m,x,y0,z0,a);vertex(o,m,x,y1,z0,a);vertex(o,m,x,y1,z1,a);vertex(o,m,x,y0,z1,a);}
    private static void planeXY(com.mojang.blaze3d.vertex.VertexConsumer o,Matrix4f m,double z,double x0,double x1,double y0,double y1,float a){vertex(o,m,x0,y0,z,a);vertex(o,m,x1,y0,z,a);vertex(o,m,x1,y1,z,a);vertex(o,m,x0,y1,z,a);}
    private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,Matrix4f pose,double x,double y,double z,float a){out.vertex(pose,(float)x,(float)y,(float)z).color(0.08F,0.88F,0.42F,a).endVertex();}
}

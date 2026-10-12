package br.com.residenciasaldeoes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import br.com.residenciasaldeoes.data.SkinApplicationMode;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.layers.VillagerProfessionLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

/** Applies a resident PNG to the original villager model and selects the vanilla outfit policy. */
public final class VillagerSkinLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public VillagerSkinLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent) { super(parent); }

    /** Forge's hook appends layers; place this before the outfit layer for Skin Parcial. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void installBeforeVanillaLayers(VillagerRenderer renderer) {
        VillagerSkinLayer layer = new VillagerSkinLayer(renderer);
        if (!renderer.addLayer(layer)) return;
        for (Class<?> type = renderer.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(renderer);
                    if (value instanceof List list && list.contains(layer)) {
                        list.remove(layer);
                        boolean foundOutfitLayer = false;
                        for (int i = 0; i < list.size(); i++) {
                            Object current = list.get(i);
                            if (current instanceof VillagerProfessionLayer<?, ?>) {
                                list.set(i, new ModeAwareProfessionLayer(renderer, (RenderLayer<Villager, VillagerModel<Villager>>) current));
                                foundOutfitLayer = true;
                            }
                        }
                        list.add(0, layer);
                        if (!foundOutfitLayer) LOGGER.warn("VillagerProfessionLayer was not found; complete skins cannot suppress vanilla biome/profession clothing on this renderer.");
                        return;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Continue through the renderer hierarchy; never replace the renderer/model.
                }
            }
        }
        LOGGER.warn("Could not place villager skin before vanilla render layers; partial skins may render above vanilla outfits.");
    }

    @Override public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, Villager villager,
                                 float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                 float netHeadYaw, float headPitch) {
        ResourceLocation skin = VillagerSkinTextures.get(villager.getUUID());
        if (skin == null) return;
        getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(skin)), packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        VillagerModel<Villager> model=getParentModel();
        renderPieces(poseStack,buffers,packedLight,villager,model,VillagerSkinTextures.outerParts(villager.getUUID()));
        renderPieces(poseStack,buffers,packedLight,villager,model,VillagerSkinTextures.accessories(villager.getUUID()));
    }

    /** Extra clothing and accessories are cuboids attached in the local coordinates of vanilla model parts. */
    private static void renderPieces(PoseStack poses,MultiBufferSource buffers,int light,Villager villager,
                                     VillagerModel<Villager> model,List<VillagerSkinTextures.PreparedPart> parts){
        for(var piece:parts){ResourceLocation texture=VillagerSkinTextures.extraTexture(villager.getUUID(),piece.texture());if(texture==null)continue;
            net.minecraft.client.model.geom.ModelPart root=model.root();
            net.minecraft.client.model.geom.ModelPart anchor=switch(piece.anchor()){
                case "HEAD"->model.getHead();case "BODY"->root.getChild("body");case "ARMS"->root.getChild("arms");
                case "RIGHT_LEG"->root.getChild("right_leg");case "LEFT_LEG"->root.getChild("left_leg");default->null;};
            if(anchor==null)continue;poses.pushPose();anchor.translateAndRotate(poses);
            piece.model().render(poses,buffers.getBuffer(RenderType.entityCutoutNoCull(texture)),light,OverlayTexture.NO_OVERLAY,1,1,1,1);poses.popPose();
        }
    }

    /** The vanilla profession layer also supplies biome clothing for unemployed villagers. */
    private static final class ModeAwareProfessionLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
        private final RenderLayer<Villager, VillagerModel<Villager>> delegate;

        private ModeAwareProfessionLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent,
                                         RenderLayer<Villager, VillagerModel<Villager>> delegate) {
            super(parent);
            this.delegate = delegate;
        }

        @Override
        public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, Villager villager,
                           float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (VillagerSkinTextures.isComplete(villager.getUUID())) return;
            delegate.render(poseStack, buffers, packedLight, villager, limbSwing, limbSwingAmount,
                    partialTick, ageInTicks, netHeadYaw, headPitch);
        }
    }
}

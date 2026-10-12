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
    private final boolean completeOverlay;

    public VillagerSkinLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent, boolean completeOverlay) {
        super(parent);
        this.completeOverlay = completeOverlay;
    }

    /** Forge's hook appends layers; place this before the outfit layer for Skin Parcial. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void installBeforeVanillaLayers(VillagerRenderer renderer) {
        VillagerSkinLayer partialLayer = new VillagerSkinLayer(renderer, false);
        VillagerSkinLayer completeOverlay = new VillagerSkinLayer(renderer, true);
        if (!renderer.addLayer(partialLayer)) return;
        if (!renderer.addLayer(completeOverlay)) {
            LOGGER.warn("Could not add the final villager accessory pass; complete skins still use the base-texture replacement, but package accessories may not render.");
            return;
        }
        for (Class<?> type = renderer.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (!List.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(renderer);
                    if (value instanceof List list && list.contains(partialLayer) && list.contains(completeOverlay)) {
                        // Build the replacement first. The previous implementation removed
                        // the skin layer before attempting a reflective list.set; if that
                        // mutation failed, the custom layer silently disappeared.
                        List<Object> replacement = new java.util.ArrayList<>(list);
                        replacement.remove(partialLayer);
                        replacement.remove(completeOverlay);
                        int wrapped = 0;
                        for (int i = 0; i < replacement.size(); i++) {
                            Object current = replacement.get(i);
                            if (current instanceof VillagerProfessionLayer<?, ?>) {
                                replacement.set(i, new ModeAwareProfessionLayer(renderer, (RenderLayer<Villager, VillagerModel<Villager>>) current));
                                wrapped++;
                            }
                        }
                        // Partial mode is painted before vanilla clothing. Complete mode
                        // uses the renderer's base texture directly; this final pass is
                        // reserved for wardrobe geometry that should sit above that texture.
                        replacement.add(0, partialLayer);
                        replacement.add(completeOverlay);
                        list.clear();
                        list.addAll(replacement);
                        if (wrapped == 0) LOGGER.warn("VillagerProfessionLayer was not found; complete skins cannot suppress vanilla biome/profession clothing on this renderer.");
                        else LOGGER.info("Skin do aldeão instalada; textura base e acessórios usam passes separados; {} camada(s) vanilla respeitam Skin Completa/Parcial.", wrapped);
                        return;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Continue through the renderer hierarchy; never replace the renderer/model.
                }
            }
        }
        LOGGER.warn("Could not place villager skin layers; complete skins still replace the base texture, but package accessories or partial ordering may be affected.");
    }

    @Override public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, Villager villager,
                                 float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                 float netHeadYaw, float headPitch) {
        ResourceLocation skin = VillagerSkinTextures.get(villager.getUUID());
        if (skin == null) return;
        VillagerModel<Villager> model=getParentModel();
        boolean complete=VillagerSkinTextures.isComplete(villager.getUUID());
        if (completeOverlay) {
            if (!complete) return;
            // Skin Completa already replaced the renderer's base texture through a
            // narrow VillagerRenderer texture hook. Keep only extra geometry here.
            renderCustomParts(poseStack,buffers,packedLight,villager,model);
            return;
        }
        if (complete) return;
        renderSkin(poseStack,buffers,packedLight,skin);
        renderCustomParts(poseStack,buffers,packedLight,villager,model);
    }

    private static void renderCustomParts(PoseStack poses,MultiBufferSource buffers,int light,Villager villager,
                                          VillagerModel<Villager> model){
        renderPieces(poses,buffers,light,villager,model,VillagerSkinTextures.outerParts(villager.getUUID()));
        renderPieces(poses,buffers,light,villager,model,VillagerSkinTextures.accessories(villager.getUUID()));
    }

    private void renderSkin(PoseStack poses,MultiBufferSource buffers,int light,ResourceLocation skin){
        getParentModel().renderToBuffer(poses,buffers.getBuffer(RenderType.entityCutoutNoCull(skin)),light,
                OverlayTexture.NO_OVERLAY,1.0F,1.0F,1.0F,1.0F);
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

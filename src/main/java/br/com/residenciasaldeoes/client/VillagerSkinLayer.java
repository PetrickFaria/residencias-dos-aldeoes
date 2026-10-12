package br.com.residenciasaldeoes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.List;

/** Draws the resident texture on the vanilla model before vanilla profession/level overlays. */
public final class VillagerSkinLayer extends RenderLayer<Villager, VillagerModel<Villager>> {
    private static final Logger LOGGER = LogUtils.getLogger();

    public VillagerSkinLayer(RenderLayerParent<Villager, VillagerModel<Villager>> parent) { super(parent); }

    /**
     * Forge's AddLayers hook only appends. Move this one layer to the beginning so the normal
     * biome/profession/level layers render over it exactly as they do over the vanilla texture.
     * Search by the inserted instance rather than a mapped private field name; this also works
     * when an optional renderer mod subclasses VillagerRenderer.
     */
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
                    if (value instanceof List list && list.remove(layer)) {
                        list.add(0, layer);
                        return;
                    }
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Continue through the renderer hierarchy; never replace the renderer/model.
                }
            }
        }
        LOGGER.warn("Could not place villager skin before vanilla render layers; profession overlays may cover the custom texture.");
    }

    @Override public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, Villager villager,
                                 float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                                 float netHeadYaw, float headPitch) {
        ResourceLocation skin = VillagerSkinTextures.get(villager.getUUID());
        if (skin == null) return;
        getParentModel().renderToBuffer(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(skin)), packedLight,
                OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    }
}

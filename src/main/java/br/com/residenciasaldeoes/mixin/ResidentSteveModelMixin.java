package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.client.ResidentAppearanceModels;
import br.com.residenciasaldeoes.client.VillagerSkinLayer;
import br.com.residenciasaldeoes.client.render.ResidentSteveRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Swaps only the visual model for a Steve resident. Villager-specific renderer layers
 * are temporarily excluded because they cast the renderer model to VillagerModel.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class ResidentSteveModelMixin {
    @Shadow @Final @Mutable protected EntityModel model;
    @Shadow @Final @Mutable protected List<RenderLayer> layers;

    @Unique private static final ThreadLocal<Deque<ResidentSteveRenderState>> RESIDENCIAS$renderStates =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "render", at = @At("HEAD"))
    private void residenciasaldeoes$useSteveModel(LivingEntity entity, float yaw, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers,
            int packedLight, CallbackInfo ci) {
        if (!(entity instanceof Villager villager) || !ResidentAppearanceModels.usesSteve(villager)) return;

        RESIDENCIAS$renderStates.get().push(new ResidentSteveRenderState(model, layers));
        model = ResidentAppearanceModels.steveModel();

        // PlayerModel renders the player's hat/jacket/sleeve/pants skin layers itself.
        // Keep only this mod's skin overlay pass; profession, guard, and other
        // VillagerModel-dependent layers must not receive a PlayerModel instance.
        List<RenderLayer> compatibleLayers = new ArrayList<>();
        for (RenderLayer layer : layers) {
            if (layer instanceof VillagerSkinLayer) compatibleLayers.add(layer);
        }
        layers = compatibleLayers;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void residenciasaldeoes$restoreVillagerRenderer(LivingEntity entity, float yaw, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers,
            int packedLight, CallbackInfo ci) {
        if (!(entity instanceof Villager villager) || !ResidentAppearanceModels.usesSteve(villager)) return;

        Deque<ResidentSteveRenderState> states = RESIDENCIAS$renderStates.get();
        if (states.isEmpty()) {
            RESIDENCIAS$renderStates.remove();
            return;
        }
        ResidentSteveRenderState previous = states.pop();
        model = previous.model();
        layers = previous.layers();
        if (states.isEmpty()) RESIDENCIAS$renderStates.remove();
    }
}

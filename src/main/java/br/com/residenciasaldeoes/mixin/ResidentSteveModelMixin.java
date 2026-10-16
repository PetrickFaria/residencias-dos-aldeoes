package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.client.ResidentAppearanceModels;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
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

/** Swaps only the model object for one resident's render call and restores the renderer afterward. */
@Mixin(LivingEntityRenderer.class)
public abstract class ResidentSteveModelMixin {
    @Shadow @Final @Mutable protected EntityModel model;
    @Unique private static final ThreadLocal<EntityModel<?>> RESIDENCIAS$previousModel = new ThreadLocal<>();

    @Inject(method="render", at=@At("HEAD"))
    private void residenciasaldeoes$useSteveModel(LivingEntity entity, float yaw, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers,
            int packedLight, CallbackInfo ci) {
        if (entity instanceof Villager villager && ResidentAppearanceModels.usesSteve(villager)) {
            RESIDENCIAS$previousModel.set(model);
            model = ResidentAppearanceModels.steveModel();
        }
    }

    @Inject(method="render", at=@At("RETURN"))
    private void residenciasaldeoes$restoreVillagerModel(LivingEntity entity, float yaw, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poses, net.minecraft.client.renderer.MultiBufferSource buffers,
            int packedLight, CallbackInfo ci) {
        EntityModel<?> previous = RESIDENCIAS$previousModel.get();
        if (previous != null) {
            model = previous;
            RESIDENCIAS$previousModel.remove();
        }
    }
}

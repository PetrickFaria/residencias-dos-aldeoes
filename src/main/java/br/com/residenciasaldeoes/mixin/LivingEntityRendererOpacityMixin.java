package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.client.ResidentOpacity;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scopes the render override to the current resident villager only. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererOpacityMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void residenciasaldeoes$beginOpacityRender(LivingEntity entity, float yaw, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poses,
            net.minecraft.client.renderer.MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        ResidentOpacity.beginRender(entity);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void residenciasaldeoes$endOpacityRender(LivingEntity entity, float yaw, float partialTick,
            com.mojang.blaze3d.vertex.PoseStack poses,
            net.minecraft.client.renderer.MultiBufferSource buffers, int packedLight, CallbackInfo ci) {
        ResidentOpacity.endRender();
    }
}

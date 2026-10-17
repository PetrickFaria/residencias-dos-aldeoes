package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.client.ResidentOpacity;
import br.com.residenciasaldeoes.client.ResidentAppearanceModels;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Scopes the render override to the current resident villager only. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererOpacityMixin {
    /**
     * Steve uses PlayerModel inside the villager renderer. Select a blended base
     * pass explicitly for that resident so its world render follows the same
     * opacity path as the customizer preview. BufferBuilderOpacityMixin applies
     * the selected alpha to this pass and to the PlayerModel skin layers.
     */
    @SuppressWarnings("unchecked")
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void residenciasaldeoes$translucentSteveBase(LivingEntity entity, boolean bodyVisible,
            boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (bodyVisible && !translucent && !glowing && entity instanceof Villager villager
                && ResidentAppearanceModels.usesSteve(villager)
                && ResidentOpacity.renderingPercent() < 100) {
            // EntityRenderer declares this abstract method; LivingEntityRenderer
            // inherits it, so it cannot be attached as a Shadow on this target.
            ResourceLocation texture = ((EntityRenderer<LivingEntity>) (Object) this).getTextureLocation(entity);
            cir.setReturnValue(RenderType.entityTranslucent(texture));
        }
    }

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

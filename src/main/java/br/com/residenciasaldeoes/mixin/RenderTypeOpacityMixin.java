package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.client.ResidentOpacity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces cutout entity passes with translucent passes only while a resident is drawn. */
@Mixin(RenderType.class)
public abstract class RenderTypeOpacityMixin {
    @Inject(method = "entityCutout(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true)
    private static void residenciasaldeoes$translucentCutout(ResourceLocation texture,
            CallbackInfoReturnable<RenderType> cir) {
        if (ResidentOpacity.renderingPercent() < 100) cir.setReturnValue(RenderType.entityTranslucent(texture));
    }

    @Inject(method = "entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true)
    private static void residenciasaldeoes$translucentCutoutNoCull(ResourceLocation texture,
            CallbackInfoReturnable<RenderType> cir) {
        if (ResidentOpacity.renderingPercent() < 100) cir.setReturnValue(RenderType.entityTranslucent(texture));
    }
    @Inject(method = "entityCutoutNoCull(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/renderer/RenderType;",
            at = @At("HEAD"), cancellable = true)
    private static void residenciasaldeoes$translucentOutlinedCutout(ResourceLocation texture, boolean outline,
            CallbackInfoReturnable<RenderType> cir) {
        if (ResidentOpacity.renderingPercent() < 100) cir.setReturnValue(RenderType.entityTranslucent(texture, outline));
    }

}

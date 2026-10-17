package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.client.OpacityPreviewBuffers;
import br.com.residenciasaldeoes.client.ResidentOpacity;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies resident alpha at the entity's returned vertex consumer, like the proven preview path. */
@Mixin(MultiBufferSource.BufferSource.class)
public abstract class ResidentBufferSourceOpacityMixin {
    @Inject(method = "getBuffer", at = @At("RETURN"), cancellable = true)
    private void residenciasaldeoes$applyResidentAlpha(RenderType type,
            CallbackInfoReturnable<VertexConsumer> cir) {
        if (ResidentOpacity.isPreviewRendering() || ResidentOpacity.renderingNameTag()
                || ResidentOpacity.hasPreviewAlphaOverride()) return;
        int opacity = ResidentOpacity.renderingPercent();
        if (opacity < 100)
            cir.setReturnValue(OpacityPreviewBuffers.withOpacity(cir.getReturnValue(), opacity));
    }
}

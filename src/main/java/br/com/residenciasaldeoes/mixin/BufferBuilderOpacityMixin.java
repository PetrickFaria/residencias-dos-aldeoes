package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.client.ResidentOpacity;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Multiplies vertex alpha only while a resident villager is rendered. */
@Mixin(BufferBuilder.class)
public abstract class BufferBuilderOpacityMixin {
    @ModifyVariable(method = "color(IIII)Lcom/mojang/blaze3d/vertex/VertexConsumer;",
            at = @At("HEAD"), argsOnly = true, ordinal = 3)
    private int residenciasaldeoes$applyResidentAlpha(int alpha) {
        if(ResidentOpacity.hasPreviewAlphaOverride())return alpha;
        return Math.max(0, Math.min(255, Math.round(alpha * (ResidentOpacity.renderingPercent() / 100.0F))));
    }
}

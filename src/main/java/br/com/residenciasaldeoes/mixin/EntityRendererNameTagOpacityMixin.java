package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.client.ResidentOpacity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevents the model's vertex-alpha override from fading the entity name tag. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererNameTagOpacityMixin {
    @Inject(method = "renderNameTag", at = @At("HEAD"))
    private void residenciasaldeoes$beginOpaqueNameTag(Entity entity, Component name, PoseStack poses,
                                                        MultiBufferSource buffers, int packedLight,
                                                        CallbackInfo ci) {
        ResidentOpacity.beginNameTag();
    }

    @Inject(method = "renderNameTag", at = @At("RETURN"))
    private void residenciasaldeoes$endOpaqueNameTag(Entity entity, Component name, PoseStack poses,
                                                      MultiBufferSource buffers, int packedLight,
                                                      CallbackInfo ci) {
        ResidentOpacity.endNameTag();
    }
}

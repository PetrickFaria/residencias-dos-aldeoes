package br.com.residenciasaldeoes.mixin;

import br.com.residenciasaldeoes.client.VillagerSkinTextures;
import br.com.residenciasaldeoes.client.ResidentAppearanceModels;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.renderer.entity.VillagerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies a resident's full skin as the renderer's base texture, preserving PNG alpha. */
@Mixin(VillagerRenderer.class)
public abstract class VillagerRendererMixin {
    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void residenciasaldeoes$useFullResidentSkin(Villager villager,
                                                         CallbackInfoReturnable<ResourceLocation> callback) {
        if (VillagerSkinTextures.isComplete(villager.getUUID())) {
            ResourceLocation skin = VillagerSkinTextures.get(villager.getUUID());
            if (skin != null) callback.setReturnValue(skin);
        } else if (ResidentAppearanceModels.usesSteve(villager)) {
            callback.setReturnValue(DefaultPlayerSkin.getDefaultSkin(villager.getUUID()));
        }
    }
}

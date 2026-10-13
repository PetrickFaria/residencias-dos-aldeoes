package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.block.EmeraldChestBlockEntity;
import br.com.residenciasaldeoes.block.EmeraldChestBodyBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.client.resources.model.Material;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

public final class EmeraldChestRenderer extends ChestRenderer<EmeraldChestBlockEntity> {
    private static final Material EMERALD_CHEST = new Material(Sheets.CHEST_SHEET,
            new ResourceLocation(ResidenciasDosAldeoes.MODID, "entity/chest/emerald_chest"));

    public EmeraldChestRenderer(BlockEntityRendererProvider.Context context) { super(context); }

    @Override protected Material getMaterial(EmeraldChestBlockEntity entity, ChestType type) { return EMERALD_CHEST; }

    @Override public void render(EmeraldChestBlockEntity entity, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        super.render(entity, partialTick, pose, buffers, light, overlay);
        if (Minecraft.getInstance().level == null) return;
        BlockState body = ResidenciasDosAldeoes.EMERALD_CHEST_BODY.get().defaultBlockState()
                .setValue(EmeraldChestBodyBlock.FACING, entity.getBlockState().getValue(ChestBlock.FACING));
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(body, pose, buffers, light, OverlayTexture.NO_OVERLAY);
    }
}

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
import net.minecraft.core.Direction;
import net.minecraft.client.resources.model.Material;
import com.mojang.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** Vanilla animated chest renderer with the Residential Anchor's actual body and top models. */
public final class EmeraldChestRenderer extends ChestRenderer<EmeraldChestBlockEntity> {
    private static final Material EMERALD_CHEST = new Material(Sheets.CHEST_SHEET,
            new ResourceLocation(ResidenciasDosAldeoes.MODID, "entity/chest/emerald_chest"));

    public EmeraldChestRenderer(BlockEntityRendererProvider.Context context) { super(context); }

    @Override protected Material getMaterial(EmeraldChestBlockEntity entity, ChestType type) { return EMERALD_CHEST; }

    @Override public void render(EmeraldChestBlockEntity entity, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        super.render(entity, partialTick, pose, buffers, light, overlay);
        if (Minecraft.getInstance().level == null) return;
        Direction facing=entity.getBlockState().getValue(ChestBlock.FACING);
        BlockState body=ResidenciasDosAldeoes.EMERALD_CHEST_BODY.get().defaultBlockState()
                .setValue(EmeraldChestBodyBlock.FACING,facing).setValue(EmeraldChestBodyBlock.LID,false);
        pose.pushPose();
        pose.translate(0.0625D,0.0D,0.0625D);
        pose.scale(0.875F,0.5625F,0.875F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(body,pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();

        // The anchor's top panel, OBJ trim and crystal are attached to the vanilla chest lid pivot.
        BlockState lid=ResidenciasDosAldeoes.EMERALD_CHEST_BODY.get().defaultBlockState()
                .setValue(EmeraldChestBodyBlock.FACING,Direction.NORTH).setValue(EmeraldChestBodyBlock.LID,true);
        float openness=entity.getOpenNess(partialTick);
        pose.pushPose();
        pose.translate(0.5D,0.0D,0.5D);
        pose.mulPose(Vector3f.YP.rotationDegrees(facing.toYRot()));
        pose.translate(-0.5D,0.0D,-0.5D);
        pose.translate(0.5D,0.5625D,0.9375D);
        pose.mulPose(Vector3f.XP.rotation(-openness*((float)Math.PI/2.0F)));
        pose.translate(-0.5D,-0.5625D,-0.9375D);
        pose.translate(0.025D,-0.581125D,0.025D);
        pose.scale(0.95F,1.5F,0.95F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(lid,pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();

        // A small raised emerald clasp is part of the moving lid, not the fixed chest body.
        pose.pushPose();
        pose.translate(0.5D,0.0D,0.5D);
        pose.mulPose(Vector3f.YP.rotationDegrees(facing.toYRot()));
        pose.translate(-0.5D,0.0D,-0.5D);
        pose.translate(0.5D,0.5625D,0.9375D);
        pose.mulPose(Vector3f.XP.rotation(-openness*((float)Math.PI/2.0F)));
        pose.translate(-0.5D,-0.5625D,-0.9375D);
        pose.pushPose();
        pose.translate(0.455D,0.695D,-0.025D);
        pose.scale(0.09F,0.10F,0.035F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState(),
                pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.pushPose();
        pose.translate(0.477D,0.718D,-0.052D);
        pose.scale(0.046F,0.055F,0.026F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(net.minecraft.world.level.block.Blocks.EMERALD_BLOCK.defaultBlockState(),
                pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
        pose.popPose();

    }
}

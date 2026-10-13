package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.block.EmeraldChestBlockEntity;
import br.com.residenciasaldeoes.block.EmeraldChestBodyBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;

/** One custom representation: fixed anchor body and lid-attached anchor top. */
public final class EmeraldChestRenderer implements BlockEntityRenderer<EmeraldChestBlockEntity> {
    public EmeraldChestRenderer(BlockEntityRendererProvider.Context context) {}
    @Override public void render(EmeraldChestBlockEntity entity,float partialTick,PoseStack pose,
                                 MultiBufferSource buffers,int light,int overlay) {
        Direction facing=entity.getBlockState().getValue(ChestBlock.FACING);
        pose.pushPose();
        pose.translate(.5,0,.5);
        float rotation=switch(facing){case EAST->90F;case SOUTH->180F;case WEST->270F;default->0F;};
        pose.mulPose(Vector3f.YP.rotationDegrees(-rotation));
        pose.translate(-.5,0,-.5);
        BlockState body=ResidenciasDosAldeoes.EMERALD_CHEST_BODY.get().defaultBlockState()
                .setValue(EmeraldChestBodyBlock.FACING,Direction.NORTH).setValue(EmeraldChestBodyBlock.LID,false);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(body,pose,buffers,light,OverlayTexture.NO_OVERLAY);
        float openness=entity.getOpenNess(partialTick);
        openness=1F-(float)Math.pow(1F-openness,3);
        pose.translate(.5,13D/16D,15D/16D);
        pose.mulPose(Vector3f.XP.rotation(-openness*((float)Math.PI/2F)));
        pose.translate(-.5,-13D/16D,-15D/16D);
        BlockState lid=body.setValue(EmeraldChestBodyBlock.LID,true);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(lid,pose,buffers,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}

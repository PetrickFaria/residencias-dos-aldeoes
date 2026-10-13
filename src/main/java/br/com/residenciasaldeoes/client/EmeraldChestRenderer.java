package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.block.EmeraldChestBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.client.resources.model.Material;
import com.mojang.math.Vector3f;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;

/** Vanilla animated chest renderer with the mod's chest atlas and integrated 3D emerald details. */
public final class EmeraldChestRenderer extends ChestRenderer<EmeraldChestBlockEntity> {
    private static final Material EMERALD_CHEST = new Material(Sheets.CHEST_SHEET,
            new ResourceLocation(ResidenciasDosAldeoes.MODID, "entity/chest/emerald_chest"));

    public EmeraldChestRenderer(BlockEntityRendererProvider.Context context) { super(context); }

    @Override protected Material getMaterial(EmeraldChestBlockEntity entity, ChestType type) { return EMERALD_CHEST; }

    @Override public void render(EmeraldChestBlockEntity entity, float partialTick, PoseStack pose,
                                 MultiBufferSource buffers, int light, int overlay) {
        super.render(entity, partialTick, pose, buffers, light, overlay);
        pose.pushPose();
        pose.translate(.5D, 0D, .5D);
        pose.mulPose(Vector3f.YP.rotationDegrees(entity.getBlockState().getValue(net.minecraft.world.level.block.ChestBlock.FACING).toYRot()));
        pose.translate(-.5D, 0D, -.5D);
        renderChestRelief(pose, buffers, light, entity.getOpenNess(partialTick));
        renderEmerald(entity, pose, buffers, light, .08F, .58F, .50F, 0.16F);
        renderEmerald(entity, pose, buffers, light, .92F, .58F, .50F, 0.16F);
        float openness = entity.getOpenNess(partialTick);
        pose.pushPose();
        pose.translate(.5D, .375D, .9375D);
        pose.mulPose(Vector3f.XP.rotation(-openness * ((float)Math.PI / 2.0F)));
        pose.translate(-.5D, -.375D, -.9375D);
        renderEmerald(entity, pose, buffers, light, .50F, .92F, .50F, 0.30F);
        renderEmerald(entity, pose, buffers, light, .50F, .53F, .055F, 0.10F);
        pose.popPose();
        pose.popPose();
    }

    private static void renderEmerald(EmeraldChestBlockEntity entity, PoseStack pose, MultiBufferSource buffers,
                                      int light, float x, float y, float z, float scale) {
        if (Minecraft.getInstance().level == null) return;
        pose.pushPose();
        pose.translate(x, y, z);
        if (x < .1F || x > .9F) pose.mulPose(Vector3f.YP.rotationDegrees(90));
        pose.mulPose(Vector3f.ZP.rotationDegrees(45));
        pose.scale(scale, scale, scale);
        pose.translate(-.5D, -.5D, -.5D);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.EMERALD_BLOCK.defaultBlockState(),
                pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }

    /** Decorative relief follows the vanilla lid pivot; the body panels stay attached to the chest body. */
    private static void renderChestRelief(PoseStack pose, MultiBufferSource buffers, int light, float openness) {
        BlockState emerald = Blocks.EMERALD_BLOCK.defaultBlockState();
        BlockState gold = Blocks.GOLD_BLOCK.defaultBlockState();
        renderPanel(emerald, pose, buffers, light, .058F, .43F, .50F, .025F, .40F, .48F);
        renderPanel(emerald, pose, buffers, light, .942F, .43F, .50F, .025F, .40F, .48F);
        renderPanel(emerald, pose, buffers, light, .50F, .43F, .942F, .48F, .40F, .025F);
        // Bright gold rails and corner caps frame the wood front without covering the clasp.
        for (float x : new float[]{.10F, .90F}) {
            renderPanel(gold, pose, buffers, light, x, .45F, .055F, .055F, .62F, .025F);
            for (float y : new float[]{.14F, .76F})
                renderPanel(gold, pose, buffers, light, x, y, .055F, .12F, .12F, .035F);
        }
        for (float y : new float[]{.15F, .75F})
            renderPanel(gold, pose, buffers, light, .50F, y, .055F, .76F, .045F, .025F);
        pose.pushPose();
        pose.translate(.5D, .375D, .9375D);
        pose.mulPose(Vector3f.XP.rotation(-openness * ((float)Math.PI / 2.0F)));
        pose.translate(-.5D, -.375D, -.9375D);
        renderPanel(emerald, pose, buffers, light, .50F, .88F, .50F, .48F, .014F, .48F);
        for (float x : new float[]{.24F, .76F})
            renderPanel(gold, pose, buffers, light, x, .885F, .50F, .035F, .025F, .54F);
        for (float z : new float[]{.23F, .77F})
            renderPanel(gold, pose, buffers, light, .50F, .885F, z, .54F, .025F, .035F);
        pose.popPose();
    }

    private static void renderPanel(BlockState block, PoseStack pose, MultiBufferSource buffers, int light,
                                    float x, float y, float z, float sx, float sy, float sz) {
        pose.pushPose();
        pose.translate(x - sx / 2.0F, y - sy / 2.0F, z - sz / 2.0F);
        pose.scale(sx, sy, sz);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block, pose, buffers, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}

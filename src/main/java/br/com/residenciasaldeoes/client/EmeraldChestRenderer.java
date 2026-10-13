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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemTransforms;
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
        renderEmerald(entity, pose, buffers, light, .08F, .58F, .50F, 0.24F);
        renderEmerald(entity, pose, buffers, light, .92F, .58F, .50F, 0.24F);
        float openness = entity.getOpenNess(partialTick);
        pose.pushPose();
        pose.translate(.5D, .375D, .9375D);
        pose.mulPose(Vector3f.XP.rotation(-openness * ((float)Math.PI / 2.0F)));
        pose.translate(-.5D, -.375D, -.9375D);
        renderEmerald(entity, pose, buffers, light, .50F, .74F, .15F, 0.20F);
        renderEmerald(entity, pose, buffers, light, .50F, .53F, .01F, 0.18F);
        pose.popPose();
        pose.popPose();
    }

    private static void renderEmerald(EmeraldChestBlockEntity entity, PoseStack pose, MultiBufferSource buffers,
                                      int light, float x, float y, float z, float scale) {
        if (Minecraft.getInstance().level == null) return;
        pose.pushPose();
        pose.translate(x, y, z);
        pose.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(Items.EMERALD),
                ItemTransforms.TransformType.FIXED, light, OverlayTexture.NO_OVERLAY, pose, buffers, 0);
        pose.popPose();
    }
}

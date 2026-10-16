package br.com.residenciasaldeoes.client;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.Villager;

/** Humanoid Steve geometry in villager renderer slots, so villager identity/AI stay untouched. */
public final class SteveVillagerModel extends VillagerModel<Villager> {
    private final ModelPart steveHead;
    private final ModelPart steveHat;
    private final ModelPart steveArms;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart body;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    public SteveVillagerModel(ModelPart root) {
        super(root);
        steveHead = root.getChild("head");
        steveHat = root.getChild("hat");
        steveArms = root.getChild("arms");
        rightArm = steveArms.getChild("right_arm");
        leftArm = steveArms.getChild("left_arm");
        body = root.getChild("body");
        rightLeg = root.getChild("right_leg");
        leftLeg = root.getChild("left_leg");
        root.getChild("nose").visible = false;
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        // VillagerModel expects a separate hat part. The inner child is kept empty for
        // integrations that look up the classic villager hierarchy by name.
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create(), PartPose.ZERO);
        PartDefinition hat = root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(32, 0).addBox(-4, -8, -4, 8, 8, 8), PartPose.ZERO);
        hat.addOrReplaceChild("hat_rim", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("nose", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(16, 16).addBox(-4, 0, -2, 8, 12, 4), PartPose.ZERO);
        PartDefinition arms = root.addOrReplaceChild("arms", CubeListBuilder.create(), PartPose.ZERO);
        arms.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(40, 16)
                .addBox(-2, -2, -2, 4, 12, 4), PartPose.offset(-5, 2, 0));
        arms.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(32, 48)
                .addBox(-2, -2, -2, 4, 12, 4), PartPose.offset(5, 2, 0));
        root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 16)
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(-2, 12, 0));
        root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(16, 48)
                .addBox(-2, 0, -2, 4, 12, 4), PartPose.offset(2, 12, 0));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override public void setupAnim(Villager villager, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float netHeadYaw, float headPitch) {
        float yaw = netHeadYaw * ((float)Math.PI / 180F);
        float pitch = headPitch * ((float)Math.PI / 180F);
        steveHead.yRot = yaw; steveHead.xRot = pitch;
        steveHat.yRot = yaw; steveHat.xRot = pitch;
        rightArm.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 2.0F * limbSwingAmount * 0.5F;
        leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 2.0F * limbSwingAmount * 0.5F;
        rightArm.yRot = leftArm.yRot = 0.0F;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + (float)Math.PI) * 1.4F * limbSwingAmount;
        body.xRot = 0.0F;
        steveArms.xRot = 0.0F; steveArms.yRot = 0.0F; steveArms.zRot = 0.0F;
    }
}

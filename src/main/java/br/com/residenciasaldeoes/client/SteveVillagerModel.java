package br.com.residenciasaldeoes.client;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.npc.Villager;

/**
 * Classic-arm player model used only for a resident's appearance.
 * The entity remains a Villager; PlayerModel supplies the correct human proportions,
 * outer skin layers, and standard 64x64 player-skin UV layout.
 */
public final class SteveVillagerModel extends PlayerModel<Villager> {
    public SteveVillagerModel(ModelPart root) {
        super(root, false);
    }

    public static LayerDefinition createLayer() {
        return LayerDefinition.create(
                PlayerModel.createMesh(new CubeDeformation(0.0F), false),
                64,
                64
        );
    }
}

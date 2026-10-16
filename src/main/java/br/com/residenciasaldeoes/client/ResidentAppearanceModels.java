package br.com.residenciasaldeoes.client;

import br.com.residenciasaldeoes.ResidenciasDosAldeoes;
import br.com.residenciasaldeoes.data.ResidentModelType;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.Villager;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client-side model assignments received from the resident SavedData. */
public final class ResidentAppearanceModels {
    public static final net.minecraft.client.model.geom.ModelLayerLocation STEVE_LAYER =
            new net.minecraft.client.model.geom.ModelLayerLocation(
                    new ResourceLocation(ResidenciasDosAldeoes.MODID, "resident_steve"), "main");
    private static final Map<UUID, ResidentModelType> TYPES = new ConcurrentHashMap<>();
    private static SteveVillagerModel steveModel;

    private ResidentAppearanceModels() {}

    public static ResidentModelType get(UUID id) { return TYPES.getOrDefault(id, ResidentModelType.VILLAGER); }
    public static void set(UUID id, ResidentModelType type) {
        if (id == null) return;
        if (type == null || type == ResidentModelType.VILLAGER) TYPES.remove(id);
        else TYPES.put(id, type);
    }
    public static boolean usesSteve(Villager villager) { return get(villager.getUUID()) == ResidentModelType.STEVE; }
    public static void clear(UUID id) { if (id != null) TYPES.remove(id); }
    public static void clearAll() { TYPES.clear(); steveModel = null; }

    public static SteveVillagerModel steveModel() {
        if (steveModel == null) {
            steveModel = new SteveVillagerModel(Minecraft.getInstance().getEntityModels().bakeLayer(STEVE_LAYER));
        }
        return steveModel;
    }
}

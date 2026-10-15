package br.com.residenciasaldeoes.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client mirror of per-resident opacity, plus temporary skin-preview values. */
public final class ResidentOpacity {
    private static final Map<UUID, Integer> VALUES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> RENDERING = new ThreadLocal<>();

    private ResidentOpacity() {}

    public static int get(UUID id) { return VALUES.getOrDefault(id, 100); }
    public static void set(UUID id, int value) {
        if (id != null) VALUES.put(id, Math.max(0, Math.min(100, value)));
    }
    public static void clear(UUID id) { if (id != null) VALUES.remove(id); }
    public static void clearAll() { VALUES.clear(); RENDERING.remove(); }

    public static void beginRender(net.minecraft.world.entity.Entity entity) {
        RENDERING.remove();
        if (entity instanceof net.minecraft.world.entity.npc.Villager villager) {
            int opacity = get(villager.getUUID());
            if (opacity < 100) RENDERING.set(opacity);
        }
    }
    public static void endRender() { RENDERING.remove(); }
    public static int renderingPercent() {
        Integer value = RENDERING.get();
        return value == null ? 100 : value;
    }
    public static float multiplyAlpha(float alpha) {
        return alpha * (renderingPercent() / 100.0F);
    }
}

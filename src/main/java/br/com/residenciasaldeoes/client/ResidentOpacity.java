package br.com.residenciasaldeoes.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Client mirror of per-resident opacity, plus temporary skin-preview values. */
public final class ResidentOpacity {
    private static final Map<UUID, Integer> VALUES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Integer> RENDERING = new ThreadLocal<>();
    private static final ThreadLocal<Integer> PREVIEW_RENDERING = new ThreadLocal<>();
    private static final ThreadLocal<Integer> NAME_TAG_DEPTH = ThreadLocal.withInitial(() -> 0);

    private ResidentOpacity() {}

    public static int get(UUID id) { return VALUES.getOrDefault(id, 100); }
    public static void set(UUID id, int value) {
        if (id != null) VALUES.put(id, Math.max(0, Math.min(100, value)));
    }
    public static void clear(UUID id) { if (id != null) VALUES.remove(id); }
    public static void clearAll() { VALUES.clear(); RENDERING.remove(); PREVIEW_RENDERING.remove(); NAME_TAG_DEPTH.remove(); }

    public static void beginRender(net.minecraft.world.entity.Entity entity) {
        RENDERING.remove();
        if (entity instanceof net.minecraft.world.entity.npc.Villager villager) {
            int opacity = get(villager.getUUID());
            if (opacity < 100) RENDERING.set(opacity);
        }
    }
    public static void endRender() { RENDERING.remove(); }
    /** Explicit render scope for the temporary villager drawn by the skin customizer. */
    public static void beginPreviewRender(net.minecraft.world.entity.Entity entity) {
        PREVIEW_RENDERING.set(entity instanceof net.minecraft.world.entity.npc.Villager villager ? get(villager.getUUID()) : 100);
    }
    public static void endPreviewRender() { PREVIEW_RENDERING.remove(); }
    /** Keep Minecraft's name-tag pass independent from the resident model opacity. */
    public static void beginNameTag() { NAME_TAG_DEPTH.set(NAME_TAG_DEPTH.get() + 1); }
    public static void endNameTag() {
        int depth = NAME_TAG_DEPTH.get() - 1;
        if (depth <= 0) NAME_TAG_DEPTH.remove(); else NAME_TAG_DEPTH.set(depth);
    }
    public static int renderingPercent() {
        if (NAME_TAG_DEPTH.get() > 0) return 100;
        Integer preview = PREVIEW_RENDERING.get();
        if (preview != null) return preview;
        Integer value = RENDERING.get();
        return value == null ? 100 : value;
    }
    public static float multiplyAlpha(float alpha) {
        return alpha * (renderingPercent() / 100.0F);
    }
}

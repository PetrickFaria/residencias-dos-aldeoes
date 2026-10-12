package br.com.residenciasaldeoes.data;

/** Rendering policy for a resident's custom villager UV texture. */
public enum SkinApplicationMode {
    /** Replaces the visible vanilla biome, profession, and level outfit layers. */
    COMPLETE,
    /** Keeps vanilla outfit layers visible over the custom base texture. */
    PARTIAL;

    public static SkinApplicationMode fromSerialized(String value) {
        if (value == null) return COMPLETE;
        try {
            return valueOf(value);
        } catch (IllegalArgumentException ignored) {
            return COMPLETE;
        }
    }
}

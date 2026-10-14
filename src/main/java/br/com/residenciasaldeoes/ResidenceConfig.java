package br.com.residenciasaldeoes;

import net.minecraftforge.common.ForgeConfigSpec;

/** Server-side gameplay values. Defaults preserve the behavior approved in 1.0.49. */
public final class ResidenceConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue BASE_FRAGMENTS_PER_RESIDENT;
    public static final ForgeConfigSpec.IntValue PAYMENT_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue COLLECTION_DELAY_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_FRAGMENTS_PER_TRIP;
    public static final ForgeConfigSpec.IntValue CHEST_SEARCH_RADIUS;
    public static final ForgeConfigSpec SPEC;

    static {
        BUILDER.push("pagamento");
        BASE_FRAGMENTS_PER_RESIDENT = BUILDER
                .comment("Fragmentos base por residente; indicadores podem acrescentar até 4.")
                .defineInRange("fragmentosBasePorResidente", 3, 0, 64);
        PAYMENT_DELAY_TICKS = BUILDER
                .comment("Espera após o amanhecer antes do pagamento (20 ticks = 1 segundo).")
                .defineInRange("esperaPagamentoTicks", 200, 20, 12000);
        BUILDER.pop();

        BUILDER.push("transporte");
        COLLECTION_DELAY_TICKS = BUILDER
                .comment("Espera após o pagamento antes de iniciar o transporte (20 ticks = 1 segundo).")
                .defineInRange("esperaTransporteTicks", 300, 20, 12000);
        MAX_FRAGMENTS_PER_TRIP = BUILDER
                .comment("Máximo de fragmentos retirados do Bloco Residencial por viagem.")
                .defineInRange("maximoPorViagem", 15, 1, 64);
        CHEST_SEARCH_RADIUS = BUILDER
                .comment("Raio de localização de Baús Esmerálicos, em blocos; só consulta chunks já carregados.")
                .defineInRange("alcanceLocalizacaoBau", 1000, 16, 4096);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    private ResidenceConfig() {}
}

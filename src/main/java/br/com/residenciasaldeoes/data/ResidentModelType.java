package br.com.residenciasaldeoes.data;

/** Visual geometry only; the underlying entity remains a vanilla Villager. */
public enum ResidentModelType {
    VILLAGER,
    STEVE;

    public static ResidentModelType byId(int id) {
        return id == 1 ? STEVE : VILLAGER;
    }
}

package com.zetsumei.nocoin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(
    modid = Nocoin.MODID,
    bus = Mod.EventBusSubscriber.Bus.MOD
)
public class Config {

    private static final ForgeConfigSpec.Builder BUILDER =
        new ForgeConfigSpec.Builder();

    // ============== Configuration des drops NOCOIN ==============

    private static final ForgeConfigSpec.IntValue DEFAULT_MONSTER_DROP_MIN =
        BUILDER.comment(
            "Minimum de NOCOIN droppé par les mobs hostiles"
        ).defineInRange("defaultMonsterDropMin", 1, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue DEFAULT_MONSTER_DROP_MAX =
        BUILDER.comment(
            "Maximum de NOCOIN droppé par les mobs hostiles"
        ).defineInRange("defaultMonsterDropMax", 3, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.IntValue DEFAULT_PASSIVE_DROPS =
        BUILDER.comment(
            "NOCOIN droppé par les mobs passifs (vaches, cochons, etc.)"
        ).defineInRange("defaultPassiveDrops", 0, 0, Integer.MAX_VALUE);

    // Liste des mobs avec des drops personnalisés (format: "minecraft:zombie=10")
    private static final ForgeConfigSpec.ConfigValue<
        List<? extends String>
    > CUSTOM_MOB_DROPS = BUILDER.comment(
        "Drops personnalisés par mob (format: 'minecraft:zombie=10')",
        "Les mobs listés ici ignoreront les valeurs par défaut"
    ).defineListAllowEmpty(
        "customMobDrops",
        List.of(
            "minecraft:ender_dragon=1000",
            "minecraft:wither=500",
            "minecraft:elder_guardian=100",
            "minecraft:warden=200"
        ),
        Config::validateMobDropEntry
    );

    // ============== Configuration du Gacha ==============

    private static final ForgeConfigSpec.LongValue GACHA_KEY_PRICE =
        BUILDER.comment("Prix d'une Clé Gacha en NOCOIN").defineInRange(
            "gachaKeyPrice",
            50L,
            1L,
            Long.MAX_VALUE
        );

    // ============== Configuration de la boutique NOCOIN ==============

    private static final ForgeConfigSpec.ConfigValue<
        List<? extends String>
    > SHOP_ITEMS = BUILDER.comment(
        "Articles de base dans la boutique NOCOIN (chargés au démarrage)",
        "Les admins peuvent ajouter/modifier/supprimer des articles avec /nocoin shop admin",
        "Format: 'minecraft:item_id;prix;quantité;nom_affiché' ou 'minecraft:item_id;prix;quantité'",
        "La boutique est vide par défaut - utilisez les commandes admin pour ajouter des articles"
    ).defineListAllowEmpty(
        "shopItems",
        List.of(),
        Config::validateShopItemEntry
    );

    static final ForgeConfigSpec SPEC = BUILDER.build();

    // Variables accessibles depuis le code
    public static int defaultMonsterDropMin;
    public static int defaultMonsterDropMax;
    public static int defaultPassiveDrops;
    public static Map<String, Long> mobDrops;
    public static List<ShopItemConfig> shopItems;
    public static long gachaKeyPrice;

    private static boolean validateMobDropEntry(final Object obj) {
        if (!(obj instanceof String entry)) {
            return false;
        }
        // Format attendu: "namespace:mob_id=amount"
        if (!entry.contains("=")) {
            return false;
        }
        String[] parts = entry.split("=");
        if (parts.length != 2) {
            return false;
        }
        try {
            Long.parseLong(parts[1]);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static boolean validateShopItemEntry(final Object obj) {
        if (!(obj instanceof String entry)) {
            return false;
        }
        // Format attendu: "namespace:item_id;prix;quantité" ou "namespace:item_id;prix;quantité;nom"
        String[] parts = entry.split(";");
        if (parts.length < 3 || parts.length > 4) {
            return false;
        }
        // Vérifie que l'ID de l'item contient un namespace
        if (!parts[0].contains(":")) {
            return false;
        }
        try {
            Long.parseLong(parts[1]); // prix
            Integer.parseInt(parts[2]); // quantité
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        defaultMonsterDropMin = DEFAULT_MONSTER_DROP_MIN.get();
        defaultMonsterDropMax = DEFAULT_MONSTER_DROP_MAX.get();
        defaultPassiveDrops = DEFAULT_PASSIVE_DROPS.get();

        // Parse les drops personnalisés
        mobDrops = new HashMap<>();
        for (String entry : CUSTOM_MOB_DROPS.get()) {
            String[] parts = entry.split("=");
            if (parts.length == 2) {
                try {
                    mobDrops.put(parts[0], Long.parseLong(parts[1]));
                } catch (NumberFormatException ignored) {}
            }
        }

        // Parse les articles de la boutique
        shopItems = new ArrayList<>();
        for (String entry : SHOP_ITEMS.get()) {
            String[] parts = entry.split(";");
            if (parts.length >= 3) {
                try {
                    String itemId = parts[0];
                    long price = Long.parseLong(parts[1]);
                    int quantity = Integer.parseInt(parts[2]);
                    String displayName = parts.length >= 4 ? parts[3] : null;
                    shopItems.add(
                        new ShopItemConfig(itemId, price, quantity, displayName)
                    );
                } catch (NumberFormatException ignored) {}
            }
        }

        // Configuration Gacha
        gachaKeyPrice = GACHA_KEY_PRICE.get();

        // Recharger le ShopManager avec la nouvelle configuration
        com.zetsumei.nocoin.shop.ShopManager.getInstance().reloadFromConfig();
    }

    /**
     * Classe représentant un article de boutique configuré.
     */
    public static class ShopItemConfig {

        private final String itemId;
        private final long price;
        private final int quantity;
        private final String displayName;

        public ShopItemConfig(
            String itemId,
            long price,
            int quantity,
            String displayName
        ) {
            this.itemId = itemId;
            this.price = price;
            this.quantity = quantity;
            this.displayName = displayName;
        }

        public String getItemId() {
            return itemId;
        }

        public long getPrice() {
            return price;
        }

        public int getQuantity() {
            return quantity;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean hasCustomDisplayName() {
            return displayName != null && !displayName.isEmpty();
        }
    }
}

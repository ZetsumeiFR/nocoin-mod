package com.zetsumei.nocoin.shop;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestionnaire de persistance pour les articles de la boutique NOCOIN.
 * Sauvegarde et charge les articles depuis un fichier JSON.
 */
public class ShopPersistence {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SHOP_FILE_NAME = "nocoin_shop.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Récupère le chemin du fichier de sauvegarde de la boutique.
     */
    public static Path getShopFilePath() {
        return FMLPaths.CONFIGDIR.get().resolve(SHOP_FILE_NAME);
    }

    /**
     * Sauvegarde les articles de la boutique dans le fichier JSON.
     *
     * @param items la liste des articles à sauvegarder
     * @return true si la sauvegarde a réussi
     */
    public static boolean saveShopItems(List<ShopItemData> items) {
        Path filePath = getShopFilePath();

        try (Writer writer = Files.newBufferedWriter(filePath)) {
            GSON.toJson(items, writer);
            LOGGER.info("Boutique NOCOIN sauvegardée: {} articles", items.size());
            return true;
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la sauvegarde de la boutique: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Charge les articles de la boutique depuis le fichier JSON.
     *
     * @return la liste des articles, ou une liste vide si le fichier n'existe pas
     */
    public static List<ShopItemData> loadShopItems() {
        Path filePath = getShopFilePath();

        if (!Files.exists(filePath)) {
            LOGGER.info("Fichier boutique non trouvé, création d'une boutique vide");
            return new ArrayList<>();
        }

        try (Reader reader = Files.newBufferedReader(filePath)) {
            Type listType = new TypeToken<List<ShopItemData>>() {}.getType();
            List<ShopItemData> items = GSON.fromJson(reader, listType);
            if (items == null) {
                items = new ArrayList<>();
            }
            LOGGER.info("Boutique NOCOIN chargée: {} articles", items.size());
            return items;
        } catch (IOException e) {
            LOGGER.error("Erreur lors du chargement de la boutique: {}", e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            LOGGER.error("Erreur de parsing du fichier boutique: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Classe de données pour la sérialisation JSON des articles.
     */
    public static class ShopItemData {
        private String itemId;
        private long price;
        private int quantity;
        private String displayName;

        public ShopItemData() {
            // Constructeur vide requis pour Gson
        }

        public ShopItemData(String itemId, long price, int quantity, String displayName) {
            this.itemId = itemId;
            this.price = price;
            this.quantity = quantity;
            this.displayName = displayName;
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public long getPrice() {
            return price;
        }

        public void setPrice(long price) {
            this.price = price;
        }

        public int getQuantity() {
            return quantity;
        }

        public void setQuantity(int quantity) {
            this.quantity = quantity;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}

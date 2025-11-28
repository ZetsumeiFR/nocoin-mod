package com.zetsumei.nocoin.shop;

import com.mojang.logging.LogUtils;
import com.zetsumei.nocoin.Config;
import com.zetsumei.nocoin.capability.NocoinCapabilityProvider;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Gestionnaire de la boutique NOCOIN.
 * Gère les articles disponibles et traite les achats.
 * Les admins peuvent ajouter/modifier/supprimer des articles dynamiquement.
 */
public class ShopManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static ShopManager instance;

    private final List<ShopItem> items;

    private ShopManager() {
        this.items = new ArrayList<>();
    }

    /**
     * Récupère l'instance singleton du gestionnaire de boutique.
     */
    public static ShopManager getInstance() {
        if (instance == null) {
            instance = new ShopManager();
        }
        return instance;
    }

    /**
     * Initialise la boutique au démarrage du serveur.
     * Charge d'abord depuis le fichier JSON, puis ajoute les items de la config.
     */
    public void initialize() {
        items.clear();

        // Charger depuis le fichier JSON persisté
        List<ShopPersistence.ShopItemData> persistedItems = ShopPersistence.loadShopItems();
        int id = 0;
        for (ShopPersistence.ShopItemData data : persistedItems) {
            ShopItem shopItem = new ShopItem(id++, data.getItemId(), data.getPrice(), data.getQuantity(), data.getDisplayName());
            if (shopItem.isValid()) {
                items.add(shopItem);
                LOGGER.debug("Article boutique chargé (JSON): {} ({})", shopItem.getItemId(), shopItem.getPrice());
            } else {
                LOGGER.warn("Article boutique invalide ignoré: {}", data.getItemId());
            }
        }

        LOGGER.info("Boutique NOCOIN initialisée: {} articles", items.size());
    }

    /**
     * Recharge les articles depuis la configuration.
     * Appelé lors du chargement de la configuration Forge.
     * Ajoute les items de la config aux items existants (du fichier JSON).
     */
    public void reloadFromConfig() {
        // Si la boutique n'a jamais été initialisée, l'initialiser
        if (items.isEmpty()) {
            initialize();
        }

        // Ajouter les items de la config (s'il y en a)
        if (Config.shopItems != null && !Config.shopItems.isEmpty()) {
            int nextId = items.isEmpty() ? 0 : items.stream().mapToInt(ShopItem::getId).max().orElse(0) + 1;
            for (Config.ShopItemConfig config : Config.shopItems) {
                // Vérifier si l'item n'existe pas déjà
                if (items.stream().noneMatch(i -> i.getItemId().equals(config.getItemId()))) {
                    ShopItem shopItem = new ShopItem(nextId++, config);
                    if (shopItem.isValid()) {
                        items.add(shopItem);
                        LOGGER.debug("Article boutique ajouté (config): {} ({})", shopItem.getItemId(), shopItem.getPrice());
                    }
                }
            }
        }
    }

    /**
     * Sauvegarde les articles actuels dans le fichier JSON.
     *
     * @return true si la sauvegarde a réussi
     */
    public boolean saveToFile() {
        List<ShopPersistence.ShopItemData> dataList = new ArrayList<>();
        for (ShopItem item : items) {
            dataList.add(new ShopPersistence.ShopItemData(
                    item.getItemId(),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getDisplayName()
            ));
        }
        return ShopPersistence.saveShopItems(dataList);
    }

    /**
     * Recharge la boutique depuis le fichier JSON.
     */
    public void reloadFromFile() {
        initialize();
        LOGGER.info("Boutique rechargée depuis le fichier");
    }

    /**
     * Ajoute un nouvel article à la boutique (quantité = 1 par défaut).
     *
     * @param itemId      l'identifiant Minecraft de l'item (ex: "minecraft:diamond")
     * @param price       le prix en NOCOIN
     * @param displayName le nom d'affichage personnalisé (peut être null)
     * @return le résultat de l'opération
     */
    public AddItemResult addItem(String itemId, long price, String displayName) {
        return addItem(itemId, price, 1, displayName);
    }

    /**
     * Ajoute un nouvel article à la boutique avec une quantité spécifique.
     *
     * @param itemId      l'identifiant Minecraft de l'item (ex: "minecraft:diamond")
     * @param price       le prix en NOCOIN
     * @param quantity    la quantité donnée
     * @param displayName le nom d'affichage personnalisé (peut être null)
     * @return le résultat de l'opération
     */
    public AddItemResult addItem(String itemId, long price, int quantity, String displayName) {
        // Vérifier que l'item Minecraft existe
        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
        if (resourceLocation == null) {
            return AddItemResult.failure("ID d'item invalide: " + itemId);
        }

        Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
        if (item == null) {
            return AddItemResult.failure("Item non trouvé: " + itemId);
        }

        // Vérifier que l'item n'existe pas déjà dans la boutique
        if (items.stream().anyMatch(i -> i.getItemId().equals(itemId))) {
            return AddItemResult.failure("Cet article existe déjà dans la boutique");
        }

        // Créer le nouvel article
        int newId = items.isEmpty() ? 0 : items.stream().mapToInt(ShopItem::getId).max().orElse(0) + 1;
        ShopItem shopItem = new ShopItem(newId, itemId, price, quantity, displayName);

        items.add(shopItem);

        // Sauvegarder automatiquement
        saveToFile();

        LOGGER.info("Article ajouté à la boutique: {} (prix: {})", itemId, price);
        return AddItemResult.success(shopItem);
    }

    /**
     * Modifie un article existant dans la boutique.
     *
     * @param shopItemId  l'ID de l'article dans la boutique
     * @param newPrice    le nouveau prix (null pour ne pas modifier)
     * @param newDisplayName le nouveau nom d'affichage (null pour ne pas modifier, chaîne vide pour supprimer)
     * @return le résultat de l'opération
     */
    public ModifyItemResult modifyItem(int shopItemId, Long newPrice, String newDisplayName) {
        Optional<ShopItem> optItem = getItemById(shopItemId);
        if (optItem.isEmpty()) {
            return ModifyItemResult.failure("Article non trouvé avec l'ID: " + shopItemId);
        }

        ShopItem oldItem = optItem.get();

        // Créer un nouvel article avec les modifications
        long price = newPrice != null ? newPrice : oldItem.getPrice();
        String displayName = newDisplayName != null ?
                (newDisplayName.isEmpty() ? null : newDisplayName) :
                oldItem.getDisplayName();

        ShopItem newItem = new ShopItem(shopItemId, oldItem.getItemId(), price, oldItem.getQuantity(), displayName);

        // Remplacer l'ancien article
        int index = items.indexOf(oldItem);
        items.set(index, newItem);

        // Sauvegarder automatiquement
        saveToFile();

        LOGGER.info("Article modifié dans la boutique: {} (nouveau prix: {})",
                newItem.getItemId(), price);
        return ModifyItemResult.success(newItem);
    }

    /**
     * Supprime un article de la boutique.
     *
     * @param shopItemId l'ID de l'article à supprimer
     * @return le résultat de l'opération
     */
    public RemoveItemResult removeItem(int shopItemId) {
        Optional<ShopItem> optItem = getItemById(shopItemId);
        if (optItem.isEmpty()) {
            return RemoveItemResult.failure("Article non trouvé avec l'ID: " + shopItemId);
        }

        ShopItem item = optItem.get();
        items.remove(item);

        // Réassigner les IDs pour garder la cohérence
        reassignIds();

        // Sauvegarder automatiquement
        saveToFile();

        LOGGER.info("Article supprimé de la boutique: {}", item.getItemId());
        return RemoveItemResult.success(item);
    }

    /**
     * Vide complètement la boutique.
     *
     * @return le nombre d'articles supprimés
     */
    public int clearShop() {
        int count = items.size();
        items.clear();
        saveToFile();
        LOGGER.info("Boutique vidée: {} articles supprimés", count);
        return count;
    }

    /**
     * Réassigne les IDs des articles après une suppression.
     */
    private void reassignIds() {
        List<ShopItem> newItems = new ArrayList<>();
        int newId = 0;
        for (ShopItem item : items) {
            newItems.add(new ShopItem(newId++, item.getItemId(), item.getPrice(), item.getQuantity(), item.getDisplayName()));
        }
        items.clear();
        items.addAll(newItems);
    }

    /**
     * Récupère la liste des articles disponibles (lecture seule).
     */
    public List<ShopItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    /**
     * Récupère un article par son ID.
     */
    public Optional<ShopItem> getItemById(int id) {
        return items.stream().filter(item -> item.getId() == id).findFirst();
    }

    /**
     * Tente d'effectuer un achat pour un joueur.
     *
     * @param player le joueur qui achète
     * @param shopItemId l'ID de l'article à acheter
     * @return le résultat de l'achat
     */
    public PurchaseResult processPurchase(ServerPlayer player, int shopItemId) {
        // Vérifier que l'article existe
        Optional<ShopItem> optItem = getItemById(shopItemId);
        if (optItem.isEmpty()) {
            return PurchaseResult.failure(PurchaseResult.FailureReason.ITEM_NOT_FOUND);
        }

        ShopItem shopItem = optItem.get();

        // Vérifier que l'article est valide
        if (!shopItem.isValid()) {
            return PurchaseResult.failure(PurchaseResult.FailureReason.ITEM_INVALID);
        }

        // Vérifier que le joueur a assez de NOCOIN
        final PurchaseResult[] result = {null};

        player.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
            long price = shopItem.getPrice();

            if (!cap.hasEnough(price)) {
                result[0] = PurchaseResult.failure(PurchaseResult.FailureReason.INSUFFICIENT_BALANCE);
                return;
            }

            // Vérifier que l'inventaire a de la place
            ItemStack itemToGive = shopItem.createItemStack();
            if (!canAddToInventory(player, itemToGive)) {
                result[0] = PurchaseResult.failure(PurchaseResult.FailureReason.INVENTORY_FULL);
                return;
            }

            // Effectuer la transaction
            cap.removeBalance(price);

            // Donner l'item au joueur
            if (!player.getInventory().add(itemToGive)) {
                // En cas de problème (ne devrait pas arriver), drop l'item
                player.drop(itemToGive, false);
            }

            // Synchroniser le solde
            NocoinNetworkHandler.sendBalanceToClient(player, cap.getBalance());

            LOGGER.info("Joueur {} a acheté {} pour {} NOCOIN",
                    player.getName().getString(),
                    shopItem.getItemId(),
                    price);

            result[0] = PurchaseResult.success(shopItem, cap.getBalance());
        });

        return result[0] != null ? result[0] : PurchaseResult.failure(PurchaseResult.FailureReason.UNKNOWN_ERROR);
    }

    /**
     * Vérifie si un ItemStack peut être ajouté à l'inventaire du joueur.
     */
    private boolean canAddToInventory(ServerPlayer player, ItemStack stack) {
        // Créer une copie pour tester
        ItemStack testStack = stack.copy();

        // Vérifier si on peut ajouter aux stacks existants
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (slotStack.isEmpty()) {
                return true; // Slot vide trouvé
            }
            if (ItemStack.isSameItemSameTags(slotStack, testStack)) {
                int canAdd = slotStack.getMaxStackSize() - slotStack.getCount();
                if (canAdd >= testStack.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    // ============== Classes de résultats ==============

    /**
     * Résultat d'une tentative d'ajout d'article.
     */
    public static class AddItemResult {
        private final boolean success;
        private final String errorMessage;
        private final ShopItem addedItem;

        private AddItemResult(boolean success, String errorMessage, ShopItem addedItem) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.addedItem = addedItem;
        }

        public static AddItemResult success(ShopItem item) {
            return new AddItemResult(true, null, item);
        }

        public static AddItemResult failure(String errorMessage) {
            return new AddItemResult(false, errorMessage, null);
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public ShopItem getAddedItem() { return addedItem; }
    }

    /**
     * Résultat d'une tentative de modification d'article.
     */
    public static class ModifyItemResult {
        private final boolean success;
        private final String errorMessage;
        private final ShopItem modifiedItem;

        private ModifyItemResult(boolean success, String errorMessage, ShopItem modifiedItem) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.modifiedItem = modifiedItem;
        }

        public static ModifyItemResult success(ShopItem item) {
            return new ModifyItemResult(true, null, item);
        }

        public static ModifyItemResult failure(String errorMessage) {
            return new ModifyItemResult(false, errorMessage, null);
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public ShopItem getModifiedItem() { return modifiedItem; }
    }

    /**
     * Résultat d'une tentative de suppression d'article.
     */
    public static class RemoveItemResult {
        private final boolean success;
        private final String errorMessage;
        private final ShopItem removedItem;

        private RemoveItemResult(boolean success, String errorMessage, ShopItem removedItem) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.removedItem = removedItem;
        }

        public static RemoveItemResult success(ShopItem item) {
            return new RemoveItemResult(true, null, item);
        }

        public static RemoveItemResult failure(String errorMessage) {
            return new RemoveItemResult(false, errorMessage, null);
        }

        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public ShopItem getRemovedItem() { return removedItem; }
    }

    /**
     * Résultat d'une tentative d'achat.
     */
    public static class PurchaseResult {
        private final boolean success;
        private final FailureReason failureReason;
        private final ShopItem purchasedItem;
        private final long newBalance;

        private PurchaseResult(boolean success, FailureReason failureReason, ShopItem purchasedItem, long newBalance) {
            this.success = success;
            this.failureReason = failureReason;
            this.purchasedItem = purchasedItem;
            this.newBalance = newBalance;
        }

        public static PurchaseResult success(ShopItem item, long newBalance) {
            return new PurchaseResult(true, null, item, newBalance);
        }

        public static PurchaseResult failure(FailureReason reason) {
            return new PurchaseResult(false, reason, null, -1);
        }

        public boolean isSuccess() {
            return success;
        }

        public FailureReason getFailureReason() {
            return failureReason;
        }

        public ShopItem getPurchasedItem() {
            return purchasedItem;
        }

        public long getNewBalance() {
            return newBalance;
        }

        public Component getMessageComponent() {
            if (success) {
                Component message = Component.literal("Achat réussi : ")
                        .append(purchasedItem.getDisplayComponent());
                if (purchasedItem.getQuantity() > 1) {
                    message = message.copy().append(Component.literal(" x" + purchasedItem.getQuantity()));
                }
                return message.copy().append(Component.literal(" pour " + String.format("%,d", purchasedItem.getPrice()) + " NOCOIN"));
            } else {
                return switch (failureReason) {
                    case ITEM_NOT_FOUND -> Component.literal("Article non trouvé");
                    case ITEM_INVALID -> Component.literal("Article invalide");
                    case INSUFFICIENT_BALANCE -> Component.literal("Solde NOCOIN insuffisant");
                    case INVENTORY_FULL -> Component.literal("Inventaire plein");
                    case UNKNOWN_ERROR -> Component.literal("Erreur inconnue");
                };
            }
        }

        public enum FailureReason {
            ITEM_NOT_FOUND,
            ITEM_INVALID,
            INSUFFICIENT_BALANCE,
            INVENTORY_FULL,
            UNKNOWN_ERROR
        }
    }
}

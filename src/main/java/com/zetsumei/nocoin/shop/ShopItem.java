package com.zetsumei.nocoin.shop;

import com.zetsumei.nocoin.Config;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Représente un article disponible dans la boutique NOCOIN.
 */
public class ShopItem {

    private final int id;
    private final String itemId;
    private final long price;
    private final int quantity;
    private final String displayName;
    private ItemStack cachedItemStack;

    public ShopItem(int id, String itemId, long price, int quantity, String displayName) {
        this.id = id;
        this.itemId = itemId;
        this.price = price;
        this.quantity = quantity;
        this.displayName = displayName;
    }

    public ShopItem(int id, Config.ShopItemConfig config) {
        this(id, config.getItemId(), config.getPrice(), config.getQuantity(), config.getDisplayName());
    }

    /**
     * Crée un ShopItem depuis un buffer réseau.
     */
    public static ShopItem fromNetwork(FriendlyByteBuf buf) {
        int id = buf.readVarInt();
        String itemId = buf.readUtf();
        long price = buf.readLong();
        int quantity = buf.readVarInt();
        boolean hasDisplayName = buf.readBoolean();
        String displayName = hasDisplayName ? buf.readUtf() : null;
        return new ShopItem(id, itemId, price, quantity, displayName);
    }

    /**
     * Écrit le ShopItem dans un buffer réseau.
     */
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeVarInt(id);
        buf.writeUtf(itemId);
        buf.writeLong(price);
        buf.writeVarInt(quantity);
        buf.writeBoolean(displayName != null);
        if (displayName != null) {
            buf.writeUtf(displayName);
        }
    }

    /**
     * Récupère l'identifiant unique de l'article dans la boutique.
     */
    public int getId() {
        return id;
    }

    /**
     * Récupère l'identifiant de l'item Minecraft (ex: "minecraft:diamond").
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Récupère le prix en NOCOIN.
     */
    public long getPrice() {
        return price;
    }

    /**
     * Récupère la quantité donnée lors de l'achat.
     */
    public int getQuantity() {
        return quantity;
    }

    /**
     * Récupère le nom d'affichage personnalisé.
     * @return le nom personnalisé ou null si non défini
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Vérifie si l'article a un nom d'affichage personnalisé.
     */
    public boolean hasCustomDisplayName() {
        return displayName != null && !displayName.isEmpty();
    }

    /**
     * Crée l'ItemStack correspondant à cet article.
     * @return l'ItemStack ou ItemStack.EMPTY si l'item n'existe pas
     */
    public ItemStack createItemStack() {
        if (cachedItemStack == null) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
            if (resourceLocation != null) {
                Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
                if (item != null) {
                    cachedItemStack = new ItemStack(item, quantity);
                } else {
                    cachedItemStack = ItemStack.EMPTY;
                }
            } else {
                cachedItemStack = ItemStack.EMPTY;
            }
        }
        return cachedItemStack.copy();
    }

    /**
     * Vérifie si l'article est valide (l'item existe dans le jeu).
     */
    public boolean isValid() {
        return !createItemStack().isEmpty();
    }

    /**
     * Récupère le composant de texte pour l'affichage du nom.
     */
    public Component getDisplayComponent() {
        if (hasCustomDisplayName()) {
            return Component.literal(displayName);
        }
        ItemStack stack = createItemStack();
        if (!stack.isEmpty()) {
            return stack.getHoverName();
        }
        return Component.literal(itemId);
    }

    /**
     * Récupère le texte du prix formaté.
     */
    public Component getPriceComponent() {
        return Component.literal(String.format("%,d", price) + " NOCOIN");
    }

    /**
     * Récupère le texte de quantité.
     */
    public Component getQuantityComponent() {
        return Component.literal("x" + quantity);
    }
}

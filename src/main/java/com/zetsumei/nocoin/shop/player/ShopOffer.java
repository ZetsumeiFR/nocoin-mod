package com.zetsumei.nocoin.shop.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

/**
 * Représente une offre dans un magasin joueur.
 * Peut être une offre d'achat (le propriétaire achète aux visiteurs) ou de vente (le propriétaire vend aux visiteurs).
 */
public class ShopOffer {

    public enum OfferType {
        SELL,  // Le propriétaire vend l'item aux visiteurs
        BUY    // Le propriétaire achète l'item aux visiteurs
    }

    private final UUID offerId;
    private final OfferType type;
    private final String itemId;
    private int quantity;
    private long pricePerUnit;
    private int stock;  // Pour les offres de vente: nombre d'items en stock
                        // Pour les offres d'achat: nombre max d'items à acheter (0 = illimité)
    private boolean active;

    // Cache pour l'ItemStack
    private transient ItemStack cachedItemStack;

    /**
     * Crée une nouvelle offre.
     */
    public ShopOffer(OfferType type, String itemId, int quantity, long pricePerUnit, int stock) {
        this.offerId = UUID.randomUUID();
        this.type = type;
        this.itemId = itemId;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.stock = stock;
        this.active = true;
    }

    /**
     * Constructeur privé pour la désérialisation.
     */
    private ShopOffer(UUID offerId, OfferType type, String itemId, int quantity, long pricePerUnit, int stock, boolean active) {
        this.offerId = offerId;
        this.type = type;
        this.itemId = itemId;
        this.quantity = quantity;
        this.pricePerUnit = pricePerUnit;
        this.stock = stock;
        this.active = active;
    }

    // ============== Sérialisation NBT ==============

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("offerId", offerId);
        tag.putString("type", type.name());
        tag.putString("itemId", itemId);
        tag.putInt("quantity", quantity);
        tag.putLong("pricePerUnit", pricePerUnit);
        tag.putInt("stock", stock);
        tag.putBoolean("active", active);
        return tag;
    }

    public static ShopOffer load(CompoundTag tag) {
        UUID offerId = tag.getUUID("offerId");
        OfferType type = OfferType.valueOf(tag.getString("type"));
        String itemId = tag.getString("itemId");
        int quantity = tag.getInt("quantity");
        long pricePerUnit = tag.getLong("pricePerUnit");
        int stock = tag.getInt("stock");
        boolean active = tag.getBoolean("active");
        return new ShopOffer(offerId, type, itemId, quantity, pricePerUnit, stock, active);
    }

    // ============== Sérialisation Réseau ==============

    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUUID(offerId);
        buf.writeEnum(type);
        buf.writeUtf(itemId);
        buf.writeVarInt(quantity);
        buf.writeLong(pricePerUnit);
        buf.writeVarInt(stock);
        buf.writeBoolean(active);
    }

    public static ShopOffer fromNetwork(FriendlyByteBuf buf) {
        UUID offerId = buf.readUUID();
        OfferType type = buf.readEnum(OfferType.class);
        String itemId = buf.readUtf();
        int quantity = buf.readVarInt();
        long pricePerUnit = buf.readLong();
        int stock = buf.readVarInt();
        boolean active = buf.readBoolean();
        return new ShopOffer(offerId, type, itemId, quantity, pricePerUnit, stock, active);
    }

    // ============== Getters ==============

    public UUID getOfferId() {
        return offerId;
    }

    public OfferType getType() {
        return type;
    }

    public String getItemId() {
        return itemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public long getPricePerUnit() {
        return pricePerUnit;
    }

    public long getTotalPrice() {
        return pricePerUnit * quantity;
    }

    public int getStock() {
        return stock;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setStock(int stock) {
        this.stock = Math.max(0, stock);
    }


    public void setPricePerUnit(long pricePerUnit) {
        this.pricePerUnit = Math.max(1, pricePerUnit);
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.max(1, quantity);
        this.cachedItemStack = null; // Invalider le cache
    }

    public void addStock(int amount) {
        this.stock = Math.max(0, this.stock + amount);
    }

    public void removeStock(int amount) {
        this.stock = Math.max(0, this.stock - amount);
    }

    /**
     * Vérifie si l'offre a du stock disponible.
     * Pour les offres d'achat, stock = 0 signifie illimité.
     */
    public boolean hasStock() {
        if (type == OfferType.BUY) {
            return stock == 0 || stock >= quantity;
        }
        return stock >= quantity;
    }

    // ============== ItemStack ==============

    /**
     * Crée l'ItemStack correspondant à cette offre.
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
     * Vérifie si l'offre est valide (l'item existe).
     */
    public boolean isValid() {
        return !createItemStack().isEmpty();
    }

    // ============== Affichage ==============

    /**
     * Récupère le nom d'affichage de l'item.
     */
    public Component getItemDisplayName() {
        ItemStack stack = createItemStack();
        if (!stack.isEmpty()) {
            return stack.getHoverName();
        }
        return Component.literal(itemId);
    }

    /**
     * Récupère le composant de prix formaté.
     */
    public Component getPriceComponent() {
        return Component.literal(String.format("%,d", getTotalPrice()) + " NC");
    }

    /**
     * Récupère le composant de type d'offre.
     */
    public Component getTypeComponent() {
        return type == OfferType.SELL
                ? Component.literal("Vente")
                : Component.literal("Achat");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ShopOffer other = (ShopOffer) obj;
        return offerId.equals(other.offerId);
    }

    @Override
    public int hashCode() {
        return offerId.hashCode();
    }
}

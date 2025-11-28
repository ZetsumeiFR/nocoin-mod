package com.zetsumei.nocoin.gacha;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Représente une récompense possible dans le Gacha.
 * Supporte la configuration dynamique et la persistance JSON.
 */
public class GachaReward {
    private final String itemId;
    private final GachaRarity rarity;
    private final String displayName;
    private final double weight;
    private transient Item cachedItem;

    /**
     * Constructeur principal pour la configuration dynamique.
     */
    public GachaReward(String itemId, GachaRarity rarity, String displayName, double weight) {
        this.itemId = itemId;
        this.rarity = rarity;
        this.displayName = displayName;
        this.weight = weight;
    }

    /**
     * Constructeur à partir d'un Item Forge (rétrocompatibilité).
     */
    public GachaReward(Item item, GachaRarity rarity, String displayName, double weight) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        this.itemId = key != null ? key.toString() : "minecraft:air";
        this.rarity = rarity;
        this.displayName = displayName;
        this.weight = weight;
        this.cachedItem = item;
    }

    /**
     * Récupère l'ID de l'item (ex: "minecraft:diamond").
     */
    public String getItemId() {
        return itemId;
    }

    /**
     * Récupère l'Item Minecraft correspondant.
     */
    public Item getItem() {
        if (cachedItem == null) {
            ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
            if (resourceLocation != null) {
                cachedItem = ForgeRegistries.ITEMS.getValue(resourceLocation);
            }
        }
        return cachedItem != null ? cachedItem : Items.AIR;
    }

    public GachaRarity getRarity() {
        return rarity;
    }

    public String getCharacterName() {
        return displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getWeight() {
        return weight;
    }

    public ItemStack createStack() {
        return new ItemStack(getItem());
    }

    /**
     * Vérifie si la récompense est valide (l'item existe).
     */
    public boolean isValid() {
        return getItem() != Items.AIR;
    }

    /**
     * Crée une copie avec un nouveau poids.
     */
    public GachaReward withWeight(double newWeight) {
        return new GachaReward(itemId, rarity, displayName, newWeight);
    }

    /**
     * Sérialise vers JSON.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("itemId", itemId);
        json.addProperty("rarity", rarity.name());
        json.addProperty("displayName", displayName);
        json.addProperty("weight", weight);
        return json;
    }

    /**
     * Désérialise depuis JSON.
     */
    public static GachaReward fromJson(JsonObject json) {
        String itemId = json.get("itemId").getAsString();
        GachaRarity rarity = GachaRarity.valueOf(json.get("rarity").getAsString());
        String displayName = json.get("displayName").getAsString();
        double weight = json.get("weight").getAsDouble();
        return new GachaReward(itemId, rarity, displayName, weight);
    }

    @Override
    public String toString() {
        return String.format("GachaReward{item=%s, rarity=%s, name=%s, weight=%.2f}",
                itemId, rarity.name(), displayName, weight);
    }
}

package com.zetsumei.nocoin.gacha;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestionnaire du système de Gacha.
 * Gère les probabilités et le tirage des récompenses.
 * Supporte la configuration dynamique via commandes admin.
 */
public class GachaManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static GachaManager INSTANCE;

    private final List<GachaReward> rewards = new ArrayList<>();
    private final Random random = new Random();
    private final Path configPath;
    private final Gson gson;

    // Probabilités de base pour chaque rareté (peuvent être modifiées)
    private double fiveStarBaseRate = 3.0;   // 3%
    private double fourStarBaseRate = 15.0;  // 15%
    private double threeStarBaseRate = 82.0; // 82%

    private GachaManager() {
        this.configPath = FMLPaths.CONFIGDIR.get().resolve("nocoin_gacha.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        loadOrInitialize();
    }

    public static GachaManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GachaManager();
        }
        return INSTANCE;
    }

    /**
     * Charge la configuration depuis le fichier JSON ou initialise avec les valeurs par défaut.
     */
    private void loadOrInitialize() {
        if (Files.exists(configPath)) {
            loadFromFile();
        } else {
            initializeDefaultRewards();
            saveToFile();
        }
    }

    /**
     * Charge les récompenses depuis le fichier JSON.
     */
    public void loadFromFile() {
        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            
            rewards.clear();
            
            // Charger les probabilités de rareté
            if (root.has("rarityRates")) {
                JsonObject rates = root.getAsJsonObject("rarityRates");
                fiveStarBaseRate = rates.has("fiveStar") ? rates.get("fiveStar").getAsDouble() : 3.0;
                fourStarBaseRate = rates.has("fourStar") ? rates.get("fourStar").getAsDouble() : 15.0;
                threeStarBaseRate = rates.has("threeStar") ? rates.get("threeStar").getAsDouble() : 82.0;
            }
            
            // Charger les récompenses
            if (root.has("rewards")) {
                JsonArray rewardsArray = root.getAsJsonArray("rewards");
                for (JsonElement element : rewardsArray) {
                    try {
                        GachaReward reward = GachaReward.fromJson(element.getAsJsonObject());
                        if (reward.isValid()) {
                            rewards.add(reward);
                        } else {
                            LOGGER.warn("Récompense gacha ignorée (item invalide): {}", reward.getItemId());
                        }
                    } catch (Exception e) {
                        LOGGER.error("Erreur lors du chargement d'une récompense gacha", e);
                    }
                }
            }
            
            LOGGER.info("Gacha chargé: {} récompenses depuis {}", rewards.size(), configPath);
            
        } catch (Exception e) {
            LOGGER.error("Erreur lors du chargement de la configuration gacha, utilisation des valeurs par défaut", e);
            initializeDefaultRewards();
        }
    }

    /**
     * Sauvegarde les récompenses dans le fichier JSON.
     */
    public void saveToFile() {
        try {
            JsonObject root = new JsonObject();
            
            // Sauvegarder les probabilités de rareté
            JsonObject rates = new JsonObject();
            rates.addProperty("fiveStar", fiveStarBaseRate);
            rates.addProperty("fourStar", fourStarBaseRate);
            rates.addProperty("threeStar", threeStarBaseRate);
            root.add("rarityRates", rates);
            
            // Sauvegarder les récompenses
            JsonArray rewardsArray = new JsonArray();
            for (GachaReward reward : rewards) {
                rewardsArray.add(reward.toJson());
            }
            root.add("rewards", rewardsArray);
            
            // Écrire le fichier
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                gson.toJson(root, writer);
            }
            
            LOGGER.info("Configuration gacha sauvegardée: {} récompenses", rewards.size());
            
        } catch (IOException e) {
            LOGGER.error("Erreur lors de la sauvegarde de la configuration gacha", e);
        }
    }

    /**
     * Recharge la configuration depuis le fichier.
     */
    public void reload() {
        loadFromFile();
    }

    /**
     * Initialise la liste des récompenses par défaut.
     * Par défaut, la gacha machine est VIDE - les admins doivent configurer le contenu.
     */
    private void initializeDefaultRewards() {
        rewards.clear();
        // Par défaut, aucune récompense n'est configurée
        // Les admins doivent utiliser /gacha add pour ajouter des items
        LOGGER.info("Gacha initialisé vide - utilisez /gacha add pour configurer les récompenses");
    }

    private void addDefaultReward(Item item, GachaRarity rarity, String name, double weight) {
        rewards.add(new GachaReward(item, rarity, name, weight));
    }

    // ==================== COMMANDES ADMIN ====================

    /**
     * Ajoute une nouvelle récompense au gacha.
     * @param itemId ID de l'item (ex: "minecraft:diamond")
     * @param rarity Rareté de la récompense
     * @param displayName Nom d'affichage
     * @param weight Poids dans le tirage (par défaut 1.0)
     * @return true si ajouté avec succès
     */
    public boolean addReward(String itemId, GachaRarity rarity, String displayName, double weight) {
        // Vérifier que l'item existe
        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
        if (resourceLocation == null) {
            return false;
        }
        
        Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
        if (item == null || item == Items.AIR) {
            return false;
        }
        
        // Vérifier si l'item existe déjà
        Optional<GachaReward> existing = findRewardByItemId(itemId);
        if (existing.isPresent()) {
            return false; // Item déjà présent
        }
        
        GachaReward reward = new GachaReward(itemId, rarity, displayName, weight);
        rewards.add(reward);
        saveToFile();
        
        LOGGER.info("Récompense gacha ajoutée: {} ({}) avec poids {}", displayName, rarity.getDisplayStars(), weight);
        return true;
    }

    /**
     * Retire une récompense du gacha par son ID d'item.
     * @param itemId ID de l'item à retirer
     * @return true si retiré avec succès
     */
    public boolean removeReward(String itemId) {
        Optional<GachaReward> toRemove = findRewardByItemId(itemId);
        if (toRemove.isPresent()) {
            rewards.remove(toRemove.get());
            saveToFile();
            LOGGER.info("Récompense gacha retirée: {}", itemId);
            return true;
        }
        return false;
    }

    /**
     * Vide toutes les récompenses du gacha.
     * @return Le nombre de récompenses supprimées
     */
    public int clearAllRewards() {
        int count = rewards.size();
        rewards.clear();
        saveToFile();
        LOGGER.info("Toutes les récompenses gacha ont été supprimées ({} items)", count);
        return count;
    }

    /**
     * Modifie le poids d'une récompense existante.
     * @param itemId ID de l'item
     * @param newWeight Nouveau poids
     * @return true si modifié avec succès
     */
    public boolean setRewardWeight(String itemId, double newWeight) {
        if (newWeight <= 0) {
            return false;
        }
        
        for (int i = 0; i < rewards.size(); i++) {
            if (rewards.get(i).getItemId().equals(itemId)) {
                GachaReward oldReward = rewards.get(i);
                rewards.set(i, oldReward.withWeight(newWeight));
                saveToFile();
                LOGGER.info("Poids gacha modifié: {} -> {}", itemId, newWeight);
                return true;
            }
        }
        return false;
    }

    /**
     * Modifie la rareté d'une récompense existante.
     * @param itemId ID de l'item
     * @param newRarity Nouvelle rareté
     * @return true si modifié avec succès
     */
    public boolean setRewardRarity(String itemId, GachaRarity newRarity) {
        for (int i = 0; i < rewards.size(); i++) {
            if (rewards.get(i).getItemId().equals(itemId)) {
                GachaReward old = rewards.get(i);
                rewards.set(i, new GachaReward(old.getItemId(), newRarity, old.getDisplayName(), old.getWeight()));
                saveToFile();
                LOGGER.info("Rareté gacha modifiée: {} -> {}", itemId, newRarity.getDisplayStars());
                return true;
            }
        }
        return false;
    }

    /**
     * Recherche une récompense par son ID d'item.
     */
    public Optional<GachaReward> findRewardByItemId(String itemId) {
        return rewards.stream()
                .filter(r -> r.getItemId().equals(itemId))
                .findFirst();
    }

    /**
     * Définit les probabilités de rareté.
     */
    public void setRarityRates(double fiveStar, double fourStar, double threeStar) {
        this.fiveStarBaseRate = fiveStar;
        this.fourStarBaseRate = fourStar;
        this.threeStarBaseRate = threeStar;
        saveToFile();
        LOGGER.info("Probabilités gacha mises à jour: 5★={}%, 4★={}%, 3★={}%", fiveStar, fourStar, threeStar);
    }

    // ==================== TIRAGE ====================

    /**
     * Effectue un tirage et retourne la récompense.
     * @return La récompense obtenue
     */
    public GachaReward pull() {
        if (rewards.isEmpty()) {
            LOGGER.warn("Aucune récompense gacha configurée!");
            return null;
        }

        // Déterminer la rareté
        GachaRarity rarity = determineRarity();

        // Filtrer les récompenses de cette rareté
        List<GachaReward> possibleRewards = rewards.stream()
                .filter(r -> r.getRarity() == rarity)
                .toList();

        if (possibleRewards.isEmpty()) {
            // Fallback: prendre n'importe quelle récompense
            possibleRewards = new ArrayList<>(rewards);
        }

        // Sélection pondérée parmi les récompenses de la rareté
        double totalWeight = possibleRewards.stream().mapToDouble(GachaReward::getWeight).sum();
        double roll = random.nextDouble() * totalWeight;

        double cumulative = 0;
        for (GachaReward reward : possibleRewards) {
            cumulative += reward.getWeight();
            if (roll <= cumulative) {
                return reward;
            }
        }

        // Fallback (ne devrait jamais arriver)
        return possibleRewards.get(possibleRewards.size() - 1);
    }

    /**
     * Détermine la rareté du tirage basé sur les probabilités.
     */
    private GachaRarity determineRarity() {
        double roll = random.nextDouble() * 100.0;

        if (roll < fiveStarBaseRate) {
            return GachaRarity.FIVE_STAR;
        } else if (roll < fiveStarBaseRate + fourStarBaseRate) {
            return GachaRarity.FOUR_STAR;
        } else {
            return GachaRarity.THREE_STAR;
        }
    }

    /**
     * Effectue un tirage et donne la récompense au joueur.
     * @param player Le joueur
     * @return Le résultat du tirage, ou null si l'inventaire est plein
     */
    public GachaPullResult pullAndGive(ServerPlayer player) {
        GachaReward reward = pull();
        if (reward == null) {
            return null;
        }
        
        ItemStack stack = reward.createStack();

        // Vérifier si l'inventaire peut recevoir l'item
        if (!player.getInventory().add(stack)) {
            // Inventaire plein - drop l'item au sol
            player.drop(stack, false);
        }

        return new GachaPullResult(reward, stack);
    }

    /**
     * Résultat d'un tirage Gacha.
     */
    public record GachaPullResult(GachaReward reward, ItemStack itemStack) {
        public GachaRarity getRarity() {
            return reward.getRarity();
        }

        public String getCharacterName() {
            return reward.getCharacterName();
        }
    }

    // ==================== GETTERS ====================

    /**
     * Retourne la liste de toutes les récompenses possibles.
     */
    public List<GachaReward> getAllRewards() {
        return new ArrayList<>(rewards);
    }

    /**
     * Retourne les récompenses filtrées par rareté.
     */
    public List<GachaReward> getRewardsByRarity(GachaRarity rarity) {
        return rewards.stream()
                .filter(r -> r.getRarity() == rarity)
                .collect(Collectors.toList());
    }

    /**
     * Retourne le nombre de récompenses.
     */
    public int getRewardCount() {
        return rewards.size();
    }

    /**
     * Retourne les probabilités de chaque rareté.
     */
    public static double getFiveStarRate() {
        return getInstance().fiveStarBaseRate;
    }

    public static double getFourStarRate() {
        return getInstance().fourStarBaseRate;
    }

    public static double getThreeStarRate() {
        return getInstance().threeStarBaseRate;
    }

    /**
     * Retourne le chemin du fichier de configuration.
     */
    public Path getConfigPath() {
        return configPath;
    }
}
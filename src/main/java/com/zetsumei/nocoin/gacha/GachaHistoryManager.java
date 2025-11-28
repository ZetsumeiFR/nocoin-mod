package com.zetsumei.nocoin.gacha;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;

/**
 * Gestionnaire de l'historique des tirages gacha par joueur.
 * Sauvegarde les derniers tirages pour chaque joueur.
 */
public class GachaHistoryManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_HISTORY_PER_PLAYER = 50;
    private static GachaHistoryManager INSTANCE;

    private final Map<UUID, List<GachaHistory>> playerHistories =
        new ConcurrentHashMap<>();
    private final Path savePath;
    private final Gson gson;

    private GachaHistoryManager() {
        this.savePath = FMLPaths.CONFIGDIR.get().resolve(
            "nocoin_gacha_history.json"
        );
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        load();
    }

    public static GachaHistoryManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new GachaHistoryManager();
        }
        return INSTANCE;
    }

    /**
     * Ajoute un tirage à l'historique d'un joueur.
     */
    public void addHistory(UUID playerId, GachaHistory history) {
        List<GachaHistory> histories = playerHistories.computeIfAbsent(
            playerId,
            k -> new ArrayList<>()
        );
        histories.add(0, history); // Ajoute au début (plus récent en premier)

        // Limite la taille de l'historique
        while (histories.size() > MAX_HISTORY_PER_PLAYER) {
            histories.remove(histories.size() - 1);
        }

        save();
    }

    /**
     * Ajoute plusieurs tirages à l'historique (pour le multi-pull).
     */
    public void addHistories(UUID playerId, List<GachaHistory> newHistories) {
        List<GachaHistory> histories = playerHistories.computeIfAbsent(
            playerId,
            k -> new ArrayList<>()
        );

        // Ajoute les nouveaux au début (plus récents en premier)
        for (int i = newHistories.size() - 1; i >= 0; i--) {
            histories.add(0, newHistories.get(i));
        }

        // Limite la taille de l'historique
        while (histories.size() > MAX_HISTORY_PER_PLAYER) {
            histories.remove(histories.size() - 1);
        }

        save();
    }

    /**
     * Récupère l'historique d'un joueur.
     */
    public List<GachaHistory> getHistory(UUID playerId) {
        return new ArrayList<>(
            playerHistories.getOrDefault(playerId, new ArrayList<>())
        );
    }

    /**
     * Charge l'historique depuis le fichier.
     */
    private void load() {
        if (!Files.exists(savePath)) {
            return;
        }

        try (
            Reader reader = Files.newBufferedReader(
                savePath,
                StandardCharsets.UTF_8
            )
        ) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();

            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                UUID playerId = UUID.fromString(entry.getKey());
                JsonArray historyArray = entry.getValue().getAsJsonArray();
                List<GachaHistory> histories = new ArrayList<>();

                for (JsonElement element : historyArray) {
                    JsonObject obj = element.getAsJsonObject();
                    histories.add(
                        new GachaHistory(
                            obj.get("itemId").getAsString(),
                            obj.get("displayName").getAsString(),
                            obj.get("stars").getAsInt(),
                            obj.get("timestamp").getAsLong()
                        )
                    );
                }

                playerHistories.put(playerId, histories);
            }

            LOGGER.info(
                "Historique gacha chargé pour {} joueurs",
                playerHistories.size()
            );
        } catch (Exception e) {
            LOGGER.error("Erreur lors du chargement de l'historique gacha", e);
        }
    }

    /**
     * Sauvegarde l'historique dans le fichier.
     */
    private void save() {
        try {
            JsonObject root = new JsonObject();

            for (Map.Entry<
                UUID,
                List<GachaHistory>
            > entry : playerHistories.entrySet()) {
                JsonArray historyArray = new JsonArray();

                for (GachaHistory history : entry.getValue()) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("itemId", history.getItemId());
                    obj.addProperty("displayName", history.getDisplayName());
                    obj.addProperty("stars", history.getStars());
                    obj.addProperty("timestamp", history.getTimestamp());
                    historyArray.add(obj);
                }

                root.add(entry.getKey().toString(), historyArray);
            }

            Files.createDirectories(savePath.getParent());
            try (
                Writer writer = Files.newBufferedWriter(
                    savePath,
                    StandardCharsets.UTF_8
                )
            ) {
                gson.toJson(root, writer);
            }
        } catch (IOException e) {
            LOGGER.error(
                "Erreur lors de la sauvegarde de l'historique gacha",
                e
            );
        }
    }
}

package com.zetsumei.nocoin.gacha;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente l'historique d'un tirage gacha.
 */
public class GachaHistory {

    private final String itemId;
    private final String displayName;
    private final int stars;
    private final long timestamp;

    public GachaHistory(
        String itemId,
        String displayName,
        int stars,
        long timestamp
    ) {
        this.itemId = itemId;
        this.displayName = displayName;
        this.stars = stars;
        this.timestamp = timestamp;
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getStars() {
        return stars;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Crée un GachaHistory à partir d'un GachaReward.
     */
    public static GachaHistory fromReward(GachaReward reward) {
        return new GachaHistory(
            reward.getItemId(),
            reward.getDisplayName(),
            reward.getRarity().getStars(),
            System.currentTimeMillis()
        );
    }
}

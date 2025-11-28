package com.zetsumei.nocoin.gacha;

import net.minecraft.ChatFormatting;

/**
 * Rareté des récompenses du Gacha.
 * Inspiré du système de Genshin Impact.
 */
public enum GachaRarity {
    THREE_STAR(3, "★★★", ChatFormatting.BLUE, 82.0),
    FOUR_STAR(4, "★★★★", ChatFormatting.LIGHT_PURPLE, 15.0),
    FIVE_STAR(5, "★★★★★", ChatFormatting.GOLD, 3.0);

    private final int stars;
    private final String displayStars;
    private final ChatFormatting color;
    private final double baseChance;

    GachaRarity(int stars, String displayStars, ChatFormatting color, double baseChance) {
        this.stars = stars;
        this.displayStars = displayStars;
        this.color = color;
        this.baseChance = baseChance;
    }

    public int getStars() {
        return stars;
    }

    public String getDisplayStars() {
        return displayStars;
    }

    public ChatFormatting getColor() {
        return color;
    }

    public double getBaseChance() {
        return baseChance;
    }
}

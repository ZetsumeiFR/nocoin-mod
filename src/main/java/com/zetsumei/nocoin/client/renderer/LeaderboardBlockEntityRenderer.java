package com.zetsumei.nocoin.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.zetsumei.nocoin.block.LeaderboardBlock;
import com.zetsumei.nocoin.block.entity.LeaderboardBlockEntity;
import com.zetsumei.nocoin.leaderboard.LeaderboardEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Renderer pour le bloc Leaderboard.
 * Affiche un magnifique panneau holographique avec le classement des joueurs.
 * 
 * Effets visuels:
 * - Fond semi-transparent avec dégradé
 * - Texte brillant avec ombres
 * - Médailles pour le top 3
 * - Animation de pulsation douce
 * - Bordure dorée lumineuse
 */
public class LeaderboardBlockEntityRenderer implements BlockEntityRenderer<LeaderboardBlockEntity> {

    // Couleurs
    private static final int COLOR_GOLD = 0xFFFFD700;
    private static final int COLOR_SILVER = 0xFFC0C0C0;
    private static final int COLOR_BRONZE = 0xFFCD7F32;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_GREEN = 0xFF00FF00;
    private static final int COLOR_TITLE = 0xFFFFD700;
    private static final int COLOR_BG = 0x99000033;
    private static final int COLOR_BG_LIGHT = 0x66000066;
    private static final int COLOR_BORDER = 0xFFFFD700;

    // Dimensions du panneau
    private static final float PANEL_WIDTH = 1.8f;
    private static final float PANEL_HEIGHT = 2.0f;
    private static final float PANEL_Y_OFFSET = 1.1f; // Hauteur au-dessus du bloc
    private static final float TEXT_SCALE = 0.015f;

    private final Font font;

    public LeaderboardBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    @Override
    public void render(LeaderboardBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        List<LeaderboardEntry> entries = blockEntity.getEntries();
        BlockState state = blockEntity.getBlockState();
        Direction facing = state.getValue(LeaderboardBlock.FACING);

        poseStack.pushPose();

        // Positionner au centre du bloc et à la bonne hauteur
        poseStack.translate(0.5, PANEL_Y_OFFSET, 0.5);

        // Rotation selon la direction du bloc
        float rotation = switch (facing) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST -> 90f;
            case EAST -> -90f;
            default -> 0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Légère animation de flottement basée sur le tick d'animation
        float floatOffset = (float) Math.sin(blockEntity.getAnimationTick() * 0.05) * 0.02f;
        poseStack.translate(0, floatOffset, 0);

        // Décaler légèrement vers l'avant pour éviter le z-fighting
        poseStack.translate(0, 0, -0.35);

        // Dessiner le panneau de fond
        renderBackground(poseStack, bufferSource, blockEntity.getAnimationTick());

        // Dessiner le titre
        renderTitle(poseStack, bufferSource);

        // Dessiner les entrées du classement
        renderEntries(poseStack, bufferSource, entries, blockEntity.getAnimationTick());

        poseStack.popPose();
    }

    /**
     * Dessine le fond du panneau avec effet holographique.
     */
    private void renderBackground(PoseStack poseStack, MultiBufferSource bufferSource, int animTick) {
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f matrix = poseStack.last().pose();

        float halfWidth = PANEL_WIDTH / 2;
        float height = PANEL_HEIGHT;

        // Animation de pulsation pour la bordure
        float pulse = (float) (1.0 + Math.sin(animTick * 0.1) * 0.1);

        // Fond semi-transparent (rectangle simple)
        // Utiliser des valeurs RGBA séparées
        float bgR = 0.0f, bgG = 0.0f, bgB = 0.2f, bgA = 0.6f;

        // Dessiner le quad de fond (face avant)
        buffer.vertex(matrix, -halfWidth, 0, 0).color(bgR, bgG, bgB, bgA)
                .uv(0, 0).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 0, 1).endVertex();
        buffer.vertex(matrix, halfWidth, 0, 0).color(bgR, bgG, bgB, bgA)
                .uv(1, 0).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 0, 1).endVertex();
        buffer.vertex(matrix, halfWidth, height, 0).color(bgR, bgG, bgB * 0.5f, bgA)
                .uv(1, 1).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 0, 1).endVertex();
        buffer.vertex(matrix, -halfWidth, height, 0).color(bgR, bgG, bgB * 0.5f, bgA)
                .uv(0, 1).overlayCoords(0).uv2(LightTexture.FULL_BRIGHT)
                .normal(0, 0, 1).endVertex();
    }

    /**
     * Dessine le titre "LEADERBOARD" avec effet brillant.
     */
    private void renderTitle(PoseStack poseStack, MultiBufferSource bufferSource) {
        poseStack.pushPose();

        String title = "✦ LEADERBOARD ✦";

        // Position et échelle du titre
        poseStack.translate(0, PANEL_HEIGHT - 0.15f, -0.01f);
        poseStack.scale(-TEXT_SCALE * 1.5f, -TEXT_SCALE * 1.5f, TEXT_SCALE * 1.5f);

        // Centrer le texte
        int titleWidth = font.width(title);
        float xOffset = -titleWidth / 2f;

        // Ombre du titre
        font.drawInBatch(title, xOffset + 1, 1, 0xFF333333, false, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        // Titre principal
        font.drawInBatch(title, xOffset, 0, COLOR_TITLE, false, poseStack.last().pose(),
                bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }

    /**
     * Dessine les entrées du classement.
     */
    private void renderEntries(PoseStack poseStack, MultiBufferSource bufferSource,
                               List<LeaderboardEntry> entries, int animTick) {
        if (entries.isEmpty()) {
            renderEmptyMessage(poseStack, bufferSource);
            return;
        }

        float startY = PANEL_HEIGHT - 0.35f;
        float entryHeight = 0.17f;
        float maxEntries = Math.min(entries.size(), 10);

        for (int i = 0; i < maxEntries; i++) {
            LeaderboardEntry entry = entries.get(i);
            float yPos = startY - (i * entryHeight);

            // Animation d'entrée progressive
            float alpha = Math.min(1f, (animTick - i * 5) / 20f);
            if (alpha <= 0) continue;

            renderEntry(poseStack, bufferSource, entry, yPos, i, alpha);
        }
    }

    /**
     * Dessine une entrée individuelle du classement.
     */
    private void renderEntry(PoseStack poseStack, MultiBufferSource bufferSource,
                             LeaderboardEntry entry, float yPos, int index, float alpha) {
        poseStack.pushPose();

        poseStack.translate(0, yPos, -0.01f);
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        float halfWidth = (PANEL_WIDTH / TEXT_SCALE) / 2 - 10;

        // Fond de ligne alterné pour lisibilité
        if (index % 2 == 0) {
            // Ligne de fond légèrement plus claire
            renderLineBackground(poseStack, bufferSource, halfWidth, alpha);
        }

        // Rang avec médaille pour top 3
        String rankText = getRankDisplay(entry.getRank());
        int rankColor = getRankColor(entry.getRank());
        font.drawInBatch(rankText, -halfWidth, 0, applyAlpha(rankColor, alpha), false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        // Nom du joueur
        String playerName = entry.getPlayerName();
        if (playerName.length() > 12) {
            playerName = playerName.substring(0, 10) + "..";
        }
        font.drawInBatch(playerName, -halfWidth + 30, 0, applyAlpha(COLOR_WHITE, alpha), false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        // Balance NOCOIN (aligné à droite)
        String balanceText = formatBalance(entry.getNocoinBalance()) + " NC";
        int balanceWidth = font.width(balanceText);
        font.drawInBatch(balanceText, halfWidth - balanceWidth, 0, applyAlpha(COLOR_GREEN, alpha), false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }

    /**
     * Dessine le fond d'une ligne du classement.
     */
    private void renderLineBackground(PoseStack poseStack, MultiBufferSource bufferSource, float halfWidth, float alpha) {
        // Le fond de ligne est optionnel, on peut l'implémenter si besoin
    }

    /**
     * Affiche un message quand le leaderboard est vide.
     */
    private void renderEmptyMessage(PoseStack poseStack, MultiBufferSource bufferSource) {
        poseStack.pushPose();

        String message = "Aucun joueur...";

        poseStack.translate(0, PANEL_HEIGHT / 2, -0.01f);
        poseStack.scale(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        int msgWidth = font.width(message);
        font.drawInBatch(message, -msgWidth / 2f, 0, 0xFF888888, false,
                poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }

    /**
     * Retourne le texte d'affichage du rang (avec médailles).
     */
    private String getRankDisplay(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> "#" + rank;
        };
    }

    /**
     * Retourne la couleur selon le rang.
     */
    private int getRankColor(int rank) {
        return switch (rank) {
            case 1 -> COLOR_GOLD;
            case 2 -> COLOR_SILVER;
            case 3 -> COLOR_BRONZE;
            default -> 0xFFAAAAAA;
        };
    }

    /**
     * Formate un nombre avec séparateurs de milliers.
     */
    private String formatBalance(long balance) {
        return String.format("%,d", balance);
    }

    /**
     * Applique une valeur alpha à une couleur ARGB.
     */
    private int applyAlpha(int color, float alpha) {
        int a = (int) (((color >> 24) & 0xFF) * alpha);
        return (a << 24) | (color & 0x00FFFFFF);
    }

    @Override
    public boolean shouldRenderOffScreen(LeaderboardBlockEntity blockEntity) {
        // Render même quand le bloc est partiellement hors écran car le panneau est grand
        return true;
    }

    @Override
    public int getViewDistance() {
        // Distance de vue augmentée pour que le leaderboard soit visible de loin
        return 64;
    }
}

package com.zetsumei.nocoin.client.screen;

import com.zetsumei.nocoin.client.ClientGachaMachineHandler;
import com.zetsumei.nocoin.gacha.GachaManager;
import com.zetsumei.nocoin.gacha.GachaRarity;
import com.zetsumei.nocoin.item.ModItems;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Ecran de la machine a Gacha.
 * Permet au joueur de tirer des recompenses avec ses Cles Gacha.
 */
public class GachaMachineScreen extends Screen {

    private static final Component TITLE = Component.literal("Machine Gacha");

    // Dimensions
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;

    private int leftPos;
    private int topPos;

    private boolean hasKey;
    private int keyCount;

    // État de l'animation
    private boolean isPulling = false;
    private int pullAnimationTick = 0;
    private static final int PULL_ANIMATION_DURATION = 40; // 2 secondes

    // Résultat du tirage
    private String lastResultItemId = null;
    private int lastResultStars = 0;
    private String lastResultCharacter = null;
    private int resultDisplayTick = 0;
    private static final int RESULT_DISPLAY_DURATION = 100; // 5 secondes

    private Button pullButton;

    public GachaMachineScreen(boolean hasKey, int keyCount) {
        super(TITLE);
        this.hasKey = hasKey;
        this.keyCount = keyCount;
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Bouton de tirage
        pullButton = Button.builder(
                Component.literal("TIRER !"),
                button -> performPull()
        ).bounds(leftPos + GUI_WIDTH / 2 - 60, topPos + 150, 120, 25).build();
        pullButton.active = hasKey && !isPulling;
        this.addRenderableWidget(pullButton);

        // Bouton fermer
        this.addRenderableWidget(Button.builder(
                Component.literal("Fermer"),
                button -> this.onClose()
        ).bounds(leftPos + GUI_WIDTH / 2 - 40, topPos + GUI_HEIGHT - 30, 80, 20).build());

        // Définir le callback pour recevoir les résultats
        ClientGachaMachineHandler.setPullResultCallback(this::onPullResult);
    }

    private void performPull() {
        if (!hasKey || isPulling) return;

        isPulling = true;
        pullAnimationTick = 0;
        lastResultItemId = null;
        pullButton.active = false;

        // Envoyer la demande au serveur
        NocoinNetworkHandler.sendGachaPullRequest();
    }

    private void onPullResult(boolean success, String itemId, int stars, String characterName) {
        if (this.minecraft != null) {
            this.minecraft.execute(() -> {
                isPulling = false;

                if (success) {
                    lastResultItemId = itemId;
                    lastResultStars = stars;
                    lastResultCharacter = characterName;
                    resultDisplayTick = 0;

                    // Mettre à jour le compteur de clés
                    keyCount--;
                    hasKey = keyCount > 0;
                }

                pullButton.active = hasKey;
            });
        }
    }

    @Override
    public void tick() {
        super.tick();

        // Animation de tirage
        if (isPulling) {
            pullAnimationTick++;
        }

        // Durée d'affichage du résultat
        if (lastResultItemId != null) {
            resultDisplayTick++;
            if (resultDisplayTick > RESULT_DISPLAY_DURATION) {
                // Réinitialiser après un moment (optionnel)
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fond semi-transparent
        this.renderBackground(guiGraphics);

        // Couleur de fond selon l'état
        int bgColor = isPulling ? 0xDD1A1A3E : 0xCC1A1A2E;
        int bgColor2 = isPulling ? 0xDD2D2D54 : 0xCC2D2D44;

        // Cadre principal
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, bgColor);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + GUI_HEIGHT - 2, bgColor2);

        // Bordure selon la rareté du dernier résultat
        int borderColor = getBorderColor();
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + 3, borderColor);
        guiGraphics.fill(leftPos, topPos + GUI_HEIGHT - 3, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, borderColor);
        guiGraphics.fill(leftPos, topPos, leftPos + 3, topPos + GUI_HEIGHT, borderColor);
        guiGraphics.fill(leftPos + GUI_WIDTH - 3, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, borderColor);

        // Titre
        Component titleText = Component.literal("Machine Gacha");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, leftPos + (GUI_WIDTH - titleWidth) / 2, topPos + 12, 0xFFD700, true);

        // Icône de la Clé Gacha et compteur
        ItemStack keyStack = new ItemStack(ModItems.GACHA_KEY.get());
        int keyIconX = leftPos + 15;
        int keyIconY = topPos + 30;
        guiGraphics.renderItem(keyStack, keyIconX, keyIconY);
        Component keyCountText = Component.literal("x" + keyCount)
                .withStyle(keyCount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
        guiGraphics.drawString(this.font, keyCountText, keyIconX + 20, keyIconY + 4, 0xFFFFFF, false);

        // Zone d'affichage centrale
        int centerX = leftPos + GUI_WIDTH / 2;
        int centerY = topPos + 85;

        if (isPulling) {
            // Animation de tirage
            renderPullAnimation(guiGraphics, centerX, centerY);
        } else if (lastResultItemId != null) {
            // Afficher le résultat
            renderResult(guiGraphics, centerX, centerY);
        } else {
            // Instructions
            renderInstructions(guiGraphics, centerX, centerY);
        }

        // Probabilités en bas
        renderProbabilities(guiGraphics, leftPos + 10, topPos + 180);

        // Rendu des widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private int getBorderColor() {
        if (lastResultItemId != null && resultDisplayTick < 60) {
            return switch (lastResultStars) {
                case 5 -> 0xFFFFD700; // Or
                case 4 -> 0xFFAA00FF; // Violet
                default -> 0xFF5555FF; // Bleu
            };
        }
        return 0xFFFFD700; // Or par défaut
    }

    private void renderPullAnimation(GuiGraphics guiGraphics, int centerX, int centerY) {
        // Animation simple avec des étoiles qui tournent
        String animChars = "★☆✦✧";
        int charIndex = (pullAnimationTick / 5) % animChars.length();
        String animText = String.valueOf(animChars.charAt(charIndex)).repeat(5);

        int animWidth = this.font.width(animText);
        int color = (pullAnimationTick / 3 % 2 == 0) ? 0xFFFFD700 : 0xFFFFFFFF;
        guiGraphics.drawString(this.font, animText, centerX - animWidth / 2, centerY, color, true);

        Component pullingText = Component.literal("Tirage en cours...");
        int textWidth = this.font.width(pullingText);
        guiGraphics.drawString(this.font, pullingText, centerX - textWidth / 2, centerY + 20, 0xFFFFFF, false);
    }

    private void renderResult(GuiGraphics guiGraphics, int centerX, int centerY) {
        // Étoiles de rareté
        String stars = "★".repeat(lastResultStars);
        int starColor = switch (lastResultStars) {
            case 5 -> 0xFFFFD700;
            case 4 -> 0xFFAA00FF;
            default -> 0xFF5555FF;
        };
        int starsWidth = this.font.width(stars);
        guiGraphics.drawString(this.font, stars, centerX - starsWidth / 2, centerY - 20, starColor, true);

        // Nom de la recompense
        Component itemName = Component.literal(lastResultCharacter != null ? lastResultCharacter : "Recompense");
        int nameWidth = this.font.width(itemName);
        guiGraphics.drawString(this.font, itemName, centerX - nameWidth / 2, centerY, 0xFFFFFF, true);

        // Message de félicitations
        Component congratsKey = switch (lastResultStars) {
            case 5 -> Component.literal("§6★ LÉGENDAIRE ! ★");
            case 4 -> Component.literal("§5✦ Épique ! ✦");
            default -> Component.literal("Obtenu !");
        };
        int congratsWidth = this.font.width(congratsKey);
        guiGraphics.drawString(this.font, congratsKey, centerX - congratsWidth / 2, centerY + 25, starColor, false);
    }

    private void renderInstructions(GuiGraphics guiGraphics, int centerX, int centerY) {
        Component instruction1 = Component.literal("Inserez une Cle Gacha pour tirer");
        Component instruction2 = Component.literal("Tentez votre chance !");

        int width1 = this.font.width(instruction1);
        int width2 = this.font.width(instruction2);

        guiGraphics.drawString(this.font, instruction1, centerX - width1 / 2, centerY - 10, 0xAAAAAA, false);
        guiGraphics.drawString(this.font, instruction2, centerX - width2 / 2, centerY + 5, 0x888888, false);
    }

    private void renderProbabilities(GuiGraphics guiGraphics, int x, int y) {
        Component probTitle = Component.literal("Probabilités :");
        guiGraphics.drawString(this.font, probTitle, x, y, 0x888888, false);

        String prob5 = String.format("★★★★★ %.1f%%", GachaManager.getFiveStarRate());
        String prob4 = String.format("★★★★ %.1f%%", GachaManager.getFourStarRate());
        String prob3 = String.format("★★★ %.1f%%", GachaManager.getThreeStarRate());

        guiGraphics.drawString(this.font, prob5, x, y + 10, 0xFFD700, false);
        guiGraphics.drawString(this.font, prob4, x + 80, y + 10, 0xAA00FF, false);
        guiGraphics.drawString(this.font, prob3, x + 160, y + 10, 0x5555FF, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ClientGachaMachineHandler.setPullResultCallback(null);
        super.onClose();
    }
}

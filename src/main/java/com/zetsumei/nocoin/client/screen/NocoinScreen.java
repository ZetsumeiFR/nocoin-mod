package com.zetsumei.nocoin.client.screen;

import com.zetsumei.nocoin.client.ClientLeaderboardData;
import com.zetsumei.nocoin.client.ClientNocoinData;
import com.zetsumei.nocoin.leaderboard.LeaderboardManager;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Écran d'affichage du solde NOCOIN du joueur.
 */
public class NocoinScreen extends Screen {

    private static final Component TITLE = Component.literal("Portefeuille NOCOIN");

    // Dimensions de l'interface
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 170;

    private int leftPos;
    private int topPos;

    public NocoinScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();

        // Centrer l'interface
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Bouton boutique
        this.addRenderableWidget(Button.builder(
                Component.literal("Boutique"),
                button -> openShop()
        ).bounds(this.leftPos + GUI_WIDTH / 2 - 50, this.topPos + GUI_HEIGHT - 80, 100, 20).build());

        // Bouton classement
        this.addRenderableWidget(Button.builder(
                Component.literal("Classement"),
                button -> openLeaderboard()
        ).bounds(this.leftPos + GUI_WIDTH / 2 - 50, this.topPos + GUI_HEIGHT - 55, 100, 20).build());

        // Bouton fermer
        this.addRenderableWidget(Button.builder(
                Component.literal("Fermer"),
                button -> this.onClose()
        ).bounds(this.leftPos + GUI_WIDTH / 2 - 40, this.topPos + GUI_HEIGHT - 30, 80, 20).build());
    }

    private void openShop() {
        // Demander les données de la boutique au serveur
        NocoinNetworkHandler.requestShopItemsFromServer();
        // Ouvrir l'écran de la boutique
        Minecraft.getInstance().setScreen(new ShopScreen());
    }

    private void openLeaderboard() {
        // Réinitialiser les données du classement
        ClientLeaderboardData.clear();
        // Demander les données du classement au serveur
        NocoinNetworkHandler.requestLeaderboardFromServer(LeaderboardManager.LeaderboardType.NOCOIN);
        // Ouvrir l'écran du classement
        Minecraft.getInstance().setScreen(new LeaderboardScreen());
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fond semi-transparent
        this.renderBackground(guiGraphics);
        
        // Cadre de l'interface (fond coloré simple)
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xCC1A1A2E);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + GUI_HEIGHT - 2, 0xCC2D2D44);
        
        // Bordure dorée
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + 2, 0xFFFFD700);
        guiGraphics.fill(leftPos, topPos + GUI_HEIGHT - 2, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFFFFD700);
        guiGraphics.fill(leftPos, topPos, leftPos + 2, topPos + GUI_HEIGHT, 0xFFFFD700);
        guiGraphics.fill(leftPos + GUI_WIDTH - 2, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFFFFD700);
        
        // Titre centré
        Component titleText = Component.literal("Portefeuille NOCOIN");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, leftPos + (GUI_WIDTH - titleWidth) / 2, topPos + 10, 0xFFD700, true);

        // Icône de monnaie (caractère symbolique)
        Component coinIcon = Component.literal("\u2B22"); // Hexagone
        guiGraphics.drawString(this.font, coinIcon, leftPos + 20, topPos + 40, 0xFFD700, true);

        // Solde du joueur
        long balance = ClientNocoinData.getBalance();
        Component balanceText = Component.literal("Solde : " + formatBalance(balance) + " NOCOIN");
        guiGraphics.drawString(this.font, balanceText, leftPos + 35, topPos + 40, 0xFFFFFF, false);

        // Information supplémentaire
        Component infoText = Component.literal("Tuez des mobs pour en gagner !");
        int infoWidth = this.font.width(infoText);
        guiGraphics.drawString(this.font, infoText, leftPos + (GUI_WIDTH - infoWidth) / 2, topPos + 60, 0xAAAAAA, false);
        
        // Rendu des widgets (boutons)
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    /**
     * Formate le solde avec des séparateurs de milliers.
     */
    private String formatBalance(long balance) {
        return String.format("%,d", balance);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Fermer avec Échap ou la touche du menu
        if (this.shouldCloseOnEsc() && keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}

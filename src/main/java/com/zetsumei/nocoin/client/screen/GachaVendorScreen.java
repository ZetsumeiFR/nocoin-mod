package com.zetsumei.nocoin.client.screen;

import com.zetsumei.nocoin.client.ClientNocoinData;
import com.zetsumei.nocoin.client.ClientVendorHandler;
import com.zetsumei.nocoin.item.ModItems;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Écran d'achat auprès du vendeur Gacha.
 * Permet d'acheter des Clés Gacha contre des NOCOIN.
 */
public class GachaVendorScreen extends Screen {

    private static final Component TITLE = Component.literal("Vendeur Gacha");

    // Dimensions
    private static final int GUI_WIDTH = 240;
    private static final int GUI_HEIGHT = 180;

    private int leftPos;
    private int topPos;
    private final long keyPrice;
    private int quantity = 1;

    private Button buyButton;
    private Button decreaseButton;
    private Button increaseButton;

    public GachaVendorScreen(long keyPrice) {
        super(TITLE);
        this.keyPrice = keyPrice;
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Demander le solde actuel
        NocoinNetworkHandler.requestBalanceFromServer();

        // Bouton diminuer quantité
        decreaseButton = Button.builder(
                Component.literal("-"),
                button -> {
                    if (quantity > 1) {
                        quantity--;
                        updateButtons();
                    }
                }
        ).bounds(leftPos + 60, topPos + 100, 30, 20).build();
        this.addRenderableWidget(decreaseButton);

        // Bouton augmenter quantité
        increaseButton = Button.builder(
                Component.literal("+"),
                button -> {
                    if (quantity < 64) {
                        quantity++;
                        updateButtons();
                    }
                }
        ).bounds(leftPos + 150, topPos + 100, 30, 20).build();
        this.addRenderableWidget(increaseButton);

        // Bouton acheter
        buyButton = Button.builder(
                Component.literal("Acheter"),
                button -> purchaseKeys()
        ).bounds(leftPos + GUI_WIDTH / 2 - 50, topPos + 130, 100, 20).build();
        this.addRenderableWidget(buyButton);

        // Bouton fermer
        this.addRenderableWidget(Button.builder(
                Component.literal("Fermer"),
                button -> this.onClose()
        ).bounds(leftPos + GUI_WIDTH / 2 - 40, topPos + GUI_HEIGHT - 30, 80, 20).build());

        updateButtons();
    }

    private void updateButtons() {
        long totalPrice = keyPrice * quantity;
        boolean canAfford = ClientNocoinData.getBalance() >= totalPrice;

        buyButton.active = canAfford;
        decreaseButton.active = quantity > 1;
        increaseButton.active = quantity < 64 && ClientNocoinData.getBalance() >= keyPrice * (quantity + 1);
    }

    private void purchaseKeys() {
        // Définir callback pour rafraîchir l'UI
        ClientVendorHandler.setPurchaseCallback((success, qty) -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    if (success) {
                        // Afficher message de succès (optionnel, le serveur envoie déjà un message)
                    }
                    updateButtons();
                });
            }
        });

        // Envoyer demande d'achat
        NocoinNetworkHandler.sendBuyGachaKeyRequest(quantity);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fond semi-transparent
        this.renderBackground(guiGraphics);

        // Cadre principal
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xCC1A1A2E);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + GUI_HEIGHT - 2, 0xCC2D2D44);

        // Bordure dorée
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + 2, 0xFFFFD700);
        guiGraphics.fill(leftPos, topPos + GUI_HEIGHT - 2, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFFFFD700);
        guiGraphics.fill(leftPos, topPos, leftPos + 2, topPos + GUI_HEIGHT, 0xFFFFD700);
        guiGraphics.fill(leftPos + GUI_WIDTH - 2, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFFFFD700);

        // Titre
        Component titleText = Component.literal("Vendeur Gacha");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, leftPos + (GUI_WIDTH - titleWidth) / 2, topPos + 10, 0xFFD700, true);

        // Solde du joueur
        long balance = ClientNocoinData.getBalance();
        Component balanceText = Component.literal("Solde : " + formatBalance(balance) + " NOCOIN");
        guiGraphics.drawString(this.font, balanceText, leftPos + 10, topPos + 28, 0xFFFFFF, false);

        // Icône de la Clé Gacha
        ItemStack keyStack = new ItemStack(ModItems.GACHA_KEY.get());
        int iconX = leftPos + GUI_WIDTH / 2 - 8;
        int iconY = topPos + 50;
        guiGraphics.renderItem(keyStack, iconX, iconY);

        // Nom de l'item
        Component itemName = Component.literal("Clé Gacha");
        int nameWidth = this.font.width(itemName);
        guiGraphics.drawString(this.font, itemName, leftPos + (GUI_WIDTH - nameWidth) / 2, topPos + 72, 0xFFFFFF, false);

        // Prix unitaire
        Component unitPriceText = Component.literal("Prix unitaire : " + formatBalance(keyPrice) + " NOCOIN");
        int unitPriceWidth = this.font.width(unitPriceText);
        guiGraphics.drawString(this.font, unitPriceText, leftPos + (GUI_WIDTH - unitPriceWidth) / 2, topPos + 85, 0xAAAAAA, false);

        // Quantité sélectionnée
        Component qtyText = Component.literal(String.valueOf(quantity));
        int qtyWidth = this.font.width(qtyText);
        guiGraphics.drawString(this.font, qtyText, leftPos + (GUI_WIDTH - qtyWidth) / 2, topPos + 105, 0xFFFFFF, false);

        // Prix total
        long totalPrice = keyPrice * quantity;
        boolean canAfford = balance >= totalPrice;
        int priceColor = canAfford ? 0x00FF00 : 0xFF5555;
        Component totalPriceText = Component.literal("Total : " + formatBalance(totalPrice) + " NOCOIN");
        int totalPriceWidth = this.font.width(totalPriceText);
        guiGraphics.drawString(this.font, totalPriceText, leftPos + (GUI_WIDTH - totalPriceWidth) / 2, topPos + 118, priceColor, false);

        // Rendu des widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Tooltip sur l'icône de la clé
        if (mouseX >= iconX && mouseX <= iconX + 16 && mouseY >= iconY && mouseY <= iconY + 16) {
            guiGraphics.renderTooltip(this.font, keyStack, mouseX, mouseY);
        }
    }

    private String formatBalance(long balance) {
        return String.format("%,d", balance);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        // Mettre à jour l'état des boutons (au cas où le solde change)
        updateButtons();
    }
}

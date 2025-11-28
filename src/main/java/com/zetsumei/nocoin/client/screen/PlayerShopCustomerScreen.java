package com.zetsumei.nocoin.client.screen;

import com.zetsumei.nocoin.client.ClientNocoinData;
import com.zetsumei.nocoin.client.ClientPlayerShopHandler;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import com.zetsumei.nocoin.shop.player.ShopOffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Écran d'achat/vente pour les visiteurs du magasin.
 * Affiche les offres disponibles et permet de réaliser des transactions.
 */
public class PlayerShopCustomerScreen extends Screen {

    private static final Component TITLE = Component.literal("Magasin Joueur");

    // Dimensions
    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 260;
    private static final int OFFER_HEIGHT = 28;
    private static final int MAX_VISIBLE_OFFERS = 6;

    private final BlockPos shopPos;
    private final String shopName;
    private final String ownerName;
    private final List<ShopOffer> sellOffers;
    private final List<ShopOffer> buyOffers;

    private int leftPos;
    private int topPos;

    // Onglets
    private enum Tab { BUY, SELL }
    private Tab currentTab = Tab.BUY;

    private int scrollOffset = 0;
    private String statusMessage = "";
    private int statusMessageTicks = 0;
    private boolean statusSuccess = false;

    public PlayerShopCustomerScreen(BlockPos shopPos, String shopName, String ownerName,
                                     List<ShopOffer> sellOffers, List<ShopOffer> buyOffers) {
        super(TITLE);
        this.shopPos = shopPos;
        this.shopName = shopName;
        this.ownerName = ownerName;
        this.sellOffers = new ArrayList<>(sellOffers);
        this.buyOffers = new ArrayList<>(buyOffers);
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Onglets
        this.addRenderableWidget(Button.builder(
                Component.literal("ACHETER")
                        .withStyle(currentTab == Tab.BUY ? ChatFormatting.GOLD : ChatFormatting.GRAY),
                button -> {
                    currentTab = Tab.BUY;
                    scrollOffset = 0;
                    rebuildWidgets();
                }
        ).bounds(leftPos + 10, topPos + 50, 80, 20).build());

        this.addRenderableWidget(Button.builder(
                Component.literal("VENDRE")
                        .withStyle(currentTab == Tab.SELL ? ChatFormatting.GOLD : ChatFormatting.GRAY),
                button -> {
                    currentTab = Tab.SELL;
                    scrollOffset = 0;
                    rebuildWidgets();
                }
        ).bounds(leftPos + 95, topPos + 50, 80, 20).build());

        // Bouton fermer
        this.addRenderableWidget(Button.builder(
                Component.literal("Fermer"),
                button -> this.onClose()
        ).bounds(leftPos + GUI_WIDTH / 2 - 40, topPos + GUI_HEIGHT - 28, 80, 20).build());

        // Boutons de scroll
        List<ShopOffer> currentOffers = getCurrentOffers();
        if (currentOffers.size() > MAX_VISIBLE_OFFERS) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("▲"),
                    button -> {
                        if (scrollOffset > 0) {
                            scrollOffset--;
                            rebuildWidgets();
                        }
                    }
            ).bounds(leftPos + GUI_WIDTH - 30, topPos + 80, 20, 15).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("▼"),
                    button -> {
                        if (scrollOffset < currentOffers.size() - MAX_VISIBLE_OFFERS) {
                            scrollOffset++;
                            rebuildWidgets();
                        }
                    }
            ).bounds(leftPos + GUI_WIDTH - 30, topPos + GUI_HEIGHT - 65, 20, 15).build());
        }

        initOfferButtons();
    }

    private List<ShopOffer> getCurrentOffers() {
        return currentTab == Tab.BUY ? sellOffers : buyOffers;
    }

    private void initOfferButtons() {
        List<ShopOffer> currentOffers = getCurrentOffers();
        int startY = topPos + 80;

        for (int i = 0; i < MAX_VISIBLE_OFFERS && i + scrollOffset < currentOffers.size(); i++) {
            final ShopOffer offer = currentOffers.get(i + scrollOffset);
            int y = startY + (i * OFFER_HEIGHT);

            boolean canExecute = canExecuteTransaction(offer);

            // Bouton d'action
            Component buttonText = currentTab == Tab.BUY ?
                    Component.literal("Acheter") :
                    Component.literal("Vendre");

            this.addRenderableWidget(Button.builder(
                    buttonText,
                    button -> executeTransaction(offer)
            ).bounds(leftPos + GUI_WIDTH - 90, y + 4, 55, 20).build());
        }
    }

    private boolean canExecuteTransaction(ShopOffer offer) {
        long balance = ClientNocoinData.getBalance();

        if (currentTab == Tab.BUY) {
            // Acheter au magasin : besoin d'assez de NOCOIN
            return balance >= offer.getTotalPrice() && offer.hasStock();
        } else {
            // Vendre au magasin : vérifier qu'on a les items (simplifié côté client)
            return offer.hasStock();
        }
    }

    private void executeTransaction(ShopOffer offer) {
        ClientPlayerShopHandler.setTransactionCallback((success, message) -> {
            if (this.minecraft != null) {
                this.minecraft.execute(() -> {
                    statusMessage = message;
                    statusSuccess = success;
                    statusMessageTicks = 60; // 3 secondes
                    rebuildWidgets();
                });
            }
        });

        if (currentTab == Tab.BUY) {
            NocoinNetworkHandler.sendPlayerShopTransaction(shopPos, offer.getOfferId(), true);
        } else {
            NocoinNetworkHandler.sendPlayerShopTransaction(shopPos, offer.getOfferId(), false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // Cadre principal
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xDD1A1A2E);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + GUI_HEIGHT - 2, 0xDD2D2D44);

        // Bordure
        int borderColor = currentTab == Tab.BUY ? 0xFF00AA00 : 0xFFFFAA00;
        renderBorder(guiGraphics, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, borderColor);

        // Nom du magasin
        Component shopNameText = Component.literal(shopName).withStyle(ChatFormatting.GOLD);
        int shopNameWidth = this.font.width(shopNameText);
        guiGraphics.drawString(this.font, shopNameText, leftPos + (GUI_WIDTH - shopNameWidth) / 2, topPos + 8, 0xFFFFFF, true);

        // Propriétaire
        Component ownerText = Component.literal("Propriétaire : " + ownerName)
                .withStyle(ChatFormatting.GRAY);
        int ownerWidth = this.font.width(ownerText);
        guiGraphics.drawString(this.font, ownerText, leftPos + (GUI_WIDTH - ownerWidth) / 2, topPos + 22, 0xFFFFFF, false);

        // Solde du joueur
        long balance = ClientNocoinData.getBalance();
        Component balanceText = Component.literal("Solde : " + formatBalance(balance) + " NOCOIN");
        guiGraphics.drawString(this.font, balanceText, leftPos + 10, topPos + 38, 0xFFFFFF, false);

        // Ligne de séparation
        guiGraphics.fill(leftPos + 10, topPos + 72, leftPos + GUI_WIDTH - 10, topPos + 73, 0xFF555555);

        // Description de l'onglet
        Component tabDescription = currentTab == Tab.BUY ?
                Component.literal("Articles que vous pouvez acheter") :
                Component.literal("Articles que le magasin souhaite acheter");
        guiGraphics.drawString(this.font, tabDescription, leftPos + 180, topPos + 55, 0xAAAAAA, false);

        // Rendu des offres
        renderOffers(guiGraphics, mouseX, mouseY);

        // Message de statut
        if (statusMessageTicks > 0) {
            int msgColor = statusSuccess ? 0x00FF00 : 0xFF5555;
            int msgWidth = this.font.width(statusMessage);
            guiGraphics.drawString(this.font, statusMessage,
                    leftPos + (GUI_WIDTH - msgWidth) / 2, topPos + GUI_HEIGHT - 50, msgColor, true);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderOffers(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<ShopOffer> currentOffers = getCurrentOffers();
        int startY = topPos + 80;

        if (currentOffers.isEmpty()) {
            Component emptyText = currentTab == Tab.BUY ?
                    Component.literal("Aucun article en vente") :
                    Component.literal("Aucune demande d'achat");
            int textWidth = this.font.width(emptyText);
            guiGraphics.drawString(this.font, emptyText,
                    leftPos + (GUI_WIDTH - textWidth) / 2, startY + 50, 0x888888, false);
            return;
        }

        for (int i = 0; i < MAX_VISIBLE_OFFERS && i + scrollOffset < currentOffers.size(); i++) {
            ShopOffer offer = currentOffers.get(i + scrollOffset);
            int y = startY + (i * OFFER_HEIGHT);

            // Fond de l'offre
            boolean canExecute = canExecuteTransaction(offer);
            int bgColor = canExecute ? 0x44008800 : 0x44444444;
            guiGraphics.fill(leftPos + 5, y, leftPos + GUI_WIDTH - 95, y + OFFER_HEIGHT - 2, bgColor);

            // Icône de l'item
            ItemStack itemStack = offer.createItemStack();
            if (!itemStack.isEmpty()) {
                guiGraphics.renderItem(itemStack, leftPos + 10, y + 5);

                // Tooltip sur l'item
                if (mouseX >= leftPos + 10 && mouseX <= leftPos + 26 &&
                        mouseY >= y + 5 && mouseY <= y + 21) {
                    guiGraphics.renderTooltip(this.font, itemStack, mouseX, mouseY);
                }
            }

            // Nom de l'item et quantité
            Component itemText = Component.literal(offer.getQuantity() + "x ")
                    .append(offer.getItemDisplayName());
            guiGraphics.drawString(this.font, itemText, leftPos + 32, y + 4, 0xFFFFFF, false);

            // Prix
            long price = offer.getTotalPrice();
            boolean canAfford = ClientNocoinData.getBalance() >= price;
            int priceColor = currentTab == Tab.BUY ?
                    (canAfford ? 0x00FF00 : 0xFF5555) : 0xFFD700;
            guiGraphics.drawString(this.font, offer.getPriceComponent(), leftPos + 32, y + 15, priceColor, false);

            // Stock restant (pour les offres de vente)
            if (currentTab == Tab.BUY && offer.getStock() >= 0) {
                Component stockText = Component.literal("Stock : " + offer.getStock())
                        .withStyle(offer.hasStock() ? ChatFormatting.GREEN : ChatFormatting.RED);
                guiGraphics.drawString(this.font, stockText, leftPos + 150, y + 15, 0xFFFFFF, false);
            }
        }
    }

    private void renderBorder(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 2, color);
        guiGraphics.fill(x, y + height - 2, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 2, y + height, color);
        guiGraphics.fill(x + width - 2, y, x + width, y + height, color);
    }

    private String formatBalance(long balance) {
        return String.format("%,d", balance);
    }

    @Override
    public void tick() {
        super.tick();
        if (statusMessageTicks > 0) {
            statusMessageTicks--;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        List<ShopOffer> currentOffers = getCurrentOffers();
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset--;
            rebuildWidgets();
            return true;
        } else if (delta < 0 && scrollOffset < currentOffers.size() - MAX_VISIBLE_OFFERS) {
            scrollOffset++;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}

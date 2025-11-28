package com.zetsumei.nocoin.client.screen;

import com.zetsumei.nocoin.client.ClientNocoinData;
import com.zetsumei.nocoin.client.ClientShopData;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import com.zetsumei.nocoin.shop.ShopItem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Écran de la boutique NOCOIN.
 */
public class ShopScreen extends Screen {

    private static final Component TITLE = Component.literal("Boutique NOCOIN");

    // Dimensions de l'interface
    private static final int GUI_WIDTH = 280;
    private static final int GUI_HEIGHT = 220;
    private static final int ITEMS_PER_PAGE = 5;
    private static final int ITEM_HEIGHT = 30;
    private static final int ITEM_START_Y = 40;

    private int leftPos;
    private int topPos;
    private int currentPage = 0;
    private boolean isLoading = true;

    public ShopScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();

        // Centrer l'interface
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Demander les données de la boutique au serveur
        if (!ClientShopData.isLoaded()) {
            NocoinNetworkHandler.requestShopItemsFromServer();
        } else {
            isLoading = false;
        }

        // Rafraîchir le solde
        NocoinNetworkHandler.requestBalanceFromServer();

        // Créer les boutons
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();

        List<ShopItem> items = ClientShopData.getShopItems();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));

        // Boutons d'achat pour chaque article de la page
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = items.get(i);
            int yOffset = ITEM_START_Y + ((i - startIndex) * ITEM_HEIGHT);

            final int itemId = item.getId();
            Button buyButton = Button.builder(
                    Component.literal("Acheter"),
                    button -> purchaseItem(itemId)
            ).bounds(leftPos + GUI_WIDTH - 60, topPos + yOffset + 4, 50, 20).build();

            // Désactiver le bouton si le joueur n'a pas assez de NOCOIN
            buyButton.active = ClientNocoinData.getBalance() >= item.getPrice();

            this.addRenderableWidget(buyButton);
        }

        // Boutons de navigation
        Button prevButton = Button.builder(
                Component.literal("<"),
                button -> {
                    if (currentPage > 0) {
                        currentPage--;
                        rebuildButtons();
                    }
                }
        ).bounds(leftPos + 10, topPos + GUI_HEIGHT - 30, 30, 20).build();
        prevButton.active = currentPage > 0;
        this.addRenderableWidget(prevButton);

        Button nextButton = Button.builder(
                Component.literal(">"),
                button -> {
                    if (currentPage < totalPages - 1) {
                        currentPage++;
                        rebuildButtons();
                    }
                }
        ).bounds(leftPos + GUI_WIDTH - 40, topPos + GUI_HEIGHT - 30, 30, 20).build();
        nextButton.active = currentPage < totalPages - 1;
        this.addRenderableWidget(nextButton);

        // Bouton fermer
        this.addRenderableWidget(Button.builder(
                Component.literal("Fermer"),
                button -> this.onClose()
        ).bounds(leftPos + GUI_WIDTH / 2 - 40, topPos + GUI_HEIGHT - 30, 80, 20).build());
    }

    private void purchaseItem(int itemId) {
        // Définir un callback pour rafraîchir l'UI après l'achat
        ClientShopData.setPurchaseCallback(success -> {
            if (success && this.minecraft != null) {
                this.minecraft.execute(this::rebuildButtons);
            }
        });

        // Envoyer la demande d'achat au serveur
        NocoinNetworkHandler.sendPurchaseRequest(itemId);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fond semi-transparent
        this.renderBackground(guiGraphics);

        // Cadre de l'interface
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xCC1A1A2E);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + GUI_HEIGHT - 2, 0xCC2D2D44);

        // Bordure dorée
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + 2, 0xFFFFD700);
        guiGraphics.fill(leftPos, topPos + GUI_HEIGHT - 2, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFFFFD700);
        guiGraphics.fill(leftPos, topPos, leftPos + 2, topPos + GUI_HEIGHT, 0xFFFFD700);
        guiGraphics.fill(leftPos + GUI_WIDTH - 2, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFFFFD700);

        // Titre
        Component titleText = Component.literal("Boutique NOCOIN");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, leftPos + (GUI_WIDTH - titleWidth) / 2, topPos + 8, 0xFFD700, true);

        // Solde du joueur
        long balance = ClientNocoinData.getBalance();
        Component balanceText = Component.literal("Solde : " + formatBalance(balance) + " NOCOIN");
        guiGraphics.drawString(this.font, balanceText, leftPos + 10, topPos + 22, 0xFFFFFF, false);

        // Vérifier si les données sont chargées
        if (!ClientShopData.isLoaded()) {
            if (isLoading) {
                Component loadingText = Component.literal("Chargement de la boutique...");
                int loadingWidth = this.font.width(loadingText);
                guiGraphics.drawString(this.font, loadingText, leftPos + (GUI_WIDTH - loadingWidth) / 2, topPos + 100, 0xAAAAAA, false);
            }
        } else {
            isLoading = false;
            renderShopItems(guiGraphics, mouseX, mouseY);
        }

        // Info de pagination
        List<ShopItem> items = ClientShopData.getShopItems();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        Component pageText = Component.literal((currentPage + 1) + "/" + totalPages);
        int pageWidth = this.font.width(pageText);
        guiGraphics.drawString(this.font, pageText, leftPos + (GUI_WIDTH - pageWidth) / 2, topPos + GUI_HEIGHT - 25, 0xAAAAAA, false);

        // Rendu des widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // Tooltips pour les items survolés
        renderItemTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderShopItems(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<ShopItem> items = ClientShopData.getShopItems();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = items.get(i);
            int yOffset = ITEM_START_Y + ((i - startIndex) * ITEM_HEIGHT);

            // Fond de l'article
            int itemY = topPos + yOffset;
            guiGraphics.fill(leftPos + 5, itemY, leftPos + GUI_WIDTH - 5, itemY + ITEM_HEIGHT - 2, 0x40000000);

            // Icône de l'item
            ItemStack stack = item.createItemStack();
            if (!stack.isEmpty()) {
                guiGraphics.renderItem(stack, leftPos + 10, itemY + 6);
                guiGraphics.renderItemDecorations(this.font, stack, leftPos + 10, itemY + 6);
            }

            // Nom de l'article
            Component name = item.getDisplayComponent();
            guiGraphics.drawString(this.font, name, leftPos + 35, itemY + 5, 0xFFFFFF, false);

            // Prix
            long price = item.getPrice();
            boolean canAfford = ClientNocoinData.getBalance() >= price;
            int priceColor = canAfford ? 0x00FF00 : 0xFF5555;
            Component priceText = Component.literal(formatBalance(price) + " NC");
            guiGraphics.drawString(this.font, priceText, leftPos + 35, itemY + 16, priceColor, false);

            // Quantité
            if (item.getQuantity() > 1) {
                Component qtyText = Component.literal("x" + item.getQuantity());
                int qtyWidth = this.font.width(qtyText);
                guiGraphics.drawString(this.font, qtyText, leftPos + GUI_WIDTH - 70 - qtyWidth, itemY + 10, 0xAAAAAA, false);
            }
        }
    }

    private void renderItemTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        List<ShopItem> items = ClientShopData.getShopItems();
        int startIndex = currentPage * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem item = items.get(i);
            int yOffset = ITEM_START_Y + ((i - startIndex) * ITEM_HEIGHT);
            int itemY = topPos + yOffset;

            // Zone de l'icône
            if (mouseX >= leftPos + 10 && mouseX <= leftPos + 26 &&
                    mouseY >= itemY + 6 && mouseY <= itemY + 22) {
                ItemStack stack = item.createItemStack();
                if (!stack.isEmpty()) {
                    guiGraphics.renderTooltip(this.font, stack, mouseX, mouseY);
                }
            }
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.shouldCloseOnEsc() && keyCode == 256) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        super.tick();

        // Si les données viennent d'être chargées, reconstruire les boutons
        if (isLoading && ClientShopData.isLoaded()) {
            isLoading = false;
            rebuildButtons();
        }
    }
}

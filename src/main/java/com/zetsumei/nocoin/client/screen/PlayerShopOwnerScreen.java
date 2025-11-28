package com.zetsumei.nocoin.client.screen;

import com.zetsumei.nocoin.client.ClientNocoinData;
import com.zetsumei.nocoin.client.ClientPlayerShopHandler;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import com.zetsumei.nocoin.network.player.UpdateOfferStockPacket;
import com.zetsumei.nocoin.shop.player.ShopOffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Écran de configuration du magasin pour le propriétaire.
 * Permet de gérer les offres d'achat et de vente.
 */
public class PlayerShopOwnerScreen extends Screen {

    private static final Component TITLE = Component.literal("Gestion du Magasin");

    // Dimensions
    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 240;
    private static final int OFFER_HEIGHT = 24;
    private static final int MAX_VISIBLE_OFFERS = 6;

    private final BlockPos shopPos;
    private String shopName;
    private final String ownerName;
    private final List<ShopOffer> offers;

    private int leftPos;
    private int topPos;
    private int scrollOffset = 0;

    // Composants UI pour l'ajout d'offre
    private EditBox itemIdField;
    private EditBox quantityField;
    private EditBox priceField;
    private Button addSellButton;
    private Button addBuyButton;
    private EditBox shopNameField;
    private boolean showAddPanel = false;

    // Composants UI pour l'édition d'offre
    private EditBox editQuantityField;
    private EditBox editPriceField;
    private ShopOffer editingOffer = null;
    private boolean showEditPanel = false;

    public PlayerShopOwnerScreen(BlockPos shopPos, String shopName, String ownerName, List<ShopOffer> offers) {
        super(TITLE);
        this.shopPos = shopPos;
        this.shopName = shopName;
        this.ownerName = ownerName;
        this.offers = new ArrayList<>(offers);
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Champ nom du magasin
        shopNameField = new EditBox(this.font, leftPos + 80, topPos + 25, 150, 18,
                Component.literal("Nom du magasin"));
        shopNameField.setMaxLength(32);
        shopNameField.setValue(shopName);
        shopNameField.setResponder(s -> {
            this.shopName = s;
        });
        this.addRenderableWidget(shopNameField);

        // Bouton sauvegarder le nom
        this.addRenderableWidget(Button.builder(
                Component.literal("✓"),
                button -> saveShopName()
        ).bounds(leftPos + 235, topPos + 25, 20, 18).build());

        // Bouton pour afficher/masquer le panneau d'ajout
        this.addRenderableWidget(Button.builder(
                Component.literal("Ajouter offre"),
                button -> {
                    showAddPanel = !showAddPanel;
                    showEditPanel = false;
                    editingOffer = null;
                    rebuildWidgets();
                }
        ).bounds(leftPos + GUI_WIDTH - 90, topPos + 50, 80, 20).build());

        if (showAddPanel) {
            initAddOfferPanel();
        } else if (showEditPanel && editingOffer != null) {
            initEditOfferPanel();
        }

        // Bouton fermer
        this.addRenderableWidget(Button.builder(
                Component.literal("Fermer"),
                button -> this.onClose()
        ).bounds(leftPos + GUI_WIDTH / 2 - 40, topPos + GUI_HEIGHT - 28, 80, 20).build());

        // Boutons de scroll
        if (offers.size() > MAX_VISIBLE_OFFERS) {
            this.addRenderableWidget(Button.builder(
                    Component.literal("▲"),
                    button -> {
                        if (scrollOffset > 0) scrollOffset--;
                    }
            ).bounds(leftPos + GUI_WIDTH - 25, topPos + 80, 20, 15).build());

            this.addRenderableWidget(Button.builder(
                    Component.literal("▼"),
                    button -> {
                        if (scrollOffset < offers.size() - MAX_VISIBLE_OFFERS) scrollOffset++;
                    }
            ).bounds(leftPos + GUI_WIDTH - 25, topPos + 175, 20, 15).build());
        }

        initOfferButtons();
    }

    private void initAddOfferPanel() {
        int panelY = topPos + 75;

        // Champ ID de l'item
        itemIdField = new EditBox(this.font, leftPos + 10, panelY, 130, 16,
                Component.literal("ID Item"));
        itemIdField.setMaxLength(64);
        itemIdField.setHint(Component.literal("minecraft:diamond"));
        this.addRenderableWidget(itemIdField);

        // Champ quantité
        quantityField = new EditBox(this.font, leftPos + 145, panelY, 40, 16,
                Component.literal("Qté"));
        quantityField.setMaxLength(3);
        quantityField.setValue("1");
        quantityField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addRenderableWidget(quantityField);

        // Champ prix
        priceField = new EditBox(this.font, leftPos + 190, panelY, 60, 16,
                Component.literal("Prix"));
        priceField.setMaxLength(10);
        priceField.setValue("100");
        priceField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addRenderableWidget(priceField);

        // Boutons ajouter offre
        addSellButton = Button.builder(
                Component.literal("Vente"),
                button -> addOffer(ShopOffer.OfferType.SELL)
        ).bounds(leftPos + 10, panelY + 20, 70, 18).build();
        this.addRenderableWidget(addSellButton);

        addBuyButton = Button.builder(
                Component.literal("Achat"),
                button -> addOffer(ShopOffer.OfferType.BUY)
        ).bounds(leftPos + 85, panelY + 20, 70, 18).build();
        this.addRenderableWidget(addBuyButton);
    }


    private void initEditOfferPanel() {
        if (editingOffer == null) return;

        int panelY = topPos + 75;

        // Afficher l'item en cours d'édition
        // Le nom de l'item sera affiché dans le rendu

        // Champ quantité
        editQuantityField = new EditBox(this.font, leftPos + 10, panelY + 20, 60, 16,
                Component.literal("Qté"));
        editQuantityField.setMaxLength(3);
        editQuantityField.setValue(String.valueOf(editingOffer.getQuantity()));
        editQuantityField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addRenderableWidget(editQuantityField);

        // Champ prix
        editPriceField = new EditBox(this.font, leftPos + 80, panelY + 20, 80, 16,
                Component.literal("Prix"));
        editPriceField.setMaxLength(10);
        editPriceField.setValue(String.valueOf(editingOffer.getPricePerUnit()));
        editPriceField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.addRenderableWidget(editPriceField);

        // Bouton sauvegarder
        this.addRenderableWidget(Button.builder(
                Component.literal("Sauvegarder"),
                button -> saveEditedOffer()
        ).bounds(leftPos + 170, panelY + 20, 60, 18).build());

        // Bouton annuler
        this.addRenderableWidget(Button.builder(
                Component.literal("Annuler"),
                button -> {
                    showEditPanel = false;
                    editingOffer = null;
                    rebuildWidgets();
                }
        ).bounds(leftPos + 235, panelY + 20, 50, 18).build());
    }

    private void saveEditedOffer() {
        if (editingOffer == null) return;

        int newQuantity;
        long newPrice;
        try {
            newQuantity = Integer.parseInt(editQuantityField.getValue());
            newPrice = Long.parseLong(editPriceField.getValue());
        } catch (NumberFormatException e) {
            return;
        }

        if (newQuantity <= 0 || newPrice <= 0) return;

        // Envoyer la mise à jour au serveur
        NocoinNetworkHandler.sendUpdateOffer(shopPos, editingOffer.getOfferId(),
                newPrice, newQuantity, editingOffer.isActive());

        // Mettre à jour localement
        editingOffer.setPricePerUnit(newPrice);
        editingOffer.setQuantity(newQuantity);

        // Fermer le panneau d'édition
        showEditPanel = false;
        editingOffer = null;
        rebuildWidgets();
    }

    private void startEditOffer(ShopOffer offer) {
        editingOffer = offer;
        showEditPanel = true;
        showAddPanel = false;
        rebuildWidgets();
    }

    private void initOfferButtons() {
        int startY = (showAddPanel || showEditPanel) ? topPos + 120 : topPos + 80;
        int maxOffers = (showAddPanel || showEditPanel) ? MAX_VISIBLE_OFFERS - 2 : MAX_VISIBLE_OFFERS;

        for (int i = 0; i < maxOffers && i + scrollOffset < offers.size(); i++) {
            final ShopOffer offer = offers.get(i + scrollOffset);
            int y = startY + (i * OFFER_HEIGHT);

            // Bouton supprimer
            this.addRenderableWidget(Button.builder(
                    Component.literal("✕").withStyle(ChatFormatting.RED),
                    button -> removeOffer(offer)
            ).bounds(leftPos + GUI_WIDTH - 50, y + 2, 20, 18).build());

            // Bouton activer/désactiver
            Component toggleText = offer.isActive() ?
                    Component.literal("OUI").withStyle(ChatFormatting.GREEN) :
                    Component.literal("NON").withStyle(ChatFormatting.RED);
            this.addRenderableWidget(Button.builder(
                    toggleText,
                    button -> toggleOffer(offer)
            ).bounds(leftPos + GUI_WIDTH - 80, y + 2, 25, 18).build());

            // Bouton éditer (crayon)
            this.addRenderableWidget(Button.builder(
                    Component.literal("✎").withStyle(ChatFormatting.YELLOW),
                    button -> startEditOffer(offer)
            ).bounds(leftPos + GUI_WIDTH - 105, y + 2, 20, 18).build());

            // Bouton ajouter au stock (seulement pour les offres de vente)
            if (offer.getType() == ShopOffer.OfferType.SELL) {
                this.addRenderableWidget(Button.builder(
                        Component.literal("+").withStyle(ChatFormatting.GREEN),
                        button -> addToStock(offer)
                ).bounds(leftPos + GUI_WIDTH - 130, y + 2, 20, 18).build());
            }
        }
    }

    private void saveShopName() {
        NocoinNetworkHandler.sendUpdateShopName(shopPos, shopName);
    }

    private void addOffer(ShopOffer.OfferType type) {
        String itemId = itemIdField.getValue();
        if (itemId.isEmpty()) return;

        // Ajouter le préfixe minecraft: si absent
        if (!itemId.contains(":")) {
            itemId = "minecraft:" + itemId;
        }

        int quantity;
        long price;
        try {
            quantity = Integer.parseInt(quantityField.getValue());
            price = Long.parseLong(priceField.getValue());
        } catch (NumberFormatException e) {
            return;
        }

        if (quantity <= 0 || price <= 0) return;

        // Déterminer le stock initial (pour SELL = 0, pour BUY = illimité par défaut)
        int stock = type == ShopOffer.OfferType.SELL ? 0 : 0;

        NocoinNetworkHandler.sendAddShopOffer(shopPos, type, itemId, quantity, price, stock);

        // Ajouter temporairement à l'affichage local
        ShopOffer newOffer = new ShopOffer(type, itemId, quantity, price, stock);
        offers.add(newOffer);
        rebuildWidgets();

        // Réinitialiser les champs
        itemIdField.setValue("");
        quantityField.setValue("1");
        priceField.setValue("100");
    }

    private void removeOffer(ShopOffer offer) {
        NocoinNetworkHandler.sendRemoveShopOffer(shopPos, offer.getOfferId());
        offers.remove(offer);
        rebuildWidgets();
    }

    private void toggleOffer(ShopOffer offer) {
        boolean newActive = !offer.isActive();
        offer.setActive(newActive);
        // Envoyer la mise à jour au serveur
        NocoinNetworkHandler.sendUpdateOffer(shopPos, offer.getOfferId(),
                offer.getPricePerUnit(), offer.getQuantity(), newActive);
        rebuildWidgets();
    }

    /**
     * Ajoute des items de l'inventaire du joueur au stock de l'offre.
     * Ajoute la quantité définie dans l'offre (quantity) à chaque clic.
     */
    private void addToStock(ShopOffer offer) {
        // Envoyer une demande d'ajout au stock (1 unité de la quantité de l'offre)
        NocoinNetworkHandler.sendUpdateOfferStock(shopPos, offer.getOfferId(),
                UpdateOfferStockPacket.Action.ADD, offer.getQuantity());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // Cadre principal
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xDD1A1A2E);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + GUI_HEIGHT - 2, 0xDD2D2D44);

        // Bordure dorée
        renderBorder(guiGraphics, leftPos, topPos, GUI_WIDTH, GUI_HEIGHT, 0xFFFFD700);

        // Titre
        Component titleText = Component.literal("Gestion du Magasin");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, leftPos + (GUI_WIDTH - titleWidth) / 2, topPos + 8, 0xFFD700, true);

        // Label nom du magasin
        guiGraphics.drawString(this.font, Component.literal("Nom :"),
                leftPos + 10, topPos + 28, 0xFFFFFF, false);

        // Solde du propriétaire
        long balance = ClientNocoinData.getBalance();
        Component balanceText = Component.literal("Solde : " + formatBalance(balance) + " NOCOIN");
        guiGraphics.drawString(this.font, balanceText, leftPos + 10, topPos + 50, 0xAAAAAA, false);

        // Ligne de séparation
        int separatorY = (showAddPanel || showEditPanel) ? topPos + 115 : topPos + 75;
        guiGraphics.fill(leftPos + 10, separatorY, leftPos + GUI_WIDTH - 10, separatorY + 1, 0xFF555555);

        // Label des offres
        guiGraphics.drawString(this.font, Component.literal("Offres (" + offers.size() + "/9)"),
                leftPos + 10, separatorY + 5, 0xFFFFFF, false);

        // Rendu du panneau d'édition (titre de l'item en cours d'édition)
        if (showEditPanel && editingOffer != null) {
            Component editTitle = Component.literal("Modification : ")
                    .append(editingOffer.getItemDisplayName());
            guiGraphics.drawString(this.font, editTitle, leftPos + 10, topPos + 78, 0xFFD700, false);
        }

        // Rendu des offres
        renderOffers(guiGraphics, mouseX, mouseY);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderOffers(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int startY = (showAddPanel || showEditPanel) ? topPos + 120 : topPos + 80;
        int maxOffers = (showAddPanel || showEditPanel) ? MAX_VISIBLE_OFFERS - 2 : MAX_VISIBLE_OFFERS;

        for (int i = 0; i < maxOffers && i + scrollOffset < offers.size(); i++) {
            ShopOffer offer = offers.get(i + scrollOffset);
            int y = startY + (i * OFFER_HEIGHT);

            // Fond de l'offre
            int bgColor = offer.isActive() ? 0x44008800 : 0x44880000;
            guiGraphics.fill(leftPos + 5, y, leftPos + GUI_WIDTH - 90, y + OFFER_HEIGHT - 2, bgColor);

            // Type d'offre
            Component typeLabel = offer.getType() == ShopOffer.OfferType.SELL ?
                    Component.literal("[VEND]").withStyle(ChatFormatting.GREEN) :
                    Component.literal("[ACHÈTE]").withStyle(ChatFormatting.GOLD);
            guiGraphics.drawString(this.font, typeLabel, leftPos + 8, y + 3, 0xFFFFFF, false);

            // Icône de l'item
            ItemStack itemStack = offer.createItemStack();
            if (!itemStack.isEmpty()) {
                guiGraphics.renderItem(itemStack, leftPos + 55, y + 3);
            }

            // Nom de l'item et quantité
            Component itemText = Component.literal(offer.getQuantity() + "x ")
                    .append(offer.getItemDisplayName());
            guiGraphics.drawString(this.font, itemText, leftPos + 75, y + 3, 0xFFFFFF, false);

            // Prix
            guiGraphics.drawString(this.font, offer.getPriceComponent(),
                    leftPos + 75, y + 13, 0xFFD700, false);

            // Stock (pour les offres de vente)
            if (offer.getType() == ShopOffer.OfferType.SELL) {
                Component stockText = Component.literal("Stock : " + offer.getStock())
                        .withStyle(offer.getStock() > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
                guiGraphics.drawString(this.font, stockText, leftPos + 180, y + 13, 0xFFFFFF, false);
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
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0 && scrollOffset > 0) {
            scrollOffset--;
            rebuildWidgets();
            return true;
        } else if (delta < 0 && scrollOffset < offers.size() - MAX_VISIBLE_OFFERS) {
            scrollOffset++;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}

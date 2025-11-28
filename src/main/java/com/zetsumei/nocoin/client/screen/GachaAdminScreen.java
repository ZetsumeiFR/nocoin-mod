package com.zetsumei.nocoin.client.screen;

import com.zetsumei.nocoin.gacha.GachaRarity;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import com.zetsumei.nocoin.network.gacha.GachaCatalogPacket;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * Ecran d'administration de la machine Gacha.
 * Permet aux admins de gérer les récompenses et les taux.
 */
public class GachaAdminScreen extends Screen {

    private static final Component TITLE = Component.literal(
        "Administration Gacha"
    );

    private static final int GUI_WIDTH = 360;
    private static final int GUI_HEIGHT = 280;

    private int leftPos;
    private int topPos;

    // Position de la machine gacha
    private final BlockPos machinePos;

    // Onglets admin
    private enum AdminTab {
        REWARDS,
        RATES,
        ADD_REWARD,
    }

    private AdminTab currentTab = AdminTab.REWARDS;

    private Button tabRewardsButton;
    private Button tabRatesButton;
    private Button tabAddButton;

    // Données reçues du serveur
    private List<GachaCatalogPacket.CatalogEntry> rewards;
    private double fiveStarRate;
    private double fourStarRate;
    private double threeStarRate;

    // Liste des récompenses pour affichage
    private int rewardScrollOffset = 0;
    private static final int REWARDS_PER_PAGE = 8;
    private int selectedRewardIndex = -1;

    // Champs pour ajouter une récompense
    private EditBox itemIdField;
    private EditBox displayNameField;
    private EditBox weightField;
    private GachaRarity selectedRarity = GachaRarity.THREE_STAR;

    // Champs pour les taux
    private EditBox rate3Field;
    private EditBox rate4Field;
    private EditBox rate5Field;
    private Button applyRatesButton;

    // Boutons pour l'onglet Ajouter
    private Button rarity3Button;
    private Button rarity4Button;
    private Button rarity5Button;
    private Button addRewardButton;

    // Boutons pour l'onglet Récompenses
    private Button deleteButton;
    private Button plusWeightButton;
    private Button minusWeightButton;
    private Button changeRarityButton;

    // Messages de feedback
    private String feedbackMessage = "";
    private int feedbackColor = 0xFFFFFF;
    private int feedbackTick = 0;

    public GachaAdminScreen(
        BlockPos machinePos,
        List<GachaCatalogPacket.CatalogEntry> rewards,
        double fiveStarRate,
        double fourStarRate,
        double threeStarRate
    ) {
        super(TITLE);
        this.machinePos = machinePos;
        this.rewards = new ArrayList<>(rewards);
        this.fiveStarRate = fiveStarRate;
        this.fourStarRate = fourStarRate;
        this.threeStarRate = threeStarRate;
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Onglets
        int tabY = topPos + 5;
        int tabWidth = 100;

        tabRewardsButton = Button.builder(
            Component.literal("Récompenses"),
            button -> switchTab(AdminTab.REWARDS)
        )
            .bounds(leftPos + 15, tabY, tabWidth, 18)
            .build();
        this.addRenderableWidget(tabRewardsButton);

        tabRatesButton = Button.builder(Component.literal("Taux"), button ->
            switchTab(AdminTab.RATES)
        )
            .bounds(leftPos + 125, tabY, tabWidth, 18)
            .build();
        this.addRenderableWidget(tabRatesButton);

        tabAddButton = Button.builder(Component.literal("Ajouter"), button ->
            switchTab(AdminTab.ADD_REWARD)
        )
            .bounds(leftPos + 235, tabY, tabWidth, 18)
            .build();
        this.addRenderableWidget(tabAddButton);

        // Bouton fermer
        this.addRenderableWidget(
            Button.builder(Component.literal("Fermer"), button ->
                this.onClose()
            )
                .bounds(
                    leftPos + GUI_WIDTH / 2 - 40,
                    topPos + GUI_HEIGHT - 30,
                    80,
                    20
                )
                .build()
        );

        // Initialiser les widgets spécifiques aux onglets
        initRewardsTab();
        initRatesTab();
        initAddRewardTab();

        updateTabVisibility();
    }

    private void initRewardsTab() {
        // Boutons de modification pour la récompense sélectionnée
        deleteButton = Button.builder(Component.literal("Supprimer"), button ->
            deleteSelectedReward()
        )
            .bounds(leftPos + 20, topPos + 220, 80, 20)
            .build();
        this.addRenderableWidget(deleteButton);

        plusWeightButton = Button.builder(Component.literal("+Poids"), button ->
            modifyWeight(10)
        )
            .bounds(leftPos + 110, topPos + 220, 60, 20)
            .build();
        this.addRenderableWidget(plusWeightButton);

        minusWeightButton = Button.builder(Component.literal("-Poids"), button ->
            modifyWeight(-10)
        )
            .bounds(leftPos + 180, topPos + 220, 60, 20)
            .build();
        this.addRenderableWidget(minusWeightButton);

        changeRarityButton = Button.builder(Component.literal("Changer Rareté"), button ->
            cycleRarity()
        )
            .bounds(leftPos + 250, topPos + 220, 90, 20)
            .build();
        this.addRenderableWidget(changeRarityButton);
    }

    private void initRatesTab() {
        int fieldY = topPos + 80;
        int fieldWidth = 60;

        rate5Field = new EditBox(
            this.font,
            leftPos + 150,
            fieldY,
            fieldWidth,
            18,
            Component.literal("5★")
        );
        rate5Field.setMaxLength(5);
        rate5Field.setValue(String.format("%.1f", fiveStarRate));
        this.addRenderableWidget(rate5Field);

        rate4Field = new EditBox(
            this.font,
            leftPos + 150,
            fieldY + 30,
            fieldWidth,
            18,
            Component.literal("4★")
        );
        rate4Field.setMaxLength(5);
        rate4Field.setValue(String.format("%.1f", fourStarRate));
        this.addRenderableWidget(rate4Field);

        rate3Field = new EditBox(
            this.font,
            leftPos + 150,
            fieldY + 60,
            fieldWidth,
            18,
            Component.literal("3★")
        );
        rate3Field.setMaxLength(5);
        rate3Field.setValue(String.format("%.1f", threeStarRate));
        this.addRenderableWidget(rate3Field);

        applyRatesButton = Button.builder(Component.literal("Appliquer les Taux"), button ->
            applyRates()
        )
            .bounds(leftPos + GUI_WIDTH / 2 - 60, topPos + 180, 120, 20)
            .build();
        this.addRenderableWidget(applyRatesButton);
    }

    private void initAddRewardTab() {
        int fieldY = topPos + 60;
        int fieldWidth = 180;

        itemIdField = new EditBox(
            this.font,
            leftPos + 130,
            fieldY,
            fieldWidth,
            18,
            Component.literal("Item ID")
        );
        itemIdField.setMaxLength(100);
        itemIdField.setHint(Component.literal("minecraft:diamond"));
        this.addRenderableWidget(itemIdField);

        displayNameField = new EditBox(
            this.font,
            leftPos + 130,
            fieldY + 30,
            fieldWidth,
            18,
            Component.literal("Nom")
        );
        displayNameField.setMaxLength(50);
        displayNameField.setHint(Component.literal("Nom affiché"));
        this.addRenderableWidget(displayNameField);

        weightField = new EditBox(
            this.font,
            leftPos + 130,
            fieldY + 60,
            60,
            18,
            Component.literal("Poids")
        );
        weightField.setMaxLength(5);
        weightField.setValue("100");
        this.addRenderableWidget(weightField);

        // Boutons de sélection de rareté
        rarity3Button = Button.builder(Component.literal("★★★"), button ->
            selectedRarity = GachaRarity.THREE_STAR
        )
            .bounds(leftPos + 50, fieldY + 95, 50, 20)
            .build();
        this.addRenderableWidget(rarity3Button);

        rarity4Button = Button.builder(Component.literal("★★★★"), button ->
            selectedRarity = GachaRarity.FOUR_STAR
        )
            .bounds(leftPos + 110, fieldY + 95, 60, 20)
            .build();
        this.addRenderableWidget(rarity4Button);

        rarity5Button = Button.builder(Component.literal("★★★★★"), button ->
            selectedRarity = GachaRarity.FIVE_STAR
        )
            .bounds(leftPos + 180, fieldY + 95, 70, 20)
            .build();
        this.addRenderableWidget(rarity5Button);

        addRewardButton = Button.builder(Component.literal("Ajouter Récompense"), button ->
            addReward()
        )
            .bounds(leftPos + GUI_WIDTH / 2 - 70, topPos + 190, 140, 20)
            .build();
        this.addRenderableWidget(addRewardButton);
    }

    private void switchTab(AdminTab tab) {
        this.currentTab = tab;
        updateTabVisibility();
    }

    private void updateTabVisibility() {
        boolean isRewardsTab = currentTab == AdminTab.REWARDS;
        boolean isRatesTab = currentTab == AdminTab.RATES;
        boolean isAddTab = currentTab == AdminTab.ADD_REWARD;

        // Les champs de texte pour l'onglet Taux
        if (rate3Field != null) rate3Field.visible = isRatesTab;
        if (rate4Field != null) rate4Field.visible = isRatesTab;
        if (rate5Field != null) rate5Field.visible = isRatesTab;
        if (applyRatesButton != null) applyRatesButton.visible = isRatesTab;

        // Les champs de texte et boutons pour l'onglet Ajouter
        if (itemIdField != null) itemIdField.visible = isAddTab;
        if (displayNameField != null) displayNameField.visible = isAddTab;
        if (weightField != null) weightField.visible = isAddTab;
        if (rarity3Button != null) rarity3Button.visible = isAddTab;
        if (rarity4Button != null) rarity4Button.visible = isAddTab;
        if (rarity5Button != null) rarity5Button.visible = isAddTab;
        if (addRewardButton != null) addRewardButton.visible = isAddTab;

        // Les boutons pour l'onglet Récompenses
        if (deleteButton != null) deleteButton.visible = isRewardsTab;
        if (plusWeightButton != null) plusWeightButton.visible = isRewardsTab;
        if (minusWeightButton != null) minusWeightButton.visible = isRewardsTab;
        if (changeRarityButton != null) changeRarityButton.visible = isRewardsTab;
    }

    private void deleteSelectedReward() {
        if (selectedRewardIndex >= 0 && selectedRewardIndex < rewards.size()) {
            GachaCatalogPacket.CatalogEntry entry = rewards.get(
                selectedRewardIndex
            );
            NocoinNetworkHandler.sendGachaAdminRemoveReward(machinePos, entry.getItemId());
            showFeedback(
                "Suppression envoyée: " + entry.getDisplayName(),
                0x00FF00
            );

            rewards.remove(selectedRewardIndex);
            selectedRewardIndex = -1;
        } else {
            showFeedback("Sélectionnez une récompense d'abord", 0xFF5555);
        }
    }

    private void modifyWeight(int delta) {
        if (selectedRewardIndex >= 0 && selectedRewardIndex < rewards.size()) {
            GachaCatalogPacket.CatalogEntry entry = rewards.get(
                selectedRewardIndex
            );
            double newWeight = Math.max(1, entry.getWeight() + delta);
            NocoinNetworkHandler.sendGachaAdminModifyWeight(
                machinePos,
                entry.getItemId(),
                newWeight
            );
            showFeedback("Poids modifié: " + newWeight, 0x00FF00);
        } else {
            showFeedback("Sélectionnez une récompense d'abord", 0xFF5555);
        }
    }

    private void cycleRarity() {
        if (selectedRewardIndex >= 0 && selectedRewardIndex < rewards.size()) {
            GachaCatalogPacket.CatalogEntry entry = rewards.get(
                selectedRewardIndex
            );
            int currentStars = entry.getRarity().getStars();
            int newStars = (currentStars % 3) + 3; // Cycle 3 -> 4 -> 5 -> 3
            GachaRarity newRarity = switch (newStars) {
                case 4 -> GachaRarity.FOUR_STAR;
                case 5 -> GachaRarity.FIVE_STAR;
                default -> GachaRarity.THREE_STAR;
            };
            NocoinNetworkHandler.sendGachaAdminModifyRarity(
                machinePos,
                entry.getItemId(),
                newRarity
            );
            showFeedback(
                "Rareté changée: " + "★".repeat(newStars),
                getStarColor(newStars)
            );
        } else {
            showFeedback("Sélectionnez une récompense d'abord", 0xFF5555);
        }
    }

    private void applyRates() {
        try {
            double rate5 = Double.parseDouble(
                rate5Field.getValue().replace(",", ".")
            );
            double rate4 = Double.parseDouble(
                rate4Field.getValue().replace(",", ".")
            );
            double rate3 = Double.parseDouble(
                rate3Field.getValue().replace(",", ".")
            );

            double total = rate5 + rate4 + rate3;
            if (Math.abs(total - 100.0) > 0.1) {
                showFeedback(
                    "Le total doit être 100%! (Actuel: " +
                        String.format("%.1f", total) +
                        "%)",
                    0xFF5555
                );
                return;
            }

            NocoinNetworkHandler.sendGachaAdminSetRates(machinePos, rate5, rate4, rate3);
            showFeedback("Taux appliqués!", 0x00FF00);
        } catch (NumberFormatException e) {
            showFeedback("Valeurs numériques invalides", 0xFF5555);
        }
    }

    private void addReward() {
        String itemId = itemIdField.getValue().trim();
        String displayName = displayNameField.getValue().trim();
        String weightStr = weightField.getValue().trim();

        if (itemId.isEmpty()) {
            showFeedback("L'ID de l'item est requis", 0xFF5555);
            return;
        }

        if (displayName.isEmpty()) {
            showFeedback("Le nom est requis", 0xFF5555);
            return;
        }

        double weight;
        try {
            weight = Double.parseDouble(weightStr);
            if (weight <= 0) {
                showFeedback("Le poids doit être positif", 0xFF5555);
                return;
            }
        } catch (NumberFormatException e) {
            showFeedback("Poids invalide", 0xFF5555);
            return;
        }

        NocoinNetworkHandler.sendGachaAdminAddReward(
            machinePos,
            itemId,
            selectedRarity,
            displayName,
            weight
        );
        showFeedback("Récompense ajoutée: " + displayName, 0x00FF00);

        // Réinitialiser les champs
        itemIdField.setValue("");
        displayNameField.setValue("");
        weightField.setValue("100");
    }

    private void showFeedback(String message, int color) {
        this.feedbackMessage = message;
        this.feedbackColor = color;
        this.feedbackTick = 60;
    }

    @Override
    public void tick() {
        super.tick();
        if (feedbackTick > 0) {
            feedbackTick--;
        }
    }

    @Override
    public void render(
        GuiGraphics guiGraphics,
        int mouseX,
        int mouseY,
        float partialTick
    ) {
        this.renderBackground(guiGraphics);

        // Cadre principal
        guiGraphics.fill(
            leftPos,
            topPos,
            leftPos + GUI_WIDTH,
            topPos + GUI_HEIGHT,
            0xDD2A2A4E
        );
        guiGraphics.fill(
            leftPos + 2,
            topPos + 2,
            leftPos + GUI_WIDTH - 2,
            topPos + GUI_HEIGHT - 2,
            0xDD3D3D64
        );

        // Bordure rouge admin
        guiGraphics.fill(
            leftPos,
            topPos,
            leftPos + GUI_WIDTH,
            topPos + 3,
            0xFFFF4444
        );
        guiGraphics.fill(
            leftPos,
            topPos + GUI_HEIGHT - 3,
            leftPos + GUI_WIDTH,
            topPos + GUI_HEIGHT,
            0xFFFF4444
        );
        guiGraphics.fill(
            leftPos,
            topPos,
            leftPos + 3,
            topPos + GUI_HEIGHT,
            0xFFFF4444
        );
        guiGraphics.fill(
            leftPos + GUI_WIDTH - 3,
            topPos,
            leftPos + GUI_WIDTH,
            topPos + GUI_HEIGHT,
            0xFFFF4444
        );

        // Indicateur d'onglet actif
        int activeTabX = switch (currentTab) {
            case REWARDS -> leftPos + 15;
            case RATES -> leftPos + 125;
            case ADD_REWARD -> leftPos + 235;
        };
        guiGraphics.fill(
            activeTabX,
            topPos + 23,
            activeTabX + 100,
            topPos + 25,
            0xFFFF4444
        );

        // Titre
        Component titleText = Component.literal("Administration Gacha");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(
            this.font,
            titleText,
            leftPos + (GUI_WIDTH - titleWidth) / 2,
            topPos + 30,
            0xFFFF4444,
            true
        );

        // Contenu selon l'onglet
        switch (currentTab) {
            case REWARDS -> renderRewardsTab(guiGraphics, mouseX, mouseY);
            case RATES -> renderRatesTab(guiGraphics, mouseX, mouseY);
            case ADD_REWARD -> renderAddRewardTab(guiGraphics, mouseX, mouseY);
        }

        // Message de feedback
        if (feedbackTick > 0 && !feedbackMessage.isEmpty()) {
            int msgWidth = this.font.width(feedbackMessage);
            guiGraphics.drawString(
                this.font,
                feedbackMessage,
                leftPos + (GUI_WIDTH - msgWidth) / 2,
                topPos + GUI_HEIGHT - 50,
                feedbackColor,
                false
            );
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderRewardsTab(
        GuiGraphics guiGraphics,
        int mouseX,
        int mouseY
    ) {
        if (rewards.isEmpty()) {
            Component empty = Component.literal("Aucune récompense configurée");
            int emptyWidth = this.font.width(empty);
            guiGraphics.drawString(
                this.font,
                empty,
                leftPos + (GUI_WIDTH - emptyWidth) / 2,
                topPos + 100,
                0xAAAAAA,
                false
            );
            return;
        }

        int startY = topPos + 50;
        int itemHeight = 20;

        for (
            int i = 0;
            i < REWARDS_PER_PAGE && i + rewardScrollOffset < rewards.size();
            i++
        ) {
            int index = i + rewardScrollOffset;
            GachaCatalogPacket.CatalogEntry entry = rewards.get(index);
            int y = startY + i * itemHeight;

            // Fond (sélectionné ou alterné)
            int bgColor = (index == selectedRewardIndex)
                ? 0x60FFFF00
                : (i % 2 == 0 ? 0x40000000 : 0x20000000);
            guiGraphics.fill(
                leftPos + 10,
                y,
                leftPos + GUI_WIDTH - 10,
                y + itemHeight,
                bgColor
            );

            // Étoiles
            String stars = "★".repeat(entry.getRarity().getStars());
            int starColor = getStarColor(entry.getRarity().getStars());
            guiGraphics.drawString(
                this.font,
                stars,
                leftPos + 15,
                y + 6,
                starColor,
                false
            );

            // Nom
            guiGraphics.drawString(
                this.font,
                entry.getDisplayName(),
                leftPos + 60,
                y + 6,
                0xFFFFFF,
                false
            );

            // Poids
            String weight = "W:" + String.format("%.0f", entry.getWeight());
            int weightWidth = this.font.width(weight);
            guiGraphics.drawString(
                this.font,
                weight,
                leftPos + GUI_WIDTH - 20 - weightWidth,
                y + 6,
                0xAAAAAA,
                false
            );
        }

        // Instructions
        Component hint = Component.literal(
            "Cliquez pour sélectionner, molette pour défiler"
        );
        int hintWidth = this.font.width(hint);
        guiGraphics.drawString(
            this.font,
            hint,
            leftPos + (GUI_WIDTH - hintWidth) / 2,
            topPos + 205,
            0x666666,
            false
        );
    }

    private void renderRatesTab(
        GuiGraphics guiGraphics,
        int mouseX,
        int mouseY
    ) {
        int labelX = leftPos + 40;
        int fieldY = topPos + 80;

        guiGraphics.drawString(
            this.font,
            "Taux 5★ (Légendaire):",
            labelX,
            fieldY + 5,
            0xFFD700,
            false
        );
        guiGraphics.drawString(
            this.font,
            "%",
            leftPos + 215,
            fieldY + 5,
            0xFFFFFF,
            false
        );

        guiGraphics.drawString(
            this.font,
            "Taux 4★ (Épique):",
            labelX,
            fieldY + 35,
            0xAA00FF,
            false
        );
        guiGraphics.drawString(
            this.font,
            "%",
            leftPos + 215,
            fieldY + 35,
            0xFFFFFF,
            false
        );

        guiGraphics.drawString(
            this.font,
            "Taux 3★ (Commun):",
            labelX,
            fieldY + 65,
            0x5555FF,
            false
        );
        guiGraphics.drawString(
            this.font,
            "%",
            leftPos + 215,
            fieldY + 65,
            0xFFFFFF,
            false
        );

        // Calcul du total
        try {
            double rate5 = Double.parseDouble(
                rate5Field.getValue().replace(",", ".")
            );
            double rate4 = Double.parseDouble(
                rate4Field.getValue().replace(",", ".")
            );
            double rate3 = Double.parseDouble(
                rate3Field.getValue().replace(",", ".")
            );
            double total = rate5 + rate4 + rate3;

            int totalColor = Math.abs(total - 100.0) < 0.1
                ? 0x00FF00
                : 0xFF5555;
            String totalStr = String.format("Total: %.1f%%", total);
            int totalWidth = this.font.width(totalStr);
            guiGraphics.drawString(
                this.font,
                totalStr,
                leftPos + (GUI_WIDTH - totalWidth) / 2,
                fieldY + 100,
                totalColor,
                false
            );
        } catch (NumberFormatException e) {
            guiGraphics.drawString(
                this.font,
                "Valeurs invalides",
                leftPos + GUI_WIDTH / 2 - 40,
                fieldY + 100,
                0xFF5555,
                false
            );
        }
    }

    private void renderAddRewardTab(
        GuiGraphics guiGraphics,
        int mouseX,
        int mouseY
    ) {
        int labelX = leftPos + 20;
        int fieldY = topPos + 60;

        guiGraphics.drawString(
            this.font,
            "Item ID:",
            labelX,
            fieldY + 5,
            0xFFFFFF,
            false
        );
        guiGraphics.drawString(
            this.font,
            "Nom affiché:",
            labelX,
            fieldY + 35,
            0xFFFFFF,
            false
        );
        guiGraphics.drawString(
            this.font,
            "Poids:",
            labelX,
            fieldY + 65,
            0xFFFFFF,
            false
        );

        // Rareté sélectionnée
        guiGraphics.drawString(
            this.font,
            "Rareté:",
            labelX,
            fieldY + 100,
            0xFFFFFF,
            false
        );

        String selectedStars = "→ " + selectedRarity.getDisplayStars();
        int starColor = getStarColor(selectedRarity.getStars());
        guiGraphics.drawString(
            this.font,
            selectedStars,
            leftPos + 260,
            fieldY + 100,
            starColor,
            true
        );

        // Aide
        Component help1 = Component.literal(
            "Format Item ID: namespace:item_name"
        );
        Component help2 = Component.literal(
            "Ex: minecraft:diamond, nocoin:gacha_key"
        );
        guiGraphics.drawString(
            this.font,
            help1,
            leftPos + 20,
            topPos + 215,
            0x666666,
            false
        );
        guiGraphics.drawString(
            this.font,
            help2,
            leftPos + 20,
            topPos + 225,
            0x666666,
            false
        );
    }

    private int getStarColor(int stars) {
        return switch (stars) {
            case 5 -> 0xFFFFD700;
            case 4 -> 0xFFAA00FF;
            default -> 0xFF5555FF;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentTab == AdminTab.REWARDS && button == 0) {
            int startY = topPos + 50;
            int itemHeight = 20;

            for (
                int i = 0;
                i < REWARDS_PER_PAGE && i + rewardScrollOffset < rewards.size();
                i++
            ) {
                int y = startY + i * itemHeight;
                if (
                    mouseX >= leftPos + 10 &&
                    mouseX <= leftPos + GUI_WIDTH - 10 &&
                    mouseY >= y &&
                    mouseY < y + itemHeight
                ) {
                    selectedRewardIndex = i + rewardScrollOffset;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentTab == AdminTab.REWARDS && !rewards.isEmpty()) {
            int maxScroll = Math.max(0, rewards.size() - REWARDS_PER_PAGE);
            rewardScrollOffset = Math.max(
                0,
                Math.min(maxScroll, rewardScrollOffset - (int) delta)
            );
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

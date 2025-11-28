package com.zetsumei.nocoin.client.screen;

import com.zetsumei.nocoin.client.ClientGachaMachineHandler;
import com.zetsumei.nocoin.gacha.GachaHistory;
import com.zetsumei.nocoin.gacha.GachaManager;
import com.zetsumei.nocoin.item.ModItems;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import com.zetsumei.nocoin.network.gacha.GachaCatalogPacket;
import com.zetsumei.nocoin.network.gacha.GachaMultiPullResultPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Ecran de la machine a Gacha avec onglets.
 * - Tirage : tirage simple ou x10
 * - Catalogue : liste des récompenses possibles
 * - Historique : historique des tirages du joueur
 */
public class GachaMachineScreen extends Screen {

    private static final Component TITLE = Component.literal("Machine Gacha");

    // Dimensions
    private static final int GUI_WIDTH = 320;
    private static final int GUI_HEIGHT = 240;

    private int leftPos;
    private int topPos;

    // Position de la machine gacha
    private final BlockPos machinePos;

    private boolean hasKey;
    private int keyCount;

    // Onglet actuel
    private enum Tab { PULL, CATALOG, HISTORY }
    private Tab currentTab = Tab.PULL;

    // Boutons d'onglets
    private Button tabPullButton;
    private Button tabCatalogButton;
    private Button tabHistoryButton;

    // État de l'animation
    private boolean isPulling = false;
    private int pullAnimationTick = 0;
    private static final int PULL_ANIMATION_DURATION = 40;

    // Résultat du tirage simple
    private String lastResultItemId = null;
    private int lastResultStars = 0;
    private String lastResultCharacter = null;
    private int resultDisplayTick = 0;
    private static final int RESULT_DISPLAY_DURATION = 100;

    // Multi-pull
    private List<GachaMultiPullResultPacket.PullResult> multiPullResults = new ArrayList<>();
    private int multiPullDisplayIndex = 0;
    private int multiPullAnimTick = 0;
    private boolean isMultiPulling = false;

    // Catalogue
    private List<GachaCatalogPacket.CatalogEntry> catalogEntries = new ArrayList<>();
    private int catalogScrollOffset = 0;
    private static final int CATALOG_ITEMS_PER_PAGE = 8;

    // Historique
    private List<GachaHistory> historyEntries = new ArrayList<>();
    private int historyScrollOffset = 0;
    private static final int HISTORY_ITEMS_PER_PAGE = 8;

    private Button pullButton;
    private Button multiPullButton;

    public GachaMachineScreen(BlockPos machinePos, boolean hasKey, int keyCount) {
        super(TITLE);
        this.machinePos = machinePos;
        this.hasKey = hasKey;
        this.keyCount = keyCount;
    }

    @Override
    protected void init() {
        super.init();

        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Onglets
        int tabY = topPos + 5;
        int tabWidth = 80;
        
        tabPullButton = Button.builder(
                Component.literal("Tirage"),
                button -> switchTab(Tab.PULL)
        ).bounds(leftPos + 20, tabY, tabWidth, 18).build();
        this.addRenderableWidget(tabPullButton);

        tabCatalogButton = Button.builder(
                Component.literal("Catalogue"),
                button -> {
                    switchTab(Tab.CATALOG);
                    requestCatalog();
                }
        ).bounds(leftPos + 110, tabY, tabWidth, 18).build();
        this.addRenderableWidget(tabCatalogButton);

        tabHistoryButton = Button.builder(
                Component.literal("Historique"),
                button -> {
                    switchTab(Tab.HISTORY);
                    requestHistory();
                }
        ).bounds(leftPos + 200, tabY, tabWidth, 18).build();
        this.addRenderableWidget(tabHistoryButton);

        // Boutons de tirage
        pullButton = Button.builder(
                Component.literal("TIRER x1"),
                button -> performPull()
        ).bounds(leftPos + 40, topPos + 160, 100, 25).build();
        pullButton.active = hasKey && !isPulling && !isMultiPulling;
        this.addRenderableWidget(pullButton);

        multiPullButton = Button.builder(
                Component.literal("TIRER x10"),
                button -> performMultiPull()
        ).bounds(leftPos + 180, topPos + 160, 100, 25).build();
        multiPullButton.active = keyCount >= 10 && !isPulling && !isMultiPulling;
        this.addRenderableWidget(multiPullButton);

        // Bouton fermer
        this.addRenderableWidget(Button.builder(
                Component.literal("Fermer"),
                button -> this.onClose()
        ).bounds(leftPos + GUI_WIDTH / 2 - 40, topPos + GUI_HEIGHT - 30, 80, 20).build());

        // Callbacks
        ClientGachaMachineHandler.setPullResultCallback(this::onPullResult);
        ClientGachaMachineHandler.setMultiPullResultCallback((success, results) -> onMultiPullResult(results, results.size()));
        ClientGachaMachineHandler.setCatalogCallback((entries, r5, r4, r3) -> onCatalogReceived(entries));
        ClientGachaMachineHandler.setHistoryCallback(this::onHistoryReceived);

        updateTabVisibility();
    }

    private void switchTab(Tab tab) {
        this.currentTab = tab;
        updateTabVisibility();
    }

    private void updateTabVisibility() {
        boolean isPullTab = currentTab == Tab.PULL;
        pullButton.visible = isPullTab;
        multiPullButton.visible = isPullTab;
        
        // Reset scroll
        if (currentTab == Tab.CATALOG) {
            catalogScrollOffset = 0;
        } else if (currentTab == Tab.HISTORY) {
            historyScrollOffset = 0;
        }
    }

    private void requestCatalog() {
        NocoinNetworkHandler.requestGachaCatalog(machinePos);
    }

    private void requestHistory() {
        NocoinNetworkHandler.requestGachaHistory(machinePos);
    }

    private void performPull() {
        if (!hasKey || isPulling || isMultiPulling) return;

        isPulling = true;
        pullAnimationTick = 0;
        lastResultItemId = null;
        multiPullResults.clear();
        pullButton.active = false;
        multiPullButton.active = false;

        NocoinNetworkHandler.sendGachaPullRequest(machinePos);
    }

    private void performMultiPull() {
        if (keyCount < 10 || isPulling || isMultiPulling) return;

        isMultiPulling = true;
        isPulling = true;
        pullAnimationTick = 0;
        multiPullResults.clear();
        multiPullDisplayIndex = 0;
        multiPullAnimTick = 0;
        lastResultItemId = null;
        pullButton.active = false;
        multiPullButton.active = false;

        NocoinNetworkHandler.sendGachaMultiPullRequest(machinePos, 10);
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

                    keyCount--;
                    hasKey = keyCount > 0;
                }

                updatePullButtons();
            });
        }
    }

    private void onMultiPullResult(List<GachaMultiPullResultPacket.PullResult> results, int keysUsed) {
        if (this.minecraft != null) {
            this.minecraft.execute(() -> {
                isPulling = false;
                this.multiPullResults = new ArrayList<>(results);
                this.multiPullDisplayIndex = 0;
                this.multiPullAnimTick = 0;
                
                keyCount -= keysUsed;
                hasKey = keyCount > 0;

                updatePullButtons();
            });
        }
    }

    private void onCatalogReceived(List<GachaCatalogPacket.CatalogEntry> entries) {
        if (this.minecraft != null) {
            this.minecraft.execute(() -> {
                this.catalogEntries = new ArrayList<>(entries);
                this.catalogScrollOffset = 0;
            });
        }
    }

    private void onHistoryReceived(List<GachaHistory> entries) {
        if (this.minecraft != null) {
            this.minecraft.execute(() -> {
                this.historyEntries = new ArrayList<>(entries);
                this.historyScrollOffset = 0;
            });
        }
    }

    private void updatePullButtons() {
        pullButton.active = hasKey && !isPulling && !isMultiPulling && multiPullResults.isEmpty();
        multiPullButton.active = keyCount >= 10 && !isPulling && !isMultiPulling && multiPullResults.isEmpty();
    }

    @Override
    public void tick() {
        super.tick();

        if (isPulling) {
            pullAnimationTick++;
        }

        if (lastResultItemId != null) {
            resultDisplayTick++;
        }

        // Animation multi-pull
        if (!multiPullResults.isEmpty() && multiPullDisplayIndex < multiPullResults.size()) {
            multiPullAnimTick++;
            if (multiPullAnimTick >= 15) { // Affiche chaque résultat pendant 0.75s
                multiPullAnimTick = 0;
                multiPullDisplayIndex++;
            }
        }

        // Fin de l'affichage multi-pull
        if (!multiPullResults.isEmpty() && multiPullDisplayIndex >= multiPullResults.size()) {
            // Garder les résultats affichés un moment
            multiPullAnimTick++;
            if (multiPullAnimTick >= 100) {
                multiPullResults.clear();
                updatePullButtons();
            }
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int bgColor = isPulling ? 0xDD1A1A3E : 0xCC1A1A2E;
        int bgColor2 = isPulling ? 0xDD2D2D54 : 0xCC2D2D44;

        // Cadre principal
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, bgColor);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + GUI_HEIGHT - 2, bgColor2);

        // Bordure
        int borderColor = getBorderColor();
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + 3, borderColor);
        guiGraphics.fill(leftPos, topPos + GUI_HEIGHT - 3, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, borderColor);
        guiGraphics.fill(leftPos, topPos, leftPos + 3, topPos + GUI_HEIGHT, borderColor);
        guiGraphics.fill(leftPos + GUI_WIDTH - 3, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, borderColor);

        // Indicateur d'onglet actif
        int activeTabX = switch (currentTab) {
            case PULL -> leftPos + 20;
            case CATALOG -> leftPos + 110;
            case HISTORY -> leftPos + 200;
        };
        guiGraphics.fill(activeTabX, topPos + 23, activeTabX + 80, topPos + 25, 0xFFFFD700);

        // Contenu selon l'onglet
        switch (currentTab) {
            case PULL -> renderPullTab(guiGraphics, mouseX, mouseY);
            case CATALOG -> renderCatalogTab(guiGraphics, mouseX, mouseY);
            case HISTORY -> renderHistoryTab(guiGraphics, mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderPullTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Titre
        Component titleText = Component.literal("Machine Gacha");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, leftPos + (GUI_WIDTH - titleWidth) / 2, topPos + 30, 0xFFD700, true);

        // Clés
        ItemStack keyStack = new ItemStack(ModItems.GACHA_KEY.get());
        int keyIconX = leftPos + 15;
        int keyIconY = topPos + 45;
        guiGraphics.renderItem(keyStack, keyIconX, keyIconY);
        Component keyCountText = Component.literal("x" + keyCount)
                .withStyle(keyCount > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);
        guiGraphics.drawString(this.font, keyCountText, keyIconX + 20, keyIconY + 4, 0xFFFFFF, false);

        int centerX = leftPos + GUI_WIDTH / 2;
        int centerY = topPos + 95;

        // Multi-pull results
        if (!multiPullResults.isEmpty()) {
            renderMultiPullResults(guiGraphics, centerX, centerY);
        } else if (isPulling) {
            renderPullAnimation(guiGraphics, centerX, centerY);
        } else if (lastResultItemId != null) {
            renderResult(guiGraphics, centerX, centerY);
        } else {
            renderInstructions(guiGraphics, centerX, centerY);
        }

        // Probabilités
        renderProbabilities(guiGraphics, leftPos + 10, topPos + 195);
    }

    private void renderMultiPullResults(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (multiPullDisplayIndex < multiPullResults.size()) {
            // Affichage animé
            GachaMultiPullResultPacket.PullResult current = multiPullResults.get(multiPullDisplayIndex);

            String countText = String.format("Tirage %d/%d", multiPullDisplayIndex + 1, multiPullResults.size());
            int countWidth = this.font.width(countText);
            guiGraphics.drawString(this.font, countText, centerX - countWidth / 2, centerY - 50, 0xFFFFFF, false);

            String stars = "★".repeat(current.getStars());
            int starColor = getStarColor(current.getStars());
            int starsWidth = this.font.width(stars);
            guiGraphics.drawString(this.font, stars, centerX - starsWidth / 2, centerY - 35, starColor, true);

            // Afficher l'icône de l'item
            ItemStack itemStack = getItemStackFromId(current.getItemId());
            if (!itemStack.isEmpty()) {
                guiGraphics.renderItem(itemStack, centerX - 8, centerY - 18);
            }

            String name = current.getDisplayName();
            int nameWidth = this.font.width(name);
            guiGraphics.drawString(this.font, name, centerX - nameWidth / 2, centerY + 5, 0xFFFFFF, true);
        } else {
            // Résumé final
            Component summaryTitle = Component.literal("Résultats x10");
            int titleWidth = this.font.width(summaryTitle);
            guiGraphics.drawString(this.font, summaryTitle, centerX - titleWidth / 2, centerY - 55, 0xFFD700, true);

            int y = centerY - 35;
            int col1X = leftPos + 25;
            int col2X = leftPos + GUI_WIDTH / 2 + 5;

            for (int i = 0; i < multiPullResults.size(); i++) {
                GachaMultiPullResultPacket.PullResult r = multiPullResults.get(i);
                int x = (i < 5) ? col1X : col2X;
                int yOffset = (i < 5) ? i * 18 : (i - 5) * 18;

                // Afficher l'icône
                ItemStack itemStack = getItemStackFromId(r.getItemId());
                if (!itemStack.isEmpty()) {
                    guiGraphics.renderItem(itemStack, x, y + yOffset - 1);
                }

                String stars = "★".repeat(r.getStars());
                int starColor = getStarColor(r.getStars());
                guiGraphics.drawString(this.font, stars, x + 18, y + yOffset + 4, starColor, false);
                guiGraphics.drawString(this.font, r.getDisplayName(), x + 50, y + yOffset + 4, 0xFFFFFF, false);
            }
        }
    }

    private void renderCatalogTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component titleText = Component.literal("Catalogue des Récompenses");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, leftPos + (GUI_WIDTH - titleWidth) / 2, topPos + 30, 0xFFD700, true);

        if (catalogEntries.isEmpty()) {
            Component loading = Component.literal("Chargement...");
            int loadWidth = this.font.width(loading);
            guiGraphics.drawString(this.font, loading, leftPos + (GUI_WIDTH - loadWidth) / 2, topPos + 100, 0xAAAAAA, false);
            return;
        }

        int startY = topPos + 50;
        int itemHeight = 20;

        for (int i = 0; i < CATALOG_ITEMS_PER_PAGE && i + catalogScrollOffset < catalogEntries.size(); i++) {
            GachaCatalogPacket.CatalogEntry entry = catalogEntries.get(i + catalogScrollOffset);
            int y = startY + i * itemHeight;

            // Fond alterné
            if (i % 2 == 0) {
                guiGraphics.fill(leftPos + 10, y, leftPos + GUI_WIDTH - 10, y + itemHeight, 0x40000000);
            }

            // Icône de l'item
            ItemStack itemStack = getItemStackFromId(entry.getItemId());
            if (!itemStack.isEmpty()) {
                guiGraphics.renderItem(itemStack, leftPos + 12, y + 2);
            }

            // Étoiles
            String stars = "★".repeat(entry.getRarity().getStars());
            int starColor = getStarColor(entry.getRarity().getStars());
            guiGraphics.drawString(this.font, stars, leftPos + 32, y + 6, starColor, false);

            // Nom
            guiGraphics.drawString(this.font, entry.getDisplayName(), leftPos + 75, y + 6, 0xFFFFFF, false);

            // Probabilité
            String chance = String.format("%.2f%%", entry.getEffectiveChance());
            int chanceWidth = this.font.width(chance);
            guiGraphics.drawString(this.font, chance, leftPos + GUI_WIDTH - 20 - chanceWidth, y + 6, 0xAAAAAA, false);
        }

        // Indicateur de scroll
        if (catalogEntries.size() > CATALOG_ITEMS_PER_PAGE) {
            String scrollInfo = String.format("%d-%d / %d",
                catalogScrollOffset + 1,
                Math.min(catalogScrollOffset + CATALOG_ITEMS_PER_PAGE, catalogEntries.size()),
                catalogEntries.size());
            int scrollWidth = this.font.width(scrollInfo);
            guiGraphics.drawString(this.font, scrollInfo, leftPos + (GUI_WIDTH - scrollWidth) / 2, topPos + GUI_HEIGHT - 50, 0x888888, false);
        }
    }

    private void renderHistoryTab(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        Component titleText = Component.literal("Historique des Tirages");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, leftPos + (GUI_WIDTH - titleWidth) / 2, topPos + 30, 0xFFD700, true);

        if (historyEntries.isEmpty()) {
            Component empty = Component.literal("Aucun tirage effectué");
            int emptyWidth = this.font.width(empty);
            guiGraphics.drawString(this.font, empty, leftPos + (GUI_WIDTH - emptyWidth) / 2, topPos + 100, 0xAAAAAA, false);
            return;
        }

        int startY = topPos + 50;
        int itemHeight = 20;
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");

        for (int i = 0; i < HISTORY_ITEMS_PER_PAGE && i + historyScrollOffset < historyEntries.size(); i++) {
            GachaHistory entry = historyEntries.get(i + historyScrollOffset);
            int y = startY + i * itemHeight;

            // Fond alterné
            if (i % 2 == 0) {
                guiGraphics.fill(leftPos + 10, y, leftPos + GUI_WIDTH - 10, y + itemHeight, 0x40000000);
            }

            // Icône de l'item
            ItemStack itemStack = getItemStackFromId(entry.getItemId());
            if (!itemStack.isEmpty()) {
                guiGraphics.renderItem(itemStack, leftPos + 12, y + 2);
            }

            // Étoiles
            String stars = "★".repeat(entry.getStars());
            int starColor = getStarColor(entry.getStars());
            guiGraphics.drawString(this.font, stars, leftPos + 32, y + 6, starColor, false);

            // Nom
            guiGraphics.drawString(this.font, entry.getDisplayName(), leftPos + 75, y + 6, 0xFFFFFF, false);

            // Date
            String date = sdf.format(new Date(entry.getTimestamp()));
            int dateWidth = this.font.width(date);
            guiGraphics.drawString(this.font, date, leftPos + GUI_WIDTH - 20 - dateWidth, y + 6, 0x888888, false);
        }

        // Indicateur de scroll
        if (historyEntries.size() > HISTORY_ITEMS_PER_PAGE) {
            String scrollInfo = String.format("%d-%d / %d",
                historyScrollOffset + 1,
                Math.min(historyScrollOffset + HISTORY_ITEMS_PER_PAGE, historyEntries.size()),
                historyEntries.size());
            int scrollWidth = this.font.width(scrollInfo);
            guiGraphics.drawString(this.font, scrollInfo, leftPos + (GUI_WIDTH - scrollWidth) / 2, topPos + GUI_HEIGHT - 50, 0x888888, false);
        }
    }

    private int getStarColor(int stars) {
        return switch (stars) {
            case 5 -> 0xFFFFD700; // Or
            case 4 -> 0xFFAA00FF; // Violet
            default -> 0xFF5555FF; // Bleu
        };
    }

    /**
     * Crée un ItemStack à partir d'un itemId (ex: "minecraft:diamond").
     */
    private ItemStack getItemStackFromId(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ResourceLocation resourceLocation = ResourceLocation.tryParse(itemId);
        if (resourceLocation != null) {
            Item item = ForgeRegistries.ITEMS.getValue(resourceLocation);
            if (item != null && item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    private int getBorderColor() {
        if (lastResultItemId != null && resultDisplayTick < 60) {
            return getStarColor(lastResultStars);
        }
        if (!multiPullResults.isEmpty() && multiPullDisplayIndex < multiPullResults.size()) {
            return getStarColor(multiPullResults.get(multiPullDisplayIndex).getStars());
        }
        return 0xFFFFD700;
    }

    private void renderPullAnimation(GuiGraphics guiGraphics, int centerX, int centerY) {
        String animChars = "★☆✦✧";
        int charIndex = (pullAnimationTick / 5) % animChars.length();
        String animText = String.valueOf(animChars.charAt(charIndex)).repeat(5);

        int animWidth = this.font.width(animText);
        int color = (pullAnimationTick / 3 % 2 == 0) ? 0xFFFFD700 : 0xFFFFFFFF;
        guiGraphics.drawString(this.font, animText, centerX - animWidth / 2, centerY, color, true);

        Component pullingText = Component.literal(isMultiPulling ? "Tirage x10 en cours..." : "Tirage en cours...");
        int textWidth = this.font.width(pullingText);
        guiGraphics.drawString(this.font, pullingText, centerX - textWidth / 2, centerY + 20, 0xFFFFFF, false);
    }

    private void renderResult(GuiGraphics guiGraphics, int centerX, int centerY) {
        String stars = "★".repeat(lastResultStars);
        int starColor = getStarColor(lastResultStars);
        int starsWidth = this.font.width(stars);
        guiGraphics.drawString(this.font, stars, centerX - starsWidth / 2, centerY - 35, starColor, true);

        // Afficher l'icône de l'item
        ItemStack itemStack = getItemStackFromId(lastResultItemId);
        if (!itemStack.isEmpty()) {
            guiGraphics.renderItem(itemStack, centerX - 8, centerY - 18);
        }

        Component itemName = Component.literal(lastResultCharacter != null ? lastResultCharacter : "Récompense");
        int nameWidth = this.font.width(itemName);
        guiGraphics.drawString(this.font, itemName, centerX - nameWidth / 2, centerY + 5, 0xFFFFFF, true);

        Component congratsKey = switch (lastResultStars) {
            case 5 -> Component.literal("§6★ LÉGENDAIRE ! ★");
            case 4 -> Component.literal("§5✦ Épique ! ✦");
            default -> Component.literal("Obtenu !");
        };
        int congratsWidth = this.font.width(congratsKey);
        guiGraphics.drawString(this.font, congratsKey, centerX - congratsWidth / 2, centerY + 25, starColor, false);
    }

    private void renderInstructions(GuiGraphics guiGraphics, int centerX, int centerY) {
        Component instruction1 = Component.literal("Insérez une Clé Gacha pour tirer");
        Component instruction2 = Component.literal("x1 = 1 clé | x10 = 10 clés");

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
        guiGraphics.drawString(this.font, prob4, x + 90, y + 10, 0xAA00FF, false);
        guiGraphics.drawString(this.font, prob3, x + 180, y + 10, 0x5555FF, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (currentTab == Tab.CATALOG && !catalogEntries.isEmpty()) {
            int maxScroll = Math.max(0, catalogEntries.size() - CATALOG_ITEMS_PER_PAGE);
            catalogScrollOffset = Math.max(0, Math.min(maxScroll, catalogScrollOffset - (int) delta));
            return true;
        } else if (currentTab == Tab.HISTORY && !historyEntries.isEmpty()) {
            int maxScroll = Math.max(0, historyEntries.size() - HISTORY_ITEMS_PER_PAGE);
            historyScrollOffset = Math.max(0, Math.min(maxScroll, historyScrollOffset - (int) delta));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        ClientGachaMachineHandler.setPullResultCallback(null);
        ClientGachaMachineHandler.setMultiPullResultCallback(null);
        ClientGachaMachineHandler.setCatalogCallback(null);
        ClientGachaMachineHandler.setHistoryCallback(null);
        super.onClose();
    }
}

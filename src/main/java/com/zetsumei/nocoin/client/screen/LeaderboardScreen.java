package com.zetsumei.nocoin.client.screen;

import com.zetsumei.nocoin.client.ClientLeaderboardData;
import com.zetsumei.nocoin.leaderboard.LeaderboardEntry;
import com.zetsumei.nocoin.leaderboard.LeaderboardManager;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Ecran du classement des joueurs.
 * Affiche le classement NOCOIN.
 */
public class LeaderboardScreen extends Screen {

    private static final Component TITLE = Component.literal("Classement");

    // Dimensions de l'interface
    private static final int GUI_WIDTH = 300;
    private static final int GUI_HEIGHT = 260;
    private static final int ENTRIES_PER_PAGE = 10;
    private static final int ENTRY_HEIGHT = 18;
    private static final int ENTRY_START_Y = 45;

    private int leftPos;
    private int topPos;
    private int currentPage = 0;
    private boolean isLoading = true;

    public LeaderboardScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        super.init();

        // Centrer l'interface
        this.leftPos = (this.width - GUI_WIDTH) / 2;
        this.topPos = (this.height - GUI_HEIGHT) / 2;

        // Reinitialiser l'etat de chargement
        ClientLeaderboardData.resetLoadingState();

        // Demander les donnees au serveur
        NocoinNetworkHandler.requestLeaderboardFromServer(LeaderboardManager.LeaderboardType.NOCOIN);

        // Creer les boutons
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();

        // Calculer le nombre total de pages
        List<LeaderboardEntry> entries = ClientLeaderboardData.getNocoinEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ENTRIES_PER_PAGE));

        // Boutons de navigation
        Button prevButton = Button.builder(
                Component.literal("\u25c4"),
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
                Component.literal("\u25ba"),
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fond semi-transparent
        this.renderBackground(guiGraphics);

        // Cadre de l'interface
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xCC1A1A2E);
        guiGraphics.fill(leftPos + 2, topPos + 2, leftPos + GUI_WIDTH - 2, topPos + GUI_HEIGHT - 2, 0xCC2D2D44);

        // Bordure doree
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + 2, 0xFFFFD700);
        guiGraphics.fill(leftPos, topPos + GUI_HEIGHT - 2, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFFFFD700);
        guiGraphics.fill(leftPos, topPos, leftPos + 2, topPos + GUI_HEIGHT, 0xFFFFD700);
        guiGraphics.fill(leftPos + GUI_WIDTH - 2, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xFFFFD700);

        // Titre
        Component titleText = Component.literal("Classement NOCOIN");
        int titleWidth = this.font.width(titleText);
        guiGraphics.drawString(this.font, titleText, leftPos + (GUI_WIDTH - titleWidth) / 2, topPos + 10, 0xFFD700, true);

        // Verifier si les donnees sont chargees
        if (!ClientLeaderboardData.isNocoinLoaded()) {
            if (isLoading) {
                Component loadingText = Component.literal("Chargement...");
                int loadingWidth = this.font.width(loadingText);
                guiGraphics.drawString(this.font, loadingText, leftPos + (GUI_WIDTH - loadingWidth) / 2, topPos + 120, 0xAAAAAA, false);
            }
        } else {
            isLoading = false;
            renderLeaderboardEntries(guiGraphics);
        }

        // Info de pagination
        List<LeaderboardEntry> entries = ClientLeaderboardData.getNocoinEntries();
        int totalPages = Math.max(1, (int) Math.ceil((double) entries.size() / ENTRIES_PER_PAGE));
        Component pageText = Component.literal((currentPage + 1) + "/" + totalPages);
        int pageWidth = this.font.width(pageText);
        guiGraphics.drawString(this.font, pageText, leftPos + (GUI_WIDTH - pageWidth) / 2, topPos + GUI_HEIGHT - 25, 0xAAAAAA, false);

        // Rendu des widgets
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderLeaderboardEntries(GuiGraphics guiGraphics) {
        List<LeaderboardEntry> entries = ClientLeaderboardData.getNocoinEntries();

        if (entries.isEmpty()) {
            Component emptyText = Component.literal("Aucun joueur a afficher");
            int emptyWidth = this.font.width(emptyText);
            guiGraphics.drawString(this.font, emptyText, leftPos + (GUI_WIDTH - emptyWidth) / 2, topPos + 120, 0xAAAAAA, false);
            return;
        }

        int startIndex = currentPage * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, entries.size());
        String currentPlayer = ClientLeaderboardData.getCurrentPlayerName();

        for (int i = startIndex; i < endIndex; i++) {
            LeaderboardEntry entry = entries.get(i);
            int yOffset = ENTRY_START_Y + ((i - startIndex) * ENTRY_HEIGHT);
            int entryY = topPos + yOffset;

            // Couleur de fond : mettre en surbrillance le joueur actuel
            boolean isCurrentPlayer = entry.getPlayerName().equals(currentPlayer);
            int bgColor = isCurrentPlayer ? 0x40FFD700 : 0x30000000;
            guiGraphics.fill(leftPos + 5, entryY, leftPos + GUI_WIDTH - 5, entryY + ENTRY_HEIGHT - 2, bgColor);

            // Rang avec medailles pour le top 3
            String rankText = getRankDisplay(entry.getRank());
            int rankColor = getRankColor(entry.getRank());
            guiGraphics.drawString(this.font, rankText, leftPos + 10, entryY + 4, rankColor, true);

            // Nom du joueur
            String playerName = entry.getPlayerName();
            if (playerName.length() > 16) {
                playerName = playerName.substring(0, 14) + "...";
            }
            int nameColor = isCurrentPlayer ? 0xFFD700 : 0xFFFFFF;
            guiGraphics.drawString(this.font, playerName, leftPos + 50, entryY + 4, nameColor, false);

            // Valeur NOCOIN
            String valueText = formatBalance(entry.getNocoinBalance()) + " NC";
            int valueWidth = this.font.width(valueText);
            guiGraphics.drawString(this.font, valueText, leftPos + GUI_WIDTH - valueWidth - 15, entryY + 4, 0x00FF00, false);
        }

        // Afficher la position du joueur actuel s'il n'est pas visible
        renderPlayerPosition(guiGraphics, entries, currentPlayer);
    }

    private void renderPlayerPosition(GuiGraphics guiGraphics, List<LeaderboardEntry> entries, String currentPlayer) {
        // Verifier si le joueur actuel est dans la page visible
        int startIndex = currentPage * ENTRIES_PER_PAGE;
        int endIndex = Math.min(startIndex + ENTRIES_PER_PAGE, entries.size());

        boolean playerVisible = false;
        LeaderboardEntry playerEntry = null;

        for (int i = startIndex; i < endIndex; i++) {
            if (entries.get(i).getPlayerName().equals(currentPlayer)) {
                playerVisible = true;
                break;
            }
        }

        // Trouver l'entree du joueur si non visible
        if (!playerVisible) {
            for (LeaderboardEntry entry : entries) {
                if (entry.getPlayerName().equals(currentPlayer)) {
                    playerEntry = entry;
                    break;
                }
            }

            if (playerEntry != null) {
                // Afficher la position du joueur en bas
                int infoY = topPos + GUI_HEIGHT - 50;
                guiGraphics.fill(leftPos + 5, infoY, leftPos + GUI_WIDTH - 5, infoY + 16, 0x60FFD700);

                Component yourRank = Component.literal("Votre rang : #" + playerEntry.getRank());
                guiGraphics.drawString(this.font, yourRank, leftPos + 10, infoY + 4, 0xFFD700, true);

                String valueText = formatBalance(playerEntry.getNocoinBalance()) + " NC";
                int valueWidth = this.font.width(valueText);
                guiGraphics.drawString(this.font, valueText, leftPos + GUI_WIDTH - valueWidth - 15, infoY + 4, 0x00FF00, false);
            }
        }
    }

    private String getRankDisplay(int rank) {
        return switch (rank) {
            case 1 -> "\ud83e\udd47";
            case 2 -> "\ud83e\udd48";
            case 3 -> "\ud83e\udd49";
            default -> "#" + rank;
        };
    }

    private int getRankColor(int rank) {
        return switch (rank) {
            case 1 -> 0xFFD700; // Or
            case 2 -> 0xC0C0C0; // Argent
            case 3 -> 0xCD7F32; // Bronze
            default -> 0xAAAAAA;
        };
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

        // Si les donnees viennent d'etre chargees, reconstruire les boutons
        if (isLoading && ClientLeaderboardData.isNocoinLoaded()) {
            isLoading = false;
            rebuildButtons();
        }
    }
}

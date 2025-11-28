package com.zetsumei.nocoin.network;

import com.zetsumei.nocoin.Config;
import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.block.entity.GachaMachineBlockEntity;
import com.zetsumei.nocoin.block.entity.PlayerShopBlockEntity;
import com.zetsumei.nocoin.capability.NocoinCapabilityProvider;
import com.zetsumei.nocoin.gacha.GachaHistory;
import com.zetsumei.nocoin.gacha.GachaHistoryManager;
import com.zetsumei.nocoin.gacha.GachaManager;
import com.zetsumei.nocoin.gacha.GachaReward;
import com.zetsumei.nocoin.leaderboard.LeaderboardEntry;
import com.zetsumei.nocoin.leaderboard.LeaderboardManager;
import com.zetsumei.nocoin.network.gacha.*;
import com.zetsumei.nocoin.network.player.*;
import com.zetsumei.nocoin.shop.ShopManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Gestionnaire réseau pour la synchronisation des données NOCOIN.
 */
public class NocoinNetworkHandler {

    private static final String PROTOCOL_VERSION = "4";

    public static final SimpleChannel CHANNEL =
        NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Nocoin.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
        );

    private static int packetId = 0;

    /**
     * Enregistre tous les paquets réseau.
     */
    public static void register() {
        // Paquets existants pour le solde
        CHANNEL.registerMessage(
            packetId++,
            SyncBalancePacket.class,
            SyncBalancePacket::encode,
            SyncBalancePacket::decode,
            SyncBalancePacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            RequestBalancePacket.class,
            RequestBalancePacket::encode,
            RequestBalancePacket::decode,
            RequestBalancePacket::handle
        );

        // Nouveaux paquets pour la boutique
        CHANNEL.registerMessage(
            packetId++,
            ShopItemsPacket.class,
            ShopItemsPacket::encode,
            ShopItemsPacket::decode,
            ShopItemsPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            RequestShopItemsPacket.class,
            RequestShopItemsPacket::encode,
            RequestShopItemsPacket::decode,
            RequestShopItemsPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            PurchasePacket.class,
            PurchasePacket::encode,
            PurchasePacket::decode,
            PurchasePacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            PurchaseResultPacket.class,
            PurchaseResultPacket::encode,
            PurchaseResultPacket::decode,
            PurchaseResultPacket::handle
        );

        // Paquets pour le vendeur Gacha
        CHANNEL.registerMessage(
            packetId++,
            OpenVendorScreenPacket.class,
            OpenVendorScreenPacket::encode,
            OpenVendorScreenPacket::decode,
            OpenVendorScreenPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            BuyGachaKeyPacket.class,
            BuyGachaKeyPacket::encode,
            BuyGachaKeyPacket::decode,
            BuyGachaKeyPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            GachaKeyPurchaseResultPacket.class,
            GachaKeyPurchaseResultPacket::encode,
            GachaKeyPurchaseResultPacket::decode,
            GachaKeyPurchaseResultPacket::handle
        );

        // Paquets pour la machine à Gacha
        CHANNEL.registerMessage(
            packetId++,
            OpenGachaMachinePacket.class,
            OpenGachaMachinePacket::encode,
            OpenGachaMachinePacket::decode,
            OpenGachaMachinePacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            GachaPullPacket.class,
            GachaPullPacket::encode,
            GachaPullPacket::decode,
            GachaPullPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            GachaPullResultPacket.class,
            GachaPullResultPacket::encode,
            GachaPullResultPacket::decode,
            GachaPullResultPacket::handle
        );

        // Paquets pour le magasin joueur
        CHANNEL.registerMessage(
            packetId++,
            OpenPlayerShopOwnerPacket.class,
            OpenPlayerShopOwnerPacket::encode,
            OpenPlayerShopOwnerPacket::decode,
            OpenPlayerShopOwnerPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            OpenPlayerShopCustomerPacket.class,
            OpenPlayerShopCustomerPacket::encode,
            OpenPlayerShopCustomerPacket::decode,
            OpenPlayerShopCustomerPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            AddShopOfferPacket.class,
            AddShopOfferPacket::encode,
            AddShopOfferPacket::decode,
            AddShopOfferPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            RemoveShopOfferPacket.class,
            RemoveShopOfferPacket::encode,
            RemoveShopOfferPacket::decode,
            RemoveShopOfferPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            UpdateShopNamePacket.class,
            UpdateShopNamePacket::encode,
            UpdateShopNamePacket::decode,
            UpdateShopNamePacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            PlayerShopTransactionPacket.class,
            PlayerShopTransactionPacket::encode,
            PlayerShopTransactionPacket::decode,
            PlayerShopTransactionPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            PlayerShopTransactionResultPacket.class,
            PlayerShopTransactionResultPacket::encode,
            PlayerShopTransactionResultPacket::decode,
            PlayerShopTransactionResultPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            UpdateOfferStockPacket.class,
            UpdateOfferStockPacket::encode,
            UpdateOfferStockPacket::decode,
            UpdateOfferStockPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            UpdateOfferPacket.class,
            UpdateOfferPacket::encode,
            UpdateOfferPacket::decode,
            UpdateOfferPacket::handle
        );

        // Paquets pour le classement
        CHANNEL.registerMessage(
            packetId++,
            RequestLeaderboardPacket.class,
            RequestLeaderboardPacket::encode,
            RequestLeaderboardPacket::decode,
            RequestLeaderboardPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            LeaderboardDataPacket.class,
            LeaderboardDataPacket::encode,
            LeaderboardDataPacket::decode,
            LeaderboardDataPacket::handle
        );

        // Paquets pour le catalogue et l'historique du Gacha
        CHANNEL.registerMessage(
            packetId++,
            RequestGachaCatalogPacket.class,
            RequestGachaCatalogPacket::encode,
            RequestGachaCatalogPacket::decode,
            RequestGachaCatalogPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            GachaCatalogPacket.class,
            GachaCatalogPacket::encode,
            GachaCatalogPacket::decode,
            GachaCatalogPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            RequestGachaHistoryPacket.class,
            RequestGachaHistoryPacket::encode,
            RequestGachaHistoryPacket::decode,
            RequestGachaHistoryPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            GachaHistoryPacket.class,
            GachaHistoryPacket::encode,
            GachaHistoryPacket::decode,
            GachaHistoryPacket::handle
        );

        // Paquets pour le multi-tirage
        CHANNEL.registerMessage(
            packetId++,
            GachaMultiPullPacket.class,
            GachaMultiPullPacket::encode,
            GachaMultiPullPacket::decode,
            GachaMultiPullPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            GachaMultiPullResultPacket.class,
            GachaMultiPullResultPacket::encode,
            GachaMultiPullResultPacket::decode,
            GachaMultiPullResultPacket::handle
        );

        // Paquets pour l'administration du Gacha
        CHANNEL.registerMessage(
            packetId++,
            OpenGachaAdminPacket.class,
            OpenGachaAdminPacket::encode,
            OpenGachaAdminPacket::decode,
            OpenGachaAdminPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            GachaAdminAddRewardPacket.class,
            GachaAdminAddRewardPacket::encode,
            GachaAdminAddRewardPacket::decode,
            GachaAdminAddRewardPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            GachaAdminRemoveRewardPacket.class,
            GachaAdminRemoveRewardPacket::encode,
            GachaAdminRemoveRewardPacket::decode,
            GachaAdminRemoveRewardPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            GachaAdminModifyRewardPacket.class,
            GachaAdminModifyRewardPacket::encode,
            GachaAdminModifyRewardPacket::decode,
            GachaAdminModifyRewardPacket::handle
        );

        CHANNEL.registerMessage(
            packetId++,
            GachaAdminSetRatesPacket.class,
            GachaAdminSetRatesPacket::encode,
            GachaAdminSetRatesPacket::decode,
            GachaAdminSetRatesPacket::handle
        );
    }

    /**
     * Envoie le solde au client.
     * @param player le joueur destinataire
     * @param balance le solde à envoyer
     */
    public static void sendBalanceToClient(ServerPlayer player, long balance) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new SyncBalancePacket(balance)
        );
    }

    /**
     * Demande le solde au serveur (appelé depuis le client).
     */
    public static void requestBalanceFromServer() {
        CHANNEL.sendToServer(new RequestBalancePacket());
    }

    /**
     * Envoie la liste des articles de boutique au client.
     * @param player le joueur destinataire
     */
    public static void sendShopItemsToClient(ServerPlayer player) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new ShopItemsPacket(ShopManager.getInstance().getItems())
        );
    }

    /**
     * Demande la liste des articles de boutique au serveur (appelé depuis le client).
     */
    public static void requestShopItemsFromServer() {
        CHANNEL.sendToServer(new RequestShopItemsPacket());
    }

    /**
     * Envoie une demande d'achat au serveur (appelé depuis le client).
     * @param shopItemId l'ID de l'article à acheter
     */
    public static void sendPurchaseRequest(int shopItemId) {
        CHANNEL.sendToServer(new PurchasePacket(shopItemId));
    }

    /**
     * Envoie le résultat d'un achat au client.
     * @param player le joueur destinataire
     * @param success si l'achat a réussi
     * @param newBalance le nouveau solde après l'achat
     */
    public static void sendPurchaseResultToClient(
        ServerPlayer player,
        boolean success,
        long newBalance
    ) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new PurchaseResultPacket(success, newBalance)
        );
    }

    // =============== Méthodes pour le vendeur Gacha ===============

    /**
     * Envoie l'ouverture de l'écran du vendeur au client.
     * @param player le joueur destinataire
     */
    public static void sendOpenVendorScreenToClient(ServerPlayer player) {
        player
            .getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY)
            .ifPresent(cap -> {
                CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenVendorScreenPacket(
                        cap.getBalance(),
                        Config.gachaKeyPrice
                    )
                );
            });
    }

    /**
     * Demande l'achat de Clés Gacha au serveur (appelé depuis le client).
     * @param quantity le nombre de clés à acheter
     */
    public static void sendBuyGachaKeyRequest(int quantity) {
        CHANNEL.sendToServer(new BuyGachaKeyPacket(quantity));
    }

    /**
     * Envoie le résultat d'un achat de Clé Gacha au client.
     * @param player le joueur destinataire
     * @param success si l'achat a réussi
     * @param newBalance le nouveau solde après l'achat
     * @param quantity le nombre de clés achetées
     */
    public static void sendGachaKeyPurchaseResult(
        ServerPlayer player,
        boolean success,
        long newBalance,
        int quantity
    ) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new GachaKeyPurchaseResultPacket(success, newBalance, quantity)
        );
    }

    // =============== Méthodes pour la Machine à Gacha ===============

    /**
     * Envoie l'ouverture de l'écran de la machine à Gacha au client.
     * @param player le joueur destinataire
     * @param machinePos la position de la machine
     * @param hasKey si le joueur a une clé
     * @param keyCount le nombre de clés du joueur
     */
    public static void sendOpenGachaMachineScreen(
        ServerPlayer player,
        BlockPos machinePos,
        boolean hasKey,
        int keyCount
    ) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new OpenGachaMachinePacket(machinePos, hasKey, keyCount)
        );
    }

    /**
     * Demande un tirage Gacha au serveur (appelé depuis le client).
     * @param machinePos la position de la machine
     */
    public static void sendGachaPullRequest(BlockPos machinePos) {
        CHANNEL.sendToServer(new GachaPullPacket(machinePos));
    }

    /**
     * Envoie le résultat d'un tirage Gacha au client.
     * @param player le joueur destinataire
     * @param success si le tirage a réussi
     * @param itemId l'ID de l'item obtenu
     * @param stars le nombre d'étoiles de la récompense
     * @param characterName le nom du personnage
     */
    public static void sendGachaPullResult(
        ServerPlayer player,
        boolean success,
        String itemId,
        int stars,
        String characterName
    ) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new GachaPullResultPacket(success, itemId, stars, characterName)
        );
    }

    // =============== Méthodes pour le Magasin Joueur ===============

    /**
     * Envoie l'ouverture de l'écran propriétaire du magasin joueur au client.
     * @param player le joueur destinataire
     * @param shopPos la position du magasin
     * @param shopEntity l'entité du magasin
     */
    public static void sendOpenPlayerShopOwnerScreen(
        ServerPlayer player,
        BlockPos shopPos,
        PlayerShopBlockEntity shopEntity
    ) {
        player
            .getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY)
            .ifPresent(cap -> {
                CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenPlayerShopOwnerPacket(
                        shopPos,
                        shopEntity.getShopName(),
                        shopEntity.getOwnerName(),
                        shopEntity.getOffers(),
                        cap.getBalance()
                    )
                );
            });
    }

    /**
     * Envoie l'ouverture de l'écran client du magasin joueur au client.
     * @param player le joueur destinataire
     * @param shopPos la position du magasin
     * @param shopEntity l'entité du magasin
     */
    public static void sendOpenPlayerShopCustomerScreen(
        ServerPlayer player,
        BlockPos shopPos,
        PlayerShopBlockEntity shopEntity
    ) {
        player
            .getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY)
            .ifPresent(cap -> {
                CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new OpenPlayerShopCustomerPacket(
                        shopPos,
                        shopEntity.getShopName(),
                        shopEntity.getOwnerName(),
                        shopEntity.getSellOffers(),
                        shopEntity.getBuyOffers(),
                        cap.getBalance()
                    )
                );
            });
    }

    /**
     * Envoie une demande d'ajout d'offre au serveur (appelé depuis le client).
     * @param shopPos la position du magasin
     * @param type le type d'offre (SELL/BUY)
     * @param itemId l'ID de l'item
     * @param quantity la quantité
     * @param pricePerUnit le prix par unité
     * @param stock le stock initial
     */
    public static void sendAddShopOffer(
        BlockPos shopPos,
        com.zetsumei.nocoin.shop.player.ShopOffer.OfferType type,
        String itemId,
        int quantity,
        long pricePerUnit,
        int stock
    ) {
        CHANNEL.sendToServer(
            new AddShopOfferPacket(
                shopPos,
                type,
                itemId,
                quantity,
                pricePerUnit,
                stock
            )
        );
    }

    /**
     * Envoie une demande de suppression d'offre au serveur (appelé depuis le client).
     * @param shopPos la position du magasin
     * @param offerId l'ID de l'offre
     */
    public static void sendRemoveShopOffer(
        BlockPos shopPos,
        java.util.UUID offerId
    ) {
        CHANNEL.sendToServer(new RemoveShopOfferPacket(shopPos, offerId));
    }

    /**
     * Envoie une demande de mise à jour du nom du magasin au serveur (appelé depuis le client).
     * @param shopPos la position du magasin
     * @param newName le nouveau nom
     */
    public static void sendUpdateShopName(BlockPos shopPos, String newName) {
        CHANNEL.sendToServer(new UpdateShopNamePacket(shopPos, newName));
    }

    /**
     * Envoie une demande de transaction au serveur (appelé depuis le client).
     * @param shopPos la position du magasin
     * @param offerId l'ID de l'offre
     * @param isBuying true si achat, false si vente
     */
    public static void sendPlayerShopTransaction(
        BlockPos shopPos,
        java.util.UUID offerId,
        boolean isBuying
    ) {
        PlayerShopTransactionPacket.TransactionType type = isBuying
            ? PlayerShopTransactionPacket.TransactionType.BUY
            : PlayerShopTransactionPacket.TransactionType.SELL;
        CHANNEL.sendToServer(
            new PlayerShopTransactionPacket(shopPos, offerId, type)
        );
    }

    /**
     * Envoie le résultat d'une transaction au client.
     * @param player le joueur destinataire
     * @param success si la transaction a réussi
     * @param status le statut de la transaction
     * @param amountTransferred le montant transféré
     */
    public static void sendPlayerShopTransactionResult(
        ServerPlayer player,
        boolean success,
        PlayerShopBlockEntity.TransactionResult.Status status,
        long amountTransferred
    ) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new PlayerShopTransactionResultPacket(
                success,
                status,
                amountTransferred
            )
        );
    }

    /**
     * Envoie une demande de mise à jour du stock d'une offre au serveur (appelé depuis le client).
     * @param shopPos la position du magasin
     * @param offerId l'ID de l'offre
     * @param action l'action (ADD ou REMOVE)
     * @param amount la quantité à ajouter/retirer
     */
    public static void sendUpdateOfferStock(
        BlockPos shopPos,
        java.util.UUID offerId,
        UpdateOfferStockPacket.Action action,
        int amount
    ) {
        CHANNEL.sendToServer(
            new UpdateOfferStockPacket(shopPos, offerId, action, amount)
        );
    }

    /**
     * Envoie une demande de modification d'une offre au serveur (appelé depuis le client).
     * @param shopPos la position du magasin
     * @param offerId l'ID de l'offre à modifier
     * @param newPricePerUnit le nouveau prix par unité
     * @param newQuantity la nouvelle quantité
     * @param active si l'offre est active
     */
    public static void sendUpdateOffer(
        BlockPos shopPos,
        java.util.UUID offerId,
        long newPricePerUnit,
        int newQuantity,
        boolean active
    ) {
        CHANNEL.sendToServer(
            new UpdateOfferPacket(
                shopPos,
                offerId,
                newPricePerUnit,
                newQuantity,
                active
            )
        );
    }

    // =============== Méthodes pour le Classement ===============

    /**
     * Demande les données du classement au serveur (appelé depuis le client).
     * @param type le type de classement
     */
    public static void requestLeaderboardFromServer(
        LeaderboardManager.LeaderboardType type
    ) {
        CHANNEL.sendToServer(new RequestLeaderboardPacket(type));
    }

    /**
     * Envoie les données du classement au client.
     * @param player le joueur destinataire
     * @param type le type de classement
     */
    public static void sendLeaderboardToClient(
        ServerPlayer player,
        LeaderboardManager.LeaderboardType type
    ) {
        List<LeaderboardEntry> entries = LeaderboardManager.getLeaderboardByNocoin(
            player.getServer()
        );

        String playerName = player.getGameProfile().getName();
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new LeaderboardDataPacket(type, entries, playerName)
        );
    }

    /**
     * Demande et ouvre l'écran du classement pour un joueur (côté serveur).
     * Utilisé par le bloc Leaderboard quand un joueur clique dessus.
     * @param player le joueur qui a cliqué sur le bloc
     */
    public static void requestLeaderboardForPlayer(ServerPlayer player) {
        // Envoie les données NOCOIN par défaut, le client ouvrira l'écran
        sendLeaderboardToClient(
            player,
            LeaderboardManager.LeaderboardType.NOCOIN
        );
    }

    // =============== Méthodes pour le Catalogue et l'Historique Gacha ===============

    /**
     * Demande le catalogue gacha au serveur (appelé depuis le client).
     * @param machinePos la position de la machine gacha
     */
    public static void requestGachaCatalog(BlockPos machinePos) {
        CHANNEL.sendToServer(new RequestGachaCatalogPacket(machinePos));
    }

    /**
     * Envoie le catalogue gacha au client.
     * @param player le joueur destinataire
     * @param machinePos la position de la machine gacha
     */
    public static void sendGachaCatalogToClient(ServerPlayer player, BlockPos machinePos) {
        Level level = player.level();
        BlockEntity be = level.getBlockEntity(machinePos);
        if (!(be instanceof GachaMachineBlockEntity gachaBE)) {
            return;
        }

        List<GachaReward> rewards = gachaBE.getRewards();
        List<GachaCatalogPacket.CatalogEntry> entries = new ArrayList<>();

        for (GachaReward reward : rewards) {
            double effectiveChance = calculateEffectiveChanceForMachine(reward, rewards, gachaBE);
            entries.add(new GachaCatalogPacket.CatalogEntry(
                reward.getItemId(),
                reward.getDisplayName(),
                reward.getRarity(),
                reward.getWeight(),
                effectiveChance
            ));
        }

        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new GachaCatalogPacket(entries, 
                gachaBE.getFiveStarRate(),
                gachaBE.getFourStarRate(),
                gachaBE.getThreeStarRate())
        );
    }

    /**
     * Calcule la probabilité effective d'une récompense (méthode globale, legacy).
     */
    private static double calculateEffectiveChance(GachaReward reward, List<GachaReward> allRewards) {
        double rarityRate = switch (reward.getRarity()) {
            case FIVE_STAR -> GachaManager.getFiveStarRate();
            case FOUR_STAR -> GachaManager.getFourStarRate();
            case THREE_STAR -> GachaManager.getThreeStarRate();
        };

        double totalWeightInRarity = allRewards.stream()
            .filter(r -> r.getRarity() == reward.getRarity())
            .mapToDouble(GachaReward::getWeight)
            .sum();

        if (totalWeightInRarity == 0) return 0;
        return (reward.getWeight() / totalWeightInRarity) * rarityRate;
    }

    /**
     * Demande l'historique gacha au serveur (appelé depuis le client).
     * @param machinePos la position de la machine gacha
     */
    public static void requestGachaHistory(BlockPos machinePos) {
        CHANNEL.sendToServer(new RequestGachaHistoryPacket(machinePos));
    }

    /**
     * Envoie l'historique gacha au client.
     * @param player le joueur destinataire
     */
    public static void sendGachaHistoryToClient(ServerPlayer player) {
        List<GachaHistory> histories = GachaHistoryManager.getInstance().getHistory(player.getUUID());
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new GachaHistoryPacket(histories)
        );
    }

    // =============== Méthodes pour le Multi-Tirage Gacha ===============

    /**
     * Demande un multi-tirage gacha au serveur (appelé depuis le client).
     * @param machinePos la position de la machine gacha
     * @param count le nombre de tirages (max 10)
     */
    public static void sendGachaMultiPullRequest(BlockPos machinePos, int count) {
        CHANNEL.sendToServer(new GachaMultiPullPacket(machinePos, count));
    }

    /**
     * Envoie le résultat d'un multi-tirage gacha au client.
     * @param player le joueur destinataire
     * @param success si le tirage a réussi
     * @param results les résultats des tirages
     */
    public static void sendGachaMultiPullResult(
        ServerPlayer player,
        boolean success,
        List<GachaMultiPullResultPacket.PullResult> results
    ) {
        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new GachaMultiPullResultPacket(success, results)
        );
    }

    // =============== Méthodes pour l'Administration du Gacha ===============

    /**
     * Ouvre l'écran d'administration du gacha pour un joueur (côté serveur).
     * @param player le joueur admin
     * @param machinePos la position de la machine gacha
     */
    public static void sendOpenGachaAdminScreen(ServerPlayer player, BlockPos machinePos) {
        if (!player.hasPermissions(2)) {
            return;
        }

        Level level = player.level();
        BlockEntity be = level.getBlockEntity(machinePos);
        if (!(be instanceof GachaMachineBlockEntity gachaBE)) {
            return;
        }

        List<GachaReward> rewards = gachaBE.getRewards();
        List<GachaCatalogPacket.CatalogEntry> entries = new ArrayList<>();

        for (GachaReward reward : rewards) {
            double effectiveChance = calculateEffectiveChanceForMachine(reward, rewards, gachaBE);
            entries.add(new GachaCatalogPacket.CatalogEntry(
                reward.getItemId(),
                reward.getDisplayName(),
                reward.getRarity(),
                reward.getWeight(),
                effectiveChance
            ));
        }

        CHANNEL.send(
            PacketDistributor.PLAYER.with(() -> player),
            new OpenGachaAdminPacket(machinePos, entries,
                gachaBE.getFiveStarRate(),
                gachaBE.getFourStarRate(),
                gachaBE.getThreeStarRate())
        );
    }

    /**
     * Calcule la chance effective d'une récompense pour une machine spécifique.
     */
    private static double calculateEffectiveChanceForMachine(GachaReward reward, List<GachaReward> allRewards, GachaMachineBlockEntity machine) {
        double rarityRate = switch (reward.getRarity()) {
            case FIVE_STAR -> machine.getFiveStarRate();
            case FOUR_STAR -> machine.getFourStarRate();
            case THREE_STAR -> machine.getThreeStarRate();
        };

        double totalWeightForRarity = allRewards.stream()
            .filter(r -> r.getRarity() == reward.getRarity())
            .mapToDouble(GachaReward::getWeight)
            .sum();

        if (totalWeightForRarity == 0) return 0;

        return (rarityRate / 100.0) * (reward.getWeight() / totalWeightForRarity) * 100.0;
    }

    /**
     * Envoie une demande d'ajout de récompense gacha (appelé depuis le client admin).
     * @param machinePos la position de la machine gacha
     */
    public static void sendGachaAdminAddReward(BlockPos machinePos, String itemId, com.zetsumei.nocoin.gacha.GachaRarity rarity, String displayName, double weight) {
        CHANNEL.sendToServer(new GachaAdminAddRewardPacket(machinePos, itemId, rarity, displayName, weight));
    }

    /**
     * Envoie une demande de suppression de récompense gacha (appelé depuis le client admin).
     * @param machinePos la position de la machine gacha
     */
    public static void sendGachaAdminRemoveReward(BlockPos machinePos, String itemId) {
        CHANNEL.sendToServer(new GachaAdminRemoveRewardPacket(machinePos, itemId));
    }

    /**
     * Envoie une demande de modification du poids d'une récompense (appelé depuis le client admin).
     * @param machinePos la position de la machine gacha
     */
    public static void sendGachaAdminModifyWeight(BlockPos machinePos, String itemId, double newWeight) {
        CHANNEL.sendToServer(new GachaAdminModifyRewardPacket(machinePos, itemId, newWeight));
    }

    /**
     * Envoie une demande de modification de la rareté d'une récompense (appelé depuis le client admin).
     * @param machinePos la position de la machine gacha
     */
    public static void sendGachaAdminModifyRarity(BlockPos machinePos, String itemId, com.zetsumei.nocoin.gacha.GachaRarity newRarity) {
        CHANNEL.sendToServer(new GachaAdminModifyRewardPacket(machinePos, itemId, newRarity));
    }

    /**
     * Envoie une demande de modification des probabilités de rareté (appelé depuis le client admin).
     * @param machinePos la position de la machine gacha
     */
    public static void sendGachaAdminSetRates(BlockPos machinePos, double fiveStarRate, double fourStarRate, double threeStarRate) {
        CHANNEL.sendToServer(new GachaAdminSetRatesPacket(machinePos, fiveStarRate, fourStarRate, threeStarRate));
    }
}

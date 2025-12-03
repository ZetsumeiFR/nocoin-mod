package com.zetsumei.nocoin.event;

import com.mojang.logging.LogUtils;
import com.zetsumei.nocoin.Config;
import com.zetsumei.nocoin.Nocoin;
import com.zetsumei.nocoin.capability.NocoinCapabilityProvider;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.*;
import java.util.Collections;

/**
 * Gestionnaire d'événements pour le système de Sign Shops.
 *
 * Ce système permet aux joueurs de créer des boutiques en utilisant des panneaux muraux
 * placés sur des coffres ou autres conteneurs.
 *
 * Types de boutiques:
 * - [buy]: Le joueur achète à un autre joueur
 * - [sell]: Le joueur vend à un autre joueur
 * - [server-buy]: Le joueur achète au serveur (admin uniquement)
 * - [server-sell]: Le joueur vend au serveur (admin uniquement)
 *
 * Format du panneau:
 * Ligne 1: [buy] ou [sell] ou [server-buy] ou [server-sell]
 * Ligne 2: Nom de l'item (ex: Diamond, minecraft:diamond)
 * Ligne 3: Quantité (ex: 64)
 * Ligne 4: Prix (ex: 100)
 */
@Mod.EventBusSubscriber(modid = Nocoin.MODID)
public class SignShopEventHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    // Clés NBT pour les données du panneau shop
    public static final String IS_SHOP = "nocoin-is-shop";
    public static final String ACTIVATED = "nocoin-shop-activated";
    public static final String OWNER = "nocoin-owner";
    public static final String ITEMS = "nocoin-items";
    public static final String TYPE = "nocoin-shop-type";
    public static final String PRICE = "nocoin-price";
    public static final String QUANTITY = "nocoin-quantity";
    public static final String ITEM_ID = "nocoin-item-id";

    /**
     * Empêche le placement de blocs adjacents à un sign shop.
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || event.isCanceled()) return;

        boolean shouldCancel = Arrays.stream(Direction.values()).anyMatch(direction -> {
            BlockEntity be = event.getLevel().getBlockEntity(event.getPos().relative(direction));
            return be != null && be.getPersistentData().contains(IS_SHOP);
        });

        if (shouldCancel) {
            if (event.getEntity() instanceof Player player) {
                player.sendSystemMessage(Component.literal("Vous ne pouvez pas placer de bloc à côté d'un shop")
                        .withStyle(ChatFormatting.RED));
            }
            event.setCanceled(true);
        }
    }

    /**
     * Gère la destruction d'un sign shop.
     */
    @SubscribeEvent
    public static void onShopBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        BlockState state = event.getLevel().getBlockState(event.getPos());

        // Vérifier si c'est un panneau mural avec données shop
        if (state.getBlock() instanceof WallSignBlock) {
            BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
            if (!(be instanceof SignBlockEntity tile)) return;

            CompoundTag nbt = tile.getPersistentData();
            if (!nbt.isEmpty() && nbt.contains(ACTIVATED)) {
                Player player = event.getPlayer();
                boolean hasAdminPerms = player.hasPermissions(Config.getSignShopAdminLevel());

                // Vérifier si le joueur est le propriétaire ou admin
                if (nbt.hasUUID(OWNER) && !nbt.getUUID(OWNER).equals(player.getUUID())) {
                    if (!hasAdminPerms) {
                        player.sendSystemMessage(Component.literal("Vous n'êtes pas le propriétaire de ce shop")
                                .withStyle(ChatFormatting.RED));
                        event.setCanceled(true);
                        return;
                    }
                }

                // Retirer le marqueur IS_SHOP du coffre arrière
                BlockPos backBlock = getBackBlockPos(event.getPos(), state);
                BlockEntity backBe = event.getLevel().getBlockEntity(backBlock);
                if (backBe != null) {
                    backBe.getPersistentData().remove(IS_SHOP);
                }

                LOGGER.info("Sign shop détruit par {} à {}", player.getName().getString(), event.getPos());
            }
        }
        // Vérifier si c'est un conteneur marqué comme shop
        else {
            BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
            if (be != null && be.getPersistentData().contains(IS_SHOP)) {
                Player player = event.getPlayer();
                CompoundTag storageNbt = be.getPersistentData();
                boolean hasAdminPerms = player.hasPermissions(Config.getSignShopAdminLevel());

                // Chercher l'UUID du propriétaire - d'abord sur ce bloc, sinon sur un panneau shop adjacent
                UUID ownerUUID = findShopOwner((Level) event.getLevel(), event.getPos(), storageNbt);
                boolean isOwner = ownerUUID != null && ownerUUID.equals(player.getUUID());

                if (!isOwner && !hasAdminPerms) {
                    player.sendSystemMessage(Component.literal("Vous ne pouvez pas détruire le conteneur d'un shop")
                            .withStyle(ChatFormatting.RED));
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Empêche l'ouverture d'un conteneur marqué comme shop par un non-propriétaire.
     */
    @SubscribeEvent
    public static void onStorageOpen(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;

        BlockEntity invTile = event.getLevel().getBlockEntity(event.getPos());
        if (invTile == null) return;

        // Vérifier si le conteneur a un item handler
        var itemHandlerCap = invTile.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (!itemHandlerCap.isPresent()) return;

        CompoundTag nbt = invTile.getPersistentData();
        if (nbt.contains(IS_SHOP)) {
            // Chercher l'UUID du propriétaire - d'abord sur ce bloc, sinon sur un panneau shop adjacent
            UUID ownerUUID = findShopOwner(event.getLevel(), event.getPos(), nbt);

            if (ownerUUID == null || !ownerUUID.equals(event.getEntity().getUUID())) {
                if (!event.getEntity().hasPermissions(Config.getSignShopAdminLevel())) {
                    event.getEntity().sendSystemMessage(Component.literal("Vous n'êtes pas le propriétaire de ce shop")
                            .withStyle(ChatFormatting.RED));
                    event.setCanceled(true);
                }
            }
        }
    }

    /**
     * Cherche l'UUID du propriétaire d'un shop.
     * Vérifie d'abord le NBT du conteneur, puis les panneaux shop adjacents.
     */
    private static UUID findShopOwner(Level level, BlockPos containerPos, CompoundTag containerNbt) {
        // D'abord, vérifier si l'OWNER est sur le conteneur lui-même
        if (containerNbt.hasUUID(OWNER)) {
            return containerNbt.getUUID(OWNER);
        }

        // Sinon, chercher un panneau shop adjacent qui pointe vers ce conteneur
        for (Direction dir : Direction.values()) {
            BlockPos adjacentPos = containerPos.relative(dir);
            BlockState adjacentState = level.getBlockState(adjacentPos);

            // Vérifier si c'est un panneau mural
            if (adjacentState.getBlock() instanceof WallSignBlock) {
                // Vérifier si le panneau pointe vers notre conteneur
                Direction facing = adjacentState.getValue(WallSignBlock.FACING);
                BlockPos backPos = adjacentPos.relative(facing.getOpposite());

                if (backPos.equals(containerPos)) {
                    BlockEntity signTile = level.getBlockEntity(adjacentPos);
                    if (signTile instanceof SignBlockEntity) {
                        CompoundTag signNbt = signTile.getPersistentData();
                        if (signNbt.contains(ACTIVATED) && signNbt.hasUUID(OWNER)) {
                            return signNbt.getUUID(OWNER);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Gère le clic gauche sur un sign shop pour afficher les infos.
     */
    @SubscribeEvent
    public static void onSignLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof WallSignBlock)) return;

        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (!(be instanceof SignBlockEntity tile)) return;

        CompoundTag nbt = tile.getPersistentData();
        if (nbt.contains(ACTIVATED)) {
            showSaleInfo(nbt, event.getEntity(), event.getLevel());
            event.setCanceled(true);
        }
    }

    /**
     * Gère le clic droit sur un sign shop pour activer ou effectuer une transaction.
     */
    @SubscribeEvent
    public static void onSignRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;

        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof WallSignBlock)) return;

        // Trouver le bloc derrière le panneau
        BlockPos backBlockPos = getBackBlockPos(event.getPos(), state);
        BlockEntity invTile = event.getLevel().getBlockEntity(backBlockPos);
        if (invTile == null) return;

        // Vérifier que c'est bien un conteneur
        var itemHandlerCap = invTile.getCapability(ForgeCapabilities.ITEM_HANDLER);
        if (!itemHandlerCap.isPresent()) return;

        BlockEntity be = event.getLevel().getBlockEntity(event.getPos());
        if (!(be instanceof SignBlockEntity tile)) return;

        CompoundTag nbt = tile.getPersistentData();

        if (!nbt.contains(ACTIVATED)) {
            // Tenter d'activer le shop
            if (activateShop(invTile, tile, event.getLevel(), event.getPos(), event.getEntity())) {
                event.setCanceled(true);
            }
        } else {
            // Effectuer une transaction
            processTransaction(invTile, tile, event.getEntity());
            event.setCanceled(true);
        }
    }

    /**
     * Active un sign shop à partir d'un panneau configuré.
     * Format attendu:
     * - Ligne 1: [buy], [sell], [server-buy], [server-sell]
     * - Ligne 2: Nom de l'item (ex: Diamond, minecraft:diamond)
     * - Ligne 3: Quantité (ex: 64)
     * - Ligne 4: Prix (ex: 100)
     */
    private static boolean activateShop(BlockEntity storage, SignBlockEntity tile, Level level, BlockPos pos, Player player) {
        SignText frontText = tile.getFrontText();
        String actionLine = frontText.getMessage(0, true).getString().toLowerCase().trim();

        // Déterminer le type de shop
        String shopType = switch (actionLine) {
            case "[buy]" -> {
                if (!player.hasPermissions(Config.getSignShopPlayerLevel())) {
                    player.sendSystemMessage(Component.literal("Vous n'avez pas la permission de créer un shop")
                            .withStyle(ChatFormatting.RED));
                    yield null;
                }
                yield "buy";
            }
            case "[sell]" -> {
                if (!player.hasPermissions(Config.getSignShopPlayerLevel())) {
                    player.sendSystemMessage(Component.literal("Vous n'avez pas la permission de créer un shop")
                            .withStyle(ChatFormatting.RED));
                    yield null;
                }
                yield "sell";
            }
            case "[server-buy]" -> {
                if (!player.hasPermissions(Config.getSignShopAdminLevel())) {
                    player.sendSystemMessage(Component.literal("Seuls les administrateurs peuvent créer ce type de shop")
                            .withStyle(ChatFormatting.RED));
                    yield null;
                }
                yield "server-buy";
            }
            case "[server-sell]" -> {
                if (!player.hasPermissions(Config.getSignShopAdminLevel())) {
                    player.sendSystemMessage(Component.literal("Seuls les administrateurs peuvent créer ce type de shop")
                            .withStyle(ChatFormatting.RED));
                    yield null;
                }
                yield "server-sell";
            }
            default -> null;
        };

        if (shopType == null) return false;

        // Parser le nom de l'item (ligne 2)
        String itemName = frontText.getMessage(1, true).getString().trim();
        if (itemName.isEmpty()) {
            player.sendSystemMessage(Component.literal("Veuillez spécifier le nom de l'item sur la ligne 2")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        // Trouver l'item correspondant
        Item item = findItemByName(itemName);
        if (item == null) {
            player.sendSystemMessage(Component.literal("Item invalide : " + itemName)
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        // Parser la quantité (ligne 3)
        int quantity;
        try {
            String quantityLine = frontText.getMessage(2, true).getString().trim();
            quantity = Integer.parseInt(quantityLine);
            if (quantity <= 0 || quantity > 64 * 27) { // Max d'un grand coffre
                player.sendSystemMessage(Component.literal("Quantité invalide")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("Quantité invalide")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        // Parser le prix (ligne 4)
        long price;
        try {
            String priceLine = frontText.getMessage(3, true).getString().trim();
            price = Math.abs(Long.parseLong(priceLine));
            if (price <= 0) {
                player.sendSystemMessage(Component.literal("Prix invalide")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("Prix invalide")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        // Vérifier que le conteneur est valide
        IItemHandler inv = storage.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (inv == null) return false;

        // Pour les shops [buy] et [server-buy], vérifier que l'item est présent dans le coffre en quantité suffisante
        if (shopType.equals("buy") || shopType.equals("server-buy")) {
            int available = countItemInContainer(inv, item);
            if (available < quantity) {
                player.sendSystemMessage(Component.literal("Stock insuffisant : " + quantity + " requis, " + available + " disponibles")
                        .withStyle(ChatFormatting.RED));
                return false;
            }
        }

        // Créer l'ItemStack de référence
        ItemStack referenceStack = new ItemStack(item, quantity);

        // Sauvegarder les données sur le panneau
        CompoundTag nbt = tile.getPersistentData();
        nbt.putBoolean(ACTIVATED, true);
        nbt.putString(TYPE, shopType);
        nbt.putLong(PRICE, price);
        nbt.putInt(QUANTITY, quantity);
        nbt.putString(ITEM_ID, ForgeRegistries.ITEMS.getKey(item).toString());
        nbt.putUUID(OWNER, player.getUUID());

        // Sérialiser l'item de référence (avec la quantité spécifiée)
        ListTag itemsList = new ListTag();
        itemsList.add(referenceStack.save(new CompoundTag()));
        nbt.put(ITEMS, itemsList);

        // Mettre à jour le texte du panneau
        String displayItemName = referenceStack.getHoverName().getString();
        Component[] newText = new Component[]{
                Component.literal(actionLine.toUpperCase()).withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD),
                Component.literal(displayItemName).withStyle(ChatFormatting.WHITE),
                Component.literal("x" + quantity).withStyle(ChatFormatting.YELLOW),
                Component.literal(formatCurrency(price)).withStyle(ChatFormatting.GOLD)
        };

        tile.setText(new SignText(newText, newText, frontText.getColor(), frontText.hasGlowingText()), true);
        tile.setChanged();

        // Marquer le conteneur comme appartenant au shop
        CompoundTag storageNbt = storage.getPersistentData();
        storageNbt.putBoolean(IS_SHOP, true);
        storageNbt.putUUID(OWNER, player.getUUID());
        storage.setChanged();

        // Synchroniser
        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);

        player.sendSystemMessage(Component.literal("Shop " + shopType.toUpperCase() + " activé !")
                .withStyle(ChatFormatting.GREEN));
        player.sendSystemMessage(Component.literal(quantity + "x " + displayItemName + " pour " + formatCurrency(price))
                .withStyle(ChatFormatting.GRAY));

        LOGGER.info("Sign shop activé par {} à {} (type: {}, item: {}, quantité: {}, prix: {})",
                player.getName().getString(), pos, shopType, displayItemName, quantity, price);

        return true;
    }

    /**
     * Trouve un item par son nom (ID Minecraft ou nom affiché).
     */
    private static Item findItemByName(String name) {
        // Essayer d'abord avec un ResourceLocation complet (ex: minecraft:diamond)
        if (name.contains(":")) {
            ResourceLocation rl = ResourceLocation.tryParse(name.toLowerCase());
            if (rl != null) {
                Item item = ForgeRegistries.ITEMS.getValue(rl);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    return item;
                }
            }
        }

        // Essayer avec le namespace minecraft par défaut
        ResourceLocation defaultRl = ResourceLocation.tryParse("minecraft:" + name.toLowerCase().replace(" ", "_"));
        if (defaultRl != null) {
            Item item = ForgeRegistries.ITEMS.getValue(defaultRl);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                return item;
            }
        }

        // Recherche par nom affiché (plus lent, mais plus user-friendly)
        String searchName = name.toLowerCase();
        for (Item item : ForgeRegistries.ITEMS) {
            ItemStack stack = new ItemStack(item);
            String displayName = stack.getHoverName().getString().toLowerCase();
            if (displayName.equals(searchName) || displayName.contains(searchName)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Compte la quantité d'un item dans un conteneur.
     */
    private static int countItemInContainer(IItemHandler inv, Item item) {
        int count = 0;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    /**
     * Affiche les informations sur le sign shop.
     */
    private static void showSaleInfo(CompoundTag nbt, Player player, Level level) {
        String type = nbt.getString(TYPE);
        long price = nbt.getLong(PRICE);

        // Reconstituer les items
        List<ItemStack> items = new ArrayList<>();
        ListTag itemsList = nbt.getList(ITEMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < itemsList.size(); i++) {
            ItemStack stack = ItemStack.of(itemsList.getCompound(i));
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        // Construire le message d'affichage
        Component itemsDisplay = buildItemsDisplay(items);

        boolean isBuy = type.equals("buy") || type.equals("server-buy");

        if (isBuy) {
            player.sendSystemMessage(Component.literal("Acheter ").append(itemsDisplay).append(Component.literal(" pour " + formatCurrency(price)))
                    .withStyle(ChatFormatting.YELLOW));
        } else {
            player.sendSystemMessage(Component.literal("Vendre pour " + formatCurrency(price) + " : ").append(itemsDisplay)
                    .withStyle(ChatFormatting.YELLOW));
        }

        // Afficher le propriétaire
        if (nbt.hasUUID(OWNER)) {
            UUID ownerUUID = nbt.getUUID(OWNER);
            String ownerName = level.getServer() != null
                    ? level.getServer().getProfileCache().get(ownerUUID).map(p -> p.getName()).orElse("Inconnu")
                    : "Inconnu";
            player.sendSystemMessage(Component.literal("Propriétaire : " + ownerName)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Traite une transaction sur le sign shop.
     */
    private static void processTransaction(BlockEntity storage, SignBlockEntity tile, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        CompoundTag nbt = tile.getPersistentData();
        String action = nbt.getString(TYPE);
        long price = nbt.getLong(PRICE);

        // Reconstituer les items à échanger
        List<ItemStack> transItems = new ArrayList<>();
        ListTag itemsList = nbt.getList(ITEMS, Tag.TAG_COMPOUND);

        // Consolider les items identiques
        Map<String, ItemStack> consolidated = new HashMap<>();
        for (int i = 0; i < itemsList.size(); i++) {
            ItemStack stack = ItemStack.of(itemsList.getCompound(i));
            if (stack.isEmpty()) continue;

            String key = getItemKey(stack);
            if (consolidated.containsKey(key)) {
                consolidated.get(key).grow(stack.getCount());
            } else {
                consolidated.put(key, stack.copy());
            }
        }
        transItems.addAll(consolidated.values());

        IItemHandler inv = storage.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
        if (inv == null) {
            player.sendSystemMessage(Component.literal("Erreur lors de la transaction").withStyle(ChatFormatting.RED));
            return;
        }

        switch (action) {
            case "buy" -> processBuyTransaction(serverPlayer, nbt, inv, transItems, price);
            case "sell" -> processSellTransaction(serverPlayer, nbt, inv, transItems, price);
            case "server-buy" -> processServerBuyTransaction(serverPlayer, nbt, price);
            case "server-sell" -> processServerSellTransaction(serverPlayer, nbt, price);
            default -> player.sendSystemMessage(Component.literal("Erreur lors de la transaction").withStyle(ChatFormatting.RED));
        }
    }

    /**
     * Transaction d'achat (joueur achète au propriétaire du shop).
     * Utilise la quantité définie sur le panneau, pas tout le stock.
     */
    private static void processBuyTransaction(ServerPlayer buyer, CompoundTag nbt, IItemHandler inv,
                                               List<ItemStack> transItems, long price) {
        // Récupérer l'item et la quantité définis sur le panneau
        int quantity = nbt.getInt(QUANTITY);
        String itemId = nbt.getString(ITEM_ID);
        
        // Créer l'ItemStack à transférer avec la quantité exacte
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) {
            buyer.sendSystemMessage(Component.literal("Erreur lors de la transaction").withStyle(ChatFormatting.RED));
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            buyer.sendSystemMessage(Component.literal("Erreur lors de la transaction").withStyle(ChatFormatting.RED));
            return;
        }
        
        ItemStack requiredStack = new ItemStack(item, quantity);
        List<ItemStack> itemsToTransfer = Collections.singletonList(requiredStack);
        
        // Vérifier le solde de l'acheteur
        buyer.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(buyerCap -> {
            if (!buyerCap.hasEnough(price)) {
                buyer.sendSystemMessage(Component.literal("Fonds insuffisants")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            // Vérifier le stock avec la quantité exacte
            Map<Integer, ItemStack> slotMap = checkAndReserveStock(inv, itemsToTransfer);
            if (slotMap == null) {
                buyer.sendSystemMessage(Component.literal("Stock épuisé")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            // Vérifier l'espace dans l'inventaire de l'acheteur
            if (!canAddItemsToInventory(buyer, itemsToTransfer)) {
                buyer.sendSystemMessage(Component.literal("Inventaire plein")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            // Effectuer la transaction
            buyerCap.removeBalance(price);
            NocoinNetworkHandler.sendBalanceToClient(buyer, buyerCap.getBalance());

            // Transférer l'argent au propriétaire
            UUID ownerUUID = nbt.getUUID(OWNER);
            creditOwner(buyer.getServer(), ownerUUID, price);

            // Donner les items à l'acheteur (seulement la quantité définie)
            for (Map.Entry<Integer, ItemStack> entry : slotMap.entrySet()) {
                ItemStack extracted = inv.extractItem(entry.getKey(), entry.getValue().getCount(), false);
                if (!buyer.getInventory().add(extracted)) {
                    buyer.drop(extracted, false);
                }
            }

            buyer.sendSystemMessage(Component.literal("Achat réussi : ").append(buildItemsDisplay(itemsToTransfer)).append(Component.literal(" pour " + formatCurrency(price)))
                    .withStyle(ChatFormatting.GREEN));

            LOGGER.info("Transaction BUY: {} a acheté {}x {} pour {} NC", 
                    buyer.getName().getString(), quantity, item.getDescription().getString(), price);
        });
    }

    /**
     * Transaction de vente (joueur vend au propriétaire du shop).
     * Utilise la quantité définie sur le panneau.
     */
    private static void processSellTransaction(ServerPlayer seller, CompoundTag nbt, IItemHandler inv,
                                                List<ItemStack> transItems, long price) {
        UUID ownerUUID = nbt.getUUID(OWNER);
        
        // Récupérer l'item et la quantité définis sur le panneau
        int quantity = nbt.getInt(QUANTITY);
        String itemId = nbt.getString(ITEM_ID);
        
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) {
            seller.sendSystemMessage(Component.literal("Erreur lors de la transaction").withStyle(ChatFormatting.RED));
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            seller.sendSystemMessage(Component.literal("Erreur lors de la transaction").withStyle(ChatFormatting.RED));
            return;
        }
        
        ItemStack requiredStack = new ItemStack(item, quantity);
        List<ItemStack> itemsToTransfer = Collections.singletonList(requiredStack);

        // Vérifier que le propriétaire a les fonds
        ServerPlayer owner = seller.getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner == null) {
            seller.sendSystemMessage(Component.literal("Le propriétaire n'est pas en ligne")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        final boolean[] success = {false};
        owner.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(ownerCap -> {
            if (!ownerCap.hasEnough(price)) {
                seller.sendSystemMessage(Component.literal("Le propriétaire n'a pas assez de fonds")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            // Vérifier que le vendeur a les items (quantité exacte)
            if (!hasItemsInInventory(seller, itemsToTransfer)) {
                seller.sendSystemMessage(Component.literal("Vous n'avez pas assez d'items")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            // Vérifier l'espace dans le conteneur
            if (!canAddItemsToContainer(inv, itemsToTransfer)) {
                seller.sendSystemMessage(Component.literal("Le shop est plein")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            // Effectuer la transaction
            seller.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(sellerCap -> {
                ownerCap.removeBalance(price);
                sellerCap.addBalance(price);

                NocoinNetworkHandler.sendBalanceToClient(seller, sellerCap.getBalance());
                NocoinNetworkHandler.sendBalanceToClient(owner, ownerCap.getBalance());

                // Retirer les items du vendeur et les ajouter au conteneur (quantité exacte)
                removeItemsFromInventory(seller, itemsToTransfer);
                addItemsToContainer(inv, itemsToTransfer);

                seller.sendSystemMessage(Component.literal("Vente réussie : " + formatCurrency(price) + " pour ").append(buildItemsDisplay(itemsToTransfer))
                        .withStyle(ChatFormatting.GREEN));

                owner.sendSystemMessage(Component.literal(seller.getName().getString() + " a vendu ").append(buildItemsDisplay(itemsToTransfer)).append(Component.literal(" pour " + formatCurrency(price)))
                        .withStyle(ChatFormatting.AQUA));

                success[0] = true;
            });
        });

        if (success[0]) {
            LOGGER.info("Transaction SELL: {} a vendu {}x {} pour {} NC à {}",
                    seller.getName().getString(), quantity, item.getDescription().getString(), price, owner.getName().getString());
        }
    }

    /**
     * Transaction d'achat serveur (joueur achète au serveur).
     * Le serveur a un stock infini, mais respecte la quantité définie.
     */
    private static void processServerBuyTransaction(ServerPlayer buyer, CompoundTag nbt, long price) {
        // Récupérer l'item et la quantité définis sur le panneau
        int quantity = nbt.getInt(QUANTITY);
        String itemId = nbt.getString(ITEM_ID);
        
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) {
            buyer.sendSystemMessage(Component.literal("Erreur lors de la transaction").withStyle(ChatFormatting.RED));
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            buyer.sendSystemMessage(Component.literal("Erreur lors de la transaction").withStyle(ChatFormatting.RED));
            return;
        }
        
        ItemStack itemToGive = new ItemStack(item, quantity);
        List<ItemStack> itemsToTransfer = Collections.singletonList(itemToGive);
        
        buyer.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(buyerCap -> {
            if (!buyerCap.hasEnough(price)) {
                buyer.sendSystemMessage(Component.literal("Fonds insuffisants")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            // Vérifier l'espace inventaire
            if (!canAddItemsToInventory(buyer, itemsToTransfer)) {
                buyer.sendSystemMessage(Component.literal("Inventaire plein")
                        .withStyle(ChatFormatting.RED));
                return;
            }

            // Effectuer la transaction (serveur = source infinie)
            buyerCap.removeBalance(price);
            NocoinNetworkHandler.sendBalanceToClient(buyer, buyerCap.getBalance());

            // Donner les items (quantité exacte définie sur le panneau)
            ItemStack toGive = itemToGive.copy();
            if (!buyer.getInventory().add(toGive)) {
                buyer.drop(toGive, false);
            }

            buyer.sendSystemMessage(Component.literal("Achat réussi : ").append(buildItemsDisplay(itemsToTransfer)).append(Component.literal(" pour " + formatCurrency(price)))
                    .withStyle(ChatFormatting.GREEN));

            LOGGER.info("Transaction SERVER-BUY: {} a acheté {}x {} pour {} NC au serveur",
                    buyer.getName().getString(), quantity, item.getDescription().getString(), price);
        });
    }

    /**
     * Transaction de vente serveur (joueur vend au serveur).
     * Le serveur accepte la quantité exacte définie sur le panneau.
     */
    private static void processServerSellTransaction(ServerPlayer seller, CompoundTag nbt, long price) {
        // Récupérer l'item et la quantité définis sur le panneau
        int quantity = nbt.getInt(QUANTITY);
        String itemId = nbt.getString(ITEM_ID);
        
        ResourceLocation rl = ResourceLocation.tryParse(itemId);
        if (rl == null) {
            seller.sendSystemMessage(Component.literal("Erreur lors de la transaction").withStyle(ChatFormatting.RED));
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(rl);
        if (item == null || item == net.minecraft.world.item.Items.AIR) {
            seller.sendSystemMessage(Component.literal("Erreur lors de la transaction").withStyle(ChatFormatting.RED));
            return;
        }
        
        ItemStack requiredStack = new ItemStack(item, quantity);
        List<ItemStack> itemsToTransfer = Collections.singletonList(requiredStack);
        
        // Vérifier que le vendeur a les items (quantité exacte)
        if (!hasItemsInInventory(seller, itemsToTransfer)) {
            seller.sendSystemMessage(Component.literal("Vous n'avez pas assez d'items")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        seller.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(sellerCap -> {
            // Retirer les items (quantité exacte) et créditer
            removeItemsFromInventory(seller, itemsToTransfer);
            sellerCap.addBalance(price);
            NocoinNetworkHandler.sendBalanceToClient(seller, sellerCap.getBalance());

            seller.sendSystemMessage(Component.literal("Vente réussie : " + formatCurrency(price) + " pour ").append(buildItemsDisplay(itemsToTransfer))
                    .withStyle(ChatFormatting.GREEN));

            LOGGER.info("Transaction SERVER-SELL: {} a vendu {}x {} pour {} NC au serveur",
                    seller.getName().getString(), quantity, item.getDescription().getString(), price);
        });
    }

    // ==================== Méthodes utilitaires ====================

    /**
     * Obtient la position du bloc derrière le panneau mural.
     */
    private static BlockPos getBackBlockPos(BlockPos signPos, BlockState signState) {
        Direction facing = signState.getValue(WallSignBlock.FACING);
        return signPos.relative(facing.getOpposite());
    }

    /**
     * Crédite le propriétaire du shop (s'il est en ligne).
     */
    private static void creditOwner(net.minecraft.server.MinecraftServer server, UUID ownerUUID, long amount) {
        ServerPlayer owner = server.getPlayerList().getPlayer(ownerUUID);
        if (owner != null) {
            owner.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
                cap.addBalance(amount);
                NocoinNetworkHandler.sendBalanceToClient(owner, cap.getBalance());
            });
        }
        // Note: Si hors ligne, l'argent est perdu. Une future amélioration pourrait stocker les gains en attente.
    }

    /**
     * Vérifie et réserve le stock pour un achat.
     */
    private static Map<Integer, ItemStack> checkAndReserveStock(IItemHandler inv, List<ItemStack> transItems) {
        Map<Integer, ItemStack> slotMap = new HashMap<>();

        for (ItemStack required : transItems) {
            int remaining = required.getCount();

            for (int i = 0; i < inv.getSlots() && remaining > 0; i++) {
                ItemStack inSlot = inv.getStackInSlot(i);
                if (ItemStack.isSameItemSameTags(inSlot, required)) {
                    int available = slotMap.containsKey(i)
                            ? inSlot.getCount() - slotMap.get(i).getCount()
                            : inSlot.getCount();
                    int toTake = Math.min(remaining, available);

                    if (toTake > 0) {
                        ItemStack reserved = required.copy();
                        reserved.setCount(toTake);
                        slotMap.merge(i, reserved, (old, add) -> {
                            old.grow(add.getCount());
                            return old;
                        });
                        remaining -= toTake;
                    }
                }
            }

            if (remaining > 0) {
                return null; // Stock insuffisant
            }
        }

        return slotMap;
    }

    /**
     * Vérifie si le joueur a les items dans son inventaire.
     */
    private static boolean hasItemsInInventory(Player player, List<ItemStack> items) {
        for (ItemStack required : items) {
            int count = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (ItemStack.isSameItemSameTags(stack, required)) {
                    count += stack.getCount();
                }
            }
            if (count < required.getCount()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retire les items de l'inventaire du joueur.
     */
    private static void removeItemsFromInventory(Player player, List<ItemStack> items) {
        for (ItemStack toRemove : items) {
            int remaining = toRemove.getCount();
            for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
                ItemStack stack = player.getInventory().items.get(i);
                if (ItemStack.isSameItemSameTags(stack, toRemove)) {
                    int remove = Math.min(remaining, stack.getCount());
                    stack.shrink(remove);
                    remaining -= remove;
                }
            }
        }
    }

    /**
     * Vérifie si les items peuvent être ajoutés à l'inventaire du joueur.
     */
    private static boolean canAddItemsToInventory(Player player, List<ItemStack> items) {
        // Simuler l'ajout
        int emptySlots = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.isEmpty()) emptySlots++;
        }
        return emptySlots >= items.size(); // Simplification
    }

    /**
     * Vérifie si les items peuvent être ajoutés au conteneur.
     */
    private static boolean canAddItemsToContainer(IItemHandler inv, List<ItemStack> items) {
        for (ItemStack stack : items) {
            ItemStack remaining = stack.copy();
            for (int i = 0; i < inv.getSlots() && !remaining.isEmpty(); i++) {
                remaining = inv.insertItem(i, remaining, true);
            }
            if (!remaining.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Ajoute les items au conteneur.
     */
    private static void addItemsToContainer(IItemHandler inv, List<ItemStack> items) {
        for (ItemStack stack : items) {
            ItemStack remaining = stack.copy();
            for (int i = 0; i < inv.getSlots() && !remaining.isEmpty(); i++) {
                remaining = inv.insertItem(i, remaining, false);
            }
        }
    }

    /**
     * Génère une clé unique pour identifier un type d'item.
     */
    private static String getItemKey(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id != null ? id.toString() : "unknown";
    }

    /**
     * Construit l'affichage des items.
     */
    private static Component buildItemsDisplay(List<ItemStack> items) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ItemStack stack : items) {
            if (!first) sb.append(", ");
            sb.append(stack.getCount()).append("x ").append(stack.getHoverName().getString());
            first = false;
        }
        return Component.literal(sb.toString());
    }

    /**
     * Formate le prix en NOCOIN.
     */
    private static String formatCurrency(long amount) {
        return String.format("%,d NC", amount);
    }
}

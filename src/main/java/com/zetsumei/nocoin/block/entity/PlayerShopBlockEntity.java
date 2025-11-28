package com.zetsumei.nocoin.block.entity;

import com.mojang.logging.LogUtils;
import com.zetsumei.nocoin.capability.NocoinCapabilityProvider;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import com.zetsumei.nocoin.shop.player.ShopOffer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.*;

/**
 * BlockEntity pour le bloc magasin joueur.
 * Stocke les offres d'achat/vente et gère les transactions.
 */
public class PlayerShopBlockEntity extends BlockEntity {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAX_OFFERS = 9;  // Nombre max d'offres par magasin
    private static final String TAG_OWNER_UUID = "ownerUUID";
    private static final String TAG_OWNER_NAME = "ownerName";
    private static final String TAG_SHOP_NAME = "shopName";
    private static final String TAG_OFFERS = "offers";

    @Nullable
    private UUID ownerUUID;
    private String ownerName = "";
    private String shopName = "";
    private final List<ShopOffer> offers = new ArrayList<>();

    public PlayerShopBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PLAYER_SHOP.get(), pos, state);
    }

    // ============== Propriétaire ==============

    public void setOwner(Player player) {
        this.ownerUUID = player.getUUID();
        this.ownerName = player.getName().getString();
        setChanged();
        syncToClient();
    }

    @Nullable
    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isOwner(Player player) {
        return ownerUUID != null && ownerUUID.equals(player.getUUID());
    }

    public boolean hasOwner() {
        return ownerUUID != null;
    }

    // ============== Nom du magasin ==============

    public String getShopName() {
        return shopName.isEmpty() ? (ownerName + "'s Shop") : shopName;
    }

    public void setShopName(String name) {
        this.shopName = name != null ? name : "";
        setChanged();
        syncToClient();
    }

    // ============== Gestion des offres ==============

    public List<ShopOffer> getOffers() {
        return Collections.unmodifiableList(offers);
    }

    public List<ShopOffer> getActiveOffers() {
        return offers.stream()
                .filter(ShopOffer::isActive)
                .filter(ShopOffer::isValid)
                .toList();
    }

    public List<ShopOffer> getSellOffers() {
        return offers.stream()
                .filter(o -> o.getType() == ShopOffer.OfferType.SELL)
                .filter(ShopOffer::isActive)
                .filter(ShopOffer::isValid)
                .toList();
    }

    public List<ShopOffer> getBuyOffers() {
        return offers.stream()
                .filter(o -> o.getType() == ShopOffer.OfferType.BUY)
                .filter(ShopOffer::isActive)
                .filter(ShopOffer::isValid)
                .toList();
    }

    public boolean canAddOffer() {
        return offers.size() < MAX_OFFERS;
    }

    public boolean addOffer(ShopOffer offer) {
        if (!canAddOffer()) {
            return false;
        }
        offers.add(offer);
        setChanged();
        syncToClient();
        return true;
    }

    public boolean removeOffer(UUID offerId) {
        boolean removed = offers.removeIf(o -> o.getOfferId().equals(offerId));
        if (removed) {
            setChanged();
            syncToClient();
        }
        return removed;
    }

    @Nullable
    public ShopOffer getOfferById(UUID offerId) {
        return offers.stream()
                .filter(o -> o.getOfferId().equals(offerId))
                .findFirst()
                .orElse(null);
    }

    public void updateOfferStock(UUID offerId, int newStock) {
        ShopOffer offer = getOfferById(offerId);
        if (offer != null) {
            offer.setStock(newStock);
            setChanged();
            syncToClient();
        }
    }

    // ============== Transactions ==============

    /**
     * Résultat d'une transaction dans le magasin joueur.
     */
    public static class TransactionResult {
        public enum Status {
            SUCCESS,
            OFFER_NOT_FOUND,
            OFFER_INACTIVE,
            NO_STOCK,
            INSUFFICIENT_BALANCE,
            INVENTORY_FULL,
            INSUFFICIENT_ITEMS,
            OWNER_OFFLINE,
            ERROR
        }

        private final Status status;
        private final long amountTransferred;
        private final String message;

        public TransactionResult(Status status, long amountTransferred, String message) {
            this.status = status;
            this.amountTransferred = amountTransferred;
            this.message = message;
        }

        public static TransactionResult success(long amount) {
            return new TransactionResult(Status.SUCCESS, amount, "Transaction réussie");
        }

        public static TransactionResult failure(Status status) {
            return new TransactionResult(status, 0, status.name());
        }

        public boolean isSuccess() {
            return status == Status.SUCCESS;
        }

        public Status getStatus() {
            return status;
        }

        public long getAmountTransferred() {
            return amountTransferred;
        }

        public Component getMessageComponent() {
            return switch (status) {
                case SUCCESS -> Component.literal("Transaction réussie !");
                case OFFER_NOT_FOUND -> Component.literal("Offre non trouvée");
                case OFFER_INACTIVE -> Component.literal("Offre inactive");
                case NO_STOCK -> Component.literal("Stock insuffisant");
                case INSUFFICIENT_BALANCE -> Component.literal("Solde NOCOIN insuffisant");
                case INVENTORY_FULL -> Component.literal("Inventaire plein");
                case INSUFFICIENT_ITEMS -> Component.literal("Items insuffisants");
                case OWNER_OFFLINE -> Component.literal("Le propriétaire est hors ligne");
                case ERROR -> Component.literal("Erreur lors de la transaction");
            };
        }
    }

    /**
     * Exécute un achat (visiteur achète une offre de vente du propriétaire).
     * @param buyer Le joueur qui achète
     * @param offerId L'ID de l'offre
     * @return Le résultat de la transaction
     */
    public TransactionResult executeBuyFromShop(ServerPlayer buyer, UUID offerId) {
        ShopOffer offer = getOfferById(offerId);
        if (offer == null) {
            return TransactionResult.failure(TransactionResult.Status.OFFER_NOT_FOUND);
        }

        if (!offer.isActive() || offer.getType() != ShopOffer.OfferType.SELL) {
            return TransactionResult.failure(TransactionResult.Status.OFFER_INACTIVE);
        }

        if (!offer.hasStock()) {
            return TransactionResult.failure(TransactionResult.Status.NO_STOCK);
        }

        long price = offer.getTotalPrice();

        // Vérifier le solde de l'acheteur
        final TransactionResult[] result = {null};
        buyer.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(buyerCap -> {
            if (!buyerCap.hasEnough(price)) {
                result[0] = TransactionResult.failure(TransactionResult.Status.INSUFFICIENT_BALANCE);
                return;
            }

            // Vérifier l'inventaire de l'acheteur
            ItemStack itemToGive = offer.createItemStack();
            if (!canAddToInventory(buyer, itemToGive)) {
                result[0] = TransactionResult.failure(TransactionResult.Status.INVENTORY_FULL);
                return;
            }

            // Exécuter la transaction
            buyerCap.removeBalance(price);

            // Donner l'item à l'acheteur
            if (!buyer.getInventory().add(itemToGive)) {
                buyer.drop(itemToGive, false);
            }

            // Réduire le stock
            offer.removeStock(offer.getQuantity());

            // Créditer le propriétaire (si en ligne)
            creditOwner(price);

            // Synchroniser les données
            NocoinNetworkHandler.sendBalanceToClient(buyer, buyerCap.getBalance());
            setChanged();
            syncToClient();

            LOGGER.info("Transaction: {} a acheté {} pour {} NOCOIN dans le magasin de {}",
                    buyer.getName().getString(),
                    offer.getItemId(),
                    price,
                    ownerName);

            result[0] = TransactionResult.success(price);
        });

        return result[0] != null ? result[0] : TransactionResult.failure(TransactionResult.Status.ERROR);
    }

    /**
     * Exécute une vente (visiteur vend au propriétaire via une offre d'achat).
     * @param seller Le joueur qui vend
     * @param offerId L'ID de l'offre
     * @return Le résultat de la transaction
     */
    public TransactionResult executeSellToShop(ServerPlayer seller, UUID offerId) {
        ShopOffer offer = getOfferById(offerId);
        if (offer == null) {
            return TransactionResult.failure(TransactionResult.Status.OFFER_NOT_FOUND);
        }

        if (!offer.isActive() || offer.getType() != ShopOffer.OfferType.BUY) {
            return TransactionResult.failure(TransactionResult.Status.OFFER_INACTIVE);
        }

        // Vérifier le stock limite (0 = illimité)
        if (offer.getStock() > 0 && offer.getStock() < offer.getQuantity()) {
            return TransactionResult.failure(TransactionResult.Status.NO_STOCK);
        }

        long price = offer.getTotalPrice();

        // Vérifier que le vendeur a les items
        ItemStack requiredItem = offer.createItemStack();
        if (!hasItemsInInventory(seller, requiredItem)) {
            return TransactionResult.failure(TransactionResult.Status.INSUFFICIENT_ITEMS);
        }

        final TransactionResult[] result = {null};
        seller.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(sellerCap -> {
            // Retirer les items du vendeur
            removeItemsFromInventory(seller, requiredItem);

            // Créditer le vendeur
            sellerCap.addBalance(price);

            // Mettre à jour le stock si limite
            if (offer.getStock() > 0) {
                offer.removeStock(offer.getQuantity());
            }

            // Synchroniser les données
            NocoinNetworkHandler.sendBalanceToClient(seller, sellerCap.getBalance());
            setChanged();
            syncToClient();

            LOGGER.info("Transaction: {} a vendu {} pour {} NOCOIN au magasin de {}",
                    seller.getName().getString(),
                    offer.getItemId(),
                    price,
                    ownerName);

            result[0] = TransactionResult.success(price);
        });

        return result[0] != null ? result[0] : TransactionResult.failure(TransactionResult.Status.ERROR);
    }

    private void creditOwner(long amount) {
        if (level == null || ownerUUID == null) return;
        
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerUUID);
        if (owner != null) {
            owner.getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY).ifPresent(cap -> {
                cap.addBalance(amount);
                NocoinNetworkHandler.sendBalanceToClient(owner, cap.getBalance());
            });
        }
        // Note: Si le propriétaire est hors ligne, les NOCOIN sont perdus
        // Une amélioration future pourrait stocker les gains en attente
    }

    private boolean canAddToInventory(ServerPlayer player, ItemStack stack) {
        ItemStack testStack = stack.copy();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slotStack = player.getInventory().getItem(i);
            if (slotStack.isEmpty()) {
                return true;
            }
            if (ItemStack.isSameItemSameTags(slotStack, testStack)) {
                int canAdd = slotStack.getMaxStackSize() - slotStack.getCount();
                if (canAdd >= testStack.getCount()) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean hasItemsInInventory(ServerPlayer player, ItemStack required) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (ItemStack.isSameItem(stack, required)) {
                count += stack.getCount();
            }
        }
        return count >= required.getCount();
    }

    private void removeItemsFromInventory(ServerPlayer player, ItemStack toRemove) {
        int remaining = toRemove.getCount();
        for (int i = 0; i < player.getInventory().items.size() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().items.get(i);
            if (ItemStack.isSameItem(stack, toRemove)) {
                int remove = Math.min(remaining, stack.getCount());
                stack.shrink(remove);
                remaining -= remove;
            }
        }
    }

    // ============== Sauvegarde NBT ==============

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        if (ownerUUID != null) {
            tag.putUUID(TAG_OWNER_UUID, ownerUUID);
            tag.putString(TAG_OWNER_NAME, ownerName);
        }
        tag.putString(TAG_SHOP_NAME, shopName);

        ListTag offersList = new ListTag();
        for (ShopOffer offer : offers) {
            offersList.add(offer.save());
        }
        tag.put(TAG_OFFERS, offersList);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.hasUUID(TAG_OWNER_UUID)) {
            ownerUUID = tag.getUUID(TAG_OWNER_UUID);
            ownerName = tag.getString(TAG_OWNER_NAME);
        } else {
            ownerUUID = null;
            ownerName = "";
        }

        shopName = tag.getString(TAG_SHOP_NAME);

        offers.clear();
        ListTag offersList = tag.getList(TAG_OFFERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < offersList.size(); i++) {
            offers.add(ShopOffer.load(offersList.getCompound(i)));
        }
    }

    // ============== Synchronisation Client ==============

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
}

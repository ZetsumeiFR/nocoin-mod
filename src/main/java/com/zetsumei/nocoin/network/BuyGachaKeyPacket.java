package com.zetsumei.nocoin.network;

import com.mojang.logging.LogUtils;
import com.zetsumei.nocoin.Config;
import com.zetsumei.nocoin.capability.NocoinCapabilityProvider;
import com.zetsumei.nocoin.item.ModItems;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.slf4j.Logger;

/**
 * Paquet envoyé du client vers le serveur pour acheter une Clé Gacha.
 */
public class BuyGachaKeyPacket {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final int quantity;

    public BuyGachaKeyPacket(int quantity) {
        this.quantity = Math.max(1, Math.min(quantity, 64)); // Limite entre 1 et 64
    }

    public static void encode(BuyGachaKeyPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.quantity);
    }

    public static BuyGachaKeyPacket decode(FriendlyByteBuf buf) {
        return new BuyGachaKeyPacket(buf.readVarInt());
    }

    public static void handle(
        BuyGachaKeyPacket packet,
        Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player
                .getCapability(NocoinCapabilityProvider.NOCOIN_CAPABILITY)
                .ifPresent(cap -> {
                    long price = Config.gachaKeyPrice * packet.quantity;

                    // Vérifier le solde
                    if (!cap.hasEnough(price)) {
                        player.sendSystemMessage(
                            Component.literal(
                                "Solde NOCOIN insuffisant !"
                            ).withStyle(style -> style.withColor(0xFF5555))
                        );
                        // Envoyer résultat d'échec
                        NocoinNetworkHandler.sendGachaKeyPurchaseResult(
                            player,
                            false,
                            cap.getBalance(),
                            0
                        );
                        return;
                    }

                    // Créer l'ItemStack de clés Gacha
                    ItemStack keyStack = new ItemStack(
                        ModItems.GACHA_KEY.get(),
                        packet.quantity
                    );

                    // Vérifier l'inventaire
                    if (!canAddToInventory(player, keyStack)) {
                        player.sendSystemMessage(
                            Component.literal("Inventaire plein !").withStyle(
                                style -> style.withColor(0xFF5555)
                            )
                        );
                        NocoinNetworkHandler.sendGachaKeyPurchaseResult(
                            player,
                            false,
                            cap.getBalance(),
                            0
                        );
                        return;
                    }

                    // Effectuer la transaction
                    cap.removeBalance(price);

                    // Donner les clés
                    if (!player.getInventory().add(keyStack)) {
                        player.drop(keyStack, false);
                    }

                    // Synchroniser le solde
                    NocoinNetworkHandler.sendBalanceToClient(
                        player,
                        cap.getBalance()
                    );

                    // Envoyer résultat de succès
                    NocoinNetworkHandler.sendGachaKeyPurchaseResult(
                        player,
                        true,
                        cap.getBalance(),
                        packet.quantity
                    );

                    LOGGER.info(
                        "Joueur {} a acheté {} Clé(s) Gacha pour {} NOCOIN",
                        player.getName().getString(),
                        packet.quantity,
                        price
                    );
                });
        });
        context.setPacketHandled(true);
    }

    /**
     * Vérifie si un ItemStack peut être ajouté à l'inventaire du joueur.
     */
    private static boolean canAddToInventory(
        ServerPlayer player,
        ItemStack stack
    ) {
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
}

package com.zetsumei.nocoin.network.gacha;

import com.zetsumei.nocoin.block.entity.GachaMachineBlockEntity;
import com.zetsumei.nocoin.gacha.GachaHistory;
import com.zetsumei.nocoin.gacha.GachaHistoryManager;
import com.zetsumei.nocoin.gacha.GachaManager;
import com.zetsumei.nocoin.gacha.GachaManager.GachaPullResult;
import com.zetsumei.nocoin.item.ModItems;
import com.zetsumei.nocoin.network.NocoinNetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Packet client → serveur pour demander un multi-tirage (x10).
 * Inclut la position de la machine pour identifier le catalogue à utiliser.
 */
public class GachaMultiPullPacket {

    private final BlockPos machinePos;
    private final int count;

    public GachaMultiPullPacket(BlockPos machinePos, int count) {
        this.machinePos = machinePos;
        this.count = count;
    }

    public static void encode(GachaMultiPullPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.machinePos);
        buf.writeInt(packet.count);
    }

    public static GachaMultiPullPacket decode(FriendlyByteBuf buf) {
        return new GachaMultiPullPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(GachaMultiPullPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level level = player.level();
            BlockEntity be = level.getBlockEntity(packet.machinePos);
            if (!(be instanceof GachaMachineBlockEntity gachaBE)) {
                NocoinNetworkHandler.sendGachaMultiPullResult(player, false, new ArrayList<>());
                return;
            }

            int requestedCount = Math.min(packet.count, 10); // Max 10 tirages
            int availableKeys = countGachaKeys(player);
            int actualCount = Math.min(requestedCount, availableKeys);

            if (actualCount <= 0) {
                NocoinNetworkHandler.sendGachaMultiPullResult(player, false, new ArrayList<>());
                return;
            }

            // Consommer les clés
            consumeGachaKeys(player, actualCount);

            // Effectuer les tirages depuis la machine spécifique
            List<GachaMultiPullResultPacket.PullResult> results = new ArrayList<>();
            List<GachaHistory> histories = new ArrayList<>();

            for (int i = 0; i < actualCount; i++) {
                GachaPullResult result = gachaBE.pull();
                if (result != null) {
                    // Donner l'item au joueur
                    gachaBE.giveRewardToPlayer(player, result.reward());
                    
                    results.add(new GachaMultiPullResultPacket.PullResult(
                        result.reward().getItemId(),
                        result.reward().getDisplayName(),
                        result.getRarity().getStars()
                    ));
                    histories.add(GachaHistory.fromReward(result.reward()));
                }
            }

            // Enregistrer l'historique
            if (!histories.isEmpty()) {
                GachaHistoryManager.getInstance().addHistories(player.getUUID(), histories);
            }

            NocoinNetworkHandler.sendGachaMultiPullResult(player, true, results);
        });
        ctx.get().setPacketHandled(true);
    }

    private static int countGachaKeys(ServerPlayer player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.GACHA_KEY.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeGachaKeys(ServerPlayer player, int amount) {
        int remaining = amount;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(ModItems.GACHA_KEY.get())) {
                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;
            }
        }
    }
}

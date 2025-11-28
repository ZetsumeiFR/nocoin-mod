package com.zetsumei.nocoin.leaderboard;

import net.minecraft.network.FriendlyByteBuf;

/**
 * Entree du classement representant un joueur.
 */
public class LeaderboardEntry {

    private final String playerName;
    private final long nocoinBalance;
    private final int rank;

    public LeaderboardEntry(
        String playerName,
        long nocoinBalance,
        int rank
    ) {
        this.playerName = playerName;
        this.nocoinBalance = nocoinBalance;
        this.rank = rank;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getNocoinBalance() {
        return nocoinBalance;
    }

    public int getRank() {
        return rank;
    }

    /**
     * Encode l'entree dans le buffer reseau.
     */
    public void toNetwork(FriendlyByteBuf buf) {
        buf.writeUtf(playerName);
        buf.writeLong(nocoinBalance);
        buf.writeVarInt(rank);
    }

    /**
     * Decode une entree depuis le buffer reseau.
     */
    public static LeaderboardEntry fromNetwork(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        long balance = buf.readLong();
        int rank = buf.readVarInt();
        return new LeaderboardEntry(name, balance, rank);
    }
}

package net.minecraft.server.level;

import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import java.util.Set;

public final class PlayerMap {
    private final Object2BooleanMap<ServerPlayer> players = new Object2BooleanOpenHashMap<>();

    public Set<ServerPlayer> getAllPlayers() {
        return this.players.keySet();
    }

    public void addPlayer(ServerPlayer pPlayer, boolean pSkipPlayer) {
        this.players.put(pPlayer, pSkipPlayer);
    }

    public void removePlayer(ServerPlayer pPlayer) {
        this.players.removeBoolean(pPlayer);
    }

    public void ignorePlayer(ServerPlayer pPlayer) {
        this.players.replace(pPlayer, true);
    }

    public void unIgnorePlayer(ServerPlayer pPlayer) {
        this.players.replace(pPlayer, false);
    }

    public boolean ignoredOrUnknown(ServerPlayer pPlayer) {
        return this.players.getOrDefault(pPlayer, true);
    }

    public boolean ignored(ServerPlayer pPlayer) {
        return this.players.getBoolean(pPlayer);
    }
}

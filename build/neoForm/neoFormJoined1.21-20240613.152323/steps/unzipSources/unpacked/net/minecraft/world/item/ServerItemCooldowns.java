package net.minecraft.world.item;

import net.minecraft.network.protocol.game.ClientboundCooldownPacket;
import net.minecraft.server.level.ServerPlayer;

public class ServerItemCooldowns extends ItemCooldowns {
    private final ServerPlayer player;

    public ServerItemCooldowns(ServerPlayer pPlayer) {
        this.player = pPlayer;
    }

    @Override
    protected void onCooldownStarted(Item pItem, int pTicks) {
        super.onCooldownStarted(pItem, pTicks);
        this.player.connection.send(new ClientboundCooldownPacket(pItem, pTicks));
    }

    @Override
    protected void onCooldownEnded(Item pItem) {
        super.onCooldownEnded(pItem);
        this.player.connection.send(new ClientboundCooldownPacket(pItem, 0));
    }
}

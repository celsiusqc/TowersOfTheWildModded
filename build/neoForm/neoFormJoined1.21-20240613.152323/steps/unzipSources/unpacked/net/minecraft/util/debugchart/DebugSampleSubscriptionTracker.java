package net.minecraft.util.debugchart;

import com.google.common.collect.Maps;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import net.minecraft.Util;
import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;

public class DebugSampleSubscriptionTracker {
    public static final int STOP_SENDING_AFTER_TICKS = 200;
    public static final int STOP_SENDING_AFTER_MS = 10000;
    private final PlayerList playerList;
    private final EnumMap<RemoteDebugSampleType, Map<ServerPlayer, DebugSampleSubscriptionTracker.SubscriptionStartedAt>> subscriptions;
    private final Queue<DebugSampleSubscriptionTracker.SubscriptionRequest> subscriptionRequestQueue = new LinkedList<>();

    public DebugSampleSubscriptionTracker(PlayerList pPlayerList) {
        this.playerList = pPlayerList;
        this.subscriptions = new EnumMap<>(RemoteDebugSampleType.class);

        for (RemoteDebugSampleType remotedebugsampletype : RemoteDebugSampleType.values()) {
            this.subscriptions.put(remotedebugsampletype, Maps.newHashMap());
        }
    }

    public boolean shouldLogSamples(RemoteDebugSampleType pSampleType) {
        return !this.subscriptions.get(pSampleType).isEmpty();
    }

    public void broadcast(ClientboundDebugSamplePacket pPacket) {
        for (ServerPlayer serverplayer : this.subscriptions.get(pPacket.debugSampleType()).keySet()) {
            serverplayer.connection.send(pPacket);
        }
    }

    public void subscribe(ServerPlayer pPlayer, RemoteDebugSampleType pSampleType) {
        if (this.playerList.isOp(pPlayer.getGameProfile())) {
            this.subscriptionRequestQueue.add(new DebugSampleSubscriptionTracker.SubscriptionRequest(pPlayer, pSampleType));
        }
    }

    public void tick(int pTick) {
        long i = Util.getMillis();
        this.handleSubscriptions(i, pTick);
        this.handleUnsubscriptions(i, pTick);
    }

    private void handleSubscriptions(long pMillis, int pTick) {
        for (DebugSampleSubscriptionTracker.SubscriptionRequest debugsamplesubscriptiontracker$subscriptionrequest : this.subscriptionRequestQueue) {
            this.subscriptions
                .get(debugsamplesubscriptiontracker$subscriptionrequest.sampleType())
                .put(
                    debugsamplesubscriptiontracker$subscriptionrequest.player(), new DebugSampleSubscriptionTracker.SubscriptionStartedAt(pMillis, pTick)
                );
        }
    }

    private void handleUnsubscriptions(long pMillis, int pTick) {
        for (Map<ServerPlayer, DebugSampleSubscriptionTracker.SubscriptionStartedAt> map : this.subscriptions.values()) {
            map.entrySet()
                .removeIf(
                    p_323887_ -> {
                        boolean flag = !this.playerList.isOp(p_323887_.getKey().getGameProfile());
                        DebugSampleSubscriptionTracker.SubscriptionStartedAt debugsamplesubscriptiontracker$subscriptionstartedat = p_323887_.getValue();
                        return flag
                            || pTick > debugsamplesubscriptiontracker$subscriptionstartedat.tick() + 200
                                && pMillis > debugsamplesubscriptiontracker$subscriptionstartedat.millis() + 10000L;
                    }
                );
        }
    }

    static record SubscriptionRequest(ServerPlayer player, RemoteDebugSampleType sampleType) {
    }

    static record SubscriptionStartedAt(long millis, int tick) {
    }
}

package net.minecraft.client.multiplayer;

import java.util.EnumMap;
import net.minecraft.Util;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.network.protocol.game.ServerboundDebugSampleSubscriptionPacket;
import net.minecraft.util.debugchart.RemoteDebugSampleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugSampleSubscriber {
    public static final int REQUEST_INTERVAL_MS = 5000;
    private final ClientPacketListener connection;
    private final DebugScreenOverlay debugScreenOverlay;
    private final EnumMap<RemoteDebugSampleType, Long> lastRequested;

    public DebugSampleSubscriber(ClientPacketListener pConnection, DebugScreenOverlay pDebugScreenOverlay) {
        this.debugScreenOverlay = pDebugScreenOverlay;
        this.connection = pConnection;
        this.lastRequested = new EnumMap<>(RemoteDebugSampleType.class);
    }

    public void tick() {
        if (this.debugScreenOverlay.showFpsCharts()) {
            this.sendSubscriptionRequestIfNeeded(RemoteDebugSampleType.TICK_TIME);
        }
    }

    private void sendSubscriptionRequestIfNeeded(RemoteDebugSampleType pSampleType) {
        long i = Util.getMillis();
        if (i > this.lastRequested.getOrDefault(pSampleType, Long.valueOf(0L)) + 5000L) {
            this.connection.send(new ServerboundDebugSampleSubscriptionPacket(pSampleType));
            this.lastRequested.put(pSampleType, i);
        }
    }
}

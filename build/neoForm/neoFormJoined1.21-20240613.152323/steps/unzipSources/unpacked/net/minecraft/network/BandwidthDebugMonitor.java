package net.minecraft.network;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.util.debugchart.LocalSampleLogger;

public class BandwidthDebugMonitor {
    private final AtomicInteger bytesReceived = new AtomicInteger();
    private final LocalSampleLogger bandwidthLogger;

    public BandwidthDebugMonitor(LocalSampleLogger pBandwithLogger) {
        this.bandwidthLogger = pBandwithLogger;
    }

    public void onReceive(int pAmount) {
        this.bytesReceived.getAndAdd(pAmount);
    }

    public void tick() {
        this.bandwidthLogger.logSample((long)this.bytesReceived.getAndSet(0));
    }
}

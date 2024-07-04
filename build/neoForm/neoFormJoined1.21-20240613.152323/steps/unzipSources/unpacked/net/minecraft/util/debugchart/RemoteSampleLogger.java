package net.minecraft.util.debugchart;

import net.minecraft.network.protocol.game.ClientboundDebugSamplePacket;

public class RemoteSampleLogger extends AbstractSampleLogger {
    private final DebugSampleSubscriptionTracker subscriptionTracker;
    private final RemoteDebugSampleType sampleType;

    public RemoteSampleLogger(int pSize, DebugSampleSubscriptionTracker pSubscriptionTracker, RemoteDebugSampleType pSampleType) {
        this(pSize, pSubscriptionTracker, pSampleType, new long[pSize]);
    }

    public RemoteSampleLogger(int pSize, DebugSampleSubscriptionTracker pSubscriptionTracker, RemoteDebugSampleType pSampleType, long[] pDefaults) {
        super(pSize, pDefaults);
        this.subscriptionTracker = pSubscriptionTracker;
        this.sampleType = pSampleType;
    }

    @Override
    protected void useSample() {
        this.subscriptionTracker.broadcast(new ClientboundDebugSamplePacket((long[])this.sample.clone(), this.sampleType));
    }
}

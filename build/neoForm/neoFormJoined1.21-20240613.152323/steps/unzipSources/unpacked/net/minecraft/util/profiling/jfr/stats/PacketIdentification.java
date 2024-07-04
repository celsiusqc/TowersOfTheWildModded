package net.minecraft.util.profiling.jfr.stats;

import jdk.jfr.consumer.RecordedEvent;

public record PacketIdentification(String direction, String protocolId, String packetId) {
    public static PacketIdentification from(RecordedEvent pEvent) {
        return new PacketIdentification(pEvent.getString("packetDirection"), pEvent.getString("protocolId"), pEvent.getString("packetId"));
    }
}

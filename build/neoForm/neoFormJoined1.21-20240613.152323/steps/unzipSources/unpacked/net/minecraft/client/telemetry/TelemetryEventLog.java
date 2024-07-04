package net.minecraft.client.telemetry;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.concurrent.Executor;
import net.minecraft.util.eventlog.JsonEventLog;
import net.minecraft.util.thread.ProcessorMailbox;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class TelemetryEventLog implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final JsonEventLog<TelemetryEventInstance> log;
    private final ProcessorMailbox<Runnable> mailbox;

    public TelemetryEventLog(FileChannel pChannel, Executor pDispatcher) {
        this.log = new JsonEventLog<>(TelemetryEventInstance.CODEC, pChannel);
        this.mailbox = ProcessorMailbox.create(pDispatcher, "telemetry-event-log");
    }

    public TelemetryEventLogger logger() {
        return p_261508_ -> this.mailbox.tell(() -> {
                try {
                    this.log.write(p_261508_);
                } catch (IOException ioexception) {
                    LOGGER.error("Failed to write telemetry event to log", (Throwable)ioexception);
                }
            });
    }

    @Override
    public void close() {
        this.mailbox.tell(() -> IOUtils.closeQuietly(this.log));
        this.mailbox.close();
    }
}

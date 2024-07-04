package com.mojang.blaze3d.audio;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;

/**
 * Represents an OpenAL audio channel.
 */
@OnlyIn(Dist.CLIENT)
public class Channel {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int QUEUED_BUFFER_COUNT = 4;
    public static final int BUFFER_DURATION_SECONDS = 1;
    private final int source;
    private final AtomicBoolean initialized = new AtomicBoolean(true);
    private int streamingBufferSize = 16384;
    @Nullable
    private AudioStream stream;

    @Nullable
    static Channel create() {
        int[] aint = new int[1];
        AL10.alGenSources(aint);
        return OpenAlUtil.checkALError("Allocate new source") ? null : new Channel(aint[0]);
    }

    private Channel(int pSource) {
        this.source = pSource;
    }

    public void destroy() {
        if (this.initialized.compareAndSet(true, false)) {
            AL10.alSourceStop(this.source);
            OpenAlUtil.checkALError("Stop");
            if (this.stream != null) {
                try {
                    this.stream.close();
                } catch (IOException ioexception) {
                    LOGGER.error("Failed to close audio stream", (Throwable)ioexception);
                }

                this.removeProcessedBuffers();
                this.stream = null;
            }

            AL10.alDeleteSources(new int[]{this.source});
            OpenAlUtil.checkALError("Cleanup");
        }
    }

    public void play() {
        AL10.alSourcePlay(this.source);
    }

    private int getState() {
        return !this.initialized.get() ? 4116 : AL10.alGetSourcei(this.source, 4112);
    }

    public void pause() {
        if (this.getState() == 4114) {
            AL10.alSourcePause(this.source);
        }
    }

    public void unpause() {
        if (this.getState() == 4115) {
            AL10.alSourcePlay(this.source);
        }
    }

    public void stop() {
        if (this.initialized.get()) {
            AL10.alSourceStop(this.source);
            OpenAlUtil.checkALError("Stop");
        }
    }

    public boolean playing() {
        return this.getState() == 4114;
    }

    public boolean stopped() {
        return this.getState() == 4116;
    }

    /**
     * Sets the position of the audio channel.
     *
     * @param pSource the position of the audio channel
     */
    public void setSelfPosition(Vec3 pSource) {
        AL10.alSourcefv(this.source, 4100, new float[]{(float)pSource.x, (float)pSource.y, (float)pSource.z});
    }

    /**
     * Sets the pitch of the audio channel.
     *
     * @param pPitch the pitch of the audio channel
     */
    public void setPitch(float pPitch) {
        AL10.alSourcef(this.source, 4099, pPitch);
    }

    /**
     * Sets whether the audio channel should loop.
     *
     * @param pLooping {@code true} if the audio channel should loop, {@code false}
     *                 otherwise
     */
    public void setLooping(boolean pLooping) {
        AL10.alSourcei(this.source, 4103, pLooping ? 1 : 0);
    }

    /**
     * Sets the volume of the audio channel.
     *
     * @param pVolume the volume of the audio channel
     */
    public void setVolume(float pVolume) {
        AL10.alSourcef(this.source, 4106, pVolume);
    }

    public void disableAttenuation() {
        AL10.alSourcei(this.source, 53248, 0);
    }

    /**
     * Sets linear attenuation for the audio channel.
     *
     * @param pLinearAttenuation the linear attenuation of the audio channel
     */
    public void linearAttenuation(float pLinearAttenuation) {
        AL10.alSourcei(this.source, 53248, 53251);
        AL10.alSourcef(this.source, 4131, pLinearAttenuation);
        AL10.alSourcef(this.source, 4129, 1.0F);
        AL10.alSourcef(this.source, 4128, 0.0F);
    }

    /**
     * Sets whether the audio channel should be relative to the listener's position.
     *
     * @param pRelative {@code true} if the audio channel should be relative, {@code
     *                  false} otherwise
     */
    public void setRelative(boolean pRelative) {
        AL10.alSourcei(this.source, 514, pRelative ? 1 : 0);
    }

    /**
     * Attaches a static buffer to the audio channel.
     *
     * @param pBuffer the buffer to attach
     */
    public void attachStaticBuffer(SoundBuffer pBuffer) {
        pBuffer.getAlBuffer().ifPresent(p_83676_ -> AL10.alSourcei(this.source, 4105, p_83676_));
    }

    /**
     * Attaches a buffer stream to the audio channel.
     *
     * @param pStream the stream to attach
     */
    public void attachBufferStream(AudioStream pStream) {
        this.stream = pStream;
        AudioFormat audioformat = pStream.getFormat();
        this.streamingBufferSize = calculateBufferSize(audioformat, 1);
        this.pumpBuffers(4);
    }

    /**
     * Calculates the buffer size for an audio stream.
     * @return the buffer size
     *
     * @param pFormat       the audio format of the stream
     * @param pSampleAmount the number of samples to buffer
     */
    private static int calculateBufferSize(AudioFormat pFormat, int pSampleAmount) {
        return (int)((float)(pSampleAmount * pFormat.getSampleSizeInBits()) / 8.0F * (float)pFormat.getChannels() * pFormat.getSampleRate());
    }

    /**
     * Reads and queues audio buffers from the stream.
     *
     * @param pReadCount the number of buffers to read and queue
     */
    private void pumpBuffers(int pReadCount) {
        if (this.stream != null) {
            try {
                for (int i = 0; i < pReadCount; i++) {
                    ByteBuffer bytebuffer = this.stream.read(this.streamingBufferSize);
                    if (bytebuffer != null) {
                        new SoundBuffer(bytebuffer, this.stream.getFormat())
                            .releaseAlBuffer()
                            .ifPresent(p_83669_ -> AL10.alSourceQueueBuffers(this.source, new int[]{p_83669_}));
                    }
                }
            } catch (IOException ioexception) {
                LOGGER.error("Failed to read from audio stream", (Throwable)ioexception);
            }
        }
    }

    public void updateStream() {
        if (this.stream != null) {
            int i = this.removeProcessedBuffers();
            this.pumpBuffers(i);
        }
    }

    private int removeProcessedBuffers() {
        int i = AL10.alGetSourcei(this.source, 4118);
        if (i > 0) {
            int[] aint = new int[i];
            AL10.alSourceUnqueueBuffers(this.source, aint);
            OpenAlUtil.checkALError("Unqueue buffers");
            AL10.alDeleteBuffers(aint);
            OpenAlUtil.checkALError("Remove processed buffers");
        }

        return i;
    }
}

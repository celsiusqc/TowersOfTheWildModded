package net.minecraft.client.server;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.slf4j.Logger;

// NeoForge: Make this client-only class also available on the server
public class LanServerPinger extends Thread {
    private static final AtomicInteger UNIQUE_THREAD_ID = new AtomicInteger(0);
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String MULTICAST_GROUP = net.neoforged.neoforge.network.DualStackUtils.getMulticastGroup();
    public static final int PING_PORT = 4445;
    private static final long PING_INTERVAL = 1500L;
    private final String motd;
    private final DatagramSocket socket;
    private boolean isRunning = true;
    private final String serverAddress;

    public LanServerPinger(String pMotd, String pServerAddress) throws IOException {
        super("LanServerPinger #" + UNIQUE_THREAD_ID.incrementAndGet());
        this.motd = pMotd;
        this.serverAddress = pServerAddress;
        this.setDaemon(true);
        this.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        this.socket = new DatagramSocket();
    }

    @Override
    public void run() {
        String s = createPingString(this.motd, this.serverAddress);
        byte[] abyte = s.getBytes(StandardCharsets.UTF_8);

        while (!this.isInterrupted() && this.isRunning) {
            try {
                InetAddress inetaddress = InetAddress.getByName(MULTICAST_GROUP);
                DatagramPacket datagrampacket = new DatagramPacket(abyte, abyte.length, inetaddress, 4445);
                this.socket.send(datagrampacket);
            } catch (IOException ioexception) {
                LOGGER.warn("LanServerPinger: {}", ioexception.getMessage());
                break;
            }

            try {
                sleep(1500L);
            } catch (InterruptedException interruptedexception) {
            }
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        this.isRunning = false;
    }

    public static String createPingString(String pMotdMessage, String pAdMessage) {
        return "[MOTD]" + pMotdMessage + "[/MOTD][AD]" + pAdMessage + "[/AD]";
    }

    public static String parseMotd(String pPingResponse) {
        int i = pPingResponse.indexOf("[MOTD]");
        if (i < 0) {
            return "missing no";
        } else {
            int j = pPingResponse.indexOf("[/MOTD]", i + "[MOTD]".length());
            return j < i ? "missing no" : pPingResponse.substring(i + "[MOTD]".length(), j);
        }
    }

    public static String parseAddress(String pPingResponse) {
        int i = pPingResponse.indexOf("[/MOTD]");
        if (i < 0) {
            return null;
        } else {
            int j = pPingResponse.indexOf("[/MOTD]", i + "[/MOTD]".length());
            if (j >= 0) {
                return null;
            } else {
                int k = pPingResponse.indexOf("[AD]", i + "[/MOTD]".length());
                if (k < 0) {
                    return null;
                } else {
                    int l = pPingResponse.indexOf("[/AD]", k + "[AD]".length());
                    return l < k ? null : pPingResponse.substring(k + "[AD]".length(), l);
                }
            }
        }
    }
}

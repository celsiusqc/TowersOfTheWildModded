package net.minecraft.network;

import com.google.common.base.Suppliers;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.logging.LogUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.flow.FlowControlHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.concurrent.Future;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.BundlerInfo;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.handshake.HandshakeProtocols;
import net.minecraft.network.protocol.handshake.ServerHandshakePacketListener;
import net.minecraft.network.protocol.login.ClientLoginPacketListener;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.LoginProtocols;
import net.minecraft.network.protocol.status.ClientStatusPacketListener;
import net.minecraft.network.protocol.status.StatusProtocols;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.util.Mth;
import net.minecraft.util.debugchart.LocalSampleLogger;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Connection extends SimpleChannelInboundHandler<Packet<?>> {
    private static final float AVERAGE_PACKETS_SMOOTHING = 0.75F;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final Marker ROOT_MARKER = MarkerFactory.getMarker("NETWORK");
    public static final Marker PACKET_MARKER = Util.make(MarkerFactory.getMarker("NETWORK_PACKETS"), p_202569_ -> p_202569_.add(ROOT_MARKER));
    public static final Marker PACKET_RECEIVED_MARKER = Util.make(MarkerFactory.getMarker("PACKET_RECEIVED"), p_202562_ -> p_202562_.add(PACKET_MARKER));
    public static final Marker PACKET_SENT_MARKER = Util.make(MarkerFactory.getMarker("PACKET_SENT"), p_202557_ -> p_202557_.add(PACKET_MARKER));
    public static final Supplier<NioEventLoopGroup> NETWORK_WORKER_GROUP = Suppliers.memoize(
        () -> new NioEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Client IO #%d").setDaemon(true).build())
    );
    public static final Supplier<EpollEventLoopGroup> NETWORK_EPOLL_WORKER_GROUP = Suppliers.memoize(
        () -> new EpollEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build())
    );
    public static final Supplier<DefaultEventLoopGroup> LOCAL_WORKER_GROUP = Suppliers.memoize(
        () -> new DefaultEventLoopGroup(0, new ThreadFactoryBuilder().setNameFormat("Netty Local Client IO #%d").setDaemon(true).build())
    );
    private static final ProtocolInfo<ServerHandshakePacketListener> INITIAL_PROTOCOL = HandshakeProtocols.SERVERBOUND;
    private final PacketFlow receiving;
    private volatile boolean sendLoginDisconnect = true;
    private final Queue<Consumer<Connection>> pendingActions = Queues.newConcurrentLinkedQueue();
    /**
     * The active channel
     */
    private Channel channel;
    /**
     * The address of the remote party
     */
    private SocketAddress address;
    @Nullable
    private volatile PacketListener disconnectListener;
    /**
     * The PacketListener instance responsible for processing received packets
     */
    @Nullable
    private volatile PacketListener packetListener;
    @Nullable
    private DisconnectionDetails disconnectionDetails;
    private boolean encrypted;
    private boolean disconnectionHandled;
    private int receivedPackets;
    private int sentPackets;
    private float averageReceivedPackets;
    private float averageSentPackets;
    private int tickCount;
    private boolean handlingFault;
    @Nullable
    private volatile DisconnectionDetails delayedDisconnect;
    @Nullable
    BandwidthDebugMonitor bandwidthDebugMonitor;
    @Nullable
    private ProtocolInfo<?> inboundProtocol;

    public Connection(PacketFlow pReceiving) {
        this.receiving = pReceiving;
    }

    @Override
    public void channelActive(ChannelHandlerContext pContext) throws Exception {
        super.channelActive(pContext);
        this.channel = pContext.channel();
        this.address = this.channel.remoteAddress();
        if (this.delayedDisconnect != null) {
            this.disconnect(this.delayedDisconnect);
        }
        net.neoforged.neoforge.network.connection.ConnectionUtils.setConnection(pContext, this);
    }

    @Override
    public void channelInactive(ChannelHandlerContext pContext) {
        this.disconnect(Component.translatable("disconnect.endOfStream"));
        net.neoforged.neoforge.network.connection.ConnectionUtils.removeConnection(pContext);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext pContext, Throwable pException) {
        if (pException instanceof SkipPacketException) {
            LOGGER.debug("Skipping packet due to errors", pException.getCause());
        } else {
            boolean flag = !this.handlingFault;
            this.handlingFault = true;
            if (this.channel.isOpen()) {
                if (pException instanceof TimeoutException) {
                    LOGGER.debug("Timeout", pException);
                    this.disconnect(Component.translatable("disconnect.timeout"));
                } else {
                    Component component = Component.translatable("disconnect.genericReason", "Internal Exception: " + pException);
                    PacketListener packetlistener = this.packetListener;
                    if (packetlistener != null) {
                        ConnectionProtocol protocol = packetlistener.protocol();
                        if (protocol == ConnectionProtocol.CONFIGURATION || protocol == ConnectionProtocol.PLAY) {
                            // Neo: Always log critical network exceptions for config and play packets
                            LOGGER.error("Exception caught in connection", pException);
                        }
                    }
                    DisconnectionDetails disconnectiondetails;
                    if (packetlistener != null) {
                        disconnectiondetails = packetlistener.createDisconnectionInfo(component, pException);
                    } else {
                        disconnectiondetails = new DisconnectionDetails(component);
                    }

                    if (flag) {
                        LOGGER.debug("Failed to sent packet", pException);
                        if (this.getSending() == PacketFlow.CLIENTBOUND) {
                            Packet<?> packet = (Packet<?>)(this.sendLoginDisconnect
                                ? new ClientboundLoginDisconnectPacket(component)
                                : new ClientboundDisconnectPacket(component));
                            this.send(packet, PacketSendListener.thenRun(() -> this.disconnect(disconnectiondetails)));
                        } else {
                            this.disconnect(disconnectiondetails);
                        }

                        this.setReadOnly();
                    } else {
                        LOGGER.debug("Double fault", pException);
                        this.disconnect(disconnectiondetails);
                    }
                }
            }
        }
    }

    protected void channelRead0(ChannelHandlerContext pContext, Packet<?> pPacket) {
        if (this.channel.isOpen()) {
            PacketListener packetlistener = this.packetListener;
            if (packetlistener == null) {
                throw new IllegalStateException("Received a packet before the packet listener was initialized");
            } else {
                if (packetlistener.shouldHandleMessage(pPacket)) {
                    try {
                        genericsFtw(pPacket, packetlistener);
                    } catch (RunningOnDifferentThreadException runningondifferentthreadexception) {
                    } catch (RejectedExecutionException rejectedexecutionexception) {
                        this.disconnect(Component.translatable("multiplayer.disconnect.server_shutdown"));
                    } catch (ClassCastException classcastexception) {
                        LOGGER.error("Received {} that couldn't be processed", pPacket.getClass(), classcastexception);
                        this.disconnect(Component.translatable("multiplayer.disconnect.invalid_packet"));
                    }

                    this.receivedPackets++;
                }
            }
        }
    }

    private static <T extends PacketListener> void genericsFtw(Packet<T> pPacket, PacketListener pListener) {
        pPacket.handle((T)pListener);
    }

    private void validateListener(ProtocolInfo<?> pProtocolInfo, PacketListener pPacketListener) {
        Validate.notNull(pPacketListener, "packetListener");
        PacketFlow packetflow = pPacketListener.flow();
        if (packetflow != this.receiving) {
            throw new IllegalStateException("Trying to set listener for wrong side: connection is " + this.receiving + ", but listener is " + packetflow);
        } else {
            ConnectionProtocol connectionprotocol = pPacketListener.protocol();
            if (pProtocolInfo.id() != connectionprotocol) {
                throw new IllegalStateException("Listener protocol (" + connectionprotocol + ") does not match requested one " + pProtocolInfo);
            }
        }
    }

    private static void syncAfterConfigurationChange(ChannelFuture pFuture) {
        try {
            pFuture.syncUninterruptibly();
        } catch (Exception exception) {
            if (exception instanceof ClosedChannelException) {
                LOGGER.info("Connection closed during protocol change");
            } else {
                throw exception;
            }
        }
    }

    public <T extends PacketListener> void setupInboundProtocol(ProtocolInfo<T> pProtocolInfo, T pPacketInfo) {
        this.validateListener(pProtocolInfo, pPacketInfo);
        if (pProtocolInfo.flow() != this.getReceiving()) {
            throw new IllegalStateException("Invalid inbound protocol: " + pProtocolInfo.id());
        } else {
            this.inboundProtocol = pProtocolInfo;
            this.packetListener = pPacketInfo;
            this.disconnectListener = null;
            UnconfiguredPipelineHandler.InboundConfigurationTask unconfiguredpipelinehandler$inboundconfigurationtask = UnconfiguredPipelineHandler.setupInboundProtocol(
                pProtocolInfo
            );
            BundlerInfo bundlerinfo = pProtocolInfo.bundlerInfo();
            if (bundlerinfo != null) {
                PacketBundlePacker packetbundlepacker = new PacketBundlePacker(bundlerinfo);
                unconfiguredpipelinehandler$inboundconfigurationtask = unconfiguredpipelinehandler$inboundconfigurationtask.andThen(
                    p_319518_ -> p_319518_.pipeline().addAfter("decoder", "bundler", packetbundlepacker)
                );
            }

            syncAfterConfigurationChange(this.channel.writeAndFlush(unconfiguredpipelinehandler$inboundconfigurationtask));
        }
    }

    public void setupOutboundProtocol(ProtocolInfo<?> pProtocolInfo) {
        if (pProtocolInfo.flow() != this.getSending()) {
            throw new IllegalStateException("Invalid outbound protocol: " + pProtocolInfo.id());
        } else {
            UnconfiguredPipelineHandler.OutboundConfigurationTask unconfiguredpipelinehandler$outboundconfigurationtask = UnconfiguredPipelineHandler.setupOutboundProtocol(
                pProtocolInfo
            );
            BundlerInfo bundlerinfo = pProtocolInfo.bundlerInfo();
            if (bundlerinfo != null) {
                PacketBundleUnpacker packetbundleunpacker = new PacketBundleUnpacker(bundlerinfo);
                unconfiguredpipelinehandler$outboundconfigurationtask = unconfiguredpipelinehandler$outboundconfigurationtask.andThen(
                    p_319516_ -> {
                        p_319516_.pipeline().addAfter("encoder", "unbundler", packetbundleunpacker);
                        // Neo: our handlers must be between the encoder and the unbundler, so re-inject them
                        // Note, this call must be inside the .andThen lambda, or it will actually run before the unbundler gets added.
                        net.neoforged.neoforge.network.filters.NetworkFilters.injectIfNecessary(this);
                    }
                );
            }

            boolean flag = pProtocolInfo.id() == ConnectionProtocol.LOGIN;
            syncAfterConfigurationChange(
                this.channel.writeAndFlush(unconfiguredpipelinehandler$outboundconfigurationtask.andThen(p_319527_ -> this.sendLoginDisconnect = flag))
            );
        }
    }

    public void setListenerForServerboundHandshake(PacketListener pPacketListener) {
        if (this.packetListener != null) {
            throw new IllegalStateException("Listener already set");
        } else if (this.receiving == PacketFlow.SERVERBOUND && pPacketListener.flow() == PacketFlow.SERVERBOUND && pPacketListener.protocol() == INITIAL_PROTOCOL.id()) {
            this.packetListener = pPacketListener;
        } else {
            throw new IllegalStateException("Invalid initial listener");
        }
    }

    public void initiateServerboundStatusConnection(String pHostName, int pPort, ClientStatusPacketListener pDisconnectListener) {
        this.initiateServerboundConnection(pHostName, pPort, StatusProtocols.SERVERBOUND, StatusProtocols.CLIENTBOUND, pDisconnectListener, ClientIntent.STATUS);
    }

    public void initiateServerboundPlayConnection(String pHostName, int pPort, ClientLoginPacketListener pDisconnectListener) {
        this.initiateServerboundConnection(pHostName, pPort, LoginProtocols.SERVERBOUND, LoginProtocols.CLIENTBOUND, pDisconnectListener, ClientIntent.LOGIN);
    }

    public <S extends ServerboundPacketListener, C extends ClientboundPacketListener> void initiateServerboundPlayConnection(
        String pHostName, int pPort, ProtocolInfo<S> pServerboundProtocol, ProtocolInfo<C> pClientbountProtocol, C pDisconnectListener, boolean pIsTransfer
    ) {
        this.initiateServerboundConnection(pHostName, pPort, pServerboundProtocol, pClientbountProtocol, pDisconnectListener, pIsTransfer ? ClientIntent.TRANSFER : ClientIntent.LOGIN);
    }

    private <S extends ServerboundPacketListener, C extends ClientboundPacketListener> void initiateServerboundConnection(
        String pHostName, int pPort, ProtocolInfo<S> pServerboundProtocol, ProtocolInfo<C> pClientboundProtocol, C pDisconnectListener, ClientIntent pIntention
    ) {
        if (pServerboundProtocol.id() != pClientboundProtocol.id()) {
            throw new IllegalStateException("Mismatched initial protocols");
        } else {
            this.disconnectListener = pDisconnectListener;
            this.runOnceConnected(
                p_319525_ -> {
                    this.setupInboundProtocol(pClientboundProtocol, pDisconnectListener);
                    p_319525_.sendPacket(
                        new ClientIntentionPacket(SharedConstants.getCurrentVersion().getProtocolVersion(), pHostName, pPort, pIntention), null, true
                    );
                    this.setupOutboundProtocol(pServerboundProtocol);
                }
            );
        }
    }

    public void send(Packet<?> pPacket) {
        this.send(pPacket, null);
    }

    public void send(Packet<?> pPacket, @Nullable PacketSendListener pSendListener) {
        this.send(pPacket, pSendListener, true);
    }

    public void send(Packet<?> pPacket, @Nullable PacketSendListener pListener, boolean pFlush) {
        if (this.isConnected()) {
            this.flushQueue();
            this.sendPacket(pPacket, pListener, pFlush);
        } else {
            this.pendingActions.add(p_293706_ -> p_293706_.sendPacket(pPacket, pListener, pFlush));
        }
    }

    public void runOnceConnected(Consumer<Connection> pAction) {
        if (this.isConnected()) {
            this.flushQueue();
            pAction.accept(this);
        } else {
            this.pendingActions.add(pAction);
        }
    }

    private void sendPacket(Packet<?> pPacket, @Nullable PacketSendListener pSendListener, boolean pFlush) {
        this.sentPackets++;
        if (this.channel.eventLoop().inEventLoop()) {
            this.doSendPacket(pPacket, pSendListener, pFlush);
        } else {
            this.channel.eventLoop().execute(() -> this.doSendPacket(pPacket, pSendListener, pFlush));
        }
    }

    private void doSendPacket(Packet<?> pPacket, @Nullable PacketSendListener pSendListener, boolean pFlush) {
        ChannelFuture channelfuture = pFlush ? this.channel.writeAndFlush(pPacket) : this.channel.write(pPacket);
        if (pSendListener != null) {
            channelfuture.addListener(p_243167_ -> {
                if (p_243167_.isSuccess()) {
                    pSendListener.onSuccess();
                } else {
                    Packet<?> packet = pSendListener.onFailure();
                    if (packet != null) {
                        ChannelFuture channelfuture1 = this.channel.writeAndFlush(packet);
                        channelfuture1.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                    }
                }
            });
        }

        channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
    }

    public void flushChannel() {
        if (this.isConnected()) {
            this.flush();
        } else {
            this.pendingActions.add(Connection::flush);
        }
    }

    private void flush() {
        if (this.channel.eventLoop().inEventLoop()) {
            this.channel.flush();
        } else {
            this.channel.eventLoop().execute(() -> this.channel.flush());
        }
    }

    private void flushQueue() {
        if (this.channel != null && this.channel.isOpen()) {
            synchronized (this.pendingActions) {
                Consumer<Connection> consumer;
                while ((consumer = this.pendingActions.poll()) != null) {
                    consumer.accept(this);
                }
            }
        }
    }

    public void tick() {
        this.flushQueue();
        if (this.packetListener instanceof TickablePacketListener tickablepacketlistener) {
            tickablepacketlistener.tick();
        }

        if (!this.isConnected() && !this.disconnectionHandled) {
            this.handleDisconnection();
        }

        if (this.channel != null) {
            this.channel.flush();
        }

        if (this.tickCount++ % 20 == 0) {
            this.tickSecond();
        }

        if (this.bandwidthDebugMonitor != null) {
            this.bandwidthDebugMonitor.tick();
        }
    }

    protected void tickSecond() {
        this.averageSentPackets = Mth.lerp(0.75F, (float)this.sentPackets, this.averageSentPackets);
        this.averageReceivedPackets = Mth.lerp(0.75F, (float)this.receivedPackets, this.averageReceivedPackets);
        this.sentPackets = 0;
        this.receivedPackets = 0;
    }

    public SocketAddress getRemoteAddress() {
        return this.address;
    }

    public String getLoggableAddress(boolean pLogIps) {
        if (this.address == null) {
            return "local";
        } else {
            return pLogIps ? net.neoforged.neoforge.network.DualStackUtils.getAddressString(this.address) : "IP hidden";
        }
    }

    /**
     * Closes the channel with a given reason. The reason is stored for later and will be used for informational purposes (info log on server,
     * disconnection screen on the client). This method is also called on the client when the server requests disconnection via
     * {@code ClientboundDisconnectPacket}.
     *
     * Closing the channel this way does not send any disconnection packets, it simply terminates the underlying netty channel.
     */
    public void disconnect(Component pMessage) {
        this.disconnect(new DisconnectionDetails(pMessage));
    }

    public void disconnect(DisconnectionDetails p_350867_) {
        if (this.channel == null) {
            this.delayedDisconnect = p_350867_;
        }

        if (this.isConnected()) {
            this.channel.close().awaitUninterruptibly();
            this.disconnectionDetails = p_350867_;
        }
    }

    public boolean isMemoryConnection() {
        return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
    }

    public PacketFlow getReceiving() {
        return this.receiving;
    }

    public PacketFlow getSending() {
        return this.receiving.getOpposite();
    }

    public static Connection connectToServer(InetSocketAddress pAddress, boolean pUseEpollIfAvailable, @Nullable LocalSampleLogger pSampleLogger) {
        Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        if (pSampleLogger != null) {
            connection.setBandwidthLogger(pSampleLogger);
        }

        ChannelFuture channelfuture = connect(pAddress, pUseEpollIfAvailable, connection);
        channelfuture.syncUninterruptibly();
        return connection;
    }

    public static ChannelFuture connect(InetSocketAddress pAddress, boolean pUseEpollIfAvailable, final Connection pConnection) {
        net.neoforged.neoforge.network.DualStackUtils.checkIPv6(pAddress.getAddress());
        Class<? extends SocketChannel> oclass;
        EventLoopGroup eventloopgroup;
        if (Epoll.isAvailable() && pUseEpollIfAvailable) {
            oclass = EpollSocketChannel.class;
            eventloopgroup = NETWORK_EPOLL_WORKER_GROUP.get();
        } else {
            oclass = NioSocketChannel.class;
            eventloopgroup = NETWORK_WORKER_GROUP.get();
        }

        return new Bootstrap().group(eventloopgroup).handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel p_129552_) {
                try {
                    p_129552_.config().setOption(ChannelOption.TCP_NODELAY, true);
                } catch (ChannelException channelexception) {
                }

                ChannelPipeline channelpipeline = p_129552_.pipeline().addLast("timeout", new ReadTimeoutHandler(30));
                Connection.configureSerialization(channelpipeline, PacketFlow.CLIENTBOUND, false, pConnection.bandwidthDebugMonitor);
                pConnection.configurePacketHandler(channelpipeline);
            }
        }).channel(oclass).connect(pAddress.getAddress(), pAddress.getPort());
    }

    private static String outboundHandlerName(boolean pClientbound) {
        return pClientbound ? "encoder" : "outbound_config";
    }

    private static String inboundHandlerName(boolean pServerbound) {
        return pServerbound ? "decoder" : "inbound_config";
    }

    public void configurePacketHandler(ChannelPipeline pPipeline) {
        pPipeline.addLast("hackfix", new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext p_320587_, Object p_320392_, ChannelPromise p_320515_) throws Exception {
                super.write(p_320587_, p_320392_, p_320515_);
            }
        }).addLast("packet_handler", this);
    }

    public static void configureSerialization(ChannelPipeline pPipeline, PacketFlow pFlow, boolean p_341592_, @Nullable BandwidthDebugMonitor pBandwithDebugMonitor) {
        PacketFlow packetflow = pFlow.getOpposite();
        boolean flag = pFlow == PacketFlow.SERVERBOUND;
        boolean flag1 = packetflow == PacketFlow.SERVERBOUND;
        pPipeline.addLast("splitter", createFrameDecoder(pBandwithDebugMonitor, p_341592_))
            .addLast(new FlowControlHandler())
            .addLast(inboundHandlerName(flag), (ChannelHandler)(flag ? new PacketDecoder<>(INITIAL_PROTOCOL) : new UnconfiguredPipelineHandler.Inbound()))
            .addLast("prepender", createFrameEncoder(p_341592_))
            .addLast(outboundHandlerName(flag1), (ChannelHandler)(flag1 ? new PacketEncoder<>(INITIAL_PROTOCOL) : new UnconfiguredPipelineHandler.Outbound()));
    }

    private static ChannelOutboundHandler createFrameEncoder(boolean p_341616_) {
        return (ChannelOutboundHandler)(p_341616_ ? new NoOpFrameEncoder() : new Varint21LengthFieldPrepender());
    }

    private static ChannelInboundHandler createFrameDecoder(@Nullable BandwidthDebugMonitor pBandwithDebugMonitor, boolean p_341702_) {
        if (!p_341702_) {
            return new Varint21FrameDecoder(pBandwithDebugMonitor);
        } else {
            return (ChannelInboundHandler)(pBandwithDebugMonitor != null ? new MonitorFrameDecoder(pBandwithDebugMonitor) : new NoOpFrameDecoder());
        }
    }

    public static void configureInMemoryPipeline(ChannelPipeline pPipeline, PacketFlow pFlow) {
        configureSerialization(pPipeline, pFlow, true, null);
    }

    /**
     * Prepares a clientside Connection for a local in-memory connection ("single player").
     * Establishes a connection to the socket supplied and configures the channel pipeline (only the packet handler is necessary,
     * since this is for an in-memory connection). Returns the newly created instance.
     */
    public static Connection connectToLocalServer(SocketAddress pAddress) {
        final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
        new Bootstrap().group(LOCAL_WORKER_GROUP.get()).handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel p_320092_) {
                ChannelPipeline channelpipeline = p_320092_.pipeline();
                Connection.configureInMemoryPipeline(channelpipeline, PacketFlow.CLIENTBOUND);
                connection.configurePacketHandler(channelpipeline);
            }
        }).channel(LocalChannel.class).connect(pAddress).syncUninterruptibly();
        return connection;
    }

    /**
     * Enables encryption for this connection using the given decrypting and encrypting ciphers.
     * This adds new handlers to this connection's pipeline which handle the decrypting and encrypting.
     * This happens as part of the normal network handshake.
     *
     * @see net.minecraft.network.protocol.login.ClientboundHelloPacket
     * @see net.minecraft.network.protocol.login.ServerboundKeyPacket
     */
    public void setEncryptionKey(Cipher pDecryptingCipher, Cipher pEncryptingCipher) {
        this.encrypted = true;
        this.channel.pipeline().addBefore("splitter", "decrypt", new CipherDecoder(pDecryptingCipher));
        this.channel.pipeline().addBefore("prepender", "encrypt", new CipherEncoder(pEncryptingCipher));
    }

    public boolean isEncrypted() {
        return this.encrypted;
    }

    public boolean isConnected() {
        return this.channel != null && this.channel.isOpen();
    }

    public boolean isConnecting() {
        return this.channel == null;
    }

    @Nullable
    public PacketListener getPacketListener() {
        return this.packetListener;
    }

    @Nullable
    public DisconnectionDetails getDisconnectionDetails() {
        return this.disconnectionDetails;
    }

    public void setReadOnly() {
        if (this.channel != null) {
            this.channel.config().setAutoRead(false);
        }
    }

    /**
     * Enables or disables compression for this connection. If {@code threshold} is >= 0 then a {@link CompressionDecoder} and {@link CompressionEncoder}
     * are installed in the pipeline or updated if they already exist. If {@code threshold} is < 0 then any such codec are removed.
     *
     * Compression is enabled as part of the connection handshake when the server sends {@link net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket}.
     */
    public void setupCompression(int pThreshold, boolean pValidateDecompressed) {
        if (pThreshold >= 0) {
            if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder compressiondecoder) {
                compressiondecoder.setThreshold(pThreshold, pValidateDecompressed);
            } else {
                this.channel.pipeline().addAfter("splitter", "decompress", new CompressionDecoder(pThreshold, pValidateDecompressed));
            }

            if (this.channel.pipeline().get("compress") instanceof CompressionEncoder compressionencoder) {
                compressionencoder.setThreshold(pThreshold);
            } else {
                this.channel.pipeline().addAfter("prepender", "compress", new CompressionEncoder(pThreshold));
            }
        } else {
            if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
                this.channel.pipeline().remove("decompress");
            }

            if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
                this.channel.pipeline().remove("compress");
            }
        }
    }

    public void handleDisconnection() {
        if (this.channel != null && !this.channel.isOpen()) {
            if (this.disconnectionHandled) {
                LOGGER.warn("handleDisconnection() called twice");
            } else {
                this.disconnectionHandled = true;
                PacketListener packetlistener = this.getPacketListener();
                PacketListener packetlistener1 = packetlistener != null ? packetlistener : this.disconnectListener;
                if (packetlistener1 != null) {
                    DisconnectionDetails disconnectiondetails = Objects.requireNonNullElseGet(
                        this.getDisconnectionDetails(), () -> new DisconnectionDetails(Component.translatable("multiplayer.disconnect.generic"))
                    );
                    packetlistener1.onDisconnect(disconnectiondetails);
                }
            }
        }
    }

    public float getAverageReceivedPackets() {
        return this.averageReceivedPackets;
    }

    public float getAverageSentPackets() {
        return this.averageSentPackets;
    }

    public void setBandwidthLogger(LocalSampleLogger pBandwithLogger) {
        this.bandwidthDebugMonitor = new BandwidthDebugMonitor(pBandwithLogger);
    }

    public Channel channel() {
        return this.channel;
    }

    public PacketFlow getDirection() {
        return this.receiving;
    }

    public ProtocolInfo<?> getInboundProtocol() {
        return Objects.requireNonNull(this.inboundProtocol, "Inbound protocol not set?");
    }
}

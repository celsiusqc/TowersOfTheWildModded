package net.minecraft.world.level.saveddata.maps;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import io.netty.buffer.ByteBuf;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.MapDecorations;
import net.minecraft.world.item.component.MapItemColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

public class MapItemSavedData extends SavedData {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MAP_SIZE = 128;
    private static final int HALF_MAP_SIZE = 64;
    public static final int MAX_SCALE = 4;
    public static final int TRACKED_DECORATION_LIMIT = 256;
    private static final String FRAME_PREFIX = "frame-";
    public final int centerX;
    public final int centerZ;
    public final ResourceKey<Level> dimension;
    private final boolean trackingPosition;
    private final boolean unlimitedTracking;
    public final byte scale;
    public byte[] colors = new byte[16384];
    public final boolean locked;
    private final List<MapItemSavedData.HoldingPlayer> carriedBy = Lists.newArrayList();
    private final Map<Player, MapItemSavedData.HoldingPlayer> carriedByPlayers = Maps.newHashMap();
    private final Map<String, MapBanner> bannerMarkers = Maps.newHashMap();
    final Map<String, MapDecoration> decorations = Maps.newLinkedHashMap();
    private final Map<String, MapFrame> frameMarkers = Maps.newHashMap();
    private int trackedDecorationCount;

    public static SavedData.Factory<MapItemSavedData> factory() {
        return new SavedData.Factory<>(() -> {
            throw new IllegalStateException("Should never create an empty map saved data");
        }, MapItemSavedData::load, DataFixTypes.SAVED_DATA_MAP_DATA);
    }

    private MapItemSavedData(
        int pX, int pZ, byte pScale, boolean pTrackingPosition, boolean pUnlimitedTracking, boolean pLocked, ResourceKey<Level> pDimension
    ) {
        this.scale = pScale;
        this.centerX = pX;
        this.centerZ = pZ;
        this.dimension = pDimension;
        this.trackingPosition = pTrackingPosition;
        this.unlimitedTracking = pUnlimitedTracking;
        this.locked = pLocked;
        this.setDirty();
    }

    public static MapItemSavedData createFresh(
        double pX, double pZ, byte pScale, boolean pTrackingPosition, boolean pUnlimitedTracking, ResourceKey<Level> pDimension
    ) {
        int i = 128 * (1 << pScale);
        int j = Mth.floor((pX + 64.0) / (double)i);
        int k = Mth.floor((pZ + 64.0) / (double)i);
        int l = j * i + i / 2 - 64;
        int i1 = k * i + i / 2 - 64;
        return new MapItemSavedData(l, i1, pScale, pTrackingPosition, pUnlimitedTracking, false, pDimension);
    }

    public static MapItemSavedData createForClient(byte pScale, boolean pLocked, ResourceKey<Level> pDimension) {
        return new MapItemSavedData(0, 0, pScale, false, false, pLocked, pDimension);
    }

    public static MapItemSavedData load(CompoundTag p_164808_, HolderLookup.Provider p_324560_) {
        ResourceKey<Level> resourcekey = DimensionType.parseLegacy(new Dynamic<>(NbtOps.INSTANCE, p_164808_.get("dimension")))
            .resultOrPartial(LOGGER::error)
            .orElseThrow(() -> new IllegalArgumentException("Invalid map dimension: " + p_164808_.get("dimension")));
        int i = p_164808_.getInt("xCenter");
        int j = p_164808_.getInt("zCenter");
        byte b0 = (byte)Mth.clamp(p_164808_.getByte("scale"), 0, 4);
        boolean flag = !p_164808_.contains("trackingPosition", 1) || p_164808_.getBoolean("trackingPosition");
        boolean flag1 = p_164808_.getBoolean("unlimitedTracking");
        boolean flag2 = p_164808_.getBoolean("locked");
        MapItemSavedData mapitemsaveddata = new MapItemSavedData(i, j, b0, flag, flag1, flag2, resourcekey);
        byte[] abyte = p_164808_.getByteArray("colors");
        if (abyte.length == 16384) {
            mapitemsaveddata.colors = abyte;
        }

        RegistryOps<Tag> registryops = p_324560_.createSerializationContext(NbtOps.INSTANCE);

        for (MapBanner mapbanner : MapBanner.LIST_CODEC
            .parse(registryops, p_164808_.get("banners"))
            .resultOrPartial(p_323448_ -> LOGGER.warn("Failed to parse map banner: '{}'", p_323448_))
            .orElse(List.of())) {
            mapitemsaveddata.bannerMarkers.put(mapbanner.getId(), mapbanner);
            mapitemsaveddata.addDecoration(
                mapbanner.getDecoration(),
                null,
                mapbanner.getId(),
                (double)mapbanner.pos().getX(),
                (double)mapbanner.pos().getZ(),
                180.0,
                mapbanner.name().orElse(null)
            );
        }

        ListTag listtag = p_164808_.getList("frames", 10);

        for (int k = 0; k < listtag.size(); k++) {
            MapFrame mapframe = MapFrame.load(listtag.getCompound(k));
            if (mapframe != null) {
                mapitemsaveddata.frameMarkers.put(mapframe.getId(), mapframe);
                mapitemsaveddata.addDecoration(
                    MapDecorationTypes.FRAME,
                    null,
                    getFrameKey(mapframe.getEntityId()),
                    (double)mapframe.getPos().getX(),
                    (double)mapframe.getPos().getZ(),
                    (double)mapframe.getRotation(),
                    null
                );
            }
        }

        return mapitemsaveddata;
    }

    @Override
    public CompoundTag save(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        ResourceLocation.CODEC
            .encodeStart(NbtOps.INSTANCE, this.dimension.location())
            .resultOrPartial(LOGGER::error)
            .ifPresent(p_77954_ -> pTag.put("dimension", p_77954_));
        pTag.putInt("xCenter", this.centerX);
        pTag.putInt("zCenter", this.centerZ);
        pTag.putByte("scale", this.scale);
        pTag.putByteArray("colors", this.colors);
        pTag.putBoolean("trackingPosition", this.trackingPosition);
        pTag.putBoolean("unlimitedTracking", this.unlimitedTracking);
        pTag.putBoolean("locked", this.locked);
        RegistryOps<Tag> registryops = pRegistries.createSerializationContext(NbtOps.INSTANCE);
        pTag.put("banners", MapBanner.LIST_CODEC.encodeStart(registryops, List.copyOf(this.bannerMarkers.values())).getOrThrow());
        ListTag listtag = new ListTag();

        for (MapFrame mapframe : this.frameMarkers.values()) {
            listtag.add(mapframe.save());
        }

        pTag.put("frames", listtag);
        return pTag;
    }

    public MapItemSavedData locked() {
        MapItemSavedData mapitemsaveddata = new MapItemSavedData(
            this.centerX, this.centerZ, this.scale, this.trackingPosition, this.unlimitedTracking, true, this.dimension
        );
        mapitemsaveddata.bannerMarkers.putAll(this.bannerMarkers);
        mapitemsaveddata.decorations.putAll(this.decorations);
        mapitemsaveddata.trackedDecorationCount = this.trackedDecorationCount;
        System.arraycopy(this.colors, 0, mapitemsaveddata.colors, 0, this.colors.length);
        mapitemsaveddata.setDirty();
        return mapitemsaveddata;
    }

    public MapItemSavedData scaled() {
        return createFresh(
            (double)this.centerX, (double)this.centerZ, (byte)Mth.clamp(this.scale + 1, 0, 4), this.trackingPosition, this.unlimitedTracking, this.dimension
        );
    }

    private static Predicate<ItemStack> mapMatcher(ItemStack pStack) {
        MapId mapid = pStack.get(DataComponents.MAP_ID);
        return p_330169_ -> p_330169_ == pStack ? true : p_330169_.is(pStack.getItem()) && Objects.equals(mapid, p_330169_.get(DataComponents.MAP_ID));
    }

    /**
     * Adds the player passed to the list of visible players and checks to see which players are visible
     */
    public void tickCarriedBy(Player pPlayer, ItemStack pMapStack) {
        if (!this.carriedByPlayers.containsKey(pPlayer)) {
            MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = new MapItemSavedData.HoldingPlayer(pPlayer);
            this.carriedByPlayers.put(pPlayer, mapitemsaveddata$holdingplayer);
            this.carriedBy.add(mapitemsaveddata$holdingplayer);
        }

        Predicate<ItemStack> predicate = mapMatcher(pMapStack);
        if (!pPlayer.getInventory().contains(predicate)) {
            this.removeDecoration(pPlayer.getName().getString());
        }

        for (int i = 0; i < this.carriedBy.size(); i++) {
            MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer1 = this.carriedBy.get(i);
            String s = mapitemsaveddata$holdingplayer1.player.getName().getString();
            if (!mapitemsaveddata$holdingplayer1.player.isRemoved()
                && (mapitemsaveddata$holdingplayer1.player.getInventory().contains(predicate) || pMapStack.isFramed())) {
                if (!pMapStack.isFramed() && mapitemsaveddata$holdingplayer1.player.level().dimension() == this.dimension && this.trackingPosition) {
                    this.addDecoration(
                        MapDecorationTypes.PLAYER,
                        mapitemsaveddata$holdingplayer1.player.level(),
                        s,
                        mapitemsaveddata$holdingplayer1.player.getX(),
                        mapitemsaveddata$holdingplayer1.player.getZ(),
                        (double)mapitemsaveddata$holdingplayer1.player.getYRot(),
                        null
                    );
                }
            } else {
                this.carriedByPlayers.remove(mapitemsaveddata$holdingplayer1.player);
                this.carriedBy.remove(mapitemsaveddata$holdingplayer1);
                this.removeDecoration(s);
            }
        }

        if (pMapStack.isFramed() && this.trackingPosition) {
            ItemFrame itemframe = pMapStack.getFrame();
            BlockPos blockpos = itemframe.getPos();
            MapFrame mapframe1 = this.frameMarkers.get(MapFrame.frameId(blockpos));
            if (mapframe1 != null && itemframe.getId() != mapframe1.getEntityId() && this.frameMarkers.containsKey(mapframe1.getId())) {
                this.removeDecoration(getFrameKey(mapframe1.getEntityId()));
            }

            MapFrame mapframe = new MapFrame(blockpos, itemframe.getDirection().get2DDataValue() * 90, itemframe.getId());
            this.addDecoration(
                MapDecorationTypes.FRAME,
                pPlayer.level(),
                getFrameKey(itemframe.getId()),
                (double)blockpos.getX(),
                (double)blockpos.getZ(),
                (double)(itemframe.getDirection().get2DDataValue() * 90),
                null
            );
            this.frameMarkers.put(mapframe.getId(), mapframe);
        }

        MapDecorations mapdecorations = pMapStack.getOrDefault(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY);
        if (!this.decorations.keySet().containsAll(mapdecorations.decorations().keySet())) {
            mapdecorations.decorations().forEach((p_352892_, p_352893_) -> {
                if (!this.decorations.containsKey(p_352892_)) {
                    this.addDecoration(p_352893_.type(), pPlayer.level(), p_352892_, p_352893_.x(), p_352893_.z(), (double)p_352893_.rotation(), null);
                }
            });
        }
    }

    private void removeDecoration(String pIdentifier) {
        MapDecoration mapdecoration = this.decorations.remove(pIdentifier);
        if (mapdecoration != null && mapdecoration.type().value().trackCount()) {
            this.trackedDecorationCount--;
        }

        this.setDecorationsDirty();
    }

    public static void addTargetDecoration(ItemStack pStack, BlockPos pPos, String pType, Holder<MapDecorationType> pMapDecorationType) {
        MapDecorations.Entry mapdecorations$entry = new MapDecorations.Entry(pMapDecorationType, (double)pPos.getX(), (double)pPos.getZ(), 180.0F);
        pStack.update(DataComponents.MAP_DECORATIONS, MapDecorations.EMPTY, p_330166_ -> p_330166_.withDecoration(pType, mapdecorations$entry));
        if (pMapDecorationType.value().hasMapColor()) {
            pStack.set(DataComponents.MAP_COLOR, new MapItemColor(pMapDecorationType.value().mapColor()));
        }
    }

    private void addDecoration(
        Holder<MapDecorationType> pDecorationType,
        @Nullable LevelAccessor pLevel,
        String pId,
        double pX,
        double pZ,
        double pYRot,
        @Nullable Component pDisplayName
    ) {
        int i = 1 << this.scale;
        float f = (float)(pX - (double)this.centerX) / (float)i;
        float f1 = (float)(pZ - (double)this.centerZ) / (float)i;
        byte b0 = (byte)((int)((double)(f * 2.0F) + 0.5));
        byte b1 = (byte)((int)((double)(f1 * 2.0F) + 0.5));
        int j = 63;
        byte b2;
        if (f >= -63.0F && f1 >= -63.0F && f <= 63.0F && f1 <= 63.0F) {
            pYRot += pYRot < 0.0 ? -8.0 : 8.0;
            b2 = (byte)((int)(pYRot * 16.0 / 360.0));
            if (this.dimension == Level.NETHER && pLevel != null) {
                int l = (int)(pLevel.getLevelData().getDayTime() / 10L);
                b2 = (byte)(l * l * 34187121 + l * 121 >> 15 & 15);
            }
        } else {
            if (!pDecorationType.is(MapDecorationTypes.PLAYER)) {
                this.removeDecoration(pId);
                return;
            }

            int k = 320;
            if (Math.abs(f) < 320.0F && Math.abs(f1) < 320.0F) {
                pDecorationType = MapDecorationTypes.PLAYER_OFF_MAP;
            } else {
                if (!this.unlimitedTracking) {
                    this.removeDecoration(pId);
                    return;
                }

                pDecorationType = MapDecorationTypes.PLAYER_OFF_LIMITS;
            }

            b2 = 0;
            if (f <= -63.0F) {
                b0 = -128;
            }

            if (f1 <= -63.0F) {
                b1 = -128;
            }

            if (f >= 63.0F) {
                b0 = 127;
            }

            if (f1 >= 63.0F) {
                b1 = 127;
            }
        }

        MapDecoration mapdecoration1 = new MapDecoration(pDecorationType, b0, b1, b2, Optional.ofNullable(pDisplayName));
        MapDecoration mapdecoration = this.decorations.put(pId, mapdecoration1);
        if (!mapdecoration1.equals(mapdecoration)) {
            if (mapdecoration != null && mapdecoration.type().value().trackCount()) {
                this.trackedDecorationCount--;
            }

            if (pDecorationType.value().trackCount()) {
                this.trackedDecorationCount++;
            }

            this.setDecorationsDirty();
        }
    }

    @Nullable
    public Packet<?> getUpdatePacket(MapId pMapId, Player pPlayer) {
        MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = this.carriedByPlayers.get(pPlayer);
        return mapitemsaveddata$holdingplayer == null ? null : mapitemsaveddata$holdingplayer.nextUpdatePacket(pMapId);
    }

    private void setColorsDirty(int pX, int pZ) {
        this.setDirty();

        for (MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer : this.carriedBy) {
            mapitemsaveddata$holdingplayer.markColorsDirty(pX, pZ);
        }
    }

    private void setDecorationsDirty() {
        this.setDirty();
        this.carriedBy.forEach(MapItemSavedData.HoldingPlayer::markDecorationsDirty);
    }

    public MapItemSavedData.HoldingPlayer getHoldingPlayer(Player pPlayer) {
        MapItemSavedData.HoldingPlayer mapitemsaveddata$holdingplayer = this.carriedByPlayers.get(pPlayer);
        if (mapitemsaveddata$holdingplayer == null) {
            mapitemsaveddata$holdingplayer = new MapItemSavedData.HoldingPlayer(pPlayer);
            this.carriedByPlayers.put(pPlayer, mapitemsaveddata$holdingplayer);
            this.carriedBy.add(mapitemsaveddata$holdingplayer);
        }

        return mapitemsaveddata$holdingplayer;
    }

    public boolean toggleBanner(LevelAccessor pAccessor, BlockPos pPos) {
        double d0 = (double)pPos.getX() + 0.5;
        double d1 = (double)pPos.getZ() + 0.5;
        int i = 1 << this.scale;
        double d2 = (d0 - (double)this.centerX) / (double)i;
        double d3 = (d1 - (double)this.centerZ) / (double)i;
        int j = 63;
        if (d2 >= -63.0 && d3 >= -63.0 && d2 <= 63.0 && d3 <= 63.0) {
            MapBanner mapbanner = MapBanner.fromWorld(pAccessor, pPos);
            if (mapbanner == null) {
                return false;
            }

            if (this.bannerMarkers.remove(mapbanner.getId(), mapbanner)) {
                this.removeDecoration(mapbanner.getId());
                return true;
            }

            if (!this.isTrackedCountOverLimit(256)) {
                this.bannerMarkers.put(mapbanner.getId(), mapbanner);
                this.addDecoration(mapbanner.getDecoration(), pAccessor, mapbanner.getId(), d0, d1, 180.0, mapbanner.name().orElse(null));
                return true;
            }
        }

        return false;
    }

    public void checkBanners(BlockGetter pReader, int pX, int pZ) {
        Iterator<MapBanner> iterator = this.bannerMarkers.values().iterator();

        while (iterator.hasNext()) {
            MapBanner mapbanner = iterator.next();
            if (mapbanner.pos().getX() == pX && mapbanner.pos().getZ() == pZ) {
                MapBanner mapbanner1 = MapBanner.fromWorld(pReader, mapbanner.pos());
                if (!mapbanner.equals(mapbanner1)) {
                    iterator.remove();
                    this.removeDecoration(mapbanner.getId());
                }
            }
        }
    }

    public Collection<MapBanner> getBanners() {
        return this.bannerMarkers.values();
    }

    public void removedFromFrame(BlockPos pPos, int pEntityId) {
        this.removeDecoration(getFrameKey(pEntityId));
        this.frameMarkers.remove(MapFrame.frameId(pPos));
    }

    public boolean updateColor(int pX, int pZ, byte pColor) {
        byte b0 = this.colors[pX + pZ * 128];
        if (b0 != pColor) {
            this.setColor(pX, pZ, pColor);
            return true;
        } else {
            return false;
        }
    }

    public void setColor(int pX, int pZ, byte pColor) {
        this.colors[pX + pZ * 128] = pColor;
        this.setColorsDirty(pX, pZ);
    }

    public boolean isExplorationMap() {
        for (MapDecoration mapdecoration : this.decorations.values()) {
            if (mapdecoration.type().value().explorationMapElement()) {
                return true;
            }
        }

        return false;
    }

    public void addClientSideDecorations(List<MapDecoration> pDecorations) {
        this.decorations.clear();
        this.trackedDecorationCount = 0;

        for (int i = 0; i < pDecorations.size(); i++) {
            MapDecoration mapdecoration = pDecorations.get(i);
            this.decorations.put("icon-" + i, mapdecoration);
            if (mapdecoration.type().value().trackCount()) {
                this.trackedDecorationCount++;
            }
        }
    }

    public Iterable<MapDecoration> getDecorations() {
        return this.decorations.values();
    }

    public boolean isTrackedCountOverLimit(int pTrackedCount) {
        return this.trackedDecorationCount >= pTrackedCount;
    }

    private static String getFrameKey(int p_353065_) {
        return "frame-" + p_353065_;
    }

    public class HoldingPlayer {
        public final Player player;
        private boolean dirtyData = true;
        /**
         * The lowest dirty x value
         */
        private int minDirtyX;
        /**
         * The lowest dirty z value
         */
        private int minDirtyY;
        /**
         * The highest dirty x value
         */
        private int maxDirtyX = 127;
        /**
         * The highest dirty z value
         */
        private int maxDirtyY = 127;
        private boolean dirtyDecorations = true;
        private int tick;
        public int step;

        HoldingPlayer(Player pPlayer) {
            this.player = pPlayer;
        }

        private MapItemSavedData.MapPatch createPatch() {
            int i = this.minDirtyX;
            int j = this.minDirtyY;
            int k = this.maxDirtyX + 1 - this.minDirtyX;
            int l = this.maxDirtyY + 1 - this.minDirtyY;
            byte[] abyte = new byte[k * l];

            for (int i1 = 0; i1 < k; i1++) {
                for (int j1 = 0; j1 < l; j1++) {
                    abyte[i1 + j1 * k] = MapItemSavedData.this.colors[i + i1 + (j + j1) * 128];
                }
            }

            return new MapItemSavedData.MapPatch(i, j, k, l, abyte);
        }

        @Nullable
        Packet<?> nextUpdatePacket(MapId pMapId) {
            MapItemSavedData.MapPatch mapitemsaveddata$mappatch;
            if (this.dirtyData) {
                this.dirtyData = false;
                mapitemsaveddata$mappatch = this.createPatch();
            } else {
                mapitemsaveddata$mappatch = null;
            }

            Collection<MapDecoration> collection;
            if (this.dirtyDecorations && this.tick++ % 5 == 0) {
                this.dirtyDecorations = false;
                collection = MapItemSavedData.this.decorations.values();
            } else {
                collection = null;
            }

            return collection == null && mapitemsaveddata$mappatch == null
                ? null
                : new ClientboundMapItemDataPacket(pMapId, MapItemSavedData.this.scale, MapItemSavedData.this.locked, collection, mapitemsaveddata$mappatch);
        }

        void markColorsDirty(int pX, int pZ) {
            if (this.dirtyData) {
                this.minDirtyX = Math.min(this.minDirtyX, pX);
                this.minDirtyY = Math.min(this.minDirtyY, pZ);
                this.maxDirtyX = Math.max(this.maxDirtyX, pX);
                this.maxDirtyY = Math.max(this.maxDirtyY, pZ);
            } else {
                this.dirtyData = true;
                this.minDirtyX = pX;
                this.minDirtyY = pZ;
                this.maxDirtyX = pX;
                this.maxDirtyY = pZ;
            }
        }

        private void markDecorationsDirty() {
            this.dirtyDecorations = true;
        }
    }

    public static record MapPatch(int startX, int startY, int width, int height, byte[] mapColors) {
        public static final StreamCodec<ByteBuf, Optional<MapItemSavedData.MapPatch>> STREAM_CODEC = StreamCodec.of(
            MapItemSavedData.MapPatch::write, MapItemSavedData.MapPatch::read
        );

        private static void write(ByteBuf p_323934_, Optional<MapItemSavedData.MapPatch> p_323549_) {
            if (p_323549_.isPresent()) {
                MapItemSavedData.MapPatch mapitemsaveddata$mappatch = p_323549_.get();
                p_323934_.writeByte(mapitemsaveddata$mappatch.width);
                p_323934_.writeByte(mapitemsaveddata$mappatch.height);
                p_323934_.writeByte(mapitemsaveddata$mappatch.startX);
                p_323934_.writeByte(mapitemsaveddata$mappatch.startY);
                FriendlyByteBuf.writeByteArray(p_323934_, mapitemsaveddata$mappatch.mapColors);
            } else {
                p_323934_.writeByte(0);
            }
        }

        private static Optional<MapItemSavedData.MapPatch> read(ByteBuf p_323587_) {
            int i = p_323587_.readUnsignedByte();
            if (i > 0) {
                int j = p_323587_.readUnsignedByte();
                int k = p_323587_.readUnsignedByte();
                int l = p_323587_.readUnsignedByte();
                byte[] abyte = FriendlyByteBuf.readByteArray(p_323587_);
                return Optional.of(new MapItemSavedData.MapPatch(k, l, i, j, abyte));
            } else {
                return Optional.empty();
            }
        }

        public void applyToMap(MapItemSavedData pSavedData) {
            for (int i = 0; i < this.width; i++) {
                for (int j = 0; j < this.height; j++) {
                    pSavedData.setColor(this.startX + i, this.startY + j, this.mapColors[i + j * this.width]);
                }
            }
        }
    }
}

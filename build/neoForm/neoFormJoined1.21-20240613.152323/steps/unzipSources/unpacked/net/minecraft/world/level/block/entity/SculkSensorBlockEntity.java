package net.minecraft.world.level.block.entity;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.BlockPositionSource;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gameevent.GameEventListener;
import net.minecraft.world.level.gameevent.PositionSource;
import net.minecraft.world.level.gameevent.vibrations.VibrationSystem;
import org.slf4j.Logger;

public class SculkSensorBlockEntity extends BlockEntity implements GameEventListener.Provider<VibrationSystem.Listener>, VibrationSystem {
    private static final Logger LOGGER = LogUtils.getLogger();
    private VibrationSystem.Data vibrationData;
    private final VibrationSystem.Listener vibrationListener;
    private final VibrationSystem.User vibrationUser = this.createVibrationUser();
    private int lastVibrationFrequency;

    protected SculkSensorBlockEntity(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
        this.vibrationData = new VibrationSystem.Data();
        this.vibrationListener = new VibrationSystem.Listener(this);
    }

    public SculkSensorBlockEntity(BlockPos pPos, BlockState pBlockState) {
        this(BlockEntityType.SCULK_SENSOR, pPos, pBlockState);
    }

    public VibrationSystem.User createVibrationUser() {
        return new SculkSensorBlockEntity.VibrationUser(this.getBlockPos());
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.lastVibrationFrequency = pTag.getInt("last_vibration_frequency");
        RegistryOps<Tag> registryops = pRegistries.createSerializationContext(NbtOps.INSTANCE);
        if (pTag.contains("listener", 10)) {
            VibrationSystem.Data.CODEC
                .parse(registryops, pTag.getCompound("listener"))
                .resultOrPartial(p_351973_ -> LOGGER.error("Failed to parse vibration listener for Sculk Sensor: '{}'", p_351973_))
                .ifPresent(p_281146_ -> this.vibrationData = p_281146_);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.putInt("last_vibration_frequency", this.lastVibrationFrequency);
        RegistryOps<Tag> registryops = pRegistries.createSerializationContext(NbtOps.INSTANCE);
        VibrationSystem.Data.CODEC
            .encodeStart(registryops, this.vibrationData)
            .resultOrPartial(p_351974_ -> LOGGER.error("Failed to encode vibration listener for Sculk Sensor: '{}'", p_351974_))
            .ifPresent(p_222820_ -> pTag.put("listener", p_222820_));
    }

    @Override
    public VibrationSystem.Data getVibrationData() {
        return this.vibrationData;
    }

    @Override
    public VibrationSystem.User getVibrationUser() {
        return this.vibrationUser;
    }

    public int getLastVibrationFrequency() {
        return this.lastVibrationFrequency;
    }

    public void setLastVibrationFrequency(int pLastVibrationFrequency) {
        this.lastVibrationFrequency = pLastVibrationFrequency;
    }

    public VibrationSystem.Listener getListener() {
        return this.vibrationListener;
    }

    protected class VibrationUser implements VibrationSystem.User {
        public static final int LISTENER_RANGE = 8;
        protected final BlockPos blockPos;
        private final PositionSource positionSource;

        public VibrationUser(BlockPos pPos) {
            this.blockPos = pPos;
            this.positionSource = new BlockPositionSource(pPos);
        }

        @Override
        public int getListenerRadius() {
            return 8;
        }

        @Override
        public PositionSource getPositionSource() {
            return this.positionSource;
        }

        @Override
        public boolean canTriggerAvoidVibration() {
            return true;
        }

        @Override
        public boolean canReceiveVibration(ServerLevel pLevel, BlockPos pPos, Holder<GameEvent> pGameEvent, @Nullable GameEvent.Context pContext) {
            return !pPos.equals(this.blockPos) || !pGameEvent.is(GameEvent.BLOCK_DESTROY) && !pGameEvent.is(GameEvent.BLOCK_PLACE)
                ? SculkSensorBlock.canActivate(SculkSensorBlockEntity.this.getBlockState())
                : false;
        }

        @Override
        public void onReceiveVibration(
            ServerLevel pLevel, BlockPos pPos, Holder<GameEvent> pGameEvent, @Nullable Entity pEntity, @Nullable Entity pPlayerEntity, float pDistance
        ) {
            BlockState blockstate = SculkSensorBlockEntity.this.getBlockState();
            if (SculkSensorBlock.canActivate(blockstate)) {
                SculkSensorBlockEntity.this.setLastVibrationFrequency(VibrationSystem.getGameEventFrequency(pGameEvent));
                int i = VibrationSystem.getRedstoneStrengthForDistance(pDistance, this.getListenerRadius());
                if (blockstate.getBlock() instanceof SculkSensorBlock sculksensorblock) {
                    sculksensorblock.activate(pEntity, pLevel, this.blockPos, blockstate, i, SculkSensorBlockEntity.this.getLastVibrationFrequency());
                }
            }
        }

        @Override
        public void onDataChanged() {
            SculkSensorBlockEntity.this.setChanged();
        }

        @Override
        public boolean requiresAdjacentChunksToBeTicking() {
            return true;
        }
    }
}

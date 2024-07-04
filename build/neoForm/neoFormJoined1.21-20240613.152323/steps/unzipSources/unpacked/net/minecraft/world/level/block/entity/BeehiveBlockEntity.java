package net.minecraft.world.level.block.entity;

import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import io.netty.buffer.ByteBuf;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.slf4j.Logger;

public class BeehiveBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG_FLOWER_POS = "flower_pos";
    private static final String BEES = "bees";
    static final List<String> IGNORED_BEE_TAGS = Arrays.asList(
        "Air",
        "ArmorDropChances",
        "ArmorItems",
        "Brain",
        "CanPickUpLoot",
        "DeathTime",
        "FallDistance",
        "FallFlying",
        "Fire",
        "HandDropChances",
        "HandItems",
        "HurtByTimestamp",
        "HurtTime",
        "LeftHanded",
        "Motion",
        "NoGravity",
        "OnGround",
        "PortalCooldown",
        "Pos",
        "Rotation",
        "SleepingX",
        "SleepingY",
        "SleepingZ",
        "CannotEnterHiveTicks",
        "TicksSincePollination",
        "CropsGrownSincePollination",
        "hive_pos",
        "Passengers",
        "leash",
        "UUID"
    );
    public static final int MAX_OCCUPANTS = 3;
    private static final int MIN_TICKS_BEFORE_REENTERING_HIVE = 400;
    private static final int MIN_OCCUPATION_TICKS_NECTAR = 2400;
    public static final int MIN_OCCUPATION_TICKS_NECTARLESS = 600;
    private final List<BeehiveBlockEntity.BeeData> stored = Lists.newArrayList();
    @Nullable
    private BlockPos savedFlowerPos;

    public BeehiveBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.BEEHIVE, pPos, pBlockState);
    }

    @Override
    public void setChanged() {
        if (this.isFireNearby()) {
            this.emptyAllLivingFromHive(null, this.level.getBlockState(this.getBlockPos()), BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY);
        }

        super.setChanged();
    }

    public boolean isFireNearby() {
        if (this.level == null) {
            return false;
        } else {
            for (BlockPos blockpos : BlockPos.betweenClosed(this.worldPosition.offset(-1, -1, -1), this.worldPosition.offset(1, 1, 1))) {
                if (this.level.getBlockState(blockpos).getBlock() instanceof FireBlock) {
                    return true;
                }
            }

            return false;
        }
    }

    public boolean isEmpty() {
        return this.stored.isEmpty();
    }

    public boolean isFull() {
        return this.stored.size() == 3;
    }

    public void emptyAllLivingFromHive(@Nullable Player pPlayer, BlockState pState, BeehiveBlockEntity.BeeReleaseStatus pReleaseStatus) {
        List<Entity> list = this.releaseAllOccupants(pState, pReleaseStatus);
        if (pPlayer != null) {
            for (Entity entity : list) {
                if (entity instanceof Bee) {
                    Bee bee = (Bee)entity;
                    if (pPlayer.position().distanceToSqr(entity.position()) <= 16.0) {
                        if (!this.isSedated()) {
                            bee.setTarget(pPlayer);
                        } else {
                            bee.setStayOutOfHiveCountdown(400);
                        }
                    }
                }
            }
        }
    }

    private List<Entity> releaseAllOccupants(BlockState pState, BeehiveBlockEntity.BeeReleaseStatus pReleaseStatus) {
        List<Entity> list = Lists.newArrayList();
        this.stored
            .removeIf(p_330138_ -> releaseOccupant(this.level, this.worldPosition, pState, p_330138_.toOccupant(), list, pReleaseStatus, this.savedFlowerPos));
        if (!list.isEmpty()) {
            super.setChanged();
        }

        return list;
    }

    @VisibleForDebug
    public int getOccupantCount() {
        return this.stored.size();
    }

    public static int getHoneyLevel(BlockState pState) {
        return pState.getValue(BeehiveBlock.HONEY_LEVEL);
    }

    @VisibleForDebug
    public boolean isSedated() {
        return CampfireBlock.isSmokeyPos(this.level, this.getBlockPos());
    }

    public void addOccupant(Entity pOccupant) {
        if (this.stored.size() < 3) {
            pOccupant.stopRiding();
            pOccupant.ejectPassengers();
            this.storeBee(BeehiveBlockEntity.Occupant.of(pOccupant));
            if (this.level != null) {
                if (pOccupant instanceof Bee bee && bee.hasSavedFlowerPos() && (!this.hasSavedFlowerPos() || this.level.random.nextBoolean())) {
                    this.savedFlowerPos = bee.getSavedFlowerPos();
                }

                BlockPos blockpos = this.getBlockPos();
                this.level
                    .playSound(
                        null,
                        (double)blockpos.getX(),
                        (double)blockpos.getY(),
                        (double)blockpos.getZ(),
                        SoundEvents.BEEHIVE_ENTER,
                        SoundSource.BLOCKS,
                        1.0F,
                        1.0F
                    );
                this.level.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(pOccupant, this.getBlockState()));
            }

            pOccupant.discard();
            super.setChanged();
        }
    }

    public void storeBee(BeehiveBlockEntity.Occupant p_330820_) {
        this.stored.add(new BeehiveBlockEntity.BeeData(p_330820_));
    }

    private static boolean releaseOccupant(
        Level pLevel,
        BlockPos pPos,
        BlockState pState,
        BeehiveBlockEntity.Occupant pOccupant,
        @Nullable List<Entity> pStoredInHives,
        BeehiveBlockEntity.BeeReleaseStatus pReleaseStatus,
        @Nullable BlockPos pStoredFlowerPos
    ) {
        if ((pLevel.isNight() || pLevel.isRaining()) && pReleaseStatus != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
            return false;
        } else {
            Direction direction = pState.getValue(BeehiveBlock.FACING);
            BlockPos blockpos = pPos.relative(direction);
            boolean flag = !pLevel.getBlockState(blockpos).getCollisionShape(pLevel, blockpos).isEmpty();
            if (flag && pReleaseStatus != BeehiveBlockEntity.BeeReleaseStatus.EMERGENCY) {
                return false;
            } else {
                Entity entity = pOccupant.createEntity(pLevel, pPos);
                if (entity != null) {
                    if (entity instanceof Bee bee) {
                        if (pStoredFlowerPos != null && !bee.hasSavedFlowerPos() && pLevel.random.nextFloat() < 0.9F) {
                            bee.setSavedFlowerPos(pStoredFlowerPos);
                        }

                        if (pReleaseStatus == BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED) {
                            bee.dropOffNectar();
                            if (pState.is(BlockTags.BEEHIVES, p_202037_ -> p_202037_.hasProperty(BeehiveBlock.HONEY_LEVEL))) {
                                int i = getHoneyLevel(pState);
                                if (i < 5) {
                                    int j = pLevel.random.nextInt(100) == 0 ? 2 : 1;
                                    if (i + j > 5) {
                                        j--;
                                    }

                                    pLevel.setBlockAndUpdate(pPos, pState.setValue(BeehiveBlock.HONEY_LEVEL, Integer.valueOf(i + j)));
                                }
                            }
                        }

                        if (pStoredInHives != null) {
                            pStoredInHives.add(bee);
                        }

                        float f = entity.getBbWidth();
                        double d3 = flag ? 0.0 : 0.55 + (double)(f / 2.0F);
                        double d0 = (double)pPos.getX() + 0.5 + d3 * (double)direction.getStepX();
                        double d1 = (double)pPos.getY() + 0.5 - (double)(entity.getBbHeight() / 2.0F);
                        double d2 = (double)pPos.getZ() + 0.5 + d3 * (double)direction.getStepZ();
                        entity.moveTo(d0, d1, d2, entity.getYRot(), entity.getXRot());
                    }

                    pLevel.playSound(null, pPos, SoundEvents.BEEHIVE_EXIT, SoundSource.BLOCKS, 1.0F, 1.0F);
                    pLevel.gameEvent(GameEvent.BLOCK_CHANGE, pPos, GameEvent.Context.of(entity, pLevel.getBlockState(pPos)));
                    return pLevel.addFreshEntity(entity);
                } else {
                    return false;
                }
            }
        }
    }

    private boolean hasSavedFlowerPos() {
        return this.savedFlowerPos != null;
    }

    private static void tickOccupants(
        Level pLevel, BlockPos pPos, BlockState pState, List<BeehiveBlockEntity.BeeData> pData, @Nullable BlockPos pSavedFlowerPos
    ) {
        boolean flag = false;
        Iterator<BeehiveBlockEntity.BeeData> iterator = pData.iterator();

        while (iterator.hasNext()) {
            BeehiveBlockEntity.BeeData beehiveblockentity$beedata = iterator.next();
            if (beehiveblockentity$beedata.tick()) {
                BeehiveBlockEntity.BeeReleaseStatus beehiveblockentity$beereleasestatus = beehiveblockentity$beedata.hasNectar()
                    ? BeehiveBlockEntity.BeeReleaseStatus.HONEY_DELIVERED
                    : BeehiveBlockEntity.BeeReleaseStatus.BEE_RELEASED;
                if (releaseOccupant(
                    pLevel, pPos, pState, beehiveblockentity$beedata.toOccupant(), null, beehiveblockentity$beereleasestatus, pSavedFlowerPos
                )) {
                    flag = true;
                    iterator.remove();
                }
            }
        }

        if (flag) {
            setChanged(pLevel, pPos, pState);
        }
    }

    public static void serverTick(Level pLevel, BlockPos pPos, BlockState pState, BeehiveBlockEntity pBeehive) {
        tickOccupants(pLevel, pPos, pState, pBeehive.stored, pBeehive.savedFlowerPos);
        if (!pBeehive.stored.isEmpty() && pLevel.getRandom().nextDouble() < 0.005) {
            double d0 = (double)pPos.getX() + 0.5;
            double d1 = (double)pPos.getY();
            double d2 = (double)pPos.getZ() + 0.5;
            pLevel.playSound(null, d0, d1, d2, SoundEvents.BEEHIVE_WORK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        DebugPackets.sendHiveInfo(pLevel, pPos, pState, pBeehive);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);
        this.stored.clear();
        if (pTag.contains("bees")) {
            BeehiveBlockEntity.Occupant.LIST_CODEC
                .parse(NbtOps.INSTANCE, pTag.get("bees"))
                .resultOrPartial(p_330133_ -> LOGGER.error("Failed to parse bees: '{}'", p_330133_))
                .ifPresent(p_330134_ -> p_330134_.forEach(this::storeBee));
        }

        this.savedFlowerPos = NbtUtils.readBlockPos(pTag, "flower_pos").orElse(null);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.saveAdditional(pTag, pRegistries);
        pTag.put("bees", BeehiveBlockEntity.Occupant.LIST_CODEC.encodeStart(NbtOps.INSTANCE, this.getBees()).getOrThrow());
        if (this.hasSavedFlowerPos()) {
            pTag.put("flower_pos", NbtUtils.writeBlockPos(this.savedFlowerPos));
        }
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput pComponentInput) {
        super.applyImplicitComponents(pComponentInput);
        this.stored.clear();
        List<BeehiveBlockEntity.Occupant> list = pComponentInput.getOrDefault(DataComponents.BEES, List.of());
        list.forEach(this::storeBee);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder pComponents) {
        super.collectImplicitComponents(pComponents);
        pComponents.set(DataComponents.BEES, this.getBees());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag pTag) {
        super.removeComponentsFromTag(pTag);
        pTag.remove("bees");
    }

    private List<BeehiveBlockEntity.Occupant> getBees() {
        return this.stored.stream().map(BeehiveBlockEntity.BeeData::toOccupant).toList();
    }

    static class BeeData {
        private final BeehiveBlockEntity.Occupant occupant;
        private int ticksInHive;

        BeeData(BeehiveBlockEntity.Occupant pOccupant) {
            this.occupant = pOccupant;
            this.ticksInHive = pOccupant.ticksInHive();
        }

        public boolean tick() {
            return this.ticksInHive++ > this.occupant.minTicksInHive;
        }

        public BeehiveBlockEntity.Occupant toOccupant() {
            return new BeehiveBlockEntity.Occupant(this.occupant.entityData, this.ticksInHive, this.occupant.minTicksInHive);
        }

        public boolean hasNectar() {
            return this.occupant.entityData.getUnsafe().getBoolean("HasNectar");
        }
    }

    public static enum BeeReleaseStatus {
        HONEY_DELIVERED,
        BEE_RELEASED,
        EMERGENCY;
    }

    public static record Occupant(CustomData entityData, int ticksInHive, int minTicksInHive) {
        public static final Codec<BeehiveBlockEntity.Occupant> CODEC = RecordCodecBuilder.create(
            p_337984_ -> p_337984_.group(
                        CustomData.CODEC.optionalFieldOf("entity_data", CustomData.EMPTY).forGetter(BeehiveBlockEntity.Occupant::entityData),
                        Codec.INT.fieldOf("ticks_in_hive").forGetter(BeehiveBlockEntity.Occupant::ticksInHive),
                        Codec.INT.fieldOf("min_ticks_in_hive").forGetter(BeehiveBlockEntity.Occupant::minTicksInHive)
                    )
                    .apply(p_337984_, BeehiveBlockEntity.Occupant::new)
        );
        public static final Codec<List<BeehiveBlockEntity.Occupant>> LIST_CODEC = CODEC.listOf();
        public static final StreamCodec<ByteBuf, BeehiveBlockEntity.Occupant> STREAM_CODEC = StreamCodec.composite(
            CustomData.STREAM_CODEC,
            BeehiveBlockEntity.Occupant::entityData,
            ByteBufCodecs.VAR_INT,
            BeehiveBlockEntity.Occupant::ticksInHive,
            ByteBufCodecs.VAR_INT,
            BeehiveBlockEntity.Occupant::minTicksInHive,
            BeehiveBlockEntity.Occupant::new
        );

        public static BeehiveBlockEntity.Occupant of(Entity pEntity) {
            CompoundTag compoundtag = new CompoundTag();
            pEntity.save(compoundtag);
            BeehiveBlockEntity.IGNORED_BEE_TAGS.forEach(compoundtag::remove);
            boolean flag = compoundtag.getBoolean("HasNectar");
            return new BeehiveBlockEntity.Occupant(CustomData.of(compoundtag), 0, flag ? 2400 : 600);
        }

        public static BeehiveBlockEntity.Occupant create(int pTicksInHive) {
            CompoundTag compoundtag = new CompoundTag();
            compoundtag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(EntityType.BEE).toString());
            return new BeehiveBlockEntity.Occupant(CustomData.of(compoundtag), pTicksInHive, 600);
        }

        @Nullable
        public Entity createEntity(Level pLevel, BlockPos pPos) {
            CompoundTag compoundtag = this.entityData.copyTag();
            BeehiveBlockEntity.IGNORED_BEE_TAGS.forEach(compoundtag::remove);
            Entity entity = EntityType.loadEntityRecursive(compoundtag, pLevel, p_331097_ -> p_331097_);
            if (entity != null && entity.getType().is(EntityTypeTags.BEEHIVE_INHABITORS)) {
                entity.setNoGravity(true);
                if (entity instanceof Bee bee) {
                    bee.setHivePos(pPos);
                    setBeeReleaseData(this.ticksInHive, bee);
                }

                return entity;
            } else {
                return null;
            }
        }

        private static void setBeeReleaseData(int pTicksInHive, Bee pBee) {
            int i = pBee.getAge();
            if (i < 0) {
                pBee.setAge(Math.min(0, i + pTicksInHive));
            } else if (i > 0) {
                pBee.setAge(Math.max(0, i - pTicksInHive));
            }

            pBee.setInLoveTime(Math.max(0, pBee.getInLoveTime() - pTicksInHive));
        }
    }
}

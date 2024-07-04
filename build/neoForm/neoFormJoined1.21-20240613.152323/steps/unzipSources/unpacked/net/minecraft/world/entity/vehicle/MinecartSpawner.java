package net.minecraft.world.entity.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BaseSpawner;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class MinecartSpawner extends AbstractMinecart {
    private final BaseSpawner spawner = new BaseSpawner() {
        @Override
        public void broadcastEvent(Level p_150342_, BlockPos p_150343_, int p_150344_) {
            p_150342_.broadcastEntityEvent(MinecartSpawner.this, (byte)p_150344_);
        }

        @Override
        public com.mojang.datafixers.util.Either<net.minecraft.world.level.block.entity.BlockEntity, net.minecraft.world.entity.Entity> getOwner() {
            return com.mojang.datafixers.util.Either.right(MinecartSpawner.this);
        }
    };
    private final Runnable ticker;

    public MinecartSpawner(EntityType<? extends MinecartSpawner> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.ticker = this.createTicker(pLevel);
    }

    public MinecartSpawner(Level pLevel, double pX, double pY, double pZ) {
        super(EntityType.SPAWNER_MINECART, pLevel, pX, pY, pZ);
        this.ticker = this.createTicker(pLevel);
    }

    @Override
    protected Item getDropItem() {
        return Items.MINECART;
    }

    private Runnable createTicker(Level pLevel) {
        return pLevel instanceof ServerLevel
            ? () -> this.spawner.serverTick((ServerLevel)pLevel, this.blockPosition())
            : () -> this.spawner.clientTick(pLevel, this.blockPosition());
    }

    @Override
    public AbstractMinecart.Type getMinecartType() {
        return AbstractMinecart.Type.SPAWNER;
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return Blocks.SPAWNER.defaultBlockState();
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    @Override
    protected void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.spawner.load(this.level(), this.blockPosition(), pCompound);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        this.spawner.save(pCompound);
    }

    /**
     * Handles an entity event received from a {@link net.minecraft.network.protocol.game.ClientboundEntityEventPacket}.
     */
    @Override
    public void handleEntityEvent(byte pId) {
        this.spawner.onEventTriggered(this.level(), pId);
    }

    @Override
    public void tick() {
        super.tick();
        this.ticker.run();
    }

    public BaseSpawner getSpawner() {
        return this.spawner;
    }

    @Override
    public boolean onlyOpCanSetNbt() {
        return true;
    }
}

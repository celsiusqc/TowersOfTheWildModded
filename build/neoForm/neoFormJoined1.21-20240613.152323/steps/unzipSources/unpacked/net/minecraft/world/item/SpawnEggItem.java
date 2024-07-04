package net.minecraft.world.item;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Spawner;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class SpawnEggItem extends Item {
    private static final Map<EntityType<? extends Mob>, SpawnEggItem> BY_ID = Maps.newIdentityHashMap();
    private static final MapCodec<EntityType<?>> ENTITY_TYPE_FIELD_CODEC = BuiltInRegistries.ENTITY_TYPE.byNameCodec().fieldOf("id");
    private final int backgroundColor;
    private final int highlightColor;
    private final EntityType<?> defaultType;

    /**
 * @deprecated Forge: Use {@link
 *             net.neoforged.neoforge.common.DeferredSpawnEggItem} instead for
 *             suppliers
 */
    @Deprecated
    public SpawnEggItem(EntityType<? extends Mob> pDefaultType, int pBackgroundColor, int pHighlightColor, Item.Properties pProperties) {
        super(pProperties);
        this.defaultType = pDefaultType;
        this.backgroundColor = pBackgroundColor;
        this.highlightColor = pHighlightColor;
        if (pDefaultType != null)
        BY_ID.put(pDefaultType, this);
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        if (!(level instanceof ServerLevel)) {
            return InteractionResult.SUCCESS;
        } else {
            ItemStack itemstack = pContext.getItemInHand();
            BlockPos blockpos = pContext.getClickedPos();
            Direction direction = pContext.getClickedFace();
            BlockState blockstate = level.getBlockState(blockpos);
            if (level.getBlockEntity(blockpos) instanceof Spawner spawner) {
                EntityType<?> entitytype1 = this.getType(itemstack);
                spawner.setEntityId(entitytype1, level.getRandom());
                level.sendBlockUpdated(blockpos, blockstate, blockstate, 3);
                level.gameEvent(pContext.getPlayer(), GameEvent.BLOCK_CHANGE, blockpos);
                itemstack.shrink(1);
                return InteractionResult.CONSUME;
            } else {
                BlockPos blockpos1;
                if (blockstate.getCollisionShape(level, blockpos).isEmpty()) {
                    blockpos1 = blockpos;
                } else {
                    blockpos1 = blockpos.relative(direction);
                }

                EntityType<?> entitytype = this.getType(itemstack);
                if (entitytype.spawn(
                        (ServerLevel)level,
                        itemstack,
                        pContext.getPlayer(),
                        blockpos1,
                        MobSpawnType.SPAWN_EGG,
                        true,
                        !Objects.equals(blockpos, blockpos1) && direction == Direction.UP
                    )
                    != null) {
                    itemstack.shrink(1);
                    level.gameEvent(pContext.getPlayer(), GameEvent.ENTITY_PLACE, blockpos);
                }

                return InteractionResult.CONSUME;
            }
        }
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see {@link #onItemUse}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        BlockHitResult blockhitresult = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.SOURCE_ONLY);
        if (blockhitresult.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(itemstack);
        } else if (!(pLevel instanceof ServerLevel)) {
            return InteractionResultHolder.success(itemstack);
        } else {
            BlockPos blockpos = blockhitresult.getBlockPos();
            if (!(pLevel.getBlockState(blockpos).getBlock() instanceof LiquidBlock)) {
                return InteractionResultHolder.pass(itemstack);
            } else if (pLevel.mayInteract(pPlayer, blockpos) && pPlayer.mayUseItemAt(blockpos, blockhitresult.getDirection(), itemstack)) {
                EntityType<?> entitytype = this.getType(itemstack);
                Entity entity = entitytype.spawn((ServerLevel)pLevel, itemstack, pPlayer, blockpos, MobSpawnType.SPAWN_EGG, false, false);
                if (entity == null) {
                    return InteractionResultHolder.pass(itemstack);
                } else {
                    itemstack.consume(1, pPlayer);
                    pPlayer.awardStat(Stats.ITEM_USED.get(this));
                    pLevel.gameEvent(pPlayer, GameEvent.ENTITY_PLACE, entity.position());
                    return InteractionResultHolder.consume(itemstack);
                }
            } else {
                return InteractionResultHolder.fail(itemstack);
            }
        }
    }

    public boolean spawnsEntity(ItemStack pStack, EntityType<?> pEntityType) {
        return Objects.equals(this.getType(pStack), pEntityType);
    }

    public int getColor(int pTintIndex) {
        return pTintIndex == 0 ? this.backgroundColor : this.highlightColor;
    }

    @Nullable
    public static SpawnEggItem byId(@Nullable EntityType<?> pType) {
        var ret = net.neoforged.neoforge.common.DeferredSpawnEggItem.deferredOnlyById(pType);
        if (ret != null) return ret;
        return BY_ID.get(pType);
    }

    public static Iterable<SpawnEggItem> eggs() {
        return Iterables.unmodifiableIterable(BY_ID.values());
    }

    public EntityType<?> getType(ItemStack pStack) {
        CustomData customdata = pStack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
        return !customdata.isEmpty() ? customdata.read(ENTITY_TYPE_FIELD_CODEC).result().orElse(getDefaultType()) : getDefaultType();
    }

    @Override
    public FeatureFlagSet requiredFeatures() {
        return this.getDefaultType().requiredFeatures();
    }

    public Optional<Mob> spawnOffspringFromSpawnEgg(
        Player pPlayer, Mob pMob, EntityType<? extends Mob> pEntityType, ServerLevel pServerLevel, Vec3 pPos, ItemStack pStack
    ) {
        if (!this.spawnsEntity(pStack, pEntityType)) {
            return Optional.empty();
        } else {
            Mob mob;
            if (pMob instanceof AgeableMob) {
                mob = ((AgeableMob)pMob).getBreedOffspring(pServerLevel, (AgeableMob)pMob);
            } else {
                mob = pEntityType.create(pServerLevel);
            }

            if (mob == null) {
                return Optional.empty();
            } else {
                mob.setBaby(true);
                if (!mob.isBaby()) {
                    return Optional.empty();
                } else {
                    mob.moveTo(pPos.x(), pPos.y(), pPos.z(), 0.0F, 0.0F);
                    pServerLevel.addFreshEntityWithPassengers(mob);
                    mob.setCustomName(pStack.get(DataComponents.CUSTOM_NAME));
                    pStack.consume(1, pPlayer);
                    return Optional.of(mob);
                }
            }
        }
    }

    protected EntityType<?> getDefaultType() {
        return defaultType;
    }
}

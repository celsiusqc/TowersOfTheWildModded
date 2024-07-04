package net.minecraft.world.item;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class CrossbowItem extends ProjectileWeaponItem {
    private static final float MAX_CHARGE_DURATION = 1.25F;
    public static final int DEFAULT_RANGE = 8;
    /**
     * Set to {@code true} when the crossbow is 20% charged.
     */
    private boolean startSoundPlayed = false;
    /**
     * Set to {@code true} when the crossbow is 50% charged.
     */
    private boolean midLoadSoundPlayed = false;
    private static final float START_SOUND_PERCENT = 0.2F;
    private static final float MID_SOUND_PERCENT = 0.5F;
    private static final float ARROW_POWER = 3.15F;
    private static final float FIREWORK_POWER = 1.6F;
    public static final float MOB_ARROW_POWER = 1.6F;
    private static final CrossbowItem.ChargingSounds DEFAULT_SOUNDS = new CrossbowItem.ChargingSounds(
        Optional.of(SoundEvents.CROSSBOW_LOADING_START), Optional.of(SoundEvents.CROSSBOW_LOADING_MIDDLE), Optional.of(SoundEvents.CROSSBOW_LOADING_END)
    );

    public CrossbowItem(Item.Properties pProperties) {
        super(pProperties);
    }

    @Override
    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return ARROW_OR_FIREWORK;
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return ARROW_ONLY;
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see {@link #onItemUse}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        ChargedProjectiles chargedprojectiles = itemstack.get(DataComponents.CHARGED_PROJECTILES);
        if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
            this.performShooting(pLevel, pPlayer, pHand, itemstack, getShootingPower(chargedprojectiles), 1.0F, null);
            return InteractionResultHolder.consume(itemstack);
        } else if (!pPlayer.getProjectile(itemstack).isEmpty()) {
            this.startSoundPlayed = false;
            this.midLoadSoundPlayed = false;
            pPlayer.startUsingItem(pHand);
            return InteractionResultHolder.consume(itemstack);
        } else {
            return InteractionResultHolder.fail(itemstack);
        }
    }

    private static float getShootingPower(ChargedProjectiles pProjectile) {
        return pProjectile.contains(Items.FIREWORK_ROCKET) ? 1.6F : 3.15F;
    }

    /**
     * Called when the player stops using an Item (stops holding the right mouse button).
     */
    @Override
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft) {
        int i = this.getUseDuration(pStack, pEntityLiving) - pTimeLeft;
        float f = getPowerForTime(i, pStack, pEntityLiving);
        if (f >= 1.0F && !isCharged(pStack) && tryLoadProjectiles(pEntityLiving, pStack)) {
            CrossbowItem.ChargingSounds crossbowitem$chargingsounds = this.getChargingSounds(pStack);
            crossbowitem$chargingsounds.end()
                .ifPresent(
                    p_352852_ -> pLevel.playSound(
                            null,
                            pEntityLiving.getX(),
                            pEntityLiving.getY(),
                            pEntityLiving.getZ(),
                            p_352852_.value(),
                            pEntityLiving.getSoundSource(),
                            1.0F,
                            1.0F / (pLevel.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F
                        )
                );
        }
    }

    private static boolean tryLoadProjectiles(LivingEntity pShooter, ItemStack pCrossbowStack) {
        List<ItemStack> list = draw(pCrossbowStack, pShooter.getProjectile(pCrossbowStack), pShooter);
        if (!list.isEmpty()) {
            pCrossbowStack.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.of(list));
            return true;
        } else {
            return false;
        }
    }

    public static boolean isCharged(ItemStack pCrossbowStack) {
        ChargedProjectiles chargedprojectiles = pCrossbowStack.getOrDefault(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
        return !chargedprojectiles.isEmpty();
    }

    @Override
    protected void shootProjectile(
        LivingEntity pShooter, Projectile pProjectile, int pIndex, float pVelocity, float pInaccuracy, float pAngle, @Nullable LivingEntity pTarget
    ) {
        Vector3f vector3f;
        if (pTarget != null) {
            double d0 = pTarget.getX() - pShooter.getX();
            double d1 = pTarget.getZ() - pShooter.getZ();
            double d2 = Math.sqrt(d0 * d0 + d1 * d1);
            double d3 = pTarget.getY(0.3333333333333333) - pProjectile.getY() + d2 * 0.2F;
            vector3f = getProjectileShotVector(pShooter, new Vec3(d0, d3, d1), pAngle);
        } else {
            Vec3 vec3 = pShooter.getUpVector(1.0F);
            Quaternionf quaternionf = new Quaternionf().setAngleAxis((double)(pAngle * (float) (Math.PI / 180.0)), vec3.x, vec3.y, vec3.z);
            Vec3 vec31 = pShooter.getViewVector(1.0F);
            vector3f = vec31.toVector3f().rotate(quaternionf);
        }

        pProjectile.shoot((double)vector3f.x(), (double)vector3f.y(), (double)vector3f.z(), pVelocity, pInaccuracy);
        float f = getShotPitch(pShooter.getRandom(), pIndex);
        pShooter.level().playSound(null, pShooter.getX(), pShooter.getY(), pShooter.getZ(), SoundEvents.CROSSBOW_SHOOT, pShooter.getSoundSource(), 1.0F, f);
    }

    private static Vector3f getProjectileShotVector(LivingEntity pShooter, Vec3 pDistance, float pAngle) {
        Vector3f vector3f = pDistance.toVector3f().normalize();
        Vector3f vector3f1 = new Vector3f(vector3f).cross(new Vector3f(0.0F, 1.0F, 0.0F));
        if ((double)vector3f1.lengthSquared() <= 1.0E-7) {
            Vec3 vec3 = pShooter.getUpVector(1.0F);
            vector3f1 = new Vector3f(vector3f).cross(vec3.toVector3f());
        }

        Vector3f vector3f2 = new Vector3f(vector3f).rotateAxis((float) (Math.PI / 2), vector3f1.x, vector3f1.y, vector3f1.z);
        return new Vector3f(vector3f).rotateAxis(pAngle * (float) (Math.PI / 180.0), vector3f2.x, vector3f2.y, vector3f2.z);
    }

    @Override
    protected Projectile createProjectile(Level pLevel, LivingEntity pShooter, ItemStack pWeapon, ItemStack pAmmo, boolean pIsCrit) {
        if (pAmmo.is(Items.FIREWORK_ROCKET)) {
            return new FireworkRocketEntity(pLevel, pAmmo, pShooter, pShooter.getX(), pShooter.getEyeY() - 0.15F, pShooter.getZ(), true);
        } else {
            Projectile projectile = super.createProjectile(pLevel, pShooter, pWeapon, pAmmo, pIsCrit);
            if (projectile instanceof AbstractArrow abstractarrow) {
                abstractarrow.setSoundEvent(SoundEvents.CROSSBOW_HIT);
            }

            return projectile;
        }
    }

    @Override
    protected int getDurabilityUse(ItemStack pStack) {
        return pStack.is(Items.FIREWORK_ROCKET) ? 3 : 1;
    }

    public void performShooting(
        Level pLevel, LivingEntity pShooter, InteractionHand pHand, ItemStack pWeapon, float pVelocity, float pInaccuracy, @Nullable LivingEntity pTarget
    ) {
        if (pLevel instanceof ServerLevel serverlevel) {
            if (pShooter instanceof Player player && net.neoforged.neoforge.event.EventHooks.onArrowLoose(pWeapon, pShooter.level(), player, 1, true) < 0) return;
            ChargedProjectiles chargedprojectiles = pWeapon.set(DataComponents.CHARGED_PROJECTILES, ChargedProjectiles.EMPTY);
            if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
                this.shoot(serverlevel, pShooter, pHand, pWeapon, chargedprojectiles.getItems(), pVelocity, pInaccuracy, pShooter instanceof Player, pTarget);
                if (pShooter instanceof ServerPlayer serverplayer) {
                    CriteriaTriggers.SHOT_CROSSBOW.trigger(serverplayer, pWeapon);
                    serverplayer.awardStat(Stats.ITEM_USED.get(pWeapon.getItem()));
                }
            }
        }
    }

    private static float getShotPitch(RandomSource pRandom, int pIndex) {
        return pIndex == 0 ? 1.0F : getRandomShotPitch((pIndex & 1) == 1, pRandom);
    }

    private static float getRandomShotPitch(boolean pIsHighPitched, RandomSource pRandom) {
        float f = pIsHighPitched ? 0.63F : 0.43F;
        return 1.0F / (pRandom.nextFloat() * 0.5F + 1.8F) + f;
    }

    /**
     * Called as the item is being used by an entity.
     */
    @Override
    public void onUseTick(Level pLevel, LivingEntity pLivingEntity, ItemStack pStack, int pCount) {
        if (!pLevel.isClientSide) {
            CrossbowItem.ChargingSounds crossbowitem$chargingsounds = this.getChargingSounds(pStack);
            float f = (float)(pStack.getUseDuration(pLivingEntity) - pCount) / (float)getChargeDuration(pStack, pLivingEntity);
            if (f < 0.2F) {
                this.startSoundPlayed = false;
                this.midLoadSoundPlayed = false;
            }

            if (f >= 0.2F && !this.startSoundPlayed) {
                this.startSoundPlayed = true;
                crossbowitem$chargingsounds.start()
                    .ifPresent(
                        p_352849_ -> pLevel.playSound(
                                null, pLivingEntity.getX(), pLivingEntity.getY(), pLivingEntity.getZ(), p_352849_.value(), SoundSource.PLAYERS, 0.5F, 1.0F
                            )
                    );
            }

            if (f >= 0.5F && !this.midLoadSoundPlayed) {
                this.midLoadSoundPlayed = true;
                crossbowitem$chargingsounds.mid()
                    .ifPresent(
                        p_352855_ -> pLevel.playSound(
                                null, pLivingEntity.getX(), pLivingEntity.getY(), pLivingEntity.getZ(), p_352855_.value(), SoundSource.PLAYERS, 0.5F, 1.0F
                            )
                    );
            }
        }
    }

    /**
     * How long it takes to use or consume an item
     */
    @Override
    public int getUseDuration(ItemStack pStack, LivingEntity p_344898_) {
        return getChargeDuration(pStack, p_344898_) + 3;
    }

    /**
     * The time the crossbow must be used to reload it
     */
    public static int getChargeDuration(ItemStack pCrossbowStack, LivingEntity p_345687_) {
        float f = EnchantmentHelper.modifyCrossbowChargingTime(pCrossbowStack, p_345687_, 1.25F);
        return Mth.floor(f * 20.0F);
    }

    /**
     * Returns the action that specifies what animation to play when the item is being used.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.CROSSBOW;
    }

    CrossbowItem.ChargingSounds getChargingSounds(ItemStack p_345050_) {
        return EnchantmentHelper.pickHighestLevel(p_345050_, EnchantmentEffectComponents.CROSSBOW_CHARGING_SOUNDS).orElse(DEFAULT_SOUNDS);
    }

    private static float getPowerForTime(int pUseTime, ItemStack pCrossbowStack, LivingEntity p_344803_) {
        float f = (float)pUseTime / (float)getChargeDuration(pCrossbowStack, p_344803_);
        if (f > 1.0F) {
            f = 1.0F;
        }

        return f;
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        ChargedProjectiles chargedprojectiles = pStack.get(DataComponents.CHARGED_PROJECTILES);
        if (chargedprojectiles != null && !chargedprojectiles.isEmpty()) {
            ItemStack itemstack = chargedprojectiles.getItems().get(0);
            pTooltipComponents.add(Component.translatable("item.minecraft.crossbow.projectile").append(CommonComponents.SPACE).append(itemstack.getDisplayName()));
            if (pTooltipFlag.isAdvanced() && itemstack.is(Items.FIREWORK_ROCKET)) {
                List<Component> list = Lists.newArrayList();
                Items.FIREWORK_ROCKET.appendHoverText(itemstack, pContext, list, pTooltipFlag);
                if (!list.isEmpty()) {
                    for (int i = 0; i < list.size(); i++) {
                        list.set(i, Component.literal("  ").append(list.get(i)).withStyle(ChatFormatting.GRAY));
                    }

                    pTooltipComponents.addAll(list);
                }
            }
        }
    }

    /**
     * If this stack's item is a crossbow
     */
    @Override
    public boolean useOnRelease(ItemStack pStack) {
        return pStack.is(this);
    }

    @Override
    public int getDefaultProjectileRange() {
        return 8;
    }

    public static record ChargingSounds(Optional<Holder<SoundEvent>> start, Optional<Holder<SoundEvent>> mid, Optional<Holder<SoundEvent>> end) {
        public static final Codec<CrossbowItem.ChargingSounds> CODEC = RecordCodecBuilder.create(
            p_345672_ -> p_345672_.group(
                        SoundEvent.CODEC.optionalFieldOf("start").forGetter(CrossbowItem.ChargingSounds::start),
                        SoundEvent.CODEC.optionalFieldOf("mid").forGetter(CrossbowItem.ChargingSounds::mid),
                        SoundEvent.CODEC.optionalFieldOf("end").forGetter(CrossbowItem.ChargingSounds::end)
                    )
                    .apply(p_345672_, CrossbowItem.ChargingSounds::new)
        );
    }
}

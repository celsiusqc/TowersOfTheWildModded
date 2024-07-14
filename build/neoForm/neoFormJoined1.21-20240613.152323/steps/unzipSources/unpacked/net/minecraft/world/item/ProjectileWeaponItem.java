package net.minecraft.world.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;

public abstract class ProjectileWeaponItem extends Item {
    public static final Predicate<ItemStack> ARROW_ONLY = p_43017_ -> p_43017_.is(ItemTags.ARROWS);
    public static final Predicate<ItemStack> ARROW_OR_FIREWORK = ARROW_ONLY.or(p_43015_ -> p_43015_.is(Items.FIREWORK_ROCKET));

    public ProjectileWeaponItem(Item.Properties pProperties) {
        super(pProperties);
    }

    public Predicate<ItemStack> getSupportedHeldProjectiles() {
        return this.getAllSupportedProjectiles();
    }

    public abstract Predicate<ItemStack> getAllSupportedProjectiles();

    public static ItemStack getHeldProjectile(LivingEntity pShooter, Predicate<ItemStack> pIsAmmo) {
        if (pIsAmmo.test(pShooter.getItemInHand(InteractionHand.OFF_HAND))) {
            return pShooter.getItemInHand(InteractionHand.OFF_HAND);
        } else {
            return pIsAmmo.test(pShooter.getItemInHand(InteractionHand.MAIN_HAND)) ? pShooter.getItemInHand(InteractionHand.MAIN_HAND) : ItemStack.EMPTY;
        }
    }

    @Override
    public int getEnchantmentValue() {
        return 1;
    }

    public abstract int getDefaultProjectileRange();

    protected void shoot(
        ServerLevel p_346125_,
        LivingEntity p_330728_,
        InteractionHand p_331152_,
        ItemStack p_330646_,
        List<ItemStack> p_331726_,
        float p_331007_,
        float p_331445_,
        boolean p_331107_,
        @Nullable LivingEntity p_331167_
    ) {
        float f = EnchantmentHelper.processProjectileSpread(p_346125_, p_330646_, p_330728_, 0.0F);
        float f1 = p_331726_.size() == 1 ? 0.0F : 2.0F * f / (float)(p_331726_.size() - 1);
        float f2 = (float)((p_331726_.size() - 1) % 2) * f1 / 2.0F;
        float f3 = 1.0F;

        for (int i = 0; i < p_331726_.size(); i++) {
            ItemStack itemstack = p_331726_.get(i);
            if (!itemstack.isEmpty()) {
                float f4 = f2 + f3 * (float)((i + 1) / 2) * f1;
                f3 = -f3;
                Projectile projectile = this.createProjectile(p_346125_, p_330728_, p_330646_, itemstack, p_331107_);
                this.shootProjectile(p_330728_, projectile, i, p_331007_, p_331445_, f4, p_331167_);
                p_346125_.addFreshEntity(projectile);
                p_330646_.hurtAndBreak(this.getDurabilityUse(itemstack), p_330728_, LivingEntity.getSlotForHand(p_331152_));
                if (p_330646_.isEmpty()) {
                    break;
                }
            }
        }
    }

    protected int getDurabilityUse(ItemStack pStack) {
        return 1;
    }

    protected abstract void shootProjectile(
        LivingEntity pShooter, Projectile pProjectile, int pIndex, float pVelocity, float pInaccuracy, float pAngle, @Nullable LivingEntity pTarget
    );

    protected Projectile createProjectile(Level pLevel, LivingEntity pShooter, ItemStack pWeapon, ItemStack pAmmo, boolean pIsCrit) {
        ArrowItem arrowitem = pAmmo.getItem() instanceof ArrowItem arrowitem1 ? arrowitem1 : (ArrowItem)Items.ARROW;
        AbstractArrow abstractarrow = arrowitem.createArrow(pLevel, pAmmo, pShooter, pWeapon);
        if (pIsCrit) {
            abstractarrow.setCritArrow(true);
        }

        return customArrow(abstractarrow, pAmmo, pWeapon);
    }

    protected static List<ItemStack> draw(ItemStack pWeapon, ItemStack pAmmo, LivingEntity pShooter) {
        if (pAmmo.isEmpty()) {
            return List.of();
        } else {
            int i = pShooter.level() instanceof ServerLevel serverlevel ? EnchantmentHelper.processProjectileCount(serverlevel, pWeapon, pShooter, 1) : 1;
            List<ItemStack> list = new ArrayList<>(i);
            ItemStack itemstack1 = pAmmo.copy();

            for (int j = 0; j < i; j++) {
                ItemStack itemstack = useAmmo(pWeapon, j == 0 ? pAmmo : itemstack1, pShooter, j > 0);
                if (!itemstack.isEmpty()) {
                    list.add(itemstack);
                }
            }

            return list;
        }
    }

    protected static ItemStack useAmmo(ItemStack pWeapon, ItemStack pAmmo, LivingEntity pShooter, boolean pIntangable) {
        // Neo: Adjust this check to respect ArrowItem#isInfinite, bypassing processAmmoUse if true.
        int i = !pIntangable && pShooter.level() instanceof ServerLevel serverlevel && !(pShooter.hasInfiniteMaterials() || (pAmmo.getItem() instanceof ArrowItem ai && ai.isInfinite(pAmmo, pWeapon, pShooter)))
            ? EnchantmentHelper.processAmmoUse(serverlevel, pWeapon, pAmmo, 1)
            : 0;
        if (i > pAmmo.getCount()) {
            return ItemStack.EMPTY;
        } else if (i == 0) {
            ItemStack itemstack1 = pAmmo.copyWithCount(1);
            itemstack1.set(DataComponents.INTANGIBLE_PROJECTILE, Unit.INSTANCE);
            return itemstack1;
        } else {
            ItemStack itemstack = pAmmo.split(i);
            if (pAmmo.isEmpty() && pShooter instanceof Player player) {
                player.getInventory().removeItem(pAmmo);
            }

            return itemstack;
        }
    }

    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        return arrow;
    }

    /**
     * Neo: Controls what ammo ItemStack that Creative Mode should return if the player has no valid ammo in inventory.
     * Modded weapons should override this to return their own ammo if they do not use vanilla arrows.
     * @param player The player (if in context) firing the weapon
     * @param projectileWeaponItem The weapon ItemStack the ammo is for
     * @return The default ammo ItemStack for this weapon
     */
    public ItemStack getDefaultCreativeAmmo(@Nullable Player player, ItemStack projectileWeaponItem) {
        return Items.ARROW.getDefaultInstance();
    }
}

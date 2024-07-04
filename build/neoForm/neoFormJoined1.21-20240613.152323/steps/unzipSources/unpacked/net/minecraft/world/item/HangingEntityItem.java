package net.minecraft.world.item;

import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.GlowItemFrame;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class HangingEntityItem extends Item {
    private static final Component TOOLTIP_RANDOM_VARIANT = Component.translatable("painting.random").withStyle(ChatFormatting.GRAY);
    private final EntityType<? extends HangingEntity> type;

    public HangingEntityItem(EntityType<? extends HangingEntity> pType, Item.Properties pProperties) {
        super(pProperties);
        this.type = pType;
    }

    /**
     * Called when this item is used when targeting a Block
     */
    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        BlockPos blockpos = pContext.getClickedPos();
        Direction direction = pContext.getClickedFace();
        BlockPos blockpos1 = blockpos.relative(direction);
        Player player = pContext.getPlayer();
        ItemStack itemstack = pContext.getItemInHand();
        if (player != null && !this.mayPlace(player, direction, itemstack, blockpos1)) {
            return InteractionResult.FAIL;
        } else {
            Level level = pContext.getLevel();
            HangingEntity hangingentity;
            if (this.type == EntityType.PAINTING) {
                Optional<Painting> optional = Painting.create(level, blockpos1, direction);
                if (optional.isEmpty()) {
                    return InteractionResult.CONSUME;
                }

                hangingentity = optional.get();
            } else if (this.type == EntityType.ITEM_FRAME) {
                hangingentity = new ItemFrame(level, blockpos1, direction);
            } else {
                if (this.type != EntityType.GLOW_ITEM_FRAME) {
                    return InteractionResult.sidedSuccess(level.isClientSide);
                }

                hangingentity = new GlowItemFrame(level, blockpos1, direction);
            }

            CustomData customdata = itemstack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
            if (!customdata.isEmpty()) {
                EntityType.updateCustomEntityTag(level, player, hangingentity, customdata);
            }

            if (hangingentity.survives()) {
                if (!level.isClientSide) {
                    hangingentity.playPlacementSound();
                    level.gameEvent(player, GameEvent.ENTITY_PLACE, hangingentity.position());
                    level.addFreshEntity(hangingentity);
                }

                itemstack.shrink(1);
                return InteractionResult.sidedSuccess(level.isClientSide);
            } else {
                return InteractionResult.CONSUME;
            }
        }
    }

    protected boolean mayPlace(Player pPlayer, Direction pDirection, ItemStack pHangingEntityStack, BlockPos pPos) {
        return !pDirection.getAxis().isVertical() && pPlayer.mayUseItemAt(pPos, pDirection, pHangingEntityStack);
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        HolderLookup.Provider holderlookup$provider = pContext.registries();
        if (holderlookup$provider != null && this.type == EntityType.PAINTING) {
            CustomData customdata = pStack.getOrDefault(DataComponents.ENTITY_DATA, CustomData.EMPTY);
            if (!customdata.isEmpty()) {
                customdata.read(holderlookup$provider.createSerializationContext(NbtOps.INSTANCE), Painting.VARIANT_MAP_CODEC)
                    .result()
                    .ifPresentOrElse(p_344401_ -> {
                        p_344401_.unwrapKey().ifPresent(p_270217_ -> {
                            pTooltipComponents.add(Component.translatable(p_270217_.location().toLanguageKey("painting", "title")).withStyle(ChatFormatting.YELLOW));
                            pTooltipComponents.add(Component.translatable(p_270217_.location().toLanguageKey("painting", "author")).withStyle(ChatFormatting.GRAY));
                        });
                        pTooltipComponents.add(Component.translatable("painting.dimensions", p_344401_.value().width(), p_344401_.value().height()));
                    }, () -> pTooltipComponents.add(TOOLTIP_RANDOM_VARIANT));
            } else if (pTooltipFlag.isCreative()) {
                pTooltipComponents.add(TOOLTIP_RANDOM_VARIANT);
            }
        }
    }
}

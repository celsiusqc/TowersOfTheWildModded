package net.minecraft.world.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.util.StringUtil;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;

public class WrittenBookItem extends Item {
    public WrittenBookItem(Item.Properties pProperties) {
        super(pProperties);
    }

    /**
     * Gets the title name of the book
     */
    @Override
    public Component getName(ItemStack pStack) {
        WrittenBookContent writtenbookcontent = pStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (writtenbookcontent != null) {
            String s = writtenbookcontent.title().raw();
            if (!StringUtil.isBlank(s)) {
                return Component.literal(s);
            }
        }

        return super.getName(pStack);
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        WrittenBookContent writtenbookcontent = pStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (writtenbookcontent != null) {
            if (!StringUtil.isBlank(writtenbookcontent.author())) {
                pTooltipComponents.add(Component.translatable("book.byAuthor", writtenbookcontent.author()).withStyle(ChatFormatting.GRAY));
            }

            pTooltipComponents.add(Component.translatable("book.generation." + writtenbookcontent.generation()).withStyle(ChatFormatting.GRAY));
        }
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see {@link #onItemUse}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pHand);
        pPlayer.openItemGui(itemstack, pHand);
        pPlayer.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(itemstack, pLevel.isClientSide());
    }

    public static boolean resolveBookComponents(ItemStack pBookStack, CommandSourceStack pResolvingSource, @Nullable Player pResolvingPlayer) {
        WrittenBookContent writtenbookcontent = pBookStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
        if (writtenbookcontent != null && !writtenbookcontent.resolved()) {
            WrittenBookContent writtenbookcontent1 = writtenbookcontent.resolve(pResolvingSource, pResolvingPlayer);
            if (writtenbookcontent1 != null) {
                pBookStack.set(DataComponents.WRITTEN_BOOK_CONTENT, writtenbookcontent1);
                return true;
            }

            pBookStack.set(DataComponents.WRITTEN_BOOK_CONTENT, writtenbookcontent.markResolved());
        }

        return false;
    }
}

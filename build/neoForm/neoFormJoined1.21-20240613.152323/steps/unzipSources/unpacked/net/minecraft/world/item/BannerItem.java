package net.minecraft.world.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.AbstractBannerBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import org.apache.commons.lang3.Validate;

public class BannerItem extends StandingAndWallBlockItem {
    public BannerItem(Block pBlock, Block pWallBlock, Item.Properties pProperties) {
        super(pBlock, pWallBlock, pProperties, Direction.DOWN);
        Validate.isInstanceOf(AbstractBannerBlock.class, pBlock);
        Validate.isInstanceOf(AbstractBannerBlock.class, pWallBlock);
    }

    public static void appendHoverTextFromBannerBlockEntityTag(ItemStack pStack, List<Component> pTooltipComponents) {
        BannerPatternLayers bannerpatternlayers = pStack.get(DataComponents.BANNER_PATTERNS);
        if (bannerpatternlayers != null) {
            for (int i = 0; i < Math.min(bannerpatternlayers.layers().size(), 6); i++) {
                BannerPatternLayers.Layer bannerpatternlayers$layer = bannerpatternlayers.layers().get(i);
                pTooltipComponents.add(bannerpatternlayers$layer.description().withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public DyeColor getColor() {
        return ((AbstractBannerBlock)this.getBlock()).getColor();
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        appendHoverTextFromBannerBlockEntityTag(pStack, pTooltipComponents);
    }
}

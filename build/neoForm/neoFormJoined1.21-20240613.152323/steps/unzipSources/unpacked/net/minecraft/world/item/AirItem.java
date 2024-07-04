package net.minecraft.world.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

public class AirItem extends Item {
    private final Block block;

    public AirItem(Block pBlock, Item.Properties pProperties) {
        super(pProperties);
        this.block = pBlock;
    }

    @Override
    public String getDescriptionId() {
        return this.block.getDescriptionId();
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        this.block.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
    }
}

package net.minecraft.world.item;

import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.SkullBlockEntity;

public class PlayerHeadItem extends StandingAndWallBlockItem {
    public PlayerHeadItem(Block pBlock, Block pWallBlock, Item.Properties pProperties) {
        super(pBlock, pWallBlock, pProperties, Direction.DOWN);
    }

    @Override
    public Component getName(ItemStack pStack) {
        ResolvableProfile resolvableprofile = pStack.get(DataComponents.PROFILE);
        return (Component)(resolvableprofile != null && resolvableprofile.name().isPresent()
            ? Component.translatable(this.getDescriptionId() + ".named", resolvableprofile.name().get())
            : super.getName(pStack));
    }

    @Override
    public void verifyComponentsAfterLoad(ItemStack pStack) {
        ResolvableProfile resolvableprofile = pStack.get(DataComponents.PROFILE);
        if (resolvableprofile != null && !resolvableprofile.isResolved()) {
            resolvableprofile.resolve()
                .thenAcceptAsync(p_332155_ -> pStack.set(DataComponents.PROFILE, p_332155_), SkullBlockEntity.CHECKED_MAIN_THREAD_EXECUTOR);
        }
    }
}

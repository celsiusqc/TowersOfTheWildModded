package net.minecraft.world.item;

import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

public class InstrumentItem extends Item {
    private final TagKey<Instrument> instruments;

    public InstrumentItem(Item.Properties pProperties, TagKey<Instrument> pInstruments) {
        super(pProperties);
        this.instruments = pInstruments;
    }

    @Override
    public void appendHoverText(ItemStack pStack, Item.TooltipContext pContext, List<Component> pTooltipComponents, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, pTooltipComponents, pTooltipFlag);
        Optional<ResourceKey<Instrument>> optional = this.getInstrument(pStack).flatMap(Holder::unwrapKey);
        if (optional.isPresent()) {
            MutableComponent mutablecomponent = Component.translatable(Util.makeDescriptionId("instrument", optional.get().location()));
            pTooltipComponents.add(mutablecomponent.withStyle(ChatFormatting.GRAY));
        }
    }

    public static ItemStack create(Item pItem, Holder<Instrument> pInstrument) {
        ItemStack itemstack = new ItemStack(pItem);
        itemstack.set(DataComponents.INSTRUMENT, pInstrument);
        return itemstack;
    }

    public static void setRandom(ItemStack pStack, TagKey<Instrument> pInstrumentTag, RandomSource pRandom) {
        Optional<Holder<Instrument>> optional = BuiltInRegistries.INSTRUMENT.getRandomElementOf(pInstrumentTag, pRandom);
        optional.ifPresent(p_330088_ -> pStack.set(DataComponents.INSTRUMENT, (Holder<Instrument>)p_330088_));
    }

    /**
     * Called to trigger the item's "innate" right click behavior. To handle when this item is used on a Block, see {@link net.minecraft.world.item.Item#useOn(net.minecraft.world.item.context.UseOnContext)}.
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack itemstack = pPlayer.getItemInHand(pUsedHand);
        Optional<? extends Holder<Instrument>> optional = this.getInstrument(itemstack);
        if (optional.isPresent()) {
            Instrument instrument = optional.get().value();
            pPlayer.startUsingItem(pUsedHand);
            play(pLevel, pPlayer, instrument);
            pPlayer.getCooldowns().addCooldown(this, instrument.useDuration());
            pPlayer.awardStat(Stats.ITEM_USED.get(this));
            return InteractionResultHolder.consume(itemstack);
        } else {
            return InteractionResultHolder.fail(itemstack);
        }
    }

    /**
     * How long it takes to use or consume an item
     */
    @Override
    public int getUseDuration(ItemStack pStack, LivingEntity p_345916_) {
        Optional<Holder<Instrument>> optional = this.getInstrument(pStack);
        return optional.<Integer>map(p_248418_ -> p_248418_.value().useDuration()).orElse(0);
    }

    private Optional<Holder<Instrument>> getInstrument(ItemStack pStack) {
        Holder<Instrument> holder = pStack.get(DataComponents.INSTRUMENT);
        if (holder != null) {
            return Optional.of(holder);
        } else {
            Iterator<Holder<Instrument>> iterator = BuiltInRegistries.INSTRUMENT.getTagOrEmpty(this.instruments).iterator();
            return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
        }
    }

    /**
     * Returns the action that specifies what animation to play when the item is being used.
     */
    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.TOOT_HORN;
    }

    private static void play(Level pLevel, Player pPlayer, Instrument pInstrument) {
        SoundEvent soundevent = pInstrument.soundEvent().value();
        float f = pInstrument.range() / 16.0F;
        pLevel.playSound(pPlayer, pPlayer, soundevent, SoundSource.RECORDS, f, 1.0F);
        pLevel.gameEvent(GameEvent.INSTRUMENT_PLAY, pPlayer.position(), GameEvent.Context.of(pPlayer));
    }
}

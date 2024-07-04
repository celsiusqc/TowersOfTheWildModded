package net.minecraft.server.bossevents;

import com.google.common.collect.Maps;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class CustomBossEvents {
    private final Map<ResourceLocation, CustomBossEvent> events = Maps.newHashMap();

    @Nullable
    public CustomBossEvent get(ResourceLocation pId) {
        return this.events.get(pId);
    }

    public CustomBossEvent create(ResourceLocation pId, Component pName) {
        CustomBossEvent custombossevent = new CustomBossEvent(pId, pName);
        this.events.put(pId, custombossevent);
        return custombossevent;
    }

    public void remove(CustomBossEvent pBossbar) {
        this.events.remove(pBossbar.getTextId());
    }

    public Collection<ResourceLocation> getIds() {
        return this.events.keySet();
    }

    public Collection<CustomBossEvent> getEvents() {
        return this.events.values();
    }

    public CompoundTag save(HolderLookup.Provider pLevelRegistry) {
        CompoundTag compoundtag = new CompoundTag();

        for (CustomBossEvent custombossevent : this.events.values()) {
            compoundtag.put(custombossevent.getTextId().toString(), custombossevent.save(pLevelRegistry));
        }

        return compoundtag;
    }

    public void load(CompoundTag pTag, HolderLookup.Provider pLevelRegistry) {
        for (String s : pTag.getAllKeys()) {
            ResourceLocation resourcelocation = ResourceLocation.parse(s);
            this.events.put(resourcelocation, CustomBossEvent.load(pTag.getCompound(s), resourcelocation, pLevelRegistry));
        }
    }

    public void onPlayerConnect(ServerPlayer pPlayer) {
        for (CustomBossEvent custombossevent : this.events.values()) {
            custombossevent.onPlayerConnect(pPlayer);
        }
    }

    public void onPlayerDisconnect(ServerPlayer pPlayer) {
        for (CustomBossEvent custombossevent : this.events.values()) {
            custombossevent.onPlayerDisconnect(pPlayer);
        }
    }
}

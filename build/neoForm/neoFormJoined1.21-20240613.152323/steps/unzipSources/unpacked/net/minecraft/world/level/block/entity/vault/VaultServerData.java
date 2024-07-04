package net.minecraft.world.level.block.entity.vault;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class VaultServerData {
    static final String TAG_NAME = "server_data";
    static Codec<VaultServerData> CODEC = RecordCodecBuilder.create(
        p_338073_ -> p_338073_.group(
                    UUIDUtil.CODEC_LINKED_SET.lenientOptionalFieldOf("rewarded_players", Set.of()).forGetter(p_323523_ -> p_323523_.rewardedPlayers),
                    Codec.LONG.lenientOptionalFieldOf("state_updating_resumes_at", Long.valueOf(0L)).forGetter(p_323634_ -> p_323634_.stateUpdatingResumesAt),
                    ItemStack.CODEC.listOf().lenientOptionalFieldOf("items_to_eject", List.of()).forGetter(p_323976_ -> p_323976_.itemsToEject),
                    Codec.INT.lenientOptionalFieldOf("total_ejections_needed", Integer.valueOf(0)).forGetter(p_323753_ -> p_323753_.totalEjectionsNeeded)
                )
                .apply(p_338073_, VaultServerData::new)
    );
    private static final int MAX_REWARD_PLAYERS = 128;
    private final Set<UUID> rewardedPlayers = new ObjectLinkedOpenHashSet<>();
    private long stateUpdatingResumesAt;
    private final List<ItemStack> itemsToEject = new ObjectArrayList<>();
    private long lastInsertFailTimestamp;
    private int totalEjectionsNeeded;
    boolean isDirty;

    VaultServerData(Set<UUID> p_324455_, long p_324396_, List<ItemStack> p_324515_, int p_324586_) {
        this.rewardedPlayers.addAll(p_324455_);
        this.stateUpdatingResumesAt = p_324396_;
        this.itemsToEject.addAll(p_324515_);
        this.totalEjectionsNeeded = p_324586_;
    }

    VaultServerData() {
    }

    void setLastInsertFailTimestamp(long pLastInsertFailTimestamp) {
        this.lastInsertFailTimestamp = pLastInsertFailTimestamp;
    }

    long getLastInsertFailTimestamp() {
        return this.lastInsertFailTimestamp;
    }

    Set<UUID> getRewardedPlayers() {
        return this.rewardedPlayers;
    }

    boolean hasRewardedPlayer(Player pPlayer) {
        return this.rewardedPlayers.contains(pPlayer.getUUID());
    }

    @VisibleForTesting
    public void addToRewardedPlayers(Player pPlayer) {
        this.rewardedPlayers.add(pPlayer.getUUID());
        if (this.rewardedPlayers.size() > 128) {
            Iterator<UUID> iterator = this.rewardedPlayers.iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }

        this.markChanged();
    }

    long stateUpdatingResumesAt() {
        return this.stateUpdatingResumesAt;
    }

    void pauseStateUpdatingUntil(long pTime) {
        this.stateUpdatingResumesAt = pTime;
        this.markChanged();
    }

    List<ItemStack> getItemsToEject() {
        return this.itemsToEject;
    }

    void markEjectionFinished() {
        this.totalEjectionsNeeded = 0;
        this.markChanged();
    }

    void setItemsToEject(List<ItemStack> pItemsToEject) {
        this.itemsToEject.clear();
        this.itemsToEject.addAll(pItemsToEject);
        this.totalEjectionsNeeded = this.itemsToEject.size();
        this.markChanged();
    }

    ItemStack getNextItemToEject() {
        return this.itemsToEject.isEmpty() ? ItemStack.EMPTY : Objects.requireNonNullElse(this.itemsToEject.get(this.itemsToEject.size() - 1), ItemStack.EMPTY);
    }

    ItemStack popNextItemToEject() {
        if (this.itemsToEject.isEmpty()) {
            return ItemStack.EMPTY;
        } else {
            this.markChanged();
            return Objects.requireNonNullElse(this.itemsToEject.remove(this.itemsToEject.size() - 1), ItemStack.EMPTY);
        }
    }

    void set(VaultServerData pOther) {
        this.stateUpdatingResumesAt = pOther.stateUpdatingResumesAt();
        this.itemsToEject.clear();
        this.itemsToEject.addAll(pOther.itemsToEject);
        this.rewardedPlayers.clear();
        this.rewardedPlayers.addAll(pOther.rewardedPlayers);
    }

    private void markChanged() {
        this.isDirty = true;
    }

    public float ejectionProgress() {
        return this.totalEjectionsNeeded == 1 ? 1.0F : 1.0F - Mth.inverseLerp((float)this.getItemsToEject().size(), 1.0F, (float)this.totalEjectionsNeeded);
    }
}

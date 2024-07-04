package net.minecraft.world.level.block.entity.vault;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

public class VaultSharedData {
    static final String TAG_NAME = "shared_data";
    static Codec<VaultSharedData> CODEC = RecordCodecBuilder.create(
        p_338074_ -> p_338074_.group(
                    ItemStack.lenientOptionalFieldOf("display_item").forGetter(p_324217_ -> p_324217_.displayItem),
                    UUIDUtil.CODEC_LINKED_SET.lenientOptionalFieldOf("connected_players", Set.of()).forGetter(p_324110_ -> p_324110_.connectedPlayers),
                    Codec.DOUBLE
                        .lenientOptionalFieldOf("connected_particles_range", Double.valueOf(VaultConfig.DEFAULT.deactivationRange()))
                        .forGetter(p_323486_ -> p_323486_.connectedParticlesRange)
                )
                .apply(p_338074_, VaultSharedData::new)
    );
    private ItemStack displayItem = ItemStack.EMPTY;
    private Set<UUID> connectedPlayers = new ObjectLinkedOpenHashSet<>();
    private double connectedParticlesRange = VaultConfig.DEFAULT.deactivationRange();
    boolean isDirty;

    VaultSharedData(ItemStack p_324245_, Set<UUID> p_324007_, double p_324069_) {
        this.displayItem = p_324245_;
        this.connectedPlayers.addAll(p_324007_);
        this.connectedParticlesRange = p_324069_;
    }

    VaultSharedData() {
    }

    public ItemStack getDisplayItem() {
        return this.displayItem;
    }

    public boolean hasDisplayItem() {
        return !this.displayItem.isEmpty();
    }

    public void setDisplayItem(ItemStack pDisplayItem) {
        if (!ItemStack.matches(this.displayItem, pDisplayItem)) {
            this.displayItem = pDisplayItem.copy();
            this.markDirty();
        }
    }

    boolean hasConnectedPlayers() {
        return !this.connectedPlayers.isEmpty();
    }

    Set<UUID> getConnectedPlayers() {
        return this.connectedPlayers;
    }

    double connectedParticlesRange() {
        return this.connectedParticlesRange;
    }

    void updateConnectedPlayersWithinRange(ServerLevel pLevel, BlockPos pPos, VaultServerData pServerData, VaultConfig pConfig, double pDeactivationRange) {
        Set<UUID> set = pConfig.playerDetector()
            .detect(pLevel, pConfig.entitySelector(), pPos, pDeactivationRange, false)
            .stream()
            .filter(p_324308_ -> !pServerData.getRewardedPlayers().contains(p_324308_))
            .collect(Collectors.toSet());
        if (!this.connectedPlayers.equals(set)) {
            this.connectedPlayers = set;
            this.markDirty();
        }
    }

    private void markDirty() {
        this.isDirty = true;
    }

    void set(VaultSharedData pOther) {
        this.displayItem = pOther.displayItem;
        this.connectedPlayers = pOther.connectedPlayers;
        this.connectedParticlesRange = pOther.connectedParticlesRange;
    }
}

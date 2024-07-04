package net.minecraft.network.protocol.game;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketType;
import net.minecraft.world.item.trading.MerchantOffers;

public class ClientboundMerchantOffersPacket implements Packet<ClientGamePacketListener> {
    public static final StreamCodec<RegistryFriendlyByteBuf, ClientboundMerchantOffersPacket> STREAM_CODEC = Packet.codec(
        ClientboundMerchantOffersPacket::write, ClientboundMerchantOffersPacket::new
    );
    private final int containerId;
    private final MerchantOffers offers;
    private final int villagerLevel;
    private final int villagerXp;
    private final boolean showProgress;
    private final boolean canRestock;

    public ClientboundMerchantOffersPacket(int pContainerId, MerchantOffers pOffers, int pVillagerLevel, int pVillagerXp, boolean pShowProgress, boolean pCanRestock) {
        this.containerId = pContainerId;
        this.offers = pOffers.copy();
        this.villagerLevel = pVillagerLevel;
        this.villagerXp = pVillagerXp;
        this.showProgress = pShowProgress;
        this.canRestock = pCanRestock;
    }

    private ClientboundMerchantOffersPacket(RegistryFriendlyByteBuf p_320716_) {
        this.containerId = p_320716_.readVarInt();
        this.offers = MerchantOffers.STREAM_CODEC.decode(p_320716_);
        this.villagerLevel = p_320716_.readVarInt();
        this.villagerXp = p_320716_.readVarInt();
        this.showProgress = p_320716_.readBoolean();
        this.canRestock = p_320716_.readBoolean();
    }

    private void write(RegistryFriendlyByteBuf p_320747_) {
        p_320747_.writeVarInt(this.containerId);
        MerchantOffers.STREAM_CODEC.encode(p_320747_, this.offers);
        p_320747_.writeVarInt(this.villagerLevel);
        p_320747_.writeVarInt(this.villagerXp);
        p_320747_.writeBoolean(this.showProgress);
        p_320747_.writeBoolean(this.canRestock);
    }

    @Override
    public PacketType<ClientboundMerchantOffersPacket> type() {
        return GamePacketTypes.CLIENTBOUND_MERCHANT_OFFERS;
    }

    /**
     * Passes this Packet on to the NetHandler for processing.
     */
    public void handle(ClientGamePacketListener pHandler) {
        pHandler.handleMerchantOffers(this);
    }

    public int getContainerId() {
        return this.containerId;
    }

    public MerchantOffers getOffers() {
        return this.offers;
    }

    public int getVillagerLevel() {
        return this.villagerLevel;
    }

    public int getVillagerXp() {
        return this.villagerXp;
    }

    public boolean showProgress() {
        return this.showProgress;
    }

    public boolean canRestock() {
        return this.canRestock;
    }
}

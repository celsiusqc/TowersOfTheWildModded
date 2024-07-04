package net.minecraft.client;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQueryPacket;
import net.minecraft.network.protocol.game.ServerboundEntityTagQueryPacket;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugQueryHandler {
    private final ClientPacketListener connection;
    private int transactionId = -1;
    @Nullable
    private Consumer<CompoundTag> callback;

    public DebugQueryHandler(ClientPacketListener pConnection) {
        this.connection = pConnection;
    }

    public boolean handleResponse(int pTransactionId, @Nullable CompoundTag pTag) {
        if (this.transactionId == pTransactionId && this.callback != null) {
            this.callback.accept(pTag);
            this.callback = null;
            return true;
        } else {
            return false;
        }
    }

    private int startTransaction(Consumer<CompoundTag> pCallback) {
        this.callback = pCallback;
        return ++this.transactionId;
    }

    public void queryEntityTag(int pEntId, Consumer<CompoundTag> pTag) {
        int i = this.startTransaction(pTag);
        this.connection.send(new ServerboundEntityTagQueryPacket(i, pEntId));
    }

    public void queryBlockEntityTag(BlockPos pPos, Consumer<CompoundTag> pTag) {
        int i = this.startTransaction(pTag);
        this.connection.send(new ServerboundBlockEntityTagQueryPacket(i, pPos));
    }
}

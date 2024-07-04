package net.minecraft.world.level.saveddata.maps;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

public class MapFrame {
    private final BlockPos pos;
    private final int rotation;
    private final int entityId;

    public MapFrame(BlockPos pPos, int pRotation, int pEntityId) {
        this.pos = pPos;
        this.rotation = pRotation;
        this.entityId = pEntityId;
    }

    @Nullable
    public static MapFrame load(CompoundTag pCompoundTag) {
        Optional<BlockPos> optional = NbtUtils.readBlockPos(pCompoundTag, "pos");
        if (optional.isEmpty()) {
            return null;
        } else {
            int i = pCompoundTag.getInt("rotation");
            int j = pCompoundTag.getInt("entity_id");
            return new MapFrame(optional.get(), i, j);
        }
    }

    public CompoundTag save() {
        CompoundTag compoundtag = new CompoundTag();
        compoundtag.put("pos", NbtUtils.writeBlockPos(this.pos));
        compoundtag.putInt("rotation", this.rotation);
        compoundtag.putInt("entity_id", this.entityId);
        return compoundtag;
    }

    public BlockPos getPos() {
        return this.pos;
    }

    public int getRotation() {
        return this.rotation;
    }

    public int getEntityId() {
        return this.entityId;
    }

    public String getId() {
        return frameId(this.pos);
    }

    public static String frameId(BlockPos pPos) {
        return "frame-" + pPos.getX() + "," + pPos.getY() + "," + pPos.getZ();
    }
}

package net.minecraft.network.syncher;

/**
 * A Key for {@link SynchedEntityData}.
 */
public record EntityDataAccessor<T>(int id, EntityDataSerializer<T> serializer) {
    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else if (pOther != null && this.getClass() == pOther.getClass()) {
            EntityDataAccessor<?> entitydataaccessor = (EntityDataAccessor<?>)pOther;
            return this.id == entitydataaccessor.id;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public String toString() {
        return "<entity data: " + this.id + ">";
    }
}

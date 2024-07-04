package net.minecraft.network.syncher;

import java.util.List;

public interface SyncedDataHolder {
    void onSyncedDataUpdated(EntityDataAccessor<?> pDataAccessor);

    void onSyncedDataUpdated(List<SynchedEntityData.DataValue<?>> pNewData);
}

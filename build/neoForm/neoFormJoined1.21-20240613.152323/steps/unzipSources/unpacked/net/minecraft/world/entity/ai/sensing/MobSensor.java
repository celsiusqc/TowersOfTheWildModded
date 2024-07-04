package net.minecraft.world.entity.ai.sensing;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class MobSensor<T extends LivingEntity> extends Sensor<T> {
    private final BiPredicate<T, LivingEntity> mobTest;
    private final Predicate<T> readyTest;
    private final MemoryModuleType<Boolean> toSet;
    private final int memoryTimeToLive;

    public MobSensor(int pScanRate, BiPredicate<T, LivingEntity> pMobTest, Predicate<T> pReadyTest, MemoryModuleType<Boolean> pToSet, int pMemoryTimeToLive) {
        super(pScanRate);
        this.mobTest = pMobTest;
        this.readyTest = pReadyTest;
        this.toSet = pToSet;
        this.memoryTimeToLive = pMemoryTimeToLive;
    }

    @Override
    protected void doTick(ServerLevel pLevel, T pEntity) {
        if (!this.readyTest.test(pEntity)) {
            this.clearMemory(pEntity);
        } else {
            this.checkForMobsNearby(pEntity);
        }
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return Set.of(MemoryModuleType.NEAREST_LIVING_ENTITIES);
    }

    public void checkForMobsNearby(T pSensingEntity) {
        Optional<List<LivingEntity>> optional = pSensingEntity.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES);
        if (!optional.isEmpty()) {
            boolean flag = optional.get().stream().anyMatch(p_316324_ -> this.mobTest.test(pSensingEntity, p_316324_));
            if (flag) {
                this.mobDetected(pSensingEntity);
            }
        }
    }

    public void mobDetected(T pSensingEntity) {
        pSensingEntity.getBrain().setMemoryWithExpiry(this.toSet, true, (long)this.memoryTimeToLive);
    }

    public void clearMemory(T pSensingEntity) {
        pSensingEntity.getBrain().eraseMemory(this.toSet);
    }
}

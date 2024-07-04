package net.minecraft.world.level.gameevent;

import net.minecraft.core.Holder;
import net.minecraft.world.phys.Vec3;

public interface GameEventListenerRegistry {
    GameEventListenerRegistry NOOP = new GameEventListenerRegistry() {
        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public void register(GameEventListener p_251092_) {
        }

        @Override
        public void unregister(GameEventListener p_251937_) {
        }

        @Override
        public boolean visitInRangeListeners(
            Holder<GameEvent> p_316158_, Vec3 p_249086_, GameEvent.Context p_249012_, GameEventListenerRegistry.ListenerVisitor p_252106_
        ) {
            return false;
        }
    };

    boolean isEmpty();

    void register(GameEventListener pListener);

    void unregister(GameEventListener pListener);

    boolean visitInRangeListeners(Holder<GameEvent> pGameEvent, Vec3 pPos, GameEvent.Context pContext, GameEventListenerRegistry.ListenerVisitor pVisitor);

    @FunctionalInterface
    public interface ListenerVisitor {
        void visit(GameEventListener pListener, Vec3 pPos);
    }
}

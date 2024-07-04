package net.minecraft.gametest.framework;

public interface GameTestBatchListener {
    void testBatchStarting(GameTestBatch pBatch);

    void testBatchFinished(GameTestBatch pBatch);
}

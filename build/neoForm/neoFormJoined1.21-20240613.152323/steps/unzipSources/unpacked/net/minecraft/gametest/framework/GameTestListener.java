package net.minecraft.gametest.framework;

public interface GameTestListener {
    void testStructureLoaded(GameTestInfo pTestInfo);

    void testPassed(GameTestInfo pTest, GameTestRunner pRunner);

    void testFailed(GameTestInfo pTest, GameTestRunner pRunner);

    void testAddedForRerun(GameTestInfo pOldTest, GameTestInfo pNewTest, GameTestRunner pRunner);
}

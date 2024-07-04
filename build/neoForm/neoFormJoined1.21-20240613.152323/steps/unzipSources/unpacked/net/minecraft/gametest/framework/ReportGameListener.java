package net.minecraft.gametest.framework;

import com.google.common.base.MoreObjects;
import java.util.Arrays;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WritableBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.commons.lang3.exception.ExceptionUtils;

class ReportGameListener implements GameTestListener {
    private int attempts = 0;
    private int successes = 0;

    public ReportGameListener() {
    }

    @Override
    public void testStructureLoaded(GameTestInfo pTestInfo) {
        spawnBeacon(pTestInfo, Blocks.LIGHT_GRAY_STAINED_GLASS);
        this.attempts++;
    }

    private void handleRetry(GameTestInfo pTestInfo, GameTestRunner pRunner, boolean pPassed) {
        RetryOptions retryoptions = pTestInfo.retryOptions();
        String s = String.format("[Run: %4d, Ok: %4d, Fail: %4d", this.attempts, this.successes, this.attempts - this.successes);
        if (!retryoptions.unlimitedTries()) {
            s = s + String.format(", Left: %4d", retryoptions.numberOfTries() - this.attempts);
        }

        s = s + "]";
        String s1 = pTestInfo.getTestName() + " " + (pPassed ? "passed" : "failed") + "! " + pTestInfo.getRunTime() + "ms";
        String s2 = String.format("%-53s%s", s, s1);
        if (pPassed) {
            reportPassed(pTestInfo, s2);
        } else {
            say(pTestInfo.getLevel(), ChatFormatting.RED, s2);
        }

        if (retryoptions.hasTriesLeft(this.attempts, this.successes)) {
            pRunner.rerunTest(pTestInfo);
        }
    }

    @Override
    public void testPassed(GameTestInfo pTest, GameTestRunner pRunner) {
        this.successes++;
        if (pTest.retryOptions().hasRetries()) {
            this.handleRetry(pTest, pRunner, true);
        } else if (!pTest.isFlaky()) {
            reportPassed(pTest, pTest.getTestName() + " passed! (" + pTest.getRunTime() + "ms)");
        } else {
            if (this.successes >= pTest.requiredSuccesses()) {
                reportPassed(pTest, pTest + " passed " + this.successes + " times of " + this.attempts + " attempts.");
            } else {
                say(
                    pTest.getLevel(),
                    ChatFormatting.GREEN,
                    "Flaky test " + pTest + " succeeded, attempt: " + this.attempts + " successes: " + this.successes
                );
                pRunner.rerunTest(pTest);
            }
        }
    }

    @Override
    public void testFailed(GameTestInfo pTest, GameTestRunner pRunner) {
        if (!pTest.isFlaky()) {
            reportFailure(pTest, pTest.getError());
            if (pTest.retryOptions().hasRetries()) {
                this.handleRetry(pTest, pRunner, false);
            }
        } else {
            TestFunction testfunction = pTest.getTestFunction();
            String s = "Flaky test " + pTest + " failed, attempt: " + this.attempts + "/" + testfunction.maxAttempts();
            if (testfunction.requiredSuccesses() > 1) {
                s = s + ", successes: " + this.successes + " (" + testfunction.requiredSuccesses() + " required)";
            }

            say(pTest.getLevel(), ChatFormatting.YELLOW, s);
            if (pTest.maxAttempts() - this.attempts + this.successes >= pTest.requiredSuccesses()) {
                pRunner.rerunTest(pTest);
            } else {
                reportFailure(pTest, new ExhaustedAttemptsException(this.attempts, this.successes, pTest));
            }
        }
    }

    @Override
    public void testAddedForRerun(GameTestInfo pOldTest, GameTestInfo pNewTest, GameTestRunner pRunner) {
        pNewTest.addListener(this);
    }

    public static void reportPassed(GameTestInfo pTestInfo, String pMessage) {
        updateBeaconGlass(pTestInfo, Blocks.LIME_STAINED_GLASS);
        visualizePassedTest(pTestInfo, pMessage);
    }

    private static void visualizePassedTest(GameTestInfo pTestInfo, String pMessage) {
        say(pTestInfo.getLevel(), ChatFormatting.GREEN, pMessage);
        GlobalTestReporter.onTestSuccess(pTestInfo);
    }

    protected static void reportFailure(GameTestInfo pTestInfo, Throwable pError) {
        updateBeaconGlass(pTestInfo, pTestInfo.isRequired() ? Blocks.RED_STAINED_GLASS : Blocks.ORANGE_STAINED_GLASS);
        spawnLectern(pTestInfo, Util.describeError(pError));
        visualizeFailedTest(pTestInfo, pError);
    }

    protected static void visualizeFailedTest(GameTestInfo pTestInfo, Throwable pError) {
        String s = pError.getMessage() + (pError.getCause() == null ? "" : " cause: " + Util.describeError(pError.getCause()));
        String s1 = (pTestInfo.isRequired() ? "" : "(optional) ") + pTestInfo.getTestName() + " failed! " + s;
        say(pTestInfo.getLevel(), pTestInfo.isRequired() ? ChatFormatting.RED : ChatFormatting.YELLOW, s1);
        Throwable throwable = MoreObjects.firstNonNull(ExceptionUtils.getRootCause(pError), pError);
        if (throwable instanceof GameTestAssertPosException gametestassertposexception) {
            showRedBox(pTestInfo.getLevel(), gametestassertposexception.getAbsolutePos(), gametestassertposexception.getMessageToShowAtBlock());
        }

        GlobalTestReporter.onTestFailed(pTestInfo);
    }

    protected static void spawnBeacon(GameTestInfo pTestInfo, Block pBlock) {
        ServerLevel serverlevel = pTestInfo.getLevel();
        BlockPos blockpos = getBeaconPos(pTestInfo);
        serverlevel.setBlockAndUpdate(blockpos, Blocks.BEACON.defaultBlockState().rotate(pTestInfo.getRotation()));
        updateBeaconGlass(pTestInfo, pBlock);

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos blockpos1 = blockpos.offset(i, -1, j);
                serverlevel.setBlockAndUpdate(blockpos1, Blocks.IRON_BLOCK.defaultBlockState());
            }
        }
    }

    private static BlockPos getBeaconPos(GameTestInfo p_352434_) {
        BlockPos blockpos = p_352434_.getStructureBlockPos();
        BlockPos blockpos1 = new BlockPos(-1, -2, -1);
        return StructureTemplate.transform(blockpos.offset(blockpos1), Mirror.NONE, p_352434_.getRotation(), blockpos);
    }

    private static void updateBeaconGlass(GameTestInfo p_352228_, Block p_352195_) {
        ServerLevel serverlevel = p_352228_.getLevel();
        BlockPos blockpos = getBeaconPos(p_352228_);
        if (serverlevel.getBlockState(blockpos).is(Blocks.BEACON)) {
            BlockPos blockpos1 = blockpos.offset(0, 1, 0);
            serverlevel.setBlockAndUpdate(blockpos1, p_352195_.defaultBlockState());
        }
    }

    private static void spawnLectern(GameTestInfo pTestInfo, String pMessage) {
        ServerLevel serverlevel = pTestInfo.getLevel();
        BlockPos blockpos = pTestInfo.getStructureBlockPos();
        BlockPos blockpos1 = new BlockPos(-1, 0, -1);
        BlockPos blockpos2 = StructureTemplate.transform(blockpos.offset(blockpos1), Mirror.NONE, pTestInfo.getRotation(), blockpos);
        serverlevel.setBlockAndUpdate(blockpos2, Blocks.LECTERN.defaultBlockState().rotate(pTestInfo.getRotation()));
        BlockState blockstate = serverlevel.getBlockState(blockpos2);
        ItemStack itemstack = createBook(pTestInfo.getTestName(), pTestInfo.isRequired(), pMessage);
        LecternBlock.tryPlaceBook(null, serverlevel, blockpos2, blockstate, itemstack);
    }

    private static ItemStack createBook(String pTestName, boolean pRequired, String pMessage) {
        StringBuffer stringbuffer = new StringBuffer();
        Arrays.stream(pTestName.split("\\.")).forEach(p_177716_ -> stringbuffer.append(p_177716_).append('\n'));
        if (!pRequired) {
            stringbuffer.append("(optional)\n");
        }

        stringbuffer.append("-------------------\n");
        ItemStack itemstack = new ItemStack(Items.WRITABLE_BOOK);
        itemstack.set(DataComponents.WRITABLE_BOOK_CONTENT, new WritableBookContent(List.of(Filterable.passThrough(stringbuffer + pMessage))));
        return itemstack;
    }

    protected static void say(ServerLevel pServerLevel, ChatFormatting pFormatting, String pMessage) {
        pServerLevel.getPlayers(p_177705_ -> true).forEach(p_177709_ -> p_177709_.sendSystemMessage(Component.literal(pMessage).withStyle(pFormatting)));
    }

    private static void showRedBox(ServerLevel pServerLevel, BlockPos pPos, String pDisplayMessage) {
        DebugPackets.sendGameTestAddMarker(pServerLevel, pPos, pDisplayMessage, -2130771968, Integer.MAX_VALUE);
    }
}

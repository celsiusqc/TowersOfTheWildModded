package net.minecraft.commands.execution;

import net.minecraft.resources.ResourceLocation;

public interface TraceCallbacks extends AutoCloseable {
    void onCommand(int pDepth, String pCommand);

    void onReturn(int pDepth, String pCommand, int pReturnValue);

    void onError(String pErrorMessage);

    void onCall(int pDepth, ResourceLocation pFunction, int pCommands);

    @Override
    void close();
}

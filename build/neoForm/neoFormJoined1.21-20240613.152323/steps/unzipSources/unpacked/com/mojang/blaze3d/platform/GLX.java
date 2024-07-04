package com.mojang.blaze3d.platform;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.DontObfuscate;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import net.minecraft.client.renderer.GameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWErrorCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

@OnlyIn(Dist.CLIENT)
@DontObfuscate
public class GLX {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static String cpuInfo;

    public static String getOpenGLVersionString() {
        RenderSystem.assertOnRenderThread();
        return GLFW.glfwGetCurrentContext() == 0L
            ? "NO CONTEXT"
            : GlStateManager._getString(7937) + " GL version " + GlStateManager._getString(7938) + ", " + GlStateManager._getString(7936);
    }

    public static int _getRefreshRate(Window pWindow) {
        RenderSystem.assertOnRenderThread();
        long i = GLFW.glfwGetWindowMonitor(pWindow.getWindow());
        if (i == 0L) {
            i = GLFW.glfwGetPrimaryMonitor();
        }

        GLFWVidMode glfwvidmode = i == 0L ? null : GLFW.glfwGetVideoMode(i);
        return glfwvidmode == null ? 0 : glfwvidmode.refreshRate();
    }

    public static String _getLWJGLVersion() {
        return Version.getVersion();
    }

    public static LongSupplier _initGlfw() {
        Window.checkGlfwError((p_242032_, p_242033_) -> {
            throw new IllegalStateException(String.format(Locale.ROOT, "GLFW error before init: [0x%X]%s", p_242032_, p_242033_));
        });
        List<String> list = Lists.newArrayList();
        GLFWErrorCallback glfwerrorcallback = GLFW.glfwSetErrorCallback((p_304051_, p_304052_) -> {
            String s1 = p_304052_ == 0L ? "" : MemoryUtil.memUTF8(p_304052_);
            list.add(String.format(Locale.ROOT, "GLFW error during init: [0x%X]%s", p_304051_, s1));
        });
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Failed to initialize GLFW, errors: " + Joiner.on(",").join(list));
        } else {
            LongSupplier longsupplier = () -> (long)(GLFW.glfwGetTime() * 1.0E9);

            for (String s : list) {
                LOGGER.error("GLFW error collected during initialization: {}", s);
            }

            RenderSystem.setErrorCallback(glfwerrorcallback);
            return longsupplier;
        }
    }

    public static void _setGlfwErrorCallback(GLFWErrorCallbackI pErrorCallback) {
        GLFWErrorCallback glfwerrorcallback = GLFW.glfwSetErrorCallback(pErrorCallback);
        if (glfwerrorcallback != null) {
            glfwerrorcallback.free();
        }
    }

    public static boolean _shouldClose(Window pWindow) {
        return GLFW.glfwWindowShouldClose(pWindow.getWindow());
    }

    public static void _init(int pDebugVerbosity, boolean pSynchronous) {
        try {
            CentralProcessor centralprocessor = new SystemInfo().getHardware().getProcessor();
            cpuInfo = String.format(Locale.ROOT, "%dx %s", centralprocessor.getLogicalProcessorCount(), centralprocessor.getProcessorIdentifier().getName())
                .replaceAll("\\s+", " ");
        } catch (Throwable throwable) {
        }

        GlDebug.enableDebugCallback(pDebugVerbosity, pSynchronous);
    }

    public static String _getCpuInfo() {
        return cpuInfo == null ? "<unknown>" : cpuInfo;
    }

    public static void _renderCrosshair(int pLineLength, boolean pRenderX, boolean pRenderY, boolean pRenderZ) {
        if (pRenderX || pRenderY || pRenderZ) {
            RenderSystem.assertOnRenderThread();
            GlStateManager._depthMask(false);
            GlStateManager._disableCull();
            RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
            Tesselator tesselator = RenderSystem.renderThreadTesselator();
            BufferBuilder bufferbuilder = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            RenderSystem.lineWidth(4.0F);
            if (pRenderX) {
                bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(1.0F, 0.0F, 0.0F);
                bufferbuilder.addVertex((float)pLineLength, 0.0F, 0.0F).setColor(-16777216).setNormal(1.0F, 0.0F, 0.0F);
            }

            if (pRenderY) {
                bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(0.0F, 1.0F, 0.0F);
                bufferbuilder.addVertex(0.0F, (float)pLineLength, 0.0F).setColor(-16777216).setNormal(0.0F, 1.0F, 0.0F);
            }

            if (pRenderZ) {
                bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16777216).setNormal(0.0F, 0.0F, 1.0F);
                bufferbuilder.addVertex(0.0F, 0.0F, (float)pLineLength).setColor(-16777216).setNormal(0.0F, 0.0F, 1.0F);
            }

            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
            RenderSystem.lineWidth(2.0F);
            bufferbuilder = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
            if (pRenderX) {
                bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-65536).setNormal(1.0F, 0.0F, 0.0F);
                bufferbuilder.addVertex((float)pLineLength, 0.0F, 0.0F).setColor(-65536).setNormal(1.0F, 0.0F, 0.0F);
            }

            if (pRenderY) {
                bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-16711936).setNormal(0.0F, 1.0F, 0.0F);
                bufferbuilder.addVertex(0.0F, (float)pLineLength, 0.0F).setColor(-16711936).setNormal(0.0F, 1.0F, 0.0F);
            }

            if (pRenderZ) {
                bufferbuilder.addVertex(0.0F, 0.0F, 0.0F).setColor(-8421377).setNormal(0.0F, 0.0F, 1.0F);
                bufferbuilder.addVertex(0.0F, 0.0F, (float)pLineLength).setColor(-8421377).setNormal(0.0F, 0.0F, 1.0F);
            }

            BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
            RenderSystem.lineWidth(1.0F);
            GlStateManager._enableCull();
            GlStateManager._depthMask(true);
        }
    }

    public static <T> T make(Supplier<T> pSupplier) {
        return pSupplier.get();
    }

    public static <T> T make(T pValue, Consumer<T> pConsumer) {
        pConsumer.accept(pValue);
        return pValue;
    }
}

package com.mojang.blaze3d.shaders;

import com.mojang.blaze3d.preprocessor.GlslPreprocessor;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import java.io.InputStream;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EffectProgram extends Program {
    private static final GlslPreprocessor PREPROCESSOR = new GlslPreprocessor() {
        @Override
        public String applyImport(boolean p_166595_, String p_166596_) {
            return "#error Import statement not supported";
        }
    };
    private int references;

    private EffectProgram(Program.Type pType, int pId, String pName) {
        super(pType, pId, pName);
    }

    public void attachToEffect(Effect pEffect) {
        RenderSystem.assertOnRenderThread();
        this.references++;
        this.attachToShader(pEffect);
    }

    @Override
    public void close() {
        RenderSystem.assertOnRenderThread();
        this.references--;
        if (this.references <= 0) {
            super.close();
        }
    }

    public static EffectProgram compileShader(Program.Type pType, String pName, InputStream pShaderData, String pSourceName) throws IOException {
        RenderSystem.assertOnRenderThread();
        int i = compileShaderInternal(pType, pName, pShaderData, pSourceName, PREPROCESSOR);
        EffectProgram effectprogram = new EffectProgram(pType, i, pName);
        pType.getPrograms().put(pName, effectprogram);
        return effectprogram;
    }
}

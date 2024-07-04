package com.mojang.blaze3d.vertex;

import com.mojang.blaze3d.platform.GlStateManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record VertexFormatElement(int id, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
    public static final int MAX_COUNT = 32;
    private static final VertexFormatElement[] BY_ID = new VertexFormatElement[32];
    private static final List<VertexFormatElement> ELEMENTS = new ArrayList<>(32);
    public static final VertexFormatElement POSITION = register(0, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.POSITION, 3);
    public static final VertexFormatElement COLOR = register(1, 0, VertexFormatElement.Type.UBYTE, VertexFormatElement.Usage.COLOR, 4);
    public static final VertexFormatElement UV0 = register(2, 0, VertexFormatElement.Type.FLOAT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement UV = UV0;
    public static final VertexFormatElement UV1 = register(3, 1, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement UV2 = register(4, 2, VertexFormatElement.Type.SHORT, VertexFormatElement.Usage.UV, 2);
    public static final VertexFormatElement NORMAL = register(5, 0, VertexFormatElement.Type.BYTE, VertexFormatElement.Usage.NORMAL, 3);

    public VertexFormatElement(int id, int index, VertexFormatElement.Type type, VertexFormatElement.Usage usage, int count) {
        if (id < 0 || id >= BY_ID.length) {
            throw new IllegalArgumentException("Element ID must be in range [0; " + BY_ID.length + ")");
        } else if (!this.supportsUsage(index, usage)) {
            throw new IllegalStateException("Multiple vertex elements of the same type other than UVs are not supported");
        } else {
            this.id = id;
            this.index = index;
            this.type = type;
            this.usage = usage;
            this.count = count;
        }
    }

    public static VertexFormatElement register(
        int p_350751_, int p_350658_, VertexFormatElement.Type p_350861_, VertexFormatElement.Usage p_350763_, int p_350519_
    ) {
        VertexFormatElement vertexformatelement = new VertexFormatElement(p_350751_, p_350658_, p_350861_, p_350763_, p_350519_);
        if (BY_ID[p_350751_] != null) {
            throw new IllegalArgumentException("Duplicate element registration for: " + p_350751_);
        } else {
            BY_ID[p_350751_] = vertexformatelement;
            ELEMENTS.add(vertexformatelement);
            return vertexformatelement;
        }
    }

    private boolean supportsUsage(int pIndex, VertexFormatElement.Usage pUsage) {
        return pIndex == 0 || pUsage == VertexFormatElement.Usage.UV;
    }

    @Override
    public String toString() {
        return this.count + "," + this.usage + "," + this.type + " (" + this.id + ")";
    }

    public int mask() {
        return 1 << this.id;
    }

    public int byteSize() {
        return this.type.size() * this.count;
    }

    public void setupBufferState(int pStateIndex, long pOffset, int pStride) {
        this.usage.setupState.setupBufferState(this.count, this.type.glType(), pStride, pOffset, pStateIndex);
    }

    @Nullable
    public static VertexFormatElement byId(int p_350894_) {
        return BY_ID[p_350894_];
    }

    public static Stream<VertexFormatElement> elementsFromMask(int p_350349_) {
        return ELEMENTS.stream().filter(p_350573_ -> p_350573_ != null && (p_350349_ & p_350573_.mask()) != 0);
    }

    public static int findNextId() {
        for (int i = 0; i < BY_ID.length; i++) {
            if (BY_ID[i] == null) {
                return i;
            }
        }
        throw new IllegalStateException("VertexFormatElement count limit exceeded");
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Type {
        FLOAT(4, "Float", 5126),
        UBYTE(1, "Unsigned Byte", 5121),
        BYTE(1, "Byte", 5120),
        USHORT(2, "Unsigned Short", 5123),
        SHORT(2, "Short", 5122),
        UINT(4, "Unsigned Int", 5125),
        INT(4, "Int", 5124);

        private final int size;
        private final String name;
        private final int glType;

        private Type(int pSize, String pName, int pGlType) {
            this.size = pSize;
            this.name = pName;
            this.glType = pGlType;
        }

        public int size() {
            return this.size;
        }

        public int glType() {
            return this.glType;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Usage implements net.neoforged.neoforge.common.IExtensibleEnum {
        POSITION(
            "Position",
            (p_349733_, p_349734_, p_349735_, p_349736_, p_349737_) -> GlStateManager._vertexAttribPointer(
                    p_349737_, p_349733_, p_349734_, false, p_349735_, p_349736_
                )
        ),
        NORMAL(
            "Normal",
            (p_349718_, p_349719_, p_349720_, p_349721_, p_349722_) -> GlStateManager._vertexAttribPointer(
                    p_349722_, p_349718_, p_349719_, true, p_349720_, p_349721_
                )
        ),
        COLOR(
            "Vertex Color",
            (p_349713_, p_349714_, p_349715_, p_349716_, p_349717_) -> GlStateManager._vertexAttribPointer(
                    p_349717_, p_349713_, p_349714_, true, p_349715_, p_349716_
                )
        ),
        UV("UV", (p_349723_, p_349724_, p_349725_, p_349726_, p_349727_) -> {
            if (p_349724_ == 5126) {
                GlStateManager._vertexAttribPointer(p_349727_, p_349723_, p_349724_, false, p_349725_, p_349726_);
            } else {
                GlStateManager._vertexAttribIPointer(p_349727_, p_349723_, p_349724_, p_349725_, p_349726_);
            }
        }),
        GENERIC(
            "Generic",
            (p_349728_, p_349729_, p_349730_, p_349731_, p_349732_) -> GlStateManager._vertexAttribPointer(
                    p_349732_, p_349728_, p_349729_, false, p_349730_, p_349731_
                )
        );

        private final String name;
        final VertexFormatElement.Usage.SetupState setupState;

        private Usage(String p_166975_, VertexFormatElement.Usage.SetupState p_166976_) {
            this.name = p_166975_;
            this.setupState = p_166976_;
        }

        @Override
        public String toString() {
            return this.name;
        }

        @FunctionalInterface
        @OnlyIn(Dist.CLIENT)
        public interface SetupState {
            void setupBufferState(int p_167053_, int p_167054_, int p_167055_, long p_167056_, int p_167057_);
        }

        public static Usage create(String name, String usageName, SetupState setupState) {
            throw new IllegalArgumentException("Enum not extended");
        }
    }
}

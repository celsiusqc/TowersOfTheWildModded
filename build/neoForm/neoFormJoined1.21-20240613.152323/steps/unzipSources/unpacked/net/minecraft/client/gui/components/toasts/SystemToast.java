package net.minecraft.client.gui.components.toasts;

import com.google.common.collect.ImmutableList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SystemToast implements Toast {
    private static final ResourceLocation BACKGROUND_SPRITE = ResourceLocation.withDefaultNamespace("toast/system");
    private static final int MAX_LINE_SIZE = 200;
    private static final int LINE_SPACING = 12;
    private static final int MARGIN = 10;
    private final SystemToast.SystemToastId id;
    private Component title;
    private List<FormattedCharSequence> messageLines;
    private long lastChanged;
    private boolean changed;
    private final int width;
    private boolean forceHide;

    public SystemToast(SystemToast.SystemToastId pId, Component pTitle, @Nullable Component pMessage) {
        this(
            pId,
            pTitle,
            nullToEmpty(pMessage),
            Math.max(160, 30 + Math.max(Minecraft.getInstance().font.width(pTitle), pMessage == null ? 0 : Minecraft.getInstance().font.width(pMessage)))
        );
    }

    public static SystemToast multiline(Minecraft pMinecraft, SystemToast.SystemToastId pId, Component pTitle, Component pMessage) {
        Font font = pMinecraft.font;
        List<FormattedCharSequence> list = font.split(pMessage, 200);
        int i = Math.max(200, list.stream().mapToInt(font::width).max().orElse(200));
        return new SystemToast(pId, pTitle, list, i + 30);
    }

    private SystemToast(SystemToast.SystemToastId pId, Component pTitle, List<FormattedCharSequence> pMessageLines, int pWidth) {
        this.id = pId;
        this.title = pTitle;
        this.messageLines = pMessageLines;
        this.width = pWidth;
    }

    private static ImmutableList<FormattedCharSequence> nullToEmpty(@Nullable Component pMessage) {
        return pMessage == null ? ImmutableList.of() : ImmutableList.of(pMessage.getVisualOrderText());
    }

    @Override
    public int width() {
        return this.width;
    }

    @Override
    public int height() {
        return 20 + Math.max(this.messageLines.size(), 1) * 12;
    }

    public void forceHide() {
        this.forceHide = true;
    }

    @Override
    public Toast.Visibility render(GuiGraphics pGuiGraphics, ToastComponent pToastComponent, long pTimeSinceLastVisible) {
        if (this.changed) {
            this.lastChanged = pTimeSinceLastVisible;
            this.changed = false;
        }

        int i = this.width();
        if (i == 160 && this.messageLines.size() <= 1) {
            pGuiGraphics.blitSprite(BACKGROUND_SPRITE, 0, 0, i, this.height());
        } else {
            int j = this.height();
            int k = 28;
            int l = Math.min(4, j - 28);
            this.renderBackgroundRow(pGuiGraphics, i, 0, 0, 28);

            for (int i1 = 28; i1 < j - l; i1 += 10) {
                this.renderBackgroundRow(pGuiGraphics, i, 16, i1, Math.min(16, j - i1 - l));
            }

            this.renderBackgroundRow(pGuiGraphics, i, 32 - l, j - l, l);
        }

        if (this.messageLines.isEmpty()) {
            pGuiGraphics.drawString(pToastComponent.getMinecraft().font, this.title, 18, 12, -256, false);
        } else {
            pGuiGraphics.drawString(pToastComponent.getMinecraft().font, this.title, 18, 7, -256, false);

            for (int j1 = 0; j1 < this.messageLines.size(); j1++) {
                pGuiGraphics.drawString(pToastComponent.getMinecraft().font, this.messageLines.get(j1), 18, 18 + j1 * 12, -1, false);
            }
        }

        double d0 = (double)this.id.displayTime * pToastComponent.getNotificationDisplayTimeMultiplier();
        long k1 = pTimeSinceLastVisible - this.lastChanged;
        return !this.forceHide && (double)k1 < d0 ? Toast.Visibility.SHOW : Toast.Visibility.HIDE;
    }

    private void renderBackgroundRow(GuiGraphics pGuiGraphics, int pWidth, int pVOffset, int pY, int pHeight) {
        int i = pVOffset == 0 ? 20 : 5;
        int j = Math.min(60, pWidth - i);
        ResourceLocation resourcelocation = BACKGROUND_SPRITE;
        pGuiGraphics.blitSprite(resourcelocation, 160, 32, 0, pVOffset, 0, pY, i, pHeight);

        for (int k = i; k < pWidth - j; k += 64) {
            pGuiGraphics.blitSprite(resourcelocation, 160, 32, 32, pVOffset, k, pY, Math.min(64, pWidth - k - j), pHeight);
        }

        pGuiGraphics.blitSprite(resourcelocation, 160, 32, 160 - j, pVOffset, pWidth - j, pY, j, pHeight);
    }

    public void reset(Component pTitle, @Nullable Component pMessage) {
        this.title = pTitle;
        this.messageLines = nullToEmpty(pMessage);
        this.changed = true;
    }

    public SystemToast.SystemToastId getToken() {
        return this.id;
    }

    public static void add(ToastComponent pToastComponent, SystemToast.SystemToastId pId, Component pTitle, @Nullable Component pMessage) {
        pToastComponent.addToast(new SystemToast(pId, pTitle, pMessage));
    }

    public static void addOrUpdate(ToastComponent pToastComponent, SystemToast.SystemToastId pId, Component pTitle, @Nullable Component pMessage) {
        SystemToast systemtoast = pToastComponent.getToast(SystemToast.class, pId);
        if (systemtoast == null) {
            add(pToastComponent, pId, pTitle, pMessage);
        } else {
            systemtoast.reset(pTitle, pMessage);
        }
    }

    public static void forceHide(ToastComponent pToastComponent, SystemToast.SystemToastId pId) {
        SystemToast systemtoast = pToastComponent.getToast(SystemToast.class, pId);
        if (systemtoast != null) {
            systemtoast.forceHide();
        }
    }

    public static void onWorldAccessFailure(Minecraft pMinecraft, String pMessage) {
        add(
            pMinecraft.getToasts(),
            SystemToast.SystemToastId.WORLD_ACCESS_FAILURE,
            Component.translatable("selectWorld.access_failure"),
            Component.literal(pMessage)
        );
    }

    public static void onWorldDeleteFailure(Minecraft pMinecraft, String pMessage) {
        add(
            pMinecraft.getToasts(),
            SystemToast.SystemToastId.WORLD_ACCESS_FAILURE,
            Component.translatable("selectWorld.delete_failure"),
            Component.literal(pMessage)
        );
    }

    public static void onPackCopyFailure(Minecraft pMinecraft, String pMessage) {
        add(pMinecraft.getToasts(), SystemToast.SystemToastId.PACK_COPY_FAILURE, Component.translatable("pack.copyFailure"), Component.literal(pMessage));
    }

    public static void onFileDropFailure(Minecraft p_350375_, int p_350447_) {
        add(
            p_350375_.getToasts(),
            SystemToast.SystemToastId.FILE_DROP_FAILURE,
            Component.translatable("gui.fileDropFailure.title"),
            Component.translatable("gui.fileDropFailure.detail", p_350447_)
        );
    }

    public static void onLowDiskSpace(Minecraft pMinecraft) {
        addOrUpdate(
            pMinecraft.getToasts(),
            SystemToast.SystemToastId.LOW_DISK_SPACE,
            Component.translatable("chunk.toast.lowDiskSpace"),
            Component.translatable("chunk.toast.lowDiskSpace.description")
        );
    }

    public static void onChunkLoadFailure(Minecraft pMinecraft, ChunkPos pChunkPos) {
        addOrUpdate(
            pMinecraft.getToasts(),
            SystemToast.SystemToastId.CHUNK_LOAD_FAILURE,
            Component.translatable("chunk.toast.loadFailure", Component.translationArg(pChunkPos)).withStyle(ChatFormatting.RED),
            Component.translatable("chunk.toast.checkLog")
        );
    }

    public static void onChunkSaveFailure(Minecraft pMinecraft, ChunkPos pChunkPos) {
        addOrUpdate(
            pMinecraft.getToasts(),
            SystemToast.SystemToastId.CHUNK_SAVE_FAILURE,
            Component.translatable("chunk.toast.saveFailure", Component.translationArg(pChunkPos)).withStyle(ChatFormatting.RED),
            Component.translatable("chunk.toast.checkLog")
        );
    }

    @OnlyIn(Dist.CLIENT)
    public static class SystemToastId {
        public static final SystemToast.SystemToastId NARRATOR_TOGGLE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId WORLD_BACKUP = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PACK_LOAD_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId WORLD_ACCESS_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PACK_COPY_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId FILE_DROP_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId PERIODIC_NOTIFICATION = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId LOW_DISK_SPACE = new SystemToast.SystemToastId(10000L);
        public static final SystemToast.SystemToastId CHUNK_LOAD_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId CHUNK_SAVE_FAILURE = new SystemToast.SystemToastId();
        public static final SystemToast.SystemToastId UNSECURE_SERVER_WARNING = new SystemToast.SystemToastId(10000L);
        final long displayTime;

        public SystemToastId(long pDisplayTime) {
            this.displayTime = pDisplayTime;
        }

        public SystemToastId() {
            this(5000L);
        }
    }
}

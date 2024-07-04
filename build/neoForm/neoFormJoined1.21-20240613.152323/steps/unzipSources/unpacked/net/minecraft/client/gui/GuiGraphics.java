package net.minecraft.client.gui;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public class GuiGraphics implements net.neoforged.neoforge.client.extensions.IGuiGraphicsExtension {
    public static final float MAX_GUI_Z = 10000.0F;
    public static final float MIN_GUI_Z = -10000.0F;
    private static final int EXTRA_SPACE_AFTER_FIRST_TOOLTIP_LINE = 2;
    private final Minecraft minecraft;
    private final PoseStack pose;
    private final MultiBufferSource.BufferSource bufferSource;
    private final GuiGraphics.ScissorStack scissorStack = new GuiGraphics.ScissorStack();
    private final GuiSpriteManager sprites;
    private boolean managed;

    private GuiGraphics(Minecraft pMinecraft, PoseStack pPose, MultiBufferSource.BufferSource pBufferSource) {
        this.minecraft = pMinecraft;
        this.pose = pPose;
        this.bufferSource = pBufferSource;
        this.sprites = pMinecraft.getGuiSprites();
    }

    public GuiGraphics(Minecraft pMinecraft, MultiBufferSource.BufferSource pBufferSource) {
        this(pMinecraft, new PoseStack(), pBufferSource);
    }

    /**
     * Executes a runnable while managing the render state. The render state is flushed before and after executing the runnable.
     *
     * @param pRunnable the runnable to execute.
     */
    @Deprecated
    public void drawManaged(Runnable pRunnable) {
        this.flush();
        this.managed = true;
        pRunnable.run();
        this.managed = false;
        this.flush();
    }

    @Deprecated
    private void flushIfUnmanaged() {
        if (!this.managed) {
            this.flush();
        }
    }

    @Deprecated
    private void flushIfManaged() {
        if (this.managed) {
            this.flush();
        }
    }

    public int guiWidth() {
        return this.minecraft.getWindow().getGuiScaledWidth();
    }

    public int guiHeight() {
        return this.minecraft.getWindow().getGuiScaledHeight();
    }

    public PoseStack pose() {
        return this.pose;
    }

    public MultiBufferSource.BufferSource bufferSource() {
        return this.bufferSource;
    }

    public void flush() {
        RenderSystem.disableDepthTest();
        this.bufferSource.endBatch();
        RenderSystem.enableDepthTest();
    }

    /**
     * Draws a horizontal line from minX to maxX at the specified y-coordinate with the given color.
     *
     * @param pMinX  the x-coordinate of the start point.
     * @param pMaxX  the x-coordinate of the end point.
     * @param pY     the y-coordinate of the line.
     * @param pColor the color of the line.
     */
    public void hLine(int pMinX, int pMaxX, int pY, int pColor) {
        this.hLine(RenderType.gui(), pMinX, pMaxX, pY, pColor);
    }

    /**
     * Draws a horizontal line from minX to maxX at the specified y-coordinate with the given color using the specified render type.
     *
     * @param pRenderType the render type to use.
     * @param pMinX       the x-coordinate of the start point.
     * @param pMaxX       the x-coordinate of the end point.
     * @param pY          the y-coordinate of the line.
     * @param pColor      the color of the line.
     */
    public void hLine(RenderType pRenderType, int pMinX, int pMaxX, int pY, int pColor) {
        if (pMaxX < pMinX) {
            int i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        this.fill(pRenderType, pMinX, pY, pMaxX + 1, pY + 1, pColor);
    }

    /**
     * Draws a vertical line from minY to maxY at the specified x-coordinate with the given color.
     *
     * @param pX     the x-coordinate of the line.
     * @param pMinY  the y-coordinate of the start point.
     * @param pMaxY  the y-coordinate of the end point.
     * @param pColor the color of the line.
     */
    public void vLine(int pX, int pMinY, int pMaxY, int pColor) {
        this.vLine(RenderType.gui(), pX, pMinY, pMaxY, pColor);
    }

    /**
     * Draws a vertical line from minY to maxY at the specified x-coordinate with the given color using the specified render type.
     *
     * @param pRenderType the render type to use.
     * @param pX          the x-coordinate of the line.
     * @param pMinY       the y-coordinate of the start point.
     * @param pMaxY       the y-coordinate of the end point.
     * @param pColor      the color of the line.
     */
    public void vLine(RenderType pRenderType, int pX, int pMinY, int pMaxY, int pColor) {
        if (pMaxY < pMinY) {
            int i = pMinY;
            pMinY = pMaxY;
            pMaxY = i;
        }

        this.fill(pRenderType, pX, pMinY + 1, pX + 1, pMaxY, pColor);
    }

    /**
     * Enables scissoring with the specified screen coordinates.
     *
     * @param pMinX the minimum x-coordinate of the scissor region.
     * @param pMinY the minimum y-coordinate of the scissor region.
     * @param pMaxX the maximum x-coordinate of the scissor region.
     * @param pMaxY the maximum y-coordinate of the scissor region.
     */
    public void enableScissor(int pMinX, int pMinY, int pMaxX, int pMaxY) {
        this.applyScissor(this.scissorStack.push(new ScreenRectangle(pMinX, pMinY, pMaxX - pMinX, pMaxY - pMinY)));
    }

    public void disableScissor() {
        this.applyScissor(this.scissorStack.pop());
    }

    public boolean containsPointInScissor(int pX, int pY) {
        return this.scissorStack.containsPoint(pX, pY);
    }

    /**
     * Applies scissoring based on the provided screen rectangle.
     *
     * @param pRectangle the screen rectangle to apply scissoring with. Can be null to
     *                   disable scissoring.
     */
    private void applyScissor(@Nullable ScreenRectangle pRectangle) {
        this.flushIfManaged();
        if (pRectangle != null) {
            Window window = Minecraft.getInstance().getWindow();
            int i = window.getHeight();
            double d0 = window.getGuiScale();
            double d1 = (double)pRectangle.left() * d0;
            double d2 = (double)i - (double)pRectangle.bottom() * d0;
            double d3 = (double)pRectangle.width() * d0;
            double d4 = (double)pRectangle.height() * d0;
            RenderSystem.enableScissor((int)d1, (int)d2, Math.max(0, (int)d3), Math.max(0, (int)d4));
        } else {
            RenderSystem.disableScissor();
        }
    }

    /**
     * Sets the current rendering color.
     *
     * @param pRed   the red component of the color.
     * @param pGreen the green component of the color.
     * @param pBlue  the blue component of the color.
     * @param pAlpha the alpha component of the color.
     */
    public void setColor(float pRed, float pGreen, float pBlue, float pAlpha) {
        this.flushIfManaged();
        RenderSystem.setShaderColor(pRed, pGreen, pBlue, pAlpha);
    }

    /**
     * Fills a rectangle with the specified color using the given coordinates as the boundaries.
     *
     * @param pMinX  the minimum x-coordinate of the rectangle.
     * @param pMinY  the minimum y-coordinate of the rectangle.
     * @param pMaxX  the maximum x-coordinate of the rectangle.
     * @param pMaxY  the maximum y-coordinate of the rectangle.
     * @param pColor the color to fill the rectangle with.
     */
    public void fill(int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
        this.fill(pMinX, pMinY, pMaxX, pMaxY, 0, pColor);
    }

    /**
     * Fills a rectangle with the specified color and z-level using the given coordinates as the boundaries.
     *
     * @param pMinX  the minimum x-coordinate of the rectangle.
     * @param pMinY  the minimum y-coordinate of the rectangle.
     * @param pMaxX  the maximum x-coordinate of the rectangle.
     * @param pMaxY  the maximum y-coordinate of the rectangle.
     * @param pZ     the z-level of the rectangle.
     * @param pColor the color to fill the rectangle with.
     */
    public void fill(int pMinX, int pMinY, int pMaxX, int pMaxY, int pZ, int pColor) {
        this.fill(RenderType.gui(), pMinX, pMinY, pMaxX, pMaxY, pZ, pColor);
    }

    /**
     * Fills a rectangle with the specified color using the given render type and coordinates as the boundaries.
     *
     * @param pRenderType the render type to use.
     * @param pMinX       the minimum x-coordinate of the rectangle.
     * @param pMinY       the minimum y-coordinate of the rectangle.
     * @param pMaxX       the maximum x-coordinate of the rectangle.
     * @param pMaxY       the maximum y-coordinate of the rectangle.
     * @param pColor      the color to fill the rectangle with.
     */
    public void fill(RenderType pRenderType, int pMinX, int pMinY, int pMaxX, int pMaxY, int pColor) {
        this.fill(pRenderType, pMinX, pMinY, pMaxX, pMaxY, 0, pColor);
    }

    /**
     * Fills a rectangle with the specified color and z-level using the given render type and coordinates as the boundaries.
     *
     * @param pRenderType the render type to use.
     * @param pMinX       the minimum x-coordinate of the rectangle.
     * @param pMinY       the minimum y-coordinate of the rectangle.
     * @param pMaxX       the maximum x-coordinate of the rectangle.
     * @param pMaxY       the maximum y-coordinate of the rectangle.
     * @param pZ          the z-level of the rectangle.
     * @param pColor      the color to fill the rectangle with.
     */
    public void fill(RenderType pRenderType, int pMinX, int pMinY, int pMaxX, int pMaxY, int pZ, int pColor) {
        Matrix4f matrix4f = this.pose.last().pose();
        if (pMinX < pMaxX) {
            int i = pMinX;
            pMinX = pMaxX;
            pMaxX = i;
        }

        if (pMinY < pMaxY) {
            int j = pMinY;
            pMinY = pMaxY;
            pMaxY = j;
        }

        VertexConsumer vertexconsumer = this.bufferSource.getBuffer(pRenderType);
        vertexconsumer.addVertex(matrix4f, (float)pMinX, (float)pMinY, (float)pZ).setColor(pColor);
        vertexconsumer.addVertex(matrix4f, (float)pMinX, (float)pMaxY, (float)pZ).setColor(pColor);
        vertexconsumer.addVertex(matrix4f, (float)pMaxX, (float)pMaxY, (float)pZ).setColor(pColor);
        vertexconsumer.addVertex(matrix4f, (float)pMaxX, (float)pMinY, (float)pZ).setColor(pColor);
        this.flushIfUnmanaged();
    }

    /**
     * Fills a rectangle with a gradient color from colorFrom to colorTo using the given coordinates as the boundaries.
     *
     * @param pX1        the x-coordinate of the first corner of the rectangle.
     * @param pY1        the y-coordinate of the first corner of the rectangle.
     * @param pX2        the x-coordinate of the second corner of the rectangle.
     * @param pY2        the y-coordinate of the second corner of the rectangle.
     * @param pColorFrom the starting color of the gradient.
     * @param pColorTo   the ending color of the gradient.
     */
    public void fillGradient(int pX1, int pY1, int pX2, int pY2, int pColorFrom, int pColorTo) {
        this.fillGradient(pX1, pY1, pX2, pY2, 0, pColorFrom, pColorTo);
    }

    /**
     * Fills a rectangle with a gradient color from colorFrom to colorTo at the specified z-level using the given coordinates as the boundaries.
     *
     * @param pX1        the x-coordinate of the first corner of the rectangle.
     * @param pY1        the y-coordinate of the first corner of the rectangle.
     * @param pX2        the x-coordinate of the second corner of the rectangle.
     * @param pY2        the y-coordinate of the second corner of the rectangle.
     * @param pZ         the z-level of the rectangle.
     * @param pColorFrom the starting color of the gradient.
     * @param pColorTo   the ending color of the gradient.
     */
    public void fillGradient(int pX1, int pY1, int pX2, int pY2, int pZ, int pColorFrom, int pColorTo) {
        this.fillGradient(RenderType.gui(), pX1, pY1, pX2, pY2, pColorFrom, pColorTo, pZ);
    }

    /**
     * Fills a rectangle with a gradient color from colorFrom to colorTo at the specified z-level using the given render type and coordinates as the boundaries.
     *
     * @param pRenderType the render type to use.
     * @param pX1         the x-coordinate of the first corner of the rectangle.
     * @param pY1         the y-coordinate of the first corner of the rectangle.
     * @param pX2         the x-coordinate of the second corner of the rectangle.
     * @param pY2         the y-coordinate of the second corner of the rectangle.
     * @param pColorFrom  the starting color of the gradient.
     * @param pColorTo    the ending color of the gradient.
     * @param pZ          the z-level of the rectangle.
     */
    public void fillGradient(RenderType pRenderType, int pX1, int pY1, int pX2, int pY2, int pColorFrom, int pColorTo, int pZ) {
        VertexConsumer vertexconsumer = this.bufferSource.getBuffer(pRenderType);
        this.fillGradient(vertexconsumer, pX1, pY1, pX2, pY2, pZ, pColorFrom, pColorTo);
        this.flushIfUnmanaged();
    }

    /**
     * The core `fillGradient` method.
     * <p>
     * Fills a rectangle with a gradient color from colorFrom to colorTo at the specified z-level using the given render type and coordinates as the boundaries.
     *
     * @param pConsumer  the {@linkplain VertexConsumer} object for drawing the
     *                   vertices on screen.
     * @param pX1        the x-coordinate of the first corner of the rectangle.
     * @param pY1        the y-coordinate of the first corner of the rectangle.
     * @param pX2        the x-coordinate of the second corner of the rectangle.
     * @param pY2        the y-coordinate of the second corner of the rectangle.
     * @param pZ         the z-level of the rectangle.
     * @param pColorFrom the starting color of the gradient.
     * @param pColorTo   the ending color of the gradient.
     */
    private void fillGradient(VertexConsumer pConsumer, int pX1, int pY1, int pX2, int pY2, int pZ, int pColorFrom, int pColorTo) {
        Matrix4f matrix4f = this.pose.last().pose();
        pConsumer.addVertex(matrix4f, (float)pX1, (float)pY1, (float)pZ).setColor(pColorFrom);
        pConsumer.addVertex(matrix4f, (float)pX1, (float)pY2, (float)pZ).setColor(pColorTo);
        pConsumer.addVertex(matrix4f, (float)pX2, (float)pY2, (float)pZ).setColor(pColorTo);
        pConsumer.addVertex(matrix4f, (float)pX2, (float)pY1, (float)pZ).setColor(pColorFrom);
    }

    public void fillRenderType(RenderType pRenderType, int pX1, int pY1, int pX2, int pY2, int pZ) {
        Matrix4f matrix4f = this.pose.last().pose();
        VertexConsumer vertexconsumer = this.bufferSource.getBuffer(pRenderType);
        vertexconsumer.addVertex(matrix4f, (float)pX1, (float)pY1, (float)pZ);
        vertexconsumer.addVertex(matrix4f, (float)pX1, (float)pY2, (float)pZ);
        vertexconsumer.addVertex(matrix4f, (float)pX2, (float)pY2, (float)pZ);
        vertexconsumer.addVertex(matrix4f, (float)pX2, (float)pY1, (float)pZ);
        this.flushIfUnmanaged();
    }

    /**
     * Draws a centered string at the specified coordinates using the given font, text, and color.
     *
     * @param pFont  the font to use for rendering.
     * @param pText  the text to draw.
     * @param pX     the x-coordinate of the center of the string.
     * @param pY     the y-coordinate of the string.
     * @param pColor the color of the string.
     */
    public void drawCenteredString(Font pFont, String pText, int pX, int pY, int pColor) {
        this.drawString(pFont, pText, pX - pFont.width(pText) / 2, pY, pColor);
    }

    /**
     * Draws a centered string at the specified coordinates using the given font, text component, and color.
     *
     * @param pFont  the font to use for rendering.
     * @param pText  the text component to draw.
     * @param pX     the x-coordinate of the center of the string.
     * @param pY     the y-coordinate of the string.
     * @param pColor the color of the string.
     */
    public void drawCenteredString(Font pFont, Component pText, int pX, int pY, int pColor) {
        FormattedCharSequence formattedcharsequence = pText.getVisualOrderText();
        this.drawString(pFont, formattedcharsequence, pX - pFont.width(formattedcharsequence) / 2, pY, pColor);
    }

    /**
     * Draws a centered string at the specified coordinates using the given font, formatted character sequence, and color.
     *
     * @param pFont  the font to use for rendering.
     * @param pText  the formatted character sequence to draw.
     * @param pX     the x-coordinate of the center of the string.
     * @param pY     the y-coordinate of the string.
     * @param pColor the color of the string.
     */
    public void drawCenteredString(Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor) {
        this.drawString(pFont, pText, pX - pFont.width(pText) / 2, pY, pColor);
    }

    /**
     * Draws a string at the specified coordinates using the given font, text, and color. Returns the width of the drawn string.
     * <p>
     * @return the width of the drawn string.
     *
     * @param pFont  the font to use for rendering.
     * @param pText  the text to draw.
     * @param pX     the x-coordinate of the string.
     * @param pY     the y-coordinate of the string.
     * @param pColor the color of the string.
     */
    public int drawString(Font pFont, @Nullable String pText, int pX, int pY, int pColor) {
        return this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    /**
     * Draws a string at the specified coordinates using the given font, text, color, and drop shadow. Returns the width of the drawn string.
     * <p>
     * @return the width of the drawn string.
     *
     * @param pFont       the font to use for rendering.
     * @param pText       the text to draw.
     * @param pX          the x-coordinate of the string.
     * @param pY          the y-coordinate of the string.
     * @param pColor      the color of the string.
     * @param pDropShadow whether to apply a drop shadow to the string.
     */
    public int drawString(Font pFont, @Nullable String pText, int pX, int pY, int pColor, boolean pDropShadow) {
        return this.drawString(pFont, pText, (float)pX, (float)pY, pColor, pDropShadow);
    }

    // Forge: Add float variant for x,y coordinates, with a string as input
    public int drawString(Font p_283343_, @Nullable String p_281896_, float p_283569_, float p_283418_, int p_281560_, boolean p_282130_) {
        if (p_281896_ == null) {
            return 0;
        } else {
            int i = p_283343_.drawInBatch(
                p_281896_,
                (float)p_283569_,
                (float)p_283418_,
                p_281560_,
                p_282130_,
                this.pose.last().pose(),
                this.bufferSource,
                Font.DisplayMode.NORMAL,
                0,
                15728880,
                p_283343_.isBidirectional()
            );
            this.flushIfUnmanaged();
            return i;
        }
    }

    /**
     * Draws a formatted character sequence at the specified coordinates using the given font, text, and color. Returns the width of the drawn string.
     * <p>
     * @return the width of the drawn string.
     *
     * @param pFont  the font to use for rendering.
     * @param pText  the formatted character sequence to draw.
     * @param pX     the x-coordinate of the string.
     * @param pY     the y-coordinate of the string.
     * @param pColor the color of the string
     */
    public int drawString(Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor) {
        return this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    /**
     * Draws a formatted character sequence at the specified coordinates using the given font, text, color, and drop shadow. Returns the width of the drawn string.
     * <p>
     * @return returns the width of the drawn string.
     *
     * @param pFont       the font to use for rendering.
     * @param pText       the formatted character sequence to draw.
     * @param pX          the x-coordinate of the string.
     * @param pY          the y-coordinate of the string.
     * @param pColor      the color of the string.
     * @param pDropShadow whether to apply a drop shadow to the string.
     */
    public int drawString(Font pFont, FormattedCharSequence pText, int pX, int pY, int pColor, boolean pDropShadow) {
        return this.drawString(pFont, pText, (float)pX, (float)pY, pColor, pDropShadow);
    }

    // Forge: Add float variant for x,y coordinates, with a formatted char sequence as input
    public int drawString(Font p_282636_, FormattedCharSequence p_281596_, float p_281586_, float p_282816_, int p_281743_, boolean p_282394_) {
        int i = p_282636_.drawInBatch(
            p_281596_,
            (float)p_281586_,
            (float)p_282816_,
            p_281743_,
            p_282394_,
            this.pose.last().pose(),
            this.bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            15728880
        );
        this.flushIfUnmanaged();
        return i;
    }

    /**
     * Draws a component's visual order text at the specified coordinates using the given font, text component, and color.
     * <p>
     * @return the width of the drawn string.
     *
     * @param pFont  the font to use for rendering.
     * @param pText  the text component to draw.
     * @param pX     the x-coordinate of the string.
     * @param pY     the y-coordinate of the string.
     * @param pColor the color of the string.
     */
    public int drawString(Font pFont, Component pText, int pX, int pY, int pColor) {
        return this.drawString(pFont, pText, pX, pY, pColor, true);
    }

    /**
     * Draws a component's visual order text at the specified coordinates using the given font, text component, color, and drop shadow.
     * <p>
     * @return the width of the drawn string.
     *
     * @param pFont       the font to use for rendering.
     * @param pText       the text component to draw.
     * @param pX          the x-coordinate of the string.
     * @param pY          the y-coordinate of the string.
     * @param pColor      the color of the string.
     * @param pDropShadow whether to apply a drop shadow to the string.
     */
    public int drawString(Font pFont, Component pText, int pX, int pY, int pColor, boolean pDropShadow) {
        return this.drawString(pFont, pText.getVisualOrderText(), pX, pY, pColor, pDropShadow);
    }

    /**
     * Draws a formatted text with word wrapping at the specified coordinates using the given font, text, line width, and color.
     *
     * @param pFont      the font to use for rendering.
     * @param pText      the formatted text to draw.
     * @param pX         the x-coordinate of the starting position.
     * @param pY         the y-coordinate of the starting position.
     * @param pLineWidth the maximum width of each line before wrapping.
     * @param pColor     the color of the text.
     */
    public void drawWordWrap(Font pFont, FormattedText pText, int pX, int pY, int pLineWidth, int pColor) {
        for (FormattedCharSequence formattedcharsequence : pFont.split(pText, pLineWidth)) {
            this.drawString(pFont, formattedcharsequence, pX, pY, pColor, false);
            pY += 9;
        }
    }

    public int drawStringWithBackdrop(Font p_348650_, Component p_348614_, int p_348465_, int p_348495_, int p_348581_, int p_348666_) {
        int i = this.minecraft.options.getBackgroundColor(0.0F);
        if (i != 0) {
            int j = 2;
            this.fill(p_348465_ - 2, p_348495_ - 2, p_348465_ + p_348581_ + 2, p_348495_ + 9 + 2, FastColor.ARGB32.multiply(i, p_348666_));
        }

        return this.drawString(p_348650_, p_348614_, p_348465_, p_348495_, p_348666_, true);
    }

    /**
     * Blits a portion of the specified texture atlas sprite onto the screen at the given coordinates.
     *
     * @param pX          the x-coordinate of the blit position.
     * @param pY          the y-coordinate of the blit position.
     * @param pBlitOffset the z-level offset for rendering order.
     * @param pWidth      the width of the blitted portion.
     * @param pHeight     the height of the blitted portion.
     * @param pSprite     the texture atlas sprite to blit.
     */
    public void blit(int pX, int pY, int pBlitOffset, int pWidth, int pHeight, TextureAtlasSprite pSprite) {
        this.blitSprite(pSprite, pX, pY, pBlitOffset, pWidth, pHeight);
    }

    /**
     * Blits a portion of the specified texture atlas sprite onto the screen at the given coordinates with a color tint.
     *
     * @param pX          the x-coordinate of the blit position.
     * @param pY          the y-coordinate of the blit position.
     * @param pBlitOffset the z-level offset for rendering order.
     * @param pWidth      the width of the blitted portion.
     * @param pHeight     the height of the blitted portion.
     * @param pSprite     the texture atlas sprite to blit.
     * @param pRed        the red component of the color tint.
     * @param pGreen      the green component of the color tint.
     * @param pBlue       the blue component of the color tint.
     * @param pAlpha      the alpha component of the color tint.
     */
    public void blit(
        int pX,
        int pY,
        int pBlitOffset,
        int pWidth,
        int pHeight,
        TextureAtlasSprite pSprite,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        this.innerBlit(
            pSprite.atlasLocation(),
            pX,
            pX + pWidth,
            pY,
            pY + pHeight,
            pBlitOffset,
            pSprite.getU0(),
            pSprite.getU1(),
            pSprite.getV0(),
            pSprite.getV1(),
            pRed,
            pGreen,
            pBlue,
            pAlpha
        );
    }

    /**
     * Renders an outline rectangle on the screen with the specified color.
     *
     * @param pX      the x-coordinate of the top-left corner of the rectangle.
     * @param pY      the y-coordinate of the top-left corner of the rectangle.
     * @param pWidth  the width of the blitted portion.
     * @param pHeight the height of the rectangle.
     * @param pColor  the color of the outline.
     */
    public void renderOutline(int pX, int pY, int pWidth, int pHeight, int pColor) {
        this.fill(pX, pY, pX + pWidth, pY + 1, pColor);
        this.fill(pX, pY + pHeight - 1, pX + pWidth, pY + pHeight, pColor);
        this.fill(pX, pY + 1, pX + 1, pY + pHeight - 1, pColor);
        this.fill(pX + pWidth - 1, pY + 1, pX + pWidth, pY + pHeight - 1, pColor);
    }

    public void blitSprite(ResourceLocation pSprite, int pX, int pY, int pWidth, int pHeight) {
        this.blitSprite(pSprite, pX, pY, 0, pWidth, pHeight);
    }

    public void blitSprite(ResourceLocation pSprite, int pX, int pY, int pBlitOffset, int pWidth, int pHeight) {
        TextureAtlasSprite textureatlassprite = this.sprites.getSprite(pSprite);
        GuiSpriteScaling guispritescaling = this.sprites.getSpriteScaling(textureatlassprite);
        if (guispritescaling instanceof GuiSpriteScaling.Stretch) {
            this.blitSprite(textureatlassprite, pX, pY, pBlitOffset, pWidth, pHeight);
        } else if (guispritescaling instanceof GuiSpriteScaling.Tile guispritescaling$tile) {
            this.blitTiledSprite(
                textureatlassprite,
                pX,
                pY,
                pBlitOffset,
                pWidth,
                pHeight,
                0,
                0,
                guispritescaling$tile.width(),
                guispritescaling$tile.height(),
                guispritescaling$tile.width(),
                guispritescaling$tile.height()
            );
        } else if (guispritescaling instanceof GuiSpriteScaling.NineSlice guispritescaling$nineslice) {
            this.blitNineSlicedSprite(textureatlassprite, guispritescaling$nineslice, pX, pY, pBlitOffset, pWidth, pHeight);
        }
    }

    public void blitSprite(
        ResourceLocation pSprite, int pTextureWidth, int pTextureHeight, int pUPosition, int pVPosition, int pX, int pY, int pUWidth, int pVHeight
    ) {
        this.blitSprite(pSprite, pTextureWidth, pTextureHeight, pUPosition, pVPosition, pX, pY, 0, pUWidth, pVHeight);
    }

    public void blitSprite(
        ResourceLocation pSprite,
        int pTextureWidth,
        int pTextureHeight,
        int pUPosition,
        int pVPosition,
        int pX,
        int pY,
        int pBlitOffset,
        int pUWidth,
        int pVHeight
    ) {
        TextureAtlasSprite textureatlassprite = this.sprites.getSprite(pSprite);
        GuiSpriteScaling guispritescaling = this.sprites.getSpriteScaling(textureatlassprite);
        if (guispritescaling instanceof GuiSpriteScaling.Stretch) {
            this.blitSprite(textureatlassprite, pTextureWidth, pTextureHeight, pUPosition, pVPosition, pX, pY, pBlitOffset, pUWidth, pVHeight);
        } else {
            this.blitSprite(textureatlassprite, pX, pY, pBlitOffset, pUWidth, pVHeight);
        }
    }

    private void blitSprite(
        TextureAtlasSprite pSprite,
        int pTextureWidth,
        int pTextureHeight,
        int pUPosition,
        int pVPosition,
        int pX,
        int pY,
        int pBlitOffset,
        int pUWidth,
        int pVHeight
    ) {
        if (pUWidth != 0 && pVHeight != 0) {
            this.innerBlit(
                pSprite.atlasLocation(),
                pX,
                pX + pUWidth,
                pY,
                pY + pVHeight,
                pBlitOffset,
                pSprite.getU((float)pUPosition / (float)pTextureWidth),
                pSprite.getU((float)(pUPosition + pUWidth) / (float)pTextureWidth),
                pSprite.getV((float)pVPosition / (float)pTextureHeight),
                pSprite.getV((float)(pVPosition + pVHeight) / (float)pTextureHeight)
            );
        }
    }

    private void blitSprite(TextureAtlasSprite pSprite, int pX, int pY, int pBlitOffset, int pWidth, int pHeight) {
        if (pWidth != 0 && pHeight != 0) {
            this.innerBlit(
                pSprite.atlasLocation(),
                pX,
                pX + pWidth,
                pY,
                pY + pHeight,
                pBlitOffset,
                pSprite.getU0(),
                pSprite.getU1(),
                pSprite.getV0(),
                pSprite.getV1()
            );
        }
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given coordinates.
     *
     * @param pAtlasLocation the location of the texture atlas.
     * @param pX             the x-coordinate of the blit position.
     * @param pY             the y-coordinate of the blit position.
     * @param pUOffset       the horizontal texture coordinate offset.
     * @param pVOffset       the vertical texture coordinate offset.
     * @param pUWidth        the width of the blitted portion in texture coordinates.
     * @param pVHeight       the height of the blitted portion in texture coordinates.
     */
    public void blit(ResourceLocation pAtlasLocation, int pX, int pY, int pUOffset, int pVOffset, int pUWidth, int pVHeight) {
        this.blit(pAtlasLocation, pX, pY, 0, (float)pUOffset, (float)pVOffset, pUWidth, pVHeight, 256, 256);
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given coordinates with a blit offset and texture coordinates.
     *
     * @param pAtlasLocation the location of the texture atlas.
     * @param pX             the x-coordinate of the blit position.
     * @param pY             the y-coordinate of the blit position.
     * @param pBlitOffset    the z-level offset for rendering order.
     * @param pUOffset       the horizontal texture coordinate offset.
     * @param pVOffset       the vertical texture coordinate offset.
     * @param pUWidth        the width of the blitted portion in texture coordinates.
     * @param pVHeight       the height of the blitted portion in texture coordinates.
     * @param pTextureWidth  the width of the texture.
     * @param pTextureHeight the height of the texture.
     */
    public void blit(
        ResourceLocation pAtlasLocation,
        int pX,
        int pY,
        int pBlitOffset,
        float pUOffset,
        float pVOffset,
        int pUWidth,
        int pVHeight,
        int pTextureWidth,
        int pTextureHeight
    ) {
        this.blit(
            pAtlasLocation,
            pX,
            pX + pUWidth,
            pY,
            pY + pVHeight,
            pBlitOffset,
            pUWidth,
            pVHeight,
            pUOffset,
            pVOffset,
            pTextureWidth,
            pTextureHeight
        );
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given position and dimensions with texture coordinates.
     *
     * @param pAtlasLocation the location of the texture atlas.
     * @param pX             the x-coordinate of the top-left corner of the blit
     *                       position.
     * @param pY             the y-coordinate of the top-left corner of the blit
     *                       position.
     * @param pWidth         the width of the blitted portion.
     * @param pHeight        the height of the blitted portion.
     * @param pUOffset       the horizontal texture coordinate offset.
     * @param pVOffset       the vertical texture coordinate offset.
     * @param pUWidth        the width of the blitted portion in texture coordinates.
     * @param pVHeight       the height of the blitted portion in texture coordinates.
     * @param pTextureWidth  the width of the texture.
     * @param pTextureHeight the height of the texture.
     */
    public void blit(
        ResourceLocation pAtlasLocation,
        int pX,
        int pY,
        int pWidth,
        int pHeight,
        float pUOffset,
        float pVOffset,
        int pUWidth,
        int pVHeight,
        int pTextureWidth,
        int pTextureHeight
    ) {
        this.blit(
            pAtlasLocation, pX, pX + pWidth, pY, pY + pHeight, 0, pUWidth, pVHeight, pUOffset, pVOffset, pTextureWidth, pTextureHeight
        );
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given position and dimensions with texture coordinates.
     *
     * @param pAtlasLocation the location of the texture atlas.
     * @param pX             the x-coordinate of the top-left corner of the blit
     *                       position.
     * @param pY             the y-coordinate of the top-left corner of the blit
     *                       position.
     * @param pUOffset       the horizontal texture coordinate offset.
     * @param pVOffset       the vertical texture coordinate offset.
     * @param pWidth         the width of the blitted portion.
     * @param pHeight        the height of the blitted portion.
     * @param pTextureWidth  the width of the texture.
     * @param pTextureHeight the height of the texture.
     */
    public void blit(
        ResourceLocation pAtlasLocation, int pX, int pY, float pUOffset, float pVOffset, int pWidth, int pHeight, int pTextureWidth, int pTextureHeight
    ) {
        this.blit(pAtlasLocation, pX, pY, pWidth, pHeight, pUOffset, pVOffset, pWidth, pHeight, pTextureWidth, pTextureHeight);
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates and texture coordinates.
     *
     * @param pAtlasLocation the location of the texture atlas.
     * @param pX1            the x-coordinate of the first corner of the blit position
     *                       .
     * @param pX2            the x-coordinate of the second corner of the blit
     *                       position.
     * @param pY1            the y-coordinate of the first corner of the blit position
     *                       .
     * @param pY2            the y-coordinate of the second corner of the blit
     *                       position.
     * @param pBlitOffset    the z-level offset for rendering order.
     * @param pUWidth        the width of the blitted portion in texture coordinates.
     * @param pVHeight       the height of the blitted portion in texture coordinates.
     * @param pUOffset       the horizontal texture coordinate offset.
     * @param pVOffset       the vertical texture coordinate offset.
     * @param pTextureWidth  the width of the texture.
     * @param pTextureHeight the height of the texture.
     */
    void blit(
        ResourceLocation pAtlasLocation,
        int pX1,
        int pX2,
        int pY1,
        int pY2,
        int pBlitOffset,
        int pUWidth,
        int pVHeight,
        float pUOffset,
        float pVOffset,
        int pTextureWidth,
        int pTextureHeight
    ) {
        this.innerBlit(
            pAtlasLocation,
            pX1,
            pX2,
            pY1,
            pY2,
            pBlitOffset,
            (pUOffset + 0.0F) / (float)pTextureWidth,
            (pUOffset + (float)pUWidth) / (float)pTextureWidth,
            (pVOffset + 0.0F) / (float)pTextureHeight,
            (pVOffset + (float)pVHeight) / (float)pTextureHeight
        );
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates and texture coordinates without color tinting.
     *
     * @param pAtlasLocation the location of the texture atlas.
     * @param pX1            the x-coordinate of the first corner of the blit position
     *                       .
     * @param pX2            the x-coordinate of the second corner of the blit
     *                       position.
     * @param pY1            the y-coordinate of the first corner of the blit position
     *                       .
     * @param pY2            the y-coordinate of the second corner of the blit
     *                       position.
     * @param pBlitOffset    the z-level offset for rendering order.
     * @param pMinU          the minimum horizontal texture coordinate.
     * @param pMaxU          the maximum horizontal texture coordinate.
     * @param pMinV          the minimum vertical texture coordinate.
     * @param pMaxV          the maximum vertical texture coordinate.
     */
    void innerBlit(
        ResourceLocation pAtlasLocation,
        int pX1,
        int pX2,
        int pY1,
        int pY2,
        int pBlitOffset,
        float pMinU,
        float pMaxU,
        float pMinV,
        float pMaxV
    ) {
        RenderSystem.setShaderTexture(0, pAtlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = this.pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix4f, (float)pX1, (float)pY1, (float)pBlitOffset).setUv(pMinU, pMinV);
        bufferbuilder.addVertex(matrix4f, (float)pX1, (float)pY2, (float)pBlitOffset).setUv(pMinU, pMaxV);
        bufferbuilder.addVertex(matrix4f, (float)pX2, (float)pY2, (float)pBlitOffset).setUv(pMaxU, pMaxV);
        bufferbuilder.addVertex(matrix4f, (float)pX2, (float)pY1, (float)pBlitOffset).setUv(pMaxU, pMinV);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates, texture coordinates, and color tint.
     *
     * @param pAtlasLocation the location of the texture atlas.
     * @param pX1            the x-coordinate of the first corner of the blit position
     *                       .
     * @param pX2            the x-coordinate of the second corner of the blit
     *                       position.
     * @param pY1            the y-coordinate of the first corner of the blit position
     *                       .
     * @param pY2            the y-coordinate of the second corner of the blit
     *                       position.
     * @param pBlitOffset    the z-level offset for rendering order.
     * @param pMinU          the minimum horizontal texture coordinate.
     * @param pMaxU          the maximum horizontal texture coordinate.
     * @param pMinV          the minimum vertical texture coordinate.
     * @param pMaxV          the maximum vertical texture coordinate.
     * @param pRed           the red component of the color tint.
     * @param pGreen         the green component of the color tint.
     * @param pBlue          the blue component of the color tint.
     * @param pAlpha         the alpha component of the color tint.
     */
    void innerBlit(
        ResourceLocation pAtlasLocation,
        int pX1,
        int pX2,
        int pY1,
        int pY2,
        int pBlitOffset,
        float pMinU,
        float pMaxU,
        float pMinV,
        float pMaxV,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha
    ) {
        RenderSystem.setShaderTexture(0, pAtlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        Matrix4f matrix4f = this.pose.last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bufferbuilder.addVertex(matrix4f, (float)pX1, (float)pY1, (float)pBlitOffset)
            .setUv(pMinU, pMinV)
            .setColor(pRed, pGreen, pBlue, pAlpha);
        bufferbuilder.addVertex(matrix4f, (float)pX1, (float)pY2, (float)pBlitOffset)
            .setUv(pMinU, pMaxV)
            .setColor(pRed, pGreen, pBlue, pAlpha);
        bufferbuilder.addVertex(matrix4f, (float)pX2, (float)pY2, (float)pBlitOffset)
            .setUv(pMaxU, pMaxV)
            .setColor(pRed, pGreen, pBlue, pAlpha);
        bufferbuilder.addVertex(matrix4f, (float)pX2, (float)pY1, (float)pBlitOffset)
            .setUv(pMaxU, pMinV)
            .setColor(pRed, pGreen, pBlue, pAlpha);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private void blitNineSlicedSprite(
        TextureAtlasSprite pSprite, GuiSpriteScaling.NineSlice pNineSlice, int pX, int pY, int pBlitOffset, int pWidth, int pHeight
    ) {
        GuiSpriteScaling.NineSlice.Border guispritescaling$nineslice$border = pNineSlice.border();
        int i = Math.min(guispritescaling$nineslice$border.left(), pWidth / 2);
        int j = Math.min(guispritescaling$nineslice$border.right(), pWidth / 2);
        int k = Math.min(guispritescaling$nineslice$border.top(), pHeight / 2);
        int l = Math.min(guispritescaling$nineslice$border.bottom(), pHeight / 2);
        if (pWidth == pNineSlice.width() && pHeight == pNineSlice.height()) {
            this.blitSprite(pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, pBlitOffset, pWidth, pHeight);
        } else if (pHeight == pNineSlice.height()) {
            this.blitSprite(pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, pBlitOffset, i, pHeight);
            this.blitTiledSprite(
                pSprite,
                pX + i,
                pY,
                pBlitOffset,
                pWidth - j - i,
                pHeight,
                i,
                0,
                pNineSlice.width() - j - i,
                pNineSlice.height(),
                pNineSlice.width(),
                pNineSlice.height()
            );
            this.blitSprite(
                pSprite, pNineSlice.width(), pNineSlice.height(), pNineSlice.width() - j, 0, pX + pWidth - j, pY, pBlitOffset, j, pHeight
            );
        } else if (pWidth == pNineSlice.width()) {
            this.blitSprite(pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, pBlitOffset, pWidth, k);
            this.blitTiledSprite(
                pSprite,
                pX,
                pY + k,
                pBlitOffset,
                pWidth,
                pHeight - l - k,
                0,
                k,
                pNineSlice.width(),
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height()
            );
            this.blitSprite(
                pSprite, pNineSlice.width(), pNineSlice.height(), 0, pNineSlice.height() - l, pX, pY + pHeight - l, pBlitOffset, pWidth, l
            );
        } else {
            this.blitSprite(pSprite, pNineSlice.width(), pNineSlice.height(), 0, 0, pX, pY, pBlitOffset, i, k);
            this.blitTiledSprite(
                pSprite, pX + i, pY, pBlitOffset, pWidth - j - i, k, i, 0, pNineSlice.width() - j - i, k, pNineSlice.width(), pNineSlice.height()
            );
            this.blitSprite(pSprite, pNineSlice.width(), pNineSlice.height(), pNineSlice.width() - j, 0, pX + pWidth - j, pY, pBlitOffset, j, k);
            this.blitSprite(pSprite, pNineSlice.width(), pNineSlice.height(), 0, pNineSlice.height() - l, pX, pY + pHeight - l, pBlitOffset, i, l);
            this.blitTiledSprite(
                pSprite,
                pX + i,
                pY + pHeight - l,
                pBlitOffset,
                pWidth - j - i,
                l,
                i,
                pNineSlice.height() - l,
                pNineSlice.width() - j - i,
                l,
                pNineSlice.width(),
                pNineSlice.height()
            );
            this.blitSprite(
                pSprite,
                pNineSlice.width(),
                pNineSlice.height(),
                pNineSlice.width() - j,
                pNineSlice.height() - l,
                pX + pWidth - j,
                pY + pHeight - l,
                pBlitOffset,
                j,
                l
            );
            this.blitTiledSprite(
                pSprite,
                pX,
                pY + k,
                pBlitOffset,
                i,
                pHeight - l - k,
                0,
                k,
                i,
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height()
            );
            this.blitTiledSprite(
                pSprite,
                pX + i,
                pY + k,
                pBlitOffset,
                pWidth - j - i,
                pHeight - l - k,
                i,
                k,
                pNineSlice.width() - j - i,
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height()
            );
            this.blitTiledSprite(
                pSprite,
                pX + pWidth - j,
                pY + k,
                pBlitOffset,
                i,
                pHeight - l - k,
                pNineSlice.width() - j,
                k,
                j,
                pNineSlice.height() - l - k,
                pNineSlice.width(),
                pNineSlice.height()
            );
        }
    }

    private void blitTiledSprite(
        TextureAtlasSprite pSprite,
        int pX,
        int pY,
        int pBlitOffset,
        int pWidth,
        int pHeight,
        int pUPosition,
        int pVPosition,
        int pSpriteWidth,
        int pSpriteHeight,
        int pNineSliceWidth,
        int pNineSliceHeight
    ) {
        if (pWidth > 0 && pHeight > 0) {
            if (pSpriteWidth > 0 && pSpriteHeight > 0) {
                for (int i = 0; i < pWidth; i += pSpriteWidth) {
                    int j = Math.min(pSpriteWidth, pWidth - i);

                    for (int k = 0; k < pHeight; k += pSpriteHeight) {
                        int l = Math.min(pSpriteHeight, pHeight - k);
                        this.blitSprite(pSprite, pNineSliceWidth, pNineSliceHeight, pUPosition, pVPosition, pX + i, pY + k, pBlitOffset, j, l);
                    }
                }
            } else {
                throw new IllegalArgumentException("Tiled sprite texture size must be positive, got " + pSpriteWidth + "x" + pSpriteHeight);
            }
        }
    }

    /**
     * Renders an item stack at the specified coordinates.
     *
     * @param pStack the item stack to render.
     * @param pX     the x-coordinate of the rendering position.
     * @param pY     the y-coordinate of the rendering position.
     */
    public void renderItem(ItemStack pStack, int pX, int pY) {
        this.renderItem(this.minecraft.player, this.minecraft.level, pStack, pX, pY, 0);
    }

    /**
     * Renders an item stack at the specified coordinates with a random seed.
     *
     * @param pStack the item stack to render.
     * @param pX     the x-coordinate of the rendering position.
     * @param pY     the y-coordinate of the rendering position.
     * @param pSeed  the random seed.
     */
    public void renderItem(ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(this.minecraft.player, this.minecraft.level, pStack, pX, pY, pSeed);
    }

    /**
     * Renders an item stack at the specified coordinates with a random seed and a custom value.
     *
     * @param pStack     the item stack to render.
     * @param pX         the x-coordinate of the rendering position.
     * @param pY         the y-coordinate of the rendering position.
     * @param pSeed      the random seed.
     * @param pGuiOffset the GUI offset.
     */
    public void renderItem(ItemStack pStack, int pX, int pY, int pSeed, int pGuiOffset) {
        this.renderItem(this.minecraft.player, this.minecraft.level, pStack, pX, pY, pSeed, pGuiOffset);
    }

    /**
     * Renders a fake item stack at the specified coordinates.
     *
     * @param pStack the fake item stack to render.
     * @param pX     the x-coordinate of the rendering position.
     * @param pY     the y-coordinate of the rendering position.
     */
    public void renderFakeItem(ItemStack pStack, int pX, int pY) {
        this.renderFakeItem(pStack, pX, pY, 0);
    }

    public void renderFakeItem(ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(null, this.minecraft.level, pStack, pX, pY, pSeed);
    }

    /**
     * Renders an item stack for a living entity at the specified coordinates with a random seed.
     *
     * @param pEntity the living entity.
     * @param pStack  the item stack to render.
     * @param pX      the x-coordinate of the rendering position.
     * @param pY      the y-coordinate of the rendering position.
     * @param pSeed   the random seed.
     */
    public void renderItem(LivingEntity pEntity, ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(pEntity, pEntity.level(), pStack, pX, pY, pSeed);
    }

    /**
     * Renders an item stack for a living entity in a specific level at the specified coordinates with a random seed.
     *
     * @param pEntity the living entity. Can be null.
     * @param pLevel  the level in which the rendering occurs. Can be null.
     * @param pStack  the item stack to render.
     * @param pX      the x-coordinate of the rendering position.
     * @param pY      the y-coordinate of the rendering position.
     * @param pSeed   the random seed.
     */
    private void renderItem(@Nullable LivingEntity pEntity, @Nullable Level pLevel, ItemStack pStack, int pX, int pY, int pSeed) {
        this.renderItem(pEntity, pLevel, pStack, pX, pY, pSeed, 0);
    }

    /**
     * Renders an item stack for a living entity in a specific level at the specified coordinates with a random seed and a custom GUI offset.
     *
     * @param pEntity    the living entity. Can be null.
     * @param pLevel     the level in which the rendering occurs. Can be null.
     * @param pStack     the item stack to render.
     * @param pX         the x-coordinate of the rendering position.
     * @param pY         the y-coordinate of the rendering position.
     * @param pSeed      the random seed.
     * @param pGuiOffset the GUI offset value.
     */
    private void renderItem(
        @Nullable LivingEntity pEntity, @Nullable Level pLevel, ItemStack pStack, int pX, int pY, int pSeed, int pGuiOffset
    ) {
        if (!pStack.isEmpty()) {
            BakedModel bakedmodel = this.minecraft.getItemRenderer().getModel(pStack, pLevel, pEntity, pSeed);
            this.pose.pushPose();
            this.pose.translate((float)(pX + 8), (float)(pY + 8), (float)(150 + (bakedmodel.isGui3d() ? pGuiOffset : 0)));

            try {
                this.pose.scale(16.0F, -16.0F, 16.0F);
                boolean flag = !bakedmodel.usesBlockLight();
                if (flag) {
                    Lighting.setupForFlatItems();
                }

                this.minecraft
                    .getItemRenderer()
                    .render(pStack, ItemDisplayContext.GUI, false, this.pose, this.bufferSource(), 15728880, OverlayTexture.NO_OVERLAY, bakedmodel);
                this.flush();
                if (flag) {
                    Lighting.setupFor3DItems();
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Rendering item");
                CrashReportCategory crashreportcategory = crashreport.addCategory("Item being rendered");
                crashreportcategory.setDetail("Item Type", () -> String.valueOf(pStack.getItem()));
                crashreportcategory.setDetail("Item Components", () -> String.valueOf(pStack.getComponents()));
                crashreportcategory.setDetail("Item Foil", () -> String.valueOf(pStack.hasFoil()));
                throw new ReportedException(crashreport);
            }

            this.pose.popPose();
        }
    }

    /**
     * Renders additional decorations for an item stack at the specified coordinates.
     *
     * @param pFont  the font used for rendering text.
     * @param pStack the item stack to decorate.
     * @param pX     the x-coordinate of the rendering position.
     * @param pY     the y-coordinate of the rendering position.
     */
    public void renderItemDecorations(Font pFont, ItemStack pStack, int pX, int pY) {
        this.renderItemDecorations(pFont, pStack, pX, pY, null);
    }

    /**
     * Renders additional decorations for an item stack at the specified coordinates with optional custom text.
     *
     * @param pFont  the font used for rendering text.
     * @param pStack the item stack to decorate.
     * @param pX     the x-coordinate of the rendering position.
     * @param pY     the y-coordinate of the rendering position.
     * @param pText  the custom text to display. Can be null.
     */
    public void renderItemDecorations(Font pFont, ItemStack pStack, int pX, int pY, @Nullable String pText) {
        if (!pStack.isEmpty()) {
            this.pose.pushPose();
            if (pStack.getCount() != 1 || pText != null) {
                String s = pText == null ? String.valueOf(pStack.getCount()) : pText;
                this.pose.translate(0.0F, 0.0F, 200.0F);
                this.drawString(pFont, s, pX + 19 - 2 - pFont.width(s), pY + 6 + 3, 16777215, true);
            }

            if (pStack.isBarVisible()) {
                int l = pStack.getBarWidth();
                int i = pStack.getBarColor();
                int j = pX + 2;
                int k = pY + 13;
                this.fill(RenderType.guiOverlay(), j, k, j + 13, k + 2, -16777216);
                this.fill(RenderType.guiOverlay(), j, k, j + l, k + 1, i | 0xFF000000);
            }

            LocalPlayer localplayer = this.minecraft.player;
            float f = localplayer == null
                ? 0.0F
                : localplayer.getCooldowns().getCooldownPercent(pStack.getItem(), this.minecraft.getTimer().getGameTimeDeltaPartialTick(true));
            if (f > 0.0F) {
                int i1 = pY + Mth.floor(16.0F * (1.0F - f));
                int j1 = i1 + Mth.ceil(16.0F * f);
                this.fill(RenderType.guiOverlay(), pX, i1, pX + 16, j1, Integer.MAX_VALUE);
            }

            this.pose.popPose();
            net.neoforged.neoforge.client.ItemDecoratorHandler.of(pStack).render(this, pFont, pStack, pX, pY);
        }
    }

    private ItemStack tooltipStack = ItemStack.EMPTY;

    /**
     * Renders a tooltip for an item stack at the specified mouse coordinates.
     *
     * @param pFont   the font used for rendering text.
     * @param pStack  the item stack to display the tooltip for.
     * @param pMouseX the x-coordinate of the mouse position.
     * @param pMouseY the y-coordinate of the mouse position.
     */
    public void renderTooltip(Font pFont, ItemStack pStack, int pMouseX, int pMouseY) {
        this.tooltipStack = pStack;
        this.renderTooltip(pFont, Screen.getTooltipFromItem(this.minecraft, pStack), pStack.getTooltipImage(), pMouseX, pMouseY);
        this.tooltipStack = ItemStack.EMPTY;
    }

    public void renderTooltip(Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY) {
        this.tooltipStack = stack;
        this.renderTooltip(font, textComponents, tooltipComponent, mouseX, mouseY);
        this.tooltipStack = ItemStack.EMPTY;
    }

    /**
     * Renders a tooltip with customizable components at the specified mouse coordinates.
     *
     * @param pFont                   the font used for rendering text.
     * @param pTooltipLines           the lines of the tooltip.
     * @param pVisualTooltipComponent the visual tooltip component. Can be empty.
     * @param pMouseX                 the x-coordinate of the mouse position.
     * @param pMouseY                 the y-coordinate of the mouse position.
     */
    public void renderTooltip(Font pFont, List<Component> pTooltipLines, Optional<TooltipComponent> pVisualTooltipComponent, int pMouseX, int pMouseY) {
        List<ClientTooltipComponent> list = net.neoforged.neoforge.client.ClientHooks.gatherTooltipComponents(this.tooltipStack, pTooltipLines, pVisualTooltipComponent, pMouseX, guiWidth(), guiHeight(), pFont);
        this.renderTooltipInternal(pFont, list, pMouseX, pMouseY, DefaultTooltipPositioner.INSTANCE);
    }

    /**
     * Renders a tooltip with a single line of text at the specified mouse coordinates.
     *
     * @param pFont   the font used for rendering text.
     * @param pText   the text to display in the tooltip.
     * @param pMouseX the x-coordinate of the mouse position.
     * @param pMouseY the y-coordinate of the mouse position.
     */
    public void renderTooltip(Font pFont, Component pText, int pMouseX, int pMouseY) {
        this.renderTooltip(pFont, List.of(pText.getVisualOrderText()), pMouseX, pMouseY);
    }

    /**
     * Renders a tooltip with multiple lines of component-based text at the specified mouse coordinates.
     *
     * @param pFont         the font used for rendering text.
     * @param pTooltipLines the lines of the tooltip as components.
     * @param pMouseX       the x-coordinate of the mouse position.
     * @param pMouseY       the y-coordinate of the mouse position.
     */
    public void renderComponentTooltip(Font pFont, List<Component> pTooltipLines, int pMouseX, int pMouseY) {
        List<ClientTooltipComponent> components = net.neoforged.neoforge.client.ClientHooks.gatherTooltipComponents(this.tooltipStack, pTooltipLines, pMouseX, guiWidth(), guiHeight(), pFont);
        this.renderTooltipInternal(pFont, components, pMouseX, pMouseY, DefaultTooltipPositioner.INSTANCE);
    }

    /**
     * Renders a tooltip with multiple lines of component-based text at the specified mouse coordinates.
     *
     * @param pFont         the font used for rendering text.
     * @param pTooltipLines the lines of the tooltip as components.
     * @param pMouseX       the x-coordinate of the mouse position.
     * @param pMouseY       the y-coordinate of the mouse position.
     */
    public void renderComponentTooltip(Font pFont, List<? extends net.minecraft.network.chat.FormattedText> pTooltipLines, int pMouseX, int pMouseY, ItemStack stack) {
        this.tooltipStack = stack;
        List<ClientTooltipComponent> components = net.neoforged.neoforge.client.ClientHooks.gatherTooltipComponents(stack, pTooltipLines, pMouseX, guiWidth(), guiHeight(), pFont);
        this.renderTooltipInternal(pFont, components, pMouseX, pMouseY, DefaultTooltipPositioner.INSTANCE);
        this.tooltipStack = ItemStack.EMPTY;
    }

    /**
     * Renders a tooltip with multiple lines of formatted text at the specified mouse coordinates.
     *
     * @param pFont         the font used for rendering text.
     * @param pTooltipLines the lines of the tooltip as formatted character sequences.
     * @param pMouseX       the x-coordinate of the mouse position.
     * @param pMouseY       the y-coordinate of the mouse position.
     */
    public void renderTooltip(Font pFont, List<? extends FormattedCharSequence> pTooltipLines, int pMouseX, int pMouseY) {
        this.renderTooltipInternal(
            pFont,
            pTooltipLines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()),
            pMouseX,
            pMouseY,
            DefaultTooltipPositioner.INSTANCE
        );
    }

    /**
     * Renders a tooltip with multiple lines of formatted text using a custom tooltip positioner at the specified mouse coordinates.
     *
     * @param pFont              the font used for rendering text.
     * @param pTooltipLines      the lines of the tooltip as formatted character
     *                           sequences.
     * @param pTooltipPositioner the positioner to determine the tooltip's position.
     * @param pMouseX            the x-coordinate of the mouse position.
     * @param pMouseY            the y-coordinate of the mouse position.
     */
    public void renderTooltip(Font pFont, List<FormattedCharSequence> pTooltipLines, ClientTooltipPositioner pTooltipPositioner, int pMouseX, int pMouseY) {
        this.renderTooltipInternal(
            pFont, pTooltipLines.stream().map(ClientTooltipComponent::create).collect(Collectors.toList()), pMouseX, pMouseY, pTooltipPositioner
        );
    }

    /**
     * Renders an internal tooltip with customizable tooltip components at the specified mouse coordinates using a tooltip positioner.
     *
     * @param pFont              the font used for rendering text.
     * @param pComponents        the tooltip components to render.
     * @param pMouseX            the x-coordinate of the mouse position.
     * @param pMouseY            the y-coordinate of the mouse position.
     * @param pTooltipPositioner the positioner to determine the tooltip's position.
     */
    private void renderTooltipInternal(Font pFont, List<ClientTooltipComponent> pComponents, int pMouseX, int pMouseY, ClientTooltipPositioner pTooltipPositioner) {
        if (!pComponents.isEmpty()) {
            net.neoforged.neoforge.client.event.RenderTooltipEvent.Pre preEvent = net.neoforged.neoforge.client.ClientHooks.onRenderTooltipPre(this.tooltipStack, this, pMouseX, pMouseY, guiWidth(), guiHeight(), pComponents, pFont, pTooltipPositioner);
            if (preEvent.isCanceled()) return;
            int i = 0;
            int j = pComponents.size() == 1 ? -2 : 0;

            for (ClientTooltipComponent clienttooltipcomponent : pComponents) {
                int k = clienttooltipcomponent.getWidth(preEvent.getFont());
                if (k > i) {
                    i = k;
                }

                j += clienttooltipcomponent.getHeight();
            }

            int i2 = i;
            int j2 = j;
            Vector2ic vector2ic = pTooltipPositioner.positionTooltip(this.guiWidth(), this.guiHeight(), preEvent.getX(), preEvent.getY(), i2, j2);
            int l = vector2ic.x();
            int i1 = vector2ic.y();
            this.pose.pushPose();
            int j1 = 400;
            net.neoforged.neoforge.client.event.RenderTooltipEvent.Color colorEvent = net.neoforged.neoforge.client.ClientHooks.onRenderTooltipColor(this.tooltipStack, this, l, i1, preEvent.getFont(), pComponents);
            this.drawManaged(() -> TooltipRenderUtil.renderTooltipBackground(this, l, i1, i2, j2, 400, colorEvent.getBackgroundStart(), colorEvent.getBackgroundEnd(), colorEvent.getBorderStart(), colorEvent.getBorderEnd()));
            this.pose.translate(0.0F, 0.0F, 400.0F);
            int k1 = i1;

            for (int l1 = 0; l1 < pComponents.size(); l1++) {
                ClientTooltipComponent clienttooltipcomponent1 = pComponents.get(l1);
                clienttooltipcomponent1.renderText(preEvent.getFont(), l, k1, this.pose.last().pose(), this.bufferSource);
                k1 += clienttooltipcomponent1.getHeight() + (l1 == 0 ? 2 : 0);
            }

            k1 = i1;

            for (int k2 = 0; k2 < pComponents.size(); k2++) {
                ClientTooltipComponent clienttooltipcomponent2 = pComponents.get(k2);
                clienttooltipcomponent2.renderImage(preEvent.getFont(), l, k1, this);
                k1 += clienttooltipcomponent2.getHeight() + (k2 == 0 ? 2 : 0);
            }

            this.pose.popPose();
        }
    }

    /**
     * Renders a hover effect for a text component at the specified mouse coordinates.
     *
     * @param pFont   the font used for rendering text.
     * @param pStyle  the style of the text component. Can be null.
     * @param pMouseX the x-coordinate of the mouse position.
     * @param pMouseY the y-coordinate of the mouse position.
     */
    public void renderComponentHoverEffect(Font pFont, @Nullable Style pStyle, int pMouseX, int pMouseY) {
        if (pStyle != null && pStyle.getHoverEvent() != null) {
            HoverEvent hoverevent = pStyle.getHoverEvent();
            HoverEvent.ItemStackInfo hoverevent$itemstackinfo = hoverevent.getValue(HoverEvent.Action.SHOW_ITEM);
            if (hoverevent$itemstackinfo != null) {
                this.renderTooltip(pFont, hoverevent$itemstackinfo.getItemStack(), pMouseX, pMouseY);
            } else {
                HoverEvent.EntityTooltipInfo hoverevent$entitytooltipinfo = hoverevent.getValue(HoverEvent.Action.SHOW_ENTITY);
                if (hoverevent$entitytooltipinfo != null) {
                    if (this.minecraft.options.advancedItemTooltips) {
                        this.renderComponentTooltip(pFont, hoverevent$entitytooltipinfo.getTooltipLines(), pMouseX, pMouseY);
                    }
                } else {
                    Component component = hoverevent.getValue(HoverEvent.Action.SHOW_TEXT);
                    if (component != null) {
                        this.renderTooltip(pFont, pFont.split(component, Math.max(this.guiWidth() / 2, 200)), pMouseX, pMouseY);
                    }
                }
            }
        }
    }

    /**
     * A utility class for managing a stack of screen rectangles for scissoring.
     */
    @OnlyIn(Dist.CLIENT)
    static class ScissorStack {
        private final Deque<ScreenRectangle> stack = new ArrayDeque<>();

        /**
         * Pushes a screen rectangle onto the scissor stack.
         * <p>
         * @return The resulting intersection of the pushed rectangle with the previous top rectangle on the stack, or the pushed rectangle if the stack is empty.
         *
         * @param pScissor the screen rectangle to push.
         */
        public ScreenRectangle push(ScreenRectangle pScissor) {
            ScreenRectangle screenrectangle = this.stack.peekLast();
            if (screenrectangle != null) {
                ScreenRectangle screenrectangle1 = Objects.requireNonNullElse(pScissor.intersection(screenrectangle), ScreenRectangle.empty());
                this.stack.addLast(screenrectangle1);
                return screenrectangle1;
            } else {
                this.stack.addLast(pScissor);
                return pScissor;
            }
        }

        @Nullable
        public ScreenRectangle pop() {
            if (this.stack.isEmpty()) {
                throw new IllegalStateException("Scissor stack underflow");
            } else {
                this.stack.removeLast();
                return this.stack.peekLast();
            }
        }

        public boolean containsPoint(int pX, int pY) {
            return this.stack.isEmpty() ? true : this.stack.peek().containsPoint(pX, pY);
        }
    }
}

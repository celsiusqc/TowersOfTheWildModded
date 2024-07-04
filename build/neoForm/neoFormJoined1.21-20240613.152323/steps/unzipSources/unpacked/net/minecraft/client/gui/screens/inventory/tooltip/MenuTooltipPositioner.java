package net.minecraft.client.gui.screens.inventory.tooltip;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2i;
import org.joml.Vector2ic;

@OnlyIn(Dist.CLIENT)
public class MenuTooltipPositioner implements ClientTooltipPositioner {
    private static final int MARGIN = 5;
    private static final int MOUSE_OFFSET_X = 12;
    public static final int MAX_OVERLAP_WITH_WIDGET = 3;
    public static final int MAX_DISTANCE_TO_WIDGET = 5;
    private final ScreenRectangle screenRectangle;

    public MenuTooltipPositioner(ScreenRectangle pScreenRectangle) {
        this.screenRectangle = pScreenRectangle;
    }

    @Override
    public Vector2ic positionTooltip(int pScreenWidth, int pScreenHeight, int pMouseX, int pMouseY, int pTooltipWidth, int pTooltipHeight) {
        Vector2i vector2i = new Vector2i(pMouseX + 12, pMouseY);
        if (vector2i.x + pTooltipWidth > pScreenWidth - 5) {
            vector2i.x = Math.max(pMouseX - 12 - pTooltipWidth, 9);
        }

        vector2i.y += 3;
        int i = pTooltipHeight + 3 + 3;
        int j = this.screenRectangle.bottom() + 3 + getOffset(0, 0, this.screenRectangle.height());
        int k = pScreenHeight - 5;
        if (j + i <= k) {
            vector2i.y = vector2i.y + getOffset(vector2i.y, this.screenRectangle.top(), this.screenRectangle.height());
        } else {
            vector2i.y = vector2i.y - (i + getOffset(vector2i.y, this.screenRectangle.bottom(), this.screenRectangle.height()));
        }

        return vector2i;
    }

    private static int getOffset(int pMouseY, int pWidgetY, int pWidgetHeight) {
        int i = Math.min(Math.abs(pMouseY - pWidgetY), pWidgetHeight);
        return Math.round(Mth.lerp((float)i / (float)pWidgetHeight, (float)(pWidgetHeight - 3), 5.0F));
    }
}

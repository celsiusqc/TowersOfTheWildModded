package net.minecraft.client.gui.navigation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record ScreenPosition(int x, int y) {
    public static ScreenPosition of(ScreenAxis pAxis, int pPrimaryPosition, int pSecondaryPosition) {
        return switch (pAxis) {
            case HORIZONTAL -> new ScreenPosition(pPrimaryPosition, pSecondaryPosition);
            case VERTICAL -> new ScreenPosition(pSecondaryPosition, pPrimaryPosition);
        };
    }

    public ScreenPosition step(ScreenDirection pDirection) {
        return switch (pDirection) {
            case DOWN -> new ScreenPosition(this.x, this.y + 1);
            case UP -> new ScreenPosition(this.x, this.y - 1);
            case LEFT -> new ScreenPosition(this.x - 1, this.y);
            case RIGHT -> new ScreenPosition(this.x + 1, this.y);
        };
    }

    public int getCoordinate(ScreenAxis pAxis) {
        return switch (pAxis) {
            case HORIZONTAL -> this.x;
            case VERTICAL -> this.y;
        };
    }
}

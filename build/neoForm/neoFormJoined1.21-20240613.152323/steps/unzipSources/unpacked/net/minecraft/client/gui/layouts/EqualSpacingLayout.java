package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EqualSpacingLayout extends AbstractLayout {
    private final EqualSpacingLayout.Orientation orientation;
    private final List<EqualSpacingLayout.ChildContainer> children = new ArrayList<>();
    private final LayoutSettings defaultChildLayoutSettings = LayoutSettings.defaults();

    public EqualSpacingLayout(int pWidth, int pHeight, EqualSpacingLayout.Orientation pOrientation) {
        this(0, 0, pWidth, pHeight, pOrientation);
    }

    public EqualSpacingLayout(int pX, int pY, int pWidth, int pHeight, EqualSpacingLayout.Orientation pOrientation) {
        super(pX, pY, pWidth, pHeight);
        this.orientation = pOrientation;
    }

    @Override
    public void arrangeElements() {
        super.arrangeElements();
        if (!this.children.isEmpty()) {
            int i = 0;
            int j = this.orientation.getSecondaryLength(this);

            for (EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer : this.children) {
                i += this.orientation.getPrimaryLength(equalspacinglayout$childcontainer);
                j = Math.max(j, this.orientation.getSecondaryLength(equalspacinglayout$childcontainer));
            }

            int k = this.orientation.getPrimaryLength(this) - i;
            int l = this.orientation.getPrimaryPosition(this);
            Iterator<EqualSpacingLayout.ChildContainer> iterator = this.children.iterator();
            EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer1 = iterator.next();
            this.orientation.setPrimaryPosition(equalspacinglayout$childcontainer1, l);
            l += this.orientation.getPrimaryLength(equalspacinglayout$childcontainer1);
            if (this.children.size() >= 2) {
                Divisor divisor = new Divisor(k, this.children.size() - 1);

                while (divisor.hasNext()) {
                    l += divisor.nextInt();
                    EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer2 = iterator.next();
                    this.orientation.setPrimaryPosition(equalspacinglayout$childcontainer2, l);
                    l += this.orientation.getPrimaryLength(equalspacinglayout$childcontainer2);
                }
            }

            int i1 = this.orientation.getSecondaryPosition(this);

            for (EqualSpacingLayout.ChildContainer equalspacinglayout$childcontainer3 : this.children) {
                this.orientation.setSecondaryPosition(equalspacinglayout$childcontainer3, i1, j);
            }

            switch (this.orientation) {
                case HORIZONTAL:
                    this.height = j;
                    break;
                case VERTICAL:
                    this.width = j;
            }
        }
    }

    @Override
    public void visitChildren(Consumer<LayoutElement> pVisitor) {
        this.children.forEach(p_296421_ -> pVisitor.accept(p_296421_.child));
    }

    public LayoutSettings newChildLayoutSettings() {
        return this.defaultChildLayoutSettings.copy();
    }

    public LayoutSettings defaultChildLayoutSetting() {
        return this.defaultChildLayoutSettings;
    }

    public <T extends LayoutElement> T addChild(T pChild) {
        return this.addChild(pChild, this.newChildLayoutSettings());
    }

    public <T extends LayoutElement> T addChild(T pChild, LayoutSettings pLayoutSettings) {
        this.children.add(new EqualSpacingLayout.ChildContainer(pChild, pLayoutSettings));
        return pChild;
    }

    public <T extends LayoutElement> T addChild(T pChild, Consumer<LayoutSettings> pLayoutSettingsCreator) {
        return this.addChild(pChild, Util.make(this.newChildLayoutSettings(), pLayoutSettingsCreator));
    }

    @OnlyIn(Dist.CLIENT)
    static class ChildContainer extends AbstractLayout.AbstractChildWrapper {
        protected ChildContainer(LayoutElement p_295358_, LayoutSettings p_295638_) {
            super(p_295358_, p_295638_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static enum Orientation {
        HORIZONTAL,
        VERTICAL;

        int getPrimaryLength(LayoutElement pElement) {
            return switch (this) {
                case HORIZONTAL -> pElement.getWidth();
                case VERTICAL -> pElement.getHeight();
            };
        }

        int getPrimaryLength(EqualSpacingLayout.ChildContainer pContainer) {
            return switch (this) {
                case HORIZONTAL -> pContainer.getWidth();
                case VERTICAL -> pContainer.getHeight();
            };
        }

        int getSecondaryLength(LayoutElement pElement) {
            return switch (this) {
                case HORIZONTAL -> pElement.getHeight();
                case VERTICAL -> pElement.getWidth();
            };
        }

        int getSecondaryLength(EqualSpacingLayout.ChildContainer pContainer) {
            return switch (this) {
                case HORIZONTAL -> pContainer.getHeight();
                case VERTICAL -> pContainer.getWidth();
            };
        }

        void setPrimaryPosition(EqualSpacingLayout.ChildContainer pContainer, int pPosition) {
            switch (this) {
                case HORIZONTAL:
                    pContainer.setX(pPosition, pContainer.getWidth());
                    break;
                case VERTICAL:
                    pContainer.setY(pPosition, pContainer.getHeight());
            }
        }

        void setSecondaryPosition(EqualSpacingLayout.ChildContainer pContainer, int pPosition, int pLength) {
            switch (this) {
                case HORIZONTAL:
                    pContainer.setY(pPosition, pLength);
                    break;
                case VERTICAL:
                    pContainer.setX(pPosition, pLength);
            }
        }

        int getPrimaryPosition(LayoutElement pElement) {
            return switch (this) {
                case HORIZONTAL -> pElement.getX();
                case VERTICAL -> pElement.getY();
            };
        }

        int getSecondaryPosition(LayoutElement pElement) {
            return switch (this) {
                case HORIZONTAL -> pElement.getY();
                case VERTICAL -> pElement.getX();
            };
        }
    }
}

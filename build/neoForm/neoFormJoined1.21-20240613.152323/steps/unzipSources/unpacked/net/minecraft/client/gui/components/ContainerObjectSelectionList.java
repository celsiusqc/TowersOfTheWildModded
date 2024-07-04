package net.minecraft.client.gui.components;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class ContainerObjectSelectionList<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList<E> {
    public ContainerObjectSelectionList(Minecraft pMinecraft, int pWidth, int pHeight, int pY, int pItemHeight) {
        super(pMinecraft, pWidth, pHeight, pY, pItemHeight);
    }

    /**
     * Retrieves the next focus path based on the given focus navigation event.
     * <p>
     * @return the next focus path as a ComponentPath, or {@code null} if there is no next focus path.
     *
     * @param pEvent the focus navigation event.
     */
    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent pEvent) {
        if (this.getItemCount() == 0) {
            return null;
        } else if (!(pEvent instanceof FocusNavigationEvent.ArrowNavigation focusnavigationevent$arrownavigation)) {
            return super.nextFocusPath(pEvent);
        } else {
            E e = this.getFocused();
            if (focusnavigationevent$arrownavigation.direction().getAxis() == ScreenAxis.HORIZONTAL && e != null) {
                return ComponentPath.path(this, e.nextFocusPath(pEvent));
            } else {
                int i = -1;
                ScreenDirection screendirection = focusnavigationevent$arrownavigation.direction();
                if (e != null) {
                    i = e.children().indexOf(e.getFocused());
                }

                if (i == -1) {
                    switch (screendirection) {
                        case LEFT:
                            i = Integer.MAX_VALUE;
                            screendirection = ScreenDirection.DOWN;
                            break;
                        case RIGHT:
                            i = 0;
                            screendirection = ScreenDirection.DOWN;
                            break;
                        default:
                            i = 0;
                    }
                }

                E e1 = e;

                ComponentPath componentpath;
                do {
                    e1 = this.nextEntry(screendirection, p_351636_ -> !p_351636_.children().isEmpty(), e1);
                    if (e1 == null) {
                        return null;
                    }

                    componentpath = e1.focusPathAtIndex(focusnavigationevent$arrownavigation, i);
                } while (componentpath == null);

                return ComponentPath.path(this, componentpath);
            }
        }
    }

    /**
     * Sets the focus state of the GUI element.
     *
     * @param pFocused the focused GUI element.
     */
    @Override
    public void setFocused(@Nullable GuiEventListener pFocused) {
        if (this.getFocused() != pFocused) {
            super.setFocused(pFocused);
            if (pFocused == null) {
                this.setSelected(null);
            }
        }
    }

    @Override
    public NarratableEntry.NarrationPriority narrationPriority() {
        return this.isFocused() ? NarratableEntry.NarrationPriority.FOCUSED : super.narrationPriority();
    }

    @Override
    protected boolean isSelectedItem(int pIndex) {
        return false;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
        E e = this.getHovered();
        if (e != null) {
            e.updateNarration(pNarrationElementOutput.nest());
            this.narrateListElementPosition(pNarrationElementOutput, e);
        } else {
            E e1 = this.getFocused();
            if (e1 != null) {
                e1.updateNarration(pNarrationElementOutput.nest());
                this.narrateListElementPosition(pNarrationElementOutput, e1);
            }
        }

        pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
    }

    @OnlyIn(Dist.CLIENT)
    public abstract static class Entry<E extends ContainerObjectSelectionList.Entry<E>> extends AbstractSelectionList.Entry<E> implements ContainerEventHandler {
        @Nullable
        private GuiEventListener focused;
        @Nullable
        private NarratableEntry lastNarratable;
        private boolean dragging;

        @Override
        public boolean isDragging() {
            return this.dragging;
        }

        /**
         * Sets if the GUI element is dragging or not.
         *
         * @param pDragging the dragging state of the GUI element.
         */
        @Override
        public void setDragging(boolean pDragging) {
            this.dragging = pDragging;
        }

        /**
         * Called when a mouse button is clicked within the GUI element.
         * <p>
         * @return {@code true} if the event is consumed, {@code false} otherwise.
         *
         * @param pMouseX the X coordinate of the mouse.
         * @param pMouseY the Y coordinate of the mouse.
         * @param pButton the button that was clicked.
         */
        @Override
        public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
            return ContainerEventHandler.super.mouseClicked(pMouseX, pMouseY, pButton);
        }

        /**
         * Sets the focus state of the GUI element.
         *
         * @param pListener the focused GUI element.
         */
        @Override
        public void setFocused(@Nullable GuiEventListener pListener) {
            if (this.focused != null) {
                this.focused.setFocused(false);
            }

            if (pListener != null) {
                pListener.setFocused(true);
            }

            this.focused = pListener;
        }

        @Nullable
        @Override
        public GuiEventListener getFocused() {
            return this.focused;
        }

        @Nullable
        public ComponentPath focusPathAtIndex(FocusNavigationEvent pEvent, int pIndex) {
            if (this.children().isEmpty()) {
                return null;
            } else {
                ComponentPath componentpath = this.children().get(Math.min(pIndex, this.children().size() - 1)).nextFocusPath(pEvent);
                return ComponentPath.path(this, componentpath);
            }
        }

        /**
         * Retrieves the next focus path based on the given focus navigation event.
         * <p>
         * @return the next focus path as a ComponentPath, or {@code null} if there is no next focus path.
         *
         * @param pEvent the focus navigation event.
         */
        @Nullable
        @Override
        public ComponentPath nextFocusPath(FocusNavigationEvent pEvent) {
            if (pEvent instanceof FocusNavigationEvent.ArrowNavigation focusnavigationevent$arrownavigation) {
                int i = switch (focusnavigationevent$arrownavigation.direction()) {
                    case LEFT -> -1;
                    case RIGHT -> 1;
                    case UP, DOWN -> 0;
                };
                if (i == 0) {
                    return null;
                }

                int j = Mth.clamp(i + this.children().indexOf(this.getFocused()), 0, this.children().size() - 1);

                for (int k = j; k >= 0 && k < this.children().size(); k += i) {
                    GuiEventListener guieventlistener = this.children().get(k);
                    ComponentPath componentpath = guieventlistener.nextFocusPath(pEvent);
                    if (componentpath != null) {
                        return ComponentPath.path(this, componentpath);
                    }
                }
            }

            return ContainerEventHandler.super.nextFocusPath(pEvent);
        }

        public abstract List<? extends NarratableEntry> narratables();

        void updateNarration(NarrationElementOutput pNarrationElementOutput) {
            List<? extends NarratableEntry> list = this.narratables();
            Screen.NarratableSearchResult screen$narratablesearchresult = Screen.findNarratableWidget(list, this.lastNarratable);
            if (screen$narratablesearchresult != null) {
                if (screen$narratablesearchresult.priority.isTerminal()) {
                    this.lastNarratable = screen$narratablesearchresult.entry;
                }

                if (list.size() > 1) {
                    pNarrationElementOutput.add(
                        NarratedElementType.POSITION,
                        Component.translatable("narrator.position.object_list", screen$narratablesearchresult.index + 1, list.size())
                    );
                    if (screen$narratablesearchresult.priority == NarratableEntry.NarrationPriority.FOCUSED) {
                        pNarrationElementOutput.add(NarratedElementType.USAGE, Component.translatable("narration.component_list.usage"));
                    }
                }

                screen$narratablesearchresult.entry.updateNarration(pNarrationElementOutput.nest());
            }
        }
    }
}

package net.minecraft.client.gui;

import javax.annotation.Nullable;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Represents a path of components in a user interface hierarchy.
 * <p>
 * It provides methods to create and manipulate component paths.
 */
@OnlyIn(Dist.CLIENT)
public interface ComponentPath {
    /**
     * Creates a leaf component path with the specified {@code GuiEventListener} component.
     * <p>
     * @return a new leaf component path.
     *
     * @param pComponent the component associated with the leaf path
     */
    static ComponentPath leaf(GuiEventListener pComponent) {
        return new ComponentPath.Leaf(pComponent);
    }

    /**
     * Creates a component path with the specified {@code ContainerEventHandler} component and an optional child path.
     * <p>
     * @return a new component path, or {@code null} if the child path is null
     *
     * @param pComponent the component associated with the path
     * @param pChildPath the child path associated with the component
     */
    @Nullable
    static ComponentPath path(ContainerEventHandler pComponent, @Nullable ComponentPath pChildPath) {
        return pChildPath == null ? null : new ComponentPath.Path(pComponent, pChildPath);
    }

    /**
     * Creates a new {@code ComponentPath} leaf node with the specified {@code GuiEventListener} component and an array of {@code ContainerEventHandler} ancestors.
     * <p>
     * @return a new component path
     *
     * @param pLeafComponent      the new 'Leaf' component associated with the path
     * @param pAncestorComponents the array of ancestor components associated with the
     *                            path, ordered in reverse ascending order towards
     *                            root.
     */
    static ComponentPath path(GuiEventListener pLeafComponent, ContainerEventHandler... pAncestorComponents) {
        ComponentPath componentpath = leaf(pLeafComponent);

        for (ContainerEventHandler containereventhandler : pAncestorComponents) {
            componentpath = path(containereventhandler, componentpath);
        }

        return componentpath;
    }

    GuiEventListener component();

    /**
     * Applies focus to or removes focus from the component associated with this component path.
     *
     * @param pFocused {@code true} to apply focus, {@code false} to remove focus.
     */
    void applyFocus(boolean pFocused);

    /**
     * The {@code Leaf} class represents a leaf component path in the hierarchy.
     */
    @OnlyIn(Dist.CLIENT)
    public static record Leaf(GuiEventListener component) implements ComponentPath {
        /**
         * Applies focus to or removes focus from the component associated with this leaf path.
         * focused {@code true} to apply focus, {@code false} to remove focus
         *
         * @param pFocused {@code true} to apply focus, {@code false} to remove focus.
         */
        @Override
        public void applyFocus(boolean pFocused) {
            this.component.setFocused(pFocused);
        }
    }

    /**
     * The {@code Path} class represents a non-leaf component path in the hierarchy.
     */
    @OnlyIn(Dist.CLIENT)
    public static record Path(ContainerEventHandler component, ComponentPath childPath) implements ComponentPath {
        /**
         * Applies focus to or removes focus from the component associated with this component path.
         * focused {@code true} to apply focus, {@code false} to remove focus
         *
         * @param pFocused {@code true} to apply focus, {@code false} to remove focus.
         */
        @Override
        public void applyFocus(boolean pFocused) {
            if (!pFocused) {
                this.component.setFocused(null);
            } else {
                this.component.setFocused(this.childPath.component());
            }

            this.childPath.applyFocus(pFocused);
        }
    }
}

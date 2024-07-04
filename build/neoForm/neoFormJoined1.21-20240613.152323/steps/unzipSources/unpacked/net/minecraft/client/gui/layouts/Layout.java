package net.minecraft.client.gui.layouts;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface Layout extends LayoutElement {
    void visitChildren(Consumer<LayoutElement> pVisitor);

    @Override
    default void visitWidgets(Consumer<AbstractWidget> pConsumer) {
        this.visitChildren(p_270634_ -> p_270634_.visitWidgets(pConsumer));
    }

    default void arrangeElements() {
        this.visitChildren(p_270565_ -> {
            if (p_270565_ instanceof Layout layout) {
                layout.arrangeElements();
            }
        });
    }
}

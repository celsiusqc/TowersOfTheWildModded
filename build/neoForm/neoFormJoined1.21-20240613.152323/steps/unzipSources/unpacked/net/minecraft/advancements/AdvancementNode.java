package net.minecraft.advancements;

import com.google.common.annotations.VisibleForTesting;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import javax.annotation.Nullable;

public class AdvancementNode {
    private final AdvancementHolder holder;
    @Nullable
    private final AdvancementNode parent;
    private final Set<AdvancementNode> children = new ReferenceOpenHashSet<>();

    @VisibleForTesting
    public AdvancementNode(AdvancementHolder pHolder, @Nullable AdvancementNode pParent) {
        this.holder = pHolder;
        this.parent = pParent;
    }

    public Advancement advancement() {
        return this.holder.value();
    }

    public AdvancementHolder holder() {
        return this.holder;
    }

    @Nullable
    public AdvancementNode parent() {
        return this.parent;
    }

    public AdvancementNode root() {
        return getRoot(this);
    }

    public static AdvancementNode getRoot(AdvancementNode pNode) {
        AdvancementNode advancementnode = pNode;

        while (true) {
            AdvancementNode advancementnode1 = advancementnode.parent();
            if (advancementnode1 == null) {
                return advancementnode;
            }

            advancementnode = advancementnode1;
        }
    }

    public Iterable<AdvancementNode> children() {
        return this.children;
    }

    @VisibleForTesting
    public void addChild(AdvancementNode pChild) {
        this.children.add(pChild);
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            if (pOther instanceof AdvancementNode advancementnode && this.holder.equals(advancementnode.holder)) {
                return true;
            }

            return false;
        }
    }

    @Override
    public int hashCode() {
        return this.holder.hashCode();
    }

    @Override
    public String toString() {
        return this.holder.id().toString();
    }
}

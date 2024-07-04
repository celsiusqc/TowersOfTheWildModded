package net.minecraft.advancements;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

public class AdvancementTree {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<ResourceLocation, AdvancementNode> nodes = new Object2ObjectOpenHashMap<>();
    private final Set<AdvancementNode> roots = new ObjectLinkedOpenHashSet<>();
    private final Set<AdvancementNode> tasks = new ObjectLinkedOpenHashSet<>();
    @Nullable
    private AdvancementTree.Listener listener;

    private void remove(AdvancementNode pNode) {
        for (AdvancementNode advancementnode : pNode.children()) {
            this.remove(advancementnode);
        }

        LOGGER.info("Forgot about advancement {}", pNode.holder());
        this.nodes.remove(pNode.holder().id());
        if (pNode.parent() == null) {
            this.roots.remove(pNode);
            if (this.listener != null) {
                this.listener.onRemoveAdvancementRoot(pNode);
            }
        } else {
            this.tasks.remove(pNode);
            if (this.listener != null) {
                this.listener.onRemoveAdvancementTask(pNode);
            }
        }
    }

    public void remove(Set<ResourceLocation> pAdvancements) {
        for (ResourceLocation resourcelocation : pAdvancements) {
            AdvancementNode advancementnode = this.nodes.get(resourcelocation);
            if (advancementnode == null) {
                LOGGER.warn("Told to remove advancement {} but I don't know what that is", resourcelocation);
            } else {
                this.remove(advancementnode);
            }
        }
    }

    public void addAll(Collection<AdvancementHolder> pAdvancements) {
        List<AdvancementHolder> list = new ArrayList<>(pAdvancements);

        while (!list.isEmpty()) {
            if (!list.removeIf(this::tryInsert)) {
                LOGGER.error("Couldn't load advancements: {}", list);
                break;
            }
        }

        LOGGER.info("Loaded {} advancements", this.nodes.size());
    }

    private boolean tryInsert(AdvancementHolder p_301290_) {
        Optional<ResourceLocation> optional = p_301290_.value().parent();
        AdvancementNode advancementnode = optional.map(this.nodes::get).orElse(null);
        if (advancementnode == null && optional.isPresent()) {
            return false;
        } else {
            AdvancementNode advancementnode1 = new AdvancementNode(p_301290_, advancementnode);
            if (advancementnode != null) {
                advancementnode.addChild(advancementnode1);
            }

            this.nodes.put(p_301290_.id(), advancementnode1);
            if (advancementnode == null) {
                this.roots.add(advancementnode1);
                if (this.listener != null) {
                    this.listener.onAddAdvancementRoot(advancementnode1);
                }
            } else {
                this.tasks.add(advancementnode1);
                if (this.listener != null) {
                    this.listener.onAddAdvancementTask(advancementnode1);
                }
            }

            return true;
        }
    }

    public void clear() {
        this.nodes.clear();
        this.roots.clear();
        this.tasks.clear();
        if (this.listener != null) {
            this.listener.onAdvancementsCleared();
        }
    }

    public Iterable<AdvancementNode> roots() {
        return this.roots;
    }

    public Collection<AdvancementNode> nodes() {
        return this.nodes.values();
    }

    @Nullable
    public AdvancementNode get(ResourceLocation pId) {
        return this.nodes.get(pId);
    }

    @Nullable
    public AdvancementNode get(AdvancementHolder pAdvancement) {
        return this.nodes.get(pAdvancement.id());
    }

    public void setListener(@Nullable AdvancementTree.Listener pListener) {
        this.listener = pListener;
        if (pListener != null) {
            for (AdvancementNode advancementnode : this.roots) {
                pListener.onAddAdvancementRoot(advancementnode);
            }

            for (AdvancementNode advancementnode1 : this.tasks) {
                pListener.onAddAdvancementTask(advancementnode1);
            }
        }
    }

    public interface Listener {
        void onAddAdvancementRoot(AdvancementNode pAdvancement);

        void onRemoveAdvancementRoot(AdvancementNode pAdvancement);

        void onAddAdvancementTask(AdvancementNode pAdvancement);

        void onRemoveAdvancementTask(AdvancementNode pAdvancement);

        void onAdvancementsCleared();
    }
}

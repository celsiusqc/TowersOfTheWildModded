package net.minecraft.server.packs.resources;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.List;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ResourceLocationPattern;

public class ResourceFilterSection {
    private static final Codec<ResourceFilterSection> CODEC = RecordCodecBuilder.create(
        p_261431_ -> p_261431_.group(Codec.list(ResourceLocationPattern.CODEC).fieldOf("block").forGetter(p_215520_ -> p_215520_.blockList))
                .apply(p_261431_, ResourceFilterSection::new)
    );
    public static final MetadataSectionType<ResourceFilterSection> TYPE = MetadataSectionType.fromCodec("filter", CODEC);
    private final List<ResourceLocationPattern> blockList;

    public ResourceFilterSection(List<ResourceLocationPattern> p_215518_) {
        this.blockList = List.copyOf(p_215518_);
    }

    public boolean isNamespaceFiltered(String pNamespace) {
        return this.blockList.stream().anyMatch(p_261433_ -> p_261433_.namespacePredicate().test(pNamespace));
    }

    public boolean isPathFiltered(String pPath) {
        return this.blockList.stream().anyMatch(p_261430_ -> p_261430_.pathPredicate().test(pPath));
    }
}

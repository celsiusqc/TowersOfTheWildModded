package net.minecraft.client.searchtree;

import com.google.common.collect.ImmutableList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FullTextSearchTree<T> extends IdSearchTree<T> {
    private final SearchTree<T> plainTextSearchTree;

    public FullTextSearchTree(Function<T, Stream<String>> pFilter, Function<T, Stream<ResourceLocation>> pIdGetter, List<T> pContents) {
        super(pIdGetter, pContents);
        this.plainTextSearchTree = SearchTree.plainText(pContents, pFilter);
    }

    @Override
    protected List<T> searchPlainText(String pQuery) {
        return this.plainTextSearchTree.search(pQuery);
    }

    @Override
    protected List<T> searchResourceLocation(String pNamespace, String pPath) {
        List<T> list = this.resourceLocationSearchTree.searchNamespace(pNamespace);
        List<T> list1 = this.resourceLocationSearchTree.searchPath(pPath);
        List<T> list2 = this.plainTextSearchTree.search(pPath);
        Iterator<T> iterator = new MergingUniqueIterator<>(list1.iterator(), list2.iterator(), this.additionOrder);
        return ImmutableList.copyOf(new IntersectionIterator<>(list.iterator(), iterator, this.additionOrder));
    }
}

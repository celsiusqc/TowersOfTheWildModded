package net.minecraft.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.Util;

public class ClassTreeIdRegistry {
    public static final int NO_ID_VALUE = -1;
    private final Object2IntMap<Class<?>> classToLastIdCache = Util.make(new Object2IntOpenHashMap<>(), p_326384_ -> p_326384_.defaultReturnValue(-1));

    public int getLastIdFor(Class<?> pClazz) {
        int i = this.classToLastIdCache.getInt(pClazz);
        if (i != -1) {
            return i;
        } else {
            Class<?> oclass = pClazz;

            while ((oclass = oclass.getSuperclass()) != Object.class) {
                int j = this.classToLastIdCache.getInt(oclass);
                if (j != -1) {
                    return j;
                }
            }

            return -1;
        }
    }

    public int getCount(Class<?> pClazz) {
        return this.getLastIdFor(pClazz) + 1;
    }

    public int define(Class<?> pClazz) {
        int i = this.getLastIdFor(pClazz);
        int j = i == -1 ? 0 : i + 1;
        this.classToLastIdCache.put(pClazz, j);
        return j;
    }
}

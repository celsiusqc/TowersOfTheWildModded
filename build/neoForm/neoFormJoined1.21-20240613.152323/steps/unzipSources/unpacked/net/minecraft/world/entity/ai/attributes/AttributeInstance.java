package net.minecraft.world.entity.ai.attributes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

public class AttributeInstance {
    private static final String BASE_FIELD = "base";
    private static final String MODIFIERS_FIELD = "modifiers";
    public static final String ID_FIELD = "id";
    /**
     * The Attribute this is an instance of
     */
    private final Holder<Attribute> attribute;
    private final Map<AttributeModifier.Operation, Map<ResourceLocation, AttributeModifier>> modifiersByOperation = Maps.newEnumMap(
        AttributeModifier.Operation.class
    );
    private final Map<ResourceLocation, AttributeModifier> modifierById = new Object2ObjectArrayMap<>();
    private final Map<ResourceLocation, AttributeModifier> permanentModifiers = new Object2ObjectArrayMap<>();
    private double baseValue;
    private boolean dirty = true;
    private double cachedValue;
    private final Consumer<AttributeInstance> onDirty;

    public AttributeInstance(Holder<Attribute> pAttribute, Consumer<AttributeInstance> pOnDirty) {
        this.attribute = pAttribute;
        this.onDirty = pOnDirty;
        this.baseValue = pAttribute.value().getDefaultValue();
    }

    public Holder<Attribute> getAttribute() {
        return this.attribute;
    }

    public double getBaseValue() {
        return this.baseValue;
    }

    public void setBaseValue(double pBaseValue) {
        if (pBaseValue != this.baseValue) {
            this.baseValue = pBaseValue;
            this.setDirty();
        }
    }

    @VisibleForTesting
    Map<ResourceLocation, AttributeModifier> getModifiers(AttributeModifier.Operation pOperation) {
        return this.modifiersByOperation.computeIfAbsent(pOperation, p_332604_ -> new Object2ObjectOpenHashMap<>());
    }

    public Set<AttributeModifier> getModifiers() {
        return ImmutableSet.copyOf(this.modifierById.values());
    }

    @Nullable
    public AttributeModifier getModifier(ResourceLocation p_351007_) {
        return this.modifierById.get(p_351007_);
    }

    public boolean hasModifier(ResourceLocation p_350421_) {
        return this.modifierById.get(p_350421_) != null;
    }

    private void addModifier(AttributeModifier pModifier) {
        AttributeModifier attributemodifier = this.modifierById.putIfAbsent(pModifier.id(), pModifier);
        if (attributemodifier != null) {
            throw new IllegalArgumentException("Modifier is already applied on this attribute!");
        } else {
            this.getModifiers(pModifier.operation()).put(pModifier.id(), pModifier);
            this.setDirty();
        }
    }

    public void addOrUpdateTransientModifier(AttributeModifier pModifier) {
        AttributeModifier attributemodifier = this.modifierById.put(pModifier.id(), pModifier);
        if (pModifier != attributemodifier) {
            this.getModifiers(pModifier.operation()).put(pModifier.id(), pModifier);
            this.setDirty();
        }
    }

    public void addTransientModifier(AttributeModifier pModifier) {
        this.addModifier(pModifier);
    }

    public void addOrReplacePermanentModifier(AttributeModifier p_353041_) {
        this.removeModifier(p_353041_.id());
        this.addModifier(p_353041_);
        this.permanentModifiers.put(p_353041_.id(), p_353041_);
    }

    public void addPermanentModifier(AttributeModifier pModifier) {
        this.addModifier(pModifier);
        this.permanentModifiers.put(pModifier.id(), pModifier);
    }

    protected void setDirty() {
        this.dirty = true;
        this.onDirty.accept(this);
    }

    public void removeModifier(AttributeModifier pModifier) {
        this.removeModifier(pModifier.id());
    }

    public boolean removeModifier(ResourceLocation p_350300_) {
        AttributeModifier attributemodifier = this.modifierById.remove(p_350300_);
        if (attributemodifier == null) {
            return false;
        } else {
            this.getModifiers(attributemodifier.operation()).remove(p_350300_);
            this.permanentModifiers.remove(p_350300_);
            this.setDirty();
            return true;
        }
    }

    public void removeModifiers() {
        for (AttributeModifier attributemodifier : this.getModifiers()) {
            this.removeModifier(attributemodifier);
        }
    }

    public double getValue() {
        if (this.dirty) {
            this.cachedValue = this.calculateValue();
            this.dirty = false;
        }

        return this.cachedValue;
    }

    private double calculateValue() {
        double d0 = this.getBaseValue();

        for (AttributeModifier attributemodifier : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_VALUE)) {
            d0 += attributemodifier.amount();
        }

        double d1 = d0;

        for (AttributeModifier attributemodifier1 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_BASE)) {
            d1 += d0 * attributemodifier1.amount();
        }

        for (AttributeModifier attributemodifier2 : this.getModifiersOrEmpty(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)) {
            d1 *= 1.0 + attributemodifier2.amount();
        }

        return this.attribute.value().sanitizeValue(d1);
    }

    private Collection<AttributeModifier> getModifiersOrEmpty(AttributeModifier.Operation pOperation) {
        return this.modifiersByOperation.getOrDefault(pOperation, Map.of()).values();
    }

    public void replaceFrom(AttributeInstance pInstance) {
        this.baseValue = pInstance.baseValue;
        this.modifierById.clear();
        this.modifierById.putAll(pInstance.modifierById);
        this.permanentModifiers.clear();
        this.permanentModifiers.putAll(pInstance.permanentModifiers);
        this.modifiersByOperation.clear();
        pInstance.modifiersByOperation
            .forEach((p_332605_, p_332606_) -> this.getModifiers(p_332605_).putAll((Map<? extends ResourceLocation, ? extends AttributeModifier>)p_332606_));
        this.setDirty();
    }

    public CompoundTag save() {
        CompoundTag compoundtag = new CompoundTag();
        ResourceKey<Attribute> resourcekey = this.attribute
            .unwrapKey()
            .orElseThrow(() -> new IllegalStateException("Tried to serialize unregistered attribute"));
        compoundtag.putString("id", resourcekey.location().toString());
        compoundtag.putDouble("base", this.baseValue);
        if (!this.permanentModifiers.isEmpty()) {
            ListTag listtag = new ListTag();

            for (AttributeModifier attributemodifier : this.permanentModifiers.values()) {
                listtag.add(attributemodifier.save());
            }

            compoundtag.put("modifiers", listtag);
        }

        return compoundtag;
    }

    public void load(CompoundTag pNbt) {
        this.baseValue = pNbt.getDouble("base");
        if (pNbt.contains("modifiers", 9)) {
            ListTag listtag = pNbt.getList("modifiers", 10);

            for (int i = 0; i < listtag.size(); i++) {
                AttributeModifier attributemodifier = AttributeModifier.load(listtag.getCompound(i));
                if (attributemodifier != null) {
                    this.modifierById.put(attributemodifier.id(), attributemodifier);
                    this.getModifiers(attributemodifier.operation()).put(attributemodifier.id(), attributemodifier);
                    this.permanentModifiers.put(attributemodifier.id(), attributemodifier);
                }
            }
        }

        this.setDirty();
    }
}

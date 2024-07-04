package net.minecraft.world.entity.ai.attributes;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;

public class AttributeSupplier {
    private final Map<Holder<Attribute>, AttributeInstance> instances;

    AttributeSupplier(Map<Holder<Attribute>, AttributeInstance> pInstances) {
        this.instances = pInstances;
    }

    private AttributeInstance getAttributeInstance(Holder<Attribute> pAttribute) {
        AttributeInstance attributeinstance = this.instances.get(pAttribute);
        if (attributeinstance == null) {
            throw new IllegalArgumentException("Can't find attribute " + pAttribute.getRegisteredName());
        } else {
            return attributeinstance;
        }
    }

    public double getValue(Holder<Attribute> pAttribute) {
        return this.getAttributeInstance(pAttribute).getValue();
    }

    public double getBaseValue(Holder<Attribute> pAttribute) {
        return this.getAttributeInstance(pAttribute).getBaseValue();
    }

    public double getModifierValue(Holder<Attribute> p_316754_, ResourceLocation p_350708_) {
        AttributeModifier attributemodifier = this.getAttributeInstance(p_316754_).getModifier(p_350708_);
        if (attributemodifier == null) {
            throw new IllegalArgumentException("Can't find modifier " + p_350708_ + " on attribute " + p_316754_.getRegisteredName());
        } else {
            return attributemodifier.amount();
        }
    }

    @Nullable
    public AttributeInstance createInstance(Consumer<AttributeInstance> pOnDirty, Holder<Attribute> pAttribute) {
        AttributeInstance attributeinstance = this.instances.get(pAttribute);
        if (attributeinstance == null) {
            return null;
        } else {
            AttributeInstance attributeinstance1 = new AttributeInstance(pAttribute, pOnDirty);
            attributeinstance1.replaceFrom(attributeinstance);
            return attributeinstance1;
        }
    }

    public static AttributeSupplier.Builder builder() {
        return new AttributeSupplier.Builder();
    }

    public boolean hasAttribute(Holder<Attribute> pAttribute) {
        return this.instances.containsKey(pAttribute);
    }

    public boolean hasModifier(Holder<Attribute> p_316471_, ResourceLocation p_350394_) {
        AttributeInstance attributeinstance = this.instances.get(p_316471_);
        return attributeinstance != null && attributeinstance.getModifier(p_350394_) != null;
    }

    public static class Builder {
        private final Map<Holder<Attribute>, AttributeInstance> builder = new java.util.HashMap<>();
        private boolean instanceFrozen;
        private final java.util.List<AttributeSupplier.Builder> others = new java.util.ArrayList<>();

        public Builder() { }

        public Builder(AttributeSupplier attributeMap) {
            this.builder.putAll(attributeMap.instances);
        }

        public void combine(Builder other) {
            this.builder.putAll(other.builder);
            others.add(other);
        }

        public boolean hasAttribute(Holder<Attribute> attribute) {
            return this.builder.containsKey(attribute);
        }

        private AttributeInstance create(Holder<Attribute> pAttribute) {
            AttributeInstance attributeinstance = new AttributeInstance(pAttribute, p_315942_ -> {
                if (this.instanceFrozen) {
                    throw new UnsupportedOperationException("Tried to change value for default attribute instance: " + pAttribute.getRegisteredName());
                }
            });
            this.builder.put(pAttribute, attributeinstance);
            return attributeinstance;
        }

        public AttributeSupplier.Builder add(Holder<Attribute> pAttribute) {
            this.create(pAttribute);
            return this;
        }

        public AttributeSupplier.Builder add(Holder<Attribute> pAttribute, double pBaseValue) {
            AttributeInstance attributeinstance = this.create(pAttribute);
            attributeinstance.setBaseValue(pBaseValue);
            return this;
        }

        public AttributeSupplier build() {
            this.instanceFrozen = true;
            others.forEach(b -> b.instanceFrozen = true);
            return new AttributeSupplier(ImmutableMap.copyOf(this.builder));
        }
    }
}

package net.minecraft.world.level.block.state.properties;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Optional;

public class BooleanProperty extends Property<Boolean> {
    private final ImmutableSet<Boolean> values = ImmutableSet.of(true, false);

    protected BooleanProperty(String pName) {
        super(pName, Boolean.class);
    }

    @Override
    public Collection<Boolean> getPossibleValues() {
        return this.values;
    }

    public static BooleanProperty create(String pName) {
        return new BooleanProperty(pName);
    }

    @Override
    public Optional<Boolean> getValue(String pValue) {
        return !"true".equals(pValue) && !"false".equals(pValue) ? Optional.empty() : Optional.of(Boolean.valueOf(pValue));
    }

    /**
     * @return the name for the given value.
     */
    public String getName(Boolean pValue) {
        return pValue.toString();
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else {
            if (pOther instanceof BooleanProperty booleanproperty && super.equals(pOther)) {
                return this.values.equals(booleanproperty.values);
            }

            return false;
        }
    }

    @Override
    public int generateHashCode() {
        return 31 * super.generateHashCode() + this.values.hashCode();
    }
}

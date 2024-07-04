package net.minecraft.world.entity.ai.attributes;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Defines an entity attribute. These are properties of entities that can be dynamically modified.
 * @see net.minecraft.core.Registry#ATTRIBUTE
 */
public class Attribute {
    public static final Codec<Holder<Attribute>> CODEC = BuiltInRegistries.ATTRIBUTE.holderByNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<Attribute>> STREAM_CODEC = ByteBufCodecs.holderRegistry(Registries.ATTRIBUTE);
    /**
     * The default value of the attribute.
     */
    private final double defaultValue;
    /**
     * Whether the value of this attribute should be kept in sync on the client.
     */
    private boolean syncable;
    /**
     * A description Id for the attribute. This is most commonly used as the localization key.
     */
    private final String descriptionId;
    private Attribute.Sentiment sentiment = Attribute.Sentiment.POSITIVE;

    protected Attribute(String pDescriptionId, double pDefaultValue) {
        this.defaultValue = pDefaultValue;
        this.descriptionId = pDescriptionId;
    }

    public double getDefaultValue() {
        return this.defaultValue;
    }

    public boolean isClientSyncable() {
        return this.syncable;
    }

    /**
     * Sets whether the attribute value should be synced to the client.
     * @return The same attribute instance being modified.
     *
     * @param pWatch Whether the attribute value should be kept in sync.
     */
    public Attribute setSyncable(boolean pWatch) {
        this.syncable = pWatch;
        return this;
    }

    public Attribute setSentiment(Attribute.Sentiment p_347714_) {
        this.sentiment = p_347714_;
        return this;
    }

    /**
     * Sanitizes the value of the attribute to fit within the expected parameter range of the attribute.
     * @return The sanitized attribute value.
     *
     * @param pValue The value of the attribute to sanitize.
     */
    public double sanitizeValue(double pValue) {
        return pValue;
    }

    public String getDescriptionId() {
        return this.descriptionId;
    }

    public ChatFormatting getStyle(boolean p_347715_) {
        return this.sentiment.getStyle(p_347715_);
    }

    public static enum Sentiment {
        POSITIVE,
        NEUTRAL,
        NEGATIVE;

        public ChatFormatting getStyle(boolean p_347500_) {
            return switch (this) {
                case POSITIVE -> p_347500_ ? ChatFormatting.BLUE : ChatFormatting.RED;
                case NEUTRAL -> ChatFormatting.GRAY;
                case NEGATIVE -> p_347500_ ? ChatFormatting.RED : ChatFormatting.BLUE;
            };
        }
    }
}

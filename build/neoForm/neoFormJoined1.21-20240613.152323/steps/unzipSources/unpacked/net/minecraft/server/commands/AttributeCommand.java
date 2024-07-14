package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic3CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class AttributeCommand {
    private static final DynamicCommandExceptionType ERROR_NOT_LIVING_ENTITY = new DynamicCommandExceptionType(
        p_304174_ -> Component.translatableEscape("commands.attribute.failed.entity", p_304174_)
    );
    private static final Dynamic2CommandExceptionType ERROR_NO_SUCH_ATTRIBUTE = new Dynamic2CommandExceptionType(
        (p_304185_, p_304186_) -> Component.translatableEscape("commands.attribute.failed.no_attribute", p_304185_, p_304186_)
    );
    private static final Dynamic3CommandExceptionType ERROR_NO_SUCH_MODIFIER = new Dynamic3CommandExceptionType(
        (p_304182_, p_304183_, p_304184_) -> Component.translatableEscape("commands.attribute.failed.no_modifier", p_304183_, p_304182_, p_304184_)
    );
    private static final Dynamic3CommandExceptionType ERROR_MODIFIER_ALREADY_PRESENT = new Dynamic3CommandExceptionType(
        (p_304187_, p_304188_, p_304189_) -> Component.translatableEscape("commands.attribute.failed.modifier_already_present", p_304189_, p_304188_, p_304187_)
    );

    public static void register(CommandDispatcher<CommandSourceStack> pDispatcher, CommandBuildContext pContext) {
        pDispatcher.register(
            Commands.literal("attribute")
                .requires(p_212441_ -> p_212441_.hasPermission(2))
                .then(
                    Commands.argument("target", EntityArgument.entity())
                        .then(
                            Commands.argument("attribute", ResourceArgument.resource(pContext, Registries.ATTRIBUTE))
                                .then(
                                    Commands.literal("get")
                                        .executes(
                                            p_248109_ -> getAttributeValue(
                                                    p_248109_.getSource(),
                                                    EntityArgument.getEntity(p_248109_, "target"),
                                                    ResourceArgument.getAttribute(p_248109_, "attribute"),
                                                    1.0
                                                )
                                        )
                                        .then(
                                            Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                .executes(
                                                    p_248104_ -> getAttributeValue(
                                                            p_248104_.getSource(),
                                                            EntityArgument.getEntity(p_248104_, "target"),
                                                            ResourceArgument.getAttribute(p_248104_, "attribute"),
                                                            DoubleArgumentType.getDouble(p_248104_, "scale")
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("base")
                                        .then(
                                            Commands.literal("set")
                                                .then(
                                                    Commands.argument("value", DoubleArgumentType.doubleArg())
                                                        .executes(
                                                            p_248102_ -> setAttributeBase(
                                                                    p_248102_.getSource(),
                                                                    EntityArgument.getEntity(p_248102_, "target"),
                                                                    ResourceArgument.getAttribute(p_248102_, "attribute"),
                                                                    DoubleArgumentType.getDouble(p_248102_, "value")
                                                                )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.literal("get")
                                                .executes(
                                                    p_248112_ -> getAttributeBase(
                                                            p_248112_.getSource(),
                                                            EntityArgument.getEntity(p_248112_, "target"),
                                                            ResourceArgument.getAttribute(p_248112_, "attribute"),
                                                            1.0
                                                        )
                                                )
                                                .then(
                                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                        .executes(
                                                            p_248106_ -> getAttributeBase(
                                                                    p_248106_.getSource(),
                                                                    EntityArgument.getEntity(p_248106_, "target"),
                                                                    ResourceArgument.getAttribute(p_248106_, "attribute"),
                                                                    DoubleArgumentType.getDouble(p_248106_, "scale")
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(
                                    Commands.literal("modifier")
                                        .then(
                                            Commands.literal("add")
                                                .then(
                                                    Commands.argument("id", ResourceLocationArgument.id())
                                                        .then(
                                                            Commands.argument("value", DoubleArgumentType.doubleArg())
                                                                .then(
                                                                    Commands.literal("add_value")
                                                                        .executes(
                                                                            p_349940_ -> addModifier(
                                                                                    p_349940_.getSource(),
                                                                                    EntityArgument.getEntity(p_349940_, "target"),
                                                                                    ResourceArgument.getAttribute(p_349940_, "attribute"),
                                                                                    ResourceLocationArgument.getId(p_349940_, "id"),
                                                                                    DoubleArgumentType.getDouble(p_349940_, "value"),
                                                                                    AttributeModifier.Operation.ADD_VALUE
                                                                                )
                                                                        )
                                                                )
                                                                .then(
                                                                    Commands.literal("add_multiplied_base")
                                                                        .executes(
                                                                            p_349930_ -> addModifier(
                                                                                    p_349930_.getSource(),
                                                                                    EntityArgument.getEntity(p_349930_, "target"),
                                                                                    ResourceArgument.getAttribute(p_349930_, "attribute"),
                                                                                    ResourceLocationArgument.getId(p_349930_, "id"),
                                                                                    DoubleArgumentType.getDouble(p_349930_, "value"),
                                                                                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
                                                                                )
                                                                        )
                                                                )
                                                                .then(
                                                                    Commands.literal("add_multiplied_total")
                                                                        .executes(
                                                                            p_349945_ -> addModifier(
                                                                                    p_349945_.getSource(),
                                                                                    EntityArgument.getEntity(p_349945_, "target"),
                                                                                    ResourceArgument.getAttribute(p_349945_, "attribute"),
                                                                                    ResourceLocationArgument.getId(p_349945_, "id"),
                                                                                    DoubleArgumentType.getDouble(p_349945_, "value"),
                                                                                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.literal("remove")
                                                .then(
                                                    Commands.argument("id", ResourceLocationArgument.id())
                                                        .executes(
                                                            p_349938_ -> removeModifier(
                                                                    p_349938_.getSource(),
                                                                    EntityArgument.getEntity(p_349938_, "target"),
                                                                    ResourceArgument.getAttribute(p_349938_, "attribute"),
                                                                    ResourceLocationArgument.getId(p_349938_, "id")
                                                                )
                                                        )
                                                )
                                        )
                                        .then(
                                            Commands.literal("value")
                                                .then(
                                                    Commands.literal("get")
                                                        .then(
                                                            Commands.argument("id", ResourceLocationArgument.id())
                                                                .executes(
                                                                    p_349941_ -> getAttributeModifier(
                                                                            p_349941_.getSource(),
                                                                            EntityArgument.getEntity(p_349941_, "target"),
                                                                            ResourceArgument.getAttribute(p_349941_, "attribute"),
                                                                            ResourceLocationArgument.getId(p_349941_, "id"),
                                                                            1.0
                                                                        )
                                                                )
                                                                .then(
                                                                    Commands.argument("scale", DoubleArgumentType.doubleArg())
                                                                        .executes(
                                                                            p_349939_ -> getAttributeModifier(
                                                                                    p_349939_.getSource(),
                                                                                    EntityArgument.getEntity(p_349939_, "target"),
                                                                                    ResourceArgument.getAttribute(p_349939_, "attribute"),
                                                                                    ResourceLocationArgument.getId(p_349939_, "id"),
                                                                                    DoubleArgumentType.getDouble(p_349939_, "scale")
                                                                                )
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static AttributeInstance getAttributeInstance(Entity pEntity, Holder<Attribute> pAttribute) throws CommandSyntaxException {
        AttributeInstance attributeinstance = getLivingEntity(pEntity).getAttributes().getInstance(pAttribute);
        if (attributeinstance == null) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create(pEntity.getName(), getAttributeDescription(pAttribute));
        } else {
            return attributeinstance;
        }
    }

    private static LivingEntity getLivingEntity(Entity pTarget) throws CommandSyntaxException {
        if (!(pTarget instanceof LivingEntity)) {
            throw ERROR_NOT_LIVING_ENTITY.create(pTarget.getName());
        } else {
            return (LivingEntity)pTarget;
        }
    }

    private static LivingEntity getEntityWithAttribute(Entity pEntity, Holder<Attribute> pAttribute) throws CommandSyntaxException {
        LivingEntity livingentity = getLivingEntity(pEntity);
        if (!livingentity.getAttributes().hasAttribute(pAttribute)) {
            throw ERROR_NO_SUCH_ATTRIBUTE.create(pEntity.getName(), getAttributeDescription(pAttribute));
        } else {
            return livingentity;
        }
    }

    private static int getAttributeValue(CommandSourceStack pSource, Entity pEntity, Holder<Attribute> pAttribute, double pScale) throws CommandSyntaxException {
        LivingEntity livingentity = getEntityWithAttribute(pEntity, pAttribute);
        double d0 = livingentity.getAttributeValue(pAttribute);
        pSource.sendSuccess(
            () -> Component.translatable("commands.attribute.value.get.success", getAttributeDescription(pAttribute), pEntity.getName(), d0), false
        );
        return (int)(d0 * pScale);
    }

    private static int getAttributeBase(CommandSourceStack pSource, Entity pEntity, Holder<Attribute> pAttribute, double pScale) throws CommandSyntaxException {
        LivingEntity livingentity = getEntityWithAttribute(pEntity, pAttribute);
        double d0 = livingentity.getAttributeBaseValue(pAttribute);
        pSource.sendSuccess(
            () -> Component.translatable("commands.attribute.base_value.get.success", getAttributeDescription(pAttribute), pEntity.getName(), d0), false
        );
        return (int)(d0 * pScale);
    }

    private static int getAttributeModifier(
        CommandSourceStack p_136464_, Entity p_136465_, Holder<Attribute> p_250680_, ResourceLocation p_350277_, double p_136468_
    ) throws CommandSyntaxException {
        LivingEntity livingentity = getEntityWithAttribute(p_136465_, p_250680_);
        AttributeMap attributemap = livingentity.getAttributes();
        if (!attributemap.hasModifier(p_250680_, p_350277_)) {
            throw ERROR_NO_SUCH_MODIFIER.create(p_136465_.getName(), getAttributeDescription(p_250680_), p_350277_);
        } else {
            double d0 = attributemap.getModifierValue(p_250680_, p_350277_);
            p_136464_.sendSuccess(
                () -> Component.translatable(
                        "commands.attribute.modifier.value.get.success",
                        Component.translationArg(p_350277_),
                        getAttributeDescription(p_250680_),
                        p_136465_.getName(),
                        d0
                    ),
                false
            );
            return (int)(d0 * p_136468_);
        }
    }

    private static int setAttributeBase(CommandSourceStack pSource, Entity pEntity, Holder<Attribute> pAttribute, double pValue) throws CommandSyntaxException {
        getAttributeInstance(pEntity, pAttribute).setBaseValue(pValue);
        pSource.sendSuccess(
            () -> Component.translatable("commands.attribute.base_value.set.success", getAttributeDescription(pAttribute), pEntity.getName(), pValue),
            false
        );
        return 1;
    }

    private static int addModifier(
        CommandSourceStack p_136470_,
        Entity p_136471_,
        Holder<Attribute> p_251636_,
        ResourceLocation p_350414_,
        double p_136475_,
        AttributeModifier.Operation p_136476_
    ) throws CommandSyntaxException {
        AttributeInstance attributeinstance = getAttributeInstance(p_136471_, p_251636_);
        AttributeModifier attributemodifier = new AttributeModifier(p_350414_, p_136475_, p_136476_);
        if (attributeinstance.hasModifier(p_350414_)) {
            throw ERROR_MODIFIER_ALREADY_PRESENT.create(p_136471_.getName(), getAttributeDescription(p_251636_), p_350414_);
        } else {
            attributeinstance.addPermanentModifier(attributemodifier);
            p_136470_.sendSuccess(
                () -> Component.translatable(
                        "commands.attribute.modifier.add.success", Component.translationArg(p_350414_), getAttributeDescription(p_251636_), p_136471_.getName()
                    ),
                false
            );
            return 1;
        }
    }

    private static int removeModifier(CommandSourceStack p_136459_, Entity p_136460_, Holder<Attribute> p_250830_, ResourceLocation p_350686_) throws CommandSyntaxException {
        AttributeInstance attributeinstance = getAttributeInstance(p_136460_, p_250830_);
        if (attributeinstance.removeModifier(p_350686_)) {
            p_136459_.sendSuccess(
                () -> Component.translatable(
                        "commands.attribute.modifier.remove.success",
                        Component.translationArg(p_350686_),
                        getAttributeDescription(p_250830_),
                        p_136460_.getName()
                    ),
                false
            );
            return 1;
        } else {
            throw ERROR_NO_SUCH_MODIFIER.create(p_136460_.getName(), getAttributeDescription(p_250830_), p_350686_);
        }
    }

    private static Component getAttributeDescription(Holder<Attribute> pAttribute) {
        return Component.translatable(pAttribute.value().getDescriptionId());
    }
}
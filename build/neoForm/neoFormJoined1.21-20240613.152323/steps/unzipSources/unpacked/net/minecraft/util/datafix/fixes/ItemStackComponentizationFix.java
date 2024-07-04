package net.minecraft.util.datafix.fixes;

import com.google.common.base.Splitter;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.OptionalDynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.util.datafix.ComponentDataFixUtils;
import net.minecraft.util.datafix.schemas.NamespacedSchema;

public class ItemStackComponentizationFix extends DataFix {
    private static final int HIDE_ENCHANTMENTS = 1;
    private static final int HIDE_MODIFIERS = 2;
    private static final int HIDE_UNBREAKABLE = 4;
    private static final int HIDE_CAN_DESTROY = 8;
    private static final int HIDE_CAN_PLACE = 16;
    private static final int HIDE_ADDITIONAL = 32;
    private static final int HIDE_DYE = 64;
    private static final int HIDE_UPGRADES = 128;
    private static final Set<String> POTION_HOLDER_IDS = Set.of(
        "minecraft:potion", "minecraft:splash_potion", "minecraft:lingering_potion", "minecraft:tipped_arrow"
    );
    private static final Set<String> BUCKETED_MOB_IDS = Set.of(
        "minecraft:pufferfish_bucket",
        "minecraft:salmon_bucket",
        "minecraft:cod_bucket",
        "minecraft:tropical_fish_bucket",
        "minecraft:axolotl_bucket",
        "minecraft:tadpole_bucket"
    );
    private static final List<String> BUCKETED_MOB_TAGS = List.of(
        "NoAI", "Silent", "NoGravity", "Glowing", "Invulnerable", "Health", "Age", "Variant", "HuntingCooldown", "BucketVariantTag"
    );
    private static final Set<String> BOOLEAN_BLOCK_STATE_PROPERTIES = Set.of(
        "attached",
        "bottom",
        "conditional",
        "disarmed",
        "drag",
        "enabled",
        "extended",
        "eye",
        "falling",
        "hanging",
        "has_bottle_0",
        "has_bottle_1",
        "has_bottle_2",
        "has_record",
        "has_book",
        "inverted",
        "in_wall",
        "lit",
        "locked",
        "occupied",
        "open",
        "persistent",
        "powered",
        "short",
        "signal_fire",
        "snowy",
        "triggered",
        "unstable",
        "waterlogged",
        "berries",
        "bloom",
        "shrieking",
        "can_summon",
        "up",
        "down",
        "north",
        "east",
        "south",
        "west",
        "slot_0_occupied",
        "slot_1_occupied",
        "slot_2_occupied",
        "slot_3_occupied",
        "slot_4_occupied",
        "slot_5_occupied",
        "cracked",
        "crafting"
    );
    private static final Splitter PROPERTY_SPLITTER = Splitter.on(',');

    public ItemStackComponentizationFix(Schema pOutputSchema) {
        super(pOutputSchema, true);
    }

    private static void fixItemStack(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
        int i = pItemStackData.removeTag("HideFlags").asInt(0);
        pItemStackData.moveTagToComponent("Damage", "minecraft:damage", pTag.createInt(0));
        pItemStackData.moveTagToComponent("RepairCost", "minecraft:repair_cost", pTag.createInt(0));
        pItemStackData.moveTagToComponent("CustomModelData", "minecraft:custom_model_data");
        pItemStackData.removeTag("BlockStateTag")
            .result()
            .ifPresent(p_332594_ -> pItemStackData.setComponent("minecraft:block_state", fixBlockStateTag((Dynamic<?>)p_332594_)));
        pItemStackData.moveTagToComponent("EntityTag", "minecraft:entity_data");
        pItemStackData.fixSubTag("BlockEntityTag", false, p_330655_ -> {
            String s = NamespacedSchema.ensureNamespaced(p_330655_.get("id").asString(""));
            p_330655_ = fixBlockEntityTag(pItemStackData, p_330655_, s);
            Dynamic<?> dynamic2 = p_330655_.remove("id");
            return dynamic2.equals(p_330655_.emptyMap()) ? dynamic2 : p_330655_;
        });
        pItemStackData.moveTagToComponent("BlockEntityTag", "minecraft:block_entity_data");
        if (pItemStackData.removeTag("Unbreakable").asBoolean(false)) {
            Dynamic<?> dynamic = pTag.emptyMap();
            if ((i & 4) != 0) {
                dynamic = dynamic.set("show_in_tooltip", pTag.createBoolean(false));
            }

            pItemStackData.setComponent("minecraft:unbreakable", dynamic);
        }

        fixEnchantments(pItemStackData, pTag, "Enchantments", "minecraft:enchantments", (i & 1) != 0);
        if (pItemStackData.is("minecraft:enchanted_book")) {
            fixEnchantments(pItemStackData, pTag, "StoredEnchantments", "minecraft:stored_enchantments", (i & 32) != 0);
        }

        pItemStackData.fixSubTag("display", false, p_331784_ -> fixDisplay(pItemStackData, p_331784_, i));
        fixAdventureModeChecks(pItemStackData, pTag, i);
        fixAttributeModifiers(pItemStackData, pTag, i);
        Optional<? extends Dynamic<?>> optional = pItemStackData.removeTag("Trim").result();
        if (optional.isPresent()) {
            Dynamic<?> dynamic1 = (Dynamic<?>)optional.get();
            if ((i & 128) != 0) {
                dynamic1 = dynamic1.set("show_in_tooltip", dynamic1.createBoolean(false));
            }

            pItemStackData.setComponent("minecraft:trim", dynamic1);
        }

        if ((i & 32) != 0) {
            pItemStackData.setComponent("minecraft:hide_additional_tooltip", pTag.emptyMap());
        }

        if (pItemStackData.is("minecraft:crossbow")) {
            pItemStackData.removeTag("Charged");
            pItemStackData.moveTagToComponent("ChargedProjectiles", "minecraft:charged_projectiles", pTag.createList(Stream.empty()));
        }

        if (pItemStackData.is("minecraft:bundle")) {
            pItemStackData.moveTagToComponent("Items", "minecraft:bundle_contents", pTag.createList(Stream.empty()));
        }

        if (pItemStackData.is("minecraft:filled_map")) {
            pItemStackData.moveTagToComponent("map", "minecraft:map_id");
            Map<? extends Dynamic<?>, ? extends Dynamic<?>> map = pItemStackData.removeTag("Decorations")
                .asStream()
                .map(ItemStackComponentizationFix::fixMapDecoration)
                .collect(Collectors.toMap(Pair::getFirst, Pair::getSecond, (p_332591_, p_339605_) -> p_332591_));
            if (!map.isEmpty()) {
                pItemStackData.setComponent("minecraft:map_decorations", pTag.createMap(map));
            }
        }

        if (pItemStackData.is(POTION_HOLDER_IDS)) {
            fixPotionContents(pItemStackData, pTag);
        }

        if (pItemStackData.is("minecraft:writable_book")) {
            fixWritableBook(pItemStackData, pTag);
        }

        if (pItemStackData.is("minecraft:written_book")) {
            fixWrittenBook(pItemStackData, pTag);
        }

        if (pItemStackData.is("minecraft:suspicious_stew")) {
            pItemStackData.moveTagToComponent("effects", "minecraft:suspicious_stew_effects");
        }

        if (pItemStackData.is("minecraft:debug_stick")) {
            pItemStackData.moveTagToComponent("DebugProperty", "minecraft:debug_stick_state");
        }

        if (pItemStackData.is(BUCKETED_MOB_IDS)) {
            fixBucketedMobData(pItemStackData, pTag);
        }

        if (pItemStackData.is("minecraft:goat_horn")) {
            pItemStackData.moveTagToComponent("instrument", "minecraft:instrument");
        }

        if (pItemStackData.is("minecraft:knowledge_book")) {
            pItemStackData.moveTagToComponent("Recipes", "minecraft:recipes");
        }

        if (pItemStackData.is("minecraft:compass")) {
            fixLodestoneTracker(pItemStackData, pTag);
        }

        if (pItemStackData.is("minecraft:firework_rocket")) {
            fixFireworkRocket(pItemStackData);
        }

        if (pItemStackData.is("minecraft:firework_star")) {
            fixFireworkStar(pItemStackData);
        }

        if (pItemStackData.is("minecraft:player_head")) {
            pItemStackData.removeTag("SkullOwner").result().ifPresent(p_330565_ -> pItemStackData.setComponent("minecraft:profile", fixProfile((Dynamic<?>)p_330565_)));
        }
    }

    private static Dynamic<?> fixBlockStateTag(Dynamic<?> pTag) {
        return DataFixUtils.orElse(pTag.asMapOpt().result().map(p_339491_ -> p_339491_.collect(Collectors.toMap(Pair::getFirst, p_339490_ -> {
                String s = ((Dynamic)p_339490_.getFirst()).asString("");
                Dynamic<?> dynamic = (Dynamic<?>)p_339490_.getSecond();
                if (BOOLEAN_BLOCK_STATE_PROPERTIES.contains(s)) {
                    Optional<Boolean> optional = dynamic.asBoolean().result();
                    if (optional.isPresent()) {
                        return dynamic.createString(String.valueOf(optional.get()));
                    }
                }

                Optional<Number> optional1 = dynamic.asNumber().result();
                return optional1.isPresent() ? dynamic.createString(optional1.get().toString()) : dynamic;
            }))).map(pTag::createMap), pTag);
    }

    private static Dynamic<?> fixDisplay(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag, int pHideFlags) {
        pItemStackData.setComponent("minecraft:custom_name", pTag.get("Name"));
        pItemStackData.setComponent("minecraft:lore", pTag.get("Lore"));
        Optional<Integer> optional = pTag.get("color").asNumber().result().map(Number::intValue);
        boolean flag = (pHideFlags & 64) != 0;
        if (optional.isPresent() || flag) {
            Dynamic<?> dynamic = pTag.emptyMap().set("rgb", pTag.createInt(optional.orElse(10511680)));
            if (flag) {
                dynamic = dynamic.set("show_in_tooltip", pTag.createBoolean(false));
            }

            pItemStackData.setComponent("minecraft:dyed_color", dynamic);
        }

        Optional<String> optional1 = pTag.get("LocName").asString().result();
        if (optional1.isPresent()) {
            pItemStackData.setComponent("minecraft:item_name", ComponentDataFixUtils.createTranslatableComponent(pTag.getOps(), optional1.get()));
        }

        if (pItemStackData.is("minecraft:filled_map")) {
            pItemStackData.setComponent("minecraft:map_color", pTag.get("MapColor"));
            pTag = pTag.remove("MapColor");
        }

        return pTag.remove("Name").remove("Lore").remove("color").remove("LocName");
    }

    private static <T> Dynamic<T> fixBlockEntityTag(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<T> pTag, String pEntityId) {
        pItemStackData.setComponent("minecraft:lock", pTag.get("Lock"));
        pTag = pTag.remove("Lock");
        Optional<Dynamic<T>> optional = pTag.get("LootTable").result();
        if (optional.isPresent()) {
            Dynamic<T> dynamic = pTag.emptyMap().set("loot_table", optional.get());
            long i = pTag.get("LootTableSeed").asLong(0L);
            if (i != 0L) {
                dynamic = dynamic.set("seed", pTag.createLong(i));
            }

            pItemStackData.setComponent("minecraft:container_loot", dynamic);
            pTag = pTag.remove("LootTable").remove("LootTableSeed");
        }
        return switch (pEntityId) {
            case "minecraft:skull" -> {
                pItemStackData.setComponent("minecraft:note_block_sound", pTag.get("note_block_sound"));
                yield pTag.remove("note_block_sound");
            }
            case "minecraft:decorated_pot" -> {
                pItemStackData.setComponent("minecraft:pot_decorations", pTag.get("sherds"));
                Optional<Dynamic<T>> optional2 = pTag.get("item").result();
                if (optional2.isPresent()) {
                    pItemStackData.setComponent(
                        "minecraft:container",
                        pTag.createList(Stream.of(pTag.emptyMap().set("slot", pTag.createInt(0)).set("item", optional2.get())))
                    );
                }

                yield pTag.remove("sherds").remove("item");
            }
            case "minecraft:banner" -> {
                pItemStackData.setComponent("minecraft:banner_patterns", pTag.get("patterns"));
                Optional<Number> optional1 = pTag.get("Base").asNumber().result();
                if (optional1.isPresent()) {
                    pItemStackData.setComponent("minecraft:base_color", pTag.createString(BannerPatternFormatFix.fixColor(optional1.get().intValue())));
                }

                yield pTag.remove("patterns").remove("Base");
            }
            case "minecraft:shulker_box", "minecraft:chest", "minecraft:trapped_chest", "minecraft:furnace", "minecraft:ender_chest", "minecraft:dispenser", "minecraft:dropper", "minecraft:brewing_stand", "minecraft:hopper", "minecraft:barrel", "minecraft:smoker", "minecraft:blast_furnace", "minecraft:campfire", "minecraft:chiseled_bookshelf", "minecraft:crafter" -> {
                List<Dynamic<T>> list = pTag.get("Items")
                    .asList(
                        p_332590_ -> p_332590_.emptyMap()
                                .set("slot", p_332590_.createInt(p_332590_.get("Slot").asByte((byte)0) & 255))
                                .set("item", p_332590_.remove("Slot"))
                    );
                if (!list.isEmpty()) {
                    pItemStackData.setComponent("minecraft:container", pTag.createList(list.stream()));
                }

                yield pTag.remove("Items");
            }
            case "minecraft:beehive" -> {
                pItemStackData.setComponent("minecraft:bees", pTag.get("bees"));
                yield pTag.remove("bees");
            }
            default -> pTag;
        };
    }

    private static void fixEnchantments(
        ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag, String pKey, String pComponent, boolean pHideEnchantments
    ) {
        OptionalDynamic<?> optionaldynamic = pItemStackData.removeTag(pKey);
        List<Pair<String, Integer>> list = optionaldynamic.asList(Function.identity())
            .stream()
            .flatMap(p_330659_ -> parseEnchantment((Dynamic<?>)p_330659_).stream())
            .toList();
        if (!list.isEmpty() || pHideEnchantments) {
            Dynamic<?> dynamic = pTag.emptyMap();
            Dynamic<?> dynamic1 = pTag.emptyMap();

            for (Pair<String, Integer> pair : list) {
                dynamic1 = dynamic1.set(pair.getFirst(), pTag.createInt(pair.getSecond()));
            }

            dynamic = dynamic.set("levels", dynamic1);
            if (pHideEnchantments) {
                dynamic = dynamic.set("show_in_tooltip", pTag.createBoolean(false));
            }

            pItemStackData.setComponent(pComponent, dynamic);
        }

        if (optionaldynamic.result().isPresent() && list.isEmpty()) {
            pItemStackData.setComponent("minecraft:enchantment_glint_override", pTag.createBoolean(true));
        }
    }

    private static Optional<Pair<String, Integer>> parseEnchantment(Dynamic<?> pEnchantmentTag) {
        return pEnchantmentTag.get("id")
            .asString()
            .apply2stable((p_331946_, p_330581_) -> Pair.of(p_331946_, Mth.clamp(p_330581_.intValue(), 0, 255)), pEnchantmentTag.get("lvl").asNumber())
            .result();
    }

    private static void fixAdventureModeChecks(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag, int pHideFlags) {
        fixBlockStatePredicates(pItemStackData, pTag, "CanDestroy", "minecraft:can_break", (pHideFlags & 8) != 0);
        fixBlockStatePredicates(pItemStackData, pTag, "CanPlaceOn", "minecraft:can_place_on", (pHideFlags & 16) != 0);
    }

    private static void fixBlockStatePredicates(
        ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag, String pKey, String pComponent, boolean pHide
    ) {
        Optional<? extends Dynamic<?>> optional = pItemStackData.removeTag(pKey).result();
        if (!optional.isEmpty()) {
            Dynamic<?> dynamic = pTag.emptyMap()
                .set(
                    "predicates",
                    pTag.createList(
                        optional.get()
                            .asStream()
                            .map(
                                p_337638_ -> DataFixUtils.orElse(
                                        p_337638_.asString().map(p_330959_ -> fixBlockStatePredicate((Dynamic<?>)p_337638_, p_330959_)).result(), p_337638_
                                    )
                            )
                    )
                );
            if (pHide) {
                dynamic = dynamic.set("show_in_tooltip", pTag.createBoolean(false));
            }

            pItemStackData.setComponent(pComponent, dynamic);
        }
    }

    private static Dynamic<?> fixBlockStatePredicate(Dynamic<?> pTag, String pBlockId) {
        int i = pBlockId.indexOf(91);
        int j = pBlockId.indexOf(123);
        int k = pBlockId.length();
        if (i != -1) {
            k = i;
        }

        if (j != -1) {
            k = Math.min(k, j);
        }

        String s = pBlockId.substring(0, k);
        Dynamic<?> dynamic = pTag.emptyMap().set("blocks", pTag.createString(s.trim()));
        int l = pBlockId.indexOf(93);
        if (i != -1 && l != -1) {
            Dynamic<?> dynamic1 = pTag.emptyMap();

            for (String s1 : PROPERTY_SPLITTER.split(pBlockId.substring(i + 1, l))) {
                int i1 = s1.indexOf(61);
                if (i1 != -1) {
                    String s2 = s1.substring(0, i1).trim();
                    String s3 = s1.substring(i1 + 1).trim();
                    dynamic1 = dynamic1.set(s2, pTag.createString(s3));
                }
            }

            dynamic = dynamic.set("state", dynamic1);
        }

        int j1 = pBlockId.indexOf(125);
        if (j != -1 && j1 != -1) {
            dynamic = dynamic.set("nbt", pTag.createString(pBlockId.substring(j, j1 + 1)));
        }

        return dynamic;
    }

    private static void fixAttributeModifiers(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag, int pHideFlags) {
        OptionalDynamic<?> optionaldynamic = pItemStackData.removeTag("AttributeModifiers");
        if (!optionaldynamic.result().isEmpty()) {
            boolean flag = (pHideFlags & 2) != 0;
            List<? extends Dynamic<?>> list = optionaldynamic.asList(ItemStackComponentizationFix::fixAttributeModifier);
            Dynamic<?> dynamic = pTag.emptyMap().set("modifiers", pTag.createList(list.stream()));
            if (flag) {
                dynamic = dynamic.set("show_in_tooltip", pTag.createBoolean(false));
            }

            pItemStackData.setComponent("minecraft:attribute_modifiers", dynamic);
        }
    }

    private static Dynamic<?> fixAttributeModifier(Dynamic<?> p_331035_) {
        Dynamic<?> dynamic = p_331035_.emptyMap()
            .set("name", p_331035_.createString(""))
            .set("amount", p_331035_.createDouble(0.0))
            .set("operation", p_331035_.createString("add_value"));
        dynamic = Dynamic.copyField(p_331035_, "AttributeName", dynamic, "type");
        dynamic = Dynamic.copyField(p_331035_, "Slot", dynamic, "slot");
        dynamic = Dynamic.copyField(p_331035_, "UUID", dynamic, "uuid");
        dynamic = Dynamic.copyField(p_331035_, "Name", dynamic, "name");
        dynamic = Dynamic.copyField(p_331035_, "Amount", dynamic, "amount");
        return Dynamic.copyAndFixField(p_331035_, "Operation", dynamic, "operation", p_330453_ -> {
            return p_330453_.createString(switch (p_330453_.asInt(0)) {
                case 1 -> "add_multiplied_base";
                case 2 -> "add_multiplied_total";
                default -> "add_value";
            });
        });
    }

    private static Pair<Dynamic<?>, Dynamic<?>> fixMapDecoration(Dynamic<?> p_332095_) {
        Dynamic<?> dynamic = DataFixUtils.orElseGet(p_332095_.get("id").result(), () -> p_332095_.createString(""));
        Dynamic<?> dynamic1 = p_332095_.emptyMap()
            .set("type", p_332095_.createString(fixMapDecorationType(p_332095_.get("type").asInt(0))))
            .set("x", p_332095_.createDouble(p_332095_.get("x").asDouble(0.0)))
            .set("z", p_332095_.createDouble(p_332095_.get("z").asDouble(0.0)))
            .set("rotation", p_332095_.createFloat((float)p_332095_.get("rot").asDouble(0.0)));
        return Pair.of(dynamic, dynamic1);
    }

    private static String fixMapDecorationType(int pDecorationType) {
        return switch (pDecorationType) {
            case 1 -> "frame";
            case 2 -> "red_marker";
            case 3 -> "blue_marker";
            case 4 -> "target_x";
            case 5 -> "target_point";
            case 6 -> "player_off_map";
            case 7 -> "player_off_limits";
            case 8 -> "mansion";
            case 9 -> "monument";
            case 10 -> "banner_white";
            case 11 -> "banner_orange";
            case 12 -> "banner_magenta";
            case 13 -> "banner_light_blue";
            case 14 -> "banner_yellow";
            case 15 -> "banner_lime";
            case 16 -> "banner_pink";
            case 17 -> "banner_gray";
            case 18 -> "banner_light_gray";
            case 19 -> "banner_cyan";
            case 20 -> "banner_purple";
            case 21 -> "banner_blue";
            case 22 -> "banner_brown";
            case 23 -> "banner_green";
            case 24 -> "banner_red";
            case 25 -> "banner_black";
            case 26 -> "red_x";
            case 27 -> "village_desert";
            case 28 -> "village_plains";
            case 29 -> "village_savanna";
            case 30 -> "village_snowy";
            case 31 -> "village_taiga";
            case 32 -> "jungle_temple";
            case 33 -> "swamp_hut";
            default -> "player";
        };
    }

    private static void fixPotionContents(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
        Dynamic<?> dynamic = pTag.emptyMap();
        Optional<String> optional = pItemStackData.removeTag("Potion").asString().result().filter(p_330426_ -> !p_330426_.equals("minecraft:empty"));
        if (optional.isPresent()) {
            dynamic = dynamic.set("potion", pTag.createString(optional.get()));
        }

        dynamic = pItemStackData.moveTagInto("CustomPotionColor", dynamic, "custom_color");
        dynamic = pItemStackData.moveTagInto("custom_potion_effects", dynamic, "custom_effects");
        if (!dynamic.equals(pTag.emptyMap())) {
            pItemStackData.setComponent("minecraft:potion_contents", dynamic);
        }
    }

    private static void fixWritableBook(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
        Dynamic<?> dynamic = fixBookPages(pItemStackData, pTag);
        if (dynamic != null) {
            pItemStackData.setComponent("minecraft:writable_book_content", pTag.emptyMap().set("pages", dynamic));
        }
    }

    private static void fixWrittenBook(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
        Dynamic<?> dynamic = fixBookPages(pItemStackData, pTag);
        String s = pItemStackData.removeTag("title").asString("");
        Optional<String> optional = pItemStackData.removeTag("filtered_title").asString().result();
        Dynamic<?> dynamic1 = pTag.emptyMap();
        dynamic1 = dynamic1.set("title", createFilteredText(pTag, s, optional));
        dynamic1 = pItemStackData.moveTagInto("author", dynamic1, "author");
        dynamic1 = pItemStackData.moveTagInto("resolved", dynamic1, "resolved");
        dynamic1 = pItemStackData.moveTagInto("generation", dynamic1, "generation");
        if (dynamic != null) {
            dynamic1 = dynamic1.set("pages", dynamic);
        }

        pItemStackData.setComponent("minecraft:written_book_content", dynamic1);
    }

    @Nullable
    private static Dynamic<?> fixBookPages(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
        List<String> list = pItemStackData.removeTag("pages").asList(p_331677_ -> p_331677_.asString(""));
        Map<String, String> map = pItemStackData.removeTag("filtered_pages").asMap(p_332151_ -> p_332151_.asString("0"), p_330471_ -> p_330471_.asString(""));
        if (list.isEmpty()) {
            return null;
        } else {
            List<Dynamic<?>> list1 = new ArrayList<>(list.size());

            for (int i = 0; i < list.size(); i++) {
                String s = list.get(i);
                String s1 = map.get(String.valueOf(i));
                list1.add(createFilteredText(pTag, s, Optional.ofNullable(s1)));
            }

            return pTag.createList(list1.stream());
        }
    }

    private static Dynamic<?> createFilteredText(Dynamic<?> pTag, String pUnfilteredText, Optional<String> pFilteredText) {
        Dynamic<?> dynamic = pTag.emptyMap().set("raw", pTag.createString(pUnfilteredText));
        if (pFilteredText.isPresent()) {
            dynamic = dynamic.set("filtered", pTag.createString(pFilteredText.get()));
        }

        return dynamic;
    }

    private static void fixBucketedMobData(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
        Dynamic<?> dynamic = pTag.emptyMap();

        for (String s : BUCKETED_MOB_TAGS) {
            dynamic = pItemStackData.moveTagInto(s, dynamic, s);
        }

        if (!dynamic.equals(pTag.emptyMap())) {
            pItemStackData.setComponent("minecraft:bucket_entity_data", dynamic);
        }
    }

    private static void fixLodestoneTracker(ItemStackComponentizationFix.ItemStackData pItemStackData, Dynamic<?> pTag) {
        Optional<? extends Dynamic<?>> optional = pItemStackData.removeTag("LodestonePos").result();
        Optional<? extends Dynamic<?>> optional1 = pItemStackData.removeTag("LodestoneDimension").result();
        if (!optional.isEmpty() || !optional1.isEmpty()) {
            boolean flag = pItemStackData.removeTag("LodestoneTracked").asBoolean(true);
            Dynamic<?> dynamic = pTag.emptyMap();
            if (optional.isPresent() && optional1.isPresent()) {
                dynamic = dynamic.set("target", pTag.emptyMap().set("pos", (Dynamic<?>)optional.get()).set("dimension", (Dynamic<?>)optional1.get()));
            }

            if (!flag) {
                dynamic = dynamic.set("tracked", pTag.createBoolean(false));
            }

            pItemStackData.setComponent("minecraft:lodestone_tracker", dynamic);
        }
    }

    private static void fixFireworkStar(ItemStackComponentizationFix.ItemStackData pItemStackData) {
        pItemStackData.fixSubTag("Explosion", true, p_331995_ -> {
            pItemStackData.setComponent("minecraft:firework_explosion", fixFireworkExplosion(p_331995_));
            return p_331995_.remove("Type").remove("Colors").remove("FadeColors").remove("Trail").remove("Flicker");
        });
    }

    private static void fixFireworkRocket(ItemStackComponentizationFix.ItemStackData pItemStackData) {
        pItemStackData.fixSubTag(
            "Fireworks",
            true,
            p_331577_ -> {
                Stream<? extends Dynamic<?>> stream = p_331577_.get("Explosions").asStream().map(ItemStackComponentizationFix::fixFireworkExplosion);
                int i = p_331577_.get("Flight").asInt(0);
                pItemStackData.setComponent(
                    "minecraft:fireworks",
                    p_331577_.emptyMap().set("explosions", p_331577_.createList(stream)).set("flight_duration", p_331577_.createByte((byte)i))
                );
                return p_331577_.remove("Explosions").remove("Flight");
            }
        );
    }

    private static Dynamic<?> fixFireworkExplosion(Dynamic<?> p_332063_) {
        p_332063_ = p_332063_.set("shape", p_332063_.createString(switch (p_332063_.get("Type").asInt(0)) {
            case 1 -> "large_ball";
            case 2 -> "star";
            case 3 -> "creeper";
            case 4 -> "burst";
            default -> "small_ball";
        })).remove("Type");
        p_332063_ = p_332063_.renameField("Colors", "colors");
        p_332063_ = p_332063_.renameField("FadeColors", "fade_colors");
        p_332063_ = p_332063_.renameField("Trail", "has_trail");
        return p_332063_.renameField("Flicker", "has_twinkle");
    }

    public static Dynamic<?> fixProfile(Dynamic<?> pTag) {
        Optional<String> optional = pTag.asString().result();
        if (optional.isPresent()) {
            return isValidPlayerName(optional.get()) ? pTag.emptyMap().set("name", pTag.createString(optional.get())) : pTag.emptyMap();
        } else {
            String s = pTag.get("Name").asString("");
            Optional<? extends Dynamic<?>> optional1 = pTag.get("Id").result();
            Dynamic<?> dynamic = fixProfileProperties(pTag.get("Properties"));
            Dynamic<?> dynamic1 = pTag.emptyMap();
            if (isValidPlayerName(s)) {
                dynamic1 = dynamic1.set("name", pTag.createString(s));
            }

            if (optional1.isPresent()) {
                dynamic1 = dynamic1.set("id", (Dynamic<?>)optional1.get());
            }

            if (dynamic != null) {
                dynamic1 = dynamic1.set("properties", dynamic);
            }

            return dynamic1;
        }
    }

    private static boolean isValidPlayerName(String pName) {
        return pName.length() > 16 ? false : pName.chars().filter(p_332597_ -> p_332597_ <= 32 || p_332597_ >= 127).findAny().isEmpty();
    }

    @Nullable
    private static Dynamic<?> fixProfileProperties(OptionalDynamic<?> pTag) {
        Map<String, List<Pair<String, Optional<String>>>> map = pTag.asMap(
            p_331855_ -> p_331855_.asString(""), p_331384_ -> p_331384_.asList(p_337640_ -> {
                    String s = p_337640_.get("Value").asString("");
                    Optional<String> optional = p_337640_.get("Signature").asString().result();
                    return Pair.of(s, optional);
                })
        );
        return map.isEmpty()
            ? null
            : pTag.createList(
                map.entrySet()
                    .stream()
                    .flatMap(
                        p_331925_ -> p_331925_.getValue()
                                .stream()
                                .map(
                                    p_331949_ -> {
                                        Dynamic<?> dynamic = pTag.emptyMap()
                                            .set("name", pTag.createString(p_331925_.getKey()))
                                            .set("value", pTag.createString(p_331949_.getFirst()));
                                        Optional<String> optional = p_331949_.getSecond();
                                        return optional.isPresent() ? dynamic.set("signature", pTag.createString(optional.get())) : dynamic;
                                    }
                                )
                    )
            );
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.writeFixAndRead(
            "ItemStack componentization",
            this.getInputSchema().getType(References.ITEM_STACK),
            this.getOutputSchema().getType(References.ITEM_STACK),
            p_331180_ -> {
                Optional<? extends Dynamic<?>> optional = ItemStackComponentizationFix.ItemStackData.read(p_331180_).map(p_330696_ -> {
                    fixItemStack(p_330696_, p_330696_.tag);
                    return p_330696_.write();
                });
                return DataFixUtils.orElse(optional, p_331180_);
            }
        );
    }

    static class ItemStackData {
        private final String item;
        private final int count;
        private Dynamic<?> components;
        private final Dynamic<?> remainder;
        Dynamic<?> tag;

        private ItemStackData(String pItem, int pCount, Dynamic<?> pNbt) {
            this.item = NamespacedSchema.ensureNamespaced(pItem);
            this.count = pCount;
            this.components = pNbt.emptyMap();
            this.tag = pNbt.get("tag").orElseEmptyMap();
            this.remainder = pNbt.remove("tag");
        }

        public static Optional<ItemStackComponentizationFix.ItemStackData> read(Dynamic<?> pTag) {
            return pTag.get("id")
                .asString()
                .apply2stable(
                    (p_331191_, p_330701_) -> new ItemStackComponentizationFix.ItemStackData(
                            p_331191_, p_330701_.intValue(), pTag.remove("id").remove("Count")
                        ),
                    pTag.get("Count").asNumber()
                )
                .result();
        }

        public OptionalDynamic<?> removeTag(String pKey) {
            OptionalDynamic<?> optionaldynamic = this.tag.get(pKey);
            this.tag = this.tag.remove(pKey);
            return optionaldynamic;
        }

        public void setComponent(String pComponent, Dynamic<?> pValue) {
            this.components = this.components.set(pComponent, pValue);
        }

        public void setComponent(String pComponent, OptionalDynamic<?> pValue) {
            pValue.result().ifPresent(p_332105_ -> this.components = this.components.set(pComponent, (Dynamic<?>)p_332105_));
        }

        public Dynamic<?> moveTagInto(String pOldKey, Dynamic<?> pTag, String pNewKey) {
            Optional<? extends Dynamic<?>> optional = this.removeTag(pOldKey).result();
            return optional.isPresent() ? pTag.set(pNewKey, (Dynamic<?>)optional.get()) : pTag;
        }

        public void moveTagToComponent(String pKey, String pComponent, Dynamic<?> pTag) {
            Optional<? extends Dynamic<?>> optional = this.removeTag(pKey).result();
            if (optional.isPresent() && !optional.get().equals(pTag)) {
                this.setComponent(pComponent, (Dynamic<?>)optional.get());
            }
        }

        public void moveTagToComponent(String pKey, String pComponent) {
            this.removeTag(pKey).result().ifPresent(p_330514_ -> this.setComponent(pComponent, (Dynamic<?>)p_330514_));
        }

        public void fixSubTag(String pKey, boolean pSkipIfEmpty, UnaryOperator<Dynamic<?>> pFixer) {
            OptionalDynamic<?> optionaldynamic = this.tag.get(pKey);
            if (!pSkipIfEmpty || !optionaldynamic.result().isEmpty()) {
                Dynamic<?> dynamic = optionaldynamic.orElseEmptyMap();
                dynamic = pFixer.apply(dynamic);
                if (dynamic.equals(dynamic.emptyMap())) {
                    this.tag = this.tag.remove(pKey);
                } else {
                    this.tag = this.tag.set(pKey, dynamic);
                }
            }
        }

        public Dynamic<?> write() {
            Dynamic<?> dynamic = this.tag.emptyMap().set("id", this.tag.createString(this.item)).set("count", this.tag.createInt(this.count));
            if (!this.tag.equals(this.tag.emptyMap())) {
                this.components = this.components.set("minecraft:custom_data", this.tag);
            }

            if (!this.components.equals(this.tag.emptyMap())) {
                dynamic = dynamic.set("components", this.components);
            }

            return mergeRemainder(dynamic, this.remainder);
        }

        private static <T> Dynamic<T> mergeRemainder(Dynamic<T> pTag, Dynamic<?> pRemainder) {
            DynamicOps<T> dynamicops = pTag.getOps();
            return dynamicops.getMap(pTag.getValue())
                .flatMap(p_330670_ -> dynamicops.mergeToMap(pRemainder.convert(dynamicops).getValue(), (MapLike<T>)p_330670_))
                .map(p_331482_ -> new Dynamic<>(dynamicops, (T)p_331482_))
                .result()
                .orElse(pTag);
        }

        public boolean is(String pItem) {
            return this.item.equals(pItem);
        }

        public boolean is(Set<String> pItems) {
            return pItems.contains(this.item);
        }

        public boolean hasComponent(String pComponent) {
            return this.components.get(pComponent).result().isPresent();
        }
    }
}

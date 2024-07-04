package net.minecraft.util.datafix.schemas;

import com.google.common.collect.Maps;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.types.templates.Hook.HookFunction;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.util.datafix.fixes.References;
import org.slf4j.Logger;

public class V99 extends Schema {
    private static final Logger LOGGER = LogUtils.getLogger();
    static final Map<String, String> ITEM_TO_BLOCKENTITY = DataFixUtils.make(Maps.newHashMap(), p_145919_ -> {
        p_145919_.put("minecraft:furnace", "Furnace");
        p_145919_.put("minecraft:lit_furnace", "Furnace");
        p_145919_.put("minecraft:chest", "Chest");
        p_145919_.put("minecraft:trapped_chest", "Chest");
        p_145919_.put("minecraft:ender_chest", "EnderChest");
        p_145919_.put("minecraft:jukebox", "RecordPlayer");
        p_145919_.put("minecraft:dispenser", "Trap");
        p_145919_.put("minecraft:dropper", "Dropper");
        p_145919_.put("minecraft:sign", "Sign");
        p_145919_.put("minecraft:mob_spawner", "MobSpawner");
        p_145919_.put("minecraft:noteblock", "Music");
        p_145919_.put("minecraft:brewing_stand", "Cauldron");
        p_145919_.put("minecraft:enhanting_table", "EnchantTable");
        p_145919_.put("minecraft:command_block", "CommandBlock");
        p_145919_.put("minecraft:beacon", "Beacon");
        p_145919_.put("minecraft:skull", "Skull");
        p_145919_.put("minecraft:daylight_detector", "DLDetector");
        p_145919_.put("minecraft:hopper", "Hopper");
        p_145919_.put("minecraft:banner", "Banner");
        p_145919_.put("minecraft:flower_pot", "FlowerPot");
        p_145919_.put("minecraft:repeating_command_block", "CommandBlock");
        p_145919_.put("minecraft:chain_command_block", "CommandBlock");
        p_145919_.put("minecraft:standing_sign", "Sign");
        p_145919_.put("minecraft:wall_sign", "Sign");
        p_145919_.put("minecraft:piston_head", "Piston");
        p_145919_.put("minecraft:daylight_detector_inverted", "DLDetector");
        p_145919_.put("minecraft:unpowered_comparator", "Comparator");
        p_145919_.put("minecraft:powered_comparator", "Comparator");
        p_145919_.put("minecraft:wall_banner", "Banner");
        p_145919_.put("minecraft:standing_banner", "Banner");
        p_145919_.put("minecraft:structure_block", "Structure");
        p_145919_.put("minecraft:end_portal", "Airportal");
        p_145919_.put("minecraft:end_gateway", "EndGateway");
        p_145919_.put("minecraft:shield", "Banner");
    });
    public static final Map<String, String> ITEM_TO_ENTITY = Map.of("minecraft:armor_stand", "ArmorStand", "minecraft:painting", "Painting");
    protected static final HookFunction ADD_NAMES = new HookFunction() {
        @Override
        public <T> T apply(DynamicOps<T> p_18312_, T p_18313_) {
            return V99.addNames(new Dynamic<>(p_18312_, p_18313_), V99.ITEM_TO_BLOCKENTITY, V99.ITEM_TO_ENTITY);
        }
    };

    public V99(int pVersionKey, Schema pParent) {
        super(pVersionKey, pParent);
    }

    protected static TypeTemplate equipment(Schema pSchema) {
        return DSL.optionalFields("Equipment", DSL.list(References.ITEM_STACK.in(pSchema)));
    }

    protected static void registerMob(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.register(pMap, pName, () -> equipment(pSchema));
    }

    protected static void registerThrowableProjectile(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.register(pMap, pName, () -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema)));
    }

    protected static void registerMinecart(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.register(pMap, pName, () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema)));
    }

    protected static void registerInventory(Schema pSchema, Map<String, Supplier<TypeTemplate>> pMap, String pName) {
        pSchema.register(pMap, pName, () -> DSL.optionalFields("Items", DSL.list(References.ITEM_STACK.in(pSchema))));
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
        pSchema.register(map, "Item", p_18301_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(pSchema)));
        pSchema.registerSimple(map, "XPOrb");
        registerThrowableProjectile(pSchema, map, "ThrownEgg");
        pSchema.registerSimple(map, "LeashKnot");
        pSchema.registerSimple(map, "Painting");
        pSchema.register(map, "Arrow", p_18298_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema)));
        pSchema.register(map, "TippedArrow", p_18295_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema)));
        pSchema.register(map, "SpectralArrow", p_18292_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema)));
        registerThrowableProjectile(pSchema, map, "Snowball");
        registerThrowableProjectile(pSchema, map, "Fireball");
        registerThrowableProjectile(pSchema, map, "SmallFireball");
        registerThrowableProjectile(pSchema, map, "ThrownEnderpearl");
        pSchema.registerSimple(map, "EyeOfEnderSignal");
        pSchema.register(
            map, "ThrownPotion", p_18289_ -> DSL.optionalFields("inTile", References.BLOCK_NAME.in(pSchema), "Potion", References.ITEM_STACK.in(pSchema))
        );
        registerThrowableProjectile(pSchema, map, "ThrownExpBottle");
        pSchema.register(map, "ItemFrame", p_18284_ -> DSL.optionalFields("Item", References.ITEM_STACK.in(pSchema)));
        registerThrowableProjectile(pSchema, map, "WitherSkull");
        pSchema.registerSimple(map, "PrimedTnt");
        pSchema.register(
            map,
            "FallingSand",
            p_18279_ -> DSL.optionalFields("Block", References.BLOCK_NAME.in(pSchema), "TileEntityData", References.BLOCK_ENTITY.in(pSchema))
        );
        pSchema.register(map, "FireworksRocketEntity", p_18274_ -> DSL.optionalFields("FireworksItem", References.ITEM_STACK.in(pSchema)));
        pSchema.registerSimple(map, "Boat");
        pSchema.register(
            map, "Minecart", () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
        );
        registerMinecart(pSchema, map, "MinecartRideable");
        pSchema.register(
            map,
            "MinecartChest",
            p_18269_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
        );
        registerMinecart(pSchema, map, "MinecartFurnace");
        registerMinecart(pSchema, map, "MinecartTNT");
        pSchema.register(
            map, "MinecartSpawner", () -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema), References.UNTAGGED_SPAWNER.in(pSchema))
        );
        pSchema.register(
            map,
            "MinecartHopper",
            p_18264_ -> DSL.optionalFields("DisplayTile", References.BLOCK_NAME.in(pSchema), "Items", DSL.list(References.ITEM_STACK.in(pSchema)))
        );
        registerMinecart(pSchema, map, "MinecartCommandBlock");
        registerMob(pSchema, map, "ArmorStand");
        registerMob(pSchema, map, "Creeper");
        registerMob(pSchema, map, "Skeleton");
        registerMob(pSchema, map, "Spider");
        registerMob(pSchema, map, "Giant");
        registerMob(pSchema, map, "Zombie");
        registerMob(pSchema, map, "Slime");
        registerMob(pSchema, map, "Ghast");
        registerMob(pSchema, map, "PigZombie");
        pSchema.register(map, "Enderman", p_18259_ -> DSL.optionalFields("carried", References.BLOCK_NAME.in(pSchema), equipment(pSchema)));
        registerMob(pSchema, map, "CaveSpider");
        registerMob(pSchema, map, "Silverfish");
        registerMob(pSchema, map, "Blaze");
        registerMob(pSchema, map, "LavaSlime");
        registerMob(pSchema, map, "EnderDragon");
        registerMob(pSchema, map, "WitherBoss");
        registerMob(pSchema, map, "Bat");
        registerMob(pSchema, map, "Witch");
        registerMob(pSchema, map, "Endermite");
        registerMob(pSchema, map, "Guardian");
        registerMob(pSchema, map, "Pig");
        registerMob(pSchema, map, "Sheep");
        registerMob(pSchema, map, "Cow");
        registerMob(pSchema, map, "Chicken");
        registerMob(pSchema, map, "Squid");
        registerMob(pSchema, map, "Wolf");
        registerMob(pSchema, map, "MushroomCow");
        registerMob(pSchema, map, "SnowMan");
        registerMob(pSchema, map, "Ozelot");
        registerMob(pSchema, map, "VillagerGolem");
        pSchema.register(
            map,
            "EntityHorse",
            p_18254_ -> DSL.optionalFields(
                    "Items",
                    DSL.list(References.ITEM_STACK.in(pSchema)),
                    "ArmorItem",
                    References.ITEM_STACK.in(pSchema),
                    "SaddleItem",
                    References.ITEM_STACK.in(pSchema),
                    equipment(pSchema)
                )
        );
        registerMob(pSchema, map, "Rabbit");
        pSchema.register(
            map,
            "Villager",
            p_340701_ -> DSL.optionalFields(
                    "Inventory",
                    DSL.list(References.ITEM_STACK.in(pSchema)),
                    "Offers",
                    DSL.optionalFields("Recipes", DSL.list(References.VILLAGER_TRADE.in(pSchema))),
                    equipment(pSchema)
                )
        );
        pSchema.registerSimple(map, "EnderCrystal");
        pSchema.register(map, "AreaEffectCloud", p_340704_ -> DSL.optionalFields("Particle", References.PARTICLE.in(pSchema)));
        pSchema.registerSimple(map, "ShulkerBullet");
        pSchema.registerSimple(map, "DragonFireball");
        registerMob(pSchema, map, "Shulker");
        return map;
    }

    @Override
    public Map<String, Supplier<TypeTemplate>> registerBlockEntities(Schema pSchema) {
        Map<String, Supplier<TypeTemplate>> map = Maps.newHashMap();
        registerInventory(pSchema, map, "Furnace");
        registerInventory(pSchema, map, "Chest");
        pSchema.registerSimple(map, "EnderChest");
        pSchema.register(map, "RecordPlayer", p_18235_ -> DSL.optionalFields("RecordItem", References.ITEM_STACK.in(pSchema)));
        registerInventory(pSchema, map, "Trap");
        registerInventory(pSchema, map, "Dropper");
        pSchema.registerSimple(map, "Sign");
        pSchema.register(map, "MobSpawner", p_18223_ -> References.UNTAGGED_SPAWNER.in(pSchema));
        pSchema.registerSimple(map, "Music");
        pSchema.registerSimple(map, "Piston");
        registerInventory(pSchema, map, "Cauldron");
        pSchema.registerSimple(map, "EnchantTable");
        pSchema.registerSimple(map, "Airportal");
        pSchema.registerSimple(map, "Control");
        pSchema.registerSimple(map, "Beacon");
        pSchema.registerSimple(map, "Skull");
        pSchema.registerSimple(map, "DLDetector");
        registerInventory(pSchema, map, "Hopper");
        pSchema.registerSimple(map, "Comparator");
        pSchema.register(map, "FlowerPot", p_18192_ -> DSL.optionalFields("Item", DSL.or(DSL.constType(DSL.intType()), References.ITEM_NAME.in(pSchema))));
        pSchema.registerSimple(map, "Banner");
        pSchema.registerSimple(map, "Structure");
        pSchema.registerSimple(map, "EndGateway");
        return map;
    }

    @Override
    public void registerTypes(Schema pSchema, Map<String, Supplier<TypeTemplate>> pEntityTypes, Map<String, Supplier<TypeTemplate>> pBlockEntityTypes) {
        pSchema.registerType(false, References.LEVEL, DSL::remainder);
        pSchema.registerType(
            false,
            References.PLAYER,
            () -> DSL.optionalFields("Inventory", DSL.list(References.ITEM_STACK.in(pSchema)), "EnderItems", DSL.list(References.ITEM_STACK.in(pSchema)))
        );
        pSchema.registerType(
            false,
            References.CHUNK,
            () -> DSL.fields(
                    "Level",
                    DSL.optionalFields(
                        "Entities",
                        DSL.list(References.ENTITY_TREE.in(pSchema)),
                        "TileEntities",
                        DSL.list(DSL.or(References.BLOCK_ENTITY.in(pSchema), DSL.remainder())),
                        "TileTicks",
                        DSL.list(DSL.fields("i", References.BLOCK_NAME.in(pSchema)))
                    )
                )
        );
        pSchema.registerType(
            true,
            References.BLOCK_ENTITY,
            () -> DSL.optionalFields("components", References.DATA_COMPONENTS.in(pSchema), DSL.taggedChoiceLazy("id", DSL.string(), pBlockEntityTypes))
        );
        pSchema.registerType(
            true, References.ENTITY_TREE, () -> DSL.optionalFields("Riding", References.ENTITY_TREE.in(pSchema), References.ENTITY.in(pSchema))
        );
        pSchema.registerType(false, References.ENTITY_NAME, () -> DSL.constType(NamespacedSchema.namespacedString()));
        pSchema.registerType(true, References.ENTITY, () -> DSL.taggedChoiceLazy("id", DSL.string(), pEntityTypes));
        pSchema.registerType(
            true,
            References.ITEM_STACK,
            () -> DSL.hook(
                    DSL.optionalFields(
                        "id",
                        DSL.or(DSL.constType(DSL.intType()), References.ITEM_NAME.in(pSchema)),
                        "tag",
                        DSL.optionalFields(
                            Pair.of("EntityTag", References.ENTITY_TREE.in(pSchema)),
                            Pair.of("BlockEntityTag", References.BLOCK_ENTITY.in(pSchema)),
                            Pair.of("CanDestroy", DSL.list(References.BLOCK_NAME.in(pSchema))),
                            Pair.of("CanPlaceOn", DSL.list(References.BLOCK_NAME.in(pSchema))),
                            Pair.of("Items", DSL.list(References.ITEM_STACK.in(pSchema))),
                            Pair.of("ChargedProjectiles", DSL.list(References.ITEM_STACK.in(pSchema)))
                        )
                    ),
                    ADD_NAMES,
                    HookFunction.IDENTITY
                )
        );
        pSchema.registerType(false, References.OPTIONS, DSL::remainder);
        pSchema.registerType(false, References.BLOCK_NAME, () -> DSL.or(DSL.constType(DSL.intType()), DSL.constType(NamespacedSchema.namespacedString())));
        pSchema.registerType(false, References.ITEM_NAME, () -> DSL.constType(NamespacedSchema.namespacedString()));
        pSchema.registerType(false, References.STATS, DSL::remainder);
        pSchema.registerType(false, References.SAVED_DATA_COMMAND_STORAGE, DSL::remainder);
        pSchema.registerType(false, References.SAVED_DATA_FORCED_CHUNKS, DSL::remainder);
        pSchema.registerType(false, References.SAVED_DATA_MAP_DATA, DSL::remainder);
        pSchema.registerType(false, References.SAVED_DATA_MAP_INDEX, DSL::remainder);
        pSchema.registerType(false, References.SAVED_DATA_RAIDS, DSL::remainder);
        pSchema.registerType(false, References.SAVED_DATA_RANDOM_SEQUENCES, DSL::remainder);
        pSchema.registerType(
            false,
            References.SAVED_DATA_SCOREBOARD,
            () -> DSL.optionalFields(
                    "data", DSL.optionalFields("Objectives", DSL.list(References.OBJECTIVE.in(pSchema)), "Teams", DSL.list(References.TEAM.in(pSchema)))
                )
        );
        pSchema.registerType(
            false,
            References.SAVED_DATA_STRUCTURE_FEATURE_INDICES,
            () -> DSL.optionalFields("data", DSL.optionalFields("Features", DSL.compoundList(References.STRUCTURE_FEATURE.in(pSchema))))
        );
        pSchema.registerType(false, References.STRUCTURE_FEATURE, DSL::remainder);
        pSchema.registerType(false, References.OBJECTIVE, DSL::remainder);
        pSchema.registerType(false, References.TEAM, DSL::remainder);
        pSchema.registerType(true, References.UNTAGGED_SPAWNER, DSL::remainder);
        pSchema.registerType(false, References.POI_CHUNK, DSL::remainder);
        pSchema.registerType(false, References.WORLD_GEN_SETTINGS, DSL::remainder);
        pSchema.registerType(false, References.ENTITY_CHUNK, () -> DSL.optionalFields("Entities", DSL.list(References.ENTITY_TREE.in(pSchema))));
        pSchema.registerType(true, References.DATA_COMPONENTS, DSL::remainder);
        pSchema.registerType(
            true,
            References.VILLAGER_TRADE,
            () -> DSL.optionalFields(
                    "buy", References.ITEM_STACK.in(pSchema), "buyB", References.ITEM_STACK.in(pSchema), "sell", References.ITEM_STACK.in(pSchema)
                )
        );
        pSchema.registerType(true, References.PARTICLE, () -> DSL.constType(DSL.string()));
    }

    protected static <T> T addNames(Dynamic<T> pTag, Map<String, String> pBlockEntityRenames, Map<String, String> pEntityRenames) {
        return pTag.update("tag", p_145917_ -> p_145917_.update("BlockEntityTag", p_145912_ -> {
                String s = pTag.get("id").asString().result().map(NamespacedSchema::ensureNamespaced).orElse("minecraft:air");
                if (!"minecraft:air".equals(s)) {
                    String s1 = pBlockEntityRenames.get(s);
                    if (s1 != null) {
                        return p_145912_.set("id", pTag.createString(s1));
                    }

                    LOGGER.warn("Unable to resolve BlockEntity for ItemStack: {}", s);
                }

                return p_145912_;
            }).update("EntityTag", p_347072_ -> {
                if (p_347072_.get("id").result().isPresent()) {
                    return p_347072_;
                } else {
                    String s = NamespacedSchema.ensureNamespaced(pTag.get("id").asString(""));
                    String s1 = pEntityRenames.get(s);
                    return s1 != null ? p_347072_.set("id", pTag.createString(s1)) : p_347072_;
                }
            })).getValue();
    }
}

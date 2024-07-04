package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.OpticFinder;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.DSL.TypeReference;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import com.mojang.serialization.Dynamic;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.util.datafix.ExtraDataFixUtils;

public class BlockPosFormatAndRenamesFix extends DataFix {
    private static final List<String> PATROLLING_MOBS = List.of(
        "minecraft:witch", "minecraft:ravager", "minecraft:pillager", "minecraft:illusioner", "minecraft:evoker", "minecraft:vindicator"
    );

    public BlockPosFormatAndRenamesFix(Schema pOutputSchema) {
        super(pOutputSchema, false);
    }

    private Typed<?> fixFields(Typed<?> pData, Map<String, String> pRenames) {
        return pData.update(DSL.remainderFinder(), p_337600_ -> {
            for (Entry<String, String> entry : pRenames.entrySet()) {
                p_337600_ = p_337600_.renameAndFixField(entry.getKey(), entry.getValue(), ExtraDataFixUtils::fixBlockPos);
            }

            return p_337600_;
        });
    }

    private <T> Dynamic<T> fixMapSavedData(Dynamic<T> p_326466_) {
        return p_326466_.update("frames", p_326158_ -> p_326158_.createList(p_326158_.asStream().map(p_337598_ -> {
                p_337598_ = p_337598_.renameAndFixField("Pos", "pos", ExtraDataFixUtils::fixBlockPos);
                p_337598_ = p_337598_.renameField("Rotation", "rotation");
                return p_337598_.renameField("EntityId", "entity_id");
            }))).update("banners", p_326387_ -> p_326387_.createList(p_326387_.asStream().map(p_337601_ -> {
                p_337601_ = p_337601_.renameField("Pos", "pos");
                p_337601_ = p_337601_.renameField("Color", "color");
                return p_337601_.renameField("Name", "name");
            })));
    }

    @Override
    public TypeRewriteRule makeRule() {
        List<TypeRewriteRule> list = new ArrayList<>();
        this.addEntityRules(list);
        this.addBlockEntityRules(list);
        list.add(
            this.fixTypeEverywhereTyped(
                "BlockPos format for map frames",
                this.getInputSchema().getType(References.SAVED_DATA_MAP_DATA),
                p_326202_ -> p_326202_.update(DSL.remainderFinder(), p_326364_ -> p_326364_.update("data", this::fixMapSavedData))
            )
        );
        Type<?> type = this.getInputSchema().getType(References.ITEM_STACK);
        list.add(
            this.fixTypeEverywhereTyped(
                "BlockPos format for compass target",
                type,
                ItemStackTagFix.createFixer(type, "minecraft:compass"::equals, p_326048_ -> p_326048_.update("LodestonePos", ExtraDataFixUtils::fixBlockPos))
            )
        );
        return TypeRewriteRule.seq(list);
    }

    private void addEntityRules(List<TypeRewriteRule> pOutput) {
        pOutput.add(this.createEntityFixer(References.ENTITY, "minecraft:bee", Map.of("HivePos", "hive_pos", "FlowerPos", "flower_pos")));
        pOutput.add(this.createEntityFixer(References.ENTITY, "minecraft:end_crystal", Map.of("BeamTarget", "beam_target")));
        pOutput.add(this.createEntityFixer(References.ENTITY, "minecraft:wandering_trader", Map.of("WanderTarget", "wander_target")));

        for (String s : PATROLLING_MOBS) {
            pOutput.add(this.createEntityFixer(References.ENTITY, s, Map.of("PatrolTarget", "patrol_target")));
        }

        pOutput.add(
            this.fixTypeEverywhereTyped(
                "BlockPos format in Leash for mobs",
                this.getInputSchema().getType(References.ENTITY),
                p_326408_ -> p_326408_.update(DSL.remainderFinder(), p_337602_ -> p_337602_.renameAndFixField("Leash", "leash", ExtraDataFixUtils::fixBlockPos))
            )
        );
    }

    private void addBlockEntityRules(List<TypeRewriteRule> pOutput) {
        pOutput.add(this.createEntityFixer(References.BLOCK_ENTITY, "minecraft:beehive", Map.of("FlowerPos", "flower_pos")));
        pOutput.add(this.createEntityFixer(References.BLOCK_ENTITY, "minecraft:end_gateway", Map.of("ExitPortal", "exit_portal")));
    }

    private TypeRewriteRule createEntityFixer(TypeReference pReference, String pEntityId, Map<String, String> pRenames) {
        String s = "BlockPos format in " + pRenames.keySet() + " for " + pEntityId + " (" + pReference.typeName() + ")";
        OpticFinder<?> opticfinder = DSL.namedChoice(pEntityId, this.getInputSchema().getChoiceType(pReference, pEntityId));
        return this.fixTypeEverywhereTyped(
            s, this.getInputSchema().getType(pReference), p_325999_ -> p_325999_.updateTyped(opticfinder, p_326318_ -> this.fixFields(p_326318_, pRenames))
        );
    }
}

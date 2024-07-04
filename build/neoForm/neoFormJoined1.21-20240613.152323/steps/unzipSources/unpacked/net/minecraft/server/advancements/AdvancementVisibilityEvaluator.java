package net.minecraft.server.advancements;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.DisplayInfo;

public class AdvancementVisibilityEvaluator {
    private static final int VISIBILITY_DEPTH = 2;

    private static AdvancementVisibilityEvaluator.VisibilityRule evaluateVisibilityRule(Advancement pAdvancement, boolean pAlwaysShow) {
        Optional<DisplayInfo> optional = pAdvancement.display();
        if (optional.isEmpty()) {
            return AdvancementVisibilityEvaluator.VisibilityRule.HIDE;
        } else if (pAlwaysShow) {
            return AdvancementVisibilityEvaluator.VisibilityRule.SHOW;
        } else {
            return optional.get().isHidden() ? AdvancementVisibilityEvaluator.VisibilityRule.HIDE : AdvancementVisibilityEvaluator.VisibilityRule.NO_CHANGE;
        }
    }

    private static boolean evaluateVisiblityForUnfinishedNode(Stack<AdvancementVisibilityEvaluator.VisibilityRule> pVisibilityRules) {
        for (int i = 0; i <= 2; i++) {
            AdvancementVisibilityEvaluator.VisibilityRule advancementvisibilityevaluator$visibilityrule = pVisibilityRules.peek(i);
            if (advancementvisibilityevaluator$visibilityrule == AdvancementVisibilityEvaluator.VisibilityRule.SHOW) {
                return true;
            }

            if (advancementvisibilityevaluator$visibilityrule == AdvancementVisibilityEvaluator.VisibilityRule.HIDE) {
                return false;
            }
        }

        return false;
    }

    private static boolean evaluateVisibility(
        AdvancementNode pAdvancement,
        Stack<AdvancementVisibilityEvaluator.VisibilityRule> pVisibilityRules,
        Predicate<AdvancementNode> pPredicate,
        AdvancementVisibilityEvaluator.Output pOutput
    ) {
        boolean flag = pPredicate.test(pAdvancement);
        AdvancementVisibilityEvaluator.VisibilityRule advancementvisibilityevaluator$visibilityrule = evaluateVisibilityRule(pAdvancement.advancement(), flag);
        boolean flag1 = flag;
        pVisibilityRules.push(advancementvisibilityevaluator$visibilityrule);

        for (AdvancementNode advancementnode : pAdvancement.children()) {
            flag1 |= evaluateVisibility(advancementnode, pVisibilityRules, pPredicate, pOutput);
        }

        boolean flag2 = flag1 || evaluateVisiblityForUnfinishedNode(pVisibilityRules);
        pVisibilityRules.pop();
        pOutput.accept(pAdvancement, flag2);
        return flag1;
    }

    public static void evaluateVisibility(AdvancementNode pAdvancement, Predicate<AdvancementNode> pPredicate, AdvancementVisibilityEvaluator.Output pOutput) {
        AdvancementNode advancementnode = pAdvancement.root();
        Stack<AdvancementVisibilityEvaluator.VisibilityRule> stack = new ObjectArrayList<>();

        for (int i = 0; i <= 2; i++) {
            stack.push(AdvancementVisibilityEvaluator.VisibilityRule.NO_CHANGE);
        }

        evaluateVisibility(advancementnode, stack, pPredicate, pOutput);
    }

    public static boolean isVisible(AdvancementNode advancement, Predicate<AdvancementNode> test) {
        Stack<AdvancementVisibilityEvaluator.VisibilityRule> stack = new ObjectArrayList<>();

        for(int i = 0; i <= 2; ++i) {
            stack.push(AdvancementVisibilityEvaluator.VisibilityRule.NO_CHANGE);
        }
        return evaluateVisibility(advancement.root(), stack, test, (adv, visible) -> {});
    }

    @FunctionalInterface
    public interface Output {
        void accept(AdvancementNode pAdvancement, boolean pVisible);
    }

    static enum VisibilityRule {
        SHOW,
        HIDE,
        NO_CHANGE;
    }
}

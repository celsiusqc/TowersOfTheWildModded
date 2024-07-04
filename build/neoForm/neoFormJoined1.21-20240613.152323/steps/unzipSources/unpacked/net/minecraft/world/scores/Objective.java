package net.minecraft.world.scores;

import java.util.Objects;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public class Objective {
    private final Scoreboard scoreboard;
    private final String name;
    private final ObjectiveCriteria criteria;
    private Component displayName;
    private Component formattedDisplayName;
    private ObjectiveCriteria.RenderType renderType;
    private boolean displayAutoUpdate;
    @Nullable
    private NumberFormat numberFormat;

    public Objective(
        Scoreboard pScoreboard,
        String pName,
        ObjectiveCriteria pCriteria,
        Component pDisplayName,
        ObjectiveCriteria.RenderType pRenderType,
        boolean pDisplayAutoUpdate,
        @Nullable NumberFormat pNumberFormat
    ) {
        this.scoreboard = pScoreboard;
        this.name = pName;
        this.criteria = pCriteria;
        this.displayName = pDisplayName;
        this.formattedDisplayName = this.createFormattedDisplayName();
        this.renderType = pRenderType;
        this.displayAutoUpdate = pDisplayAutoUpdate;
        this.numberFormat = pNumberFormat;
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    public String getName() {
        return this.name;
    }

    public ObjectiveCriteria getCriteria() {
        return this.criteria;
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public boolean displayAutoUpdate() {
        return this.displayAutoUpdate;
    }

    @Nullable
    public NumberFormat numberFormat() {
        return this.numberFormat;
    }

    public NumberFormat numberFormatOrDefault(NumberFormat pDefaultValue) {
        return Objects.requireNonNullElse(this.numberFormat, pDefaultValue);
    }

    private Component createFormattedDisplayName() {
        return ComponentUtils.wrapInSquareBrackets(
            this.displayName.copy().withStyle(p_83319_ -> p_83319_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(this.name))))
        );
    }

    public Component getFormattedDisplayName() {
        return this.formattedDisplayName;
    }

    public void setDisplayName(Component pDisplayName) {
        this.displayName = pDisplayName;
        this.formattedDisplayName = this.createFormattedDisplayName();
        this.scoreboard.onObjectiveChanged(this);
    }

    public ObjectiveCriteria.RenderType getRenderType() {
        return this.renderType;
    }

    public void setRenderType(ObjectiveCriteria.RenderType pRenderType) {
        this.renderType = pRenderType;
        this.scoreboard.onObjectiveChanged(this);
    }

    public void setDisplayAutoUpdate(boolean pDisplayAutoUpdate) {
        this.displayAutoUpdate = pDisplayAutoUpdate;
        this.scoreboard.onObjectiveChanged(this);
    }

    public void setNumberFormat(@Nullable NumberFormat pNumberFormat) {
        this.numberFormat = pNumberFormat;
        this.scoreboard.onObjectiveChanged(this);
    }
}

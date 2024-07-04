package net.minecraft.world.scores;

import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

public class PlayerTeam extends Team {
    private static final int BIT_FRIENDLY_FIRE = 0;
    private static final int BIT_SEE_INVISIBLES = 1;
    private final Scoreboard scoreboard;
    private final String name;
    private final Set<String> players = Sets.newHashSet();
    private Component displayName;
    private Component playerPrefix = CommonComponents.EMPTY;
    private Component playerSuffix = CommonComponents.EMPTY;
    private boolean allowFriendlyFire = true;
    private boolean seeFriendlyInvisibles = true;
    private Team.Visibility nameTagVisibility = Team.Visibility.ALWAYS;
    private Team.Visibility deathMessageVisibility = Team.Visibility.ALWAYS;
    private ChatFormatting color = ChatFormatting.RESET;
    private Team.CollisionRule collisionRule = Team.CollisionRule.ALWAYS;
    private final Style displayNameStyle;

    public PlayerTeam(Scoreboard pScoreboard, String pName) {
        this.scoreboard = pScoreboard;
        this.name = pName;
        this.displayName = Component.literal(pName);
        this.displayNameStyle = Style.EMPTY.withInsertion(pName).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(pName)));
    }

    public Scoreboard getScoreboard() {
        return this.scoreboard;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public MutableComponent getFormattedDisplayName() {
        MutableComponent mutablecomponent = ComponentUtils.wrapInSquareBrackets(this.displayName.copy().withStyle(this.displayNameStyle));
        ChatFormatting chatformatting = this.getColor();
        if (chatformatting != ChatFormatting.RESET) {
            mutablecomponent.withStyle(chatformatting);
        }

        return mutablecomponent;
    }

    /**
     * Sets the display name for this team.
     */
    public void setDisplayName(Component pName) {
        if (pName == null) {
            throw new IllegalArgumentException("Name cannot be null");
        } else {
            this.displayName = pName;
            this.scoreboard.onTeamChanged(this);
        }
    }

    public void setPlayerPrefix(@Nullable Component pPlayerPrefix) {
        this.playerPrefix = pPlayerPrefix == null ? CommonComponents.EMPTY : pPlayerPrefix;
        this.scoreboard.onTeamChanged(this);
    }

    public Component getPlayerPrefix() {
        return this.playerPrefix;
    }

    public void setPlayerSuffix(@Nullable Component pPlayerSuffix) {
        this.playerSuffix = pPlayerSuffix == null ? CommonComponents.EMPTY : pPlayerSuffix;
        this.scoreboard.onTeamChanged(this);
    }

    public Component getPlayerSuffix() {
        return this.playerSuffix;
    }

    @Override
    public Collection<String> getPlayers() {
        return this.players;
    }

    @Override
    public MutableComponent getFormattedName(Component pFormattedName) {
        MutableComponent mutablecomponent = Component.empty().append(this.playerPrefix).append(pFormattedName).append(this.playerSuffix);
        ChatFormatting chatformatting = this.getColor();
        if (chatformatting != ChatFormatting.RESET) {
            mutablecomponent.withStyle(chatformatting);
        }

        return mutablecomponent;
    }

    public static MutableComponent formatNameForTeam(@Nullable Team pPlayerTeam, Component pPlayerName) {
        return pPlayerTeam == null ? pPlayerName.copy() : pPlayerTeam.getFormattedName(pPlayerName);
    }

    @Override
    public boolean isAllowFriendlyFire() {
        return this.allowFriendlyFire;
    }

    /**
     * Sets whether friendly fire (PVP between members of the team) is allowed.
     */
    public void setAllowFriendlyFire(boolean pFriendlyFire) {
        this.allowFriendlyFire = pFriendlyFire;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public boolean canSeeFriendlyInvisibles() {
        return this.seeFriendlyInvisibles;
    }

    /**
     * Sets whether members of this team can see other members that are invisible.
     */
    public void setSeeFriendlyInvisibles(boolean pFriendlyInvisibles) {
        this.seeFriendlyInvisibles = pFriendlyInvisibles;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public Team.Visibility getNameTagVisibility() {
        return this.nameTagVisibility;
    }

    @Override
    public Team.Visibility getDeathMessageVisibility() {
        return this.deathMessageVisibility;
    }

    /**
     * Sets the visibility flags for player name tags.
     */
    public void setNameTagVisibility(Team.Visibility pVisibility) {
        this.nameTagVisibility = pVisibility;
        this.scoreboard.onTeamChanged(this);
    }

    /**
     * Sets the visibility flags for player death messages.
     */
    public void setDeathMessageVisibility(Team.Visibility pVisibility) {
        this.deathMessageVisibility = pVisibility;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public Team.CollisionRule getCollisionRule() {
        return this.collisionRule;
    }

    /**
     * Sets the rule to be used for handling collisions with members of this team.
     */
    public void setCollisionRule(Team.CollisionRule pRule) {
        this.collisionRule = pRule;
        this.scoreboard.onTeamChanged(this);
    }

    public int packOptions() {
        int i = 0;
        if (this.isAllowFriendlyFire()) {
            i |= 1;
        }

        if (this.canSeeFriendlyInvisibles()) {
            i |= 2;
        }

        return i;
    }

    /**
     * Sets friendly fire and invisibles flags based off of the given bitmask.
     */
    public void unpackOptions(int pFlags) {
        this.setAllowFriendlyFire((pFlags & 1) > 0);
        this.setSeeFriendlyInvisibles((pFlags & 2) > 0);
    }

    /**
     * Sets the color for this team. The team color is used mainly for team kill objectives and team-specific setDisplay usage. It does _not_ affect all situations (for instance, the prefix is used for the glowing effect).
     */
    public void setColor(ChatFormatting pColor) {
        this.color = pColor;
        this.scoreboard.onTeamChanged(this);
    }

    @Override
    public ChatFormatting getColor() {
        return this.color;
    }
}

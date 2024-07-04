package net.minecraft.server;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundResetScorePacket;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket;
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket;
import net.minecraft.network.protocol.game.ClientboundSetScorePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.ScoreboardSaveData;

public class ServerScoreboard extends Scoreboard {
    private final MinecraftServer server;
    private final Set<Objective> trackedObjectives = Sets.newHashSet();
    private final List<Runnable> dirtyListeners = Lists.newArrayList();

    public ServerScoreboard(MinecraftServer pServer) {
        this.server = pServer;
    }

    @Override
    protected void onScoreChanged(ScoreHolder pScoreHolder, Objective pObjective, Score pScore) {
        super.onScoreChanged(pScoreHolder, pObjective, pScore);
        if (this.trackedObjectives.contains(pObjective)) {
            this.server
                .getPlayerList()
                .broadcastAll(
                    new ClientboundSetScorePacket(
                        pScoreHolder.getScoreboardName(),
                        pObjective.getName(),
                        pScore.value(),
                        Optional.ofNullable(pScore.display()),
                        Optional.ofNullable(pScore.numberFormat())
                    )
                );
        }

        this.setDirty();
    }

    @Override
    protected void onScoreLockChanged(ScoreHolder pScoreHolder, Objective pObjective) {
        super.onScoreLockChanged(pScoreHolder, pObjective);
        this.setDirty();
    }

    @Override
    public void onPlayerRemoved(ScoreHolder pScoreHolder) {
        super.onPlayerRemoved(pScoreHolder);
        this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(pScoreHolder.getScoreboardName(), null));
        this.setDirty();
    }

    @Override
    public void onPlayerScoreRemoved(ScoreHolder pScoreHolder, Objective pObjective) {
        super.onPlayerScoreRemoved(pScoreHolder, pObjective);
        if (this.trackedObjectives.contains(pObjective)) {
            this.server.getPlayerList().broadcastAll(new ClientboundResetScorePacket(pScoreHolder.getScoreboardName(), pObjective.getName()));
        }

        this.setDirty();
    }

    @Override
    public void setDisplayObjective(DisplaySlot pSlot, @Nullable Objective pObjective) {
        Objective objective = this.getDisplayObjective(pSlot);
        super.setDisplayObjective(pSlot, pObjective);
        if (objective != pObjective && objective != null) {
            if (this.getObjectiveDisplaySlotCount(objective) > 0) {
                this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(pSlot, pObjective));
            } else {
                this.stopTrackingObjective(objective);
            }
        }

        if (pObjective != null) {
            if (this.trackedObjectives.contains(pObjective)) {
                this.server.getPlayerList().broadcastAll(new ClientboundSetDisplayObjectivePacket(pSlot, pObjective));
            } else {
                this.startTrackingObjective(pObjective);
            }
        }

        this.setDirty();
    }

    @Override
    public boolean addPlayerToTeam(String pPlayerName, PlayerTeam pTeam) {
        if (super.addPlayerToTeam(pPlayerName, pTeam)) {
            this.server
                .getPlayerList()
                .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(pTeam, pPlayerName, ClientboundSetPlayerTeamPacket.Action.ADD));
            this.setDirty();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes the given username from the given ScorePlayerTeam. If the player is not on the team then an IllegalStateException is thrown.
     */
    @Override
    public void removePlayerFromTeam(String pUsername, PlayerTeam pPlayerTeam) {
        super.removePlayerFromTeam(pUsername, pPlayerTeam);
        this.server
            .getPlayerList()
            .broadcastAll(ClientboundSetPlayerTeamPacket.createPlayerPacket(pPlayerTeam, pUsername, ClientboundSetPlayerTeamPacket.Action.REMOVE));
        this.setDirty();
    }

    @Override
    public void onObjectiveAdded(Objective pObjective) {
        super.onObjectiveAdded(pObjective);
        this.setDirty();
    }

    @Override
    public void onObjectiveChanged(Objective pObjective) {
        super.onObjectiveChanged(pObjective);
        if (this.trackedObjectives.contains(pObjective)) {
            this.server.getPlayerList().broadcastAll(new ClientboundSetObjectivePacket(pObjective, 2));
        }

        this.setDirty();
    }

    @Override
    public void onObjectiveRemoved(Objective pObjective) {
        super.onObjectiveRemoved(pObjective);
        if (this.trackedObjectives.contains(pObjective)) {
            this.stopTrackingObjective(pObjective);
        }

        this.setDirty();
    }

    @Override
    public void onTeamAdded(PlayerTeam pPlayerTeam) {
        super.onTeamAdded(pPlayerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(pPlayerTeam, true));
        this.setDirty();
    }

    @Override
    public void onTeamChanged(PlayerTeam pPlayerTeam) {
        super.onTeamChanged(pPlayerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(pPlayerTeam, false));
        this.setDirty();
    }

    @Override
    public void onTeamRemoved(PlayerTeam pPlayerTeam) {
        super.onTeamRemoved(pPlayerTeam);
        this.server.getPlayerList().broadcastAll(ClientboundSetPlayerTeamPacket.createRemovePacket(pPlayerTeam));
        this.setDirty();
    }

    public void addDirtyListener(Runnable pRunnable) {
        this.dirtyListeners.add(pRunnable);
    }

    protected void setDirty() {
        for (Runnable runnable : this.dirtyListeners) {
            runnable.run();
        }
    }

    public List<Packet<?>> getStartTrackingPackets(Objective pObjective) {
        List<Packet<?>> list = Lists.newArrayList();
        list.add(new ClientboundSetObjectivePacket(pObjective, 0));

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == pObjective) {
                list.add(new ClientboundSetDisplayObjectivePacket(displayslot, pObjective));
            }
        }

        for (PlayerScoreEntry playerscoreentry : this.listPlayerScores(pObjective)) {
            list.add(
                new ClientboundSetScorePacket(
                    playerscoreentry.owner(),
                    pObjective.getName(),
                    playerscoreentry.value(),
                    Optional.ofNullable(playerscoreentry.display()),
                    Optional.ofNullable(playerscoreentry.numberFormatOverride())
                )
            );
        }

        return list;
    }

    public void startTrackingObjective(Objective pObjective) {
        List<Packet<?>> list = this.getStartTrackingPackets(pObjective);

        for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            for (Packet<?> packet : list) {
                serverplayer.connection.send(packet);
            }
        }

        this.trackedObjectives.add(pObjective);
    }

    public List<Packet<?>> getStopTrackingPackets(Objective pObjective) {
        List<Packet<?>> list = Lists.newArrayList();
        list.add(new ClientboundSetObjectivePacket(pObjective, 1));

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == pObjective) {
                list.add(new ClientboundSetDisplayObjectivePacket(displayslot, pObjective));
            }
        }

        return list;
    }

    public void stopTrackingObjective(Objective pObjective) {
        List<Packet<?>> list = this.getStopTrackingPackets(pObjective);

        for (ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            for (Packet<?> packet : list) {
                serverplayer.connection.send(packet);
            }
        }

        this.trackedObjectives.remove(pObjective);
    }

    public int getObjectiveDisplaySlotCount(Objective pObjective) {
        int i = 0;

        for (DisplaySlot displayslot : DisplaySlot.values()) {
            if (this.getDisplayObjective(displayslot) == pObjective) {
                i++;
            }
        }

        return i;
    }

    public SavedData.Factory<ScoreboardSaveData> dataFactory() {
        return new SavedData.Factory<>(this::createData, this::createData, DataFixTypes.SAVED_DATA_SCOREBOARD);
    }

    private ScoreboardSaveData createData() {
        ScoreboardSaveData scoreboardsavedata = new ScoreboardSaveData(this);
        this.addDirtyListener(scoreboardsavedata::setDirty);
        return scoreboardsavedata;
    }

    private ScoreboardSaveData createData(CompoundTag p_180014_, HolderLookup.Provider p_324580_) {
        return this.createData().load(p_180014_, p_324580_);
    }

    public static enum Method {
        CHANGE,
        REMOVE;
    }
}

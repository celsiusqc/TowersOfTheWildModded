package net.minecraft.client.gui.spectator.categories;

import com.mojang.authlib.GameProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuCategory;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TeleportToTeamMenuCategory implements SpectatorMenuCategory, SpectatorMenuItem {
    private static final ResourceLocation TELEPORT_TO_TEAM_SPRITE = ResourceLocation.withDefaultNamespace("spectator/teleport_to_team");
    private static final Component TELEPORT_TEXT = Component.translatable("spectatorMenu.team_teleport");
    private static final Component TELEPORT_PROMPT = Component.translatable("spectatorMenu.team_teleport.prompt");
    private final List<SpectatorMenuItem> items;

    public TeleportToTeamMenuCategory() {
        Minecraft minecraft = Minecraft.getInstance();
        this.items = createTeamEntries(minecraft, minecraft.level.getScoreboard());
    }

    private static List<SpectatorMenuItem> createTeamEntries(Minecraft pMinecraft, Scoreboard pScoreboard) {
        return pScoreboard.getPlayerTeams()
            .stream()
            .flatMap(p_260025_ -> TeleportToTeamMenuCategory.TeamSelectionItem.create(pMinecraft, p_260025_).stream())
            .toList();
    }

    @Override
    public List<SpectatorMenuItem> getItems() {
        return this.items;
    }

    @Override
    public Component getPrompt() {
        return TELEPORT_PROMPT;
    }

    @Override
    public void selectItem(SpectatorMenu pMenu) {
        pMenu.selectCategory(this);
    }

    @Override
    public Component getName() {
        return TELEPORT_TEXT;
    }

    @Override
    public void renderIcon(GuiGraphics pGuiGraphics, float pShadeColor, int pAlpha) {
        pGuiGraphics.blitSprite(TELEPORT_TO_TEAM_SPRITE, 0, 0, 16, 16);
    }

    @Override
    public boolean isEnabled() {
        return !this.items.isEmpty();
    }

    @OnlyIn(Dist.CLIENT)
    static class TeamSelectionItem implements SpectatorMenuItem {
        private final PlayerTeam team;
        private final Supplier<PlayerSkin> iconSkin;
        private final List<PlayerInfo> players;

        private TeamSelectionItem(PlayerTeam pTeam, List<PlayerInfo> pPlayers, Supplier<PlayerSkin> pIconSkin) {
            this.team = pTeam;
            this.players = pPlayers;
            this.iconSkin = pIconSkin;
        }

        public static Optional<SpectatorMenuItem> create(Minecraft pMinecraft, PlayerTeam pTeam) {
            List<PlayerInfo> list = new ArrayList<>();

            for (String s : pTeam.getPlayers()) {
                PlayerInfo playerinfo = pMinecraft.getConnection().getPlayerInfo(s);
                if (playerinfo != null && playerinfo.getGameMode() != GameType.SPECTATOR) {
                    list.add(playerinfo);
                }
            }

            if (list.isEmpty()) {
                return Optional.empty();
            } else {
                GameProfile gameprofile = list.get(RandomSource.create().nextInt(list.size())).getProfile();
                Supplier<PlayerSkin> supplier = pMinecraft.getSkinManager().lookupInsecure(gameprofile);
                return Optional.of(new TeleportToTeamMenuCategory.TeamSelectionItem(pTeam, list, supplier));
            }
        }

        @Override
        public void selectItem(SpectatorMenu pMenu) {
            pMenu.selectCategory(new TeleportToPlayerMenuCategory(this.players));
        }

        @Override
        public Component getName() {
            return this.team.getDisplayName();
        }

        @Override
        public void renderIcon(GuiGraphics pGuiGraphics, float pShadeColor, int pAlpha) {
            Integer integer = this.team.getColor().getColor();
            if (integer != null) {
                float f = (float)(integer >> 16 & 0xFF) / 255.0F;
                float f1 = (float)(integer >> 8 & 0xFF) / 255.0F;
                float f2 = (float)(integer & 0xFF) / 255.0F;
                pGuiGraphics.fill(1, 1, 15, 15, Mth.color(f * pShadeColor, f1 * pShadeColor, f2 * pShadeColor) | pAlpha << 24);
            }

            pGuiGraphics.setColor(pShadeColor, pShadeColor, pShadeColor, (float)pAlpha / 255.0F);
            PlayerFaceRenderer.draw(pGuiGraphics, this.iconSkin.get(), 2, 2, 12);
            pGuiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }
}

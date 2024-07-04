package net.minecraft.client.gui.components;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Optionull;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerTabOverlay {
    private static final ResourceLocation PING_UNKNOWN_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_unknown");
    private static final ResourceLocation PING_1_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_1");
    private static final ResourceLocation PING_2_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_2");
    private static final ResourceLocation PING_3_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_3");
    private static final ResourceLocation PING_4_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_4");
    private static final ResourceLocation PING_5_SPRITE = ResourceLocation.withDefaultNamespace("icon/ping_5");
    private static final ResourceLocation HEART_CONTAINER_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/container_blinking");
    private static final ResourceLocation HEART_CONTAINER_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/container");
    private static final ResourceLocation HEART_FULL_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/full_blinking");
    private static final ResourceLocation HEART_HALF_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/half_blinking");
    private static final ResourceLocation HEART_ABSORBING_FULL_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/absorbing_full_blinking");
    private static final ResourceLocation HEART_FULL_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/full");
    private static final ResourceLocation HEART_ABSORBING_HALF_BLINKING_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/absorbing_half_blinking");
    private static final ResourceLocation HEART_HALF_SPRITE = ResourceLocation.withDefaultNamespace("hud/heart/half");
    private static final Comparator<PlayerInfo> PLAYER_COMPARATOR = Comparator.<PlayerInfo>comparingInt(
            p_253306_ -> p_253306_.getGameMode() == GameType.SPECTATOR ? 1 : 0
        )
        .thenComparing(p_269613_ -> Optionull.mapOrDefault(p_269613_.getTeam(), PlayerTeam::getName, ""))
        .thenComparing(p_253305_ -> p_253305_.getProfile().getName(), String::compareToIgnoreCase);
    public static final int MAX_ROWS_PER_COL = 20;
    private final Minecraft minecraft;
    private final Gui gui;
    @Nullable
    private Component footer;
    @Nullable
    private Component header;
    /**
     * Weither or not the playerlist is currently being rendered
     */
    private boolean visible;
    private final Map<UUID, PlayerTabOverlay.HealthState> healthStates = new Object2ObjectOpenHashMap<>();

    public PlayerTabOverlay(Minecraft pMinecraft, Gui pGui) {
        this.minecraft = pMinecraft;
        this.gui = pGui;
    }

    public Component getNameForDisplay(PlayerInfo p_94550_) {
        return p_94550_.getTabListDisplayName() != null
            ? this.decorateName(p_94550_, p_94550_.getTabListDisplayName().copy())
            : this.decorateName(p_94550_, PlayerTeam.formatNameForTeam(p_94550_.getTeam(), Component.literal(p_94550_.getProfile().getName())));
    }

    private Component decorateName(PlayerInfo pPlayerInfo, MutableComponent pName) {
        return pPlayerInfo.getGameMode() == GameType.SPECTATOR ? pName.withStyle(ChatFormatting.ITALIC) : pName;
    }

    /**
     * Called by GuiIngame to update the information stored in the playerlist, does not actually render the list, however.
     */
    public void setVisible(boolean pVisible) {
        if (this.visible != pVisible) {
            this.healthStates.clear();
            this.visible = pVisible;
            if (pVisible) {
                Component component = ComponentUtils.formatList(this.getPlayerInfos(), Component.literal(", "), this::getNameForDisplay);
                this.minecraft.getNarrator().sayNow(Component.translatable("multiplayer.player.list.narration", component));
            }
        }
    }

    private List<PlayerInfo> getPlayerInfos() {
        return this.minecraft.player.connection.getListedOnlinePlayers().stream().sorted(PLAYER_COMPARATOR).limit(80L).toList();
    }

    public void render(GuiGraphics pGuiGraphics, int pWidth, Scoreboard pScoreboard, @Nullable Objective pObjective) {
        List<PlayerInfo> list = this.getPlayerInfos();
        List<PlayerTabOverlay.ScoreDisplayEntry> list1 = new ArrayList<>(list.size());
        int i = this.minecraft.font.width(" ");
        int j = 0;
        int k = 0;

        for (PlayerInfo playerinfo : list) {
            Component component = this.getNameForDisplay(playerinfo);
            j = Math.max(j, this.minecraft.font.width(component));
            int l = 0;
            Component component1 = null;
            int i1 = 0;
            if (pObjective != null) {
                ScoreHolder scoreholder = ScoreHolder.fromGameProfile(playerinfo.getProfile());
                ReadOnlyScoreInfo readonlyscoreinfo = pScoreboard.getPlayerScoreInfo(scoreholder, pObjective);
                if (readonlyscoreinfo != null) {
                    l = readonlyscoreinfo.value();
                }

                if (pObjective.getRenderType() != ObjectiveCriteria.RenderType.HEARTS) {
                    NumberFormat numberformat = pObjective.numberFormatOrDefault(StyledFormat.PLAYER_LIST_DEFAULT);
                    component1 = ReadOnlyScoreInfo.safeFormatValue(readonlyscoreinfo, numberformat);
                    i1 = this.minecraft.font.width(component1);
                    k = Math.max(k, i1 > 0 ? i + i1 : 0);
                }
            }

            list1.add(new PlayerTabOverlay.ScoreDisplayEntry(component, l, component1, i1));
        }

        if (!this.healthStates.isEmpty()) {
            Set<UUID> set = list.stream().map(p_250472_ -> p_250472_.getProfile().getId()).collect(Collectors.toSet());
            this.healthStates.keySet().removeIf(p_248583_ -> !set.contains(p_248583_));
        }

        int j2 = list.size();
        int k2 = j2;

        int l2;
        for (l2 = 1; k2 > 20; k2 = (j2 + l2 - 1) / l2) {
            l2++;
        }

        boolean flag2 = this.minecraft.isLocalServer() || this.minecraft.getConnection().getConnection().isEncrypted();
        int i3;
        if (pObjective != null) {
            if (pObjective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
                i3 = 90;
            } else {
                i3 = k;
            }
        } else {
            i3 = 0;
        }

        int j3 = Math.min(l2 * ((flag2 ? 9 : 0) + j + i3 + 13), pWidth - 50) / l2;
        int k3 = pWidth / 2 - (j3 * l2 + (l2 - 1) * 5) / 2;
        int l3 = 10;
        int i4 = j3 * l2 + (l2 - 1) * 5;
        List<FormattedCharSequence> list2 = null;
        if (this.header != null) {
            list2 = this.minecraft.font.split(this.header, pWidth - 50);

            for (FormattedCharSequence formattedcharsequence : list2) {
                i4 = Math.max(i4, this.minecraft.font.width(formattedcharsequence));
            }
        }

        List<FormattedCharSequence> list3 = null;
        if (this.footer != null) {
            list3 = this.minecraft.font.split(this.footer, pWidth - 50);

            for (FormattedCharSequence formattedcharsequence1 : list3) {
                i4 = Math.max(i4, this.minecraft.font.width(formattedcharsequence1));
            }
        }

        if (list2 != null) {
            pGuiGraphics.fill(pWidth / 2 - i4 / 2 - 1, l3 - 1, pWidth / 2 + i4 / 2 + 1, l3 + list2.size() * 9, Integer.MIN_VALUE);

            for (FormattedCharSequence formattedcharsequence2 : list2) {
                int j1 = this.minecraft.font.width(formattedcharsequence2);
                pGuiGraphics.drawString(this.minecraft.font, formattedcharsequence2, pWidth / 2 - j1 / 2, l3, -1);
                l3 += 9;
            }

            l3++;
        }

        pGuiGraphics.fill(pWidth / 2 - i4 / 2 - 1, l3 - 1, pWidth / 2 + i4 / 2 + 1, l3 + k2 * 9, Integer.MIN_VALUE);
        int j4 = this.minecraft.options.getBackgroundColor(553648127);

        for (int k4 = 0; k4 < j2; k4++) {
            int l4 = k4 / k2;
            int k1 = k4 % k2;
            int l1 = k3 + l4 * j3 + l4 * 5;
            int i2 = l3 + k1 * 9;
            pGuiGraphics.fill(l1, i2, l1 + j3, i2 + 8, j4);
            RenderSystem.enableBlend();
            if (k4 < list.size()) {
                PlayerInfo playerinfo1 = list.get(k4);
                PlayerTabOverlay.ScoreDisplayEntry playertaboverlay$scoredisplayentry = list1.get(k4);
                GameProfile gameprofile = playerinfo1.getProfile();
                if (flag2) {
                    Player player = this.minecraft.level.getPlayerByUUID(gameprofile.getId());
                    boolean flag = player != null && LivingEntityRenderer.isEntityUpsideDown(player);
                    boolean flag1 = player != null && player.isModelPartShown(PlayerModelPart.HAT);
                    PlayerFaceRenderer.draw(pGuiGraphics, playerinfo1.getSkin().texture(), l1, i2, 8, flag1, flag);
                    l1 += 9;
                }

                pGuiGraphics.drawString(
                    this.minecraft.font, playertaboverlay$scoredisplayentry.name, l1, i2, playerinfo1.getGameMode() == GameType.SPECTATOR ? -1862270977 : -1
                );
                if (pObjective != null && playerinfo1.getGameMode() != GameType.SPECTATOR) {
                    int j5 = l1 + j + 1;
                    int k5 = j5 + i3;
                    if (k5 - j5 > 5) {
                        this.renderTablistScore(pObjective, i2, playertaboverlay$scoredisplayentry, j5, k5, gameprofile.getId(), pGuiGraphics);
                    }
                }

                this.renderPingIcon(pGuiGraphics, j3, l1 - (flag2 ? 9 : 0), i2, playerinfo1);
            }
        }

        if (list3 != null) {
            l3 += k2 * 9 + 1;
            pGuiGraphics.fill(pWidth / 2 - i4 / 2 - 1, l3 - 1, pWidth / 2 + i4 / 2 + 1, l3 + list3.size() * 9, Integer.MIN_VALUE);

            for (FormattedCharSequence formattedcharsequence3 : list3) {
                int i5 = this.minecraft.font.width(formattedcharsequence3);
                pGuiGraphics.drawString(this.minecraft.font, formattedcharsequence3, pWidth / 2 - i5 / 2, l3, -1);
                l3 += 9;
            }
        }
    }

    protected void renderPingIcon(GuiGraphics pGuiGraphics, int pWidth, int pX, int pY, PlayerInfo pPlayerInfo) {
        ResourceLocation resourcelocation;
        if (pPlayerInfo.getLatency() < 0) {
            resourcelocation = PING_UNKNOWN_SPRITE;
        } else if (pPlayerInfo.getLatency() < 150) {
            resourcelocation = PING_5_SPRITE;
        } else if (pPlayerInfo.getLatency() < 300) {
            resourcelocation = PING_4_SPRITE;
        } else if (pPlayerInfo.getLatency() < 600) {
            resourcelocation = PING_3_SPRITE;
        } else if (pPlayerInfo.getLatency() < 1000) {
            resourcelocation = PING_2_SPRITE;
        } else {
            resourcelocation = PING_1_SPRITE;
        }

        pGuiGraphics.pose().pushPose();
        pGuiGraphics.pose().translate(0.0F, 0.0F, 100.0F);
        pGuiGraphics.blitSprite(resourcelocation, pX + pWidth - 11, pY, 10, 8);
        pGuiGraphics.pose().popPose();
    }

    private void renderTablistScore(
        Objective pObjective, int pY, PlayerTabOverlay.ScoreDisplayEntry pDisplayEntry, int pMinX, int pMaxX, UUID pPlayerUuid, GuiGraphics pGuiGraphics
    ) {
        if (pObjective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
            this.renderTablistHearts(pY, pMinX, pMaxX, pPlayerUuid, pGuiGraphics, pDisplayEntry.score);
        } else if (pDisplayEntry.formattedScore != null) {
            pGuiGraphics.drawString(this.minecraft.font, pDisplayEntry.formattedScore, pMaxX - pDisplayEntry.scoreWidth, pY, 16777215);
        }
    }

    private void renderTablistHearts(int pY, int pMinX, int pMaxX, UUID pPlayerUuid, GuiGraphics pGuiGraphics, int pHealth) {
        PlayerTabOverlay.HealthState playertaboverlay$healthstate = this.healthStates
            .computeIfAbsent(pPlayerUuid, p_249546_ -> new PlayerTabOverlay.HealthState(pHealth));
        playertaboverlay$healthstate.update(pHealth, (long)this.gui.getGuiTicks());
        int i = Mth.positiveCeilDiv(Math.max(pHealth, playertaboverlay$healthstate.displayedValue()), 2);
        int j = Math.max(pHealth, Math.max(playertaboverlay$healthstate.displayedValue(), 20)) / 2;
        boolean flag = playertaboverlay$healthstate.isBlinking((long)this.gui.getGuiTicks());
        if (i > 0) {
            int k = Mth.floor(Math.min((float)(pMaxX - pMinX - 4) / (float)j, 9.0F));
            if (k <= 3) {
                float f1 = Mth.clamp((float)pHealth / 20.0F, 0.0F, 1.0F);
                int j1 = (int)((1.0F - f1) * 255.0F) << 16 | (int)(f1 * 255.0F) << 8;
                float f = (float)pHealth / 2.0F;
                Component component = Component.translatable("multiplayer.player.list.hp", f);
                Component component1;
                if (pMaxX - this.minecraft.font.width(component) >= pMinX) {
                    component1 = component;
                } else {
                    component1 = Component.literal(Float.toString(f));
                }

                pGuiGraphics.drawString(this.minecraft.font, component1, (pMaxX + pMinX - this.minecraft.font.width(component1)) / 2, pY, j1);
            } else {
                ResourceLocation resourcelocation = flag ? HEART_CONTAINER_BLINKING_SPRITE : HEART_CONTAINER_SPRITE;

                for (int l = i; l < j; l++) {
                    pGuiGraphics.blitSprite(resourcelocation, pMinX + l * k, pY, 9, 9);
                }

                for (int i1 = 0; i1 < i; i1++) {
                    pGuiGraphics.blitSprite(resourcelocation, pMinX + i1 * k, pY, 9, 9);
                    if (flag) {
                        if (i1 * 2 + 1 < playertaboverlay$healthstate.displayedValue()) {
                            pGuiGraphics.blitSprite(HEART_FULL_BLINKING_SPRITE, pMinX + i1 * k, pY, 9, 9);
                        }

                        if (i1 * 2 + 1 == playertaboverlay$healthstate.displayedValue()) {
                            pGuiGraphics.blitSprite(HEART_HALF_BLINKING_SPRITE, pMinX + i1 * k, pY, 9, 9);
                        }
                    }

                    if (i1 * 2 + 1 < pHealth) {
                        pGuiGraphics.blitSprite(i1 >= 10 ? HEART_ABSORBING_FULL_BLINKING_SPRITE : HEART_FULL_SPRITE, pMinX + i1 * k, pY, 9, 9);
                    }

                    if (i1 * 2 + 1 == pHealth) {
                        pGuiGraphics.blitSprite(i1 >= 10 ? HEART_ABSORBING_HALF_BLINKING_SPRITE : HEART_HALF_SPRITE, pMinX + i1 * k, pY, 9, 9);
                    }
                }
            }
        }
    }

    public void setFooter(@Nullable Component pFooter) {
        this.footer = pFooter;
    }

    public void setHeader(@Nullable Component pHeader) {
        this.header = pHeader;
    }

    public void reset() {
        this.header = null;
        this.footer = null;
    }

    @OnlyIn(Dist.CLIENT)
    static class HealthState {
        private static final long DISPLAY_UPDATE_DELAY = 20L;
        private static final long DECREASE_BLINK_DURATION = 20L;
        private static final long INCREASE_BLINK_DURATION = 10L;
        private int lastValue;
        private int displayedValue;
        private long lastUpdateTick;
        private long blinkUntilTick;

        public HealthState(int pDisplayedValue) {
            this.displayedValue = pDisplayedValue;
            this.lastValue = pDisplayedValue;
        }

        public void update(int pValue, long pGuiTicks) {
            if (pValue != this.lastValue) {
                long i = pValue < this.lastValue ? 20L : 10L;
                this.blinkUntilTick = pGuiTicks + i;
                this.lastValue = pValue;
                this.lastUpdateTick = pGuiTicks;
            }

            if (pGuiTicks - this.lastUpdateTick > 20L) {
                this.displayedValue = pValue;
            }
        }

        public int displayedValue() {
            return this.displayedValue;
        }

        public boolean isBlinking(long pGuiTicks) {
            return this.blinkUntilTick > pGuiTicks && (this.blinkUntilTick - pGuiTicks) % 6L >= 3L;
        }
    }

    @OnlyIn(Dist.CLIENT)
    static record ScoreDisplayEntry(Component name, int score, @Nullable Component formattedScore, int scoreWidth) {
    }
}

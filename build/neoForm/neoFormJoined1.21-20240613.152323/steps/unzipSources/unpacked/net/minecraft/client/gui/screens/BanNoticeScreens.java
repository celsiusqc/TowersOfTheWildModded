package net.minecraft.client.gui.screens;

import com.mojang.authlib.minecraft.BanDetails;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.chat.report.BanReason;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.CommonLinks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class BanNoticeScreens {
    private static final Component TEMPORARY_BAN_TITLE = Component.translatable("gui.banned.title.temporary").withStyle(ChatFormatting.BOLD);
    private static final Component PERMANENT_BAN_TITLE = Component.translatable("gui.banned.title.permanent").withStyle(ChatFormatting.BOLD);
    public static final Component NAME_BAN_TITLE = Component.translatable("gui.banned.name.title").withStyle(ChatFormatting.BOLD);
    private static final Component SKIN_BAN_TITLE = Component.translatable("gui.banned.skin.title").withStyle(ChatFormatting.BOLD);
    private static final Component SKIN_BAN_DESCRIPTION = Component.translatable(
        "gui.banned.skin.description", Component.translationArg(CommonLinks.SUSPENSION_HELP)
    );

    public static ConfirmLinkScreen create(BooleanConsumer pCallback, BanDetails pBanDetails) {
        return new ConfirmLinkScreen(
            pCallback, getBannedTitle(pBanDetails), getBannedScreenText(pBanDetails), CommonLinks.SUSPENSION_HELP, CommonComponents.GUI_ACKNOWLEDGE, true
        );
    }

    public static ConfirmLinkScreen createSkinBan(Runnable pCallback) {
        URI uri = CommonLinks.SUSPENSION_HELP;
        return new ConfirmLinkScreen(p_351642_ -> {
            if (p_351642_) {
                Util.getPlatform().openUri(uri);
            }

            pCallback.run();
        }, SKIN_BAN_TITLE, SKIN_BAN_DESCRIPTION, uri, CommonComponents.GUI_ACKNOWLEDGE, true);
    }

    public static ConfirmLinkScreen createNameBan(String pUsername, Runnable pCallback) {
        URI uri = CommonLinks.SUSPENSION_HELP;
        return new ConfirmLinkScreen(
            p_351639_ -> {
                if (p_351639_) {
                    Util.getPlatform().openUri(uri);
                }

                pCallback.run();
            },
            NAME_BAN_TITLE,
            Component.translatable(
                "gui.banned.name.description",
                Component.literal(pUsername).withStyle(ChatFormatting.YELLOW),
                Component.translationArg(CommonLinks.SUSPENSION_HELP)
            ),
            uri,
            CommonComponents.GUI_ACKNOWLEDGE,
            true
        );
    }

    private static Component getBannedTitle(BanDetails pBanDetails) {
        return isTemporaryBan(pBanDetails) ? TEMPORARY_BAN_TITLE : PERMANENT_BAN_TITLE;
    }

    private static Component getBannedScreenText(BanDetails pBanDetails) {
        return Component.translatable(
            "gui.banned.description", getBanReasonText(pBanDetails), getBanStatusText(pBanDetails), Component.translationArg(CommonLinks.SUSPENSION_HELP)
        );
    }

    private static Component getBanReasonText(BanDetails pBanDetails) {
        String s = pBanDetails.reason();
        String s1 = pBanDetails.reasonMessage();
        if (StringUtils.isNumeric(s)) {
            int i = Integer.parseInt(s);
            BanReason banreason = BanReason.byId(i);
            Component component;
            if (banreason != null) {
                component = ComponentUtils.mergeStyles(banreason.title().copy(), Style.EMPTY.withBold(true));
            } else if (s1 != null) {
                component = Component.translatable("gui.banned.description.reason_id_message", i, s1).withStyle(ChatFormatting.BOLD);
            } else {
                component = Component.translatable("gui.banned.description.reason_id", i).withStyle(ChatFormatting.BOLD);
            }

            return Component.translatable("gui.banned.description.reason", component);
        } else {
            return Component.translatable("gui.banned.description.unknownreason");
        }
    }

    private static Component getBanStatusText(BanDetails pBanDetails) {
        if (isTemporaryBan(pBanDetails)) {
            Component component = getBanDurationText(pBanDetails);
            return Component.translatable(
                "gui.banned.description.temporary",
                Component.translatable("gui.banned.description.temporary.duration", component).withStyle(ChatFormatting.BOLD)
            );
        } else {
            return Component.translatable("gui.banned.description.permanent").withStyle(ChatFormatting.BOLD);
        }
    }

    private static Component getBanDurationText(BanDetails pBanDetails) {
        Duration duration = Duration.between(Instant.now(), pBanDetails.expires());
        long i = duration.toHours();
        if (i > 72L) {
            return CommonComponents.days(duration.toDays());
        } else {
            return i < 1L ? CommonComponents.minutes(duration.toMinutes()) : CommonComponents.hours(duration.toHours());
        }
    }

    private static boolean isTemporaryBan(BanDetails pBanDetails) {
        return pBanDetails.expires() != null;
    }
}

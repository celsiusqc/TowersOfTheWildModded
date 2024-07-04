package net.minecraft.client.multiplayer.chat.report;

import com.google.common.collect.Lists;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportChatMessage;
import com.mojang.authlib.minecraft.report.ReportEvidence;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.Optionull;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.ChatReportScreen;
import net.minecraft.client.multiplayer.chat.LoggedChatMessage;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.network.chat.SignedMessageLink;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class ChatReport extends Report {
    final IntSet reportedMessages = new IntOpenHashSet();

    ChatReport(UUID pReportId, Instant pCreatedAt, UUID pReportedProfileId) {
        super(pReportId, pCreatedAt, pReportedProfileId);
    }

    public void toggleReported(int pId, AbuseReportLimits pLimits) {
        if (this.reportedMessages.contains(pId)) {
            this.reportedMessages.remove(pId);
        } else if (this.reportedMessages.size() < pLimits.maxReportedMessageCount()) {
            this.reportedMessages.add(pId);
        }
    }

    public ChatReport copy() {
        ChatReport chatreport = new ChatReport(this.reportId, this.createdAt, this.reportedProfileId);
        chatreport.reportedMessages.addAll(this.reportedMessages);
        chatreport.comments = this.comments;
        chatreport.reason = this.reason;
        chatreport.attested = this.attested;
        return chatreport;
    }

    @Override
    public Screen createScreen(Screen pLastScreen, ReportingContext pReportingContext) {
        return new ChatReportScreen(pLastScreen, pReportingContext, this);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder extends Report.Builder<ChatReport> {
        public Builder(ChatReport pReport, AbuseReportLimits pLimits) {
            super(pReport, pLimits);
        }

        public Builder(UUID pReportedProfileId, AbuseReportLimits pLimits) {
            super(new ChatReport(UUID.randomUUID(), Instant.now(), pReportedProfileId), pLimits);
        }

        public IntSet reportedMessages() {
            return this.report.reportedMessages;
        }

        public void toggleReported(int pId) {
            this.report.toggleReported(pId, this.limits);
        }

        public boolean isReported(int pId) {
            return this.report.reportedMessages.contains(pId);
        }

        @Override
        public boolean hasContent() {
            return StringUtils.isNotEmpty(this.comments()) || !this.reportedMessages().isEmpty() || this.reason() != null;
        }

        @Nullable
        @Override
        public Report.CannotBuildReason checkBuildable() {
            if (this.report.reportedMessages.isEmpty()) {
                return Report.CannotBuildReason.NO_REPORTED_MESSAGES;
            } else if (this.report.reportedMessages.size() > this.limits.maxReportedMessageCount()) {
                return Report.CannotBuildReason.TOO_MANY_MESSAGES;
            } else if (this.report.reason == null) {
                return Report.CannotBuildReason.NO_REASON;
            } else {
                return this.report.comments.length() > this.limits.maxOpinionCommentsLength()
                    ? Report.CannotBuildReason.COMMENT_TOO_LONG
                    : super.checkBuildable();
            }
        }

        @Override
        public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext pReportingContext) {
            Report.CannotBuildReason report$cannotbuildreason = this.checkBuildable();
            if (report$cannotbuildreason != null) {
                return Either.right(report$cannotbuildreason);
            } else {
                String s = Objects.requireNonNull(this.report.reason).backendName();
                ReportEvidence reportevidence = this.buildEvidence(pReportingContext);
                ReportedEntity reportedentity = new ReportedEntity(this.report.reportedProfileId);
                AbuseReport abusereport = AbuseReport.chat(this.report.comments, s, reportevidence, reportedentity, this.report.createdAt);
                return Either.left(new Report.Result(this.report.reportId, ReportType.CHAT, abusereport));
            }
        }

        private ReportEvidence buildEvidence(ReportingContext pReportingContext) {
            List<ReportChatMessage> list = new ArrayList<>();
            ChatReportContextBuilder chatreportcontextbuilder = new ChatReportContextBuilder(this.limits.leadingContextMessageCount());
            chatreportcontextbuilder.collectAllContext(
                pReportingContext.chatLog(),
                this.report.reportedMessages,
                (p_299903_, p_300034_) -> list.add(this.buildReportedChatMessage(p_300034_, this.isReported(p_299903_)))
            );
            return new ReportEvidence(Lists.reverse(list));
        }

        private ReportChatMessage buildReportedChatMessage(LoggedChatMessage.Player pChatMessage, boolean pMessageReported) {
            SignedMessageLink signedmessagelink = pChatMessage.message().link();
            SignedMessageBody signedmessagebody = pChatMessage.message().signedBody();
            List<ByteBuffer> list = signedmessagebody.lastSeen().entries().stream().map(MessageSignature::asByteBuffer).toList();
            ByteBuffer bytebuffer = Optionull.map(pChatMessage.message().signature(), MessageSignature::asByteBuffer);
            return new ReportChatMessage(
                signedmessagelink.index(),
                signedmessagelink.sender(),
                signedmessagelink.sessionId(),
                signedmessagebody.timeStamp(),
                signedmessagebody.salt(),
                list,
                signedmessagebody.content(),
                bytebuffer,
                pMessageReported
            );
        }

        public ChatReport.Builder copy() {
            return new ChatReport.Builder(this.report.copy(), this.limits);
        }
    }
}

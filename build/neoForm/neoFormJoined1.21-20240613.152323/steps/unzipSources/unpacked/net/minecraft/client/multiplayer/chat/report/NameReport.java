package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.minecraft.report.ReportedEntity;
import com.mojang.datafixers.util.Either;
import java.time.Instant;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.reporting.NameReportScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.apache.commons.lang3.StringUtils;

@OnlyIn(Dist.CLIENT)
public class NameReport extends Report {
    private final String reportedName;

    NameReport(UUID pReportId, Instant pCreatedAt, UUID pReportedProfileId, String pReportedName) {
        super(pReportId, pCreatedAt, pReportedProfileId);
        this.reportedName = pReportedName;
    }

    public String getReportedName() {
        return this.reportedName;
    }

    public NameReport copy() {
        NameReport namereport = new NameReport(this.reportId, this.createdAt, this.reportedProfileId, this.reportedName);
        namereport.comments = this.comments;
        namereport.attested = this.attested;
        return namereport;
    }

    @Override
    public Screen createScreen(Screen pLastScreen, ReportingContext pReportingContext) {
        return new NameReportScreen(pLastScreen, pReportingContext, this);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Builder extends Report.Builder<NameReport> {
        public Builder(NameReport pReport, AbuseReportLimits pLimits) {
            super(pReport, pLimits);
        }

        public Builder(UUID pReportedProfileId, String pReportedName, AbuseReportLimits pLimits) {
            super(new NameReport(UUID.randomUUID(), Instant.now(), pReportedProfileId, pReportedName), pLimits);
        }

        @Override
        public boolean hasContent() {
            return StringUtils.isNotEmpty(this.comments());
        }

        @Nullable
        @Override
        public Report.CannotBuildReason checkBuildable() {
            return this.report.comments.length() > this.limits.maxOpinionCommentsLength() ? Report.CannotBuildReason.COMMENT_TOO_LONG : super.checkBuildable();
        }

        @Override
        public Either<Report.Result, Report.CannotBuildReason> build(ReportingContext pReportingContext) {
            Report.CannotBuildReason report$cannotbuildreason = this.checkBuildable();
            if (report$cannotbuildreason != null) {
                return Either.right(report$cannotbuildreason);
            } else {
                ReportedEntity reportedentity = new ReportedEntity(this.report.reportedProfileId);
                AbuseReport abusereport = AbuseReport.name(this.report.comments, reportedentity, this.report.createdAt);
                return Either.left(new Report.Result(this.report.reportId, ReportType.USERNAME, abusereport));
            }
        }
    }
}

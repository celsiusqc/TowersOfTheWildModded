package net.minecraft.client.multiplayer.chat.report;

import com.mojang.authlib.exceptions.MinecraftClientException;
import com.mojang.authlib.exceptions.MinecraftClientHttpException;
import com.mojang.authlib.minecraft.UserApiService;
import com.mojang.authlib.minecraft.report.AbuseReport;
import com.mojang.authlib.minecraft.report.AbuseReportLimits;
import com.mojang.authlib.yggdrasil.request.AbuseReportRequest;
import com.mojang.datafixers.util.Unit;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ThrowingComponent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface AbuseReportSender {
    static AbuseReportSender create(ReportEnvironment pEnvironment, UserApiService pUserApiService) {
        return new AbuseReportSender.Services(pEnvironment, pUserApiService);
    }

    CompletableFuture<Unit> send(UUID pId, ReportType pReportType, AbuseReport pReport);

    boolean isEnabled();

    default AbuseReportLimits reportLimits() {
        return AbuseReportLimits.DEFAULTS;
    }

    @OnlyIn(Dist.CLIENT)
    public static class SendException extends ThrowingComponent {
        public SendException(Component p_239646_, Throwable p_239647_) {
            super(p_239646_, p_239647_);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static record Services(ReportEnvironment environment, UserApiService userApiService) implements AbuseReportSender {
        private static final Component SERVICE_UNAVAILABLE_TEXT = Component.translatable("gui.abuseReport.send.service_unavailable");
        private static final Component HTTP_ERROR_TEXT = Component.translatable("gui.abuseReport.send.http_error");
        private static final Component JSON_ERROR_TEXT = Component.translatable("gui.abuseReport.send.json_error");

        @Override
        public CompletableFuture<Unit> send(UUID pId, ReportType pReportType, AbuseReport pReport) {
            return CompletableFuture.supplyAsync(
                () -> {
                    AbuseReportRequest abusereportrequest = new AbuseReportRequest(
                        1,
                        pId,
                        pReport,
                        this.environment.clientInfo(),
                        this.environment.thirdPartyServerInfo(),
                        this.environment.realmInfo(),
                        pReportType.backendName()
                    );

                    try {
                        this.userApiService.reportAbuse(abusereportrequest);
                        return Unit.INSTANCE;
                    } catch (MinecraftClientHttpException minecraftclienthttpexception) {
                        Component component1 = this.getHttpErrorDescription(minecraftclienthttpexception);
                        throw new CompletionException(new AbuseReportSender.SendException(component1, minecraftclienthttpexception));
                    } catch (MinecraftClientException minecraftclientexception) {
                        Component component = this.getErrorDescription(minecraftclientexception);
                        throw new CompletionException(new AbuseReportSender.SendException(component, minecraftclientexception));
                    }
                },
                Util.ioPool()
            );
        }

        @Override
        public boolean isEnabled() {
            return this.userApiService.canSendReports();
        }

        private Component getHttpErrorDescription(MinecraftClientHttpException pHttpException) {
            return Component.translatable("gui.abuseReport.send.error_message", pHttpException.getMessage());
        }

        private Component getErrorDescription(MinecraftClientException pException) {
            return switch (pException.getType()) {
                case SERVICE_UNAVAILABLE -> SERVICE_UNAVAILABLE_TEXT;
                case HTTP_ERROR -> HTTP_ERROR_TEXT;
                case JSON_ERROR -> JSON_ERROR_TEXT;
            };
        }

        @Override
        public AbuseReportLimits reportLimits() {
            return this.userApiService.getAbuseReportLimits();
        }
    }
}

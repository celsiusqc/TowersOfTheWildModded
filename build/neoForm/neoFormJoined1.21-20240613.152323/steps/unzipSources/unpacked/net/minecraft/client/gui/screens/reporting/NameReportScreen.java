package net.minecraft.client.gui.screens.reporting;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.CommonLayouts;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.chat.report.NameReport;
import net.minecraft.client.multiplayer.chat.report.ReportingContext;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class NameReportScreen extends AbstractReportScreen<NameReport.Builder> {
    private static final Component TITLE = Component.translatable("gui.abuseReport.name.title");
    private MultiLineEditBox commentBox;

    private NameReportScreen(Screen pLastScreen, ReportingContext pReportingContext, NameReport.Builder pReportBuilder) {
        super(TITLE, pLastScreen, pReportingContext, pReportBuilder);
    }

    public NameReportScreen(Screen pLastScreen, ReportingContext pReportingContext, UUID pReportedProfileId, String pReportedName) {
        this(pLastScreen, pReportingContext, new NameReport.Builder(pReportedProfileId, pReportedName, pReportingContext.sender().reportLimits()));
    }

    public NameReportScreen(Screen pLastScreen, ReportingContext pReportingContext, NameReport pReport) {
        this(pLastScreen, pReportingContext, new NameReport.Builder(pReport, pReportingContext.sender().reportLimits()));
    }

    @Override
    protected void addContent() {
        Component component = Component.literal(this.reportBuilder.report().getReportedName()).withStyle(ChatFormatting.YELLOW);
        this.layout
            .addChild(
                new StringWidget(Component.translatable("gui.abuseReport.name.reporting", component), this.font),
                p_300033_ -> p_300033_.alignHorizontallyLeft().padding(0, 8)
            );
        this.commentBox = this.createCommentBox(280, 9 * 8, p_352668_ -> {
            this.reportBuilder.setComments(p_352668_);
            this.onReportChanged();
        });
        this.layout.addChild(CommonLayouts.labeledElement(this.font, this.commentBox, MORE_COMMENTS_LABEL, p_299902_ -> p_299902_.paddingBottom(12)));
    }

    /**
     * Called when a mouse button is released within the GUI element.
     * <p>
     * @return {@code true} if the event is consumed, {@code false} otherwise.
     *
     * @param pMouseX the X coordinate of the mouse.
     * @param pMouseY the Y coordinate of the mouse.
     * @param pButton the button that was released.
     */
    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        return super.mouseReleased(pMouseX, pMouseY, pButton) ? true : this.commentBox.mouseReleased(pMouseX, pMouseY, pButton);
    }
}

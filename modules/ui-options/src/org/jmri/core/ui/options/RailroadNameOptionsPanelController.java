package org.jmri.core.ui.options;

import jmri.web.server.RailroadNamePreferencesPanel;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;

@OptionsPanelController.SubRegistration(
        location = "Advanced",
        displayName = "#AdvancedOption_DisplayName_RailroadName",
        keywords = "#AdvancedOption_Keywords_RailroadName",
        keywordsCategory = "General/RailroadName"
)
@NbBundle.Messages({
    "AdvancedOption_DisplayName_RailroadName=Railroad Name",
    "AdvancedOption_Keywords_RailroadName=Railroad, Railroad Name, Name",
    "RailroadOptions.RestartReason=add, remove, or edit connections"
})
public final class RailroadNameOptionsPanelController extends PreferencesPanelController {

    public RailroadNameOptionsPanelController() {
        super(new RailroadNamePreferencesPanel());
    }

    @Override
    public String getRestartReason() {
        return Bundle.RailroadOptions_RestartReason();
    }
}

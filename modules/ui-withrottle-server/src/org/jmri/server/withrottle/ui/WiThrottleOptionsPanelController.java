package org.jmri.server.withrottle.ui;

import jmri.jmrit.withrottle.WiThrottlePrefsPanel;
import org.jmri.core.ui.options.PreferencesPanelController;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;

@OptionsPanelController.SubRegistration(
        location = "NetworkServices",
        displayName = "#AdvancedOption_DisplayName_WiThrottle",
        keywords = "#AdvancedOption_Keywords_WiThrottle",
        keywordsCategory = "NetworkServices/WiThrottle"
)
@NbBundle.Messages({
    "AdvancedOption_DisplayName_WiThrottle=WiThrottle",
    "AdvancedOption_Keywords_WiThrottle=WiThrottle",
    "WiThrottleOptions.RestartReason=change WiThottle options"
})
public final class WiThrottleOptionsPanelController extends PreferencesPanelController {

    public WiThrottleOptionsPanelController() {
        super(new WiThrottlePrefsPanel());
    }

    @Override
    public String getRestartReason() {
        return Bundle.WiThrottleOptions_RestartReason();
    }
}

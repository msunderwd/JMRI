package org.jmri.server.web.ui;

import jmri.web.server.WebServerPreferencesPanel;
import org.jmri.core.ui.options.PreferencesPanelController;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle.Messages;

@OptionsPanelController.SubRegistration(
        location = "NetworkServices",
        displayName = "#AdvancedOption_DisplayName_WebServer",
        keywords = "#AdvancedOption_Keywords_WebServer",
        keywordsCategory = "NetworkServices/WebServer"
)
@Messages({
    "AdvancedOption_DisplayName_WebServer=Web",
    "AdvancedOption_Keywords_WebServer=Web, Server",
    "WebServerOptions.RestartReason=change web server options"
})
public final class WebServerOptionsPanelController extends PreferencesPanelController {

    public WebServerOptionsPanelController() {
        super(new WebServerPreferencesPanel());
    }

    @Override
    public String getRestartReason() {
        return Bundle.WebServerOptions_RestartReason();
    }
}

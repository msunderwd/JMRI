/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jmri.server.json.ui;

import jmri.jmris.json.JsonServerPreferencesPanel;
import org.jmri.core.ui.options.PreferencesPanelController;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;

@OptionsPanelController.SubRegistration(
        location = "NetworkServices",
        displayName = "#AdvancedOption_DisplayName_JsonServer",
        keywords = "#AdvancedOption_Keywords_JsonServer",
        keywordsCategory = "NetworkServices/JsonServer"
)
@NbBundle.Messages({
    "AdvancedOption_DisplayName_JsonServer=JSON",
    "AdvancedOption_Keywords_JsonServer=JSON",
    "JsonServerOptions.RestartReason=change JSON server port"
})
public final class JsonServerOptionsPanelController extends PreferencesPanelController {

    public JsonServerOptionsPanelController() {
        super(new JsonServerPreferencesPanel());
    }

    @Override
    public String getRestartReason() {
        return Bundle.JsonServerOptions_RestartReason();
    }

}

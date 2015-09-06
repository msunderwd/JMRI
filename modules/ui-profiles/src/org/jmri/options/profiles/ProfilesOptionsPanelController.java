package org.jmri.options.profiles;

import jmri.profile.ProfilePreferencesPanel;
import org.jmri.core.ui.options.PreferencesPanelController;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.NbBundle;

@OptionsPanelController.TopLevelRegistration(
        categoryName = "#OptionsCategory_Name_Profiles",
        iconBase = "org/jmri/options/profiles/Gnome-applications-office.png",
        keywords = "#OptionsCategory_Keywords_Profiles",
        keywordsCategory = "Profiles",
        position = 150
)
@NbBundle.Messages({
    "OptionsCategory_Name_Profiles=Config Profiles",
    "OptionsCategory_Keywords_Profiles=profile",
    "ProfilesOptions.RestartReason=use selected profile"
})
public final class ProfilesOptionsPanelController extends PreferencesPanelController {

    public ProfilesOptionsPanelController() {
        super(new ProfilePreferencesPanel());
    }

    @Override
    public String getRestartReason() {
        return Bundle.ProfilesOptions_RestartReason();
    }

}

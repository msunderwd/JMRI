package org.jmri.core.ui.options;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import jmri.Application;
import jmri.swing.PreferencesPanel;
import org.jmri.managers.PersistentPreferencesManager;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.DialogDisplayer;
import org.openide.LifecycleManager;
import org.openide.NotifyDescriptor;
import org.openide.awt.Notification;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract OptionsPanelController that maps the standard OptionsPanelController
 * to a JMRI PreferencesPanel.
 *
 * @author Randall Wood <randall.h.wood@alexandriasoftware.com>
 */
public abstract class PreferencesPanelController extends OptionsPanelController {

    @Messages({
        "# {0} - Application name",
        "# {1} - Reason to restart",
        "restartConfirmation.message=Restart {0} to {1} now?",
        "# {0} - Application name",
        "restartConfirmation.title=Restart {0}.",
        "# {0} - Application name",
        "# {1} - Reason to restart",
        "restartNotification.message=Click here to restart {0} to {1}.",
    })

    private Notification restartNotification = null;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final PreferencesPanel preferencesPanel;
    private final static Logger log = LoggerFactory.getLogger(PreferencesPanelController.class);

    public PreferencesPanelController(PreferencesPanel preferencesPanel) {
        if (!preferencesPanel.isPersistant()) {
            this.preferencesPanel = preferencesPanel;
        } else {
            PersistentPreferencesManager manager = Lookup.getDefault().lookup(PersistentPreferencesManager.class);
            PreferencesPanel aPanel = manager.getPanel(preferencesPanel.getClass().getName());
            if (aPanel != null) {
                this.preferencesPanel = aPanel;
            } else {
                manager.putPanel(preferencesPanel);
                this.preferencesPanel = preferencesPanel;
            }
        }
    }

    public PreferencesPanel getPreferencesPanel() {
        return this.preferencesPanel;
    }

    @Override
    public void update() {
        // do nothing
    }

    @Override
    public void applyChanges() {
        this.applyChanges(this.preferencesPanel.isPersistant());
    }

    /**
     * Provide a reason to request a restart.
     * 
     * @return the reason
     */
    public abstract String getRestartReason();
    
    /**
     * Called by {@link #applyChanges() } with a boolean value indicating the
     * changes should be saved as if the preferences are handled by the
     * persistent configuration manager or not.
     *
     * The default applyChanges passes the result of
     * <code>this.preferencesPanel.isPersistant()</code> for the isPersistent
     * parameter.
     *
     * @param isPersistent true if persistent configuration manager should be
     * invoked
     */
    protected void applyChanges(boolean isPersistent) {
        SwingUtilities.invokeLater(() -> {
            this.preferencesPanel.savePreferences();
            boolean isRestartRequired = this.preferencesPanel.isRestartRequired();
            PersistentPreferencesManager manager = Lookup.getDefault().lookup(PersistentPreferencesManager.class);
            if (isPersistent && this.preferencesPanel.isDirty()) {
                // this may result in multiple writes to the profile configuration
                // if a persistant PreferencesPanel sets itself to dirty after
                // another PreferencesPanel has already written the configuration
                manager.savePreferences();
                isRestartRequired = manager.isRestartRequired();
            }
            if (isRestartRequired) {
                this.promptToRestart(this.getRestartReason());
            }
        });
    }

    /**
     * If any action needs to be taken when the Options dialog is dismissed
     * without pressing OK, this method should be overridden.
     */
    @Override
    public void cancel() {
        // do nothing by default
    }

    /**
     * Return true if preferences can be saved or applied.
     *
     * @return true if preferences can be saved or applied.
     */
    @Override
    public boolean isValid() {
        log.info("{} isValid called.", this.preferencesPanel.getTabbedPreferencesTitle());
        return this.preferencesPanel.isPreferencesValid();
    }

    @Override
    public boolean isChanged() {
        log.info("{} isChanged called.", this.preferencesPanel.getTabbedPreferencesTitle());
        return this.preferencesPanel.isDirty();
    }

    @Override
    public JComponent getComponent(Lookup lkp) {
        return this.preferencesPanel.getPreferencesComponent();
    }

    @Override
    public HelpCtx getHelpCtx() {
        // there is no analogous mechanism in a PreferencesPanel
        // subclasses should implement this if they have specific help
        return null;
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener pl) {
        this.pcs.addPropertyChangeListener(pl);
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener pl) {
        this.pcs.removePropertyChangeListener(pl);
    }

    public void changed() {
        log.info("{} changed fired.", this.preferencesPanel.getTabbedPreferencesTitle());
        if (this.isChanged()) {
            this.pcs.firePropertyChange(OptionsPanelController.PROP_CHANGED, false, true);
        }
        if (this.isValid()) {
            this.pcs.firePropertyChange(OptionsPanelController.PROP_VALID, null, null);
        }
    }

    public void promptToRestart(String reason) {
        if (this.restartNotification == null) {
            if (NotifyDescriptor.YES_OPTION.equals(DialogDisplayer.getDefault().notify(
                    new NotifyDescriptor.Confirmation(
                            Bundle.restartConfirmation_message(Application.getApplicationName(), reason),
                            Bundle.restartConfirmation_title(Application.getApplicationName()),
                            NotifyDescriptor.YES_NO_OPTION)))) {
                LifecycleManager.getDefault().markForRestart();
                LifecycleManager.getDefault().exit();
            } else {
                this.notifyToRestart(reason);
            }
        }
    }

    public void notifyToRestart(String reason) {
        if (this.restartNotification == null) {
            this.restartNotification = NotificationDisplayer.getDefault().notify(
                    Bundle.restartConfirmation_title(Application.getApplicationName()),
                    new ImageIcon("org/jmri/core/ui/options/Gnome-view-refresh.png"),
                    Bundle.restartNotification_message(Application.getApplicationName(), reason),
                    (ActionEvent e) -> {
                        LifecycleManager.getDefault().markForRestart();
                        LifecycleManager.getDefault().exit();
                    },
                    NotificationDisplayer.Priority.HIGH,
                    NotificationDisplayer.Category.INFO);
        }
    }
}

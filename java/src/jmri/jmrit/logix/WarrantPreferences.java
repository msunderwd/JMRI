package jmri.jmrit.logix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nonnull;
import jmri.InstanceManager;
import jmri.implementation.SignalSpeedMap;
import jmri.jmrit.XmlFile;
import jmri.jmrit.logix.WarrantPreferencesPanel.DataPair;
import jmri.profile.Profile;
import jmri.profile.ProfileManager;
import jmri.util.FileUtil;
import jmri.util.prefs.AbstractPreferencesManager;
import jmri.util.prefs.InitializationException;
import org.jdom2.Attribute;
import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hold configuration data for Warrants, includes Speed Map
 *
 * @author Pete Cressman Copyright (C) 2015
 */
public class WarrantPreferences extends AbstractPreferencesManager {

    public static final String LAYOUT_PARAMS = "layoutParams"; // NOI18N
    public static final String LAYOUT_SCALE = "layoutScale"; // NOI18N
    public static final String SEARCH_DEPTH = "searchDepth"; // NOI18N
    public static final String SPEED_MAP_PARAMS = "speedMapParams"; // NOI18N
    public static final String RAMP_PREFS = "rampPrefs";         // NOI18N
    public static final String TIME_INCREMENT = "timeIncrement"; // NOI18N
    public static final String THROTTLE_SCALE = "throttleScale"; // NOI18N
    public static final String RAMP_INCREMENT = "rampIncrement"; // NOI18N
    public static final String STEP_INCREMENTS = "stepIncrements"; // NOI18N
    public static final String SPEED_NAME_PREFS = "speedNames";   // NOI18N
    public static final String SPEED_NAMES = SPEED_NAME_PREFS;
    public static final String INTERPRETATION = "interpretation"; // NOI18N
    public static final String APPEARANCE_PREFS = "appearancePrefs"; // NOI18N
    public static final String APPEARANCES = "appearances"; // NOI18N
    /**
     * @deprecated since 4.7.1; use {@link #LAYOUT_PARAMS} instead
     */
    @Deprecated
    public static final String layoutParams = LAYOUT_PARAMS;
    /**
     * @deprecated since 4.7.1; use {@link #LAYOUT_SCALE} instead
     */
    @Deprecated
    public static final String LayoutScale = LAYOUT_SCALE;
    /**
     * @deprecated since 4.7.1; use {@link #SEARCH_DEPTH} instead
     */
    @Deprecated
    public static final String SearchDepth = SEARCH_DEPTH;
    /**
     * @deprecated since 4.7.1; use {@link #SPEED_MAP_PARAMS} instead
     */
    @Deprecated
    public static final String SpeedMapParams = SPEED_MAP_PARAMS;
    /**
     * @deprecated since 4.7.1; use {@link #RAMP_PREFS} instead
     */
    @Deprecated
    public static final String RampPrefs = RAMP_PREFS;
    /**
     * @deprecated since 4.7.1; use {@link #TIME_INCREMENT} instead
     */
    @Deprecated
    public static final String TimeIncrement = TIME_INCREMENT;
    /**
     * @deprecated since 4.7.1; use {@link #THROTTLE_SCALE} instead
     */
    @Deprecated
    public static final String ThrottleScale = THROTTLE_SCALE;
    /**
     * @deprecated since 4.7.1; use {@link #RAMP_INCREMENT} instead
     */
    @Deprecated
    public static final String RampIncrement = RAMP_INCREMENT;
    /**
     * @deprecated since 4.7.1; use {@link #STEP_INCREMENTS} instead
     */
    @Deprecated
    public static final String StepIncrements = STEP_INCREMENTS;
    /**
     * @deprecated since 4.7.1; use {@link #SPEED_NAME_PREFS} instead
     */
    @Deprecated
    public static final String SpeedNamePrefs = SPEED_NAME_PREFS;
    /**
     * @deprecated since 4.7.1; use {@link #INTERPRETATION} instead
     */
    @Deprecated
    public static final String Interpretation = INTERPRETATION;
    /**
     * @deprecated since 4.7.1; use {@link #APPEARANCE_PREFS} instead
     */
    @Deprecated
    public static final String AppearancePrefs = APPEARANCE_PREFS;

    private String _fileName;
    private float _scale = 87.1f;
    private int _searchDepth = 20;      // How many tree nodes (blocks) to walk in finding routes
    private float _throttleScale = 0.5f;  // factor to approximate throttle setting to track speed

    private final LinkedHashMap<String, Float> _speedNames = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> _headAppearances = new LinkedHashMap<>();
    private int _interpretation = SignalSpeedMap.PERCENT_NORMAL;    // Interpretation of values in speed name table

    private int _msIncrTime = 1000;         // time in milliseconds between speed changes ramping up or down
    private float _throttleIncr = 0.04f;    // throttle increment for each ramp speed change

    /**
     * Get the default instance.
     *
     * @return the default instance, creating it if necessary
     */
    public static WarrantPreferences getDefault() {
        return InstanceManager.getOptionalDefault(WarrantPreferences.class).orElseGet(() -> {
            WarrantPreferences preferences = InstanceManager.setDefault(WarrantPreferences.class, new WarrantPreferences());
            try {
                preferences.initialize(ProfileManager.getDefault().getActiveProfile());
            } catch (InitializationException ex) {
                log.error("Error initializing default WarrantPreferences", ex);
            }
            return preferences;
        });
    }

    public void openFile(String name) {
        _fileName = name;
        WarrantPreferencesXml prefsXml = new WarrantPreferencesXml();
        File file = new File(_fileName);
        Element root;
        try {
            root = prefsXml.rootFromFile(file);
        } catch (java.io.FileNotFoundException ea) {
            log.debug("Could not find Warrant preferences file.  Normal if preferences have not been saved before.");
            root = null;
        } catch (IOException | JDOMException eb) {
            log.error("Exception while loading warrant preferences: " + eb);
            root = null;
        }
        if (root != null) {
            log.info("Found Warrant preferences file: {}", _fileName);
            loadLayoutParams(root.getChild(LAYOUT_PARAMS));
            if (!loadSpeedMap(root.getChild(SPEED_MAP_PARAMS))) {
                loadSpeedMapFromOldXml();
                log.error("Unable to read ramp parameters. Setting to default values.");
            }
        } else {
            loadSpeedMapFromOldXml();
        }
    }

    public void loadLayoutParams(Element child) {
        if (child == null) {
            return;
        }
        Attribute a;
        if ((a = child.getAttribute(LAYOUT_SCALE)) != null) {
            try {
                setScale(a.getFloatValue());
            } catch (DataConversionException ex) {
                setScale(87.1f);
                log.error("Unable to read layout scale. Setting to default value.", ex);
            }
        }
        if ((a = child.getAttribute(SEARCH_DEPTH)) != null) {
            try {
                setSearchDepth(a.getIntValue());
            } catch (DataConversionException ex) {
                setSearchDepth(20);
                log.error("Unable to read route search depth. Setting to default value (20).", ex);
            }
        }
    }

    private void loadSpeedMapFromOldXml() {
        SignalSpeedMap map = jmri.InstanceManager.getNullableDefault(SignalSpeedMap.class);
        if (map == null) {
            log.error("Cannot find signalSpeeds.xml file.");
            return;
        }
        Iterator<String> it = map.getValidSpeedNames().iterator();
        LinkedHashMap<String, Float> names = new LinkedHashMap<>();
        while (it.hasNext()) {
            String name = it.next();
            names.put(name, map.getSpeed(name));
        }
        this.setSpeedNames(names);

        Enumeration<String> en = map.getAppearanceIterator();
        LinkedHashMap<String, String> heads = new LinkedHashMap<>();
        while (en.hasMoreElements()) {
            String name = en.nextElement();
            heads.put(name, map.getAppearanceSpeed(name));
        }
        this.setAppearances(heads);
        setTimeIncrement(map.getStepDelay());
        setThrottleIncrement(map.getStepIncrement());
    }

    public boolean loadSpeedMap(Element child) {
        if (child == null) {
            return false;
        }
        Element rampParms = child.getChild(STEP_INCREMENTS);
        if (rampParms == null) {
            return false;
        }
        Attribute a;
        if ((a = rampParms.getAttribute(TIME_INCREMENT)) != null) {
            try {
                setTimeIncrement(a.getIntValue());
            } catch (DataConversionException ex) {
                setTimeIncrement(750);
                log.error("Unable to read ramp time increment. Setting to default value (750ms).", ex);
            }
        }
        if ((a = rampParms.getAttribute(RAMP_INCREMENT)) != null) {
            try {
                setThrottleIncrement(a.getFloatValue());
            } catch (DataConversionException ex) {
                setThrottleIncrement(0.05f);
                log.error("Unable to read ramp throttle increment. Setting to default value (0.05).", ex);
            }
        }
        if ((a = rampParms.getAttribute(THROTTLE_SCALE)) != null) {
            try {
                setThrottleScale(a.getFloatValue());
            } catch (DataConversionException ex) {
                setThrottleScale(0.70f);
                log.error("Unable to read throttle scale. Setting to default value (0.70f).", ex);
            }
        }

        rampParms = child.getChild(SPEED_NAME_PREFS);
        if (rampParms == null) {
            return false;
        }
        if ((a = rampParms.getAttribute("percentNormal")) != null) {
            if (a.getValue().equals("yes")) {
                setInterpretation(1);
            } else {
                setInterpretation(2);
            }
        }
        if ((a = rampParms.getAttribute(INTERPRETATION)) != null) {
            try {
                setInterpretation(a.getIntValue());
            } catch (DataConversionException ex) {
                setInterpretation(1);
                log.error("Unable to read interpetation of Speed Map. Setting to default value % normal.", ex);
            }
        }
        HashMap<String, Float> map = new LinkedHashMap<>();
        List<Element> list = rampParms.getChildren();
        for (int i = 0; i < list.size(); i++) {
            String name = list.get(i).getName();
            Float speed = 0f;
            try {
                speed = Float.valueOf(list.get(i).getText());
            } catch (NumberFormatException nfe) {
                log.error("Speed names has invalid content for {} = ", name, list.get(i).getText());
            }
            log.debug("Add {}, {} to AspectSpeed Table", name, speed);
            map.put(name, speed);
        }
        this.setSpeedNames(map);

        rampParms = child.getChild(APPEARANCE_PREFS);
        if (rampParms == null) {
            return false;
        }
        LinkedHashMap<String, String> heads = new LinkedHashMap<>();
        list = rampParms.getChildren();
        for (int i = 0; i < list.size(); i++) {
            String name = Bundle.getMessage(list.get(i).getName());
            String speed = list.get(i).getText();
            heads.put(name, speed);
        }
        this.setAppearances(heads);

        return true;
    }

    public void save() {
        if (_fileName == null) {
            log.error("_fileName null. Could not create warrant preferences file.");
            return;
        }

        XmlFile xmlFile = new XmlFile() {
        };
        xmlFile.makeBackupFile(_fileName);
        File file = new File(_fileName);
        try {
            File parentDir = file.getParentFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdir()) {
                    log.warn("Could not create parent directory for prefs file :{}", _fileName);
                    return;
                }
            }
            if (file.createNewFile()) {
                log.debug("Creating new warrant prefs file: {}", _fileName);
            }
        } catch (IOException ea) {
            log.error("Could not create warrant preferences file at {}.", _fileName, ea);
        }

        try {
            Element root = new Element("warrantPreferences");
            Document doc = XmlFile.newDocument(root);
            if (store(root)) {
                xmlFile.writeXML(file, doc);
            }
        } catch (IOException eb) {
            log.warn("Exception in storing warrant xml: {}", eb);
        }
    }

    public boolean store(Element root) {
        Element prefs = new Element(LAYOUT_PARAMS);
        try {
            prefs.setAttribute(LAYOUT_SCALE, Float.toString(getLayoutScale()));
            prefs.setAttribute(SEARCH_DEPTH, Integer.toString(getSearchDepth()));
            root.addContent(prefs);

            prefs = new Element(SPEED_MAP_PARAMS);
            Element rampPrefs = new Element(STEP_INCREMENTS);
            rampPrefs.setAttribute(TIME_INCREMENT, Integer.toString(getTimeIncrement()));
            rampPrefs.setAttribute(RAMP_INCREMENT, Float.toString(getThrottleIncrement()));
            rampPrefs.setAttribute(THROTTLE_SCALE, Float.toString(getThrottleScale()));
            prefs.addContent(rampPrefs);

            rampPrefs = new Element(SPEED_NAME_PREFS);
            rampPrefs.setAttribute(INTERPRETATION, Integer.toString(getInterpretation()));

            Iterator<Entry<String, Float>> it = getSpeedNameEntryIterator();
            while (it.hasNext()) {
                Entry<String, Float> ent = it.next();
                Element step = new Element(ent.getKey());
                step.setText(ent.getValue().toString());
                rampPrefs.addContent(step);
            }
            prefs.addContent(rampPrefs);

            rampPrefs = new Element(APPEARANCE_PREFS);
            Element step = new Element("SignalHeadStateRed");
            step.setText(_headAppearances.get(Bundle.getMessage("SignalHeadStateRed")));
            rampPrefs.addContent(step);
            step = new Element("SignalHeadStateFlashingRed");
            step.setText(_headAppearances.get(Bundle.getMessage("SignalHeadStateFlashingRed")));
            rampPrefs.addContent(step);
            step = new Element("SignalHeadStateGreen");
            step.setText(_headAppearances.get(Bundle.getMessage("SignalHeadStateGreen")));
            rampPrefs.addContent(step);
            step = new Element("SignalHeadStateFlashingGreen");
            step.setText(_headAppearances.get(Bundle.getMessage("SignalHeadStateFlashingGreen")));
            rampPrefs.addContent(step);
            step = new Element("SignalHeadStateYellow");
            step.setText(_headAppearances.get(Bundle.getMessage("SignalHeadStateYellow")));
            rampPrefs.addContent(step);
            step = new Element("SignalHeadStateFlashingYellow");
            step.setText(_headAppearances.get(Bundle.getMessage("SignalHeadStateFlashingYellow")));
            rampPrefs.addContent(step);
            step = new Element("SignalHeadStateLunar");
            step.setText(_headAppearances.get(Bundle.getMessage("SignalHeadStateLunar")));
            rampPrefs.addContent(step);
            step = new Element("SignalHeadStateFlashingLunar");
            step.setText(_headAppearances.get(Bundle.getMessage("SignalHeadStateFlashingLunar")));
            rampPrefs.addContent(step);
            prefs.addContent(rampPrefs);
        } catch (Exception ex) {
            log.warn("Exception in storing warrant xml.", ex);
            return false;
        }
        root.addContent(prefs);
        return true;
    }

    /**
     * @deprecated since 4.7.2 without replacement. Classes interested in
     * changes to the warrant preferences listen for those changes.
     */
    @Deprecated
    public void apply() {
    }

    /**
     * @return the scale
     * @deprecated since 4.7.1; use {@link #getLayoutScale()} instead
     */
    @Deprecated
    float getScale() {
        return this.getLayoutScale();
    }

    /**
     * @param s the scale
     * @deprecated since 4.7.1; use {@link #setLayoutScale(float)} instead
     */
    @Deprecated
    void setScale(float s) {
        this.setLayoutScale(s);
    }

    /**
     * Get the layout scale.
     *
     * @return the scale
     */
    public float getLayoutScale() {
        return _scale;
    }

    /**
     * Set the layout scale.
     *
     * @param scale the scale
     */
    public void setLayoutScale(float scale) {
        float oldScale = this._scale;
        _scale = scale;
        this.firePropertyChange(LAYOUT_SCALE, oldScale, scale);
    }

    public float getThrottleScale() {
        return _throttleScale;
    }

    public void setThrottleScale(float scale) {
        float oldScale = this._throttleScale;
        _throttleScale = scale;
        this.firePropertyChange(THROTTLE_SCALE, oldScale, scale);
    }

    int getSearchDepth() {
        return _searchDepth;
    }

    void setSearchDepth(int depth) {
        int oldDepth = this._searchDepth;
        _searchDepth = depth;
        this.firePropertyChange(SEARCH_DEPTH, oldDepth, depth);
    }

    /**
     * @return the time increment
     * @deprecated since 4.7.1; use {@link #getTimeIncrement()} instead
     */
    @Deprecated
    int getTimeIncre() {
        return getTimeIncrement();
    }

    /**
     * @param t the time increment
     * @deprecated since 4.7.1; use {@link #setTimeIncrement(int)} instead
     */
    @Deprecated
    void setTimeIncre(int t) {
        setTimeIncrement(t);
    }

    /**
     * @return the throttle increment
     * @deprecated since 4.7.1; use {@link #getThrottleIncrement()} instead
     */
    @Deprecated
    float getThrottleIncre() {
        return getThrottleIncrement();
    }

    /**
     * @param ti the throttle increment
     * @deprecated since 4.7.1; use {@link #setThrottleIncrement(float)} instead
     */
    @Deprecated
    void setThrottleIncre(float ti) {
        setThrottleIncrement(ti);
    }

    Iterator<Entry<String, Float>> getSpeedNameEntryIterator() {
        List<Entry<String, Float>> vec = new java.util.ArrayList<>();
        _speedNames.entrySet().forEach((entry) -> {
            vec.add(new DataPair<>(entry.getKey(), entry.getValue()));
        });
        return vec.iterator();
    }

    /**
     *
     * @return the number of speed names
     * @deprecated since 4.7.2; use {@link java.util.HashMap#size()} on the
     * result of {@link #getSpeedNames()} instead.
     */
    @Deprecated
    int getSpeedNamesSize() {
        return _speedNames.size();
    }

    Float getSpeedNameValue(String key) {
        return _speedNames.get(key);
    }

    @Nonnull
    @CheckReturnValue
    public HashMap<String, Float> getSpeedNames() {
        return new HashMap<>(this._speedNames);
    }

    public void setSpeedNames(@Nonnull HashMap<String, Float> map) {
        LinkedHashMap<String, Float> old = new LinkedHashMap<>(_speedNames);
        _speedNames.clear();
        _speedNames.putAll(map);
        this.firePropertyChange(SPEED_NAMES, old, new LinkedHashMap<>(_speedNames));
    }

    void setSpeedNames(ArrayList<DataPair<String, Float>> speedNameMap) {
        LinkedHashMap<String, Float> map = new LinkedHashMap<>();
        for (int i = 0; i < speedNameMap.size(); i++) {
            DataPair<String, Float> dp = speedNameMap.get(i);
            map.put(dp.getKey(), dp.getValue());
        }
        this.setSpeedNames(map);
    }

    Iterator<Entry<String, String>> getAppearanceEntryIterator() {
        List<Entry<String, String>> vec = new ArrayList<>();
        _headAppearances.entrySet().stream().forEach((entry) -> {
            vec.add(new DataPair<>(entry.getKey(), entry.getValue()));
        });
        return vec.iterator();
    }

    /**
     *
     * @return the number of signal head appearances
     * @deprecated since 4.7.2; use {@link java.util.HashMap#size()} on the
     * results of {@link #getAppearances()} instead
     */
    @Deprecated
    int getAppeaancesSize() {
        return _headAppearances.size();
    }

    String getAppearanceValue(String key) {
        return _headAppearances.get(key);
    }

    /**
     * Get a map of signal head appearances.
     *
     * @return a map of appearances or an empty map if none are defined
     */
    @Nonnull
    @CheckReturnValue
    public HashMap<String, String> getAppearances() {
        return new HashMap<>(this._headAppearances);
    }

    public void setAppearances(HashMap<String, String> map) {
        LinkedHashMap<String, String> old = new LinkedHashMap<>(this._headAppearances);
        this._headAppearances.clear();
        this._headAppearances.putAll(map);
        this.firePropertyChange(APPEARANCES, old, new LinkedHashMap<>(this._headAppearances));
    }

    void setAppearances(ArrayList<DataPair<String, String>> appearanceMap) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < appearanceMap.size(); i++) {
            DataPair<String, String> dp = appearanceMap.get(i);
            map.put(dp.getKey(), dp.getValue());
        }
        this.setAppearances(map);
    }

    public int getInterpretation() {
        return _interpretation;
    }

    void setInterpretation(int interp) {
        int oldInterpretation = this._interpretation;
        _interpretation = interp;
        this.firePropertyChange(INTERPRETATION, oldInterpretation, interp);
    }

    /**
     * Get the time increment.
     *
     * @return the time increment in milliseconds
     */
    public int getTimeIncrement() {
        return _msIncrTime;
    }

    /**
     * Set the time increment.
     *
     * @param increment the time increment in milliseconds
     */
    public void setTimeIncrement(int increment) {
        int oldIncrement = this._msIncrTime;
        this._msIncrTime = increment;
        this.firePropertyChange(TIME_INCREMENT, oldIncrement, increment);
    }

    /**
     * Get the throttle increment.
     *
     * @return the throttle increment
     */
    public float getThrottleIncrement() {
        return _throttleIncr;
    }

    /**
     * Set the throttle increment.
     *
     * @param increment the throttle increment
     */
    public void setThrottleIncrement(float increment) {
        float oldIncrement = this._throttleIncr;
        this._throttleIncr = increment;
        this.firePropertyChange(RAMP_INCREMENT, oldIncrement, increment);

    }

    @Override
    public void initialize(Profile profile) throws InitializationException {
        if (!this.isInitialized(profile) && !this.isInitializing(profile)) {
            this.setInitializing(profile, true);
            this.openFile(FileUtil.getUserFilesPath() + "signal" + File.separator + "WarrantPreferences.xml");
            this.setInitialized(profile, true);
        }
    }

    @Override
    public void savePreferences(Profile profile) {
        this.save();
    }

    public static class WarrantPreferencesXml extends XmlFile {
    }

    private final static Logger log = LoggerFactory.getLogger(WarrantPreferences.class);
}

package jmri;

import apps.gui3.TabbedPreferences;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import jmri.implementation.DccConsistManager;
import jmri.implementation.NmraConsistManager;
import jmri.jmrit.roster.RosterIconFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides methods for locating various interface implementations. These form
 * the base for locating JMRI objects, including the key managers.
 * <p>
 * The structural goal is to have the jmri package not depend on the lower
 * jmri.jmrit and jmri.jmrix packages, with the implementations still available
 * at run-time through the InstanceManager.
 * <p>
 * To retrieve the default object of a specific type, do
 * {@link InstanceManager#getDefault} where the argument is e.g.
 * "SensorManager.class". In other words, you ask for the default object of a
 * particular type. Note that this call is intended to be used in the usual case
 * of requiring the object to function; it will log a message if there isn't
 * such an object. If that's routine, then use the
 * {@link InstanceManager#getNullableDefault} method instead.
 * <p>
 * Multiple items can be held, and are retrieved as a list with
 * {@link    InstanceManager#getList}.
 * <p>
 * If a specific item is needed, e.g. one that has been constructed via a
 * complex process during startup, it should be installed with
 * {@link InstanceManager#store}.
 * <p>
 * If it's OK for the InstanceManager to create an object on first request, have
 * that object's class implement the {@link InstanceManagerAutoDefault} flag
 * interface. The InstanceManager will then construct a default object via the
 * no-argument constructor when one is first requested.
 * <p>
 * For initialization of more complex default objects, see the
 * {@link InstanceInitializer} mechanism and its default implementation in
 * {@link jmri.managers.DefaultInstanceInitializer}.
 *
 * <hr>
 * This file is part of JMRI.
 * <P>
 * JMRI is free software; you can redistribute it and/or modify it under the
 * terms of version 2 of the GNU General Public License as published by the Free
 * Software Foundation. See the "COPYING" file for a copy of this license.
 * <P>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <P>
 * @author	Bob Jacobsen Copyright (C) 2001, 2008, 2013, 2016
 * @author Matthew Harris copyright (c) 2009
 */
public class InstanceManager {

    protected static final HashMap<Class<?>, ArrayList<Object>> managerLists = new HashMap<>();
    private static final InstanceInitializer initializer = new jmri.managers.DefaultInstanceInitializer();
    // data members to hold contact with the property listeners
    private static final HashSet<PropertyChangeListener> listeners = new HashSet<>();

    /* properties */
    /**
     *
     * @deprecated since 4.5.4 use
     * {@code InstanceManager.getDefaultsPropertyName(ConsistManager.class)}
     * instead.
     */
    @Deprecated
    public static final String CONSIST_MANAGER = "consistmanager"; // NOI18N
    /**
     *
     * @deprecated since 4.5.4 use
     * {@code InstanceManager.getDefaultsPropertyName(ProgrammerManager.class)}
     * instead.
     */
    @Deprecated
    public static final String PROGRAMMER_MANAGER = "programmermanager"; // NOI18N

    /**
     * Store an object of a particular type for later retrieval via
     * {@link #getDefault} or {@link #getList}.
     *
     * @param <T>  The type of the class
     * @param item The object of type T to be stored
     * @param type The class Object for the item's type. This will be used as
     *             the key to retrieve the object later.
     */
    static public <T> void store(@Nonnull T item, @Nonnull Class<T> type) {
        log.debug("Store item of type {}", type.getName());
        if (item == null) {
            NullPointerException npe = new NullPointerException();
            log.error("Should not store null value of type {}", type.getName());
            throw npe;
        }
        ArrayList<T> l = (ArrayList<T>) getList(type);
        l.add(item);
    }

    /**
     * Retrieve a list of all objects of type T that were registered with
     * {@link #store}.
     *
     * @param <T>  The type of the class
     * @param type The class Object for the items' type.
     * @return A list of type Objects registered with the manager or an empty
     *         list.
     */
    @SuppressWarnings("unchecked") // the cast here is protected by the structure of the managerLists
    @Nonnull
    static public <T> List<T> getList(@Nonnull Class<T> type) {
        log.debug("Get list of type {}", type.getName());
        if (managerLists.get(type) == null) {
            managerLists.put(type, new ArrayList<>());
        }
        return (List<T>) managerLists.get(type);
    }

    /**
     * Deregister all objects of a particular type.
     *
     * @param <T>  The type of the class
     * @param type The class Object for the items to be removed.
     */
    static public <T> void reset(@Nonnull Class<T> type) {
        log.debug("Reset type {}", type.getName());
        managerLists.put(type, new ArrayList<>());
    }

    /**
     * Remove an object of a particular type that had earlier been registered
     * with {@link #store}.
     *
     * @param <T>  The type of the class
     * @param item The object of type T to be deregistered
     * @param type The class Object for the item's type.
     */
    static public <T> void deregister(@Nonnull T item, @Nonnull Class<T> type) {
        log.debug("Remove item type {}", type.getName());
        ArrayList<T> l = (ArrayList<T>) getList(type);
        l.remove(item);
    }

    /**
     * Retrieve the last object of type T that was registered with
     * {@link #store(java.lang.Object, java.lang.Class) }.
     * <p>
     * Unless specifically set, the default is the last object stored, see the
     * {@link #setDefault(java.lang.Class, java.lang.Object) } method.
     * <p>
     * In some cases, InstanceManager can create the object the first time it's
     * requested. For more on that, see the class comment.
     * <p>
     * In most cases, system configuration assures the existence of a default
     * object, so this method will log and throw an exception if one doesn't
     * exist. Use {@link #getNullableDefault(java.lang.Class)} or
     * {@link #getOptionalDefault(java.lang.Class)} if the default is not
     * guaranteed to exist.
     *
     * @param <T>  The type of the class
     * @param type The class Object for the item's type
     * @return The default object for type
     * @throws NullPointerException if no default object for type exists
     * @see #getNullableDefault(java.lang.Class)
     * @see #getOptionalDefault(java.lang.Class)
     */
    @Nonnull
    static public <T> T getDefault(@Nonnull Class<T> type) {
        log.trace("getDefault of type {}", type.getName());
        return Objects.requireNonNull(InstanceManager.getNullableDefault(type),
                "Required nonnull default for " + type.getName() + " does not exist.");
    }

    /**
     * Retrieve the last object of type T that was registered with
     * {@link #store(java.lang.Object, java.lang.Class) }.
     * <p>
     * Unless specifically set, the default is the last object stored, see the
     * {@link #setDefault(java.lang.Class, java.lang.Object) } method.
     * <p>
     * In some cases, InstanceManager can create the object the first time it's
     * requested. For more on that, see the class comment.
     * <p>
     * In most cases, system configuration assures the existence of a default
     * object, but this method also handles the case where one doesn't exist.
     * Use {@link #getDefault(java.lang.Class)} when the object is guaranteed to
     * exist.
     *
     * @param <T>  The type of the class
     * @param type The class Object for the item's type.
     * @return The default object for type.
     * @see #getOptionalDefault(java.lang.Class)
     */
    @CheckForNull
    static public <T> T getNullableDefault(@Nonnull Class<T> type) {
        log.trace("getOptionalDefault of type {}", type.getName());
        ArrayList<T> l = (ArrayList<T>) getList(type);
        if (l.isEmpty()) {
            // see if can autocreate
            log.debug("    attempt auto-create of {}", type.getName());
            if (InstanceManagerAutoDefault.class.isAssignableFrom(type)) {
                try {
                    l.add(type.getConstructor((Class[]) null).newInstance((Object[]) null));
                    log.debug("      auto-created default of {}", type.getName());
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    log.error("Exception creating auto-default object", e); // unexpected
                    return null;
                }
                return l.get(l.size() - 1);
            }
            // see if initializer can handle
            log.debug("    attempt initializer create of {}", type.getName());
            @SuppressWarnings("unchecked")
            T obj = (T) initializer.getDefault(type);
            if (obj != null) {
                log.debug("      initializer created default of {}", type.getName());
                l.add(obj);
                return l.get(l.size() - 1);
            }

            // don't have, can't make
            return null;
        }
        return l.get(l.size() - 1);
    }

    /**
     * Retrieve the last object of type T that was registered with
     * {@link #store(java.lang.Object, java.lang.Class)} wrapped in an
     * {@link java.util.Optional}.
     * <p>
     * Unless specifically set, the default is the last object stored, see the
     * {@link #setDefault(java.lang.Class, java.lang.Object)} method.
     * <p>
     * In some cases, InstanceManager can create the object the first time it's
     * requested. For more on that, see the class comment.
     * <p>
     * In most cases, system configuration assures the existence of a default
     * object, but this method also handles the case where one doesn't exist.
     * Use {@link #getDefault(java.lang.Class)} when the object is guaranteed to
     * exist.
     *
     * @param <T>  the type of the default class
     * @param type the class Object for the default type
     * @return the default wrapped in an Optional or an empty Optional if the
     *         default is null
     * @see #getNullableDefault(java.lang.Class)
     */
    @Nonnull
    static public <T> Optional<T> getOptionalDefault(@Nonnull Class<T> type) {
        return Optional.ofNullable(InstanceManager.getNullableDefault(type));
    }

    /**
     * Set an object of type T as the default for that type.
     * <p>
     * Also registers (stores) the object if not already present.
     * <p>
     * Now, we do that moving the item to the back of the list; see the
     * {@link #getDefault} method
     *
     * @param <T>  The type of the class
     * @param type The Class object for val
     * @param item The object to make default for type
     * @return The default for type (normally this is the item passed in)
     */
    @Nonnull
    static public <T> T setDefault(@Nonnull Class<T> type, @Nonnull T item) {
        log.trace("setDefault for type {}", type.getName());
        if (item == null) {
            NullPointerException npe = new NullPointerException();
            log.error("Should not set default of type {} to null value", type.getName());
            throw npe;
        }
        Object oldDefault = getNullableDefault(type);
        List<T> l = getList(type);
        l.remove(item);
        l.add(item);
        if (oldDefault == null || !oldDefault.equals(item)) {
            notifyPropertyChangeListener(getDefaultsPropertyName(type), oldDefault, item);
        }
        return getDefault(type);
    }

    /**
     * Dump generic content of InstanceManager by type.
     *
     * @return A formatted multiline list of managed objects
     */
    @Nonnull
    static public String contentsToString() {

        StringBuilder retval = new StringBuilder();
        for (Class<?> c : managerLists.keySet()) {
            retval.append("List of ");
            retval.append(c);
            retval.append(" with ");
            retval.append(Integer.toString(getList(c).size()));
            retval.append(" objects\n");
            for (Object o : getList(c)) {
                retval.append("    ");
                retval.append(o.getClass().toString());
                retval.append("\n");
            }
        }
        return retval.toString();
    }

    /**
     * Remove notification on changes to specific types
     *
     * @param l The listener to remove.
     */
    public static synchronized void removePropertyChangeListener(PropertyChangeListener l) {
        if (listeners.contains(l)) {
            listeners.remove(l);
        }
    }

    /**
     * Register for notification on changes to specific types
     *
     * @param l The listener to add.
     */
    public static synchronized void addPropertyChangeListener(PropertyChangeListener l) {
        // add only if not already registered
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    protected static void notifyPropertyChangeListener(String property, Object oldValue, Object newValue) {
        // make a copy of the listener vector to synchronized not needed for transmit
        HashSet<PropertyChangeListener> set;
        synchronized (InstanceManager.class) {
            set = new HashSet<>(listeners);
        }
        // forward to all listeners
        set.stream().forEach((listener) -> {
            listener.propertyChange(new PropertyChangeEvent(InstanceManager.class, property, oldValue, newValue));
        });
    }

    /**
     * Get the property name included in the
     * {@link java.beans.PropertyChangeEvent} thrown when the default for a
     * specific class is changed.
     *
     * @param clazz the class being listened for
     * @return the property name
     */
    public static String getDefaultsPropertyName(Class<?> clazz) {
        return "default-" + clazz.getName();
    }

    /* ****************************************************************************
     *                   Primary Accessors - Left (for now)
     *
     *          These are so extensively used that we're leaving for later
     *                      Please don't create any more of these 
     * ****************************************************************************/
    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default light manager. May not be the only instance.
     */
    static public LightManager lightManagerInstance() {
        return getDefault(LightManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default memory manager. May not be the only instance.
     */
    static public MemoryManager memoryManagerInstance() {
        return getDefault(MemoryManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default sensor manager. May not be the only instance.
     */
    static public SensorManager sensorManagerInstance() {
        return getDefault(SensorManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default turnout manager. May not be the only instance.
     */
    static public TurnoutManager turnoutManagerInstance() {
        return getDefault(TurnoutManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default throttle manager. May not be the only instance.
     */
    static public ThrottleManager throttleManagerInstance() {
        return getDefault(ThrottleManager.class);
    }

    /* ****************************************************************************
     *                   Primary Accessors - Deprecated for removal
     *
     *                      Please don't create any more of these 
     * ****************************************************************************/
    // Simplification order - for each type, starting with those not in the jmri package:
    //   1) Remove it from jmri.managers.DefaultInstanceInitializer, get tests to build & run
    //   2) Remove the setter from here, get tests to build & run
    //   3) Remove the accessor from here, get tests to build & run
    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default audio manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public AudioManager audioManagerInstance() {
        return getDefault(AudioManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default block manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public BlockManager blockManagerInstance() {
        return getDefault(BlockManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default clock control instance. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public ClockControl clockControlInstance() {
        return getDefault(ClockControl.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default conditional manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public ConditionalManager conditionalManagerInstance() {
        return getDefault(ConditionalManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default logix manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public LogixManager logixManagerInstance() {
        return getDefault(LogixManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default power manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public PowerManager powerManagerInstance() {
        return getDefault(PowerManager.class);
    }

    /**
     * @return the default programmer manager. May not be the only instance.
     * @deprecated Since 3.11.1, use @{link #getDefault} for either
     * GlobalProgrammerManager or AddressedProgrammerManager directly
     * @deprecated 4.5.1
     */
    @Deprecated
    static public ProgrammerManager programmerManagerInstance() {
        return getDefault(ProgrammerManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default reporter manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public ReporterManager reporterManagerInstance() {
        return getDefault(ReporterManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default roster icon factory. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public RosterIconFactory rosterIconFactoryInstance() {
        return getDefault(RosterIconFactory.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default route manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public RouteManager routeManagerInstance() {
        return getDefault(RouteManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default section manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public SectionManager sectionManagerInstance() {
        return getDefault(SectionManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default signal group manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public SignalGroupManager signalGroupManagerInstance() {
        return getDefault(SignalGroupManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default signal head manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public SignalHeadManager signalHeadManagerInstance() {
        return getDefault(SignalHeadManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default signal mast manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public SignalMastManager signalMastManagerInstance() {
        return getDefault(SignalMastManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default signal system manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public SignalSystemManager signalSystemManagerInstance() {
        return getDefault(SignalSystemManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default signal mast logic manager. May not be the only
     *         instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public SignalMastLogicManager signalMastLogicManagerInstance() {
        return getDefault(SignalMastLogicManager.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default preferences panel. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public TabbedPreferences tabbedPreferencesInstance() {
        return getDefault(TabbedPreferences.class);
    }

    /**
     * Will eventually be deprecated, use @{link #getDefault} directly.
     *
     * @return the default transit manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public TransitManager transitManagerInstance() {
        return getDefault(TransitManager.class);
    }

    /* ****************************************************************************
     *         Deprecated Accessors - removed from JMRI itself
     *
     *             Remove these in or after JMRI 4.8.1
     *                 (Check scripts first)
     * ****************************************************************************/
    /**
     * Deprecated, use @{link #getDefault} directly.
     *
     * @return the default consist manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public ConsistManager consistManagerInstance() {
        return getDefault(ConsistManager.class);
    }

    /**
     * Deprecated, use @{link #getDefault} directly.
     *
     * @return the default command station. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public CommandStation commandStationInstance() {
        return getDefault(CommandStation.class);
    }

    /**
     * Deprecated, use @{link #getDefault} directly.
     *
     * @return the default configure manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public ConfigureManager configureManagerInstance() {
        return getDefault(ConfigureManager.class);
    }

    /**
     * Deprecated, use @{link #getDefault} directly.
     *
     * @return the default shutdown manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public ShutDownManager shutDownManagerInstance() {
        return getDefault(ShutDownManager.class);
    }

    /**
     * Deprecated, use @{link #getDefault} directly.
     *
     * @return the default catalog tree manager. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public CatalogTreeManager catalogTreeManagerInstance() {
        return getDefault(CatalogTreeManager.class);
    }

    /**
     * Deprecated, use @{link #getDefault} directly.
     *
     * @return the default Timebase. May not be the only instance.
     * @deprecated 4.5.1
     */
    @Deprecated
    static public Timebase timebaseInstance() {
        return getDefault(Timebase.class);
    }

    /* ****************************************************************************
     *                   Old Style Setters - To be migrated
     *
     *                   Migrate JMRI uses of these, then move to next category
     * ****************************************************************************/
    /**
     * @param p clock control to make default
     * @deprecated Since 3.7.1, use
     * {@link #setDefault(java.lang.Class, java.lang.Object)} directly.
     */
    @Deprecated
    static public void addClockControl(ClockControl p) {
        store(p, ClockControl.class);
        setDefault(ClockControl.class, p);
    }

    // Needs to have proxy manager converted to work
    // with current list of managers (and robust default
    // management) before this can be deprecated in favor of
    // store(p, TurnoutManager.class)
    static public void setTurnoutManager(TurnoutManager p) {
        log.debug(" setTurnoutManager");
        ((jmri.managers.AbstractProxyManager) getDefault(TurnoutManager.class)).addManager(p);
        //store(p, TurnoutManager.class);
    }

    /**
     * @param p shutdown manager to make default
     * @deprecated Since 3.7.4, use
     * {@link #setDefault(java.lang.Class, java.lang.Object)} directly.
     */
    @Deprecated
    static public void setShutDownManager(ShutDownManager p) {
        store(p, ShutDownManager.class);
        setDefault(ShutDownManager.class, p);
    }

    static public void setThrottleManager(ThrottleManager p) {
        store(p, ThrottleManager.class);
    }

    /**
     * @param p signal head manager to make default
     * @deprecated Since 3.7.4, use
     * {@link #setDefault(java.lang.Class, java.lang.Object)} directly.
     */
    @Deprecated
    static public void setSignalHeadManager(SignalHeadManager p) {
        store(p, SignalHeadManager.class);
        setDefault(SignalHeadManager.class, p);
    }

    //
    // This updates the consist manager, which must be
    // either built into instances of calling code or a 
    // new service, before this can be deprecated.
    //
    static public void setCommandStation(CommandStation p) {
        store(p, CommandStation.class);

        // since there is a command station available, use
        // the NMRA consist manager instead of the generic consist
        // manager.
        if (getNullableDefault(ConsistManager.class) == null
                || getDefault(ConsistManager.class).getClass() == DccConsistManager.class) {
            setConsistManager(new NmraConsistManager());
        }
    }

    /**
     * @param p configure manager to make default
     * @deprecated Since 3.7.4, use
     * {@link #setDefault(java.lang.Class, java.lang.Object)} directly.
     */
    @Deprecated
    static public void setConfigureManager(ConfigureManager p) {
        log.debug(" setConfigureManager");
        store(p, ConfigureManager.class);
        setDefault(ConfigureManager.class, p);
    }

    //
    // This provides notification services, which 
    // must be migrated before this method can be 
    // deprecated.
    //
    static public void setConsistManager(ConsistManager p) {
        store(p, ConsistManager.class);
        notifyPropertyChangeListener(CONSIST_MANAGER, null, null);
    }

    // Needs to have proxy manager converted to work
    // with current list of managers (and robust default
    // management) before this can be deprecated in favor of
    // store(p, TurnoutManager.class)
    static public void setLightManager(LightManager p) {
        log.debug(" setLightManager");
        ((jmri.managers.AbstractProxyManager) getDefault(LightManager.class)).addManager(p);
        //store(p, LightManager.class);
    }

    //
    // Note: Also provides consist manager services on store operation.
    // Do we need a new mechanism for this? Or just move this code to
    // the 30+ classes that reference it? Or maybe have a default of the 
    // DccConsistManager that's smarter?
    //
    //
    // This provides notification services, which 
    // must be migrated before this method can be 
    // deprecated.
    //
    static public void setProgrammerManager(ProgrammerManager p) {
        if (p.isAddressedModePossible()) {
            store(p, AddressedProgrammerManager.class);
        }
        if (p.isGlobalProgrammerAvailable()) {
            store(p, GlobalProgrammerManager.class);
        }

        // Now that we have a programmer manager, install the default
        // Consist manager if Ops mode is possible, and there isn't a
        // consist manager already.
        if (programmerManagerInstance().isAddressedModePossible()
                && getNullableDefault(ConsistManager.class) == null) {
            setConsistManager(new DccConsistManager());
        }
        notifyPropertyChangeListener(PROGRAMMER_MANAGER, null, null);
    }

    // Needs to have proxy manager converted to work
    // with current list of managers (and robust default
    // management) before this can be deprecated in favor of
    // store(p, ReporterManager.class)
    static public void setReporterManager(ReporterManager p) {
        log.debug(" setReporterManager");
        ((jmri.managers.AbstractProxyManager) getDefault(ReporterManager.class)).addManager(p);
        //store(p, ReporterManager.class);
    }

    // Needs to have proxy manager converted to work
    // with current list of managers (and robust default
    // management) before this can be deprecated in favor of
    // store(p, SensorManager.class)
    static public void setSensorManager(SensorManager p) {
        log.debug(" setSensorManager");
        ((jmri.managers.AbstractProxyManager) getDefault(SensorManager.class)).addManager(p);
        //store(p, SensorManager.class);
    }

    /* ****************************************************************************
     *                   Old Style Setters - Deprecated and migrated, 
     *                                       just here for other users
     *
     *                     Check Jython scripts before removing
     * ****************************************************************************/
    ///**
    // * @deprecated Since 3.7.1, use @{link #store} and @{link #setDefault} directly.
    // */
    //@Deprecated
    //static public void setConditionalManager(ConditionalManager p) {
    //    store(p, ConditionalManager.class);
    //    setDefault(ConditionalManager.class, p);
    //}
    ///**
    // * @deprecated Since 3.7.4, use @{link #store} directly.
    // */
    //@Deprecated
    //static public void setLogixManager(LogixManager p) {
    //    store(p, LogixManager.class);
    //}
    ///**
    // * @deprecated Since 3.7.4, use @{link #store} directly.
    // */
    //@Deprecated
    //static public void setTabbedPreferences(TabbedPreferences p) {
    //    store(p, TabbedPreferences.class);
    //}
    ///**
    // * @deprecated Since 3.7.1, use @{link #store} and @{link #setDefault}
    // * directly.
    // */
    // @Deprecated
    // static public void setPowerManager(PowerManager p) {
    //     store(p, PowerManager.class);
    // }

    /* *************************************************************************** */
    private final static Logger log = LoggerFactory.getLogger(InstanceManager.class.getName());
}

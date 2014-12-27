// TrainByCarTypeFrame.java

package jmri.jmrit.operations.trains;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jmri.jmrit.operations.rollingstock.RollingStock;
import jmri.jmrit.operations.rollingstock.cars.CarTypes;
import jmri.jmrit.operations.rollingstock.cars.CarManager;
import jmri.jmrit.operations.rollingstock.cars.Car;
import jmri.jmrit.operations.locations.LocationManager;
import jmri.jmrit.operations.locations.Location;
import jmri.jmrit.operations.locations.Schedule;
import jmri.jmrit.operations.locations.ScheduleItem;
import jmri.jmrit.operations.locations.Track;
import jmri.jmrit.operations.routes.Route;
import jmri.jmrit.operations.routes.RouteLocation;
import jmri.jmrit.operations.OperationsFrame;

import java.awt.*;

import javax.swing.*;

import java.text.MessageFormat;
import java.util.List;

/**
 * Frame to display by rolling stock, the locations serviced by this train
 * 
 * @author Dan Boudreau Copyright (C) 2010, 2013, 2014
 * @version $Revision$
 */

public class TrainByCarTypeFrame extends OperationsFrame implements java.beans.PropertyChangeListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5894248098711372139L;

	// train
	Train _train;

	LocationManager locationManager = LocationManager.instance();

	// panels
	JPanel pRoute = new JPanel();

	// radio buttons

	// combo boxes
	JComboBox<Train> trainsComboBox = TrainManager.instance().getTrainComboBox();
	JComboBox<String> typeComboBox = CarTypes.instance().getComboBox();
	JComboBox<Car> carsComboBox = new JComboBox<>();

	// The car currently selected
	Car _car;

	public TrainByCarTypeFrame() {
		super();
	}

	public void initComponents(Train train) {

		_train = train;

		// general GUI config
		getContentPane().setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

		// Set up the panels
		JPanel pTrain = new JPanel();
		pTrain.setLayout(new GridBagLayout());
		pTrain.setBorder(BorderFactory.createTitledBorder(Bundle.getMessage("Train")));
		pTrain.setMaximumSize(new Dimension(2000, 50));
		
		addItem(pTrain, trainsComboBox, 0, 0);
		trainsComboBox.setSelectedItem(train);
		
		JPanel pCarType = new JPanel();
		pCarType.setLayout(new GridBagLayout());
		pCarType.setBorder(BorderFactory.createTitledBorder(Bundle.getMessage("Type")));
		pCarType.setMaximumSize(new Dimension(2000, 50));

		addItem(pCarType, typeComboBox, 0, 0);
		addItem(pCarType, carsComboBox, 1, 0);

		// increase width of combobox so large text names display properly
		Dimension boxsize = typeComboBox.getMinimumSize();
		if (boxsize != null) {
			boxsize.setSize(boxsize.width + 10, boxsize.height);
			typeComboBox.setMinimumSize(boxsize);
		}

		adjustCarsComboBoxSize();

		pRoute.setLayout(new GridBagLayout());
		JScrollPane locationPane = new JScrollPane(pRoute);
		locationPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		locationPane.setBorder(BorderFactory.createTitledBorder(Bundle.getMessage("Route")));
		updateCarsComboBox();
		updateRoute();

		getContentPane().add(pTrain);
		getContentPane().add(pCarType);
		getContentPane().add(locationPane);

		// setup combo box
		addComboBoxAction(trainsComboBox);
		addComboBoxAction(typeComboBox);
		addComboBoxAction(carsComboBox);

		locationManager.addPropertyChangeListener(this);
		CarTypes.instance().addPropertyChangeListener(this);
		// listen to all tracks and locations
		addLocationAndTrackPropertyChange();
		
		addHelpMenu("package.jmri.jmrit.operations.Operations_TrainShowCarTypesServiced", true); // NOI18N

		setPreferredSize(null);
		initMinimumSize();
	}

	public void comboBoxActionPerformed(java.awt.event.ActionEvent ae) {
		log.debug("combo box action");
		if (ae.getSource().equals(typeComboBox))
			updateCarsComboBox();
		updateRoute();
	}

	private void updateRoute() {
		if (_train != null)
			_train.removePropertyChangeListener(this);
		
		pRoute.removeAll();
		
		if (trainsComboBox.getSelectedItem() == null || trainsComboBox.getSelectedItem().equals(TrainManager.NONE)) {
			_train = null;
		} else {
			_train = (Train) trainsComboBox.getSelectedItem();
		}
		
		if (_train == null) {
			setTitle(Bundle.getMessage("MenuItemShowCarTypes"));
			repaint();
			return;		
		}
		
		setTitle(Bundle.getMessage("MenuItemShowCarTypes") + " " + _train.getName());
		_train.addPropertyChangeListener(this);
		setTitle(Bundle.getMessage("MenuItemShowCarTypes") + " " + _train.getName());
		log.debug("update locations served by train {}", _train.getName());
		
		int y = 0;
		String carType = (String) typeComboBox.getSelectedItem();
		if (_car != null)
			_car.removePropertyChangeListener(this);
		_car = null;
		if (carsComboBox.getSelectedItem() != null) {
			_car = (Car) carsComboBox.getSelectedItem();
			_car.addPropertyChangeListener(this);
		}
		Route route = _train.getRoute();
		if (route == null)
			return;
		List<RouteLocation> routeList = route.getLocationsBySequenceList();
		for (RouteLocation rl : routeList) {
			JLabel loc = new JLabel();
			String locationName = rl.getName();
			loc.setText(locationName);
			addItemLeft(pRoute, loc, 0, y++);
			Location location = locationManager.getLocationByName(locationName);
			if (_car != null && _car.getTrack() != null && !_car.getTrack().acceptsDestination(location) && _car.getLocation() != location) {
				JLabel locText = new JLabel();
				locText.setText(MessageFormat.format(Bundle.getMessage("CarOnTrackDestinationRestriction"),
						new Object[] { _car.toString(), _car.getTrackName() }));
				addItemWidth(pRoute, locText, 2, 1, y++);
				continue;
			}
			List<Track> tracks = location.getTrackByNameList(null);
			for (Track track : tracks) {
				// show the car's track if there's a track destination restriction
				if (_car != null && _car.getTrack() != null && !_car.getTrack().acceptsDestination(location)
						&& _car.getTrack() != track) {
					continue;
				}
				JLabel trk = new JLabel();
				trk.setText(track.getName());
				addItemLeft(pRoute, trk, 1, y);
				// is the car at this location and track?
				if (_car != null && location.equals(_car.getLocation()) && track.equals(_car.getTrack())) {
					JLabel here = new JLabel("  -->"); // NOI18N
					addItemLeft(pRoute, here, 0, y);
				}
				JLabel op = new JLabel();
				addItemLeft(pRoute, op, 2, y++);
				if (!_train.acceptsTypeName(carType))
					op.setText(Bundle.getMessage("X(TrainType)"));
				else if (_car != null && !_train.acceptsRoadName(_car.getRoadName()))
					op.setText(Bundle.getMessage("X(TrainRoad)"));
				// TODO need to do the same tests for caboose changes in the train's route
				else if (_car != null && _car.isCaboose() && (_train.getRequirements() & Train.CABOOSE) > 0
						&& location.equals(_car.getLocation()) && track.equals(_car.getTrack())
						&& !_train.getCabooseRoad().equals("") && !_car.getRoadName().equals(_train.getCabooseRoad())
						&& location.getName().equals(_train.getTrainDepartsName()))
					op.setText(Bundle.getMessage("X(TrainRoad)"));
				else if (_car != null && _car.hasFred() && (_train.getRequirements() & Train.FRED) > 0
						&& location.equals(_car.getLocation()) && track.equals(_car.getTrack())
						&& !_train.getCabooseRoad().equals("") && !_car.getRoadName().equals(_train.getCabooseRoad())
						&& location.getName().equals(_train.getTrainDepartsName()))
					op.setText(Bundle.getMessage("X(TrainRoad)"));
				else if (_car != null && !_car.isCaboose() && !_car.isPassenger()
						&& !_train.acceptsLoad(_car.getLoadName(), _car.getTypeName()))
					op.setText(Bundle.getMessage("X(TrainLoad)"));
				else if (_car != null && !_train.acceptsBuiltDate(_car.getBuilt()))
					op.setText(Bundle.getMessage("X(TrainBuilt)"));
				else if (_car != null && !_train.acceptsOwnerName(_car.getOwner()))
					op.setText(Bundle.getMessage("X(TrainOwner)"));
				else if (_train.skipsLocation(rl.getId()))
					op.setText(Bundle.getMessage("X(TrainSkips)"));
				else if (!rl.isDropAllowed() && !rl.isPickUpAllowed())
					op.setText(Bundle.getMessage("X(Route)"));
				else if (rl.getMaxCarMoves() <= 0)
					op.setText(Bundle.getMessage("X(RouteMoves)"));
				else if (!location.acceptsTypeName(carType))
					op.setText(Bundle.getMessage("X(LocationType)"));
				// check route before checking train, check train calls check route
				else if (!track.acceptsPickupRoute(route) && !track.acceptsDropRoute(route))
					op.setText(Bundle.getMessage("X(TrackRoute)"));
				else if (!track.acceptsPickupTrain(_train) && !track.acceptsDropTrain(_train))
					op.setText(Bundle.getMessage("X(TrackTrain)"));
				else if (!track.acceptsTypeName(carType))
					op.setText(Bundle.getMessage("X(TrackType)"));
				else if (_car != null && !track.acceptsRoadName(_car.getRoadName()))
					op.setText(Bundle.getMessage("X(TrackRoad)"));
				else if (_car != null && !track.acceptsLoad(_car.getLoadName(), _car.getTypeName()))
					op.setText(Bundle.getMessage("X(TrackLoad)"));
				else if (_car != null && !track.acceptsDestination(_car.getFinalDestination()))
					op.setText(Bundle.getMessage("X(TrackDestination)"));
				else if ((rl.getTrainDirection() & location.getTrainDirections()) == 0)
					op.setText(Bundle.getMessage("X(DirLoc)"));
				else if ((rl.getTrainDirection() & track.getTrainDirections()) == 0)
					op.setText(Bundle.getMessage("X(DirTrk)"));
				else if (!checkScheduleAttribute(TYPE, carType, null, track))
					op.setText(Bundle.getMessage("X(ScheduleType)"));
				else if (!checkScheduleAttribute(LOAD, carType, _car, track))
					op.setText(Bundle.getMessage("X(ScheduleLoad)"));
				else if (!checkScheduleAttribute(ROAD, carType, _car, track))
					op.setText(Bundle.getMessage("X(ScheduleRoad)"));
				else if (!checkScheduleAttribute(TIMETABLE, carType, _car, track))
					op.setText(Bundle.getMessage("X(ScheduleTimeTable)"));
				else if (!checkScheduleAttribute(ALL, carType, _car, track))
					op.setText(Bundle.getMessage("X(Schedule)"));
				else if (!track.acceptsPickupTrain(_train)) {
					// can the train drop off car?
					if (rl.isDropAllowed() && track.acceptsDropTrain(_train))
						op.setText(Bundle.getMessage("DropOnly"));
					else
						op.setText(Bundle.getMessage("X(TrainPickup)"));
				} else if (!track.acceptsDropTrain(_train))
					// can the train pick up car?
					if (rl.isPickUpAllowed() && track.acceptsPickupTrain(_train))
						op.setText(Bundle.getMessage("PickupOnly"));
					else
						op.setText(Bundle.getMessage("X(TrainDrop)"));
				else if (rl.isDropAllowed() && rl.isPickUpAllowed())
					op.setText(Bundle.getMessage("OK"));
				else if (rl.isDropAllowed())
					op.setText(Bundle.getMessage("DropOnly"));
				else if (rl.isPickUpAllowed())
					op.setText(Bundle.getMessage("PickupOnly"));
				else
					op.setText("X"); // default shouldn't occur
			}
		}
		pRoute.revalidate();
		repaint();
	}

	private static final String ROAD = "road"; // NOI18N
	private static final String LOAD = "load"; // NOI18N
	private static final String TIMETABLE = "timetable"; // NOI18N
	private static final String TYPE = "type"; // NOI18N
	private static final String ALL = "all"; // NOI18N

	private boolean checkScheduleAttribute(String attribute, String carType, Car car, Track track) {
		Schedule schedule = track.getSchedule();
		if (schedule == null)
			return true;
		// if car is already placed at track, don't check car type and load
		if (car != null && car.getTrack() == track)
			return true;
		List<ScheduleItem> scheduleItems = schedule.getItemsBySequenceList();
		for (ScheduleItem si : scheduleItems) {
			// check to see if schedule services car type
			if (attribute.equals(TYPE) && si.getTypeName().equals(carType))
				return true;
			// check to see if schedule services car type and load
			if (attribute.equals(LOAD)
					&& si.getTypeName().equals(carType)
					&& (si.getReceiveLoadName().equals("") || car == null || si.getReceiveLoadName().equals(
							car.getLoadName())))
				return true;
			// check to see if schedule services car type and road
			if (attribute.equals(ROAD) && si.getTypeName().equals(carType)
					&& (si.getRoadName().equals("") || car == null || si.getRoadName().equals(car.getRoadName())))
				return true;
			// check to see if schedule timetable allows delivery
			if (attribute.equals(TIMETABLE)
					&& si.getTypeName().equals(carType)
					&& (si.getSetoutTrainScheduleId().equals("") || TrainManager.instance().getTrainScheduleActiveId()
							.equals(si.getSetoutTrainScheduleId())))
				return true;
			// check to see if at least one schedule item can service car
			if (attribute.equals(ALL)
					&& si.getTypeName().equals(carType)
					&& (si.getReceiveLoadName().equals("") || car == null || si.getReceiveLoadName().equals(
							car.getLoadName()))
					&& (si.getRoadName().equals("") || car == null || si.getRoadName().equals(car.getRoadName()))
					&& (si.getSetoutTrainScheduleId().equals("") || TrainManager.instance().getTrainScheduleActiveId()
							.equals(si.getSetoutTrainScheduleId())))
				return true;
		}
		return false;
	}

	private void updateComboBox() {
		log.debug("update combobox");
		CarTypes.instance().updateComboBox(typeComboBox);
	}

	private void updateCarsComboBox() {
		log.debug("update car combobox");
		carsComboBox.removeAllItems();
		String carType = (String) typeComboBox.getSelectedItem();
		// load car combobox
		carsComboBox.addItem(null);
		List<RollingStock> cars = CarManager.instance().getByTypeList(carType);
		for (RollingStock rs : cars) {
			Car car = (Car) rs;
			carsComboBox.addItem(car);
		}
	}

	private void adjustCarsComboBoxSize() {
		List<RollingStock> cars = CarManager.instance().getList();
		for (RollingStock rs : cars) {
			Car car = (Car) rs;
			carsComboBox.addItem(car);
		}
		Dimension boxsize = carsComboBox.getMinimumSize();
		if (boxsize != null) {
			boxsize.setSize(boxsize.width + 10, boxsize.height);
			carsComboBox.setMinimumSize(boxsize);
		}
		carsComboBox.removeAllItems();
	}

	/**
	 * Add property listeners for locations and tracks
	 */
	private void addLocationAndTrackPropertyChange() {
		for (Location loc : locationManager.getList()) {
			loc.addPropertyChangeListener(this);
			for (Track track : loc.getTrackList()) {
				track.addPropertyChangeListener(this);
				Schedule schedule = track.getSchedule();
				if (schedule != null)
					schedule.addPropertyChangeListener(this);
			}
		}
	}

	/**
	 * Remove property listeners for locations and tracks
	 */
	private void removeLocationAndTrackPropertyChange() {
		for (Location loc : locationManager.getList()) {
			loc.removePropertyChangeListener(this);
			for (Track track : loc.getTrackList()) {
				track.removePropertyChangeListener(this);
				Schedule schedule = track.getSchedule();
				if (schedule != null)
					schedule.removePropertyChangeListener(this);
			}
		}
	}

	public void dispose() {
		locationManager.removePropertyChangeListener(this);
		CarTypes.instance().removePropertyChangeListener(this);
		removeLocationAndTrackPropertyChange();
		if (_train != null)
			_train.removePropertyChangeListener(this);
		if (_car != null)
			_car.removePropertyChangeListener(this);
		super.dispose();
	}

	public void propertyChange(java.beans.PropertyChangeEvent e) {
		log.debug("Property change ({}) old: ({}) new: ({})", e.getPropertyName(), e.getOldValue(), e.getNewValue()); // NOI18N
		if (e.getSource().equals(_car) || e.getSource().equals(_train))
			updateRoute();
		if (e.getSource().getClass().equals(Track.class) || e.getSource().getClass().equals(Location.class)
				|| e.getSource().getClass().equals(Schedule.class))
			updateRoute();
		if (e.getPropertyName().equals(LocationManager.LISTLENGTH_CHANGED_PROPERTY)
				|| e.getPropertyName().equals(Route.LISTCHANGE_CHANGED_PROPERTY))
			updateRoute();
		if (e.getPropertyName().equals(Train.DISPOSE_CHANGED_PROPERTY))
			dispose();
		if (e.getPropertyName().equals(CarTypes.CARTYPES_CHANGED_PROPERTY)
				|| e.getPropertyName().equals(CarTypes.CARTYPES_NAME_CHANGED_PROPERTY))
			updateComboBox();
		if (e.getPropertyName().equals(Location.LENGTH_CHANGED_PROPERTY)) {
			// a track has been add or deleted update property listeners
			removeLocationAndTrackPropertyChange();
			addLocationAndTrackPropertyChange();
		}
	}

	static Logger log = LoggerFactory.getLogger(TrainByCarTypeFrame.class.getName());
}

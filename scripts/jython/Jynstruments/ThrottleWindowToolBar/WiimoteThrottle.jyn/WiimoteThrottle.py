import jmri.jmrit.jython.Jynstrument as Jynstrument
import java.awt.CardLayout as CardLayout
import jmri.util.ResizableImagePanel as ResizableImagePanel
import java.beans.PropertyChangeListener as PropertyChangeListener
import jmri.jmrit.throttle.AddressListener as AddressListener
import javax.swing.Timer as Timer
import java.awt.event.ActionListener as ActionListener
import java.util.Calendar as Calendar
import java.lang.Runnable as Runnable
import thread
import javax.swing.SwingUtilities as SwingUtilities
import wiiremotej.event.WiiRemoteListener as WiiRemoteListener
import wiiremotej.event.WiiDeviceDiscoveryListener as WiiDeviceDiscoveryListener
import wiiremotej.WiiRemoteJ as WiiRemoteJ
import wiiremotej.event.WRButtonEvent as WRButtonEvent

# Use a Nintendo Wiimote device as a throttle
# You need to have Bluecove and WiiRemoteJ jars in classpath, JMRI lib folder is best place for that
# See: http://bluecove.org/ and http://www.world-of-cha0s.hostrocket.com/WiiRemoteJ/
#
# Author: Lionel Jeanson copyright 2009
# Part of the JMRI distribution

valueSpeedTimerRepeat = 125 # repeat time in ms for speed set task
valueSpeedIncrement = 0.05

delay4double = 500 # delay for double tap on button (ms)
speedEStopSpeed = -1
speedStopSpeed = 0
speedSlowSpeed = 0.3
speedCruiseSpeed = 0.8
speedMaxSpeed = 1

class WiimoteThrottle(Jynstrument, PropertyChangeListener, AddressListener, WiiDeviceDiscoveryListener, WiiRemoteListener, Runnable):
    #Wiimote discoverer events
    def findFinished(self, nb):
        print "Search finished, found ",nb ," wiimotes"

    def wiiDeviceDiscovered(self, evt):
        print "Found a Wiimote, number: ", evt.getNumber()
        self.wiiDevice = evt.getWiiDevice()
        ledLights = [False, False, False, False]
        ledLights[evt.getNumber()] = True
        self.wiiDevice.setLEDLights(ledLights)
        self.wiiDevice.addWiiRemoteListener(self)

    #Wiimote events        
    def buttonInputReceived(self, evt):
        print("Wiimote Button event: ", evt)
        self.sync.acquire()
        self.evt = evt
        self.sync.release()
        SwingUtilities.invokeLater(self) # Delegate processing to Swing thread (when we are here, we're in the WiiRemoteJ driver thread)

    def run(self):
        self.sync.acquire()
        evt = self.evt
        self.sync.release()
        self.speedTimer.stop() # In any case
        # ThrottleFrames
        if ( evt.wasReleased(WRButtonEvent.RIGHT) ): # NEXT
             self.getContext().nextThrottleFrame()
        if ( evt.wasReleased(WRButtonEvent.LEFT) ):  # PREVIOUS
            self.getContext().previousThrottleFrame()
        if (self.throttle != None):
            if ( evt.wasReleased(WRButtonEvent.HOME) ):  # LIGHTS
                self.throttle.setF0( not self.throttle.getF0() )
                return
            if ( evt.wasReleased(WRButtonEvent.PLUS) ):  # FORWARD
                self.throttle.setIsForward(True)
                return
            if ( evt.wasReleased(WRButtonEvent.MINUS) ):  # BACKWARD
                self.throttle.setIsForward(False)
                return
            # Speed control
            if ( evt.isPressed(WRButtonEvent.B) ): # SPEED - increment
                self.speedAction.setSpeedIncrement( valueSpeedIncrement )
                self.speedTimer.start()
                return
            if ( evt.isPressed(WRButtonEvent.A) ): # SPEED - decrement
                self.speedAction.setSpeedIncrement( -valueSpeedIncrement )
                self.speedTimer.start()
                return
            # EStop
            if ( evt.isPressed( WRButtonEvent.ONE | WRButtonEvent.TWO ) ): # estop = button1 + button2
                self.throttle.setSpeedSetting( speedEStopSpeed )
                self.lastTimeEStop = Calendar.getInstance().getTimeInMillis() # To cancel next inputs
                self.wiiDevice.vibrateFor(750)
                return
            # Speed presets
            if (Calendar.getInstance().getTimeInMillis() - self.lastTimeEStop > delay4double): # Delay for nothing after EStop
                if (( evt.wasReleased(WRButtonEvent.TWO) ) and           #STOP = button2 x2 or (button2 and curentSpeed = slow speed)
                    ( (Calendar.getInstance().getTimeInMillis() - self.lastTimeButton2 < delay4double) or ( self.throttle.getSpeedSetting() == speedSlowSpeed ))):  
                    self.throttle.setSpeedSetting( speedStopSpeed )
                    return
                if ( evt.wasReleased(WRButtonEvent.TWO) ):               # SLOW SPEED = button2
                    self.throttle.setSpeedSetting( speedSlowSpeed )
                    self.lastTimeButton2 = Calendar.getInstance().getTimeInMillis()
                    return
                if (( evt.wasReleased(WRButtonEvent.ONE) ) and           # MAX SPEED = button1x2 or (button1 and curentSpeed = cruise speed)
                    ( (Calendar.getInstance().getTimeInMillis() - self.lastTimeButton1 < delay4double) or ( self.throttle.getSpeedSetting() == speedCruiseSpeed ))): 
                    self.throttle.setSpeedSetting( speedMaxSpeed )
                    return
                if ( evt.wasReleased( WRButtonEvent.ONE) ):              # CRUISE SPEED = button1
                    self.throttle.setSpeedSetting( speedCruiseSpeed )
                    self.lastTimeButton1 = Calendar.getInstance().getTimeInMillis()
                    return

    def disconnected(self):
        self.wiiDevice = None
        print("Lost wiimote")
        
    def accelerationInputReceived(self, evt):
        pass
    def combinedInputReceived(self, evt):
        pass
    def	extensionConnected(self, extension):
        pass          
    def	extensionDisconnected(self, extension):
        pass          
    def extensionInputReceived(self, evt):
        pass
    def	extensionPartiallyInserted(self):
        pass
    def extensionUnknown(self):
        pass
    def IRInputReceived(self, evt):
        pass
    def statusReported(self, evt):
        print("Wiimote status reported: ", evt)

#Property listener part
    def propertyChange(self, event):
        self.speedTimer.stop()                     
        if (event.propertyName == "ThrottleFrame") :  # Curent throttle frame changed
            event.oldValue.getAddressPanel().removeAddressListener(self)
            self.throttle = event.newValue.getAddressPanel().getThrottle()
            self.speedAction.setThrottle( self.throttle )
            event.newValue.getAddressPanel().addAddressListener(self)

#Jynstrument main and mandatory methods
    def getExpectedContextClassName(self):
        return "jmri.jmrit.throttle.ThrottleWindow"
    
    def init(self):
        self.getContext().addPropertyChangeListener(self) #ThrottleFrame change
        self.getContext().getCurentThrottleFrame().getAddressPanel().addAddressListener(self) # change of throttle in curent frame
        self.throttle = self.getContext().getCurentThrottleFrame().getAddressPanel().getThrottle() # the throttle
        self.speedAction =  SpeedAction()  #Speed increase thread
        self.speedAction.setThrottle( self.throttle )
        self.speedTimer = Timer(valueSpeedTimerRepeat, self.speedAction ) # Very important to use swing Timer object (see Swing and multithreading doc)
        self.speedTimer.setRepeats(True)
        self.label = ResizableImagePanel(self.getFolder() + "/WiimoteThrottle.png",20,20 ) #label
        self.add(self.label)
        self.lastTimeButton1 = Calendar.getInstance().getTimeInMillis()
        self.lastTimeButton2 = Calendar.getInstance().getTimeInMillis()
        self.lastTimeEStop = Calendar.getInstance().getTimeInMillis()
        self.wiiDevice = None
        self.sync = thread.allocate_lock() # A lock protecting bellow self.evt
        self.evt = None
        java.lang.System.setProperty("bluecove.jsr82.psm_minimum_off", "true"); # Required for Bluecove + WiiRemoteJ
        WiiRemoteJ.findRemotes(self, 1) # Search for 1 Wiimote, and call back
       
    def quit(self):
        self.speedTimer.stop() 
        WiiRemoteJ.stopFind()
        if ((self.wiiDevice != None) and (self.wiiDevice.isConnected())):
            self.wiiDevice.removeWiiRemoteListener(self)
            self.wiiDevice.disconnect()
        self.wiiDevice = None
        self.speedAction = None
        self.speedTimer = None
        self.throttle = None
        self.getContext().removePropertyChangeListener(self)
        self.getContext().getCurentThrottleFrame().getAddressPanel().removeAddressListener(self)

#AddressListener part: to listen for address changes in address panel (release, acquired)
    def notifyAddressChosen(self, address, isLong):
        pass
        
    def notifyAddressThrottleFound(self, throttle):
        self.speedTimer.stop() 
        self.throttle = throttle
        self.speedAction.setThrottle( self.throttle )
            
    def notifyAddressReleased(self, address, isLong):
        self.speedTimer.stop()
        self.throttle = None
        self.speedAction.setThrottle( self.throttle )
                
# Speed timer class, to increase speed regularly once button pushed, thread stopped on button release
class SpeedAction(ActionListener):
    def __init__(self):
        self.sync = thread.allocate_lock() # Protects properties getter and setter
        self.speedIncrement = 0
        self.throttle = None

    def setSpeedIncrement(self, si):
        self.sync.acquire()
        self.speedIncrement = si
        self.sync.release()

    def getSpeedIncrement(self):
        self.sync.acquire()
        si = self.speedIncrement
        self.sync.release()
        return si

    def setThrottle(self, throt):
        self.sync.acquire()
        self.throttle = throt
        self.sync.release()
    
    def getThrottle(self):
        self.sync.acquire()
        throt = self.throttle
        self.sync.release()
        return throt

    def actionPerformed(self, e):
        throttle = self.getThrottle()
        spi = self.getSpeedIncrement()
        if (throttle != None) :
            ns =  throttle.getSpeedSetting() + spi
            if (ns < 0 ) :
                ns = 0
            if (ns > 1 ) :
                ns = 1
            throttle.setSpeedSetting( ns )


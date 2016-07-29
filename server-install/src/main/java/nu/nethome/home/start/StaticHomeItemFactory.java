/**
 * Copyright (C) 2005-2015, Stefan Strömberg <stefangs@nethome.nu>
 *
 * This file is part of OpenNetHome  (http://www.nethome.nu)
 *
 * OpenNetHome is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenNetHome is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package nu.nethome.home.start;

import nu.nethome.home.impl.HomeItemFactory;
import nu.nethome.home.item.HomeItem;
import nu.nethome.home.item.HomeItemInfo;
import nu.nethome.home.items.UPnPScanner;
import nu.nethome.home.items.UsbScanner;
import nu.nethome.home.items.net.ArpScanner;
import nu.nethome.home.items.rollertrol.RollerTrolBlindGSeries;
import nu.nethome.home.items.tellstick.Tellstick;
import nu.nethome.home.items.web.proxy.HomeCloudConnection;

import java.util.*;
import java.util.logging.Logger;

/**
 * A repository of all installed HomeItem classes in the system. Can list all supported classes
 * and create instances of them.
 *
 * @author Stefan Strömberg
 */
public class StaticHomeItemFactory implements HomeItemFactory {

    private AbstractMap<String, HomeItemClassInfo> classNameMap = new TreeMap<>();
    private AbstractMap<String, String> renamedClassMap = new TreeMap<>();
    private List<HomeItemInfo> classInfo = new ArrayList<>();
    private static Logger logger = Logger.getLogger(StaticHomeItemFactory.class.getName());

    /**
     * Create a factory connected to the EventBroker
     */
    public StaticHomeItemFactory() {
        addKnownClasses();
    }

    public HomeItem createInstance(String className) {
        HomeItem createdHomeItem;
        try {
            // Lookup real class name
            if (classNameMap.containsKey(className)) {
                createdHomeItem = classNameMap.get(className).createHomeItem();
            } else {
                // For backward compatibility, accept the full qualified Java
                // class name, and use the last part of it as class name.
                String splitClassName[] = className.split("\\.");
                String newName = splitClassName[splitClassName.length - 1];
                if (classNameMap.containsKey(newName)) {
                    createdHomeItem = classNameMap.get(newName).createHomeItem();
                }
                // Check if this is a class which has been renamed, and use the new
                // name instead in that case
                else if (renamedClassMap.containsKey(newName) &&
                        classNameMap.containsKey(renamedClassMap.get(newName))) {
                    createdHomeItem = classNameMap.get(renamedClassMap.get(newName)).createHomeItem();
                }
                // We have not found the class name - exit
                else {
                    return null;
                }
            }
        } catch (InstantiationException e) {
            logger.warning("Class " + className + " could not be instantiated " + e.getMessage());
            return null;
        } catch (IllegalAccessException e) {
            logger.warning("Not allowed to call constructor in class "
                    + className);
            return null;
        }
        return createdHomeItem;
    }

    private static final HomeItemClassInfo CLASSES_INFO[] = {
            // core-items
            new HomeItemClassInfo("XmppClient", nu.nethome.home.items.net.XmppClient.class),
            new HomeItemClassInfo("Message", nu.nethome.home.items.net.Message.class),
            new HomeItemClassInfo("MessageInteractor", nu.nethome.home.items.net.MessageInteractor.class),
            new HomeItemClassInfo("LampMessageInteractor", nu.nethome.home.items.net.LampMessageInteractor.class),
            new HomeItemClassInfo("TCPListener", nu.nethome.home.items.net.TCPListener.class),
            new HomeItemClassInfo("TCPCommandPort", nu.nethome.home.items.net.TCPCommandPort.class),
            new HomeItemClassInfo("UDPCommandPort", nu.nethome.home.items.net.UDPCommandPort.class),
            new HomeItemClassInfo("MQTTCommandPort", nu.nethome.home.items.net.MQTTCommandPort.class),
            new HomeItemClassInfo("UDPListener", nu.nethome.home.items.net.UDPListener.class),
            new HomeItemClassInfo("WeekTimer", nu.nethome.home.items.timer.WeekTimer.class),
            new HomeItemClassInfo("IntervalTimer", nu.nethome.home.items.timer.IntervalTimer.class),
            new HomeItemClassInfo("TCPProxy", nu.nethome.home.items.net.TCPProxy.class),
            new HomeItemClassInfo("AudioProtocolParser", nu.nethome.home.items.audio.AudioProtocolParser.class),
            new HomeItemClassInfo("AudioProtocolTransmitter", nu.nethome.home.items.audio.AudioProtocolTransmitter.class),
            new HomeItemClassInfo("DayLiteTimer", nu.nethome.home.items.timer.DayLiteTimer.class),
            new HomeItemClassInfo("SunTimer", nu.nethome.home.items.timer.SunTimer.SunTimer.class),
            new HomeItemClassInfo("LampRepeater", nu.nethome.home.items.timer.LampRepeater.class),
            new HomeItemClassInfo("GateKeeper", nu.nethome.home.items.net.GateKeeper.class),
            new HomeItemClassInfo("MBMThermometer", nu.nethome.home.items.misc.MBMThermometer.class),
            new HomeItemClassInfo("ValueTrigger", nu.nethome.home.items.misc.ValueTrigger.class),
            new HomeItemClassInfo("Scene", nu.nethome.home.items.misc.Scene.class),
            new HomeItemClassInfo("ArpScanner", ArpScanner.class),
            new HomeItemClassInfo("MacDevice", nu.nethome.home.items.net.MacDevice.class),
            new HomeItemClassInfo("MacDeviceGroup", nu.nethome.home.items.net.MacDeviceGroup.class),
            new HomeItemClassInfo("LmSensorsThermometer", nu.nethome.home.items.misc.LmSensorsThermometer.class),
            new HomeItemClassInfo("LmSensorsFan", nu.nethome.home.items.misc.LmSensorsFan.class),
            new HomeItemClassInfo("SatelliteCommander", nu.nethome.home.items.satellite.SatelliteCommander.class),
            new HomeItemClassInfo("SatelliteLogger", nu.nethome.home.items.satellite.SatelliteLogger.class, false),
            new HomeItemClassInfo("TrayBarIcon", nu.nethome.home.items.gui.TrayBarIcon.class),
            new HomeItemClassInfo("TeamCityBuildMonitor", nu.nethome.home.items.net.TeamCityBuildMonitor.class),
            new HomeItemClassInfo("DebugManager", nu.nethome.home.items.misc.DebugManager.class),
            new HomeItemClassInfo("WemoInsightSwitch", nu.nethome.home.items.net.wemo.WemoInsightSwitch.class),
            new HomeItemClassInfo("WemoSwitch", nu.nethome.home.items.net.wemo.WemoSwitch.class),
            new HomeItemClassInfo("WemoBridge", nu.nethome.home.items.net.wemo.WemoBridge.class),
            new HomeItemClassInfo("WemoLamp", nu.nethome.home.items.net.wemo.WemoLamp.class),
            new HomeItemClassInfo("UPnPScanner", UPnPScanner.class),
            new HomeItemClassInfo("H2DatabaseTCPServer", nu.nethome.home.items.net.H2DatabaseTCPServer.class),
            new HomeItemClassInfo("UsbScanner", UsbScanner.class),

            // rf-items
            new HomeItemClassInfo("UPMThermometer", nu.nethome.home.items.upm.UPMThermometer.class),
            new HomeItemClassInfo("UPMHygrometer", nu.nethome.home.items.upm.UPMHygrometer.class),
            new HomeItemClassInfo("UPMWindSpeed", nu.nethome.home.items.upm.UPMWindSpeed.class),
            new HomeItemClassInfo("UPMWindDirection", nu.nethome.home.items.upm.UPMWindDirection.class),
            new HomeItemClassInfo("UPMRainfall", nu.nethome.home.items.upm.UPMRainfall.class),
            new HomeItemClassInfo("UPMSoilMoisture", nu.nethome.home.items.upm.UPMSoilMoisture.class),
            new HomeItemClassInfo("OregonThermometer", nu.nethome.home.items.oregon.OregonThermometer.class),
            new HomeItemClassInfo("OregonHygrometer", nu.nethome.home.items.oregon.OregonHygrometer.class),
            new HomeItemClassInfo("OregonWind", nu.nethome.home.items.oregon.OregonWind.class),
            new HomeItemClassInfo("FineOffsetThermometer", nu.nethome.home.items.fineoffset.FineOffsetThermometer.class),
            new HomeItemClassInfo("FineOffsetHygrometer", nu.nethome.home.items.fineoffset.FineOffsetHygrometer.class),
            new HomeItemClassInfo("FineOffsetRainGauge", nu.nethome.home.items.fineoffset.FineOffsetRainGauge.class),
            new HomeItemClassInfo("FooGadgetLegacy", nu.nethome.home.items.fineoffset.FooGadgetLegacy.class),
            new HomeItemClassInfo("FooGadgetPulse", nu.nethome.home.items.fineoffset.FooGadgetPulse.class),
            new HomeItemClassInfo("NexaLamp", nu.nethome.home.items.nexa.NexaLamp.class),
            new HomeItemClassInfo("NexaRemapButton", nu.nethome.home.items.nexa.NexaRemapButton.class),
            new HomeItemClassInfo("NexaLCLamp", nu.nethome.home.items.nexa.NexaLCLamp.class),
            new HomeItemClassInfo("NexaLCDimmer", nu.nethome.home.items.nexa.NexaLCDimmer.class),
            new HomeItemClassInfo("NexaLCRemapButton", nu.nethome.home.items.nexa.NexaLCRemapButton.class),
            new HomeItemClassInfo("NexaLCAdvancedRemapButton", nu.nethome.home.items.nexa.NexaLCAdvancedRemapButton.class),
            new HomeItemClassInfo("NexaLCRemapButton4", nu.nethome.home.items.nexa.NexaLCRemapButton4.class),
            new HomeItemClassInfo("NexaSmokeDetector", nu.nethome.home.items.nexa.NexaSmokeDetector.class),
            new HomeItemClassInfo("DeltronicLamp", nu.nethome.home.items.deltronic.DeltronicLamp.class),
            new HomeItemClassInfo("DeltronicRemapButton", nu.nethome.home.items.deltronic.DeltronicRemapButton.class),
            new HomeItemClassInfo("WavemanLamp", nu.nethome.home.items.waveman.WavemanLamp.class),
            new HomeItemClassInfo("ProntoLamp", nu.nethome.home.items.pronto.ProntoLamp.class),
            new HomeItemClassInfo("ProntoDevice", nu.nethome.home.items.pronto.ProntoDevice.class),
            new HomeItemClassInfo("FHZ1000PcPort", nu.nethome.home.items.fs20.FHZ1000PcPort.class),
            new HomeItemClassInfo("FS20RemapButton", nu.nethome.home.items.fs20.FS20RemapButton.class),
            new HomeItemClassInfo("FS20Lamp", nu.nethome.home.items.fs20.FS20Lamp.class),
            new HomeItemClassInfo("RisingSunLamp", nu.nethome.home.items.risingsun.RisingSunLamp.class),
            new HomeItemClassInfo("CULTransceiver", nu.nethome.home.items.cul.CULTransceiver.class),
            new HomeItemClassInfo("JeeLink", nu.nethome.home.items.jeelink.JeeLink.class),
            new HomeItemClassInfo("GenericProntoCommander", nu.nethome.home.items.pronto.GenericProntoCommander.class),
            new HomeItemClassInfo("ZhejiangLamp", nu.nethome.home.items.zhejiang.ZhejiangLamp.class),
            new HomeItemClassInfo("RFBitBangerTransmitter", nu.nethome.home.items.rfbitbanger.RFBitBangerTransmitter.class),
            new HomeItemClassInfo("LampGroup", nu.nethome.home.items.LampGroup.class),
            new HomeItemClassInfo("PlantManager", nu.nethome.home.items.plant.PlantManager.class),
            new HomeItemClassInfo("Tellstick", Tellstick.class),
            new HomeItemClassInfo("HueBridge", nu.nethome.home.items.hue.HueBridge.class),
            new HomeItemClassInfo("HueLamp", nu.nethome.home.items.hue.HueLamp.class),
            new HomeItemClassInfo("RollerTrolBlind", nu.nethome.home.items.rollertrol.RollerTrolBlind.class),
            new HomeItemClassInfo("RollerTrolBlindGSeries", RollerTrolBlindGSeries.class),
            new HomeItemClassInfo("ZWaveController", nu.nethome.home.items.zwave.ZWaveController.class),
            new HomeItemClassInfo("ZWaveRemapButton", nu.nethome.home.items.zwave.ZWaveRemapButton.class),
            new HomeItemClassInfo("ZWaveNodeExplorer", nu.nethome.home.items.zwave.ZWaveNodeExplorer.class),
            new HomeItemClassInfo("ZWaveCommandPort", nu.nethome.home.items.zwave.ZWaveCommandPort.class),
            new HomeItemClassInfo("ZWaveNode", nu.nethome.home.items.zwave.ZWaveNode.class),
            new HomeItemClassInfo("ZWaveCentralSceneRemapButton", nu.nethome.home.items.zwave.ZWaveCentralSceneRemapButton.class),
            new HomeItemClassInfo("ZWaveLamp", nu.nethome.home.items.zwave.ZWaveLamp.class),
            new HomeItemClassInfo("ZWaveDimmer", nu.nethome.home.items.zwave.ZWaveDimmer.class),
            new HomeItemClassInfo("ZWaveBlind", nu.nethome.home.items.zwave.ZWaveBlind.class),
            new HomeItemClassInfo("ZWaveMeter", nu.nethome.home.items.zwave.ZWaveMeter.class),
            new HomeItemClassInfo("ZWaveMultiLevelSensor", nu.nethome.home.items.zwave.ZWaveMultiLevelSensor.class),
            new HomeItemClassInfo("MqttThermometer", nu.nethome.home.items.mqtt.MqttThermometer.class),
            new HomeItemClassInfo("MqttHygrometer", nu.nethome.home.items.mqtt.MqttHygrometer.class),
            new HomeItemClassInfo("MqttRemapButton", nu.nethome.home.items.mqtt.MqttRemapButton.class),

            // web-items
            new HomeItemClassInfo("Room", nu.nethome.home.items.infra.Room.class),
            new HomeItemClassInfo("Box", nu.nethome.home.items.infra.Box.class),
            new HomeItemClassInfo("Location", nu.nethome.home.items.infra.Location.class),
            new HomeItemClassInfo("Plan", nu.nethome.home.items.infra.Plan.class),
            new HomeItemClassInfo("HomeGUI", nu.nethome.home.items.web.servergui.HomeGUI.class),
            new HomeItemClassInfo("JettyWEB", nu.nethome.home.items.web.JettyWEB.class),
            new HomeItemClassInfo("TempWEB", nu.nethome.home.items.web.temp.TempWEB.class),
            new HomeItemClassInfo("ActionButton", nu.nethome.home.items.web.servergui.ActionButton.class),
            new HomeItemClassInfo("HomeCloudConnection", HomeCloudConnection.class),

            // Misc
            new HomeItemClassInfo("ValueLogger",nu.nethome.home.items.misc.ValueLogger.class),


//		new HomeItemClassInfo("X10Port","nu.nethome.home.items.x10.X10Port"),
//		new HomeItemClassInfo("X10Lamp","nu.nethome.home.items.x10.X10Lamp"),
//		new HomeItemClassInfo("X10HouseScene","nu.nethome.home.items.x10.X10HouseScene"),
//		new HomeItemClassInfo("X10RemapButton","nu.nethome.home.items.x10.X10RemapButton"),

//		new HomeItemClassInfo("JVCUX1500RRemote","nu.nethome.home.items.snap.JVCUX1500RRemote", false),
//		new HomeItemClassInfo("PhilipsTVRemote","nu.nethome.home.items.snap.PhilipsTVRemote", false),
//		new HomeItemClassInfo("PioneerRemote","nu.nethome.home.items.snap.PioneerRemote", false),
//		new HomeItemClassInfo("HKCDRemote","nu.nethome.home.items.snap.HKCDRemote", false),

//		new HomeItemClassInfo("ValueAlarm","nu.nethome.home.items.ValueAlarm", false),
//		new HomeItemClassInfo("SNAPPort","nu.nethome.home.items.snap.SNAPPort", false),
//		new HomeItemClassInfo("SNAPIrRemote","nu.nethome.home.items.snap.SNAPIrRemote", false),
//		new HomeItemClassInfo("LampMediaHandler","nu.nethome.home.items.experimental.LampMediaHandler", false),
//		new HomeItemClassInfo("FS20Button","nu.nethome.home.items.fs20.FS20Button"),
//		new HomeItemClassInfo("DebugManager","nu.nethome.home.items.experimental.DebugManager"),
//		new HomeItemClassInfo("TUIOPort","nu.nethome.home.items.experimental.TUIOPort", false),
//		new HomeItemClassInfo("SlimRemote","nu.nethome.home.items.SlimRemote"),
//		new HomeItemClassInfo("WAPService","nu.nethome.home.items.web.WAPService"),
//		new HomeItemClassInfo("SNAPUPMTranslator","nu.nethome.home.items.snap.SNAPUPMTranslator", false),
    };

    private static final String RENAMED_CLASSES[] = {
            "SatelliteService", "SatelliteLogger"
    };

    /**
     * Populate the internal class dictionary
     */
    private void addKnownClasses() {
        for (HomeItemClassInfo i : CLASSES_INFO) {
            classNameMap.put(i.getClassName(), i);
            classInfo.add(new nu.nethome.home.impl.HomeItemClassInfo(i.getItemClass()));
        }
        for (int i = 0; i < RENAMED_CLASSES.length; i += 2) {
            renamedClassMap.put(RENAMED_CLASSES[i], RENAMED_CLASSES[i + 1]);
        }
    }

    public List<String> listClasses(boolean includeHidden) {
        List<String> result = new ArrayList<>();
        for (HomeItemClassInfo info : CLASSES_INFO) {
            if (info.isPublic() || includeHidden) {
                result.add(info.getClassName());
            }
        }
        return result;
    }

    @Override
    public List<HomeItemInfo> listItemTypes() {
        return Collections.unmodifiableList(classInfo);
    }
}

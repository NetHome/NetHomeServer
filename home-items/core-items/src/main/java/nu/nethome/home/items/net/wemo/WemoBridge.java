package nu.nethome.home.items.net.wemo;

import nu.nethome.home.item.AutoCreationInfo;
import nu.nethome.home.item.HomeItem;
import nu.nethome.home.item.HomeItemAdapter;
import nu.nethome.home.item.HomeItemType;
import nu.nethome.home.system.Event;
import nu.nethome.util.plugin.Plugin;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Belkin wemo bridge device
 *
 * @author Stefan
 */

@Plugin
@HomeItemType(value = "Hardware", creationInfo = WemoBridge.WemoCreationInfo.class)
public class WemoBridge extends HomeItemAdapter implements HomeItem {

    public static final String UPN_P_CREATION_MESSAGE = "UPnP_Creation_Message";
    public static final String BELKIN_WEMO_BRIDGE_DEVICE = "urn:Belkin:device:bridge:1";
    public static final String WEMO_LIGHT_MESSAGE = "WemoLight_Message";
    public static final String DEVICE_INDEX = "DeviceIndex";
    public static final String DEVICE_ID = "DeviceID";
    public static final String FRIENDLY_NAME = "FriendlyName";
    public static final String FIRMWARE_VERSION = "FirmwareVersion";
    public static final String CAPABILITY_IDS = "CapabilityIDs";
    public static final String ON_STATE = "OnState";
    public static final String BRIGHTNESS = "Brightness";
    public static final String BRIDGE_URL = "BridgeUrl";
    public static final String BRIDGE_UDN = "BridgeUDN";
    public static final String QUIT_EVENT = "QUIT";
    public static final int UPDATE_ATTEMPTS = 3;
    private LinkedBlockingQueue<Event> eventQueue;

    public static class WemoCreationInfo implements AutoCreationInfo {
        static final String[] CREATION_EVENTS = {UPN_P_CREATION_MESSAGE};

        @Override
        public String[] getCreationEvents() {
            return CREATION_EVENTS;
        }

        @Override
        public boolean canBeCreatedBy(Event e) {
            return e.getAttribute("DeviceType").equals(BELKIN_WEMO_BRIDGE_DEVICE);
        }

        @Override
        public String getCreationIdentification(Event e) {
            return String.format("Belkin Wemo Bridge: \"%s\", UDN: %s", e.getAttribute("FriendlyName"), e.getAttribute("UDN"));
        }
    }

    private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
            + "<HomeItem Class=\"WemoBridge\" Category=\"Hardware\" >"
            + "  <Attribute Name=\"State\" Type=\"String\" Get=\"getState\" Default=\"true\" />"
            + "  <Attribute Name=\"DeviceURL\" Type=\"String\" Get=\"getDeviceURL\" Set=\"setDeviceURL\" />"
            + "  <Attribute Name=\"Identity\" Type=\"String\" Get=\"getUDN\" Init=\"setUDN\" />"
            + "  <Attribute Name=\"ConnectedLamps\" Type=\"String\" Get=\"getConnectedLamps\" />"
            + "  <Action Name=\"ReportDevices\" Method=\"reportAllDevices\" />"
            + "</HomeItem> ");

    private static Logger logger = Logger.getLogger(WemoBridge.class.getName());
    private String wemoDescriptionUrl = "";
    private String udn = "";
    private WemoBridgeSoapClient soapClient;
    private int connectedLamps = -1;

    public WemoBridge() {
        soapClient = new WemoBridgeSoapClient("");
        eventQueue = new LinkedBlockingQueue<Event>(5);
    }

    WemoBridgeSoapClient getSoapClient() {
        return soapClient;
    }

    public String getModel() {
        return MODEL;
    }

    @Override
    public void activate() {
        startBackgroundProcessThread();
        backgroundProcess(server.createEvent("ReportItems", ""));
    }

    @Override
    public void stop() {
        eventQueue.clear();
        backgroundProcess(server.createEvent(QUIT_EVENT, ""));
        super.stop();
    }

    private void startBackgroundProcessThread() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                backgroundProcessor();
            }
        }, "WemoBridge").start();
    }

    public boolean receiveEvent(Event event) {
        if (event.isType(UPN_P_CREATION_MESSAGE) &&
                event.getAttribute("DeviceType").equals(BELKIN_WEMO_BRIDGE_DEVICE) &&
                event.getAttribute("UDN").equals(udn)) {
            setDeviceURL(event.getAttribute("Location"));
            return true;
        } else if (event.isType("ReportItems") || event.isType(WEMO_LIGHT_MESSAGE)) {
            backgroundProcess(event);
        }
        return handleInit(event);
    }

    private boolean backgroundProcess(Event event) {
        return eventQueue.offer(event);
    }

    public void backgroundProcessor() {
        while (true) {
            Event event;
            try {
                event = eventQueue.take();
                if (event.isType(QUIT_EVENT)) {
                    return;
                } else if (event.isType("ReportItems")) {
                    reportAllDevices();
                } else if (event.isType(WEMO_LIGHT_MESSAGE) &&
                        event.getAttribute("Direction").equals("Out")) {
                    updateDeviceState(event);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private boolean updateDeviceState(Event event) {
        boolean isOn = event.getAttributeInt(ON_STATE) == 1;
        int brightness = event.getAttributeInt(BRIGHTNESS);
        for (int retry = 0; retry < UPDATE_ATTEMPTS; retry++) {
            try {
                boolean result = soapClient.setDeviceStatus(event.getAttribute(DEVICE_ID), isOn, brightness);
                break;
            } catch (WemoException e) {
                logger.log(Level.WARNING, "Failed to set device status in " + wemoDescriptionUrl, e);
            }
        }
        try {
            List<BridgeDeviceStatus> deviceStatuses = soapClient.getDeviceStatus(event.getAttribute(DEVICE_ID));
            for (BridgeDeviceStatus deviceStatus : deviceStatuses) {
                reportDeviceStatus(deviceStatus);
            }
        } catch (WemoException e) {
            logger.log(Level.WARNING, "Failed to get device status in " + wemoDescriptionUrl, e);
        }
        return true;
    }

    private void reportDeviceStatus(BridgeDeviceStatus deviceStatus) {
        Event event = server.createEvent(WEMO_LIGHT_MESSAGE, "");
        event.setAttribute(DEVICE_ID, deviceStatus.getDeviceID());
        event.setAttribute(CAPABILITY_IDS, deviceStatus.getCapabilityIDs());
        event.setAttribute(ON_STATE, deviceStatus.getOnState());
        event.setAttribute(BRIGHTNESS, deviceStatus.getBrightness());
        event.setAttribute(BRIDGE_URL, wemoDescriptionUrl);
        event.setAttribute(BRIDGE_UDN, udn);
        event.setAttribute("Direction", "In");
        server.send(event);
    }

    @Override
    protected boolean initAttributes(Event event) {
        setDeviceURL(event.getAttribute("Location"));
        udn = event.getAttribute("UDN");
        return true;
    }

    public String reportAllDevices() {
        try {
            List<BridgeDevice> endDevices = getSoapClient().getEndDevices(udn);
            connectedLamps = endDevices.size();
            for (BridgeDevice device : endDevices) {
                reportDevice(device);
            }
        } catch (WemoException e) {
            logger.log(Level.WARNING, "Failed to get devices from " + wemoDescriptionUrl, e);
        }
        return "";
    }

    private void reportDevice(BridgeDevice device) {
        Event event = server.createEvent(WEMO_LIGHT_MESSAGE, "");
        event.setAttribute(DEVICE_INDEX, device.getDeviceIndex());
        event.setAttribute(DEVICE_ID, device.getDeviceID());
        event.setAttribute(FRIENDLY_NAME, device.getFriendlyName());
        event.setAttribute(FIRMWARE_VERSION, device.getFirmwareVersion());
        event.setAttribute(CAPABILITY_IDS, device.getCapabilityIDs());
        event.setAttribute(ON_STATE, device.getOnState());
        event.setAttribute(BRIGHTNESS, device.getBrightness());
        event.setAttribute(BRIDGE_URL, wemoDescriptionUrl);
        event.setAttribute(BRIDGE_UDN, udn);
        event.setAttribute("Direction", "In");
        server.send(event);
    }

    public String getDeviceURL() {
        return wemoDescriptionUrl;
    }

    public void setDeviceURL(String url) {
        wemoDescriptionUrl = url;
        getSoapClient().setWemoURL(extractBaseUrl(url));
    }

    private String extractBaseUrl(String url) {
        int pos = url.indexOf("/", 9);
        if (pos > 0) {
            return url.substring(0, pos);
        }
        return url;
    }

    public String getUDN() {
        return udn;
    }

    public void setUDN(String udn) {
        this.udn = udn;
    }

    public String getConnectedLamps() {
        return connectedLamps > 0 ? Integer.toString(connectedLamps) : "";
    }
}

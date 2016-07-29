/**
 * Copyright (C) 2005-2013, Stefan Strömberg <stefangs@nethome.nu>
 *
 * This file is part of OpenNetHome (http://www.nethome.nu)
 *
 * OpenNetHome is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * OpenNetHome is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package nu.nethome.home.items.mqtt;

import nu.nethome.home.item.*;
import nu.nethome.home.system.Event;
import nu.nethome.home.system.HomeService;
import nu.nethome.util.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Presents and logs temperature values received by an FineOffset-temperature
 * sensor. The actual values are received as events which may be sent by any
 * kind of receiver module which can receive FineOffset messages from the
 * hardware devices.
 *
 * @author Stefan
 */
@Plugin
@HomeItemType(value = "Thermometers", creationEvents = "Mqtt_Message")
public class MqttThermometer extends HomeItemAdapter implements HomeItem, ValueItem {

    private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
            + "<HomeItem Class=\"MqttThermometer\" Category=\"Thermometers\" >"
            + "  <Attribute Name=\"Temperature\" 	Type=\"String\" Get=\"getValue\" Default=\"true\"  Unit=\"°C\" />"
            + "  <Attribute Name=\"Topic\" 	Type=\"String\" Get=\"getTopic\" Set=\"setTopic\" />"
            + "  <Attribute Name=\"TimeSinceUpdate\" 	Type=\"String\" Get=\"getTimeSinceUpdate\"  Unit=\"s\" />"
            + "  <Attribute Name=\"LogFile\" Type=\"String\" Get=\"getLogFile\" 	Set=\"setLogFile\" />"
            + "  <Attribute Name=\"LastUpdate\" Type=\"String\" Get=\"getLastUpdate\" />"
            + "</HomeItem> ");

    protected Logger logger = Logger.getLogger(MqttThermometer.class.getName());
    private ExtendedLoggerComponent tempLoggerComponent = new ExtendedLoggerComponent(this);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss yyyy.MM.dd ");

    // Public attributes
    private double temperature = 0;
    private Date latestUpdateOrCreation = new Date();
    protected boolean hasBeenUpdated = false;
    private String topic = "MyHome/Indoor/Floor1/Livingroom/Temperature";

    public MqttThermometer() {
    }

    @Override
    public boolean receiveEvent(Event event) {
        // Check if the event is an FineOffset_Message and in that case check if it is
        // intended for this thermometer.
        if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("Mqtt_Message")
                && (event.getAttribute("Mqtt.Topic").equals(topic))) {
            return handleEvent(event);
        } else {
            return handleInit(event);
        }
    }

    protected boolean handleEvent(Event event) {
        setValue(event.getAttribute("Mqtt.Message"));
        return true;
    }

    @Override
    public String getModel() {
        return MODEL;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String value) {
        topic = value;
    }

    /* Activate the instance
      * @see ssg.home.HomeItem#activate()
     */
    @Override
    public void activate(HomeService server) {
        super.activate(server);
        // Activate the logger component
        tempLoggerComponent.activate(server);
    }

    @Override
    protected boolean initAttributes(Event event) {
        topic = event.getAttribute("Mqtt.Topic");
        return true;
    }

    /**
     * HomeItem method which stops all object activity for program termination
     */
    public void stop() {
        tempLoggerComponent.stop();
    }

    public void setValue(String value) {
        temperature = Double.parseDouble(value);
        logger.finer("Temperature update: " + temperature + " degrees");
        // Format and store the current time.
        latestUpdateOrCreation = new Date();
        hasBeenUpdated = true;
    }

    public String getValue() {
        return getTemp();
    }

    public String getTemp() {
        return hasBeenUpdated ? String.format("%.1f", temperature) : "";
    }

    public String getLastUpdate() {
        return hasBeenUpdated ? dateFormatter.format(latestUpdateOrCreation) : "";
    }

    public String getLogFile() {
        return tempLoggerComponent.getFileName();
    }

    public void setLogFile(String logfile) {
        tempLoggerComponent.setFileName(logfile);
    }

    public String getTimeSinceUpdate() {
        return Long.toString((new Date().getTime() - latestUpdateOrCreation.getTime()) / 1000);
    }
}

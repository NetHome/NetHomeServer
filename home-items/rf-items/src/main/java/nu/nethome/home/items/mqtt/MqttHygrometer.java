/**
 * Copyright (C) 2005-2013, Stefan Strömberg <stefangs@nethome.nu>
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

package nu.nethome.home.items.mqtt;

import nu.nethome.home.item.*;
import nu.nethome.home.system.Event;
import nu.nethome.home.system.HomeService;
import nu.nethome.util.plugin.Plugin;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

@Plugin
@HomeItemType(value = "Gauges", creationEvents = "Mqtt_Message")
public class MqttHygrometer extends HomeItemAdapter implements HomeItem, ValueItem {

    private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
            + "<HomeItem Class=\"MqttHygrometer\" Category=\"Gauges\" >"
            + "  <Attribute Name=\"Humidity\" 	Type=\"String\" Get=\"getValue\" Default=\"true\"  Unit=\"%\" />"
            + "  <Attribute Name=\"Topic\" 	Type=\"String\" Get=\"getTopic\" Set=\"setTopic\" />"
            + "  <Attribute Name=\"TimeSinceUpdate\" 	Type=\"String\" Get=\"getTimeSinceUpdate\" />"
            + "  <Attribute Name=\"LogFile\" Type=\"String\" Get=\"getLogFile\" 	Set=\"setLogFile\" />"
            + "  <Attribute Name=\"LastUpdate\" Type=\"String\" Get=\"getLastUpdate\"  Unit=\"s\" />"
            + "</HomeItem> ");

    protected Logger logger = Logger.getLogger(MqttHygrometer.class.getName());
    private ExtendedLoggerComponent loggerComponent = new ExtendedLoggerComponent(this);
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("HH:mm:ss yyyy.MM.dd ");

    // Public attributes
    private double humidity = 0;
    private Date latestUpdateOrCreation = new Date();
    protected boolean hasBeenUpdated = false;
    private String topic = "MyHome/Indoor/Floor1/Livingroom/Humidity";

    public boolean receiveEvent(Event event) {
        // Check if the event is an FineOffset_Message and in that case check if it is
        // intended for this thermometer.
        if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("Mqtt_Message") &&
                (event.getAttribute("*Mqtt.Topic").equals(name))) {
            return handleEvent(event);
        } else {
            return handleInit(event);
        }
    }

    protected boolean handleEvent(Event event) {
        setValue(event.getAttribute("Mqtt.Message"));
        return true;
    }

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
    public void activate(HomeService server) {
        super.activate(server);
        // Activate the logger component
        loggerComponent.activate(server);
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
        loggerComponent.stop();
    }

    public void setValue(String value) {
        humidity = Double.parseDouble(value);
        logger.finer("Humidity update: " + humidity + " %");
        // Format and store the current time.
        latestUpdateOrCreation = new Date();
        hasBeenUpdated = true;
    }

    public String getValue() {
        return getHumidity();
    }
    
    public String getHumidity() {
        return hasBeenUpdated ? String.format("%.1f", humidity) : "";
    }

    public String getLastUpdate() {
        return hasBeenUpdated ? dateFormatter.format(latestUpdateOrCreation) : "";
    }

    public String getLogFile() {
        return loggerComponent.getFileName();
    }

    public void setLogFile(String logfile) {
        loggerComponent.setFileName(logfile);
    }

    public String getTimeSinceUpdate() {
        return Long.toString((new Date().getTime() - latestUpdateOrCreation.getTime()) / 1000);
    }
}

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

import nu.nethome.home.item.HomeItem;
import nu.nethome.home.item.HomeItemType;
import nu.nethome.home.system.Event;
import nu.nethome.util.plugin.Plugin;

import java.util.logging.Logger;
import nu.nethome.home.impl.CommandLineExecutor;
import nu.nethome.home.item.HomeItemAdapter;
import nu.nethome.home.system.HomeService;

/**
 * Listens for commands for a specific Nexa Learning Code address and button.
 * When a command is received, the corresponding command is executed.
 *
 * @author Stefan
 */
@Plugin
@HomeItemType(value = "Controls", creationEvents = "Mqtt_Message")
public class MqttRemapButton extends HomeItemAdapter implements HomeItem {

    private static final String MODEL = ("<?xml version = \"1.0\"?> \n"
            + "<HomeItem Class=\"MqttRemapButton\" Category=\"Controls\" >"
            + "  <Attribute Name=\"State\" 	Type=\"String\" Get=\"getState\" Init=\"setState\" Default=\"true\" />"
            + "  <Attribute Name=\"Topic\" 	Type=\"String\" Get=\"getTopic\" Set=\"setTopic\" />"
            + "  <Attribute Name=\"Message1\" 	Type=\"String\" Get=\"getMessage1\" Set=\"setMessage1\" />"
            + "  <Attribute Name=\"Message2\" 	Type=\"String\" Get=\"getMessage2\" Set=\"setMessage2\" />"
            + "  <Attribute Name=\"Message3\" 	Type=\"String\" Get=\"getMessage3\" Set=\"setMessage3\" />"
            + "  <Attribute Name=\"Message4\" 	Type=\"String\" Get=\"getMessage4\" Set=\"setMessage4\" />"
            + "  <Attribute Name=\"Message5\" 	Type=\"String\" Get=\"getMessage5\" Set=\"setMessage5\" />"
            + "  <Attribute Name=\"Message6\" 	Type=\"String\" Get=\"getMessage6\" Set=\"setMessage6\" />"
            + "  <Attribute Name=\"Command1\" Type=\"Command\" Get=\"getCommand1\" Set=\"setCommand1\" />"
            + "  <Attribute Name=\"Command2\" Type=\"Command\" Get=\"getCommand2\" Set=\"setCommand2\" />"
            + "  <Attribute Name=\"Command3\" Type=\"Command\" Get=\"getCommand3\" Set=\"setCommand3\" />"
            + "  <Attribute Name=\"Command4\" Type=\"Command\" Get=\"getCommand4\" Set=\"setCommand4\" />"
            + "  <Attribute Name=\"Command5\" Type=\"Command\" Get=\"getCommand5\" Set=\"setCommand5\" />"
            + "  <Attribute Name=\"Command6\" Type=\"Command\" Get=\"getCommand6\" Set=\"setCommand6\" />"
            + "  <Action Name=\"enable\" 	Method=\"enable\" />"
            + "  <Action Name=\"disable\" 	Method=\"disable\" />"
            + "</HomeItem> ");

    private static Logger logger = Logger.getLogger(MqttRemapButton.class.getName());

    // Public attributes
    private CommandLineExecutor commandExecutor;
    private boolean isEnabled = true;
    private String topic = "MyHome/Indoor/Floor1/Livingroom/Lamp";
    private String message1 = "";
    private String message2 = "";
    private String message3 = "";
    private String message4 = "";
    private String message5 = "";
    private String message6 = "";
    private String command1 = "";
    private String command2 = "";
    private String command3 = "";
    private String command4 = "";
    private String command5 = "";
    private String command6 = "";

    public MqttRemapButton() {
    }

    @Override
    public boolean receiveEvent(Event event) {
        // Check the event and see if they affect our current state.
        if (event.getAttribute(Event.EVENT_TYPE_ATTRIBUTE).equals("Mqtt_Message")
                && event.getAttribute("Direction").equals("In")
                && (event.getAttribute("Mqtt.Topic").equals(topic))) {
            //Ok, this event affects us, act on it
            processEvent(event);
            return true;
        } else {
            return handleInit(event);
        }
    }

    protected void processEvent(Event event) {
        if (!isEnabled) {
            return;
        }
        actOnEvent(event);
    }

    public void activate(HomeService server) {
        super.activate(server);
        commandExecutor = new CommandLineExecutor(server, true);
    }

    @Override
    protected boolean initAttributes(Event event) {
        topic = event.getAttribute("Mqtt.Topic");
        return true;
    }

    protected void actOnEvent(Event event) {
        String message = event.getAttribute("Mqtt.Message");

        if (message.equals(message1)) {
            performCommand(command1);
        } else if (message.equals(message2)) {
            performCommand(command2);
        } else if (message.equals(message3)) {
            performCommand(command3);
        } else if (message.equals(message4)) {
            performCommand(command4);
        } else if (message.equals(message5)) {
            performCommand(command5);
        } else if (message.equals(message6)) {
            performCommand(command6);
        }
    }

    private void performCommand(String commandString) {
        if (commandString.equals("")) {
            return;
        }
        String result = commandExecutor.executeCommandLine(commandString);
        if (!result.startsWith("ok")) {
            logger.warning(name + " could not execute command: '" + commandString + "' (" + result + ")");
        }
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

    public String getState() {
        return isEnabled ? "Enabled" : "Disabled";
    }

    public void setState(String state) {
        isEnabled = state.equalsIgnoreCase("Enabled");
    }

    public void enable() {
        isEnabled = true;
    }

    public void disable() {
        isEnabled = false;
    }

    public String getMessage1() {
        return message1;
    }

    public void setMessage1(String message1) {
        this.message1 = message1;
    }

    public String getMessage2() {
        return message2;
    }

    public void setMessage2(String message2) {
        this.message2 = message2;
    }

    public String getMessage3() {
        return message3;
    }

    public void setMessage3(String message3) {
        this.message3 = message3;
    }

    public String getMessage4() {
        return message4;
    }

    public void setMessage4(String message4) {
        this.message4 = message4;
    }

    public String getMessage5() {
        return message5;
    }

    public void setMessage5(String message5) {
        this.message5 = message5;
    }

    public String getMessage6() {
        return message6;
    }

    public void setMessage6(String message6) {
        this.message6 = message6;
    }

    public String getCommand1() {
        return command1;
    }

    public void setCommand1(String command1) {
        this.command1 = command1;
    }

    public String getCommand2() {
        return command2;
    }

    public void setCommand2(String command2) {
        this.command2 = command2;
    }

    public String getCommand3() {
        return command3;
    }

    public void setCommand3(String command3) {
        this.command3 = command3;
    }

    public String getCommand4() {
        return command4;
    }

    public void setCommand4(String command4) {
        this.command4 = command4;
    }

    public String getCommand5() {
        return command5;
    }

    public void setCommand5(String command5) {
        this.command5 = command5;
    }

    public String getCommand6() {
        return command6;
    }

    public void setCommand6(String command6) {
        this.command6 = command6;
    }

}

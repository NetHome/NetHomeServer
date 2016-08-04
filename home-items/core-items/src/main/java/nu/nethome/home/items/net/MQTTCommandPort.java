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
package nu.nethome.home.items.net;

import com.sun.tracing.dtrace.Attributes;
import java.util.List;
import java.util.logging.Level;
import nu.nethome.home.impl.CommandLineExecutor;
import nu.nethome.home.item.HomeItem;
import nu.nethome.home.item.HomeItemAdapter;
import nu.nethome.home.item.HomeItemType;
import nu.nethome.home.system.HomeService;
import nu.nethome.util.plugin.Plugin;

import java.util.logging.Logger;
import nu.nethome.home.item.ExecutionFailure;
import nu.nethome.home.item.HomeItemProxy;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * HomeItem class which listens for TCP/IP-connections on the specified port and
 * sends the content of the message as an event of type <b>TCPMessage</b> and
 * the message in the
 * <b>Value</b>-attribute.
 *
 * @author Stefan
 */
@Plugin
@HomeItemType("Ports")
public class MQTTCommandPort extends HomeItemAdapter implements HomeItem {

    private final String model = ("<?xml version = \"1.0\"?> \n"
            + "<HomeItem Class=\"MQTTCommandPort\" Category=\"Ports\" >"
            + "  <Attribute Name=\"Port\" Type=\"String\" Get=\"getPort\" Set=\"setPort\" Default=\"1883\" />"
            + "  <Attribute Name=\"Address\" Type=\"String\" Get=\"getAddress\" Set=\"setAddress\" />"
            + "  <Attribute Name=\"BaseTopic\" Type=\"String\" Get=\"getBaseTopic\" Set=\"setBaseTopic\" />"
            + "</HomeItem> ");

    /*
	 * Externally visible attributes
     */
    protected int port = 1883;
    protected String address = "tcp://localhost";
    protected String baseTopic = "MyHome/#";

    /*
	 * Internal attributes
     */
    private static Logger logger = Logger.getLogger(MQTTCommandPort.class.getName());
    protected CommandLineExecutor executor;
    protected MqttClient client;

    public MQTTCommandPort() {
    }

    /* (non-Javadoc)
	 * @see ssg.home.HomeItem#getModel()
     */
    public String getModel() {
        return model;
    }

    /*
	 * Internal implementation methods
     */
    /**
     * @return Returns the listenPort.
     */
    public String getPort() {
        return String.valueOf(port);
    }

    public void setPort(String listenPort) {
        this.port = Integer.parseInt(listenPort);
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        address = address.trim();
        if(!address.startsWith("tcp://")){
            address = "tcp://" + address;
        }
        this.address = address;
    }

    public String getBaseTopic() {
        return baseTopic;
    }

    public void setBaseTopic(String baseTopic) {
        this.baseTopic = baseTopic;
    }

    @Override
    public void activate(HomeService server) {
        super.activate(server);

        executor = new CommandLineExecutor(server, false);
        try {
            client = new MqttClient(address + ":" + port, "OpenNetHomeServer-Sub");
            client.setCallback(new SubscribeCallback());
            client.connect();
            client.subscribe(baseTopic);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (client != null) {
            try {
                client.disconnect();
            } catch (MqttException ex) {
                logger.log(Level.SEVERE, "MQTT refused to disconnect", ex);
            }
            client = null;
        }
    }

    public class SubscribeCallback implements MqttCallback {

        @Override
        public void connectionLost(Throwable cause) {
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            System.out.println("Message arrived. Topic: " + topic + " Message: " + message.toString());

            try {
                String event = "event,Mqtt_Message,Direction,In,Mqtt.Topic," + topic + ",Mqtt.Message," + message.toString();
                executor.executeCommandLine(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        private boolean hasAction(HomeItemProxy homeItemProxy, String action) {
            return false;
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken imdt) {
        }
    }
}

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

package nu.nethome.home.items.util;

import nu.nethome.home.item.HomeItem;
import nu.nethome.home.item.HomeItemInfo;
import nu.nethome.home.item.HomeItemProxy;
import nu.nethome.home.system.*;
import nu.nethome.util.plugin.PluginProvider;

import java.util.*;

/**
 * User: Stefan
 * Date: 2012-05-29
 * Time: 20:27
 */
public class TstHomeService implements HomeService, ServiceConfiguration {

    public List<HomeItem> items = new LinkedList<HomeItem>();
    protected Map<String, TstHomeItemProxy> m_Instances = new HashMap<String, TstHomeItemProxy>();
    public List<Event> sentEvents = new ArrayList<Event>();


    public int registerInstance(HomeItem item) {
   		items.add(item);
   		return 0;
   	}

    public Event createEvent(String type, String value) {
        return new TstEvent(type, value);
    }

    public void send(Event event) {
        for(HomeItem i : items) {
      			i.receiveEvent(event);
      		}
        sentEvents.add(event);
    }

    public HomeItemProxy openInstance(String name) {
        if (!m_Instances.containsKey(name)) {
      			m_Instances.put(name, new TstHomeItemProxy(name));
      		}
      		return m_Instances.get(name);
    }

    public HomeItemProxy createInstance(String publicClassName, String instanceName) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean renameInstance(String fromInstanceName, String toInstanceName) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean removeInstance(String instanceName) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<DirectoryEntry> listInstances(String pattern) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public List<HomeItemInfo> listClasses() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public PluginProvider getPluginProvider() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public ServiceState getState() {
        return null;
    }

    @Override
    public ServiceConfiguration getConfiguration() {
        return this;
    }

    public void stopServer() {
    }

    @Override
    public void registerFinalEventListener(FinalEventListener listener) {

    }

    @Override
    public void unregisterFinalEventListener(FinalEventListener listener) {

    }

    @Override
    public String getLogDirectory() {
        return "";
    }
    
    @Override
    public boolean executePython(String pythonCode) {
        return true;
    }

	@Override
	public String getValueItemLoggerDescriptor() {
		return null;
	}
}

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

package nu.nethome.home.impl;

import nu.nethome.home.item.*;
import nu.nethome.home.system.HomeService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class LocalHomeItemProxy implements HomeItemProxy {

    private final HomeItem item;
    private final StaticHomeItemModel model;
    private HomeService server;

    public LocalHomeItemProxy(HomeItem item) throws ModelException {
        this(item, null);
    }

    public LocalHomeItemProxy(HomeItem item, HomeService server) throws ModelException {
        this.item = item;
        this.server = server;
        model = StaticHomeItemModel.getModel(item);
    }

    @Override
    public String getAttributeValue(String attributeName) {
        if (attributeName.equals("ID")) {
            return Long.toString(item.getItemId());
        }
        try {
            return model.getAttribute(attributeName).getValue(item);
        } catch (InvocationTargetException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (ModelException e) {
            // Ignore
        }
        return "";
    }

    @Override
    public List<Attribute> getAttributeValues() {
        List<Attribute> result = new ArrayList<Attribute>();

        for (AttributeModel attributeModel : model.getAttributes()) {
            result.add(new ModelAttribute(getAttributeValue(attributeModel.getName()), attributeModel));
        }
        return result;
    }

    @Override
    public boolean setAttributeValue(String attributeName, String attributeValue) throws IllegalValueException {
        try {
            return setAttributeValue(attributeName, attributeValue, isItemActivated());
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof IllegalValueException) {
                throw (IllegalValueException) e.getCause();
            }
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (ModelException e) {
            // Ignore
        }
        return false;
    }

    public boolean setAttributeValue(String attributeName, String attributeValue, boolean isActivated) throws IllegalValueException, ModelException, InvocationTargetException, IllegalAccessException {
        if (isActivated) {
            model.getAttribute(attributeName).setValue(item, attributeValue);
        } else {
            model.getAttribute(attributeName).initValue(item, attributeValue);
        }
        return true;
    }

    private boolean isItemActivated() {
        return !item.getName().startsWith("#");
    }

    @Override
    public String callAction(String actionName) throws ExecutionFailure {
        if (actionName.equals("activate")) {
            item.activate(server);
            return "";
        }
        try {
            return model.getAction(actionName).call(item);
        } catch (InvocationTargetException e) {
            // Ignore
        } catch (IllegalAccessException e) {
            // Ignore
        } catch (ModelException e) {
            // Ignore
        }
        return "";
    }

    @Override
    public HomeItemModel getModel() {
        return model;
    }

    @Override
    public Object getInternalRepresentation() {
        return item;
    }

}

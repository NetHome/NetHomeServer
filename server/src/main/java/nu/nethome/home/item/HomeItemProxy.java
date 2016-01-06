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

package nu.nethome.home.item;

import java.util.List;

/**
 * This is an abstract interface representing a home item. It can
 * be viewed as a generic interface, since attributes and actions are
 * accessed by specifying their names.
 */
public interface HomeItemProxy {

    String ID_ATTRIBUTE = "ID";
    String NAME_ATTRIBUTE = "Name";
    String MODEL_ATTRIBUTE = "Model";

    /**
     * Returns the value for the attribute with the specified name. If the underlying HomeItem class
     * does not have an attribute with the specified name an empty string is returned.
     *
     * Note that there are three built in attributes which are not shown in the XML-specification
     * of an HomeItem: Model, Name and ID.
     *
     * @param attributeName Name of the attribute
     * @return The value of the attribute
     */
	String getAttributeValue(String attributeName);

    /**
     * Returns a list of attributes from the HomeItem. If an attribute name is specified, the list will only contain
     * that attribute, if the attributeName string is empty, all attributes of the HomeItem will be returned.
     * <p/>
     * Note that there are three built in attributes which are not shown in the XML-specification
     * of an HomeItem: Model, Name and ID. They are however not included in the list when no attribute name
     * is specified.
     *
     * @return List of attributes
     */
    List<Attribute> getAttributeValues();

    /**
     * Sets the specified attribute to the specified value. If the specified attribute does not exist in the
     * HomeItem, this operation has no effect. If the attribute does not accept the specified value an
     * IllegalValueException MAY be thrown. The HomeItem may also silently ignore an unacceptable value.
     *
     * The return value is set to true if the attribute was found and is settable. This does not however guarantee
     * that the value was set.
     *
     * @param attributeName Name of the attribute to set
     * @param attributeValue Value to set
     * @return true if the attribute exists and is settable.
     * @throws IllegalValueException
     */
	boolean setAttributeValue(String attributeName,
			String attributeValue) throws IllegalValueException;


    /**
     * Calls the specified action in the HomeItem. If the HomeItem does not have an action with the specified name
     * the operation has no effect.
     * @param actionName name of action to call
     * @return the value from the action
     * @throws ExecutionFailure
     */
	String callAction(String actionName) throws ExecutionFailure;

    /**
     * @return a description of the HomeItem class
     */
    HomeItemModel getModel();

    /**
     * Get the internal implementation of the HomeItem. This should only be used in vary rare
     * cases where you actually need (and know) the internal representation class. Even if you
     * know the internal representation class, this may fail if the representation is accessed
     * via a proxy (if it is located remotely).
     * @return The representation class or null if the representation is not currently available
     */
    Object getInternalRepresentation();

}
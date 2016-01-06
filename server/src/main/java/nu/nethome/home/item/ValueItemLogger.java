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

import java.util.Date;
import java.util.List;

/**
 * Defines an interface of descendant classes that wish to implement a
 * LoggerComponent. Example of such implementations are
 * LoggerComponentH2Database, or LoggerComponentFileBased. Use the
 * LoggerComponentFactory to instantiate any of these classes.
 * 
 * @author Peter Lagerhem
 */
public abstract class ValueItemLogger {

	/**
	 * Constructs the componentType.
	 */
	ValueItemLogger() {
	}

	/**
	 * Store a value into the logger component. It is up to the implementation
	 * how values are stored, and to what media.
	 * 
	 * @param destination
	 *            a destination parameter used locally by the descendant classes
	 * @param itemId
	 *            a unique id associated with values stored to the destination
	 * @param value
	 *            a value to store
	 * @return true if stored, otherwise false.
	 */
	abstract boolean store(String destination, String itemId, String value);

	/**
	 * Load values from the logger component. Values are returned in a list of
	 * Object[] instances.
	 * 
	 * @param destination
	 *            a destination parameter used locally by the descendant classes
	 * @param itemId
	 *            a unique id associated with values stored to the destination
	 * @param from
	 *            a time stamp
	 * @param to
	 *            a time stamp
	 * @return a list of Object[] instances, or an empty list if none are found.
	 */
	public abstract List<Object[]> loadBetweenDates(String destination, String itemId, Date from, Date to);

	/**
	 * Imports a csv based (a semicolon!) file and store to the destination.
	 * 
	 * @param csvFileName
	 *            a csv (a semicolon!) based file
	 * @param destination
	 *            a destination parameter used locally by the descendant classes
	 * @param itemId
	 *            a unique id associated with values stored to the destination
	 * @return
	 */
	public abstract boolean importCsvFile(String csvFileName, String destination, String itemId);

}

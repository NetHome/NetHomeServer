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

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This factory class is responsible for creating and caching exactly one
 * instance of a particular logger component.
 * 
 * @author Peter Lagerhem
 */
public class ValueItemLoggerFactory {

	private static Logger logger = Logger.getLogger(LoggerComponent.class.getName());
	private static HashMap<String, ValueItemLogger> loggerComponentCache = new HashMap<String, ValueItemLogger>();

	public static synchronized ValueItemLogger createValueItemLogger(String loggerComponentDescriptor) {

		String clean = loggerComponentDescriptor.trim().toLowerCase();

		if (clean.trim().startsWith(ValueItemLoggerH2Database.UNIQUE_IDENTIFIER)) {
			if (!loggerComponentCache.containsKey(ValueItemLoggerH2Database.UNIQUE_IDENTIFIER)) {
				loggerComponentCache.put(ValueItemLoggerH2Database.UNIQUE_IDENTIFIER, new ValueItemLoggerH2Database());
				logger.log(Level.INFO, "Enabled global logging of type: LoggerComponentH2Database with descriptor: "
						+ loggerComponentDescriptor);
			}
			return loggerComponentCache.get(ValueItemLoggerH2Database.UNIQUE_IDENTIFIER);
		}

		// Can only be file based left
		if (!loggerComponentCache.containsKey(ValueItemLoggerFileBased.UNIQUE_IDENTIFIER)) {
			loggerComponentCache.put(ValueItemLoggerFileBased.UNIQUE_IDENTIFIER, new ValueItemLoggerFileBased());
			logger.log(Level.INFO, "Enabled global logging of type: LoggerComponentFileBased with descriptor: "
					+ loggerComponentDescriptor);
		}
		return loggerComponentCache.get(ValueItemLoggerFileBased.UNIQUE_IDENTIFIER);
	}

}

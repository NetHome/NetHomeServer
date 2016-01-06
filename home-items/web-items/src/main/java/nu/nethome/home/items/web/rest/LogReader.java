/**
 * Copyright (C) 2005-2014, Stefan Strömberg <stefangs@nethome.nu>
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

package nu.nethome.home.items.web.rest;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import nu.nethome.home.item.HomeItemProxy;
import nu.nethome.home.item.ValueItemLoggerFactory;
import nu.nethome.home.item.ValueItemLoggerFileBased;
import nu.nethome.home.item.ValueItemLogger;
import nu.nethome.home.system.HomeService;
import nu.nethome.home.system.ServiceConfiguration;

public class LogReader {

    private static Logger logger = Logger.getLogger(LogReader.class.getName());
    private static final SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private HomeService service;
    private ServiceConfiguration config;
    private ValueItemLogger valueItemLogger;
    private String currentLoggerComponentDescriptor = "";

    public LogReader(HomeService server) {
        this.service = server;
        this.config = service.getConfiguration();
    }

    /**
     * Returns the component logger that should be used for reading logs. The
     * current selected component logger is defined in the service
     * configuration.
     * 
     * @return ValueItemLogger
     */
    public ValueItemLogger getLoggerComponent(String loggerComponentDescriptor, String itemId) {
        if (StringUtils.isBlank(loggerComponentDescriptor)) {
            // No logging is wanted - check and delete previous logger component
            if (valueItemLogger != null) {
                valueItemLogger = null;
                currentLoggerComponentDescriptor = "";
                logger.log(Level.INFO, "Disabled global logging");
            }
            return null;
        }

        if (loggerComponentDescriptor.compareToIgnoreCase(this.currentLoggerComponentDescriptor) == 0) {
            // Has not been updated, return current logger component.
            return valueItemLogger;
        }

        ValueItemLogger newValueItemLogger = ValueItemLoggerFactory.createValueItemLogger(loggerComponentDescriptor);
        if (newValueItemLogger == null) {
            logger.log(Level.SEVERE,
                    "Can't create ValueItemLogger for " + loggerComponentDescriptor + " and " + itemId);
            return null;
        }

        logger.log(Level.INFO, "Enabled global logging of type: " + newValueItemLogger.getClass().getName()
                + " with descriptor: " + loggerComponentDescriptor);

        valueItemLogger = newValueItemLogger;
        currentLoggerComponentDescriptor = loggerComponentDescriptor;

        return valueItemLogger;
    }

    public List<Object[]> getLog(String startTimeString, String stopTimeString, HomeItemProxy item) throws IOException {
        Date startTime = parseParameterDate(startTimeString);
        Date stopTime = parseParameterDate(stopTimeString);

        if (stopTime == null) {
            stopTime = new Date();
        }
        if (startTime == null) {
            startTime = oneWeekBack(stopTime);
        }

        String fileName = null;
        if (item != null) {

            // Global logger wins if exists!
            fileName = config.getValueItemLoggerDescriptor();

            if (StringUtils.isBlank(fileName)) {
                fileName = item.getAttributeValue("LogFile");
                if (fileName != null) {
                    // TODO: WHY THIS?
                    fileName = fromURL(fileName);
                }
            }
        }

        String itemId = item.getAttributeValue(HomeItemProxy.ID_ATTRIBUTE);
        if (StringUtils.isBlank(fileName) || StringUtils.isBlank(itemId)) {
            return Collections.<Object[]> emptyList();
        }

        ValueItemLogger logger = ValueItemLoggerFactory.createValueItemLogger(fileName);

        // Must handle LoggerComponentFileBased specially
        if (logger instanceof ValueItemLoggerFileBased) {
            fileName = getFullFileName(fileName);
        }

        if (logger == null) {
            return Collections.<Object[]> emptyList();
        }
        return logger.loadBetweenDates(fileName, itemId, startTime, stopTime);
    }

    private String getFullFileName(String fileName) {
        if (fileName.contains(File.separator) || fileName.contains("/")) {
            return fileName;
        } else {
            return service.getConfiguration().getLogDirectory() + fileName;
        }
    }

    private static Date oneWeekBack(Date stopTime) {
        return new Date(stopTime.getTime() - 1000L * 60L * 60L * 24L * 7L);
    }

    private static Date parseParameterDate(String timeString) {
        Date result = null;
        try {
            if (timeString != null) {
                result = inputDateFormat.parse(timeString);
            }
        } catch (ParseException e1) {
            // Silently ignore
        }
        return result;
    }

    public static String fromURL(String aURLFragment) {
        String result = null;
        try {
            result = URLDecoder.decode(aURLFragment, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("UTF-8 not supported", ex);
        }
        return result;
    }

}

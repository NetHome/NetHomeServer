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

import java.io.File;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import nu.nethome.home.system.HomeService;
import nu.nethome.home.system.ServiceConfiguration;

/**
 * This is a component for adding logging capabilities to a Value-item, for
 * example a thermometer. It will automatically sample values from the item
 * specified in the constructor and store them using a logger component type
 * object that is automatically created based on the following:
 * <ul>
 * <li>Local logging by the LogFile field of the NetHome item (if defined).</li>
 * <li>Global logging by the server configuration and Logging field of the
 * HomeServer item (if defined).</li>
 * </ul>
 * This makes it possible to log to a file by entering a file path or database
 * locally to the home item, and/or to a file or database global to all home
 * items. <br/>
 * These fields are parsed as follows:
 * <ul>
 * <li>File log: provide a file name optionally prefixed with file:.<br/>
 * Examples: 'file:mylogfile.log', 'mylogfile.log' or '/var/log/mylogfile.log'
 * </li>
 * <li>Database log: provide a database file name prefixed with jdbc:h2:.<br/>
 * Example: 'jdbc:h2:~/mydblog.log', 'jdbc:h2:/var/log/mydblog.log'</li>
 * </ul>
 * To add this component, add the following lines to a Value-Item:<br>
 * In Model: <br>
 * 
 * <pre>
 * {@code
 * +"  <Attribute Name=\"LogFile\" Type=\"String\" Get=\"getLogFile\" 	Set=\"setLogFile\" />"
 * }
 * </pre>
 * 
 * As attribute:<br>
 * 
 * <pre>
 * protected LoggerComponent m_TempLogger = new LoggerComponent(this);
 * </pre>
 * 
 * In Activate:<br>
 * 
 * <pre>
 * m_TempLogger.activate();
 * </pre>
 * 
 * In stop:<br>
 * 
 * <pre>
 * m_TempLogger.stop();
 * </pre>
 * 
 * For access:<br>
 * 
 * <pre>
 * public String getLogFile() {
 *     return m_TempLogger.getFileName();
 * }
 * 
 * public void setLogFile(String LogFile) {
 *     m_TempLogger.setFileName(LogFile);
 * }
 * </pre>
 *
 * @author Stefan Strömberg
 * @author Peter Lagerhem - added ComponentLoggerType with file and database
 *         support.
 */
public class LoggerComponent extends TimerTask {

    private Timer logTimer = new Timer("Logger Component", true);
    private static Logger logger = Logger.getLogger(LoggerComponent.class.getName());
    private boolean loggerIsActivated = false;
    private boolean loggerIsRunning = false;
    private String logDirectoryPath = "";
    // Public attributes
    private String logFileName = "";
    private int logInterval = 15;

    private ValueItem loggedItem = null;
    protected String homeItemId;
    protected HomeService service;
    protected ServiceConfiguration config;

    public LoggerComponent(ValueItem logged) {
        loggedItem = logged;
    }

    public void activate() {
        loggerIsActivated = true;
        if (logFileName.length() == 0) {
            return;
        }
        // Get current time
        Calendar date = Calendar.getInstance();
        // Start at next even hour
        date.set(Calendar.HOUR, date.get(Calendar.HOUR) + 1);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        // Schedule the job at m_Interval minutes interval
        logTimer.schedule(this, date.getTime(), 1000L * 60 * logInterval);
        loggerIsRunning = true;
    }

    protected void activate(String logDirectoryPath) {
        this.logDirectoryPath = logDirectoryPath;
        activate();
    }

    /**
     * HomeItem method which stops all object activity for program termination
     */
    public void stop() {
        logTimer.cancel();
        loggerIsRunning = false;
        loggerIsActivated = false;
    }

    public void run() {
        logger.fine("Value Log Timer Fired");
        String value = loggedItem.getValue();
        if (value.length() > 0) {
            storeValue(value);
        }
    }

    /**
     * Optionally stores to the local logger and optionally to the global
     * logger.
     * 
     * @param value
     */
    private void storeValue(String value) {

        if (homeItemId.equals("0")) {
            // Check if we are missing the home item id value
            homeItemId = Long.toString(((HomeItem) loggedItem).getItemId());
            logger.log(Level.INFO, "Was missing home item id, now set to: " + homeItemId);
        }

        // Check and log to global logger
        ValueItemLogger logger = ValueItemLoggerFactory.createValueItemLogger(config.getValueItemLoggerDescriptor());
        if (logger != null) {
            logger.store(config.getValueItemLoggerDescriptor(), homeItemId, value);
        }

        // Check and log to local logger
        logger = ValueItemLoggerFactory.createValueItemLogger(logFileName);
        if (logger != null) {
            logger.store(getFullFileName(), homeItemId, value);
        }
    }

    private String getFullFileName() {
        if (logFileName.contains(File.separator) || logFileName.contains("/")) {
            return logFileName;
        } else {
            return logDirectoryPath + logFileName;
        }
    }

    /**
     * @return Returns the FileName.
     */
    public String getFileName() {
        return logFileName;
    }

    /**
     * @param fileName
     *            The FileName to set.
     */
    public void setFileName(String fileName) {
        logFileName = fileName;
        // If we got a file name, and we are activated but not running - then start
        if ((fileName.length() != 0) && loggerIsActivated && !loggerIsRunning) {
            activate();
        }
    }

    /**
     * @return Returns the Interval.
     */
    public String getInterval() {
        return Integer.toString(logInterval);
    }

    /**
     * @param interval
     *            The Interval to set.
     */
    public void setInterval(String interval) {
        logInterval = Integer.parseInt(interval);
    }

    /**
     * @return the IsActivated
     */
    public boolean isActivated() {
        return loggerIsActivated;
    }

}

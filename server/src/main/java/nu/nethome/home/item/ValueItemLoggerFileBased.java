/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nu.nethome.home.item;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

/**
 * This is a file based implementation of the ValueItemLogger.
 * 
 * @author Peter Lagerhem, 2015-12-30
 */
public class ValueItemLoggerFileBased extends ValueItemLogger {

	private static Logger logger = Logger.getLogger(LoggerComponent.class.getName());
	private String logTimeFormat = "yyyy.MM.dd HH:mm:ss;";
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private static final SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	private static final int LINE_LENGTH = 21;
	public static String UNIQUE_IDENTIFIER = "file:";
	public static String KEYWORD_ITEMID = "$ITEMID$";

	/**
	 * Parse out keywords in provided filename by removing optional prefix file:
	 * and replace found $ITEMID$.
	 * 
	 * @param fileName
	 *            a provided file name that can include prefix and keyword.
	 * @return parsed file name
	 */
	public String parseFileName(String fileName, String itemId) {
		String name = fileName.trim();

		if (name.startsWith(UNIQUE_IDENTIFIER)) {
			name = name.replace(UNIQUE_IDENTIFIER, "");
		}
		if (name.startsWith(KEYWORD_ITEMID)) {
			name = name.replace(KEYWORD_ITEMID, itemId);
		}

		return name;
	}

	@Override
	boolean store(String destination, String itemId, String value) {
		boolean result = true;
		BufferedWriter out = null;
		String fileName = parseFileName(destination, itemId);
		try {
			out = new BufferedWriter(new FileWriter(fileName, true));
			// Format the current time.
			SimpleDateFormat formatter = new SimpleDateFormat(logTimeFormat);
			Date currentTime = new Date();
			String newLogLine = formatter.format(currentTime) + value;
			out.write(newLogLine);
			out.newLine();
		} catch (IOException e) {
			logger.warning("Failed to open log file: " + fileName + " Error:" + e.toString());
			result = false;
		} finally {
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					// Ignore
				}
			}
		}

		return result;
	}

	@Override
	public List<Object[]> loadBetweenDates(String destination, String itemId, Date startTime, Date stopTime) {
		List<Object[]> result = new ArrayList<Object[]>();

		BufferedReader br = null;

		try {
			// Open the data file
			FileReader reader = new FileReader(destination);
			Long startTimeMs = startTime.getTime();
			Long month = 1000L * 60L * 60L * 24L * 30L;
			boolean doOptimize = true;
			boolean justOptimized = false;
			br = new BufferedReader(reader);
			String line;
			try {
				while ((line = br.readLine()) != null) {
					try {
						// Get next log entry
						if (line.length() > LINE_LENGTH) {
							// Adapt the time format
							String minuteTime = line.substring(0, 16).replace('.', '-');
							// Parse the time stamp

							Date min = fileDateFormat.parse(minuteTime);

							// Ok, this is an ugly optimization. If the current
							// time position in the file
							// is more than two months (60 days) ahead of the
							// start of the time window, we
							// quick read 1 month worth of data, assuming that
							// there is 4 samples per hour.
							// This may lead to scanning past start of window if
							// there are holes in the data
							// series.
							if (doOptimize && ((startTimeMs - min.getTime()) > month * 2)) {
								justOptimized = true;
								continue;
							}
							// Detect if we have scanned past the window start
							// position just after an optimization scan.
							// If this is the case it may be because of the
							// optimization. In that case we have to switch
							// optimization off and start over.
							if ((min.getTime() > startTimeMs) && doOptimize && justOptimized) {
								reader.reset();
								doOptimize = false;
								continue;
							}
							justOptimized = false;
							// Check if value is within time window
							if ((min.getTime() > startTimeMs) && (min.getTime() < stopTime.getTime())) {
								// Parse the value
								double value = Double.parseDouble((line.substring(20)).replace(',', '.'));
								// Add the entry
								Object[] row = { dateFormat.format(min), value };
								result.add(row);
								doOptimize = false;
							}
						}
					} catch (NumberFormatException nfe) {
						// Bad number format in a line, try to continue
					}
				}
			} catch (Exception e) {
				System.out.println(e.toString());
			} finally {
				try {
					br.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException f) {
			System.out.println(f.toString());
		}
		return result;
	}

	@Override
	public boolean importCsvFile(String csvFileName, String destination, String itemId) {
		// TODO Auto-generated method stub
		return false;
	}

}

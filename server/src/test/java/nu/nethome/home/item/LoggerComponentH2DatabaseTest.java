/**
 * Copyright (C) 2005-2016, Stefan Strömberg <stefangs@nethome.nu>
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

import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.h2.tools.DeleteDbFiles;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.Assert;

/**
 * This junit test will create a temporary database on disk. The database is
 * deleted afterwards.
 * 
 * @author Peter Lagerhem
 */
public class LoggerComponentH2DatabaseTest {

	String connectionString = "jdbc:h2:~/test";

	public LoggerComponentH2DatabaseTest() {
	}

	@BeforeClass
	public static void setUpClass() {
		DeleteDbFiles.execute("~", "test", true);
	}

	@AfterClass
	public static void tearDownClass() {
		DeleteDbFiles.execute("~", "test", true);
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	/**
	 * Test of store method, of class LoggerComponentH2Database.
	 */
	@Test
	public void testStore() {
		System.out.println("store");

		Calendar temp = Calendar.getInstance();
		Calendar now = Calendar.getInstance();
		Calendar then = Calendar.getInstance();
		now.set(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH), temp.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
		then.set(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH), temp.get(Calendar.DAY_OF_MONTH), 23, 59, 59);

		String homeItemId = "1";

		// First save some values
		ValueItemLoggerH2Database instance = new ValueItemLoggerH2Database();
		assertTrue(instance.store(connectionString, homeItemId, "1,0"));
		try {
            Thread.sleep(1010);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
		assertTrue(instance.store(connectionString, homeItemId, "10,25"));

		// Now try to read back the values
		List<Object[]> result = instance.loadBetweenDates(connectionString, homeItemId, now.getTime(), then.getTime());
		System.out.println("Size of result: " + result.size());

        for (Object[] o : result) {
            for (Object anO : o) {
                System.out.println(anO.toString());
            }
        }
		assertTrue(result.size() == 2);
	}
	
	@Test
	public void testSQLDate() {
	    String line = "2016.03.08 20:45:00;0.0";
        String minuteTime = line.substring(0, 16).replace('.', '-');
        SimpleDateFormat fileDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date min;
        try {
            min = fileDateFormat.parse(minuteTime);
            @SuppressWarnings("deprecation")
            java.sql.Timestamp sqlDate = new java.sql.Timestamp(min.getYear(), min.getMonth(), min.getDay(), min.getHours(), min.getMinutes(), 0, 0);
            int t = 2;
            t++;
        } catch (ParseException e) {
            Assert.fail("Nope");
        }
	}
}

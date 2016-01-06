/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package nu.nethome.home.item;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.h2.tools.DeleteDbFiles;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.python.icu.util.Calendar;

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
		assertTrue(instance.store(connectionString, homeItemId, "10,25"));

		// Now try to read back the values
		List<Object[]> result = instance.loadBetweenDates(connectionString, homeItemId, now.getTime(), then.getTime());
		System.out.println("Size of result: " + result.size());

		Iterator<Object[]> iter = result.iterator();
		while (iter.hasNext()) {
			Object[] o = iter.next();
			for (int i = 0; i < o.length; i++) {
				System.out.println(o[i].toString());
			}
		}

		assertTrue(result.size() == 2);
	}

}

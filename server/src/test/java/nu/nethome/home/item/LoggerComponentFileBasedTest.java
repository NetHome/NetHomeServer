package nu.nethome.home.item;

import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;

import org.h2.store.fs.FileUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.python.icu.util.Calendar;

public class LoggerComponentFileBasedTest {

    static String connectionString = "junittest-LoggerComponentFileBasedTest";

	// static String connectionString = "~" + File.separator + "junittest";

	@BeforeClass
	public static void setUpClass() {
		if (FileUtils.isDirectory(connectionString)) {
			FileUtils.tryDelete(connectionString);
		} else  {
			FileUtils.tryDelete(connectionString);
		}
	}

	@AfterClass
	public static void tearDownClass() {
		if (FileUtils.isDirectory(connectionString)) {
			FileUtils.tryDelete(connectionString);
		} else  {
			FileUtils.tryDelete(connectionString);
		}
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testStore() {
        System.out.println("store");

        Calendar temp = Calendar.getInstance();
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        now.set(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH),  temp.get(Calendar.DAY_OF_MONTH), 0, 0, 0);
        then.set(temp.get(Calendar.YEAR), temp.get(Calendar.MONTH),  temp.get(Calendar.DAY_OF_MONTH), 23, 59, 59);

		String homeItemId = "1";
		
        // First save some values
        ValueItemLoggerFileBased instance = new ValueItemLoggerFileBased();
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

package org.binas.station.ws.it;

import static org.junit.Assert.assertEquals;

import org.binas.station.ws.BadInit_Exception;
import org.binas.station.ws.NoBinaAvail_Exception;
import org.binas.station.ws.StationView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class GetBinaMethodTest extends BaseIT {
	
	@Before
	public void setUp() {
	}
	
	@Test
	public void success() throws BadInit_Exception, NoBinaAvail_Exception {
		client.testInit(10, 10, 5, 5);
		client.getBina();
		StationView station = client.getInfo();
		assertEquals(4, station.getAvailableBinas());
	}
	
	@Test(expected = NoBinaAvail_Exception.class)
	public void noBinaAvailable() throws BadInit_Exception, NoBinaAvail_Exception {
		client.testInit(10, 10, 1, 5);
		for(int i = 0; i<2; i++)
			client.getBina();
	}
	
	@After
	public void tearDown() {
		// clear remote service state after all tests
		client.testClear();
	}
}

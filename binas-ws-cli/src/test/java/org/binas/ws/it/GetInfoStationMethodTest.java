package org.binas.ws.it;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import org.binas.ws.BadInit_Exception;
import org.binas.ws.CoordinatesView;
import org.binas.ws.EmailExists_Exception;
import org.binas.ws.InvalidEmail_Exception;
import org.binas.ws.InvalidStation_Exception;
import org.binas.ws.StationView;
import org.binas.ws.UserNotExists_Exception;
import org.binas.ws.cli.BinasClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class GetInfoStationMethodTest extends BaseIT {
	@Before
	public void setUp()
	{
		try {
			client.testInitStation("A67_Station1", 4, 4, 20, 1);
			client.testInitStation("A67_Station2", 30, 30, 20, 1);
			client.testInitStation("A67_Station3", 99, 99, 20, 1);
		} catch (BadInit_Exception e) {
			System.out.println("Bad Init Exception");
			e.printStackTrace();
		}
	}
	
	@Test
	public void success() throws InvalidStation_Exception {
		StationView sv = client.getInfoStation("A67_Station2");
		assertEquals("A67_Station2", sv.getId());
	}
	
	
	@Test(expected = InvalidStation_Exception.class)
	public void invalidStation() throws InvalidStation_Exception{
		client.getInfoStation("A67_Station0");
	}
	
	@After
	public void tearDown() {
		// clear remote service state after all tests
		client.testClear();
	}

}

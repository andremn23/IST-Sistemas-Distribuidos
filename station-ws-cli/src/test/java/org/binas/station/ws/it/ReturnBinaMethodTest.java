package org.binas.station.ws.it;

import static org.junit.Assert.assertEquals;

import org.binas.station.ws.BadInit_Exception;
import org.binas.station.ws.NoBinaAvail_Exception;
import org.binas.station.ws.NoSlotAvail_Exception;
import org.binas.station.ws.StationView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReturnBinaMethodTest extends BaseIT{
	
	@Before
	public void setUp() {
		
	}
	
	@Test
	public void success() throws BadInit_Exception, NoBinaAvail_Exception, NoSlotAvail_Exception {
		client.testInit(10, 10, 5, 5);
		client.getBina();
		client.returnBina();
		StationView station = client.getInfo();
		assertEquals(5, station.getAvailableBinas());
	}
	
	@Test(expected = NoSlotAvail_Exception.class)
	public void noSlotAvailable() throws BadInit_Exception, NoBinaAvail_Exception, NoSlotAvail_Exception {
		client.testInit(10, 10, 5, 5);
		client.returnBina();
	}
	
	
	@After
	public void tearDown() {
		// clear remote service state after all tests
		client.testClear();
	}

}

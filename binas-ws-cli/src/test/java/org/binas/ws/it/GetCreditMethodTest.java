package org.binas.ws.it;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import org.binas.ws.EmailExists_Exception;
import org.binas.ws.InvalidEmail_Exception;
import org.binas.ws.UserNotExists_Exception;
import org.binas.ws.cli.BinasClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class GetCreditMethodTest extends BaseIT {
	
	@Before
	public void setUp(){		
	}
	
	@Test
	public void success() throws UserNotExists_Exception, EmailExists_Exception, InvalidEmail_Exception {
		client.activateUser("joao@gmail.com");
		assertEquals(10, client.getCredit("joao@gmail.com"));
	}
	
	
	@Test(expected = UserNotExists_Exception.class)
	public void userNotExits() throws UserNotExists_Exception{
		client.getCredit("asdasdasda@hotmail.com");
	}
	
	@After
	public void tearDown() {
		// clear remote service state after all tests
		client.testClear();
	}

}

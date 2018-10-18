package org.binas.ws.it;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Properties;

import org.binas.ws.EmailExists_Exception;
import org.binas.ws.InvalidEmail_Exception;
import org.binas.ws.UserNotExists_Exception;
import org.binas.ws.UserView;
import org.binas.ws.cli.BinasClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


public class ActivateUserMethodTest extends BaseIT {
	
	@Before
	public void setUp(){		
	}
	
	@Test
	public void success() throws EmailExists_Exception, InvalidEmail_Exception {
		UserView uv1 = client.activateUser("methodtest@testmethod");
		UserView uv2 = new UserView();
		uv2.setEmail("methodtest@testmethod");
		uv2.setCredit(10);
		uv2.setHasBina(false);
		assertEquals(uv2.getEmail(), uv1.getEmail());
	}
	
	@Test(expected = EmailExists_Exception.class)
	public void emailExists() throws EmailExists_Exception, InvalidEmail_Exception{
		client.activateUser("methodtest@testmethod");
		client.activateUser("methodtest@testmethod");
	}
	
	@Test(expected = InvalidEmail_Exception.class)
	public void invalidEmailNoUser() throws EmailExists_Exception, InvalidEmail_Exception{
		client.activateUser("@testmethod");
	}
	
	@Test(expected = InvalidEmail_Exception.class)
	public void invalidEmailNoDomain() throws EmailExists_Exception, InvalidEmail_Exception{
		client.activateUser("testmethod@");
	}
	
	@Test(expected = InvalidEmail_Exception.class)
	public void invalidEmailWithSpaces() throws EmailExists_Exception, InvalidEmail_Exception{
		client.activateUser("test method@testmethod");
	}
	
	@Test(expected = InvalidEmail_Exception.class)
	public void invalidEmailWrongDots() throws EmailExists_Exception, InvalidEmail_Exception{
		client.activateUser("testWrongDots.@testmethod");
	}
	
	@After
	public void tearDown() {
		// clear remote service state after all tests
		client.testClear();
	}

}

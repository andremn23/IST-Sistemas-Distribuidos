package org.binas.ws.cli;

import java.util.List;

import org.binas.ws.BadInit_Exception;
import org.binas.ws.CoordinatesView;
import org.binas.ws.InvalidStation_Exception;
import org.binas.ws.StationView;
import org.binas.ws.UserNotExists_Exception;

/**
 * Class that contains the main of the BinasClient
 * 
 * Looks for Binas using arguments that come from pom.xm
 *
 */
public class BinasClientApp {

    public static void main(String[] args) throws Exception {
        // Check arguments
        if (args.length == 0) {
            System.err.println("Argument(s) missing!");
            System.err.println("Usage: java " + BinasClientApp.class.getName()
                    + " wsURL OR uddiURL wsName");
            return;
        }
        String uddiURL = null;
        String wsName = null;
        String wsURL = null;
        if (args.length == 1) {
            wsURL = args[0];
        } else if (args.length >= 2) {
            uddiURL = args[0];
            wsName = args[1];
        }

        // Create client
        BinasClient client = null;

        if (wsURL != null) {
            System.out.printf("Creating client for server at %s%n", wsURL);
            client = new BinasClient(wsURL);
        } else if (uddiURL != null) {
            System.out.printf("Creating client using UDDI at %s for server with name %s%n",
                uddiURL, wsName);
            client = new BinasClient(uddiURL, wsName);
        }

        /** Codigo para testar as mensagens SOAP */
        
        //System.out.println("Invoke ping()...");
        //String result = client.testPing("client");
        //System.out.print(result);

        System.out.println("Activating User...");
        client.activateUser("alice@A67.binas.org");

        System.out.println("Renting Bina...");
        client.rentBina("A67_Station1", "alice@A67.binas.org");

        /*System.out.println("Return Bina...");
        client.returnBina("A67_Station1", "alice@A67.binas.org");

        System.out.println("Get credit...");
        client.getCredit("alice@A67.binas.org");  */
        
	 }
}


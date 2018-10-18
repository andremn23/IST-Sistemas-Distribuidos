package example.ws.handler;

import org.w3c.dom.NodeList;
import pt.ulisboa.tecnico.sdis.kerby.*;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.security.Key;
import java.util.Iterator;
import java.util.Set;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;




public class BinasAuthorizationHandler implements SOAPHandler<SOAPMessageContext> {

    /** Password Local para o servidor Kerby
    private static final String VALID_SERVER_PASSWORD = "MTbvC3"; */

    /** Password Kerby-RNL para o servidor Kerby */
    private static final String VALID_SERVER_PASSWORD = "uOJ97RLW";

    /** Variável que guarda a server key */
    Key serverKey;



    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);


        if (!outbound) {
            try {
                // get SOAP envelope header
                serverKey = SecurityHelper.generateKeyFromPassword(VALID_SERVER_PASSWORD);
                SOAPMessage msg = smc.getMessage();
                SOAPPart sp = msg.getSOAPPart();
                SOAPEnvelope se = sp.getEnvelope();
                SOAPHeader sh = se.getHeader();
                SOAPBody sb = msg.getSOAPBody();

                // Verifica se o body está vazio, se estiver o handler termina e retorna
                if (sb == null) {
                    System.out.println("BinasAuthHandler: Body not found.");
                    return true;
                }

                /** Obtem o ticket a partir do header */

                // Váriavel que guarda o ticket cifrado recebido pelo servidor
                CipheredView cipheredticket = new CipheredView();
                Name nameTicket = se.createName("ticket", "t", "http://ticket");
                Iterator<?> it = sh.getChildElements(nameTicket);
                if (!it.hasNext()) {
                    System.out.println("BinasAuthHandler: Header element not found.");
                } else {
                    SOAPElement element = (SOAPElement) it.next();
                    byte[] byteTicket = parseBase64Binary(element.getValue());
                    CipherClerk ticketFromHeader = new CipherClerk();
                    cipheredticket = ticketFromHeader.cipherFromXMLBytes(byteTicket);
                }

                // Decifra o ticket com a server key
                Ticket decipheredTicket = null;
                String ticketEmail = null;
                try {
                    decipheredTicket = new Ticket(cipheredticket,serverKey);
                    decipheredTicket.validate();
                    ticketEmail = decipheredTicket.getX();
                } catch (KerbyException e) {
                    System.out.println("BinasAuthHandler Erro: não foi possível decifrar o ticket no servidor");
                    throw new RuntimeException();
                    //e.printStackTrace();
                }

                // Obtém o elemento do body que contém o email a partir da respectiva tag
                NodeList emailTag = se.getBody().getElementsByTagName("email");
                if (emailTag.item(0) == null) {
                    System.out.printf("BinasAuthHandler: Body vazio");
                    return true;
                }
                String bodyEmail = emailTag.item(0).getTextContent();
                if (bodyEmail.equals(ticketEmail)){
                    System.out.println("BinasAuthHandler: Email do pedido corresponde ao email autenticado");

                } else {
                    System.out.println("BinasAuthHandler Erro: A mensagem foi modificada e o email do pedido não corresponde ao email autenticado");
                    throw new RuntimeException();

                }
            }catch (Exception e) {
                System.out.print("BinasAuthHandler: Caught exception in handleMessage: ");
                System.out.println(e);
                System.out.println("BinasAuthHandler: O programa irá terminar...");
                throw new RuntimeException();
            }

        } else {
                // Outbound
                // do nothing
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        return false;
    }

    @Override
    public void close(MessageContext context) {

    }
}

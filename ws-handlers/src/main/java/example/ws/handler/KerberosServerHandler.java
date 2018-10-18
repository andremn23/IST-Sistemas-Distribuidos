package example.ws.handler;

import pt.ulisboa.tecnico.sdis.kerby.*;
import sun.misc.Request;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.util.*;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;


public class KerberosServerHandler implements SOAPHandler<SOAPMessageContext> {

    /** Password Local para o servidor Kerby
    private static final String VALID_SERVER_PASSWORD = "MTbvC3"; */

    /** Password Kerby-RNL para o servidor Kerby */
     private static final String VALID_SERVER_PASSWORD = "uOJ97RLW";

    /** Variável que guarda a client key gerada com base na password */
    private Key serverKey;
    /** Variável que guarda a session key recebida no ticket enviado pelo cliente */
    private Key sessionKey;
    /** Date formatter para ser usado no auth */
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    /** Lista que guarda todas as datas dos pedidos recebidos */
    List<Date> receivedDates = new ArrayList<Date>();



    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {
        //Quando a mensagem do pedido chega ao servidor, é intercetada pelo KerberosServerHandler, que vai aceder aos "acrescentos" e fazer as validações necessárias.
        Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);


        if (!outbound) {
            try {
                // get SOAP envelope header
                SOAPMessage msg = smc.getMessage();
                SOAPPart sp = msg.getSOAPPart();
                SOAPEnvelope se = sp.getEnvelope();
                SOAPHeader sh = se.getHeader();

                // check header
                if (sh == null) {
                    System.out.println("Server handler: Header not found.");
                    return true;
                }

                /** Obtém o Ticket e o auth a partir do header */

                // Obtem primeiro o ticket

                // Váriavel que guarda o ticket cifrado recebido pelo servidor
                CipheredView cipheredticket = new CipheredView();

                Name nameTicket = se.createName("ticket", "t", "http://ticket");
                Iterator<?> it = sh.getChildElements(nameTicket);
                if (!it.hasNext()) {
                    System.out.println("Server handler: Header element not found.");
                    //return true;
                } else {

                    SOAPElement element = (SOAPElement) it.next();
                    byte[] byteTicket = parseBase64Binary(element.getValue());
                    CipherClerk ticketFromHeader = new CipherClerk();
                    cipheredticket = ticketFromHeader.cipherFromXMLBytes(byteTicket);

                }

                // Obtem de seguida o auth
                CipheredView cipheredAuth = new CipheredView();
                //Auth auth = null;
                Name nameAuth = se.createName("auth", "a", "http://auth");
                Iterator<?> it2 = sh.getChildElements(nameAuth);
                if (!it2.hasNext()) {
                    System.out.println("Server handler: Header element not found.");
                    throw new RuntimeException();
                    //return true;
                } else {
                    SOAPElement element2 = (SOAPElement) it2.next();
                    byte[] byteAuth = parseBase64Binary(element2.getValue());
                    CipherClerk authFromHeader = new CipherClerk();
                    cipheredAuth = authFromHeader.cipherFromXMLBytes(byteAuth);

                }


                /** Verificar a frescura do pedido ao analisar o date */

                serverKey = SecurityHelper.generateKeyFromPassword(VALID_SERVER_PASSWORD);

                 // Código para decifrar um Ticket
                Ticket decipheredTicket = null;
                try {
                    decipheredTicket = new Ticket(cipheredticket,serverKey);
                    decipheredTicket.validate();
                    sessionKey = decipheredTicket.getKeyXY();
                    smc.put("sessionKey", sessionKey);
                    smc.setScope("sessionKey", MessageContext.Scope.APPLICATION);
                } catch (KerbyException e) {
                    System.out.println("Server handler Erro: não foi possível decifrar o ticket no servidor");
                    //e.printStackTrace();
                }

                // Código para decifrar um Auth
                Auth decipheredAuth = null;
                try {
                    decipheredAuth = new Auth(cipheredAuth,sessionKey);
                    smc.put("decAuthDate", decipheredAuth.getTimeRequest());
                    smc.setScope("decAuthDate", MessageContext.Scope.APPLICATION);

                } catch (KerbyException e) {
                    System.out.println("Server handler Erro: não foi possível decifrar o auth no servidor");
                    throw new RuntimeException();
                    //e.printStackTrace();
                }


                /** Verificar se o timestamp to cliente está entre T1 e T2 e se já foi recebido anteriormente */
                try{
                    Date t1 = decipheredTicket.getTime1();
                    Date t2 = decipheredTicket.getTime2();
                    if (decipheredAuth.getTimeRequest().before(t2) && decipheredAuth.getTimeRequest().after(t1)){
                        if(receivedDates.contains(decipheredAuth.getTimeRequest())){
                            throw new KerbyException();
                        } else {
                            System.out.println("Server handler: Timestamp validado com sucesso");
                            receivedDates.add(decipheredAuth.getTimeRequest());
                        }
                    }else {
                        System.out.print("Server handler Erro: O timestamp to auth não está entre T1 e T2");
                        throw new Exception();
                    }
                } catch(KerbyException e){
                    System.out.print("Server handler Erro: O timestamp to auth já foi recebido anteriormente");
                    throw new RuntimeException();

                }

            } catch (Exception e) {
                System.out.print("Server handler: Caught exception in handleMessage: ");
                System.out.println("Server handler: A execução do programa vai terminar devido a  %s%n" + e);
                throw new RuntimeException();
            }

        }
        // Código para mensagem de OUTBOUND
        else {

            /** Adiciona request Time (tReq) à mensagem soap a ser enviada ao cliente */

            // Converte o treq para XMLBytes para que possa ser adicionado ao header
            Date treqDate = (Date) smc.get("decAuthDate");
            RequestTime treq = new RequestTime(treqDate);
            CipheredView cipheredTReq = null;
            byte[] strTreq = null;
            try {
                cipheredTReq = treq.cipher(sessionKey);
                CipherClerk treqToHeader = new CipherClerk();
                strTreq = treqToHeader.cipherToXMLBytes(cipheredTReq, "treq");
            } catch (KerbyException e) {
                e.printStackTrace();
            } catch (JAXBException e) {
                e.printStackTrace();
            }
            String encodedTreq = printBase64Binary(strTreq);


            // Adiciona o ticket ao header
            try {
                SOAPMessage msg = smc.getMessage();
                SOAPPart sp = msg.getSOAPPart();
                SOAPEnvelope se = sp.getEnvelope();
                SOAPHeader sh = se.getHeader();
                if (sh == null)
                    sh = se.addHeader();
                // Adiciona o ticket no header criado
                Name name = se.createName("treq", "r", "http://treq");
                SOAPHeaderElement element = sh.addHeaderElement(name);
                element.addTextNode(encodedTreq );
                System.out.printf("Server handler: Treq adicionado ao SOAP Header%n");
            } catch (SOAPException e) {
                System.out.printf("Server handler: Failed to add SOAP header because of %s%n", e);
            }
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

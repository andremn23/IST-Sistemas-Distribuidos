package example.ws.handler;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.Properties;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import com.sun.xml.ws.client.ClientTransportException;
import pt.ulisboa.tecnico.sdis.kerby.*;
import pt.ulisboa.tecnico.sdis.kerby.cli.KerbyClient;
import pt.ulisboa.tecnico.sdis.kerby.KerbyException;
import pt.ulisboa.tecnico.sdis.kerby.cli.KerbyClientException;
import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;

public class KerberosClientHandler implements SOAPHandler<SOAPMessageContext> {

    /** Date formatter para ser usado no auth */
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    /** XML transformer factory. */
    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();
    /** XML transformer property name for XML indentation amount. */
    private static final String XML_INDENT_AMOUNT_PROPERTY = "{http://xml.apache.org/xslt}indent-amount";
    /** XML indentation amount to use (default=0). */
    private static final Integer XML_INDENT_AMOUNT_VALUE = 2;

    private static SecureRandom randomGenerator = new SecureRandom();

    /** Dados local
    private static final String VALID_CLIENT_NAME = "alice@CXX.binas.org";
    private static final String VALID_CLIENT_PASSWORD = "Zd8hqDu23t";
    private static final String VALID_SERVER_NAME = "binas@CXX.binas.org"; */

    /** Dados Kerby RNL */
    private static final String VALID_CLIENT_NAME = "alice@A67.binas.org";
    private static final String VALID_CLIENT_PASSWORD = "eDxwydkP4";
    private static final String VALID_SERVER_NAME = "binas@A67.binas.org";


    private static final String LOCAL_KERBY_SERVER = "http://localhost:8888/kerby";
    private static final String RNL_KERBY_SERVER = "http://sec.sd.rnl.tecnico.ulisboa.pt:8888/kerby.";



    private static final int VALID_DURATION = 30;
    protected static KerbyClient client;
    protected static Properties testProps;

    // Variável que guarda a resposta do Kerby a um ticket request
    SessionKeyAndTicketView ticketReq;
    // Variável que guarda a client key gerada com base na password
    Key clientKey;
    // Variável que guarda a session key partilhada entre o servidor e o cliente
    Key sessionKey;

    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    /**
     * The handleMessage method is invoked for normal processing of inbound and
     * outbound messages.
     */
    @Override
    public boolean handleMessage(SOAPMessageContext smc) {

        Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
        if (outbound) {
            // outbound message

            // Gera a client key com base na password do utilizador
            try {
                clientKey = SecurityHelper.generateKeyFromPassword(VALID_CLIENT_PASSWORD);  // Equivalente à funçao getKey()
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
            }

            // Gera o nounce a ser enviado para o SAuth
            long nounce = randomGenerator.nextLong();


            /** Instancia um cliente que se conecta a ao servidor em wsURL */

            try {
                //client = new KerbyClient(LOCAL_KERBY_SERVER);
                client = new KerbyClient(RNL_KERBY_SERVER);
            } catch (KerbyClientException e) {
                System.out.println();
                System.out.println("Client Handler Erro: Excepção capturada - não foi possível instanciar KerbyClient devido a: %s%n " + e);
                throw new RuntimeException();
                //e.printStackTrace();
            }


            /** Cliente envia C, S e nounce para obter chave de sessão e ticket */
            try {
                ticketReq = client.requestTicket(VALID_CLIENT_NAME, VALID_SERVER_NAME, nounce, VALID_DURATION);
            } catch (BadTicketRequest_Exception b) {
                System.out.println();
                System.out.println("Client Handler Erro: Excepção capturada - não foi possível efetuar o request Ticket devido a: %s%n " + b);
                throw new RuntimeException();
                //b.printStackTrace();
            } catch (ClientTransportException cte) {
                System.out.println();
                System.out.println("Client Handler Erro: Excepção capturada - não foi possível conectar ao servidor Kerbyd devido a: %s%n " + cte);
                //cte.printStackTrace();
                return false;

            } catch (RuntimeException r) {
                return false;
            }

            // Ticket e session key devolvidos pelo SAuth do Kerby
            CipheredView cipheredSessionKey = ticketReq.getSessionKey();
            CipheredView cipheredTicket = ticketReq.getTicket();

            // Código para decifrar uma session key
            try {
                SessionKey decipheredSK = new SessionKey(cipheredSessionKey, clientKey);
                sessionKey = decipheredSK.getKeyXY();
                long receivedNounce = decipheredSK.getNounce();
                smc.put("sessionKey", decipheredSK.getKeyXY());
                smc.setScope("sessionKey", MessageContext.Scope.APPLICATION);

                //Verifica o nounce devolvido pelo servidor, salvaguardando ataques de replay
                if (nounce == receivedNounce) {
                    System.out.println("Client Handler: Nounce verificado com sucesso");
                } else {
                    System.out.println("Client Handler: Nounce inválido. Terminando a execução");
                    throw new RuntimeException();
                }

            } catch (KerbyException e) {
                e.printStackTrace();
            }

            // Cria o autenticador
            Date currDate = new Date();
            Auth auth = new Auth(VALID_CLIENT_NAME, currDate);
            smc.put("authDate", currDate);
            smc.setScope("authDate", MessageContext.Scope.APPLICATION);
            CipheredView cipheredAuth = new CipheredView();
            try {
                cipheredAuth = auth.cipher(sessionKey);
            } catch (KerbyException e) {
                e.printStackTrace();
            }

            // Converte o ticket para XMLBytes para que possa ser adicionado ao header
            CipherClerk ticketToHeader = new CipherClerk();
            byte[] strTicket = null;
            try {
                strTicket = ticketToHeader.cipherToXMLBytes(cipheredTicket, "ticket");
            } catch (JAXBException e) {
                e.printStackTrace();
            }
            String encodedTicket = printBase64Binary(strTicket);


            // Converte o auth para XMLBytes para que possa ser adicionado ao header
            CipherClerk authToHeader = new CipherClerk();
            byte[] strAuth = null;
            try {
                strAuth = authToHeader.cipherToXMLBytes(cipheredAuth, "auth");
            } catch (JAXBException e) {
                e.printStackTrace();
            }
            String encodedAuth = printBase64Binary(strAuth);


            /** Adiciona Ticket e auth ao cabeçalho da mensagem */

            // Adiciona o ticket ao header
            try {
                SOAPMessage msg = smc.getMessage();
                SOAPPart sp = msg.getSOAPPart();
                SOAPEnvelope se = sp.getEnvelope();
                SOAPHeader sh = se.getHeader();
                if (sh == null)
                    sh = se.addHeader();
                // Adiciona o ticket no header criado
                Name name = se.createName("ticket", "t", "http://ticket");
                SOAPHeaderElement element = sh.addHeaderElement(name);
                element.addTextNode(encodedTicket);
                System.out.printf("Client Handler: Ticket adicionado ao SOAP Header%n");
            } catch (SOAPException e) {
                System.out.printf("Client Handler: Failed to add SOAP header because of %s%n", e);
            }

            // Adiciona de seguida o auth ao header
            try {
                SOAPMessage msg = smc.getMessage();
                SOAPPart sp = msg.getSOAPPart();
                SOAPEnvelope se = sp.getEnvelope();
                SOAPHeader sh = se.getHeader();
                if (sh == null)
                    sh = se.addHeader();
                // Adiciona o auth no header criado
                Name name = se.createName("auth", "a", "http://auth");
                SOAPHeaderElement element = sh.addHeaderElement(name);
                element.addTextNode(encodedAuth);
                System.out.printf("Client Handler: Auth adicionado ao SOAP Header%n");
            } catch (SOAPException e) {
                System.out.printf("Client Handler: Failed to add SOAP header because of %s%n", e);
                return false;
            }

        } else {
            // inbound message
            try {

                // Verifica o Treq recebido no header
                SOAPMessage msg2 = smc.getMessage();
                SOAPPart sp2 = msg2.getSOAPPart();
                SOAPEnvelope se2 = sp2.getEnvelope();
                SOAPHeader sh2 = se2.getHeader();

                if (sh2 == null) {
                    sh2 = se2.addHeader();
                }

                // Obtem de seguida o treq
                CipheredView cipheredTreq = new CipheredView();
                Name nameTreq = se2.createName("treq", "r", "http://treq");
                Iterator<?> it2 = sh2.getChildElements(nameTreq);

                if (!it2.hasNext()) {
                    System.out.println("Client Handler: Header element not found.");
                    //return true;
                } else {
                    SOAPElement element2 = (SOAPElement) it2.next();
                    byte[] byteTreq = parseBase64Binary(element2.getValue());
                    CipherClerk tReqFromHeader = new CipherClerk();
                    cipheredTreq = tReqFromHeader.cipherFromXMLBytes(byteTreq);

                    // Código para decifrar um Treq
                    RequestTime decipheredTreq = null;
                        try {
                            decipheredTreq = new RequestTime(cipheredTreq,sessionKey);
                            Date reqDate = (Date) smc.get("authDate");
                            Date responseDate = decipheredTreq.getTimeRequest();


                            if (reqDate.equals(responseDate)){
                                System.out.println("Client Handler: Resposta do servidor valida, Treq validado");
                            } else {
                                System.out.println("Client Handler: Resposta inválida: Treq não corresponde ao enviado pelo cliente");
                                throw new RuntimeException();

                            }

                        } catch (KerbyException e) {

                            System.out.println("Client Handler Erro: não foi possível decifrar o tReq no client handler, devido a: %s%n " + e);
                            //e.printStackTrace();
                        }


                }
            } catch (SOAPException e) {
                e.printStackTrace();
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }


        return true;
    }


    /** The handleFault method is invoked for fault message processing. */
    @Override
    public boolean handleFault(SOAPMessageContext smc) {
        logSOAPMessage(smc, System.out);
        return true;
    }

    /**
     * Called at the conclusion of a message exchange pattern just prior to the
     * JAX-WS runtime dispatching a message, fault or exception.
     */
    @Override
    public void close(MessageContext messageContext) {
        // nothing to clean up
    }

    /**
     * Check the MESSAGE_OUTBOUND_PROPERTY in the context to see if this is an
     * outgoing or incoming message. Write the SOAP message to the print stream with
     * indentation for easier reading.
     */
    private void logSOAPMessage(SOAPMessageContext smc, PrintStream out) {
        Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        //Quando a mensagem do pedido vai a sair do cliente, é intercetada pelo KerberosClientHandler, que vai contactar o Saut, receber a resposta do Saut, e depois adicionar informação à mensagem do pedido que vai para o servidor.
        //
        //A mensagem com "acrescentos" segue para o servidor, pela rede.

        // print current time stamp
        Date now = new Date();
        out.print(dateFormat.format(now));
        // print SOAP message direction
        out.println(" " + (outbound ? "OUT" : "IN") + " KERBEROS CLIENT bound SOAP message:");

        // pretty-print SOAP message contents
        try {
            SOAPMessage message = smc.getMessage();
            Source src = message.getSOAPPart().getContent();

            // transform the (DOM) Source into a StreamResult
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(XML_INDENT_AMOUNT_PROPERTY, XML_INDENT_AMOUNT_VALUE.toString());
            StreamResult result = new StreamResult(out);
            transformer.transform(src, result);

        } catch (SOAPException se) {
            out.print("Ignoring SOAPException in handler: ");
            out.println(se);
        } catch (TransformerConfigurationException tce) {
            out.print("Unable to transform XML into text due to configuration: ");
            out.println(tce);
        } catch (TransformerException te) {
            out.print("Unable to transform XML into text: ");
            out.println(te);
        }
    }
}

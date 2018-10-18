package example.ws.handler;

import pt.ulisboa.tecnico.sdis.kerby.CipherClerk;
import pt.ulisboa.tecnico.sdis.kerby.CipheredView;
import pt.ulisboa.tecnico.sdis.kerby.SessionKey;

import javax.crypto.Mac;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.soap.*;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import static javax.xml.bind.DatatypeConverter.parseBase64Binary;
import static javax.xml.bind.DatatypeConverter.parseQName;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;

public class MACHandler implements SOAPHandler<SOAPMessageContext> {
    @Override
    public Set<QName> getHeaders() {
        return null;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext smc) {

        Boolean outbound = (Boolean) smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

        if (outbound) {

            Key sessionKey = (Key) smc.get("sessionKey");
            smc.put("sessionKey", sessionKey);
            smc.setScope("sessionKey", MessageContext.Scope.APPLICATION);

            try {
                Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
                mac.init(sessionKey);

                SOAPMessage msg = smc.getMessage();
                SOAPBody sbody = msg.getSOAPBody();
                byte[] digest = mac.doFinal(sbody.getTextContent().getBytes());

                SOAPPart sp = msg.getSOAPPart();
                SOAPEnvelope se = sp.getEnvelope();
                SOAPHeader sh = se.getHeader();

                // check header
                if (sh == null) {
                    System.out.println("Header not found.");
                    return true;
                }
                Name macName = se.createName("mac", "m", "http://mac");
                SOAPHeaderElement element = sh.addHeaderElement(macName);
                String encodedMac = printBase64Binary(digest);
                element.addTextNode(encodedMac);
                System.out.println("MAC Handler: Digest adicionado ao header da mensagem");

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            } catch (SOAPException e) {
                e.printStackTrace();
            }


        }
        //Código para mensagem inbound
        else {

            try {
                // get SOAP envelope header
                SOAPMessage msg = smc.getMessage();
                SOAPPart sp = msg.getSOAPPart();
                SOAPEnvelope se = sp.getEnvelope();
                SOAPHeader sh = se.getHeader();
                SOAPBody sbody = msg.getSOAPBody();

                // check header
                if (sh == null) {
                    sh = se.addHeader();
                }
                /** Obtém o MAC a partir do header */

                // Váriavel que guarda o ticket cifrado recebido pelo servidor
                CipheredView cipheredticket = new CipheredView();

                Name nameTicket = se.createName("mac", "m", "http://mac");
                Iterator<?> it = sh.getChildElements(nameTicket);
                if (!it.hasNext()) {
                    System.out.println("MAC Handler: Header element not found.");
                    return false;
                } else {
                    Key sessionKey = (Key) smc.get("sessionKey");
                    //System.out.println("INBOUND sessionKey " + sessionKey);
                    SOAPElement element = (SOAPElement) it.next();
                    byte[] receivedDigest = parseBase64Binary(element.getValue());

                    // Gera um novo digest para comparar com o recebido
                    Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
                    mac.init(sessionKey);
                    byte[] serverDigest = mac.doFinal(sbody.getTextContent().getBytes());

                    if (Arrays.equals(receivedDigest, serverDigest)) {
                        System.out.println("MAC Handler: Teste de integridade validado com sucesso. Resumos iguais");
                    } else {
                        System.out.println();
                        System.out.println("MAC Handler Erro: Teste de integridade falhou. Resumos não são iguais");
                        System.out.println();
                        throw new RuntimeException();
                    }
                }
            } catch (SOAPException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
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

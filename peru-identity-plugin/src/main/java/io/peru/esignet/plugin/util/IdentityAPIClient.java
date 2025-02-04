package io.peru.esignet.plugin.util;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.peru.esignet.plugin.dto.Envelope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

@Component
public class IdentityAPIClient {

    @Value("${identity.endpoint}")
    private String identityAPI;

    @Value("${identity.endpoint.password}")
    private String identityAPIPassword;

    @Value("${identity.endpoint.dni}")
    private String identityAPIDni;

    @Value("${identity.endpoint.ruc}")
    private String identityAPIRuc;

    private XmlMapper xmlMapper = new XmlMapper();

    private static final String REQUEST = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:end=\"http://endpoint.wsconsultadni.reniec.gob.pe/\">\n" +
            "<soapenv:Header/>\n" +
            "<soapenv:Body>\n" +
            "<end:consultar>\n" +
            "<arg0>\n" +
            "<nuDniConsulta>%s</nuDniConsulta>\n" +
            "<nuDniUsuario>%s</nuDniUsuario>\n" +
            "<nuRucUsuario>%s</nuRucUsuario>\n" +
            "<password>%s</password>\n" +
            "</arg0>\n" +
            "</end:consultar>\n" +
            "</soapenv:Body>\n" +
            "</soapenv:Envelope>";


    public Envelope getIdentity(String dni) throws Exception {
        URL url = new URL(identityAPI);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
        connection.setDoOutput(true);

        // Send SOAP request
        String soapRequest = String.format(REQUEST, dni, identityAPIDni, identityAPIRuc, identityAPIPassword);

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(soapRequest.getBytes());
            outputStream.flush();
        }
        // Read the response
        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return xmlMapper.readValue(connection.getInputStream(), Envelope.class);
        } else {
            throw new RuntimeException("HTTP error code: " + connection.getResponseCode());
        }
    }
}

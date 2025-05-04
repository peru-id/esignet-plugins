package io.peru.esignet.plugin.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.mosip.esignet.api.exception.KycAuthException;
import io.peru.esignet.plugin.dto.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

@Component
@Slf4j
public class IdentityAPIClient {

    @Value("${identity.endpoint}")
    private String identityAPI;

    @Value("${identity.endpoint.password}")
    private String identityAPIPassword;

    @Value("${identity.endpoint.dni}")
    private String identityAPIDni;

    @Value("${identity.endpoint.ruc}")
    private String identityAPIRuc;

    @Value("${identity.authcode.reissue.endpoint}")
    private String authCodeReissueEndpoint;

    @Value("${identity.oauth.token.endpoint}")
    private String peruOauthTokenEndpoint;

    @Value("${identity.client.id}")
    private String clientId;

    @Value("${identity.client.secret}")
    private String clientSecret;

    @Value("${identity.redirect.uri}")
    private String redirectUri;

    @Value("${identity.oidc.userinfo.endpoint}")
    private String peruOIDCUserInfoEndpoint;

    @Value("${identity.oidc.userinfo.dni.claim}")
    private String dniClaim;

    @Value("${identity.oauth.token.endpoint.payload}")
    private String tokenPayload;


    @Autowired
    private ObjectMapper jsonMapper;  // For JSON parsing

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

    public String exchangeAuthCodeForAccessToken(String authCode) {
        try {
            URL url = new URL(peruOauthTokenEndpoint); // Will set via @Value
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            String payload=String.format(tokenPayload,authCode,clientId,clientSecret,redirectUri);

//            String body = "grant_type=authorization_code&code=" + authCode + "&client_id=" + clientId + "&client_secret=" + clientSecret + "&redirect_uri=" + redirectUri;
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            int status = connection.getResponseCode();
            InputStream inputStream = (status >= 200 && status < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            Map<String, Object> response = jsonMapper.readValue(inputStream, Map.class);
            if (response.containsKey("access_token")) {
                return response.get("access_token").toString();
            } else {
                throw new RuntimeException("Missing access_token in response: " + response);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange auth code for access token", e);
        }
    }


    public String fetchUserInfo(String accessToken) throws KycAuthException {
        try {
            URL url = new URL(peruOIDCUserInfoEndpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.getHeaderFields().put("Authorization", Collections.singletonList("Bearer=" + accessToken));

            int status = connection.getResponseCode();
            InputStream inputStream = (status >= 200 && status < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            Map<String, Object> response = jsonMapper.readValue(inputStream, Map.class);
            if (response.containsKey(dniClaim)) {
                return response.get(dniClaim).toString();
            } else
            {
                log.error("Missing Doc in userinfo: {}" , response);
                throw new KycAuthException("auth_failed");
            }

        } catch (Exception e) {
            log.error("Failed to get userInfo:" , e);
            throw new KycAuthException("auth_failed");

        }
    }

}

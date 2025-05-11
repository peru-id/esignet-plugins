package io.peru.esignet.plugin.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.mosip.esignet.api.exception.KycAuthException;
import io.peru.esignet.plugin.dto.Envelope;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Pattern;

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

    public String exchangeAuthCodeForAccessToken(String authCode) throws KycAuthException {
        try {
            URL url = new URL(peruOauthTokenEndpoint); // Will set via @Value
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setDoOutput(true);
            String payload=String.format(tokenPayload, URLEncoder.encode(authCode, StandardCharsets.UTF_8),
                    URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                    URLEncoder.encode(clientSecret, StandardCharsets.UTF_8),
                    URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));

           try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            String responseBody = getResponse(connection);

            Map<String, Object> response = jsonMapper.readValue(responseBody, Map.class);
            if (response.containsKey("access_token")) {
                return response.get("access_token").toString();
            }
            log.error("Missing access_token in response: {} " , responseBody);

        } catch (Exception e) {
            log.error("Failed to exchange auth code for access token" , e);
        }
        throw new KycAuthException("auth_failed");
    }


    public String fetchUserInfo(String accessToken) throws KycAuthException {
        try {
            URL url = new URL(peruOIDCUserInfoEndpoint);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);

            String responseBody = getResponse(connection);

            String jwtPattern = "^[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]*$";
            if (Pattern.matches(jwtPattern, responseBody)) {
                log.error("Response body matches the JWT pattern");
                String[] parts = responseBody.split("\\.");
                responseBody = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            }

            Map<String, Object> response = jsonMapper.readValue(responseBody, Map.class);
            if (response.containsKey(dniClaim)) {
                return response.get(dniClaim).toString();
            }
            log.error("Missing Doc in userinfo: {}" , responseBody);

        } catch (Exception e) {
            log.error("Failed to get userInfo:" , e);
        }
        throw new KycAuthException("auth_failed");
    }

    private String getResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        int responseCode = connection.getResponseCode();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        } catch (IOException e) {
            log.error("Failed to get read response:" , e);
        } finally {
            connection.disconnect();
        }
        return response.toString();
    }

}
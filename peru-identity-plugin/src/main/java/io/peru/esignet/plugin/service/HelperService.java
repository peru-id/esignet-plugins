package io.peru.esignet.plugin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.spi.KeyBindingValidator;
import io.mosip.esignet.api.util.ErrorConstants;
import io.peru.esignet.plugin.dto.DatosPersona;
import io.peru.esignet.plugin.dto.Envelope;
import io.peru.esignet.plugin.util.IdentityAPIClient;
import io.mosip.kernel.signature.dto.JWTSignatureRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import io.peru.esignet.plugin.dto.KycAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
public class HelperService {

    public static final String ALGO_SHA3_256 = "SHA3-256";

    private final String FIELD_ID_KEY="id";

    private static final Base64.Encoder urlSafeEncoder = Base64.getUrlEncoder().withoutPadding();

    public static final String APPLICATION_ID = "OIDC_SERVICE";

    @Value("${mosip.esignet.peru.authenticator.otp-channels:email,phone}")
    private List<String> otpChannels;

    @Value("${mosip.esignet.peru.authenticator.otp-value:111111}")
    private String otpValue;

    @Value("#{${mosip.esignet.peru.authenticator.auth-factor.kba.field-details}}")
    private List<Map<String,String>> fieldDetailList;

    @Value("${mosip.esignet.peru.authenticator.auth-factor.kba.individual-id-field}")
    private String idField;

    @Autowired
    private SignatureService signatureService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private IdentityAPIClient identityAPIClient;

    @Autowired
    private KeyBindingValidator keyBindingValidator;

    @PostConstruct
    public void initialize() throws KycAuthException {
        log.info("Started to setup Peru Authenticator");
        boolean individualIdFieldIsValid = false;
        if(fieldDetailList==null || fieldDetailList.isEmpty()){
            log.error("Invalid configuration for field-details");
            throw new KycAuthException("Peru authenticator field is not configured properly");
        }
        for (Map<String, String> field : fieldDetailList) {
            if (field.containsKey(FIELD_ID_KEY) && field.get(FIELD_ID_KEY).equals(idField)) {
                individualIdFieldIsValid = true;
                break;
            }
        }
        if (!individualIdFieldIsValid) {
            log.error("Invalid configuration: The 'individual-id-field' '{}' is not available in 'field-details'.", idField);
            throw new KycAuthException("Invalid configuration: individual-id-field is not available in field-details.");
        }
    }

    public static String b64Encode(String value) {
        return urlSafeEncoder.encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    public boolean isSupportedOtpChannel(String channel) {
        return channel != null && otpChannels.contains(channel.toLowerCase());
    }

    protected static LocalDateTime getUTCDateTime() {
        return ZonedDateTime
                .now(ZoneOffset.UTC).toLocalDateTime();
    }

    public KycAuthResult validateOtpBasedAuth(String individualId, AuthChallenge authChallenge) throws KycAuthException {
        try {
            if (authChallenge.getAuthFactorType().equals("OTP") &&
                    authChallenge.getFormat().equals("alpha-numeric") &&
                    authChallenge.getChallenge().equals(otpValue)) {
                Envelope envelope = identityAPIClient.getIdentity(individualId);

                if(envelope!=null && envelope.getBody()!=null &&
                        envelope.getBody().getConsultarResponse()!=null &&
                        envelope.getBody().getConsultarResponse().getResponseReturn()!=null &&
                        envelope.getBody().getConsultarResponse().getResponseReturn().getDatosPersona()!=null) {

                    DatosPersona datosPersona=envelope.getBody().getConsultarResponse().getResponseReturn().getDatosPersona();

                    String kycToken = generateB64EncodedHash(ALGO_SHA3_256, UUID.randomUUID().toString());
                    KycAuthResult kycAuthResult = new KycAuthResult();
                    kycAuthResult.setKycToken(kycToken);
                    kycAuthResult.setPartnerSpecificUserToken(individualId);
                    cacheService.setKycAuth(kycToken, new KycAuth(kycToken, kycToken, LocalDateTime.now(ZoneOffset.UTC),
                            "transactionId", //For production based setup this should be set with valid transaction ID
                            individualId, datosPersona
                    ));
                    return kycAuthResult;
                }
            }
        }  catch (Exception e) {
            log.error("Failed to do the Authentication",e);
            throw new KycAuthException(ErrorConstants.AUTH_FAILED );
        }
        throw new KycAuthException(ErrorConstants.AUTH_FAILED);
    }

    public KycAuthResult validateKnowledgeBasedAuth(String individualId, AuthChallenge authChallenge) throws KycAuthException {

        KycAuthResult  kycAuthResult= new KycAuthResult();

        try {
            Envelope envelope = identityAPIClient.getIdentity(individualId);

            if(envelope!=null && envelope.getBody()!=null &&
                    envelope.getBody().getConsultarResponse()!=null &&
                    envelope.getBody().getConsultarResponse().getResponseReturn()!=null &&
                    envelope.getBody().getConsultarResponse().getResponseReturn().getDatosPersona()!=null) {

                DatosPersona datosPersona=envelope.getBody().getConsultarResponse().getResponseReturn().getDatosPersona();
                boolean authStatus=verifyKnowledgeBasedChallenge(authChallenge.getChallenge(),datosPersona);
                if(authStatus){
                    String kycToken = generateB64EncodedHash(ALGO_SHA3_256, UUID.randomUUID().toString());
                    kycAuthResult.setKycToken(kycToken);
                    kycAuthResult.setPartnerSpecificUserToken(individualId);
                    cacheService.setKycAuth(kycToken,new KycAuth(kycToken, individualId, LocalDateTime.now(ZoneOffset.UTC), "transactionId",
                            individualId
                            ,datosPersona
                    ));
                    return kycAuthResult;
                }
            }
        } catch (Exception e) {
            log.error("Failed to do the Authentication",e);
            throw new KycAuthException(ErrorConstants.AUTH_FAILED );
        }
        throw new KycAuthException(ErrorConstants.AUTH_FAILED );
    }

    public KycAuthResult validateWla(String individualId, AuthChallenge authChallenge) throws KycAuthException {
        KycAuthResult  kycAuthResult= new KycAuthResult();

        try {

            BindingAuthResult bindingAuthResult = keyBindingValidator.validateBindingAuth("transactionId",
                    individualId, List.of(authChallenge));
            if(bindingAuthResult == null)
                throw new KycAuthException(ErrorConstants.AUTH_FAILED );

            Envelope envelope = identityAPIClient.getIdentity(individualId);

            if(envelope!=null && envelope.getBody()!=null &&
                    envelope.getBody().getConsultarResponse()!=null &&
                    envelope.getBody().getConsultarResponse().getResponseReturn()!=null &&
                    envelope.getBody().getConsultarResponse().getResponseReturn().getDatosPersona()!=null) {
                DatosPersona datosPersona=envelope.getBody().getConsultarResponse().getResponseReturn().getDatosPersona();
                String kycToken = generateB64EncodedHash(ALGO_SHA3_256, UUID.randomUUID().toString());
                    kycAuthResult.setKycToken(kycToken);
                    kycAuthResult.setPartnerSpecificUserToken(individualId);
                    cacheService.setKycAuth(kycToken,new KycAuth(kycToken, individualId, LocalDateTime.now(ZoneOffset.UTC), "transactionId",
                            individualId
                            ,datosPersona
                    ));
                    return kycAuthResult;
            }
        } catch (Exception e) {
            log.error("Failed to do the Authentication ",e);
            throw new KycAuthException(ErrorConstants.AUTH_FAILED );
        }
        throw new KycAuthException(ErrorConstants.AUTH_FAILED );
    }

    private boolean verifyKnowledgeBasedChallenge(String encodedChallenge,DatosPersona datosPersona) throws KycAuthException {
        if(CollectionUtils.isEmpty(fieldDetailList)){
            log.error("KBA field details not configured");
            throw new KycAuthException(ErrorConstants.AUTH_FAILED);
        }
        try{
            byte[] decodedBytes = Base64.getUrlDecoder().decode(encodedChallenge);
            String challenge = new String(decodedBytes, StandardCharsets.UTF_8);
            Map<String, String> challengeMap = objectMapper.readValue(challenge, Map.class);

            for(Map<String,String> fieldDetail:fieldDetailList){
                if(challengeMap.containsKey(fieldDetail.get(FIELD_ID_KEY))) {
                    String challengeField = fieldDetail.get(FIELD_ID_KEY);
                    String challengeValue = challengeMap.get(challengeField);
                    String identityDataValue = getIdentityDataFieldValue(datosPersona, challengeField);

                    if(fieldDetail.get("type").equals("date")) {
                        LocalDate inputDate = LocalDate.parse(challengeValue);
                        LocalDate actualDate = LocalDate.parse(identityDataValue, DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                        if(!actualDate.isEqual(inputDate))
                            return false;
                    }
                    else if(!identityDataValue.equals(challengeValue)) {
                        return false;
                    }
                }
            }
        }catch (Exception e){
            log.error("Failed to decode KBA challenge or compare it with IdentityData", e);
            throw new KycAuthException(ErrorConstants.AUTH_FAILED);
        }
        return true;
    }

    private String getIdentityDataFieldValue(DatosPersona datosPersona,String challengeField) throws Exception {
        Field field = datosPersona.getClass().getDeclaredField(challengeField);
        field.setAccessible(true);
        Object fieldValue = field.get(datosPersona);
        return (String) fieldValue;
    }

    private String generateB64EncodedHash(String algorithm, String value) throws KycAuthException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return urlSafeEncoder.encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            log.error("Invalid algorithm : {}", algorithm, ex);
            throw new KycAuthException("invalid_algorithm");
        }
    }

    public Map<String, Object> buildKycDataBasedOnPolicy(List<String> claims,DatosPersona datosPersona) throws KycExchangeException {
        Map<String, Object> kyc = new HashMap<>();
        for (String claim : claims) {
            switch (claim) {
                case "name":
                    if (datosPersona.getPrenombres() != null) {
                        kyc.put("name", datosPersona.getPrenombres());
                    }
                    break;
                case "gender":
                    if(datosPersona.getGenero()!=null){
                       kyc.put("gender",datosPersona.getGenero());
                    }
                    break;
                case "given_name":
                    if(datosPersona.getPrimerApellido()!=null){
                        kyc.put("given_name",datosPersona.getPrimerApellido());
                    }
                    break;
                case "birthdate":
                    if(datosPersona.getFechaNacimiento()!=null){
                        kyc.put("birthdate",datosPersona.getFechaNacimiento());
                    }
                    break;
            }
        }
        return kyc;
    }

    public String signKyc(Map<String, Object> kyc) throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(kyc);
        JWTSignatureRequestDto jwtSignatureRequestDto = new JWTSignatureRequestDto();
        jwtSignatureRequestDto.setApplicationId(APPLICATION_ID);
        jwtSignatureRequestDto.setReferenceId("");
        jwtSignatureRequestDto.setIncludePayload(true);
        jwtSignatureRequestDto.setIncludeCertificate(false);
        jwtSignatureRequestDto.setDataToSign(b64Encode(payload));
        jwtSignatureRequestDto.setIncludeCertHash(false);
        JWTSignatureResponseDto responseDto = signatureService.jwtSign(jwtSignatureRequestDto);
        return responseDto.getJwtSignedData();
    }

}

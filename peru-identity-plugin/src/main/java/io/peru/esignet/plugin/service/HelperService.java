package io.peru.esignet.plugin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.util.ErrorConstants;
import io.peru.esignet.plugin.dto.KycExchangeRequestDto;
import io.peru.esignet.plugin.dto.DatosPersona;
import io.peru.esignet.plugin.dto.Envelope;
import io.peru.esignet.plugin.util.HttpClient;
import io.mosip.kernel.signature.dto.JWTSignatureRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import io.peru.esignet.plugin.dto.KycAuth;
import io.peru.esignet.plugin.dto.KycAuthRequestDto;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;

@Component
@Slf4j
public class HelperService {

    public static final String ALGO_SHA3_256 = "SHA3-256";

    private final String FIELD_ID_KEY="id";

    private static final Base64.Encoder urlSafeEncoder = Base64.getUrlEncoder().withoutPadding();

    public static final String APPLICATION_ID = "OIDC_SERVICE";

    @Value("${mosip.esignet.peru.authenticator.ida.otp-channels}")
    private List<String> otpChannels;

    @Value("${mosip.esignet.peru.authenticator.ida.otp-value:111111}")
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

    public KycAuthResult doKycAuth(String relyingPartyId, String clientId, KycAuthDto kycAuthDto)
            throws KycAuthException {
            KycAuthRequestDto kycAuthRequestDto = new KycAuthRequestDto();
            kycAuthRequestDto.setTransactionId(kycAuthDto.getTransactionId());
            kycAuthRequestDto.setIndividualId(kycAuthDto.getIndividualId());
            KycAuthResult kycAuthResult=null;
            for (AuthChallenge authChallenge : kycAuthDto.getChallengeList()) {
                switch (authChallenge.getAuthFactorType()) {
                    case "KBA":
                        kycAuthResult = validateKnowledgeBasedAuth(kycAuthDto.getIndividualId(), authChallenge);
                        break;
                    case "OTP":
                        kycAuthResult = validateOtpBasedAuth(kycAuthDto);
                        break;
                    case "WLA":
                        kycAuthResult = validateWla(kycAuthDto.getIndividualId(),authChallenge);
                        break;
                    default:
                        throw new KycAuthException("invalid_auth_challenge");
                }
            }
            return  kycAuthResult;
    }

    public KycExchangeResult kycExchange(String relyingPartyId, String clientId, KycExchangeRequestDto kycExchangeRequestDto) throws KycExchangeException {
        //TODO validate relying party Id and client Id
        KycAuth result = cacheService.getKycAuth(kycExchangeRequestDto.getKycToken());
        if(result==null || result.getDatosPersona()==null ){
            throw new KycExchangeException("peru-ida-006");
        }
        try {
            Map<String, Object> kyc =buildKycDataBasedOnPolicy(kycExchangeRequestDto.getAcceptedClaims(),result.getDatosPersona());
            kyc.put("sub", result.getPartnerSpecificUserToken());

            String finalKyc= signKyc(kyc);
            KycExchangeResult kycExchangeResult = new KycExchangeResult();
            kycExchangeResult.setEncryptedKyc(finalKyc);
            return kycExchangeResult;
        } catch (Exception ex) {
            log.error("Failed to build kyc data", ex);
            throw new KycExchangeException("mock-ida-008");
        }
    }

    protected static LocalDateTime getUTCDateTime() {
        return ZonedDateTime
                .now(ZoneOffset.UTC).toLocalDateTime();
    }

    private KycAuthResult validateOtpBasedAuth(KycAuthDto kycAuthDto) throws KycAuthException {

        KycAuthResult kycAuthResult = new KycAuthResult();
        AuthChallenge authChallenge = kycAuthDto.getChallengeList().get(0);
        if (authChallenge.getAuthFactorType().equals("OTP") && authChallenge.getFormat().equals("alpha-numeric")) {

            if (authChallenge.getChallenge().equals(otpValue)) {
                String kycToken = generateB64EncodedHash(ALGO_SHA3_256, UUID.randomUUID().toString());
                kycAuthResult.setKycToken(kycToken);
                kycAuthResult.setPartnerSpecificUserToken(kycAuthDto.getIndividualId());
                cacheService.setKycAuth(kycToken, new KycAuth(kycToken, kycToken, LocalDateTime.now(ZoneOffset.UTC), kycAuthDto.getTransactionId(),
                        kycAuthDto.getIndividualId()
                        , null
                ));
                return kycAuthResult;
            } else {
                throw new KycAuthException(ErrorConstants.AUTH_FAILED);
            }
        }
        return  kycAuthResult;
    }

    private KycAuthResult validateKnowledgeBasedAuth(String individualId, AuthChallenge authChallenge) throws KycAuthException {

        KycAuthResult  kycAuthResult= new KycAuthResult();

        try {
            String response= HttpClient.sendSOAPRequest(individualId);
            Envelope envelope=HttpClient.getResponse(response);
            if(envelope!=null && envelope.getBody()!=null && envelope.getBody().getConsultarResponse()!=null && envelope.getBody().getConsultarResponse().getResponseReturn()!=null
            && envelope.getBody().getConsultarResponse().getResponseReturn().getDatosPersona()!=null){
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
            log.error("Failed to do the Authentication: {}",e);
            throw new KycAuthException(ErrorConstants.AUTH_FAILED );
        }
        throw new KycAuthException(ErrorConstants.AUTH_FAILED );
    }

    private KycAuthResult validateWla(String individualId, AuthChallenge authChallenge) throws KycAuthException {
        KycAuthResult  kycAuthResult= new KycAuthResult();

        try {
            String response= HttpClient.sendSOAPRequest(individualId);
            Envelope envelope=HttpClient.getResponse(response);
            if(envelope!=null && envelope.getBody()!=null && envelope.getBody().getConsultarResponse()!=null && envelope.getBody().getConsultarResponse().getResponseReturn()!=null
                    && envelope.getBody().getConsultarResponse().getResponseReturn().getDatosPersona()!=null){
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
            log.error("Failed to do the Authentication: {}",e);
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
                    if(fieldDetail.get("type").equals("date")) {
                        challengeValue =convertToRequiredDateFormat(challengeValue);
                    }
                    String identityDataValue = getIdentityDataFieldValue(datosPersona, challengeField);
                    if(!identityDataValue.equals(challengeValue)) {
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

    private String convertToRequiredDateFormat(String dateStr) throws KycAuthException {
        String[] dateFormats = {"dd/MM/yyyy", "dd-MM-yyyy", "yyyy-MM-dd"};
        for (String format : dateFormats) {
            try {
                Date date = new SimpleDateFormat(format).parse(dateStr);
                return new SimpleDateFormat("dd/MM/yyyy").format(date);
            } catch (ParseException e) {
                continue;
            }
        }
        log.error("Failed to decode KBA challenge or compare it with IdentityData for date: {}", dateStr);
        throw new KycAuthException(ErrorConstants.AUTH_FAILED);
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

    private Map<String, Object> buildKycDataBasedOnPolicy(List<String> claims,DatosPersona datosPersona) throws KycExchangeException {
        Map<String, Object> kyc = new HashMap<>();
        for (String claim : claims) {
            switch (claim) {
                case "name":
                    if (datosPersona.getApellidoCasada() != null) {
                        kyc.put("name", datosPersona.getPrenombres());
                    }
                    break;
                case "gender":
                    if(datosPersona.getEstadoCivil()!=null){
                       kyc.put("gender",datosPersona.getGenero());
                    }
                    break;
                case "given_name":
                    if(datosPersona.getEstadoCivil()!=null){
                        kyc.put("given_name",datosPersona.getPrimerApellido());
                    }
                    break;
                case "birthdate":
                    if(datosPersona.getEstadoCivil()!=null){
                        kyc.put("birthdate",datosPersona.getFechaNacimiento());
                    }
                    break;
            }
        }
        return kyc;
    }

    private String signKyc(Map<String, Object> kyc) throws JsonProcessingException {
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

package io.peru.esignet.plugin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.spi.KeyBindingValidator;
import io.mosip.esignet.api.util.ErrorConstants;
import io.mosip.kernel.signature.dto.JWTSignatureRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import io.peru.esignet.plugin.dto.*;
import io.peru.esignet.plugin.util.IdentityAPIClient;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HelperServiceTest {
    @InjectMocks
    private HelperService helperService;

    @Mock
    private CacheService cacheService;

    @Mock
    private IdentityAPIClient identityAPIClient;

    @Mock
    private KeyBindingValidator keyBindingValidator;

    @Mock
    private SignatureService signatureService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    public void validateOtpBasedAuth_withValidDetails_thenPass() throws Exception {
        ReflectionTestUtils.setField(helperService, "otpValue", "111111");
        String individualId = "individualId";
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("OTP");
        authChallenge.setFormat("alpha-numeric");
        authChallenge.setChallenge("111111");

        DatosPersona datosPersona = new DatosPersona();
        datosPersona.setDni("dniValue");

        ResponseReturn responseReturn = new ResponseReturn();
        responseReturn.setDatosPersona(datosPersona);

        ConsultarResponse consultarResponse = new ConsultarResponse();
        consultarResponse.setResponseReturn(responseReturn);

        Body body = new Body();
        body.setConsultarResponse(consultarResponse);

        Envelope envelope = new Envelope();
        envelope.setBody(body);

        Mockito.when(identityAPIClient.getIdentity(Mockito.anyString())).thenReturn(envelope);
        Mockito.doNothing().when(cacheService).setKycAuth(Mockito.anyString(), Mockito.any());

        KycAuthResult result = helperService.validateOtpBasedAuth(individualId, authChallenge);

        assertNotNull(result);
        assertNotNull(result.getKycToken());
        assertEquals(individualId, result.getPartnerSpecificUserToken());
    }

    @Test
    public void validateOtpBasedAuth_withInvalidDetails_thenFail() throws Exception {
        String individualId = "123";
        AuthChallenge authChallenge = mock(AuthChallenge.class);
        when(authChallenge.getAuthFactorType()).thenReturn("OTP");
        when(authChallenge.getFormat()).thenReturn("alpha-numeric");

        KycAuthException thrown = assertThrows(KycAuthException.class, () -> {
            helperService.validateOtpBasedAuth(individualId, authChallenge);
        });

        assertEquals(ErrorConstants.AUTH_FAILED, thrown.getMessage());
    }

    @Test
    public void validateKnowledgeBasedAuth_withValidDetails_thenPass() throws Exception {
        List<Map<String, String>> mockFieldDetailList = new ArrayList<>();
        Map<String, String> fieldDetail = new HashMap<>();
        fieldDetail.put("id", "dni");
        fieldDetail.put("type", "text");
        mockFieldDetailList.add(fieldDetail);

        ReflectionTestUtils.setField(helperService, "fieldDetailList", mockFieldDetailList);
        ReflectionTestUtils.setField(helperService, "otpValue", "111111");
        String individualId = "individualId";
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("KBA");
        authChallenge.setChallenge("eyAiZG5pIjogIjQ4MTQ5NTE0IiwgInByZW5vbWJyZXMiOiAiRk9SVFVOQVRBIiwgInByaW1lckFwZWxsaWRvIjogIk1JUkFOREEiLCAic2VndW5kb0FwZWxsaWRvIjogIlBBVUNBUiIsICJmZWNoYU5hY2ltaWVudG8iOiAiMTk5My0wOS0wNSIgfQ");
        authChallenge.setFormat("base64url-encoded-json");

        DatosPersona datosPersona = new DatosPersona();
        datosPersona.setDni("48149514");

        ResponseReturn responseReturn = new ResponseReturn();
        responseReturn.setDatosPersona(datosPersona);

        ConsultarResponse consultarResponse = new ConsultarResponse();
        consultarResponse.setResponseReturn(responseReturn);

        Body body = new Body();
        body.setConsultarResponse(consultarResponse);

        Envelope envelope = new Envelope();
        envelope.setBody(body);

        Mockito.when(identityAPIClient.getIdentity(Mockito.anyString())).thenReturn(envelope);
        Mockito.doNothing().when(cacheService).setKycAuth(Mockito.anyString(), Mockito.any());
        Map<String, String> mockChallengeMap = new HashMap<>();
        mockChallengeMap.put("dni", "48149514");
        Mockito.when(objectMapper.readValue(Mockito.anyString(), Mockito.eq(Map.class)))
                .thenReturn(mockChallengeMap);

        KycAuthResult result = helperService.validateKnowledgeBasedAuth(individualId, authChallenge);

        assertNotNull(result);
        assertNotNull(result.getKycToken());
        assertEquals(individualId, result.getPartnerSpecificUserToken());
    }

    @Test
    public void validateKnowledgeBasedAuth_withInvalidDetails_thenFail() throws Exception {
        String individualId = "123";
        AuthChallenge authChallenge = mock(AuthChallenge.class);
        Envelope mockEnvelope = mock(Envelope.class);
        when(identityAPIClient.getIdentity(individualId)).thenReturn(mockEnvelope);

        KycAuthException thrown = assertThrows(KycAuthException.class, () -> {
            helperService.validateKnowledgeBasedAuth(individualId, authChallenge);
        });

        assertEquals(ErrorConstants.AUTH_FAILED, thrown.getMessage());
    }

    @Test
    public void validateWla_withValidDetails_thenPass() throws Exception {
        String individualId = "individualId";
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("WLA");
        authChallenge.setChallenge("challengeData");

        BindingAuthResult bindingAuthResult = new BindingAuthResult("transcationId","individualId");
        Mockito.when(keyBindingValidator.validateBindingAuth(Mockito.anyString(), Mockito.anyString(), Mockito.anyList()))
                .thenReturn(bindingAuthResult);

        DatosPersona datosPersona = new DatosPersona();
        datosPersona.setDni("dniValue");

        ResponseReturn responseReturn = new ResponseReturn();
        responseReturn.setDatosPersona(datosPersona);

        ConsultarResponse consultarResponse = new ConsultarResponse();
        consultarResponse.setResponseReturn(responseReturn);

        Body body = new Body();
        body.setConsultarResponse(consultarResponse);

        Envelope envelope = new Envelope();
        envelope.setBody(body);

        Mockito.when(identityAPIClient.getIdentity(Mockito.anyString())).thenReturn(envelope);
        Mockito.doNothing().when(cacheService).setKycAuth(Mockito.anyString(), Mockito.any());

        KycAuthResult result = helperService.validateWla(individualId, authChallenge);

        assertNotNull(result);
        assertNotNull(result.getKycToken());
        assertEquals(individualId, result.getPartnerSpecificUserToken());
    }

    @Test
    public void validateWla_withInvalidDetails_thenFail() {
        String individualId = "123";
        AuthChallenge authChallenge = mock(AuthChallenge.class);

        KycAuthException thrown = assertThrows(KycAuthException.class, () -> {
            helperService.validateWla(individualId, authChallenge);
        });

        assertEquals(ErrorConstants.AUTH_FAILED, thrown.getMessage());
    }

    @Test
    public void buildKycDataBasedOnPolicy_withValidDetails_thenPass() throws Exception {
        List<String> claims = List.of("name", "gender");
        DatosPersona datosPersona = new DatosPersona();
        datosPersona.setPrenombres("John");
        datosPersona.setGenero("Male");

        Map<String, Object> kycData = helperService.buildKycDataBasedOnPolicy(claims, datosPersona);

        assertNotNull(kycData);
        assertEquals(2, kycData.size());
        assertEquals("John", kycData.get("name"));
        assertEquals("Male", kycData.get("gender"));
    }

    @Test
    public void signKyc_withValidDetails_thenPass() throws Exception {
        String expectedPayload = "{\"name\":\"John\"}";
        Mockito.when(objectMapper.writeValueAsString(Mockito.any(Map.class))).thenReturn(expectedPayload);
        Map<String, Object> kycData = new HashMap<>();
        kycData.put("name", "John");

        String expectedJwt = "signedJwtToken";
        JWTSignatureResponseDto jwtSignatureResponseDto = new JWTSignatureResponseDto();
        jwtSignatureResponseDto.setJwtSignedData(expectedJwt);

        Mockito.when(signatureService.jwtSign(Mockito.any(JWTSignatureRequestDto.class)))
                .thenReturn(jwtSignatureResponseDto);

        String signedJwt = helperService.signKyc(kycData);

        assertNotNull(signedJwt);
        assertEquals(expectedJwt, signedJwt);
    }

    @Test
    public void testIsSupportedOtpChannel() {
        ReflectionTestUtils.setField(helperService, "otpChannels", List.of("email", "phone"));

        boolean isEmailSupported = helperService.isSupportedOtpChannel("email");
        boolean isPhoneSupported = helperService.isSupportedOtpChannel("phone");
        boolean isSmsSupported = helperService.isSupportedOtpChannel("sms");

        assertTrue(isEmailSupported);
        assertTrue(isPhoneSupported);
        assertFalse(isSmsSupported);
    }

    @Test
    public void initialize_InvalidFieldDetails_thenFail() throws Exception {
        ReflectionTestUtils.setField(helperService, "fieldDetailList", new ArrayList<>());

        KycAuthException thrown = assertThrows(KycAuthException.class, () -> {
            helperService.initialize();
        });

        assertEquals("Peru authenticator field is not configured properly", thrown.getMessage());
    }

    @Test
    public void initialize_withInvalidIndividualIdField_thenFail() throws Exception {
        ReflectionTestUtils.setField(helperService, "fieldDetailList", List.of(Map.of("id", "otherIdField")));
        ReflectionTestUtils.setField(helperService, "idField", "individualId");

        KycAuthException thrown = assertThrows(KycAuthException.class, () -> {
            helperService.initialize();
        });

        assertEquals("Invalid configuration: individual-id-field is not available in field-details.", thrown.getMessage());
    }

}

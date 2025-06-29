package io.peru.esignet.plugin.service;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.peru.esignet.plugin.dto.DatosPersona;
import io.peru.esignet.plugin.dto.KycAuth;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PeruAuthenticationServiceTest {

    @InjectMocks
    private PeruAuthenticationService peruAuthenticationService;

    @Mock
    private HelperService helperService;

    @Mock
    private CacheService cacheService;

    @Test
    public void doKycAuth_withAuthFactorAsKba_thenPass() throws KycAuthException {

        AuthChallenge authChallenge=new AuthChallenge();
        authChallenge.setAuthFactorType("KBA");
        authChallenge.setChallenge("eyAiZG5pIjogIjQ4MTQ5NTE0IiwgInByZW5vbWJyZXMiOiAiRk9SVFVOQVRBIiwgInByaW1lckFwZWxsaWRvIjogIk1JUkFOREEiLCAic2VndW5kb0FwZWxsaWRvIjogIlBBVUNBUiIsICJmZWNoYU5hY2ltaWVudG8iOiAiMTk5My0wOS0wNSIgfQ");
        authChallenge.setFormat("base64url-encoded-json");
        KycAuthDto kycAuthDto = new KycAuthDto();
        kycAuthDto.setTransactionId("transactionId");
        kycAuthDto.setIndividualId("individualId");

        List<AuthChallenge> challengeList = new ArrayList<>();
        challengeList.add(authChallenge);
        kycAuthDto.setChallengeList(challengeList);

        KycAuthResult expectedResult = new KycAuthResult();
        expectedResult.setKycToken("kycToken");
        Mockito.when(helperService.validateKnowledgeBasedAuth("individualId",authChallenge)).thenReturn(expectedResult);

        KycAuthResult result = peruAuthenticationService.doKycAuth("relyingPartyId", "clientId", kycAuthDto);

        assertEquals(expectedResult.getKycToken(), result.getKycToken());
    }

    @Test
    public void doKycAuth_withAuthFactorAsOtp_thenPass() throws KycAuthException {

        AuthChallenge authChallenge=new AuthChallenge();
        authChallenge.setAuthFactorType("OTP");
        authChallenge.setChallenge("111111");
        authChallenge.setFormat("alpha-numeric");
        KycAuthDto kycAuthDto = new KycAuthDto();
        kycAuthDto.setTransactionId("transactionId");
        kycAuthDto.setIndividualId("individualId");

        List<AuthChallenge> challengeList = new ArrayList<>();
        challengeList.add(authChallenge);
        kycAuthDto.setChallengeList(challengeList);

        KycAuthResult expectedResult = new KycAuthResult();
        expectedResult.setKycToken("kycToken");
        Mockito.when(helperService.validateOtpBasedAuth("individualId",authChallenge)).thenReturn(expectedResult);

        KycAuthResult result = peruAuthenticationService.doKycAuth("relyingPartyId", "clientId", kycAuthDto);

        assertEquals(expectedResult.getKycToken(), result.getKycToken());
    }

    @Test
    public void doKycAuth_withAuthFactorAsWla_thenPass() throws KycAuthException {

        AuthChallenge authChallenge=new AuthChallenge();
        authChallenge.setAuthFactorType("WLA");
        KycAuthDto kycAuthDto = new KycAuthDto();
        kycAuthDto.setTransactionId("transactionId");
        kycAuthDto.setIndividualId("individualId");

        List<AuthChallenge> challengeList = new ArrayList<>();
        challengeList.add(authChallenge);
        kycAuthDto.setChallengeList(challengeList);

        KycAuthResult expectedResult = new KycAuthResult();
        expectedResult.setKycToken("kycToken");
        Mockito.when(helperService.validateWla("individualId",authChallenge)).thenReturn(expectedResult);

        KycAuthResult result = peruAuthenticationService.doKycAuth("relyingPartyId", "clientId", kycAuthDto);

        assertEquals(expectedResult.getKycToken(), result.getKycToken());
    }

    @Test
    public void doKycAuth_withInvalidAuthFactor_thenFail() throws KycAuthException {

        AuthChallenge authChallenge=new AuthChallenge();
        authChallenge.setAuthFactorType("invalid");
        KycAuthDto kycAuthDto = new KycAuthDto();
        kycAuthDto.setTransactionId("transactionId");
        kycAuthDto.setIndividualId("individualId");
        List<AuthChallenge> challengeList = new ArrayList<>();
        challengeList.add(authChallenge);
        kycAuthDto.setChallengeList(challengeList);
        try {
            peruAuthenticationService.doKycAuth("relyingPartyId", "clientId", kycAuthDto);
            Assert.fail();
        }catch (KycAuthException e){
            Assert.assertEquals("invalid_auth_challenge",e.getErrorCode());
        }
    }

    @Test
    public void doKycExchange_withValidData_thenReturnKycExchangeResult() throws KycExchangeException, JsonProcessingException {
        KycAuth kycAuth = new KycAuth("kycToken", "partnerSpecificUserToken",
                LocalDateTime.now(), "transactionId",
                "individualId", new DatosPersona());

        KycExchangeDto kycExchangeDto = new KycExchangeDto();
        kycExchangeDto.setKycToken("kycToken");

        Mockito.when(cacheService.getKycAuth(Mockito.anyString())).thenReturn(kycAuth);
        Mockito.when(helperService.buildKycDataBasedOnPolicy(any(), any())).thenReturn(new HashMap<>());
        Mockito.when(helperService.signKyc(anyMap())).thenReturn("signedKyc");

        KycExchangeResult result = peruAuthenticationService.doKycExchange("relyingPartyId", "clientId", kycExchangeDto);

        assertNotNull(result);
        assertEquals("signedKyc", result.getEncryptedKyc());
    }

    @Test()
    public void doKycExchange_whenDatosPersonaIsNull_thenThrowException() throws KycExchangeException {
        KycExchangeDto kycExchangeDto=new KycExchangeDto();
        try {
            peruAuthenticationService.doKycExchange("relyingPartyId", "clientId", kycExchangeDto);
        } catch (KycExchangeException e)
        {
            Assert.assertEquals("peru-ida-006",e.getMessage());
        }
    }

    @Test
    public void doKycExchange_whenBuildKycDataFails_thenThrowException() throws KycExchangeException {
        KycAuth kycAuth = new KycAuth("kycToken", "partnerSpecificUserToken",
                LocalDateTime.now(), "transactionId",
                "individualId", new DatosPersona());

        KycExchangeDto kycExchangeDto = new KycExchangeDto();
        kycExchangeDto.setKycToken("kycToken");
        when(cacheService.getKycAuth(anyString())).thenReturn(kycAuth);
        when(helperService.buildKycDataBasedOnPolicy(any(), any())).thenThrow(new RuntimeException("Data build failure"));

        try {
            peruAuthenticationService.doKycExchange("relyingPartyId", "clientId", kycExchangeDto);
        } catch (KycExchangeException e)
        {
            Assert.assertEquals("mock-ida-008",e.getMessage());
        }
    }

    @Test
    public void sendOtp_withValidDetails_thenPass() throws SendOtpException {
        String relyingPartyId = "relyingPartyId";
        String clientId = "clientId";
        SendOtpDto sendOtpDto = new SendOtpDto();
        sendOtpDto.setTransactionId("transactionId");
        sendOtpDto.setIndividualId("individualId");
        sendOtpDto.setOtpChannels(Arrays.asList("email", "sms"));
        SendOtpResult result = peruAuthenticationService.sendOtp(relyingPartyId, clientId, sendOtpDto);
        assertNotNull(result);
    }

    @Test
    public void testIsSupportedOtpChannel_withValidDetails_thenPass() {
        String channel = "email";
        when(helperService.isSupportedOtpChannel(channel)).thenReturn(true);
        boolean result = peruAuthenticationService.isSupportedOtpChannel(channel);
        assertTrue(result);
        verify(helperService).isSupportedOtpChannel(channel);
    }

}

package io.peru.esignet.plugin.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.esignet.api.exception.SendOtpException;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import io.mosip.kernel.signature.service.SignatureService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

//@RunWith(MockitoJUnitRunner.class)
public class PeruAuthenticationServiceTest {

    /*@InjectMocks
    private PeruAuthenticationService peruAuthenticationService;

    @Mock
    private HelperService helperService;

    @Mock
    private KeymanagerService keymanagerService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CacheService cacheService;

    @Mock
    private SignatureService signatureService;

    @Test
    public void doKycAuth_withValidDetails_thenPass() throws KycAuthException {
        String relyingPartyId = "relyingPartyId";
        String clientId = "clientId";
        KycAuthDto kycAuthDto = new KycAuthDto();
        kycAuthDto.setTransactionId("transactionId");

        KycAuthResult expectedResult = new KycAuthResult();
        when(helperService.doKycAuth(relyingPartyId, clientId, kycAuthDto)).thenReturn(expectedResult);

        KycAuthResult result = peruAuthenticationService.doKycAuth(relyingPartyId, clientId, kycAuthDto);

        assertEquals(expectedResult, result);
        verify(helperService).doKycAuth(relyingPartyId, clientId, kycAuthDto);
    }

    @Test
    public void doKycExchange_withValidDetails_thenPass() throws KycExchangeException {
        KycExchangeDto kycExchangeDto = new KycExchangeDto();
        kycExchangeDto.setTransactionId("txn1");
        kycExchangeDto.setKycToken("mock-kyc-token");
        kycExchangeDto.setIndividualId("individual-id");
        kycExchangeDto.setAcceptedClaims(Arrays.asList("name", "gender"));
        kycExchangeDto.setClaimsLocales(new String[]{"en"});
        KycExchangeResult expectedResult = new KycExchangeResult();
        expectedResult.setEncryptedKyc("mock-signed-kyc");

        when(helperService.kycExchange(anyString(), anyString(), any(KycExchangeRequestDto.class)))
                .thenReturn(expectedResult);
        KycExchangeResult result = peruAuthenticationService.doKycExchange("party1", "client1", kycExchangeDto);
        assertEquals(expectedResult.getEncryptedKyc(), result.getEncryptedKyc());
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
    }*/

}

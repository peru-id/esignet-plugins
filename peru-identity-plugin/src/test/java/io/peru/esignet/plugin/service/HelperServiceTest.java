package io.peru.esignet.plugin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mosip.esignet.api.dto.*;
import io.mosip.esignet.api.exception.KycAuthException;
import io.mosip.esignet.api.exception.KycExchangeException;
import io.mosip.kernel.signature.service.SignatureService;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;


import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

//@RunWith(MockitoJUnitRunner.class)
public class HelperServiceTest {

    /*@InjectMocks
    private HelperService helperService;

    @Mock
    private SignatureService signatureService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CacheService cacheService;

    @Test
    public void doKycAuth_withInvalidChallenge_thenFail() {
        KycAuthDto kycAuthDto = mock(KycAuthDto.class);
        AuthChallenge authChallenge = new AuthChallenge();
        authChallenge.setAuthFactorType("INVALID");
        when(kycAuthDto.getChallengeList()).thenReturn(List.of(authChallenge));

        KycAuthException exception = assertThrows(KycAuthException.class, () -> {
            helperService.doKycAuth("relyingPartyId", "clientId", kycAuthDto);
        });

        assertEquals("invalid_auth_challenge", exception.getMessage());
    }

    @Test
    public void kycExchange_withInvalidKycToken_thenFail() {
        KycExchangeRequestDto kycExchangeRequestDto = new KycExchangeRequestDto();
        kycExchangeRequestDto.setKycToken("invalidKycToken");

        when(cacheService.getKycAuth("invalidKycToken")).thenReturn(null);

        KycExchangeException exception = assertThrows(KycExchangeException.class, () -> {
            helperService.kycExchange("party1", "client1", kycExchangeRequestDto);
        });

        assertEquals("peru-ida-006", exception.getErrorCode());
    }

    @Test
    public void b64Encode_withValidDetails_thenPass() {
        String encoded = helperService.b64Encode("sampleData");
        assertNotNull(encoded);
        assertEquals("c2FtcGxlRGF0YQ", encoded);  // Base64 encoding for "sampleData"
    }*/

}

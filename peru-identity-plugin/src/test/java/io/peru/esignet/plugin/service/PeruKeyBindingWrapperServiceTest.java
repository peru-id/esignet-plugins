package io.peru.esignet.plugin.service;

import io.mosip.esignet.api.dto.AuthChallenge;
import io.mosip.esignet.api.dto.SendOtpResult;
import io.mosip.esignet.api.exception.KeyBindingException;
import io.mosip.esignet.api.exception.SendOtpException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class PeruKeyBindingWrapperServiceTest {

    @InjectMocks
    private PeruKeyBindingWrapperService peruKeyBindingWrapperService;

    @Test
    public void sendBindingOtp_withValidDetails_thenPass() throws SendOtpException {
        List<String> otpChannels = Arrays.asList("email", "sms");
        Map<String, String> requestHeaders = new HashMap<>();
        SendOtpResult expectedResult = new SendOtpResult();
        expectedResult.setTransactionId("transactionId");
        SendOtpResult result = peruKeyBindingWrapperService.sendBindingOtp("individualId", otpChannels, requestHeaders);
        assertNotNull(result);
        assertEquals("transactionId", result.getTransactionId());
    }

    @Test
    public void doKeyBinding_withInvalidBindAuthFactorType_thenFail() {
        List<AuthChallenge> challengeList = new ArrayList<>();
        challengeList.add(new AuthChallenge());
        String bindAuthFactorType = "INVALID";
        Map<String, Object> publicKeyJWK = new HashMap<>();
        Map<String, String> requestHeaders = new HashMap<>();
        try {
            peruKeyBindingWrapperService.doKeyBinding("individualId", challengeList, publicKeyJWK, bindAuthFactorType, requestHeaders);
        } catch (KeyBindingException e) {
            Assert.assertEquals("invalid_bind_auth_factor_type",e.getErrorCode());
        }
    }

    @Test
    public void testGetSupportedChallengeFormats() {
        List<String> expectedFormats = Arrays.asList("jwt");
        List<String> result = peruKeyBindingWrapperService.getSupportedChallengeFormats("WLA");
        assertNotNull(result);
        assertEquals(expectedFormats, result);
    }

}

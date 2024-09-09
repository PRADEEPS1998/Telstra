package com.telstra.p2o.serviceintent.bff.mapper;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telstra.p2o.serviceintent.bff.constant.Constants;
import com.telstra.p2o.serviceintent.bff.dto.bapi.CIBAPIResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.CIResponseParameters;
import com.telstra.p2o.serviceintent.bff.dto.bapi.SIBAPIResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.SIResponseParameters;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.RepaymentOptions;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.SIBFFResponse;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

@RunWith(MockitoJUnitRunner.class)
public class SIBFFResponseMapperTest {

    @InjectMocks SIBFFResponseMapper sibffResponseMapper;

    private String correlationId;

    private CIBAPIResponse cibapiResponse;

    private SIBAPIResponse sibapiResponse;
    private RepaymentOptions repaymentOptions;

    @Before
    public void setup() {
        correlationId = "54e12a73-a23b-531c-cf25-1192c8f8adca";
        cibapiResponse =
                (CIBAPIResponse) getResponse("/ci_bapi_response.json", CIBAPIResponse.class);
        sibapiResponse =
                (SIBAPIResponse)
                        getResponse("/si_bapi_success_response.json", SIBAPIResponse.class);
        repaymentOptions = RepaymentOptions.builder().build();
    }

    @Test
    public void createSIBFFResponseWhenOutcomeIsRedirectToMYTTest() {
        SIBFFResponse sibffResponse =
                sibffResponseMapper.createSIBFFResponse(
                        cibapiResponse,
                        repaymentOptions,
                        CI_OUTCOME.REDIRECT_TO_MYT.toString(),
                        CIResponseParameters.builder().build(),
                        false);
        Assert.assertEquals("200", sibffResponse.getStatusCode());
        Assert.assertTrue(sibffResponse.getSuccess());
        Assert.assertEquals("REDIRECT_TO_MYT", sibffResponse.getAction());
    }

    @Test
    public void createSIBFFResponseWhenOutcome_REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO() {
        SIBFFResponse sibffResponse =
                sibffResponseMapper.createSIBFFResponse(
                        cibapiResponse,
                        repaymentOptions,
                        CI_OUTCOME.REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO.toString(),
                        CIResponseParameters.builder().build(),
                        false);
        Assert.assertEquals("200", sibffResponse.getStatusCode());
        Assert.assertTrue(sibffResponse.getSuccess());
        Assert.assertEquals(
                "REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO", sibffResponse.getAction());
    }

    @Test
    public void createSIBFFResponseWhenOutcomeIsRedirectToAgoraTest() {
        SIBFFResponse sibffResponse =
                sibffResponseMapper.createSIBFFResponse(
                        cibapiResponse,
                        repaymentOptions,
                        CI_OUTCOME.REDIRECT_TO_AGORA.toString(),
                        CIResponseParameters.builder().build(),
                        false);
        Assert.assertEquals("200", sibffResponse.getStatusCode());
        Assert.assertTrue(sibffResponse.getSuccess());
        Assert.assertEquals("REDIRECT_TO_AGORA", sibffResponse.getAction());
    }

    @Test
    public void createSIBFFResponseForSIOutcomeWhenOutcomeIsRedirectToAgoraTest() {
        SIBFFResponse sibffResponse =
                sibffResponseMapper.createSIBFFResponseForSIOutcome(
                        sibapiResponse,
                        CI_OUTCOME.REDIRECT_TO_AGORA.toString(),
                        SIResponseParameters.builder().build(),
                        CIResponseParameters.builder().build(),
                        false);
        Assert.assertEquals("200", sibffResponse.getStatusCode());
        Assert.assertTrue(sibffResponse.getSuccess());
        Assert.assertEquals("REDIRECT_TO_AGORA", sibffResponse.getAction());
    }

    @Test(expected = SIBFFException.class)
    public void createSIBFFResponseForSIOutcomeExceptionTest() {
        sibffResponseMapper.createSIBFFResponseForSIOutcome(
                sibapiResponse,
                SI_OUTCOME.CONTINUE.toString(),
                null,
                CIResponseParameters.builder().build(),
                false);
    }

    @Test
    public void validateETCDetailsSIOutcomeWhenOutcomeContinueTest() {
        SIBFFResponse sibffResponse =
                sibffResponseMapper.createSIBFFResponseForSIOutcome(
                        sibapiResponse,
                        Constants.SI_OUTCOME.CONTINUE.toString(),
                        SIResponseParameters.builder()
                                .hasETC(true)
                                .isCIMTransferRODevice(true)
                                .build(),
                        getCIResponseParameters(),
                        false);
        Assert.assertEquals("200", sibffResponse.getStatusCode());
        Assert.assertTrue(sibffResponse.getSuccess());
        Assert.assertEquals("CONTINUE", sibffResponse.getAction());
        Assert.assertNotNull(sibffResponse.getEtcDetails());
        Assert.assertNotNull(sibffResponse.getEtcDetails().getDeviceDetails());
    }

    @Test
    public void validateTonAvailableSIOutcomeWhenOutcomeContinueTest() {
        SIBFFResponse sibffResponse =
                sibffResponseMapper.createSIBFFResponseForSIOutcome(
                        sibapiResponse,
                        Constants.SI_OUTCOME.CONTINUE.toString(),
                        SIResponseParameters.builder().isTONAvailable(true).build(),
                        getCIResponseParameters(),
                        false);
        Assert.assertEquals("200", sibffResponse.getStatusCode());
        Assert.assertTrue(sibffResponse.getSuccess());
        Assert.assertEquals("CONTINUE", sibffResponse.getAction());
        Assert.assertEquals(true, sibffResponse.getIsTONAvailable());
    }

    @Test
    public void validateTonNotAvailableSIOutcomeWhenOutcomeContinueTest() {
        SIBFFResponse sibffResponse =
                sibffResponseMapper.createSIBFFResponseForSIOutcome(
                        sibapiResponse,
                        Constants.SI_OUTCOME.CONTINUE.toString(),
                        SIResponseParameters.builder().isTONAvailable(false).build(),
                        getCIResponseParameters(),
                        false);
        Assert.assertEquals("200", sibffResponse.getStatusCode());
        Assert.assertTrue(sibffResponse.getSuccess());
        Assert.assertEquals("CONTINUE", sibffResponse.getAction());
        Assert.assertEquals(false, sibffResponse.getIsTONAvailable());
    }

    @Test
    public void createSIBFFResponseForSIOutcomeWhenOutcomeContinueTest() {
        SIBFFResponse sibffResponse =
                sibffResponseMapper.createSIBFFResponseForSIOutcome(
                        sibapiResponse,
                        SI_OUTCOME.CONTINUE.toString(),
                        SIResponseParameters.builder()
                                .hasETC(true)
                                .isCIMTransferRODevice(true)
                                .build(),
                        getCIResponseParameters(),
                        false);
        Assert.assertEquals("200", sibffResponse.getStatusCode());
        Assert.assertTrue(sibffResponse.getSuccess());
        Assert.assertEquals("CONTINUE", sibffResponse.getAction());
        Assert.assertNotNull(sibffResponse.getExistingPlanDetails());
        Assert.assertNotNull(sibffResponse.getSelectedPlanDetails());
    }

    @Test
    public void createSIBFFResponseForSIOutcomeWhenOutcomeAgoraRedirectTest() {
        SIBFFResponse sibffResponse =
                sibffResponseMapper.createSIBFFResponseForSIOutcome(
                        sibapiResponse,
                        SI_OUTCOME.REDIRECT_TO_AGORA.toString(),
                        SIResponseParameters.builder()
                                .hasETC(true)
                                .isCIMTransferRODevice(true)
                                .reasonForFeasibilityError("NO_PENDING_ORDERS")
                                .build(),
                        getCIResponseParameters(),
                        false);
        Assert.assertEquals("200", sibffResponse.getStatusCode());
        Assert.assertTrue(sibffResponse.getSuccess());
        Assert.assertEquals("NO_PENDING_ORDERS", sibffResponse.getReasonForFeasibilityError());
    }

    @Test
    public void createSIBFFResponseWhenOutcomeIsRemovePlanAndBuyPhoneTest() {
        SIBFFResponse sibffResponse =
                sibffResponseMapper.createSIBFFResponse(
                        cibapiResponse,
                        repaymentOptions,
                        CI_OUTCOME.REMOVE_PLAN_AND_BUY_PHONE.toString(),
                        getCIResponseParameters(),
                        false);
        Assert.assertEquals("200", sibffResponse.getStatusCode());
        Assert.assertTrue(sibffResponse.getSuccess());
        Assert.assertEquals("REMOVE_PLAN_AND_BUY_PHONE", sibffResponse.getAction());
        Assert.assertNotNull(sibffResponse.getExistingPlanDetails());
        Assert.assertNotNull(sibffResponse.getSelectedPlanDetails());
    }

    @Test(expected = SIBFFException.class)
    public void createSIBFFResponseExceptionTest() {
        sibffResponseMapper.createSIBFFResponse(
                cibapiResponse,
                repaymentOptions,
                CI_OUTCOME.REMOVE_PLAN_AND_BUY_PHONE.toString(),
                null,
                false);
    }

    private Object getResponse(String jsonPath, Class c) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Resource resource = new ClassPathResource(jsonPath);
            return objectMapper.readValue(resource.getFile(), c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CIResponseParameters getCIResponseParameters() {
        return CIResponseParameters.builder()
                .sourceSystem("B2CFORCE")
                .existingPlanName("L Data pool")
                .selectedPlanName("Xyz")
                .paymentMode("Subscription")
                .isROLinked(true)
                .deviceName("Galaxy Note 9")
                .isDavinci(true)
                .isMICA(true)
                .isPrepaidService(true)
                .isPlanInCart(true)
                .isDeviceInCart(true)
                .isDeviceROInCart(true)
                .build();
    }
}

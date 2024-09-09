package com.telstra.p2o.serviceintent.bff.helper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telstra.p2o.common.caching.data.OrderingStateInfo;
import com.telstra.p2o.serviceintent.bff.config.SIBFFConfig;
import com.telstra.p2o.serviceintent.bff.constant.Constants;
import com.telstra.p2o.serviceintent.bff.dto.bapi.CIBAPIResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.CIResponseParameters;
import com.telstra.p2o.serviceintent.bff.dto.bapi.Product;
import com.telstra.p2o.serviceintent.bff.dto.bapi.ProductGroup;
import com.telstra.p2o.serviceintent.bff.dto.bapi.ProductOffering;
import com.telstra.p2o.serviceintent.bff.dto.bapi.SIBAPIResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.SIResponseParameters;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.HeadersData;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import com.telstra.p2o.serviceintent.bff.util.SIBFFCacheUtil;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SIBFFHelperTest {

    @InjectMocks SIBFFHelper sibffHelper;

    @Mock SIBFFCacheUtil sibffCacheUtil;

    @Mock SIBFFConfig sibffConfig;

    HeadersData headersData;

    @Test
    public void getCIResponseParametersReturnsCorrectParametersForB2CForce() {
        CIBAPIResponse ciBapiResponse = createFakeCiBapiResponse();
        OrderingStateInfo orderingStateInfo = createFakeOrderingStateInfo();
        OrderingStateInfo newOrderingStateInfo = createFakeOrderingStateInfo();
        OrderingStateInfo sampleCacheData = new OrderingStateInfo();

        sampleCacheData.setAccountUUID("some-account-uuid");
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        Mockito.when(sibffConfig.getValidFlow())
                .thenReturn(Collections.singletonList("self-serve"));
        CIResponseParameters ciResponseParameters =
                sibffHelper.getCIResponseParameters(
                        ciBapiResponse, orderingStateInfo, newOrderingStateInfo, headersData);
        assertEquals("B2CFORCE", ciResponseParameters.getSourceSystem());
        assertEquals("L Data pool", ciResponseParameters.getExistingPlanName());
        assertEquals("Xyz", ciResponseParameters.getSelectedPlanName());

        assertTrue(ciResponseParameters.isPlanInCart());
        assertFalse(ciResponseParameters.isDeviceInCart());
        assertFalse(ciResponseParameters.isDeviceROInCart());
        assert (sampleCacheData.getAccountUUID().equals("some-account-uuid"));
    }

    @Test
    public void getCIResponseParamatersReturnsCorrectParametersForMobileFamily() {
        CIBAPIResponse ciBapiResponse = createFakeCiBapiResponse_device_scenario();
        OrderingStateInfo orderingStateInfo = createFakeOrderingStateInfo_device_scenario();
        OrderingStateInfo newOrderingStateInfo = createFakeOrderingStateInfo_device_scenario();
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        CIResponseParameters ciResponseParameters =
                sibffHelper.getCIResponseParameters(
                        ciBapiResponse, orderingStateInfo, newOrderingStateInfo, headersData);
        assertFalse(ciResponseParameters.isDeviceInCart());
        assertFalse(ciResponseParameters.isDeviceROInCart());
        var ciOutcome = sibffHelper.getCIOutcome(ciResponseParameters, orderingStateInfo);
        assertEquals("REDIRECT_TO_MYT", ciOutcome);
    }

    @Test(expected = SIBFFException.class)
    public void getCIResponseParametersReturnsForNullCheck() {
        CIBAPIResponse ciBapiResponse = createFakeCiBapiResponse();
        OrderingStateInfo orderingStateInfo = createFakeOrderingStateInfo();
        OrderingStateInfo newOrderingStateInfo = createFakeOrderingStateInfo();
        List<ProductGroup> productGroupList = new ArrayList<ProductGroup>();
        //  productGroupList.add(null);

        ciBapiResponse.setProductGroup(productGroupList);
        CIResponseParameters ciResponseParameters =
                sibffHelper.getCIResponseParameters(
                        ciBapiResponse, orderingStateInfo, newOrderingStateInfo, headersData);
    }

    @Test
    public void getSIResponseParametersReturnsCorrectParameters() {
        SIBAPIResponse sibapiResponse = createFakeSIBapiResponse();

        SIResponseParameters siResponseParameters =
                sibffHelper.getSIResponseParameters(sibapiResponse);

        assertTrue(siResponseParameters.isFeasible());
        assertTrue(siResponseParameters.isEligibile());
        assertTrue(siResponseParameters.isETCSuccess());
        assertTrue(siResponseParameters.isAgoraLinkFound());
        assertTrue(siResponseParameters.isFeasibleEligibleAndETCSuccess());
    }

    public void assertFalse(boolean condition) {
        assertEquals(false, condition);
    }

    @Test
    public void getSIOutcomeReturnsCorrectOutcome() {
        SIBAPIResponse sibapiResponse = createFakeSIBapiResponse();

        SIResponseParameters siResponseParameters =
                sibffHelper.getSIResponseParameters(sibapiResponse);

        assertFalse(
                Constants.SI_OUTCOME
                        .REDIRECT_TO_AGORA
                        .toString()
                        .equalsIgnoreCase(sibffHelper.getSIOutcome(siResponseParameters)));
        siResponseParameters.setAgoraLinkFound(false);
        assertTrue(
                Constants.SI_OUTCOME
                        .CONTINUE
                        .toString()
                        .equalsIgnoreCase(sibffHelper.getSIOutcome(siResponseParameters)));
        siResponseParameters.setFeasibleEligibleAndETCSuccess(false);
        assertTrue(
                Constants.SI_OUTCOME
                        .HARDSTOP
                        .toString()
                        .equalsIgnoreCase(sibffHelper.getSIOutcome(siResponseParameters)));
        siResponseParameters.setAgoraLinkFound(true);
        assertTrue(
                Constants.SI_OUTCOME
                        .REDIRECT_TO_AGORA
                        .toString()
                        .equalsIgnoreCase(sibffHelper.getSIOutcome(siResponseParameters)));
    }

    @Test
    public void getSIOutcomeReturnsCorrectOutcomeNoFeasible() {
        SIBAPIResponse sibapiResponse = createFakeSIBapiResponseNoFeasible();

        SIResponseParameters siResponseParameters =
                sibffHelper.getSIResponseParameters(sibapiResponse);

        assertEquals("SERVICE_INACTIVE", siResponseParameters.getReasonForFeasibilityError());

        assertTrue(
                Constants.SI_OUTCOME
                        .REDIRECT_TO_AGORA
                        .toString()
                        .equalsIgnoreCase(sibffHelper.getSIOutcome(siResponseParameters)));
    }

    @Test
    public void getCIOutcomeForServiceMigrationTest() {
        var ciResponseParameters =
                CIResponseParameters.builder()
                        .isDavinci(false)
                        .isPrepaidService(false)
                        .isPlanInCart(true)
                        .build();
        var outcome = sibffHelper.getCIOutcome(ciResponseParameters, new OrderingStateInfo());
        Assert.assertEquals("SERVICE_MIGRATION", outcome);
    }

    @Test
    public void getCIOutcomeForRedirectToMYTTest() {
        var ciResponseParameters =
                CIResponseParameters.builder()
                        .isDavinci(true)
                        .isPrepaidService(false)
                        .isPlanInCart(true)
                        .isDeviceInCart(false)
                        .isDeviceROInCart(false)
                        .build();
        var outcome = sibffHelper.getCIOutcome(ciResponseParameters, new OrderingStateInfo());
        Assert.assertEquals("REDIRECT_TO_MYT", outcome);
    }

    @Test
    public void getCIOutcomeForRemovePlanAndBuyPhoneTest() {
        var ciResponseParameters =
                CIResponseParameters.builder()
                        .isDavinci(true)
                        .isPrepaidService(false)
                        .isROLinked(false)
                        .isPlanInCart(true)
                        .isDeviceInCart(true)
                        .isDeviceROInCart(true)
                        .build();
        var outcome = sibffHelper.getCIOutcome(ciResponseParameters, new OrderingStateInfo());
        Assert.assertEquals("REMOVE_PLAN_AND_BUY_PHONE", outcome);
    }

    @Test
    public void getCIOutcomeForRedirectToAgoraTest() {
        var ciResponseParameters =
                CIResponseParameters.builder().isDavinci(false).isPrepaidService(true).build();
        var outcome = sibffHelper.getCIOutcome(ciResponseParameters, new OrderingStateInfo());
        Assert.assertEquals("REDIRECT_TO_AGORA", outcome);
    }

    @Test
    public void getCIOutcomeTest() {
        var ciResponseParameters =
                CIResponseParameters.builder()
                        .isDavinci(true)
                        .isPrepaidService(false)
                        .isPlanInCart(true)
                        .isDeviceInCart(true)
                        .isDeviceROInCart(false)
                        .build();
        var outcome = sibffHelper.getCIOutcome(ciResponseParameters, new OrderingStateInfo());
        Assert.assertEquals("REMOVE_PLAN_AND_BUY_PHONE", outcome);

        ciResponseParameters =
                CIResponseParameters.builder()
                        .isDavinci(true)
                        .isPrepaidService(false)
                        .isROLinked(true)
                        .isPlanInCart(true)
                        .isDeviceInCart(true)
                        .isDeviceROInCart(true)
                        .build();
        OrderingStateInfo cacheData = new OrderingStateInfo();
        Map<String, Boolean> toggle = new HashMap<>();
        toggle.put("addRoToExistingService", true);
        cacheData.setFeatureToggles(toggle);
        outcome = sibffHelper.getCIOutcome(ciResponseParameters, cacheData);
        Assert.assertEquals("REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO", outcome);

        ciResponseParameters =
                CIResponseParameters.builder()
                        .isDavinci(true)
                        .isPrepaidService(false)
                        .isROLinked(true)
                        .isPlanInCart(true)
                        .isDeviceInCart(true)
                        .isDeviceROInCart(false)
                        .build();
        outcome = sibffHelper.getCIOutcome(ciResponseParameters, new OrderingStateInfo());
        Assert.assertEquals("REMOVE_PLAN_AND_BUY_PHONE", outcome);

        ciResponseParameters =
                CIResponseParameters.builder()
                        .isDavinci(true)
                        .isPrepaidService(false)
                        .isROLinked(true)
                        .isPlanInCart(false)
                        .isDeviceInCart(true)
                        .isDeviceROInCart(true)
                        .build();
        OrderingStateInfo orderingStateCache = new OrderingStateInfo();
        orderingStateCache.setFlowAction("add-ro-mobile");
        orderingStateCache.setFeatureToggles(toggle);
        outcome = sibffHelper.getCIOutcome(ciResponseParameters, orderingStateCache);
        Assert.assertEquals("BUY_PHONE_AND_DISCONNECT_RO", outcome);
    }

    @Test
    public void getDUPProductTest() {
        ProductGroup productGroup = ProductGroup.builder().build();
        Product product =
                Product.builder()
                        .status("ACTIVE")
                        .productOffering(
                                ProductOffering.builder()
                                        .type("DISCOUNT")
                                        .subtype("HANDSET")
                                        .id("XC001016739")
                                        .build())
                        .build();
        Product result = sibffHelper.getDUPProduct(productGroup, product);
        Assert.assertNotNull(result);
    }

    @Test
    public void getCIOutcomeForHardStopTest() {
        var ciResponseParameters = CIResponseParameters.builder().build();
        var outcome = sibffHelper.getCIOutcome(ciResponseParameters, new OrderingStateInfo());
        Assert.assertEquals("HARDSTOP", outcome);
    }

    @Test
    public void getCacheDataTest() {
        OrderingStateInfo sampleCacheData = new OrderingStateInfo();
        sampleCacheData.setAccountUUID("some-account-uuid");
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        lenient()
                .when(sibffCacheUtil.getCacheKey(any(), any(), any()))
                .thenReturn("some-cache-key");
        lenient().when(sibffCacheUtil.getCacheData(any())).thenReturn(sampleCacheData);
        lenient().doNothing().when(sibffCacheUtil).validateCacheData(any(), any());
        var cacheData = sibffHelper.getCacheData(headersData);
        assert (cacheData.getAccountUUID().equals("some-account-uuid"));
    }

    private SIBAPIResponse createFakeSIBapiResponse() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("si_bapi_success_response.json");
        try {
            return new ObjectMapper().readValue(inputStream, SIBAPIResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SIBAPIResponse createFakeSIBapiResponseNoFeasible() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream =
                classLoader.getResourceAsStream("si_bapi_not_feasible_response.json");
        try {
            return new ObjectMapper().readValue(inputStream, SIBAPIResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SIBAPIResponse createFakeSIErrorBapiResponse() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream =
                classLoader.getResourceAsStream("si_bapi_partial_error_response.json");
        try {
            return new ObjectMapper().readValue(inputStream, SIBAPIResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SIBAPIResponse createFakeSIErrorEtcOutcomeBapiResponse() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream =
                classLoader.getResourceAsStream("si_bapi_partial_error_response_etc_outcome.json");
        try {
            return new ObjectMapper().readValue(inputStream, SIBAPIResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SIBAPIResponse createFakeSIErrorFeasibilityOutcomeBapiResponse() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream =
                classLoader.getResourceAsStream(
                        "si_bapi_partial_error_response_feasibility_outcome.json");
        try {
            return new ObjectMapper().readValue(inputStream, SIBAPIResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CIBAPIResponse createFakeCiBapiResponse() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("ci_bapi_response.json");
        try {
            return new ObjectMapper().readValue(inputStream, CIBAPIResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OrderingStateInfo createFakeOrderingStateInfo() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("orderingStateInfo.json");
        try {
            return new ObjectMapper().readValue(inputStream, OrderingStateInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private CIBAPIResponse createFakeCiBapiResponse_device_scenario() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream =
                classLoader.getResourceAsStream("ci_bapi_response_device_scenario.json");
        try {
            return new ObjectMapper().readValue(inputStream, CIBAPIResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private OrderingStateInfo createFakeOrderingStateInfo_device_scenario() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream inputStream =
                classLoader.getResourceAsStream("orderingStateInfo_device_scenario.json");
        try {
            return new ObjectMapper().readValue(inputStream, OrderingStateInfo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

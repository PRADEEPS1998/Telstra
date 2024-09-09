package com.telstra.p2o.serviceintent.bff.service;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.*;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telstra.p2o.common.caching.data.CartActionCodes;
import com.telstra.p2o.common.caching.data.OrderingStateInfo;
import com.telstra.p2o.common.caching.data.ProductOrder;
import com.telstra.p2o.common.caching.data.ProductOrderItems;
import com.telstra.p2o.serviceintent.bff.config.SIBFFConfig;
import com.telstra.p2o.serviceintent.bff.constant.Constants;
import com.telstra.p2o.serviceintent.bff.dto.bapi.*;
import com.telstra.p2o.serviceintent.bff.dto.bapi.DeviceAssessmentResponse;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.*;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.*;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import com.telstra.p2o.serviceintent.bff.helper.SIBFFHelper;
import com.telstra.p2o.serviceintent.bff.mapper.SIBFFResponseMapper;
import com.telstra.p2o.serviceintent.bff.util.SIBFFCacheUtil;
import com.telstra.p2o.serviceintent.bff.validator.SIBFFValidator;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class SIBFFServiceImplTest {

    @InjectMocks SIBFFServiceImpl sibffService;

    @Mock private SIBFFValidator validator;

    @Mock private SIBFFCacheUtil cacheUtil;

    @Mock private SIBFFConfig config;

    @Mock private SIBFFResponseMapper sibffResponseMapper;

    @Mock private SIBFFHelper sibffHelper;
    @Mock RestTemplate restTemplate;

    private String correlationId;

    private CIBAPIResponse cibapiResponse;

    private SIBAPIResponse sibapiResponse;

    private SIBFFResponse sibffResponse;
    private ETCBapiResponse etcBapiResponse;

    public DeviceAssessmentResponse deviceAssessmentResponse;

    private ServiceIntentGetEligibilityResponse serviceIntentGetEligibiliy_successResponse;
    private ServiceIntentGetEligibilityResponse serviceIntentGetEligibiliy_AESFailure;
    private ServiceIntentGetEligibilityResponse serviceIntentGetEligibiliy_InternalServerError;
    private GetSIPayload payload;
    private HeadersData headersData;
    String customerInformation =
            "https://p2o-service-intent-sqi.apps.np.sdppcf.com/v1/customer-order-entry/customer-information/customer/products";
    String smEligibilityDetails =
            "https://p2o-service-intent-sqi.apps.np.sdppcf.com/v1/customer-order-entry/customer-information/service /getSMEligibilityDetails";
    String serviceIntentGetEligibilityUrl =
            "https://surge-edgarp2oms-edgarp2o-edgar.sv.telstra-cave.com/v1/customer-order-entry/customer-information/service/get-eligibility";

    String getETCUrl = "https://getEtc.com";
    String assetReferenceId;
    @Mock ResponseEntity<CIBAPIResponse> responseEntity;
    @Mock ResponseEntity<SIBAPIResponse> responseEntityForSI;
    @Mock ResponseEntity<ETCBapiResponse> responseEntityForSIgetETC;
    @Mock ResponseEntity<DeviceAssessmentResponse> assessDeviceBAPIResponse;

    @Mock
    ResponseEntity<ServiceIntentGetEligibilityResponse>
            responseEntityForServiceIntentGetEligibility;

    @Mock ResponseEntity<DeviceAssessResponse> responseEntityForDeviceAssessResponse;
    ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        correlationId = "bbb44944-0f2b-11ee-be56-0242ac120002";
        payload =
                GetSIPayload.builder()
                        .accountUUID("2b2435aa-4918-55db-48a7-92daa3f7f8c7")
                        .serviceId("61475081880")
                        .build();
        Map<String, Object> claims = new HashMap<>();
        claims.put("b2c_uid", "testuid");
        claims.put("staff_userId", "testuid");
        headersData =
                HeadersData.builder()
                        .microToken("testMicrotoken")
                        .channel("self-serve")
                        .correlationId(correlationId)
                        .claims(claims)
                        .build();
        cibapiResponse =
                (CIBAPIResponse) getResponse("/ci_bapi_response.json", CIBAPIResponse.class);
        sibapiResponse =
                (SIBAPIResponse)
                        getResponse("/si_bapi_success_response.json", SIBAPIResponse.class);
        etcBapiResponse =
                (ETCBapiResponse)
                        getResponse("/getETC_bapi_success_response.json", ETCBapiResponse.class);
        sibffResponse = (SIBFFResponse) getResponse("/si_bff_response.json", SIBFFResponse.class);
        serviceIntentGetEligibiliy_successResponse =
                (ServiceIntentGetEligibilityResponse)
                        getResponse(
                                "/serviceIntentGetEligbility_success.json",
                                ServiceIntentGetEligibilityResponse.class);
        serviceIntentGetEligibiliy_AESFailure =
                (ServiceIntentGetEligibilityResponse)
                        getResponse(
                                "/serviceIntentGetEligibility_AESFailure.json",
                                ServiceIntentGetEligibilityResponse.class);
        serviceIntentGetEligibiliy_InternalServerError =
                (ServiceIntentGetEligibilityResponse)
                        getResponse(
                                "/serviceIntentGetEligibility_InternalServerError.json",
                                ServiceIntentGetEligibilityResponse.class);

        when(config.getCustomerInformationURL()).thenReturn(customerInformation);
        when(config.getSmEligibilityDetailsURL()).thenReturn(smEligibilityDetails);
        when(config.getServiceIntentGetEligibilityURL()).thenReturn(serviceIntentGetEligibilityUrl);

        OrderingStateInfo cacheData =
                (OrderingStateInfo) getResponse("/orderingStateInfo.json", OrderingStateInfo.class);
        Mockito.lenient().when(cacheUtil.getCacheData(anyString())).thenReturn(cacheData);
        Mockito.lenient()
                .when(cacheUtil.getCacheKey(anyString(), anyMap(), anyString()))
                .thenReturn(correlationId);

        lenient()
                .when(restTemplate.exchange(anyString(), any(), any(), eq(CIBAPIResponse.class)))
                .thenReturn(responseEntity);
        lenient().when(responseEntity.getBody()).thenReturn(cibapiResponse);
        lenient()
                .when(restTemplate.exchange(anyString(), any(), any(), eq(ETCBapiResponse.class)))
                .thenReturn(responseEntityForSIgetETC);
        lenient().when(responseEntityForSIgetETC.getBody()).thenReturn(etcBapiResponse);
        assetReferenceId = getAssetReferenceId(cibapiResponse);
        lenient()
                .when(restTemplate.exchange(anyString(), any(), any(), eq(SIBAPIResponse.class)))
                .thenReturn(responseEntityForSI);
        lenient().when(responseEntityForSI.getBody()).thenReturn(sibapiResponse);
        lenient()
                .when(
                        sibffHelper.getCIResponseParameters(
                                any(CIBAPIResponse.class),
                                any(OrderingStateInfo.class),
                                any(OrderingStateInfo.class),
                                any(HeadersData.class)))
                .thenReturn(CIResponseParameters.builder().build());
        lenient()
                .when(sibffHelper.getSIResponseParameters(any(SIBAPIResponse.class)))
                .thenReturn(SIResponseParameters.builder().build());
    }

    @Test
    public void getMobileServiceIntentDetailsUpdateNewOrderStateInfoTest() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.REDIRECT_TO_MYT.toString());

        when(sibffResponseMapper.createSIBFFResponse(
                        any(CIBAPIResponse.class),
                        any(RepaymentOptions.class),
                        anyString(),
                        any(CIResponseParameters.class),
                        anyBoolean()))
                .thenReturn(sibffResponse);
        SIBFFResponse response = sibffService.getMobileServiceIntentDetails(headersData, payload);
        Assert.assertNotNull(response);
    }

    @Test
    public void showCartSummaryTestWhenIsCIMTransferRODeviceTrue() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.SERVICE_MIGRATION.toString());
        sibffResponse =
                (SIBFFResponse)
                        getResponse("/si_bapi_partial_error_response.json", SIBFFResponse.class);
        cibapiResponse =
                (CIBAPIResponse) getResponse("/ci_bapi_response.json", CIBAPIResponse.class);
        lenient().when(responseEntity.getBody()).thenReturn(cibapiResponse);
        when(config.getCustomerInformationURL()).thenReturn(customerInformation);
        lenient()
                .when(sibffHelper.getSIResponseParameters(any(SIBAPIResponse.class)))
                .thenReturn(SIResponseParameters.builder().isCIMTransferRODevice(true).build());
        lenient()
                .when(
                        sibffResponseMapper.createSIBFFResponseForSIOutcome(
                                any(), any(), any(), any(), anyBoolean()))
                .thenReturn(SIBFFResponse.builder().build());

        SIBFFResponse response = sibffService.getMobileServiceIntentDetails(headersData, payload);

        Assert.assertNotNull(response);
    }

    @Test
    public void getMobileServiceIntentDetailsUpdateNewOrderStateInfoForNullSiBffResponseTest() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.REDIRECT_TO_MYT.toString());

        when(sibffResponseMapper.createSIBFFResponse(
                        any(CIBAPIResponse.class),
                        any(RepaymentOptions.class),
                        anyString(),
                        any(CIResponseParameters.class),
                        anyBoolean()))
                .thenReturn(new SIBFFResponse());
        SIBFFResponse response = sibffService.getMobileServiceIntentDetails(headersData, payload);
        Assert.assertNotNull(response);
    }

    @Test(expected = SIBFFException.class)
    public void getMobileServiceIntentDetailsHardStopTest() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.HARDSTOP.toString());
        sibffService.getMobileServiceIntentDetails(headersData, payload);
    }

    @Test
    public void getMobileServiceIntentDetailsTest() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.REDIRECT_TO_MYT.toString());
        when(sibffResponseMapper.createSIBFFResponse(
                        any(CIBAPIResponse.class),
                        any(RepaymentOptions.class),
                        anyString(),
                        any(CIResponseParameters.class),
                        anyBoolean()))
                .thenReturn(SIBFFResponse.builder().build());
        SIBFFResponse response = sibffService.getMobileServiceIntentDetails(headersData, payload);
        Assert.assertNotNull(response);
    }

    @Test
    public void
            getMobileServiceIntentDetailsForAddRoToExistingService_REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO_Test() {
        OrderingStateInfo cacheData =
                (OrderingStateInfo)
                        getResponse(
                                "/orderingStateInfo_AddRoToExistingService.json",
                                OrderingStateInfo.class);
        Mockito.lenient().when(cacheUtil.getCacheData(anyString())).thenReturn(cacheData);
        Mockito.lenient()
                .when(cacheUtil.getCacheKey(anyString(), anyMap(), anyString()))
                .thenReturn(correlationId);
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(
                        Constants.CI_OUTCOME.REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO
                                .toString());
        when(sibffResponseMapper.createSIBFFResponse(
                        any(CIBAPIResponse.class),
                        any(RepaymentOptions.class),
                        anyString(),
                        any(CIResponseParameters.class),
                        anyBoolean()))
                .thenReturn(SIBFFResponse.builder().build());
        lenient()
                .when(restTemplate.exchange(any(), any(), any(), eq(ETCBapiResponse.class)))
                .thenReturn(new ResponseEntity<>(etcBapiResponse, HttpStatus.OK));
        when(config.getETCUrl()).thenReturn(getETCUrl);
        when(sibffHelper.getCIResponseParameters(any(), any(), any(), any()))
                .thenReturn(getCIResponseParameters());
        SIBFFResponse response = sibffService.getMobileServiceIntentDetails(headersData, payload);
        Assert.assertNotNull(response);
    }

    @Test
    public void
            getMobileServiceIntentDetailsForAddRoToExistingService_BUY_PHONE_AND_DISCONNECT_RO_Test() {
        OrderingStateInfo cacheData =
                (OrderingStateInfo)
                        getResponse(
                                "/orderingStateInfo_AddRoToExistingService.json",
                                OrderingStateInfo.class);
        Mockito.lenient().when(cacheUtil.getCacheData(anyString())).thenReturn(cacheData);
        Mockito.lenient()
                .when(cacheUtil.getCacheKey(anyString(), anyMap(), anyString()))
                .thenReturn(correlationId);
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.BUY_PHONE_AND_DISCONNECT_RO.toString());
        when(sibffResponseMapper.createSIBFFResponse(
                        any(CIBAPIResponse.class),
                        any(RepaymentOptions.class),
                        anyString(),
                        any(CIResponseParameters.class),
                        anyBoolean()))
                .thenReturn(SIBFFResponse.builder().build());
        lenient()
                .when(restTemplate.exchange(any(), any(), any(), eq(ETCBapiResponse.class)))
                .thenReturn(new ResponseEntity<>(etcBapiResponse, HttpStatus.OK));
        when(config.getETCUrl()).thenReturn(getETCUrl);
        when(sibffHelper.getCIResponseParameters(any(), any(), any(), any()))
                .thenReturn(getCIResponseParameters());
        SIBFFResponse response = sibffService.getMobileServiceIntentDetails(headersData, payload);
        Assert.assertNotNull(response);
    }

    @Test
    public void getMobileServiceIntentDetailsForREDIRECT_TO_MYT_Test() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.REDIRECT_TO_MYT.toString());
        when(sibffResponseMapper.createSIBFFResponse(
                        any(CIBAPIResponse.class),
                        any(RepaymentOptions.class),
                        anyString(),
                        any(CIResponseParameters.class),
                        anyBoolean()))
                .thenReturn(SIBFFResponse.builder().build());
        lenient()
                .when(restTemplate.exchange(any(), any(), any(), eq(ETCBapiResponse.class)))
                .thenReturn(new ResponseEntity<>(etcBapiResponse, HttpStatus.OK));
        when(config.getETCUrl()).thenReturn(getETCUrl);
        when(sibffHelper.getCIResponseParameters(any(), any(), any(), any()))
                .thenReturn(getCIResponseParameters());
        SIBFFResponse response = sibffService.getMobileServiceIntentDetails(headersData, payload);
        Assert.assertNotNull(response);
    }

    @Test
    public void exception_In_getMobileServiceIntentDetailsForREDIRECT_TO_MYT_Test() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.REDIRECT_TO_MYT.toString());

        when(sibffHelper.getCIResponseParameters(any(), any(), any(), any()))
                .thenReturn(getCIResponseParameters());
        Assert.assertThrows(
                Exception.class,
                () -> sibffService.getMobileServiceIntentDetails(headersData, payload));
    }

    @Test
    public void exception_In_getEtcBAPIResponse() {
        when(config.getETCUrl())
                .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
        Assert.assertThrows(
                Exception.class,
                () -> sibffService.getEtcBAPIResponse(headersData, "1234", "1234"));
    }

    @Test(expected = SIBFFException.class)
    public void getMobileServiceIntentDetailsServiceMigrationWithHardStopTest() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.SERVICE_MIGRATION.toString());
        lenient()
                .when(sibffHelper.getSIOutcome(any(SIResponseParameters.class)))
                .thenReturn(Constants.SI_OUTCOME.HARDSTOP.toString());
        sibffService.getMobileServiceIntentDetails(headersData, payload);
    }

    @Test
    public void getMobileServiceIntentDetailsServiceMigrationTest() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.SERVICE_MIGRATION.toString());
        lenient()
                .when(sibffHelper.getSIOutcome(any(SIResponseParameters.class)))
                .thenReturn(Constants.SI_OUTCOME.CONTINUE.toString());
        when(sibffResponseMapper.createSIBFFResponseForSIOutcome(
                        any(SIBAPIResponse.class),
                        anyString(),
                        any(SIResponseParameters.class),
                        any(CIResponseParameters.class),
                        anyBoolean()))
                .thenReturn(SIBFFResponse.builder().build());
        SIBFFResponse response = sibffService.getMobileServiceIntentDetails(headersData, payload);
        Assert.assertNotNull(response);
    }

    @Test(expected = SIBFFException.class)
    public void getMobileServiceIntentDetailsServiceMigrationExceptionTest() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.SERVICE_MIGRATION.toString());
        doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .when(responseEntityForSI)
                .getBody();
        sibffService.getMobileServiceIntentDetails(headersData, payload);
    }

    @Test(expected = SIBFFException.class)
    public void getMobileServiceIntentDetailsExceptionTest() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.SERVICE_MIGRATION.toString());
        doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .when(responseEntity)
                .getBody();
        sibffService.getMobileServiceIntentDetails(headersData, payload);
    }

    @Test
    public void getStreamingServiceEligibilitySuccessResponseTest() {
        lenient()
                .when(
                        restTemplate.exchange(
                                eq(serviceIntentGetEligibilityUrl),
                                any(),
                                any(),
                                eq(ServiceIntentGetEligibilityResponse.class)))
                .thenReturn(
                        new ResponseEntity<>(
                                serviceIntentGetEligibiliy_successResponse, HttpStatus.OK));
        var streamingServiceIntentSuccess =
                sibffService.getStreamingServiceIntentDetails(headersData);
        assert (streamingServiceIntentSuccess.getData().getIsNewDeviceAllowed());
        Assert.assertEquals(true, streamingServiceIntentSuccess.getSuccess());
    }

    @Test(expected = SIBFFException.class)
    public void getStreamingServiceEligibilityInternalServerErrorTest() {
        lenient()
                .when(
                        restTemplate.exchange(
                                eq(serviceIntentGetEligibilityUrl),
                                any(),
                                any(),
                                eq(ServiceIntentGetEligibilityResponse.class)))
                .thenReturn(responseEntityForServiceIntentGetEligibility);
        doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .when(responseEntityForServiceIntentGetEligibility)
                .getBody();
        OrderingStateInfo cacheData =
                (OrderingStateInfo)
                        getResponse(
                                "/orderingStateInfoWithServiceId.json", OrderingStateInfo.class);
        when(sibffHelper.getCacheData(any(HeadersData.class))).thenReturn(cacheData);

        sibffService.getStreamingServiceIntentDetails(headersData);
    }

    @Test(expected = SIBFFException.class)
    public void getStreamingServiceEligibilityAESEligibilityErrorTest() {

        lenient()
                .when(
                        restTemplate.exchange(
                                eq(serviceIntentGetEligibilityUrl),
                                any(),
                                any(),
                                eq(ServiceIntentGetEligibilityResponse.class)))
                .thenReturn(responseEntityForServiceIntentGetEligibility);
        doThrow(
                        new HttpClientErrorException(
                                HttpStatus.FAILED_DEPENDENCY,
                                HttpStatus.FAILED_DEPENDENCY.getReasonPhrase()))
                .when(responseEntityForServiceIntentGetEligibility)
                .getBody();
        OrderingStateInfo cacheData =
                (OrderingStateInfo)
                        getResponse(
                                "/orderingStateInfoWithServiceId.json", OrderingStateInfo.class);
        when(sibffHelper.getCacheData(any(HeadersData.class))).thenReturn(cacheData);
        sibffService.getStreamingServiceIntentDetails(headersData);
    }

    @Test
    public void getMobileServiceIntentDetailsServiceMigrationWithIsCIMTransferTest() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.SERVICE_MIGRATION.toString());
        lenient()
                .when(sibffHelper.getSIOutcome(any(SIResponseParameters.class)))
                .thenReturn(Constants.SI_OUTCOME.CONTINUE.toString());
        lenient()
                .when(sibffHelper.getSIResponseParameters(any(SIBAPIResponse.class)))
                .thenReturn(SIResponseParameters.builder().isCIMTransferRODevice(true).build());
        OrderingStateInfo orderingStateInfo = new OrderingStateInfo();
        List<CartActionCodes> listCartActionCodes = new ArrayList<>();
        orderingStateInfo.setCartActionCodes(listCartActionCodes);

        CartActionCodes cartAction = new CartActionCodes();
        cartAction.setActionCode("some action");
        cartAction.setActionType("some type");
        listCartActionCodes.add(cartAction);

        // Assert
        assertEquals(listCartActionCodes, orderingStateInfo.getCartActionCodes());
        when(sibffResponseMapper.createSIBFFResponseForSIOutcome(
                        any(SIBAPIResponse.class),
                        anyString(),
                        any(SIResponseParameters.class),
                        any(CIResponseParameters.class),
                        anyBoolean()))
                .thenReturn(
                        SIBFFResponse.builder().etcDetails(sibffResponse.getEtcDetails()).build());
        SIBFFResponse response = sibffService.getMobileServiceIntentDetails(headersData, payload);
        Assert.assertNotNull(response);
    }

    @Test
    public void cacheBFFResponseTest_removeDUP() {
        CacheRequest cacheRequest = CacheRequest.builder().action(REMOVE_DUP).build();
        CacheBFFResponse cacheBFFResponse =
                CacheBFFResponse.builder()
                        .success(true)
                        .externalId(headersData.getCorrelationId())
                        .statusCode(201)
                        .time(LocalDateTime.now().toString())
                        .build();

        List<CartActionCodes> cartActionCodesList = new ArrayList<>();
        CartActionCodes cartActionCodes;
        OrderingStateInfo orderingStateInfo = new OrderingStateInfo();
        cartActionCodes =
                CartActionCodes.builder()
                        .actionCode(Constants.CART_ACTION_CODE_OC_CRTRUL_0001)
                        .productCode("productCode")
                        .actionType(REMOVE_PRODUCT_FROM_CART)
                        .build();
        cartActionCodesList.add(cartActionCodes);
        orderingStateInfo.setCartActionCodes(cartActionCodesList);
        CacheBFFResponse actualResponse = sibffService.cacheBFFResponse(cacheRequest, headersData);
        assertEquals(cacheBFFResponse.getStatusCode(), actualResponse.getStatusCode());
    }

    @Test
    public void cacheBFFResponseTest_addDUP() {
        CacheRequest cacheRequest = CacheRequest.builder().action(ADD_DUP).build();
        CacheBFFResponse cacheBFFResponse =
                CacheBFFResponse.builder()
                        .success(true)
                        .externalId(headersData.getCorrelationId())
                        .statusCode(201)
                        .time(LocalDateTime.now().toString())
                        .build();

        List<CartActionCodes> cartActionCodesList = new ArrayList<>();
        CartActionCodes cartActionCodes;
        OrderingStateInfo orderingStateInfo = new OrderingStateInfo();
        cartActionCodes =
                CartActionCodes.builder()
                        .actionCode(Constants.CART_ACTION_CODE_OC_CRTRUL_0002)
                        .productCode("productCode")
                        .actionType(ADD_PRODUCTS_TO_CART)
                        .build();
        cartActionCodesList.add(cartActionCodes);
        orderingStateInfo.setCartActionCodes(cartActionCodesList);
        CacheBFFResponse actualResponse = sibffService.cacheBFFResponse(cacheRequest, headersData);
        assertEquals(cacheBFFResponse.getStatusCode(), actualResponse.getStatusCode());
    }

    @Test
    public void cacheBFFResponse_exception() {

        CacheRequest cacheRequest = CacheRequest.builder().action("action").build();
        assertThrows(
                SIBFFException.class,
                () -> {
                    sibffService.cacheBFFResponse(cacheRequest, headersData);
                });
    }

    @Test
    public void getMobileServiceIntentDetailsServiceMigrationWithSimCategoryTest() {
        lenient()
                .when(
                        sibffHelper.getCIOutcome(
                                any(CIResponseParameters.class), any(OrderingStateInfo.class)))
                .thenReturn(Constants.CI_OUTCOME.SERVICE_MIGRATION.toString());
        lenient()
                .when(sibffHelper.getSIOutcome(any(SIResponseParameters.class)))
                .thenReturn(Constants.SI_OUTCOME.CONTINUE.toString());
        lenient()
                .when(sibffHelper.getSIResponseParameters(any(SIBAPIResponse.class)))
                .thenReturn(SIResponseParameters.builder().isCIMTransferRODevice(true).build());
        OrderingStateInfo orderingStateInfo = new OrderingStateInfo();
        List<CartActionCodes> listCartActionCodes = new ArrayList<>();
        orderingStateInfo.setCartActionCodes(listCartActionCodes);
        orderingStateInfo.setHasDUP(true);
        CartActionCodes cartAction = new CartActionCodes();
        cartAction.setActionCode("some action");
        cartAction.setActionType("some type");
        listCartActionCodes.add(cartAction);
        ProductOrder productOrder = new ProductOrder();
        productOrder.setProductOrderItems(List.of(new ProductOrderItems()));
        orderingStateInfo.setProductOrder(productOrder);
        when(cacheUtil.getCacheData(any())).thenReturn(orderingStateInfo);
        when(sibffResponseMapper.createSIBFFResponseForSIOutcome(
                        any(SIBAPIResponse.class),
                        anyString(),
                        any(SIResponseParameters.class),
                        any(CIResponseParameters.class),
                        anyBoolean()))
                .thenReturn(
                        SIBFFResponse.builder().etcDetails(sibffResponse.getEtcDetails()).build());
        doAnswer(
                        invocation -> {
                            Object arg0 = invocation.getArgument(0);
                            Object arg1 = invocation.getArgument(1);
                            assertEquals(
                                    "3G Standard SIM",
                                    ((OrderingStateInfo) arg1)
                                            .getServiceMigrationSettings()
                                            .getSimCategory());
                            return null;
                        })
                .when(cacheUtil)
                .setCacheData(anyString(), any());
        SIBFFResponse response = sibffService.getMobileServiceIntentDetails(headersData, payload);
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
                .contractAssetReferenceId("temp")
                .build();
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

    private static String getAssetReferenceId(CIBAPIResponse ciBAPIResponse) {
        String assetReferenceId = null;
        List<ProductGroup> productGroups = ciBAPIResponse.getProductGroup();
        for (ProductGroup productGroup : productGroups) {
            for (Product product : productGroup.getProduct()) {
                for (ProductRelationship productRelationship : product.getProductRelationship()) {
                    for (Product product1 : productRelationship.getProduct()) {
                        for (ProductRelationship productRelationship1 :
                                product1.getProductRelationship()) {
                            for (Product product2 : productRelationship1.getProduct()) {
                                for (ProductRelationship productRelationship2 :
                                        product2.getProductRelationship()) {
                                    if (productRelationship2.getType().equals("CONTRACT")) {
                                        for (Product product3 : productRelationship2.getProduct()) {
                                            if (product3.getProductOffering()
                                                            .getType()
                                                            .equals("CONTRACT")
                                                    && product3.getProductOffering()
                                                            .getSubtype()
                                                            .equals("DPC")) {
                                                assetReferenceId = product3.getId();
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return assetReferenceId;
    }

    @Test
    public void testGetDeviceIntentDetails() throws SIBFFException {
        OrderingStateInfo orderingStateInfo = new OrderingStateInfo();
        orderingStateInfo.setRedeemOrder(false);
        when(cacheUtil.getCacheData(Mockito.<String>any())).thenReturn(orderingStateInfo);
        when(cacheUtil.getCacheKey(Mockito.<String>any(), Mockito.<Map<String, Object>>any(), Mockito.<String>any()))
                .thenReturn("Cache Key");
        doNothing()
                .when(validator)
                .validateCacheData(Mockito.<OrderingStateInfo>any(), Mockito.<String>any());
        GetSIPayload payload = new GetSIPayload("42", "01234567-89AB-CDEF-FEDC-BA9876543210");

        cibapiResponse =
                (CIBAPIResponse) getResponse("/ci_device_intent_response.json", CIBAPIResponse.class);

        ETCBapiResponse etcBAPIResponse =
                (ETCBapiResponse) getResponse("/getETC_bapi_success_response.json", ETCBapiResponse.class);

        assertThrows(
                SIBFFException.class,
                () -> sibffService.getDeviceIntentDetails(payload, new HeadersData()));
        verify(cacheUtil).getCacheData(Mockito.<String>any());
        verify(cacheUtil)
                .getCacheKey(
                        Mockito.<String>any(),
                        Mockito.<Map<String, Object>>any(),
                        Mockito.<String>any());
        verify(validator)
                .validateCacheData(Mockito.<OrderingStateInfo>any(), Mockito.<String>any());
    }

    @Test
    public void testGetAddOnsOf() {
        Product.ProductBuilder idResult = Product.builder().id("42");
        ProductCharacteristic productCharacteristic =
                ProductCharacteristic.builder()
                        .brand("Brand")
                        .callLineIdentificationType("Call Line Identification Type")
                        .cis("Cis")
                        .colour("Colour")
                        .dataTier("Data Tier")
                        .deviceName("Device Name")
                        .directoryListingType("/directory")
                        .imei("Imei")
                        .itemCondition("Item Condition")
                        .numberOfRepayments(10)
                        .paymentAgreementId("42")
                        .paymentMode("Payment Mode")
                        .purchaseType("Purchase Type")
                        .recurringAmount(1L)
                        .repaymentAmount(1L)
                        .serialNumber("42")
                        .serviceId("42")
                        .simSerialNumber("42")
                        .sku("Sku")
                        .storage("Storage")
                        .build();
        Product.ProductBuilder productCharacteristicResult =
                idResult.productCharacteristic(productCharacteristic);
        ProductOffering productOffering =
                ProductOffering.builder()
                        .id("42")
                        .name("Name")
                        .subtype("Subtype")
                        .type("Type")
                        .build();
        Product.ProductBuilder productOfferingResult =
                productCharacteristicResult.productOffering(productOffering);
        Product associateHardware =
                productOfferingResult
                        .productRelationship(new ArrayList<>())
                        .startDate("2020-03-01")
                        .status("Status")
                        .build();
        assertTrue(sibffService.getAddOnsOf(associateHardware).isEmpty());
    }

    @Test
    public void testGetAddOnsOf7() {
        ArrayList<Product> product = new ArrayList<>();
        Product.ProductBuilder idResult = Product.builder().id("42");
        ProductCharacteristic productCharacteristic =
                ProductCharacteristic.builder()
                        .brand("Brand")
                        .callLineIdentificationType("Call Line Identification Type")
                        .cis("Cis")
                        .colour("Colour")
                        .dataTier("Data Tier")
                        .deviceName("Device Name")
                        .directoryListingType("/directory")
                        .imei("Imei")
                        .itemCondition("Item Condition")
                        .numberOfRepayments(10)
                        .paymentAgreementId("42")
                        .paymentMode("Payment Mode")
                        .purchaseType("Purchase Type")
                        .recurringAmount(1L)
                        .repaymentAmount(1L)
                        .serialNumber("42")
                        .serviceId("42")
                        .simSerialNumber("42")
                        .sku("Sku")
                        .storage("Storage")
                        .build();
        Product.ProductBuilder productCharacteristicResult =
                idResult.productCharacteristic(productCharacteristic);
        ProductOffering productOffering =
                ProductOffering.builder()
                        .id("42")
                        .name("Name")
                        .subtype("Subtype")
                        .type("Type")
                        .build();
        Product.ProductBuilder productOfferingResult =
                productCharacteristicResult.productOffering(productOffering);
        Product buildResult =
                productOfferingResult
                        .productRelationship(new ArrayList<>())
                        .startDate("2020-03-01")
                        .status("Status")
                        .build();
        product.add(buildResult);
        ProductRelationship buildResult2 =
                ProductRelationship.builder().product(product).type("ADDON").build();

        ArrayList<ProductRelationship> productRelationshipList = new ArrayList<>();
        ProductRelationship.ProductRelationshipBuilder builderResult =
                ProductRelationship.builder();
        ProductRelationship buildResult3 =
                builderResult.product(new ArrayList<>()).type("Type").build();
        productRelationshipList.add(buildResult3);
        productRelationshipList.add(buildResult2);
        Product associateHardware = mock(Product.class);
        when(associateHardware.getProductRelationship()).thenReturn(productRelationshipList);
        List<Addon> actualAddOnsOf = sibffService.getAddOnsOf(associateHardware);
        verify(associateHardware).getProductRelationship();
        Addon getResult = actualAddOnsOf.get(0);
        assertEquals("Name", getResult.getName());
        assertEquals(1, actualAddOnsOf.size());
        assertEquals(1L, getResult.getRecurringAmount().longValue());
    }

    @Test
    public void testGetDeviceNameOf() {
        Product.ProductBuilder idResult = Product.builder().id("42");
        ProductCharacteristic productCharacteristic =
                ProductCharacteristic.builder()
                        .brand("Brand")
                        .callLineIdentificationType("Call Line Identification Type")
                        .cis("Cis")
                        .colour("Colour")
                        .dataTier("Data Tier")
                        .deviceName("Device Name")
                        .directoryListingType("/directory")
                        .imei("Imei")
                        .itemCondition("Item Condition")
                        .numberOfRepayments(10)
                        .paymentAgreementId("42")
                        .paymentMode("Payment Mode")
                        .purchaseType("Purchase Type")
                        .recurringAmount(1L)
                        .repaymentAmount(1L)
                        .serialNumber("42")
                        .serviceId("42")
                        .simSerialNumber("42")
                        .sku("Sku")
                        .storage("Storage")
                        .build();
        Product.ProductBuilder productCharacteristicResult =
                idResult.productCharacteristic(productCharacteristic);
        ProductOffering productOffering =
                ProductOffering.builder()
                        .id("42")
                        .name("Name")
                        .subtype("Subtype")
                        .type("Type")
                        .build();
        Product.ProductBuilder productOfferingResult =
                productCharacteristicResult.productOffering(productOffering);
        Product hardware =
                productOfferingResult
                        .productRelationship(new ArrayList<>())
                        .startDate("2020-03-01")
                        .status("Status")
                        .build();
        assertEquals("Device Name", sibffService.getDeviceNameOf(hardware));
    }

    @Test
    public void testGetHardwareForPlan() {
        ArrayList<ProductRelationship> productRelationshipList = new ArrayList<>();
        ProductRelationship.ProductRelationshipBuilder builderResult =
                ProductRelationship.builder();
        ProductRelationship buildResult =
                builderResult.product(new ArrayList<>()).type("Type").build();
        productRelationshipList.add(buildResult);
        Product planProduct = mock(Product.class);
        when(planProduct.getProductRelationship()).thenReturn(productRelationshipList);
        assertThrows(SIBFFException.class, () -> sibffService.getHardwareForPlan(planProduct));
        verify(planProduct).getProductRelationship();
    }

    @Test
    public void testGetPlanDetails() {
        PlanDetails actualPlanDetails = sibffService.getPlanDetails(new Product());
        assertNull(actualPlanDetails.getRecurringAmount());
        assertNull(actualPlanDetails.getName());
    }

    @Test
    public void testGetPlanProductForService() {
        CIBAPIResponse.CIBAPIResponseBuilder correlationIdResult =
                CIBAPIResponse.builder().correlationId("42");
        CIBAPIResponse.CIBAPIResponseBuilder errorsResult =
                correlationIdResult.errors(new ArrayList<>());
        CIBAPIResponse ciBAPIResponse =
                errorsResult
                        .productGroup(new ArrayList<>())
                        .statusCode("Status Code")
                        .success(true)
                        .time("Time")
                        .build();
        assertThrows(
                SIBFFException.class, () -> sibffService.getPlanProductForService(ciBAPIResponse));
    }

    @Test
    public void testSetService()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method setServiceMethod =
                SIBFFServiceImpl.class.getDeclaredMethod(
                        "setService", Boolean.class, String.class, List.class);
        setServiceMethod.setAccessible(true);

        ArrayList<Service> serviceList = new ArrayList<>();
        Service.ServiceBuilder builderResult = Service.builder();
        BusinessContext businessContext =
                BusinessContext.builder()
                        .family("Family")
                        .serviceId("42")
                        .subType("Sub Type")
                        .type("Type")
                        .build();
        Service.ServiceBuilder businessContextResult =
                builderResult.businessContext(businessContext);
        Service.ServiceBuilder outcomesResult = businessContextResult.outcomes(new ArrayList<>());
        Service buildResult = outcomesResult.qualifiers(new ArrayList<>()).build();
        serviceList.add(buildResult);

        assertTrue(
                (Boolean) setServiceMethod.invoke(new SIBFFServiceImpl(), true, "42", serviceList));
        assertFalse(
                (Boolean)
                        setServiceMethod.invoke(new SIBFFServiceImpl(), false, "42", serviceList));
    }

    @Test
    public void testSetServiceMultipleServices()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method setServiceMethod =
                SIBFFServiceImpl.class.getDeclaredMethod(
                        "setService", Boolean.class, String.class, List.class);
        setServiceMethod.setAccessible(true);

        ArrayList<Service> serviceList = new ArrayList<>();
        Service.ServiceBuilder builderResult = Service.builder();
        BusinessContext businessContext =
                BusinessContext.builder()
                        .family("Family")
                        .serviceId("42")
                        .subType("Sub Type")
                        .type("Type")
                        .build();
        Service.ServiceBuilder businessContextResult =
                builderResult.businessContext(businessContext);
        Service.ServiceBuilder outcomesResult = businessContextResult.outcomes(new ArrayList<>());
        Service buildResult = outcomesResult.qualifiers(new ArrayList<>()).build();
        serviceList.add(buildResult);

        Service.ServiceBuilder builderResult2 = Service.builder();
        BusinessContext businessContext2 =
                BusinessContext.builder()
                        .family("Family")
                        .serviceId("42")
                        .subType("Sub Type")
                        .type("Type")
                        .build();
        Service.ServiceBuilder businessContextResult2 =
                builderResult2.businessContext(businessContext2);
        Service.ServiceBuilder outcomesResult2 = businessContextResult2.outcomes(new ArrayList<>());
        Service buildResult2 = outcomesResult2.qualifiers(new ArrayList<>()).build();
        serviceList.add(buildResult2);

        assertTrue(
                (Boolean) setServiceMethod.invoke(new SIBFFServiceImpl(), true, "42", serviceList));
    }

    @Test
    public void testGetResponse() {
        Class<Object> c = Object.class;
        assertThrows(
                RuntimeException.class, () -> invokePrivateMethod("getResponse", "Json Path", c));
    }

    @Test
    public void testGetResponse2() {
        Class<Object> c = Object.class;
        assertThrows(RuntimeException.class, () -> invokePrivateMethod("getResponse", "", c));
    }

    private Object invokePrivateMethod(String methodName, String jsonPath, Class<?> c) {
        try {
            Method privateMethod =
                    SIBFFServiceImpl.class.getDeclaredMethod(methodName, String.class, Class.class);
            privateMethod.setAccessible(true);
            return privateMethod.invoke(sibffService, jsonPath, c);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testBodyConditionAsNew() {
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setBodyCondition("AS_NEW");

        SIBFFServiceImpl sibffService = new SIBFFServiceImpl();
        String result = sibffService.bodyCondition(payload);

        assertEquals("No", result);
    }

    @Test
    public void testBodyConditionGeneralWearAndTear() {
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setBodyCondition("GENERAL_WEAR_AND_TEAR");

        SIBFFServiceImpl sibffService = new SIBFFServiceImpl();
        String result = sibffService.bodyCondition(payload);

        assertEquals("No", result);
    }

    @Test
    public void testBodyConditionOther() {
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setBodyCondition("DAMAGED");

        SIBFFServiceImpl sibffService = new SIBFFServiceImpl();
        String result = sibffService.bodyCondition(payload);

        assertEquals("Yes", result);
    }

    @Test
    public void testScreenConditionNoCracks() {
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setScreenCondition("NO_CRACKS");

        SIBFFServiceImpl sibffService = new SIBFFServiceImpl();
        String result = sibffService.screenCondition(payload);

        assertEquals("No", result);
    }

    @Test
    public void testScreenConditionWithCracks() {
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setScreenCondition("CRACKS");

        SIBFFServiceImpl sibffService = new SIBFFServiceImpl();
        String result = sibffService.screenCondition(payload);

        assertEquals("Yes", result);
    }

    @Test
    public void testLiquidConditionNoDamage() {
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setLiquidCondition("NO_LIQUID_DAMAGE");

        SIBFFServiceImpl sibffService = new SIBFFServiceImpl();
        String result = sibffService.liquidCondition(payload);

        assertEquals("No", result);
    }

    @Test
    public void testLiquidConditionWithDamage() {
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setLiquidCondition("LIQUID_DAMAGE");

        SIBFFServiceImpl sibffService = new SIBFFServiceImpl();
        String result = sibffService.liquidCondition(payload);

        assertEquals("Yes", result);
    }

    @Test
    public void testHandleErrorResponseForAssessDeviceBAPI_OtherException() {
        // Arrange
        Exception otherException = new RuntimeException("Some internal error");

        SIBFFServiceImpl sibffService =
                new SIBFFServiceImpl(); // replace YourClass with the actual class name

        assertThrows(
                SIBFFException.class,
                () -> {
                    sibffService.handleErrorResponseForAssessDeviceBAPI(otherException);
                });
    }

    @Test
    public void invokeAssessDeviceBAPITest() {

        HeadersData headersDatas = mock(HeadersData.class);
        AssessDeviceConditionPayload assessDeviceCondition =
                mock(AssessDeviceConditionPayload.class);
        OrderingStateInfo orderingStateInfo1 = mock(OrderingStateInfo.class);

        DeviceAssessmentResponse deviceAssessmentResponse =
                (DeviceAssessmentResponse)
                        getResponse(
                                "/DeviceAssessBAPIResponse.json", DeviceAssessmentResponse.class);

        AssessDeviceResponse assessDeviceResponse =
                (AssessDeviceResponse)
                        getResponse("/AssessGoodDeviceResponse.json", AssessDeviceResponse.class);

        AssessDeviceResponse assessDeviceBadResponse =
                (AssessDeviceResponse)
                        getResponse("/AssessBadDeviceResponse.json", AssessDeviceResponse.class);

        List<RelatedParty> relatedParty = new ArrayList<>();
        List<DeviceAssessment> deviceAssessment = new ArrayList<>();
        List<Characteristic> characteristics = new ArrayList<>();
        List<DeviceCondition> deviceConditionList = new ArrayList<>();

        DeviceCondition screenCondition =
                DeviceCondition.builder()
                        .name("screenDamage")
                        .value("No")
                        .valueType("String")
                        .build();
        DeviceCondition bodyCondition =
                DeviceCondition.builder()
                        .name("bodyDamage")
                        .value("No")
                        .valueType("String")
                        .build();
        DeviceCondition liquidCondition =
                DeviceCondition.builder()
                        .name("liquidDamage")
                        .value("No")
                        .valueType("String")
                        .build();

        deviceConditionList.add(screenCondition);
        deviceConditionList.add(bodyCondition);
        deviceConditionList.add(liquidCondition);

        Characteristic characteristic =
                Characteristic.builder()
                        .name("caseId")
                        .value("DCMMC0019661")
                        .valueType("String")
                        .build();
        characteristics.add(characteristic);

        RelatedParty relatedPart =
                RelatedParty.builder()
                        .id("c809695e-01b8-2fc6-d3b1-4bda453d3879")
                        .role("Account")
                        .type("RelatedParty")
                        .referredType("Indiviual")
                        .build();
        relatedParty.add(relatedPart);
        DeviceAssessment deviceAssessments =
                DeviceAssessment.builder()
                        .caseId("DCMMC0019661")
                        .assessmentType(AssessmentType.builder().name("grading").build())
                        .characteristics(characteristics)
                        .deviceDetails(
                                AssessDeviceConditionDetails.builder()
                                        .deviceCondition(deviceConditionList)
                                        .build())
                        .build();

        deviceAssessment.add(deviceAssessments);

        var assessDevice =
                AssessDevice.builder()
                        .serviceId("6423456767")
                        .relatedParty(relatedParty)
                        .deviceAssessment(deviceAssessment)
                        .build();

        lenient()
                .when(
                        restTemplate.exchange(
                                any(), any(), any(), eq(DeviceAssessmentResponse.class)))
                .thenReturn(new ResponseEntity<>(deviceAssessmentResponse, HttpStatus.OK));

        assertThrows(
                SIBFFException.class,
                () ->
                        sibffService.invokeAssessDeviceBAPI(
                                headersDatas, assessDeviceCondition, orderingStateInfo1));
    }

    @Test
    public void getDeviceAssessDetails() throws SIBFFException {

        OrderingStateInfo orderingStateInfo =
                (OrderingStateInfo)
                        getResponse("/orderingStateInfoDeviceAssess.json", OrderingStateInfo.class);

        AssessDevice assessDevice =
                (AssessDevice) getResponse("/DeviceAssess.json", AssessDevice.class);
        DeviceAssessmentResponse assessDeviceResponse =
                (DeviceAssessmentResponse)
                        getResponse(
                                "/DeviceAssessBAPIResponse.json", DeviceAssessmentResponse.class);

        AssessDeviceResponse.builder()
                .statusCode("200")
                .success(true)
                .data(
                        AssessDeviceData.builder()
                                .assessmentResult("good")
                                .redemptionFee("99.00")
                                .deviceAssessedFor(
                                        AssessDeviceConditionPayload.builder()
                                                .screenCondition("NO_CRACKS")
                                                .bodyCondition("AS_NEW")
                                                .liquidCondition("NO_LIQUID_DAMAGE")
                                                .build())
                                .deviceDetails(
                                        AssessDeviceDetails.builder()
                                                .name("phone")
                                                .recurringAmount("148")
                                                .build())
                                .build())
                .build();

        HeadersData headersData = mock(HeadersData.class);
        AssessDeviceConditionPayload assessDeviceCondition =
                mock(AssessDeviceConditionPayload.class);

        assertThrows(
                SIBFFException.class,
                () -> sibffService.getDeviceAssessDetails(assessDeviceCondition, headersData));
    }

    @Test
    @SneakyThrows
    public void getVerifyImeiDetails_invokeGetEligibility_False_outcome_throw_exception() {
        VerifyImeiPayload payload =
                objectMapper.readValue(
                        new ClassPathResource("/VerifyImeiRequestPayload.json").getFile(),
                        VerifyImeiPayload.class);
        ServiceIntentGetEligibilityResponse serviceIntentGetEligibilityResponse =
                objectMapper.readValue(
                        new ClassPathResource("/redemptionEligibilityResponseSuccess.json")
                                .getFile(),
                        ServiceIntentGetEligibilityResponse.class);
        serviceIntentGetEligibilityResponse
                .getServices()
                .get(0)
                .getOutcomes()
                .get(0)
                .setSuccess(false);
        OrderingStateInfo cacheData =
                (OrderingStateInfo) getResponse("/orderingStateInfo.json", OrderingStateInfo.class);
        when(cacheUtil.getCacheData(anyString())).thenReturn(cacheData);
        when(config.getServiceIntentGetEligibilityURL()).thenReturn("http://getEligibility");
        Mockito.when(
                        restTemplate.exchange(
                                anyString(),
                                any(),
                                any(),
                                eq(ServiceIntentGetEligibilityResponse.class)))
                .thenReturn(responseEntityForServiceIntentGetEligibility);
        lenient()
                .when(responseEntityForServiceIntentGetEligibility.getBody())
                .thenReturn(serviceIntentGetEligibilityResponse);
        Assertions.assertThrows(
                SIBFFException.class, () -> sibffService.verifyImeiDetails(payload, headersData));
    }

    @Test
    @SneakyThrows
    public void
            getVerifyImeiDetails_invokeGetEligibility_true_outcome_success_for_partial_imei_verification() {
        VerifyImeiPayload payload =
                objectMapper.readValue(
                        new ClassPathResource("/VerifyImeiRequestPayload.json").getFile(),
                        VerifyImeiPayload.class);
        ServiceIntentGetEligibilityResponse serviceIntentGetEligibilityResponse =
                objectMapper.readValue(
                        new ClassPathResource("/redemptionEligibilityResponseSuccess.json")
                                .getFile(),
                        ServiceIntentGetEligibilityResponse.class);
        DeviceAssessResponse deviceAssessResponse =
                objectMapper.readValue(
                        new ClassPathResource("/DeviceAssessResponse.json").getFile(),
                        DeviceAssessResponse.class);

        OrderingStateInfo cacheData =
                (OrderingStateInfo) getResponse("/orderingStateInfo.json", OrderingStateInfo.class);
        when(cacheUtil.getCacheData(anyString())).thenReturn(cacheData);
        when(config.getServiceIntentGetEligibilityURL()).thenReturn("http://getEligibility");
        Mockito.when(
                        restTemplate.exchange(
                                anyString(),
                                any(),
                                any(),
                                eq(ServiceIntentGetEligibilityResponse.class)))
                .thenReturn(responseEntityForServiceIntentGetEligibility);
        lenient()
                .when(responseEntityForServiceIntentGetEligibility.getBody())
                .thenReturn(serviceIntentGetEligibilityResponse);

        when(config.getDeviceAssessment()).thenReturn("http://deviceAssessment");
        Mockito.when(
                        restTemplate.exchange(
                                anyString(), any(), any(), eq(DeviceAssessResponse.class)))
                .thenReturn(responseEntityForDeviceAssessResponse);
        lenient()
                .when(responseEntityForDeviceAssessResponse.getBody())
                .thenReturn(deviceAssessResponse);

        DeviceAssessResponse response = sibffService.verifyImeiDetails(payload, headersData);
        assertEquals(HttpStatus.OK.value(), Integer.parseInt(response.getStatusCode()));
        assertNotNull(response.getDeviceAssessment());
        assertEquals("BLACK", response.getDeviceAssessment().get(0).getDeviceDetails().getColor());
        assertEquals(
                3,
                response.getDeviceAssessment()
                        .get(0)
                        .getOutcomes()
                        .get(0)
                        .getQuestionnaire()
                        .size());
    }

    @Test
    @SneakyThrows
    public void
            getVerifyImeiDetails_invokeGetEligibility_true_outcome_success_for_full_imei_verification() {
        VerifyImeiPayload payload =
                objectMapper.readValue(
                        new ClassPathResource("/VerifyImeiRequestPayload.json").getFile(),
                        VerifyImeiPayload.class);
        payload.setValidationType(VERIFY_IMEI_FULL);
        ServiceIntentGetEligibilityResponse serviceIntentGetEligibilityResponse =
                objectMapper.readValue(
                        new ClassPathResource("/redemptionEligibilityResponseSuccess.json")
                                .getFile(),
                        ServiceIntentGetEligibilityResponse.class);
        DeviceAssessResponse deviceAssessResponse =
                objectMapper.readValue(
                        new ClassPathResource("/DeviceAssessResponse.json").getFile(),
                        DeviceAssessResponse.class);

        OrderingStateInfo cacheData =
                (OrderingStateInfo) getResponse("/orderingStateInfo.json", OrderingStateInfo.class);
        when(cacheUtil.getCacheData(anyString())).thenReturn(cacheData);
        when(config.getServiceIntentGetEligibilityURL()).thenReturn("http://getEligibility");
        Mockito.when(
                        restTemplate.exchange(
                                anyString(),
                                any(),
                                any(),
                                eq(ServiceIntentGetEligibilityResponse.class)))
                .thenReturn(responseEntityForServiceIntentGetEligibility);
        lenient()
                .when(responseEntityForServiceIntentGetEligibility.getBody())
                .thenReturn(serviceIntentGetEligibilityResponse);

        when(config.getDeviceAssessment()).thenReturn("http://deviceAssessment");
        Mockito.when(
                        restTemplate.exchange(
                                anyString(), any(), any(), eq(DeviceAssessResponse.class)))
                .thenReturn(responseEntityForDeviceAssessResponse);
        lenient()
                .when(responseEntityForDeviceAssessResponse.getBody())
                .thenReturn(deviceAssessResponse);

        DeviceAssessResponse response = sibffService.verifyImeiDetails(payload, headersData);
        assertEquals(HttpStatus.OK.value(), Integer.parseInt(response.getStatusCode()));
        assertNotNull(response.getDeviceAssessment());
        assertEquals("BLACK", response.getDeviceAssessment().get(0).getDeviceDetails().getColor());
        assertEquals(
                3,
                response.getDeviceAssessment()
                        .get(0)
                        .getOutcomes()
                        .get(0)
                        .getQuestionnaire()
                        .size());
    }

    @Test
    @SneakyThrows
    public void getVerifyImeiDetails_invokeGetEligibility_OSI_assetReferenceId_null() {
        VerifyImeiPayload payload =
                objectMapper.readValue(
                        new ClassPathResource("/VerifyImeiRequestPayload.json").getFile(),
                        VerifyImeiPayload.class);
        ServiceIntentGetEligibilityResponse serviceIntentGetEligibilityResponse =
                objectMapper.readValue(
                        new ClassPathResource("/redemptionEligibilityResponseSuccess.json")
                                .getFile(),
                        ServiceIntentGetEligibilityResponse.class);
        serviceIntentGetEligibilityResponse
                .getServices()
                .get(0)
                .getOutcomes()
                .get(0)
                .setSuccess(false);
        OrderingStateInfo cacheData =
                (OrderingStateInfo) getResponse("/orderingStateInfo.json", OrderingStateInfo.class);

        cacheData.getSelectedService().setServiceId(null);
        when(cacheUtil.getCacheData(anyString())).thenReturn(cacheData);

        Assertions.assertThrows(
                SIBFFException.class, () -> sibffService.verifyImeiDetails(payload, headersData));
    }

    @Test
    @SneakyThrows
    public void invokeGetEligibilityAPI_null_pointerexception() {
        Exception exception =
                Assertions.assertThrows(
                        Exception.class,
                        () -> {
                            sibffService.invokeGetEligibilityAPI(null, null);
                        });
        assertEquals("Invoked Get Eligibility API Processing failednull", exception.getMessage());
    }

    @Test
    @SneakyThrows
    public void stampDeviceDetailsIntoOrderStateInfo_null_pointerexception() {
        Exception exception =
                Assertions.assertThrows(
                        Exception.class,
                        () -> {
                            sibffService.stampDeviceDetailsIntoOrderStateInfo(null, null);
                        });
        assertEquals(
                "Device Details Stamping Into OrderStateInfo failed :null", exception.getMessage());
    }

    @Test
    @SneakyThrows
    public void stampDeviceIntentDetailsIntoOrderStateInfo_null_pointerexception() {
        Exception exception =
                Assertions.assertThrows(
                        Exception.class,
                        () -> {
                            sibffService.stampDeviceIntentDetailsIntoOrderingStateInfo(null, null);
                        });
        assertEquals(
                "Device Details Stamping Into OrderStateInfo failed :null", exception.getMessage());
    }

    @Test
    public void stampDeviceIntentDetailsIntoOrderStateInfo(){

        cibapiResponse =
                (CIBAPIResponse) getResponse("/ci_device_intent_response.json", CIBAPIResponse.class);

        ETCBapiResponse etcBAPIResponse =
                (ETCBapiResponse) getResponse("/getETC_bapi_success_response.json", ETCBapiResponse.class);
        assertThrows(
                SIBFFException.class,
                () ->sibffService.stampDeviceIntentDetailsIntoOrderingStateInfo(etcBAPIResponse,new HeadersData()));
    }
}
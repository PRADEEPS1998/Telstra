package com.telstra.p2o.serviceintent.bff.validator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telstra.p2o.common.caching.data.OrderingStateInfo;
import com.telstra.p2o.common.core.exception.MicrotokenValidationException;
import com.telstra.p2o.common.core.validator.MicroTokenValidators;
import com.telstra.p2o.common.core.validator.UserInfoValidator;
import com.telstra.p2o.serviceintent.bff.constant.Constants;
import com.telstra.p2o.serviceintent.bff.dto.bapi.CIBAPIResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.DeviceAssessResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.Errors;
import com.telstra.p2o.serviceintent.bff.dto.bapi.ProductGroup;
import com.telstra.p2o.serviceintent.bff.dto.bapi.SIBAPIResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.Service;
import com.telstra.p2o.serviceintent.bff.dto.bapi.ServiceIntentGetEligibilityResponse;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.VerifyImeiPayload;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.AssessDeviceConditionPayload;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.GetSIPayload;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.HeadersData;
import com.telstra.p2o.serviceintent.bff.exception.CacheDataNotFoundException;
import com.telstra.p2o.serviceintent.bff.exception.MethodArgumentNotValidException;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFError;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import com.telstra.p2o.serviceintent.bff.exception.UnAuthorizedException;
import com.telstra.p2o.serviceintent.bff.helper.SIBFFHelper;
import com.telstra.p2o.serviceintent.bff.util.SIBFFCacheUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;

@RunWith(MockitoJUnitRunner.class)
public class SIBFFValidatorTest {

    @InjectMocks SIBFFValidator sibffValidator;

    @Mock OrderingStateInfo orderingStateInfo;

    private HeadersData headersData;

    @Mock MicroTokenValidators microTokenValidators;

    @Mock private SIBFFHelper sibffHelper;

    private GetSIPayload payload;

    @Mock SIBFFCacheUtil sibffCacheUtil;

    @Mock UserInfoValidator userInfoValidator;

    ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        orderingStateInfo = mock(OrderingStateInfo.class);
        payload =
                GetSIPayload.builder()
                        .accountUUID("testAccountId")
                        .serviceId("testServiceId")
                        .build();
    }

    @Test(expected = UnAuthorizedException.class)
    public void validateHeadersForEmptyMicroTokenTest() {
        headersData = HeadersData.builder().channel("self-serve").correlationId("12345").build();
        sibffValidator.validateHeaders(headersData, payload);
    }

    @Test
    public void validateHeadersTest() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("b2c_uid", "testuid");
        claims.put("staff_userId", "testuid");
        Mockito.when(microTokenValidators.validate(anyString())).thenReturn(claims);
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        sibffValidator.validateHeaders(headersData, payload);
        Assert.assertNotNull(headersData.getClaims());
    }

    @Test(expected = UnAuthorizedException.class)
    public void validateHeadersWhenValidationFailedTest() {
        Mockito.doThrow(
                        new MicrotokenValidationException(
                                HttpStatus.INTERNAL_SERVER_ERROR.toString()))
                .when(microTokenValidators)
                .validate(anyString());
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        sibffValidator.validateHeaders(headersData, payload);
    }

    @Test(expected = MethodArgumentNotValidException.class)
    public void validateBFFControllerRequestTest() {
        String correlationId = "";
        sibffValidator.validateBFFControllerRequest(correlationId, payload);
    }

    @Test(expected = MethodArgumentNotValidException.class)
    public void validateBFFControllerRequestWhenPayloadIsEmptyTest() {
        String correlationId = "123";
        sibffValidator.validateBFFControllerRequest(correlationId, null);
    }

    @Test(expected = MethodArgumentNotValidException.class)
    public void validateBFFControllerRequestWhenAccountIdIsEmptyTest() {
        String correlationId = "123";
        sibffValidator.validateBFFControllerRequest(
                correlationId, GetSIPayload.builder().serviceId("testServiceId").build());
    }

    @Test(expected = MethodArgumentNotValidException.class)
    public void validateBFFControllerRequestWhenServiceIdIsEmptyTest() {
        String correlationId = "123";
        sibffValidator.validateBFFControllerRequest(
                correlationId, GetSIPayload.builder().accountUUID("testAccountId").build());
    }

    @Test(expected = CacheDataNotFoundException.class)
    public void validateCacheDataTest() {
        sibffValidator.validateCacheData(null, "testCacheKey");
    }

    @Test(expected = SIBFFException.class)
    public void validateCIBAPIResponseTest() {
        sibffValidator.validateCIBAPIResponse(null);
    }

    @Test(expected = SIBFFException.class)
    public void validateCIBAPIResponseWhenProductGroupIsEmptyTest() {
        sibffValidator.validateCIBAPIResponse(CIBAPIResponse.builder().statusCode("200").build());
    }

    @Test(expected = SIBFFException.class)
    public void validateCIBAPIResponseWhenStatusNotOkTest() {
        sibffValidator.validateCIBAPIResponse(
                CIBAPIResponse.builder()
                        .productGroup(Collections.singletonList(ProductGroup.builder().build()))
                        .statusCode("422")
                        .errors(
                                Collections.singletonList(
                                        SIBFFError.builder().message("testMessage").build()))
                        .build());
    }

    @Test(expected = SIBFFException.class)
    public void validateSIBAPIResponseTest() {
        sibffValidator.validateSIBAPIResponse(null);
    }

    @Test(expected = SIBFFException.class)
    public void validateSIBAPIResponseWhenProductGroupIsEmptyTest() {
        sibffValidator.validateSIBAPIResponse(
                SIBAPIResponse.builder()
                        .statusCode("422")
                        .errors(
                                Collections.singletonList(
                                        SIBFFError.builder().message("testMessage").build()))
                        .build());
    }

    @Test(expected = UnAuthorizedException.class)
    public void validateGetStreamingServiceEligibilityHeadersForEmptyMicroTokenTest() {
        headersData = HeadersData.builder().channel("self-serve").correlationId("12345").build();
        sibffValidator.validateHeaders(headersData);
    }

    @Test
    public void validateGetStreamingServiceEligibilityHeadersTest() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("b2c_uid", "testuid");
        claims.put("staff_userId", "testuid");
        Mockito.when(microTokenValidators.validate(anyString())).thenReturn(claims);
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        sibffValidator.validateHeaders(headersData);
        Assert.assertNotNull(headersData.getClaims());
    }

    @Test(expected = UnAuthorizedException.class)
    public void validateGetStreamingServiceEligibilityHeadersWhenValidationFailedTest() {
        Mockito.doThrow(
                        new MicrotokenValidationException(
                                HttpStatus.INTERNAL_SERVER_ERROR.toString()))
                .when(microTokenValidators)
                .validate(anyString());
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        sibffValidator.validateHeaders(headersData);
    }

    @Test(expected = MethodArgumentNotValidException.class)
    public void validateStreamingServiceIntentDetailsRequestNullPayloadTest() {
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        sibffValidator.validateStreamingServiceIntentDetailsRequest(headersData, null);
    }

    @Test
    public void validateStreamingServiceIntentDetailsRequestEmptyServiceIdTest() {
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        payload = GetSIPayload.builder().accountUUID("some-account-uuid").build();
        lenient().when(sibffHelper.getCacheData(headersData)).thenReturn(orderingStateInfo);
        assertDoesNotThrow(
                () ->
                        sibffValidator.validateStreamingServiceIntentDetailsRequest(
                                headersData, payload));
    }

    @Test(expected = MethodArgumentNotValidException.class)
    public void validateStreamingServiceIntentDetailsRequestEmptyAccountUUIDTest() {
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        payload = GetSIPayload.builder().serviceId("some-service-id").build();
        lenient().when(sibffHelper.getCacheData(headersData)).thenReturn(orderingStateInfo);
        sibffValidator.validateStreamingServiceIntentDetailsRequest(headersData, payload);
    }

    @Test
    public void validateServiceIdTest() {
        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId("12345")
                        .build();
        String serviceId = "testServiceId";
        Mockito.when(sibffCacheUtil.getCacheData(any())).thenReturn(orderingStateInfo);
        doNothing().when(userInfoValidator).validateCustomerAccountInfo(any(), any());
        sibffValidator.validateServiceId(headersData, serviceId);
    }

    @Test
    public void validateAssessDeviceRequestWithEmptyScreenConditionInPayloadTest() {
        HeadersData headersData =
                HeadersData.builder().correlationId("12345").microToken("validMicroToken").build();
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setScreenCondition("");

        MethodArgumentNotValidException exception =
                assertThrows(
                        MethodArgumentNotValidException.class,
                        () -> {
                            sibffValidator.validateAssessDeviceRequest(headersData, payload);
                        });

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("ScreenCondition in input payload is missing", exception.getMessage());
    }

    @Test
    public void validateAssessDeviceRequestWithEmptyLiquidConditionInPayloadTest() {
        HeadersData headersData =
                HeadersData.builder().correlationId("12345").microToken("validMicroToken").build();
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setLiquidCondition(
                ""); // Set an empty liquidCondition to trigger validation failure

        MethodArgumentNotValidException exception =
                assertThrows(
                        MethodArgumentNotValidException.class,
                        () -> {
                            sibffValidator.validateAssessDeviceRequest(headersData, payload);
                        });

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
    }

    @Test
    public void validateAssessDeviceRequestWithEmptyBodyConditionInPayloadTest() {
        HeadersData headersData =
                HeadersData.builder().correlationId("12345").microToken("validMicroToken").build();
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setBodyCondition(""); // Set an empty bodyCondition to trigger validation failure

        MethodArgumentNotValidException exception =
                assertThrows(
                        MethodArgumentNotValidException.class,
                        () -> {
                            sibffValidator.validateAssessDeviceRequest(headersData, payload);
                        });

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
    }

    @Test
    public void validateAssessDeviceRequestWithValidPayloadTest() {
        HeadersData headersData =
                HeadersData.builder().correlationId("12345").microToken("validMicroToken").build();
        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        payload.setScreenCondition("validScreenCondition");
        payload.setLiquidCondition("validLiquidCondition");
        payload.setBodyCondition("validBodyCondition");
        sibffValidator.validateAssessDeviceRequest(headersData, payload);
    }

    @Test
    public void validateAssessDeviceRequestWithEmptyPayloadTest() {
        HeadersData headersData =
                HeadersData.builder().correlationId("12345").microToken("validMicroToken").build();
        AssessDeviceConditionPayload payload = null;

        MethodArgumentNotValidException exception =
                assertThrows(
                        MethodArgumentNotValidException.class,
                        () -> {
                            sibffValidator.validateAssessDeviceRequest(headersData, payload);
                        });

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals("Input payload is empty", exception.getMessage());
    }

    @Test
    public void validateserviceIntentGetEligibilityResponse_respose_payload_is_null() {

        ServiceIntentGetEligibilityResponse payload = null;

        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateserviceIntentGetEligibilityResponse(payload);
                        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
        assertEquals(Constants.EMPTY_REDEMPTION_ELIGIBILITY_RESPONSE, exception.getMessage());
    }

    @Test
    public void validateserviceIntentGetEligibilityResponse_respose_payload_is_statuscode_not_ok() {

        ServiceIntentGetEligibilityResponse payload = new ServiceIntentGetEligibilityResponse();
        payload.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateserviceIntentGetEligibilityResponse(payload);
                        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
        assertEquals(
                "Error Message for service intent from BAPI:  Service Error",
                exception.getMessage());
    }

    @Test
    public void validateserviceIntentGetEligibilityResponse_respose_payload_having_error() {

        ServiceIntentGetEligibilityResponse payload = new ServiceIntentGetEligibilityResponse();
        List<Errors> errors = new ArrayList<>();
        Errors errorsIntent = Errors.builder().code("1001").message("Service Error").build();
        errors.add(errorsIntent);
        payload.setErrors(errors);
        payload.setStatusCode(HttpStatus.OK.value());
        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateserviceIntentGetEligibilityResponse(payload);
                        });

        assertEquals(HttpStatus.OK.value(), exception.getStatusCode());
        assertEquals(Constants.SERVICE_ERROR.trim(), exception.getMessage().trim());
    }

    @Test
    public void
            validateserviceIntentGetEligibilityResponse_respose_payload_having_service_details_empty() {

        ServiceIntentGetEligibilityResponse payload = new ServiceIntentGetEligibilityResponse();
        payload.setStatusCode(HttpStatus.OK.value());
        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateserviceIntentGetEligibilityResponse(payload);
                        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
        assertEquals(Constants.BAPI_RESPONSE_SERVICE_DETAIL_NULL, exception.getMessage().trim());
    }

    @Test
    public void
            validateserviceIntentGetEligibilityResponse_respose_payload_having_outcome_is_empty() {

        ServiceIntentGetEligibilityResponse payload = new ServiceIntentGetEligibilityResponse();
        List<Service> services = new ArrayList<>();
        Service service = new Service();
        services.add(service);
        payload.setStatusCode(HttpStatus.OK.value());

        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateserviceIntentGetEligibilityResponse(payload);
                        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
        assertEquals(Constants.BAPI_RESPONSE_SERVICE_DETAIL_NULL, exception.getMessage().trim());
    }

    @Test
    public void validateVerifyImeiValidationResponse_respose_payload_is_null() {

        DeviceAssessResponse payload = null;

        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateVerifyImeiValidationResponse(payload);
                        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
        assertEquals(Constants.EMPTY_VALIDATE_IMEI_RESPONSE, exception.getMessage());
    }

    @Test
    public void validateVerifyImeiValidationResponse_respose_payload_is_statuscode_not_ok() {

        DeviceAssessResponse payload = new DeviceAssessResponse();
        payload.setStatusCode(Constants.HTTP_STATUS_INTERNAL_SERVER_ERROR);
        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateVerifyImeiValidationResponse(payload);
                        });

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getStatusCode());
        assertEquals(
                "Error Message for service intent from BAPI:  Service Error",
                exception.getMessage());
    }

    @Test
    public void validateVerifyImeiValidationResponse_respose_payload_having_error() {

        DeviceAssessResponse payload = new DeviceAssessResponse();
        List<Errors> errors = new ArrayList<>();
        Errors errorsIntent = Errors.builder().code("1001").message("Service Error").build();
        errors.add(errorsIntent);
        payload.setErrors(errors);
        payload.setStatusCode(Constants.HTTP_STATUS_OK);
        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateVerifyImeiValidationResponse(payload);
                        });

        assertEquals(HttpStatus.OK.value(), exception.getStatusCode());
        assertEquals(Constants.SERVICE_ERROR.trim(), exception.getMessage().trim());
    }

    @Test
    public void validateImeiPayload_respose_payload_is_null() {

        VerifyImeiPayload payload = new VerifyImeiPayload();
        payload.setValidationType("test");

        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateImeiPayload(payload);
                        });

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals(Constants.ERR_VALIDATION_TYPE_NOT_MATCHING, exception.getMessage());
    }

    @Test
    @SneakyThrows
    public void validateImeiPayload_respose_payload_dateofpurchase_is_null() {

        VerifyImeiPayload payload =
                objectMapper.readValue(
                        new ClassPathResource("/VerifyImeiRequestPayload.json").getFile(),
                        VerifyImeiPayload.class);
        payload.setValidationType("FULL");
        payload.getDeviceDetails().setDateOfPurchase(null);
        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateImeiPayload(payload);
                        });

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals(
                StringUtils.join(Constants.DATE_OF_PURCHASE, Constants.NULL_ERR_MSG),
                exception.getMessage());
    }

    @Test
    @SneakyThrows
    public void validateImeiPayload_respose_payload_typeofproof_is_null() {

        VerifyImeiPayload payload =
                objectMapper.readValue(
                        new ClassPathResource("/VerifyImeiRequestPayload.json").getFile(),
                        VerifyImeiPayload.class);
        payload.setValidationType("FULL");
        payload.getDeviceDetails().setTypeOfProof(null);
        SIBFFException exception =
                assertThrows(
                        SIBFFException.class,
                        () -> {
                            sibffValidator.validateImeiPayload(payload);
                        });

        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals(
                StringUtils.join(Constants.TYPE_OF_PROOF, Constants.NULL_ERR_MSG),
                exception.getMessage());
    }

    @Test
    public void validateHeaders_correlationid_null() {

        headersData =
                HeadersData.builder()
                        .microToken("testMicroToken")
                        .channel("self-serve")
                        .correlationId(null)
                        .build();
        MethodArgumentNotValidException exception =
                assertThrows(
                        MethodArgumentNotValidException.class,
                        () -> {
                            sibffValidator.validateHeaders(headersData);
                        });
        assertEquals(HttpStatus.BAD_REQUEST.value(), exception.getStatusCode());
        assertEquals(Constants.CORRELATION_ID_MISSING, exception.getMessage());
    }
}

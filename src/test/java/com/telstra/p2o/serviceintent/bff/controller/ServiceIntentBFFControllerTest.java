package com.telstra.p2o.serviceintent.bff.controller;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.ADD_DUP;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.X_CORRELATION_ID;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.X_MICRO_TOKEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telstra.p2o.serviceintent.bff.dto.bapi.DeviceAssessResponse;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.CacheRequest;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.VerifyImeiPayload;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.*;
import com.telstra.p2o.serviceintent.bff.exception.CacheDataNotFoundException;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import com.telstra.p2o.serviceintent.bff.service.SIBFFService;
import com.telstra.p2o.serviceintent.bff.validator.SIBFFValidator;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
@AutoConfigureMockMvc
public class ServiceIntentBFFControllerTest {
    @InjectMocks ServiceIntentBFFController serviceIntentBFFController;

    @Mock private SIBFFService sibffService;

    @Mock private RestTemplate restTemplate;
    @Autowired private WebTestClient webTestClient;

    @Mock private SIBFFValidator validator;

    private String correlationId;
    private GetSIPayload payload;

    private HeadersData headersData;

    @Autowired private MockMvc mockMvc;
    Path path;

    ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        correlationId = "123";
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
        payload =
                GetSIPayload.builder()
                        .serviceId("testServiceId")
                        .accountUUID("testAccountId")
                        .build();
        mockMvc = MockMvcBuilders.standaloneSetup(serviceIntentBFFController).build();
    }

    @Test
    public void postFullCAResponseObjectTest() {
        SIBFFResponse response = SIBFFResponse.builder().build();
        lenient()
                .doNothing()
                .when(validator)
                .validateHeaders(any(HeadersData.class), any(GetSIPayload.class));
        lenient()
                .when(sibffService.getMobileServiceIntentDetails(headersData, payload))
                .thenReturn(response);
        ResponseEntity<SIBFFResponse> cabffResponseResponseEntity =
                serviceIntentBFFController.getMobileServiceIntentDetails(
                        correlationId,
                        "gjhgh",
                        "self-serve",
                        "testSourceSystem",
                        "testFlow",
                        payload);
        assertEquals(200, cabffResponseResponseEntity.getStatusCode().value());
    }

    @SneakyThrows
    @Test
    public void cacheBFFResponse() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(X_CORRELATION_ID, headersData.getCorrelationId());
        headers.set(X_MICRO_TOKEN, headersData.getMicroToken());
        lenient().doNothing().when(validator).validateHeaders(any(HeadersData.class));
        CacheRequest cacheRequest = CacheRequest.builder().action(ADD_DUP).build();
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/cache-dup-selection")
                                .content(asJsonString(cacheRequest))
                                .accept(MediaType.APPLICATION_JSON_VALUE)
                                .headers(headers)
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return a success response for service eligibility for Fetch")
    public void getStreamingServiceDetailsSuccessTest() {
        SIBFFResponse response = SIBFFResponse.builder().build();
        lenient().doNothing().when(validator).validateHeaders(any(HeadersData.class));
        lenient()
                .doNothing()
                .when(validator)
                .validateStreamingServiceIntentDetailsRequest(
                        any(HeadersData.class), any(GetSIPayload.class));

        lenient()
                .when(sibffService.getStreamingServiceIntentDetails(headersData))
                .thenReturn(response);
        MultiValueMap<String, String> headersMap = new LinkedMultiValueMap<>();
        headersMap.add(X_CORRELATION_ID, correlationId);
        ResponseEntity<SIBFFResponse> cabffResponseResponseEntity =
                serviceIntentBFFController.getStreamingServiceIntentDetails(
                        correlationId,
                        "gjhgh",
                        "self-serve",
                        "testSourceSystem",
                        "testFlow",
                        payload);
        assertEquals(200, cabffResponseResponseEntity.getStatusCode().value());
    }

    private String asJsonString(Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGetDeviceIntentDetails() throws Exception {
        DeviceIntentResponse.DeviceIntentResponseBuilder builderResult =
                DeviceIntentResponse.builder();
        RedeemingServiceData.RedeemingServiceDataBuilder deviceNameResult =
                RedeemingServiceData.builder().deviceName("Device Name");
        PlanDetails plan = PlanDetails.builder().name("Name").recurringAmount(1L).build();
        RedeemingServiceData redeemingServiceData =
                deviceNameResult.plan(plan).serviceId("42").build();
        DeviceIntentResponse.DeviceIntentResponseBuilder redeemingServiceDataResult =
                builderResult.redeemingServiceData(redeemingServiceData);
        RepaymentOptions repaymentOptions =
                RepaymentOptions.builder()
                        .assetReferenceId("42")
                        .chargePointId("42")
                        .deviceName("Device Name")
                        .installmentsCompleted("Installments Completed")
                        .installmentsLeft("Installments Left")
                        .monthlyRoAmount("10")
                        .remainingPayoutAmount("10")
                        .retailPrice("Retail Price")
                        .status("Status")
                        .totalInstallments("Total Installments")
                        .build();
        DeviceIntentResponse buildResult =
                redeemingServiceDataResult
                        .repaymentOptions(repaymentOptions)
                        .statusCode("Status Code")
                        .success(true)
                        .time("Time")
                        .build();
        when(sibffService.getDeviceIntentDetails(
                        Mockito.<GetSIPayload>any(), Mockito.<HeadersData>any()))
                .thenReturn(buildResult);
        doNothing()
                .when(validator)
                .validateHeaders(Mockito.<HeadersData>any(), Mockito.<GetSIPayload>any());
        doNothing()
                .when(validator)
                .validateServiceId(Mockito.<HeadersData>any(), Mockito.<String>any());

        GetSIPayload getSIPayload = new GetSIPayload();
        getSIPayload.setAccountUUID("01234567-89AB-CDEF-FEDC-BA9876543210");
        getSIPayload.setServiceId("42");
        String content = (new ObjectMapper()).writeValueAsString(getSIPayload);
        MockHttpServletRequestBuilder requestBuilder =
                MockMvcRequestBuilders.post("/get-device-intent-details")
                        .header("X-Correlation-Id", "42")
                        .header("X-Microtoken", "ABC123")
                        .header("X-Channel", "X-Channel")
                        .header("X-Source-System", "X-Source-System")
                        .header("X-Flow-Name", "X-Flow-Name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content);
        MockMvcBuilders.standaloneSetup(serviceIntentBFFController)
                .build()
                .perform(requestBuilder)
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("application/json"))
                .andExpect(
                        MockMvcResultMatchers.content()
                                .string(
                                        "{\"statusCode\":\"Status Code\",\"success\":true,\"time\":\"Time\",\"redeemingServiceData\":{\"serviceId\":\"42\","
                                                + "\"plan\":{\"name\":\"Name\",\"recurringAmount\":1},\"deviceName\":\"Device Name\"},\"repaymentOptions\":{\"assetReferenceId"
                                                + "\":\"42\",\"status\":\"Status\",\"totalInstallments\":\"Total Installments\",\"installmentsCompleted\":\"Installments"
                                                + " Completed\",\"installmentsLeft\":\"Installments Left\",\"deviceName\":\"Device Name\",\"chargePointId\":\"42\","
                                                + "\"monthlyRoAmount\":\"10\",\"remainingPayoutAmount\":\"10\",\"retailPrice\":\"Retail Price\"}}"));
    }

    @Test
    public void testGetDeviceIntentDetails_MissingRequiredHeaders() {
        RestTemplate restTemplateMock = Mockito.mock(RestTemplate.class);

        GetSIPayload payload = new GetSIPayload();
        payload.setAccountUUID("01234567-89AB-CDEF-FEDC-BA9876543210");
        payload.setServiceId("42");

        HttpHeaders headers = new HttpHeaders();
        headers.add(X_MICRO_TOKEN, "ABC123");

        HttpEntity<GetSIPayload> request = new HttpEntity<>(payload, headers);

        Mockito.when(
                        restTemplateMock.exchange(
                                Mockito.anyString(),
                                Mockito.any(HttpMethod.class),
                                Mockito.any(HttpEntity.class),
                                Mockito.eq(DeviceIntentResponse.class)))
                .thenThrow(new RuntimeException("Required header(s) missing"));

        assertThrows(
                RuntimeException.class,
                () ->
                        restTemplateMock.exchange(
                                "/get-device-intent-details",
                                HttpMethod.POST,
                                request,
                                DeviceIntentResponse.class));
    }

    @Test
    public void assessDeviceTest() throws SIBFFException {

        AssessDeviceConditionPayload payload = new AssessDeviceConditionPayload();
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Correlation-Id", "correlationId");
        headers.add("X-Microtoken", "xMicroToken");
        headers.add("X-Channel", "channel");
        headers.add("X-Source-System", "sourceSystem");
        headers.add("X-Flow-Name", "flowName");

        lenient().doNothing().when(validator).validateAssessDeviceRequest(any(), any());
        lenient()
                .when(sibffService.getDeviceAssessDetails(any(), any()))
                .thenReturn(new AssessDeviceResponse());

        ResponseEntity<AssessDeviceResponse> responseEntity =
                serviceIntentBFFController.assessDevice(
                        "correlationId",
                        "xMicroToken",
                        "channel",
                        "sourceSystem",
                        "flowName",
                        payload);

        assertEquals(200, responseEntity.getStatusCodeValue());
    }

    @Test
    @SneakyThrows
    public void verifyImei_success() throws SIBFFException {

        VerifyImeiPayload payload =
                objectMapper.readValue(
                        new ClassPathResource("/VerifyImeiRequestPayload.json").getFile(),
                        VerifyImeiPayload.class);

        DeviceAssessResponse deviceAssessResponse =
                objectMapper.readValue(
                        new ClassPathResource("/DeviceAssessResponse.json").getFile(),
                        DeviceAssessResponse.class);

        lenient().doNothing().when(validator).validateVerifyImeiValidationResponse(any());
        Mockito.when(sibffService.verifyImeiDetails(any(), any())).thenReturn(deviceAssessResponse);

        ResponseEntity<DeviceAssessResponse> responseEntity =
                serviceIntentBFFController.verifyImei(
                        "correlationId",
                        "xMicroToken",
                        "channel",
                        "sourceSystem",
                        "flowName",
                        payload);

        assertEquals(HttpStatus.OK.value(), responseEntity.getStatusCodeValue());
        assertNotNull(responseEntity.getBody().getDeviceAssessment());
        assertEquals(
                "BLACK",
                responseEntity
                        .getBody()
                        .getDeviceAssessment()
                        .get(0)
                        .getDeviceDetails()
                        .getColor());
        assertEquals(
                3,
                responseEntity
                        .getBody()
                        .getDeviceAssessment()
                        .get(0)
                        .getOutcomes()
                        .get(0)
                        .getQuestionnaire()
                        .size());
    }

    @Test
    @SneakyThrows
    public void verifyImei_throw_CacheDataNotFoundException() throws SIBFFException {

        VerifyImeiPayload payload =
                objectMapper.readValue(
                        new ClassPathResource("/VerifyImeiRequestPayload.json").getFile(),
                        VerifyImeiPayload.class);

        lenient().doNothing().when(validator).validateVerifyImeiValidationResponse(any());
        Mockito.when(sibffService.verifyImeiDetails(any(), any()))
                .thenThrow(CacheDataNotFoundException.class);

        Assertions.assertThrows(
                CacheDataNotFoundException.class,
                () ->
                        serviceIntentBFFController.verifyImei(
                                "correlationId",
                                "xMicroToken",
                                "channel",
                                "sourceSystem",
                                "flowName",
                                payload));
    }
}

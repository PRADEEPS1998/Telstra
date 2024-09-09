package com.telstra.p2o.serviceintent.bff.controller;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.X_CHANNEL;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.X_CORRELATION_ID;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.X_FLOW_NAME;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.X_MICRO_TOKEN;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.X_SOURCE_SYSTEM;

import com.telstra.p2o.serviceintent.bff.constant.Constants;
import com.telstra.p2o.serviceintent.bff.dto.bapi.DeviceAssessResponse;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.CacheRequest;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.VerifyImeiPayload;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.*;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import com.telstra.p2o.serviceintent.bff.service.SIBFFService;
import com.telstra.p2o.serviceintent.bff.validator.SIBFFValidator;
import javax.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@Validated
public class ServiceIntentBFFController {

    private static final Logger logger = LoggerFactory.getLogger(ServiceIntentBFFController.class);

    private SIBFFValidator validator;

    private SIBFFService service;

    @Autowired
    public ServiceIntentBFFController(SIBFFValidator sibffValidator, SIBFFService service) {
        this.validator = sibffValidator;
        this.service = service;
    }

    @PostMapping(value = "/get-service-details", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SIBFFResponse> getMobileServiceIntentDetails(
            @RequestHeader(value = X_CORRELATION_ID) String correlationId,
            @RequestHeader(value = X_MICRO_TOKEN) String xMicroToken,
            @RequestHeader(value = X_CHANNEL, required = false) String channel,
            @RequestHeader(value = X_SOURCE_SYSTEM, required = false) String sourceSystem,
            @RequestHeader(value = X_FLOW_NAME, required = false) String flowName,
            @RequestBody GetSIPayload payload) {
        logger.info("correlation id received: {}", correlationId);
        logger.info("channel received: {}", channel);

        var headerData =
                HeadersData.builder()
                        .correlationId(correlationId)
                        .channel(channel)
                        .microToken(xMicroToken)
                        .sourceSystem(sourceSystem)
                        .flowName(flowName)
                        .build();

        validator.validateHeaders(headerData, payload);
        validator.validateServiceId(headerData, payload.getServiceId());
        var response = service.getMobileServiceIntentDetails(headerData, payload);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping(
            value = "/get-streaming-service-intent-details",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SIBFFResponse> getStreamingServiceIntentDetails(
            @RequestHeader(value = X_CORRELATION_ID) String correlationId,
            @RequestHeader(value = X_MICRO_TOKEN) String xMicroToken,
            @RequestHeader(value = X_CHANNEL) String channel,
            @RequestHeader(value = X_SOURCE_SYSTEM) String sourceSystem,
            @RequestHeader(value = X_FLOW_NAME) String flowName,
            @RequestBody GetSIPayload getSIPayload)
            throws SIBFFException {

        var headerData =
                HeadersData.builder()
                        .correlationId(correlationId)
                        .channel(channel)
                        .microToken(xMicroToken)
                        .sourceSystem(sourceSystem)
                        .flowName(flowName)
                        .build();

        validator.validateHeaders(headerData);
        validator.validateStreamingServiceIntentDetailsRequest(headerData, getSIPayload);
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set(Constants.X_CORRELATION_ID, correlationId);

        return ResponseEntity.ok()
                .headers(responseHeaders)
                .body(service.getStreamingServiceIntentDetails(headerData));
    }

    @PostMapping(
            value = "/cache-dup-selection",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CacheBFFResponse> cacheDUPSelection(
            @RequestBody CacheRequest cacheRequest,
            @RequestHeader(value = X_CORRELATION_ID) String correlationId,
            @RequestHeader(value = X_MICRO_TOKEN) String xMicroToken,
            @RequestHeader(value = X_CHANNEL, required = false) String channel,
            @RequestHeader(value = X_SOURCE_SYSTEM, required = false) String sourceSystem,
            @RequestHeader(value = X_FLOW_NAME, required = false) String flowName) {
        var headerData =
                HeadersData.builder()
                        .correlationId(correlationId)
                        .channel(channel)
                        .microToken(xMicroToken)
                        .sourceSystem(sourceSystem)
                        .flowName(flowName)
                        .build();
        validator.validateHeaders(headerData);
        CacheBFFResponse cacheBFFResponse = service.cacheBFFResponse(cacheRequest, headerData);
        return new ResponseEntity<>(cacheBFFResponse, HttpStatus.OK);
    }

    @PostMapping(
            value = "/get-device-intent-details",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeviceIntentResponse> getDeviceIntentDetails(
            @RequestHeader(value = X_CORRELATION_ID) String correlationId,
            @RequestHeader(value = X_MICRO_TOKEN) String xMicroToken,
            @RequestHeader(value = X_CHANNEL) String channel,
            @RequestHeader(value = X_SOURCE_SYSTEM) String sourceSystem,
            @RequestHeader(value = X_FLOW_NAME) String flowName,
            @RequestBody GetSIPayload payload)
            throws SIBFFException {

        var headerData =
                HeadersData.builder()
                        .correlationId(correlationId)
                        .channel(channel)
                        .microToken(xMicroToken)
                        .sourceSystem(sourceSystem)
                        .flowName(flowName)
                        .build();

        validator.validateHeaders(headerData, payload);
        validator.validateServiceId(headerData, payload.getServiceId());
        DeviceIntentResponse deviceIntentDetails =
                service.getDeviceIntentDetails(payload, headerData);
        return new ResponseEntity<>(deviceIntentDetails, HttpStatus.OK);
    }

    @PostMapping(
            value = "/assess-device",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AssessDeviceResponse> assessDevice(
            @RequestHeader(value = X_CORRELATION_ID) String correlationId,
            @RequestHeader(value = X_MICRO_TOKEN) String xMicroToken,
            @RequestHeader(value = X_CHANNEL) String channel,
            @RequestHeader(value = X_SOURCE_SYSTEM) String sourceSystem,
            @RequestHeader(value = X_FLOW_NAME) String flowName,
            @RequestBody AssessDeviceConditionPayload payload)
            throws SIBFFException {

        var headerData =
                HeadersData.builder()
                        .correlationId(correlationId)
                        .channel(channel)
                        .microToken(xMicroToken)
                        .sourceSystem(sourceSystem)
                        .flowName(flowName)
                        .build();

        validator.validateAssessDeviceRequest(headerData, payload);
        AssessDeviceResponse assessDeviceDetails =
                service.getDeviceAssessDetails(payload, headerData);
        return new ResponseEntity<>(assessDeviceDetails, HttpStatus.OK);
    }

    @PostMapping(
            value = "/verify-imei",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DeviceAssessResponse> verifyImei(
            @RequestHeader(value = X_CORRELATION_ID) String correlationId,
            @RequestHeader(value = X_MICRO_TOKEN) String xMicroToken,
            @RequestHeader(value = X_CHANNEL) String channel,
            @RequestHeader(value = X_SOURCE_SYSTEM) String sourceSystem,
            @RequestHeader(value = X_FLOW_NAME) String flowName,
            @Valid @RequestBody VerifyImeiPayload payload)
            throws SIBFFException {

        var headerData =
                HeadersData.builder()
                        .correlationId(correlationId)
                        .channel(channel)
                        .microToken(xMicroToken)
                        .sourceSystem(sourceSystem)
                        .flowName(flowName)
                        .build();

        validator.validateHeaders(headerData);
        validator.validateImeiPayload(payload);
        DeviceAssessResponse deviceAssessResponse = service.verifyImeiDetails(payload, headerData);
        return new ResponseEntity<>(deviceAssessResponse, HttpStatus.OK);
    }
}

package com.telstra.p2o.serviceintent.bff.validator;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.ACCOUNT_UUID_MISSING;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.ASSOCIATED_SERVICE;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.CACHE_DATA_NOT_FOUND;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.CORRELATION_ID_MISSING;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.EMPTY_CI_RESPONSE;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.GENERIC_ERROR;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.MICRO_TOKEN_EMPTY;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.REQUEST_PAYLOAD_EMPTY;

import com.telstra.p2o.common.caching.data.OrderingStateInfo;
import com.telstra.p2o.common.core.exception.MicrotokenValidationException;
import com.telstra.p2o.common.core.validator.MicroTokenValidators;
import com.telstra.p2o.common.core.validator.UserInfoValidator;
import com.telstra.p2o.serviceintent.bff.constant.Constants;
import com.telstra.p2o.serviceintent.bff.dto.bapi.CIBAPIResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.DeviceAssessResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.SIBAPIResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.ServiceIntentGetEligibilityResponse;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.VerifyImeiPayload;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.AssessDeviceConditionPayload;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.GetSIPayload;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.HeadersData;
import com.telstra.p2o.serviceintent.bff.exception.CacheDataNotFoundException;
import com.telstra.p2o.serviceintent.bff.exception.MethodArgumentNotValidException;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import com.telstra.p2o.serviceintent.bff.exception.UnAuthorizedException;
import com.telstra.p2o.serviceintent.bff.helper.SIBFFHelper;
import com.telstra.p2o.serviceintent.bff.util.SIBFFCacheUtil;
import java.util.HashMap;
import java.util.Map;
import jodd.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class SIBFFValidator {

    private MicroTokenValidators microTokenValidators;
    private SIBFFHelper sibffHelper;
    private SIBFFCacheUtil sibffCacheUtil;
    private UserInfoValidator userInfoValidator;

    @Autowired
    public SIBFFValidator(
            MicroTokenValidators microTokenValidators,
            SIBFFHelper sibffHelper,
            SIBFFCacheUtil sibffCacheUtil,
            UserInfoValidator userInfoValidator) {
        this.microTokenValidators = microTokenValidators;
        this.sibffHelper = sibffHelper;
        this.sibffCacheUtil = sibffCacheUtil;
        this.userInfoValidator = userInfoValidator;
    }

    public void validateHeaders(HeadersData headersData, GetSIPayload payload) {
        var microToken = headersData.getMicroToken();
        var correlationId = headersData.getCorrelationId();
        validateBFFControllerRequest(correlationId, payload);
        if (StringUtil.isEmpty(microToken))
            throw new UnAuthorizedException(
                    HttpStatus.UNAUTHORIZED.value(), MICRO_TOKEN_EMPTY, GENERIC_ERROR);
        try {
            Map<String, Object> claims = microTokenValidators.validate(microToken);
            headersData.setClaims(claims);
        } catch (MicrotokenValidationException e) {
            throw new UnAuthorizedException(
                    HttpStatus.UNAUTHORIZED.value(), e.getMessage(), GENERIC_ERROR);
        }
    }

    public void validateHeaders(HeadersData headersData) {
        var microToken = headersData.getMicroToken();
        var correlationId = headersData.getCorrelationId();
        if (StringUtil.isEmpty(microToken))
            throw new UnAuthorizedException(
                    HttpStatus.UNAUTHORIZED.value(), MICRO_TOKEN_EMPTY, GENERIC_ERROR);
        if (StringUtil.isEmpty(correlationId)) {
            throw new MethodArgumentNotValidException(
                    HttpStatus.BAD_REQUEST.value(), CORRELATION_ID_MISSING, GENERIC_ERROR);
        }
        try {
            Map<String, Object> claims = microTokenValidators.validate(microToken);
            headersData.setClaims(claims);
        } catch (MicrotokenValidationException e) {
            throw new UnAuthorizedException(
                    HttpStatus.UNAUTHORIZED.value(), e.getMessage(), GENERIC_ERROR);
        }
    }

    public void validateBFFControllerRequest(final String correlationId, GetSIPayload payload) {
        if (StringUtil.isEmpty(correlationId)) {
            throw new MethodArgumentNotValidException(
                    HttpStatus.BAD_REQUEST.value(), CORRELATION_ID_MISSING, GENERIC_ERROR);
        }
        if (payload != null) {
            if (StringUtil.isEmpty(payload.getAccountUUID()))
                throw new MethodArgumentNotValidException(
                        HttpStatus.BAD_REQUEST.value(),
                        "AccountUUID in input payload is missing",
                        GENERIC_ERROR);
            if (StringUtil.isEmpty(payload.getServiceId()))
                throw new MethodArgumentNotValidException(
                        HttpStatus.BAD_REQUEST.value(),
                        "ServiceID in input payload is missing",
                        GENERIC_ERROR);
        } else {
            throw new MethodArgumentNotValidException(
                    HttpStatus.BAD_REQUEST.value(), "Input payload is empty", GENERIC_ERROR);
        }
    }

    public void validateStreamingServiceIntentDetailsRequest(
            HeadersData headersData, GetSIPayload getSIPayload) {
        if (getSIPayload != null) {
            if (StringUtil.isEmpty(getSIPayload.getAccountUUID())
                    && StringUtil.isEmpty(sibffHelper.getCacheData(headersData).getAccountUUID()))
                throw new MethodArgumentNotValidException(
                        HttpStatus.BAD_REQUEST.value(), ACCOUNT_UUID_MISSING, GENERIC_ERROR);
        } else {
            throw new MethodArgumentNotValidException(
                    HttpStatus.BAD_REQUEST.value(), REQUEST_PAYLOAD_EMPTY, GENERIC_ERROR);
        }
    }

    public void validateCacheData(OrderingStateInfo orderingStateInfo, String cacheKey) {
        if (orderingStateInfo == null) {
            throw new CacheDataNotFoundException(
                    HttpStatus.NOT_FOUND.value(),
                    String.format(CACHE_DATA_NOT_FOUND, cacheKey),
                    GENERIC_ERROR);
        }
    }

    public void validateCIBAPIResponse(CIBAPIResponse ciBAPIResponse) {
        if (ciBAPIResponse == null) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), EMPTY_CI_RESPONSE, GENERIC_ERROR);
        }

        if (!(String.valueOf(HttpStatus.OK.value()).equals(ciBAPIResponse.getStatusCode()))) {
            throw new SIBFFException(
                    Integer.parseInt(ciBAPIResponse.getStatusCode()),
                    "Error Message for customer information from BAPI: "
                            + ciBAPIResponse.getErrors().get(0).getMessage(),
                    GENERIC_ERROR);
        }

        if (CollectionUtils.isEmpty(ciBAPIResponse.getProductGroup())) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "ProductGroup in customer information BAPI response is empty",
                    GENERIC_ERROR);
        }
    }

    public void validateSIBAPIResponse(SIBAPIResponse siBAPIResponse) {
        if (siBAPIResponse == null) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), EMPTY_CI_RESPONSE, GENERIC_ERROR);
        }

        if (!(String.valueOf(HttpStatus.OK.value()).equals(siBAPIResponse.getStatusCode()))) {
            throw new SIBFFException(
                    Integer.parseInt(siBAPIResponse.getStatusCode()),
                    "Error Message for service intent from BAPI: "
                            + siBAPIResponse.getErrors().get(0).getMessage(),
                    GENERIC_ERROR);
        }
    }

    public void validateServiceId(HeadersData headersData, String serviceId) {
        var cacheKey =
                sibffCacheUtil.getCacheKey(
                        headersData.getCorrelationId(),
                        headersData.getClaims(),
                        headersData.getChannel());

        OrderingStateInfo orderingStateInfo = sibffCacheUtil.getCacheData(cacheKey);
        Map<String, Object> map = new HashMap<>();
        map.put(ASSOCIATED_SERVICE, serviceId);
        userInfoValidator.validateCustomerAccountInfo(map, orderingStateInfo);
    }

    public void validateAssessDeviceRequest(
            HeadersData headersData, AssessDeviceConditionPayload payload) {
        validateHeaders(headersData);
        if (payload != null) {
            if (StringUtil.isEmpty(payload.getScreenCondition()))
                throw new MethodArgumentNotValidException(
                        HttpStatus.BAD_REQUEST.value(),
                        "ScreenCondition in input payload is missing",
                        GENERIC_ERROR);
            if (StringUtil.isEmpty(payload.getLiquidCondition()))
                throw new MethodArgumentNotValidException(
                        HttpStatus.BAD_REQUEST.value(),
                        "LiquidCondition in input payload is missing",
                        GENERIC_ERROR);
            if (StringUtil.isEmpty(payload.getBodyCondition()))
                throw new MethodArgumentNotValidException(
                        HttpStatus.BAD_REQUEST.value(),
                        "BodyCondition in input payload is missing",
                        GENERIC_ERROR);
        } else {
            throw new MethodArgumentNotValidException(
                    HttpStatus.BAD_REQUEST.value(), "Input payload is empty", GENERIC_ERROR);
        }
    }

    public void validateImeiPayload(VerifyImeiPayload payload) {
        if (!(Constants.VERIFY_IMEI_PARTIAL.equals(payload.getValidationType())
                || Constants.VERIFY_IMEI_FULL.equals(payload.getValidationType()))) {
            throw new SIBFFException(
                    HttpStatus.BAD_REQUEST.value(),
                    Constants.ERR_VALIDATION_TYPE_NOT_MATCHING,
                    GENERIC_ERROR);
        } else if (Constants.VERIFY_IMEI_FULL.equals(payload.getValidationType())) {
            if (StringUtils.isEmpty(payload.getDeviceDetails().getDateOfPurchase())) {
                throw new SIBFFException(
                        HttpStatus.BAD_REQUEST.value(),
                        StringUtils.join(Constants.DATE_OF_PURCHASE, Constants.NULL_ERR_MSG),
                        GENERIC_ERROR);
            } else if (StringUtils.isEmpty(payload.getDeviceDetails().getTypeOfProof())) {
                throw new SIBFFException(
                        HttpStatus.BAD_REQUEST.value(),
                        StringUtils.join(Constants.TYPE_OF_PROOF, Constants.NULL_ERR_MSG),
                        GENERIC_ERROR);
            }
        }
    }

    public void validateserviceIntentGetEligibilityResponse(
            ServiceIntentGetEligibilityResponse serviceIntentGetEligibilityResponse) {
        if (serviceIntentGetEligibilityResponse == null) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    Constants.EMPTY_REDEMPTION_ELIGIBILITY_RESPONSE,
                    GENERIC_ERROR);
        } else if (HttpStatus.OK.value() != serviceIntentGetEligibilityResponse.getStatusCode()) {
            throw new SIBFFException(
                    serviceIntentGetEligibilityResponse.getStatusCode(),
                    Constants.BAPI_SERIVE_ERROR
                            + (serviceIntentGetEligibilityResponse.getErrors() != null
                                    ? serviceIntentGetEligibilityResponse
                                            .getErrors()
                                            .get(0)
                                            .getMessage()
                                    : Constants.SERVICE_ERROR),
                    GENERIC_ERROR);
        } else if (serviceIntentGetEligibilityResponse.getErrors() != null
                && !serviceIntentGetEligibilityResponse.getErrors().isEmpty()) {
            throw new SIBFFException(
                    serviceIntentGetEligibilityResponse.getStatusCode(),
                    serviceIntentGetEligibilityResponse.getErrors().get(0).getMessage(),
                    GENERIC_ERROR);
        } else if (serviceIntentGetEligibilityResponse.getServices() == null
                || serviceIntentGetEligibilityResponse.getServices().isEmpty()) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    Constants.BAPI_RESPONSE_SERVICE_DETAIL_NULL,
                    GENERIC_ERROR);
        } else if (serviceIntentGetEligibilityResponse.getServices().get(0).getOutcomes() == null
                || serviceIntentGetEligibilityResponse
                        .getServices()
                        .get(0)
                        .getOutcomes()
                        .isEmpty()) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    Constants.ERROR_MESSAGE_OUTCOMES_NOT_NULL,
                    GENERIC_ERROR);
        }
    }

    public void validateVerifyImeiValidationResponse(DeviceAssessResponse deviceAssessResponse) {
        if (deviceAssessResponse == null) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    Constants.EMPTY_VALIDATE_IMEI_RESPONSE,
                    GENERIC_ERROR);
        } else if (HttpStatus.OK.value()
                != Integer.parseInt(deviceAssessResponse.getStatusCode())) {
            throw new SIBFFException(
                    Integer.parseInt(deviceAssessResponse.getStatusCode()),
                    Constants.BAPI_SERIVE_ERROR
                            + (deviceAssessResponse.getErrors() != null
                                    ? deviceAssessResponse.getErrors().get(0).getMessage()
                                    : " Service Error"),
                    GENERIC_ERROR);
        } else if (deviceAssessResponse.getErrors() != null
                && !deviceAssessResponse.getErrors().isEmpty()) {
            throw new SIBFFException(
                    Integer.parseInt(deviceAssessResponse.getStatusCode()),
                    deviceAssessResponse.getErrors().get(0).getMessage(),
                    GENERIC_ERROR);
        }
    }
}

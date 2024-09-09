package com.telstra.p2o.serviceintent.bff.service;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.*;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telstra.p2o.common.caching.data.*;
import com.telstra.p2o.common.caching.data.DeviceDetails;
import com.telstra.p2o.serviceintent.bff.config.SIBFFConfig;
import com.telstra.p2o.serviceintent.bff.constant.CIResponseConstants;
import com.telstra.p2o.serviceintent.bff.constant.CIResponseConstants.PRODUCT_OFFERING_TYPE;
import com.telstra.p2o.serviceintent.bff.constant.Constants;
import com.telstra.p2o.serviceintent.bff.dto.bapi.*;
import com.telstra.p2o.serviceintent.bff.dto.bapi.DeviceAssessmentResponse;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.*;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.AssessDeviceConditionDetails;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.*;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.RepaymentOptions;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import com.telstra.p2o.serviceintent.bff.helper.SIBFFHelper;
import com.telstra.p2o.serviceintent.bff.mapper.SIBFFResponseMapper;
import com.telstra.p2o.serviceintent.bff.util.SIBFFCacheUtil;
import com.telstra.p2o.serviceintent.bff.validator.SIBFFValidator;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import jodd.util.StringUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SIBFFServiceImpl implements SIBFFService {

    private static final Logger log = LoggerFactory.getLogger(SIBFFServiceImpl.class);
    public static final String YYYY_MM_DD_T_HH_MM_SS_SSSZ = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";

    @Autowired private SIBFFValidator validator;

    @Autowired private SIBFFCacheUtil cacheUtil;

    @Autowired private SIBFFConfig config;

    @Autowired private SIBFFResponseMapper sibffResponseMapper;

    @Autowired private SIBFFHelper sibffHelper;

    @Autowired RestTemplate restTemplate;

    @Override
    public SIBFFResponse getMobileServiceIntentDetails(
            HeadersData headersData, GetSIPayload payload) {
        log.info(
                "[SIBFFServiceImpl] getMobileServiceIntentDetails started for: {}",
                headersData.getCorrelationId());

        var orderingStateInfo = getCacheData(headersData);
        OrderingStateInfo newOrderingStateInfo = new OrderingStateInfo();

        var ciBAPIResponse = getCustomerInformation(payload, headersData, orderingStateInfo);
        log.info("[SIBFFServiceImpl] getCustomerInformation BAPI response: {}", ciBAPIResponse);
        validator.validateCIBAPIResponse(ciBAPIResponse);

        var ciResponseParameters =
                sibffHelper.getCIResponseParameters(
                        ciBAPIResponse, orderingStateInfo, newOrderingStateInfo, headersData);
        log.info("[SIBFFServiceImpl] ciResponseParameters: {}", ciResponseParameters);
        var outcome = sibffHelper.getCIOutcome(ciResponseParameters, orderingStateInfo);
        log.info("[SIBFFServiceImpl] getCustomerInformation CI outcome: {}", outcome);
        if (config.getValidFlow().contains(headersData.getChannel())
                && newOrderingStateInfo.getSelectedService() != null) {
            newOrderingStateInfo.getSelectedService().setServiceOutcome(outcome);
        }
        SIBFFResponse response = null;
        RepaymentOptions repaymentOptions = RepaymentOptions.builder().build();
        if (CI_OUTCOME.HARDSTOP.toString().equalsIgnoreCase(outcome)) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "No condition satisfied after Customer Information BAPI call",
                    GENERIC_ERROR);
        } else if ((Boolean.TRUE.equals(
                        orderingStateInfo.getFeatureToggle(ADD_RO_TO_EXISTING_SERVICE)))
                && (CI_OUTCOME
                                .REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO
                                .toString()
                                .equalsIgnoreCase(outcome)
                        || CI_OUTCOME
                                .BUY_PHONE_AND_DISCONNECT_RO
                                .toString()
                                .equalsIgnoreCase(outcome))) {
            log.info(
                    "CI outcome for Add Ro to existing Service: {}, for id: {}",
                    outcome,
                    headersData.getCorrelationId());
            setCartActionCodesForAddRoToExistingService(
                    outcome, orderingStateInfo, newOrderingStateInfo);
            response =
                    getEtcDetails(
                            headersData, payload, ciBAPIResponse, outcome, ciResponseParameters);

        } else if (CI_OUTCOME.SERVICE_MIGRATION.toString().equalsIgnoreCase(outcome)
                || CI_OUTCOME.REDIRECT_TO_AGORA.toString().equalsIgnoreCase(outcome)) {
            var sourceSystem = "";
            if (ciBAPIResponse != null
                    && !(CollectionUtils.isEmpty(ciBAPIResponse.getProductGroup()))) {
                sourceSystem = ciBAPIResponse.getProductGroup().get(0).getSourceSystem();
            }
            log.info("[SIBFFServiceImpl] sourceSystem in CI response: {}", sourceSystem);
            response =
                    processServiceIntent(
                            orderingStateInfo,
                            payload,
                            sourceSystem,
                            headersData,
                            ciResponseParameters,
                            newOrderingStateInfo);

        } else if (CI_OUTCOME.REMOVE_PLAN_AND_BUY_PHONE.toString().equalsIgnoreCase(outcome)
                || (CI_OUTCOME.REDIRECT_TO_MYT.toString().equalsIgnoreCase(outcome)
                        && ciResponseParameters.isROLinked())) {
            response =
                    getEtcDetails(
                            headersData, payload, ciBAPIResponse, outcome, ciResponseParameters);
        } else {

            response =
                    sibffResponseMapper.createSIBFFResponse(
                            ciBAPIResponse, repaymentOptions, outcome, ciResponseParameters, false);
        }

        updatingOrderStateInfo(
                orderingStateInfo,
                newOrderingStateInfo,
                response,
                headersData,
                ciBAPIResponse,
                ciResponseParameters);

        log.info(
                "[SIBFFServiceImpl] SIBFFResponse: {} for id: {}",
                response,
                headersData.getCorrelationId());
        log.info("[SIBFFServiceImpl] getMobileServiceIntentDetails end");
        return response;
    }

    public SIBFFResponse getEtcDetails(
            HeadersData headersData,
            GetSIPayload payload,
            CIBAPIResponse ciBAPIResponse,
            String outcome,
            CIResponseParameters ciResponseParameters) {
        SIBFFResponse response = null;
        RepaymentOptions repaymentOptions = RepaymentOptions.builder().build();
        String assetReferenceId =
                ciResponseParameters.getContractAssetReferenceId() != null
                        ? ciResponseParameters.getContractAssetReferenceId()
                        : "";
        if (!assetReferenceId.isEmpty()) {

            ETCBapiResponse etcBapiResponse =
                    getEtcBAPIResponse(headersData, payload.getAccountUUID(), assetReferenceId);
            log.info("[SIBFFServiceImpl] etcBapiResponse: {}", etcBapiResponse);

            if (nonNull(etcBapiResponse)
                    && nonNull(etcBapiResponse.getStatusCode())
                    && etcBapiResponse.getStatusCode().equals("200")) {
                if (nonNull(etcBapiResponse)
                        && nonNull(etcBapiResponse.getGetETCResponse())
                        && nonNull(etcBapiResponse.getGetETCResponse().getRepaymentOptions())
                        && !etcBapiResponse.getGetETCResponse().getRepaymentOptions().isEmpty()) {
                    repaymentOptions =
                            etcBapiResponse.getGetETCResponse().getRepaymentOptions().get(0);
                }

                response =
                        sibffResponseMapper.createSIBFFResponse(
                                ciBAPIResponse,
                                repaymentOptions,
                                outcome,
                                ciResponseParameters,
                                true);
            } else {
                throw new SIBFFException(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        etcBapiResponse.toString(),
                        etcBapiResponse.getStatusCode());
            }

        } else {
            response =
                    sibffResponseMapper.createSIBFFResponse(
                            ciBAPIResponse, repaymentOptions, outcome, ciResponseParameters, false);
        }
        return response;
    }

    @Override
    public SIBFFResponse getStreamingServiceIntentDetails(HeadersData headersData)
            throws SIBFFException {
        String serviceId = getCacheServiceId(headersData);
        Boolean isNewDeviceAllowed = false;
        String timeStamp = new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS_SSSZ).format(new Date());
        int statusCode = STATUS_CODE_200;
        if (StringUtil.isEmpty(serviceId)) {
            isNewDeviceAllowed = true;
        } else {
            var serviceIntentBAPIResponse =
                    invokeServiceIntentBAPI(headersData, buildFetchServiceIntentRequest(serviceId));
            invokeServiceIntentBAPIException(serviceIntentBAPIResponse.getStatusCode());

            isNewDeviceAllowed =
                    setService(
                            isNewDeviceAllowed, serviceId, serviceIntentBAPIResponse.getServices());
            statusCode = serviceIntentBAPIResponse.getStatusCode();
            timeStamp = serviceIntentBAPIResponse.getTime();
        }

        return SIBFFResponse.builder()
                .statusCode(String.valueOf(statusCode))
                .time(timeStamp)
                .success(true)
                .data(Data.builder().isNewDeviceAllowed(isNewDeviceAllowed).build())
                .build();
    }

    @Override
    public CacheBFFResponse cacheBFFResponse(CacheRequest cacheRequest, HeadersData headersData) {

        List<CartActionCodes> cartActionCodesList = new ArrayList<>();
        CartActionCodes cartActionCodes;
        CacheBFFResponse cacheBFFResponse;
        String cacheKey =
                cacheUtil.getCacheKey(
                        headersData.getCorrelationId(),
                        headersData.getClaims(),
                        headersData.getChannel());
        OrderingStateInfo orderingStateInfo = cacheUtil.getCacheData(cacheKey);
        if (ADD_DUP.equalsIgnoreCase(cacheRequest.getAction())) {
            orderingStateInfo.setDoSCToDUPCarryOver(true);
            var hasDeviceROInCart = getHasDeviceROInCart(orderingStateInfo);
            log.info("[SIBFFServiceImpl] hasDeviceROInCart: {}", hasDeviceROInCart);
            if (hasDeviceROInCart) {
                List<CartActionCodes> listCartActionCodes =
                        orderingStateInfo.getCartActionCodes() != null
                                ? orderingStateInfo.getCartActionCodes()
                                : new ArrayList<>();
                CartActionCodes cartAction = new CartActionCodes();
                cartAction.setActionCode(Constants.CART_ACTION_CODE_OC_CRTRUL_0006);
                cartAction.setActionType(Constants.CART_ACTION_TYPE_ADD_DUP);
                listCartActionCodes.add(cartAction);
                orderingStateInfo.setCartActionCodes(listCartActionCodes);
            }
        } else if (REMOVE_DUP.equalsIgnoreCase(cacheRequest.getAction())) {
            cartActionCodes =
                    CartActionCodes.builder()
                            .actionCode(CART_ACTION_CODE_OC_CRTRUL_0001)
                            .productCode(MBLTDAS_UPPR)
                            .actionType(REMOVE_PRODUCT_FROM_CART)
                            .build();
            log.info("cartActionCodes {}", cacheRequest.getAction() + " : " + cartActionCodes);
            cartActionCodesList.add(cartActionCodes);
            orderingStateInfo.setCartActionCodes(cartActionCodesList);
        } else {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "[SIBFFServiceImpl] Invalid input received for action",
                    GENERIC_ERROR);
        }
        cacheUtil.setCacheData(cacheKey, orderingStateInfo);
        cacheBFFResponse =
                CacheBFFResponse.builder()
                        .statusCode(201)
                        .success(true)
                        .externalId(headersData.getCorrelationId())
                        .time(LocalDateTime.now().toString())
                        .build();
        return cacheBFFResponse;
    }

    @Override
    public DeviceIntentResponse getDeviceIntentDetails(GetSIPayload payload, HeadersData headerData)
            throws SIBFFException {
        log.info(
                "[SIBFFServiceImpl] getDeviceIntentDetails started for: {}",
                headerData.getCorrelationId());
        var orderingStateInfo = getCacheData(headerData);

         if(!orderingStateInfo.isRedeemOrder()){
            throw new SIBFFException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "[SIBFFServiceImpl] DeviceIntentDetails can be pulled for dup redeem flow only",
                GENERIC_ERROR);
        }

        var ciBAPIResponse = getCustomerInformation(payload, headerData, orderingStateInfo);

        log.info("[SIBFFServiceImpl] getCustomerInformation BAPI response: {}", ciBAPIResponse);
        validator.validateCIBAPIResponse(ciBAPIResponse);
        Product planProduct = getPlanProductForService(ciBAPIResponse);

        ETCBapiResponse etcBAPIResponse =
                getEtcBAPIResponse(
                        headerData,
                        payload.getAccountUUID(),
                        orderingStateInfo.getDupROAssetReferenceId());

        List<RepaymentOptions> repaymentOptions = null;
        if (etcBAPIResponse != null
                && "200".equals(etcBAPIResponse.getStatusCode())
                && etcBAPIResponse.getGetETCResponse() != null) {
            repaymentOptions = etcBAPIResponse.getGetETCResponse().getRepaymentOptions();
        } else if (etcBAPIResponse == null) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "[SIBFFServiceImpl] Repayment details not found ",
                    GENERIC_ERROR);
        }
        Product associateHardware = getHardwareForPlan(planProduct);

        stampDeviceIntentDetailsIntoOrderingStateInfo(etcBAPIResponse,headerData);

        return DeviceIntentResponse.builder()
                .time(ciBAPIResponse.getTime())
                .statusCode(ciBAPIResponse.getStatusCode())
                .success(true)
                .redeemingServiceData(
                        RedeemingServiceData.builder()
                                .serviceId(payload.getServiceId())
                                .plan(getPlanDetails(planProduct))
                                .deviceName(getDeviceNameOf(associateHardware))
                                .addons(getAddOnsOf(associateHardware))
                                .build())
                .repaymentOptions(repaymentOptions != null ? repaymentOptions.get(0) : null)
                .build();
    }

    @Override
    public AssessDeviceResponse getDeviceAssessDetails(
            AssessDeviceConditionPayload payload, HeadersData headerData) throws SIBFFException {
        log.info(
                "[SIBFFServiceImpl] getDeviceAssessDetails started for: {}",
                headerData.getCorrelationId());
        OrderingStateInfo orderingStateInfo = getCacheData(headerData);

        DeviceAssessmentResponse assessDeviceResponse =
                invokeAssessDeviceBAPI(headerData, payload, orderingStateInfo);

        return AssessDeviceResponse.builder()
                .statusCode(assessDeviceResponse.getStatusCode())
                .success(true)
                .data(
                        AssessDeviceData.builder()
                                .assessmentResult(
                                        assessDeviceResponse
                                                .getDeviceAssessment()
                                                .get(0)
                                                .getOutcomes()
                                                .get(0)
                                                .getCharacteristics()
                                                .get(0)
                                                .getValue())
                                .redemptionFee(
                                        assessDeviceResponse
                                                .getDeviceAssessment()
                                                .get(0)
                                                .getOutcomes()
                                                .get(0)
                                                .getCharacteristics()
                                                .get(1)
                                                .getValue())
                                .deviceAssessedFor(
                                        AssessDeviceConditionPayload.builder()
                                                .screenCondition(payload.getScreenCondition())
                                                .bodyCondition(payload.getBodyCondition())
                                                .liquidCondition(payload.getLiquidCondition())
                                                .build())
                                .deviceDetails(
                                        AssessDeviceDetails.builder()
                                                .name(
                                                        orderingStateInfo
                                                                .getRedemptionDeviceDetails()
                                                                .getDeviceName())
                                                .recurringAmount(
                                                        orderingStateInfo
                                                                .getRedemptionDeviceDetails()
                                                                .getRecurringAmount())
                                                .build())
                                .build())
                .build();
    }

    List<Addon> getAddOnsOf(Product associateHardware) {
        return associateHardware.getProductRelationship().stream()
                .filter(
                        productRelationship ->
                                ADDON.equalsIgnoreCase(productRelationship.getType()))
                .flatMap(productRelationship -> productRelationship.getProduct().stream())
                .map(
                        product ->
                                Addon.builder()
                                        .name(product.getProductOffering().getName())
                                        .recurringAmount(
                                                product.getProductCharacteristic()
                                                        .getRecurringAmount())
                                        .build())
                .collect(Collectors.toList());
    }

    public String getDeviceNameOf(Product hardware) {
        return hardware.getProductCharacteristic() != null
                ? hardware.getProductCharacteristic().getDeviceName()
                : null;
    }

    public Product getHardwareForPlan(Product planProduct) {
        return planProduct.getProductRelationship().stream()
                .filter(
                        productRelationship ->
                                SERVICE.equalsIgnoreCase(productRelationship.getType()))
                .flatMap(productRelationship -> productRelationship.getProduct().stream())
                .filter(
                        product ->
                                ACTIVE.equalsIgnoreCase(product.getStatus())
                                        && SERVICE.equalsIgnoreCase(
                                                product.getProductOffering() != null
                                                        ? product.getProductOffering().getType()
                                                        : null))
                .flatMap(product -> product.getProductRelationship().stream())
                .filter(
                        productRelationship ->
                                HARDWARE.equalsIgnoreCase(productRelationship.getType()))
                .flatMap(productRelationship -> productRelationship.getProduct().stream())
                .filter(
                        product ->
                                HARDWARE.equalsIgnoreCase(
                                        product.getProductOffering() != null
                                                ? product.getProductOffering().getType()
                                                : null))
                .findFirst()
                .orElseThrow(
                        () ->
                                new SIBFFException(
                                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                        "ProductOffering HARDWARE not found in Plan",
                                        GENERIC_ERROR));
    }

    public PlanDetails getPlanDetails(Product product) {
        return PlanDetails.builder()
                .name(
                        product.getProductOffering() != null
                                ? product.getProductOffering().getName()
                                : null)
                .recurringAmount(
                        product.getProductCharacteristic() != null
                                ? product.getProductCharacteristic().getRecurringAmount()
                                : null)
                .build();
    }

    public Product getPlanProductForService(CIBAPIResponse ciBAPIResponse) {
        return ciBAPIResponse.getProductGroup().stream()
                .flatMap(productGroup -> productGroup.getProduct().stream())
                .filter(isPlanProduct())
                .findFirst()
                .orElseThrow(
                        () ->
                                new SIBFFException(
                                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                        "ProductOffering with type as Plan is not there in customer information BAPI response",
                                        GENERIC_ERROR));
    }

    public static Predicate<Product> isPlanProduct() {
        return product ->
                product.getProductOffering() != null
                        && PRODUCT_OFFERING_TYPE
                                .PLAN
                                .toString()
                                .equalsIgnoreCase(product.getProductOffering().getType());
    }

    private Boolean setService(
            Boolean isNewDeviceAllowed,
            String serviceId,
            List<com.telstra.p2o.serviceintent.bff.dto.bapi.Service> serviceList) {
        for (com.telstra.p2o.serviceintent.bff.dto.bapi.Service service : serviceList) {
            if (service.getBusinessContext() != null
                    && service.getBusinessContext().getServiceId().equals(serviceId)) {
                for (Outcome outcome : service.getOutcomes()) {
                    if (outcome.getOutcomeName().equalsIgnoreCase(AES_ELIGIBILITY)) {
                        isNewDeviceAllowed = outcome.getSuccess();
                    }
                }
            }
        }
        return isNewDeviceAllowed;
    }

    private SIBFFResponse processServiceIntent(
            OrderingStateInfo orderingStateInfo,
            GetSIPayload payload,
            String sourceSystem,
            HeadersData headersData,
            CIResponseParameters ciResponseParameters,
            OrderingStateInfo newOrderingStateInfo) {
        log.info("[SIBFFServiceImpl] processServiceIntent start");
        var siBAPIResponse =
                invokeServiceIntentBAPI(headersData, payload, sourceSystem, orderingStateInfo);

        // billing acc no added
        if (siBAPIResponse != null
                && siBAPIResponse.getSmFeasibilityEligibilityDetails() != null
                && siBAPIResponse.getSmFeasibilityEligibilityDetails().getFeasibilityOutcome()
                        != null) {
            if (siBAPIResponse
                            .getSmFeasibilityEligibilityDetails()
                            .getFeasibilityOutcome()
                            .getBillingAccountId()
                    != null) {
                newOrderingStateInfo.setBillingAccountNumber(
                        siBAPIResponse
                                .getSmFeasibilityEligibilityDetails()
                                .getFeasibilityOutcome()
                                .getBillingAccountId());
            }
            // ServiceMigrationSetting added to cache
            setServiceMigrationSettingsToCache(siBAPIResponse, newOrderingStateInfo);
        }

        log.info("[SIBFFServiceImpl] siBAPIResponse: {}", siBAPIResponse);
        validator.validateSIBAPIResponse(siBAPIResponse);

        var siResponseParameters = sibffHelper.getSIResponseParameters(siBAPIResponse);
        log.info("[SIBFFServiceImpl] siResponseParameters: {}", siResponseParameters);

        var outcome = sibffHelper.getSIOutcome(siResponseParameters);
        log.info("[SIBFFServiceImpl] getServiceIntent outcome: {}", outcome);

        SIBFFResponse response = null;
        if (SI_OUTCOME.HARDSTOP.toString().equalsIgnoreCase(outcome)) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "No condition satisfied after Service Intent BAPI call",
                    GENERIC_ERROR);
        } else {
            response =
                    sibffResponseMapper.createSIBFFResponseForSIOutcome(
                            siBAPIResponse,
                            outcome,
                            siResponseParameters,
                            ciResponseParameters,
                            orderingStateInfo.isHasDUP());
        }

        var isCIMTransferRODevice = siResponseParameters.isCIMTransferRODevice();
        log.info("[SIBFFServiceImpl] isCIMTransferRODevice: {}", headersData.getCorrelationId());
        var hasDeviceROInCart = getHasDeviceROInCart(orderingStateInfo);
        if (isCIMTransferRODevice && hasDeviceROInCart) {

            return SIBFFResponse.builder()
                    .statusCode(String.valueOf(STATUS_CODE_200))
                    .time(new SimpleDateFormat(YYYY_MM_DD_T_HH_MM_SS_SSSZ).format(new Date()))
                    .success(Boolean.TRUE)
                    .action(CIM_TRANSFER_RO_NOT_ALLOWED)
                    .build();
        }
        if (isCIMTransferRODevice) {
            // Update the CartActionCode for isCIMTransferRODevice Scenario
            List<CartActionCodes> listCartActionCodes =
                    orderingStateInfo.getCartActionCodes() != null
                            ? orderingStateInfo.getCartActionCodes()
                            : new ArrayList<>();
            CartActionCodes cartAction = new CartActionCodes();
            cartAction.setActionCode(Constants.CART_ACTION_CODE_OC_CRTRUL_0002);
            cartAction.setActionType(Constants.CART_ACTION_TYPE_ADD_RO_DEVICE);
            listCartActionCodes.add(cartAction);
            newOrderingStateInfo.setCartActionCodes(listCartActionCodes);
        }

        log.info("[SIBFFServiceImpl] SIBFFResponse in processServiceIntent: {}", response);
        log.info("[SIBFFServiceImpl] processServiceIntent end");
        return response;
    }

    private void setCartActionCodesForAddRoToExistingService(
            String outcome,
            OrderingStateInfo orderingStateInfo,
            OrderingStateInfo newOrderingStateInfo) {
        log.info(
                "Inside setCartActionCodesForAddRoToExistingService for id : {}",
                orderingStateInfo.getCorrelationId());
        log.info(
                "Toggle addRoToExistingService value : {} for Id {}",
                orderingStateInfo.getFeatureToggle(ADD_RO_TO_EXISTING_SERVICE),
                orderingStateInfo.getCorrelationId());
        List<CartActionCodes> listCartActionCodes =
                orderingStateInfo.getCartActionCodes() != null
                        ? orderingStateInfo.getCartActionCodes()
                        : new ArrayList<>();
        if ((Boolean.TRUE.equals(orderingStateInfo.getFeatureToggle(ADD_RO_TO_EXISTING_SERVICE)))
                && CI_OUTCOME
                        .REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO
                        .toString()
                        .equalsIgnoreCase(outcome)) {

            CartActionCodes cartActionCode005 = populateCartActionCode005(orderingStateInfo);
            listCartActionCodes.add(cartActionCode005);
            log.info(
                    "Setting up Cart Action Code : {} for id: {}",
                    listCartActionCodes,
                    orderingStateInfo.getCorrelationId());
        } else if ((Boolean.TRUE.equals(
                        orderingStateInfo.getFeatureToggle(ADD_RO_TO_EXISTING_SERVICE)))
                && CI_OUTCOME.BUY_PHONE_AND_DISCONNECT_RO.toString().equalsIgnoreCase(outcome)) {
            CartActionCodes cartActionCode005 = populateCartActionCode005(orderingStateInfo);
            CartActionCodes cartActionCode009 = populateCartActionCode009(orderingStateInfo);

            listCartActionCodes.add(cartActionCode005);
            listCartActionCodes.add(cartActionCode009);
            log.info(
                    "Setting up Cart Action Code : {} for id: {}",
                    listCartActionCodes,
                    orderingStateInfo.getCorrelationId());
        }
        newOrderingStateInfo.setCartActionCodes(listCartActionCodes);
        log.info(
                "newOrderingStateInfo : {} for id {}",
                newOrderingStateInfo.toString(),
                orderingStateInfo.getCorrelationId());
    }

    private CartActionCodes populateCartActionCode005(OrderingStateInfo cacheData) {
        log.info(
                "Entering populateCartActionCode005 correlation Id: {}",
                cacheData.getCorrelationId());
        var cartActionCode005 = new CartActionCodes();
        Optional<ProductOrderItems> hardwareProduct =
                cacheData.getProductOrder().getProductOrderItems().stream()
                        .filter(
                                e ->
                                        e.getProductType().equalsIgnoreCase(HARDWARE)
                                                && e.getProductFamily().equalsIgnoreCase(MOBILES)
                                                && e.getHardwarePurchaseType()
                                                        .equalsIgnoreCase(REPAYMENT))
                        .findFirst();
        if (!hardwareProduct.isEmpty()) {
            cartActionCode005 =
                    CartActionCodes.builder()
                            .actionCode(CART_ACTION_CODE_OC_CRTRUL_0005)
                            .actionType(Constants.LINK_RO)
                            .productSetId(hardwareProduct.get().getSetId())
                            .productSkuVal(hardwareProduct.get().getSkuVal())
                            .productCode(hardwareProduct.get().getProductCode())
                            .build();
        }
        return cartActionCode005;
    }

    private CartActionCodes populateCartActionCode009(OrderingStateInfo cacheData) {
        log.info(
                "Entering populateCartActionCode009 correlation Id: {}",
                cacheData.getCorrelationId());
        var cartActionCode009 = new CartActionCodes();
        Optional<ProductOrderItems> hardwareProduct =
                cacheData.getProductOrder().getProductOrderItems().stream()
                        .filter(
                                e ->
                                        e.getProductType().equalsIgnoreCase(HARDWARE)
                                                && e.getProductFamily().equalsIgnoreCase(MOBILES)
                                                && e.getHardwarePurchaseType()
                                                        .equalsIgnoreCase(REPAYMENT))
                        .findFirst();
        if (!hardwareProduct.isEmpty()) {
            cartActionCode009 =
                    CartActionCodes.builder()
                            .actionCode(CART_ACTION_CODE_OC_CRTRUL_0009)
                            .actionType(DISCONNECT)
                            .productSetId(hardwareProduct.get().getSetId())
                            .productSkuVal(hardwareProduct.get().getSkuVal())
                            .productCode(hardwareProduct.get().getProductCode())
                            .build();
        }
        return cartActionCode009;
    }

    private boolean getHasDeviceROInCart(OrderingStateInfo orderingStateInfo) {

        List<ProductOrderItems> products =
                orderingStateInfo.getProductOrder().getProductOrderItems().stream()
                        .filter(
                                p ->
                                        IsProductTypeMatched(p.getProductType())
                                                && IsHardwarePurchaseTypeMatched(
                                                        p.getHardwarePurchaseType())
                                                && IsProductFamilyMatched(p.getProductFamily()))
                        .collect(Collectors.toList());

        return !products.isEmpty();
    }

    private boolean IsProductTypeMatched(String productType) {
        if (null != productType && !productType.isEmpty()) {
            return productType.equalsIgnoreCase(PRODUCT_TYPE_HARDWARE);
        } else {
            return false;
        }
    }

    private boolean IsHardwarePurchaseTypeMatched(String hardwarePurchaseType) {
        if (null != hardwarePurchaseType && !hardwarePurchaseType.isEmpty()) {
            return hardwarePurchaseType.equalsIgnoreCase(PRODUCT_HARDWARE_PURCHASE_TYPE);
        } else {
            return false;
        }
    }

    private boolean IsProductFamilyMatched(String productFamily) {
        if (null != productFamily && !productFamily.isEmpty()) {
            return productFamily.equalsIgnoreCase(PRODUCT_FAMILY_MOBILES);
        } else {
            return false;
        }
    }

    private OrderingStateInfo getCacheData(HeadersData headersData) {
        var cacheKey =
                cacheUtil.getCacheKey(
                        headersData.getCorrelationId(),
                        headersData.getClaims(),
                        headersData.getChannel());
        log.info("[SIBFFServiceImpl] cache key: {}", cacheKey);
        var orderingStateInfo = cacheUtil.getCacheData(cacheKey);
        log.info("[SIBFFServiceImpl] OrderingStateInfo from cache: {}", orderingStateInfo);

        validator.validateCacheData(orderingStateInfo, cacheKey);
        return orderingStateInfo;
    }

    private CIBAPIResponse getCustomerInformation(
            GetSIPayload payload, HeadersData headersData, OrderingStateInfo orderingStateInfo) {
        try {
            HttpHeaders header = getHttpHeaders(headersData);

            header.set(USER_NAME, orderingStateInfo.getUserName());
            header.set(USER_TYPE, orderingStateInfo.getUserType());

            Map<String, String> params = new HashMap<>();
            params.put(ACCOUNT_UUID, payload.getAccountUUID());
            params.put(SERVICE_ID, payload.getServiceId());

            UriComponentsBuilder uri =
                    UriComponentsBuilder.fromHttpUrl(config.getCustomerInformationURL());
            params.forEach(uri::queryParam);

            var url = uri.encode().toUriString();
            log.info("[SIBFFServiceImpl] GetCustomerInformation URl: {}", url);
            return restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(header), CIBAPIResponse.class)
                    .getBody();
        } catch (Exception e) {
            return handleErrorResponseForCustomerInformation(e);
        }
    }

    private CIBAPIResponse handleErrorResponseForCustomerInformation(Exception e) {
        if (e instanceof HttpStatusCodeException) {
            final HttpStatusCodeException hsce = (HttpStatusCodeException) e;
            var message = hsce.getResponseBodyAsString();
            log.info(
                    "[SIBFFServiceImpl] Error message from customer information BAPI: {}", message);
            CIBAPIResponse errorResponse = getCIBAPIResponse(message);
            log.info("[SIBFFServiceImpl] Customer Information Response: {}", errorResponse);
            return errorResponse;
        } else {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    Constants.ERROR_MESSAGE_FOR_BAPI_CALL + e,
                    GENERIC_ERROR);
        }
    }

    private CIBAPIResponse getCIBAPIResponse(String message) {
        CIBAPIResponse response;
        try {
            var mapper = new ObjectMapper();
            response = mapper.readValue(message, CIBAPIResponse.class);
        } catch (JsonProcessingException ex) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "[SIBFFServiceImpl][getCIBAPIResponse] Error message: " + message,
                    GENERIC_ERROR);
        }
        return response;
    }

    private SIBAPIResponse invokeServiceIntentBAPI(
            HeadersData headersData,
            GetSIPayload payload,
            String sourceSystem,
            OrderingStateInfo orderingStateInfo) {
        try {
            HttpHeaders header = getHttpHeaders(headersData);

            Map<String, String> params = new HashMap<>();
            params.put(SERVICE_ID, payload.getServiceId());
            params.put(LEGACY_SYSTEM, sourceSystem);
            params.put(SOURCE, DEFAULT_SOURCE);
            params.put(CART_CONTEXT_ID, orderingStateInfo.getCartId());

            UriComponentsBuilder uri =
                    UriComponentsBuilder.fromHttpUrl(config.getSmEligibilityDetailsURL());
            params.forEach(uri::queryParam);

            var url = uri.encode().toUriString();
            log.info("[SIBFFServiceImpl] getServiceIntentDetails URl: {}", url);
            return restTemplate
                    .exchange(url, HttpMethod.GET, new HttpEntity<>(header), SIBAPIResponse.class)
                    .getBody();
        } catch (Exception e) {
            return handleErrorResponseForServiceIntent(e);
        }
    }

    private com.telstra.p2o.serviceintent.bff.dto.bapi.Service buildFetchServiceIntentRequest(
            String serviceId) {

        var businessContext =
                BusinessContext.builder()
                        .family(PRODUCT_FAMILY_GENERAL)
                        .type(PRODUCT_TYPE_HARDWARE)
                        .subType(PRODUCT_SUB_TYPE_STREAMING_DEVICES)
                        .serviceId(serviceId)
                        .build();
        var qualifier =
                Qualifier.builder()
                        .name(QUANTITY)
                        .value("1")
                        .valueType(QualifierValueType.Integer)
                        .build();

        return com.telstra.p2o.serviceintent.bff.dto.bapi.Service.builder()
                .qualifiers(Collections.singletonList(qualifier))
                .businessContext(businessContext)
                .build();
    }

    public ServiceIntentGetEligibilityResponse invokeServiceIntentBAPI(
            HeadersData headersData, com.telstra.p2o.serviceintent.bff.dto.bapi.Service service) {
        try {
            HttpHeaders header = getHttpHeaders(headersData);
            header.setContentType(MediaType.APPLICATION_JSON);
            var serviceIntentGetEligibilityRequest =
                    ServiceIntentGetEligibilityRequest.builder()
                            .services(Collections.singletonList(service))
                            .source(headersData.getSourceSystem())
                            .build();

            UriComponentsBuilder uri =
                    UriComponentsBuilder.fromHttpUrl(config.getServiceIntentGetEligibilityURL());
            var url = uri.encode().toUriString();
            return restTemplate
                    .exchange(
                            url,
                            HttpMethod.POST,
                            new HttpEntity<>(serviceIntentGetEligibilityRequest, header),
                            ServiceIntentGetEligibilityResponse.class)
                    .getBody();
        } catch (Exception e) {
            e.printStackTrace();
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    Constants.ERROR_MESSAGE_FOR_BAPI_CALL + e,
                    GENERIC_ERROR);
        }
    }

    private SIBAPIResponse handleErrorResponseForServiceIntent(Exception e) {
        if (e instanceof HttpStatusCodeException) {
            final HttpStatusCodeException hsce = (HttpStatusCodeException) e;
            var message = hsce.getResponseBodyAsString();
            log.info(Constants.ERROR_SERVICE_INTENT, message);
            SIBAPIResponse errorResponse = getSIBAPIResponse(message);
            log.info("[SIBFFServiceImpl] Service Intent Response: {}", errorResponse);
            return errorResponse;
        } else {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Exception from BAPI: " + e,
                    GENERIC_ERROR);
        }
    }

    private SIBAPIResponse getSIBAPIResponse(String errorMessage) {
        SIBAPIResponse response;
        try {
            var mapper = new ObjectMapper();
            response = mapper.readValue(errorMessage, SIBAPIResponse.class);
        } catch (JsonProcessingException ex) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "[SIBFFServiceImpl][getSIBAPIResponse] Error message: " + errorMessage,
                    GENERIC_ERROR);
        }
        return response;
    }

    private HttpHeaders getHttpHeaders(HeadersData headersData) {
        var header = new HttpHeaders();
        header.setContentType(MediaType.APPLICATION_JSON);
        header.set(X_CORRELATION_ID, headersData.getCorrelationId());
        header.set(X_MICRO_TOKEN, headersData.getMicroToken());
        header.set(X_CHANNEL, headersData.getChannel());
        header.set(X_SOURCE_SYSTEM, headersData.getSourceSystem());
        header.set(X_FLOW_NAME, headersData.getFlowName());
        return header;
    }

    private void setServiceMigrationSettingsToCache(
            SIBAPIResponse response, OrderingStateInfo newOrderingStateInfo) {
        ServiceMigrationSettings serviceMigrationSetting = new ServiceMigrationSettings();
        serviceMigrationSetting.setCli(
                response.getSmFeasibilityEligibilityDetails()
                        .getFeasibilityOutcome()
                        .getCallingLineIdentificationType());
        serviceMigrationSetting.setDl(
                response.getSmFeasibilityEligibilityDetails()
                        .getFeasibilityOutcome()
                        .getDirectoryListingIndicator());
        serviceMigrationSetting.setIsTON(
                response.getSmFeasibilityEligibilityDetails().getFeasibilityOutcome().getIsTON());
        serviceMigrationSetting.setMessageBankType(
                response.getSmFeasibilityEligibilityDetails()
                        .getFeasibilityOutcome()
                        .getMessageBankType());
        serviceMigrationSetting.setSimSerialNumber(
                response.getSmFeasibilityEligibilityDetails()
                        .getFeasibilityOutcome()
                        .getSimSerialNumber());
        serviceMigrationSetting.setSimCategory(
                response.getSmFeasibilityEligibilityDetails()
                        .getFeasibilityOutcome()
                        .getSimCategory());
        newOrderingStateInfo.setServiceMigrationSettings(serviceMigrationSetting);
        log.info(
                "[SIBFFServiceImpl] setServiceMigrationSettingsToCache: {}",
                serviceMigrationSetting);
    }

    private void setETCAmountInNewOrderingStateInfo(EtcsAmount etcsAmount, SIBFFResponse response) {
        if (response.getEtcDetails().getEtcsAmount() != null) {
            etcsAmount.setDeviceETC(response.getEtcDetails().getEtcsAmount().getDeviceETC());
            etcsAmount.setOtherETC(response.getEtcDetails().getEtcsAmount().getOtherETC());
            etcsAmount.setPlanETC(response.getEtcDetails().getEtcsAmount().getPlanETC());
            etcsAmount.setTotalETC(response.getEtcDetails().getEtcsAmount().getTotalETC());
        }
    }

    private OrderingStateInfo updatingOrderStateInfo(
            OrderingStateInfo orderingStateInfo,
            OrderingStateInfo newOrderingStateInfo,
            SIBFFResponse response,
            HeadersData headersData,
            CIBAPIResponse ciBAPIResponse,
            CIResponseParameters ciResponseParameters) {
        log.info("orderingStateInfo --->  {}", orderingStateInfo.isDoSCToDUPCarryOver());
        log.info("newOrderingStateInfo --->  {}", newOrderingStateInfo.isDoSCToDUPCarryOver());
        if (orderingStateInfo.isDoSCToDUPCarryOver()) {
            newOrderingStateInfo.setDoSCToDUPCarryOver(orderingStateInfo.isDoSCToDUPCarryOver());
        }
        if (etcDetailsExists(response)) {
            EtcDetails etcDetails = new EtcDetails();
            etcDetails.setHasETC(response.getEtcDetails().getHasETC());
            etcDetails.setIsCIMTransferRODevice(
                    response.getEtcDetails().getIsCIMTransferRODevice());
            EtcsAmount etcsAmount = new EtcsAmount();
            setETCAmountInNewOrderingStateInfo(etcsAmount, response);
            etcDetails.setEtcsAmount(etcsAmount);
            etcDetails.setDeviceDetails(
                    updateDeviceDetailsForCache(response, ciResponseParameters));
            newOrderingStateInfo.setEtcDetails(etcDetails);
        }
        if (orderStateInfoAndProductOrderAndSelectedServiceOutcomeNullCheck(orderingStateInfo)) {
            orderingStateInfo
                    .getProductOrder()
                    .getProductOrderItems()
                    .forEach(
                            productOrderItem -> {
                                if (productOrderItemValidation(
                                        productOrderItem,
                                        PRODUCT_TYPE_SUBSCRIPTION,
                                        PRODUCT_FAMILY_MOBILES)) {
                                    orderingStateInfo.setHasServiceMigration(true);
                                    newOrderingStateInfo.setHasServiceMigration(true);
                                    productOrderItem.setServiceMigrated(true);
                                }
                            });

            newOrderingStateInfo.setProductOrder(orderingStateInfo.getProductOrder());
        }

        log.info("newOrderingStateInfo updated --->  {}", newOrderingStateInfo);
        String cacheKey =
                cacheUtil.getCacheKey(
                        headersData.getCorrelationId(),
                        headersData.getClaims(),
                        headersData.getChannel());
        cacheUtil.setCacheData(cacheKey, newOrderingStateInfo);
        return newOrderingStateInfo;
    }

    private boolean etcDetailsExists(SIBFFResponse response) {
        return (response != null && response.getEtcDetails() != null);
    }

    private boolean orderStateInfoAndProductOrderAndSelectedServiceOutcomeNullCheck(
            OrderingStateInfo orderingStateInfo) {
        return (orderingStateInfo != null
                && orderingStateInfo.getProductOrder() != null
                && (!orderingStateInfo.getProductOrder().getProductOrderItems().isEmpty())
                && getSelectedServiceValidation(orderingStateInfo));
    }

    private boolean getSelectedServiceValidation(OrderingStateInfo orderingStateInfo) {
        return (orderingStateInfo.getSelectedService() != null
                && orderingStateInfo.getSelectedService().getServiceOutcome() != null
                && orderingStateInfo
                        .getSelectedService()
                        .getServiceOutcome()
                        .equalsIgnoreCase(CI_OUTCOME.SERVICE_MIGRATION.name()));
    }

    private boolean productOrderItemValidation(
            ProductOrderItems productOrderItem, String productType, String productFamily) {
        return (productOrderItem != null
                && productOrderItem.getProductType() != null
                && productOrderItem.getProductType().equalsIgnoreCase(productType)
                && productOrderItem.getProductFamily() != null
                && productOrderItem.getProductFamily().equalsIgnoreCase(productFamily));
    }

    private void invokeServiceIntentBAPIException(Integer statusCode) {
        if (statusCode == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                    GENERIC_ERROR);
        }
        if (statusCode == HttpStatus.FAILED_DEPENDENCY.value()) {
            throw new SIBFFException(
                    HttpStatus.FAILED_DEPENDENCY.value(),
                    AES_1006_ERROR_MESSAGE,
                    AES_1006_ERROR_CODE);
        }
    }

    private String getCacheServiceId(HeadersData headersData) {
        return (sibffHelper.getCacheData(headersData) != null
                        && sibffHelper.getCacheData(headersData).getSelectedService() != null
                        && sibffHelper.getCacheData(headersData).getSelectedService().getServiceId()
                                != null)
                ? sibffHelper.getCacheData(headersData).getSelectedService().getServiceId()
                : null;
    }

    private DeviceDetails updateDeviceDetailsForCache(
            SIBFFResponse response, CIResponseParameters ciResponseParameters) {
        DeviceDetails deviceDetails = new DeviceDetails();
        log.info("[SIBFFServiceImpl] updateDeviceDetailsForCache : {}" + response);
        if (response != null
                && response.getEtcDetails() != null
                && response.getEtcDetails().getDeviceDetails() != null) {
            if (ObjectUtils.isNotEmpty(
                    response.getEtcDetails().getDeviceDetails().getNoOfPayments())) {
                deviceDetails.setNoOfPayments(
                        response.getEtcDetails().getDeviceDetails().getNoOfPayments());
            }
            deviceDetails.setDeviceName(
                    response.getEtcDetails().getDeviceDetails().getDeviceName());
            deviceDetails.setBrand(response.getEtcDetails().getDeviceDetails().getBrand());
            deviceDetails.setDeviceBasePrice(
                    response.getEtcDetails().getDeviceDetails().getDeviceBasePrice());
            deviceDetails.setColor(response.getEtcDetails().getDeviceDetails().getColor());
            deviceDetails.setSku(response.getEtcDetails().getDeviceDetails().getSku());
            deviceDetails.setRemainingPayoutAmount(
                    response.getEtcDetails().getDeviceDetails().getRemainingPayoutAmount());
            deviceDetails.setSalesChannel(
                    response.getEtcDetails().getDeviceDetails().getSalesChannel());
            deviceDetails.setPaymentInstallments(
                    response.getEtcDetails().getDeviceDetails().getPaymentInstallments());
            deviceDetails.setStorage(response.getEtcDetails().getDeviceDetails().getStorage());
            deviceDetails.setInstallmentsCompleted(
                    response.getEtcDetails().getDeviceDetails().getInstallmentsCompleted());
            deviceDetails.setDeviceType(
                    response.getEtcDetails().getDeviceDetails().getDeviceType());
            deviceDetails.setSalesChannel(
                    response.getEtcDetails().getDeviceDetails().getSalesChannel());
            deviceDetails.setMROStartDate(
                    response.getEtcDetails().getDeviceDetails().getMROStartDate());
            if (CIResponseConstants.PRODUCT_OFFERING_SUBTYPE
                    .HANDSET
                    .toString()
                    .equalsIgnoreCase(ciResponseParameters.getProductSubType())) {
                deviceDetails.setDeviceRepaymentCode(
                        String.valueOf(
                                CIResponseConstants.PRODUCT_OFFERING_SUBTYPE
                                        .DRT_HANDSET_PRODUCT_CODE));
            } else if (CIResponseConstants.PRODUCT_OFFERING_SUBTYPE
                    .TABLET
                    .toString()
                    .equalsIgnoreCase(ciResponseParameters.getProductSubType())) {
                deviceDetails.setDeviceRepaymentCode(
                        String.valueOf(
                                CIResponseConstants.PRODUCT_OFFERING_SUBTYPE
                                        .DRT_TABLET_PRODUCT_CODE));
            } else if (CIResponseConstants.PRODUCT_OFFERING_SUBTYPE
                    .MODEM
                    .toString()
                    .equalsIgnoreCase(ciResponseParameters.getProductSubType())) {
                deviceDetails.setDeviceRepaymentCode(
                        String.valueOf(
                                CIResponseConstants.PRODUCT_OFFERING_SUBTYPE
                                        .DRT_MODEM_PRODUCT_CODE));
            }

        } else {
            deviceDetails = null;
        }
        return deviceDetails;
    }

    public ETCBapiResponse getEtcBAPIResponse(
            HeadersData headersData, String accountUUID, String assetReferenceId) {

        try {
            HttpHeaders header = getHttpHeaders(headersData);
            header.setContentType(MediaType.APPLICATION_JSON);

            UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(config.getETCUrl());
            uri.queryParam(ACCOUNT_UUID, accountUUID);
            uri.queryParam(ASSET_REFERENCE_ID, assetReferenceId);

            var getEtcUrl = uri.encode().toUriString();
            log.info("getETC BAPI url {}", getEtcUrl);
            return restTemplate
                    .exchange(
                            getEtcUrl,
                            HttpMethod.GET,
                            new HttpEntity<>(header),
                            ETCBapiResponse.class)
                    .getBody();
        } catch (Exception e) {
            return handleErrorResponseForServiceIntentBAPIgetETC(e);
        }
    }

    private ETCBapiResponse handleErrorResponseForServiceIntentBAPIgetETC(Exception e) {
        if (e instanceof HttpStatusCodeException) {
            final HttpStatusCodeException hsce = (HttpStatusCodeException) e;
            var message = hsce.getResponseBodyAsString();
            log.info("[SIBFFServiceImpl] Error message from service intent BAPI: {}", message);
            ETCBapiResponse errorResponse = getETCBAPIErrorResponse(message);
            log.info("[SIBFFServiceImpl] Service intent Response: {}", errorResponse);
            return errorResponse;
        } else {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Exception from BAPI: " + e,
                    GENERIC_ERROR);
        }
    }

    private ETCBapiResponse getETCBAPIErrorResponse(String errorMessage) {
        ETCBapiResponse response;
        try {
            var mapper = new ObjectMapper();
            response = mapper.readValue(errorMessage, ETCBapiResponse.class);
        } catch (JsonProcessingException ex) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "[SIBFFServiceImpl][getETCBAPIResponse] Error message: " + errorMessage,
                    GENERIC_ERROR);
        }
        return response;
    }

    public DeviceAssessmentResponse invokeAssessDeviceBAPI(
            HeadersData headersData,
            AssessDeviceConditionPayload payload,
            OrderingStateInfo orderingStateInfo) {

        HttpHeaders header = getHttpHeaders(headersData);
        header.setContentType(MediaType.APPLICATION_JSON);
        try {
            List<RelatedParty> relatedParty = new ArrayList<>();
            List<DeviceAssessment> deviceAssessment = new ArrayList<>();
            List<Characteristic> characteristics = new ArrayList<>();
            List<DeviceCondition> deviceConditionList = new ArrayList<>();

            Characteristic characteristic =
                    Characteristic.builder()
                            .name(CASE_ID)
                            .value(orderingStateInfo.getRedemptionDeviceDetails().getCaseId())
                            .valueType(CHARACTERISTIC_VALUE_TYPE_STRING)
                            .build();
            characteristics.add(characteristic);

            DeviceCondition screenCondition =
                    DeviceCondition.builder()
                            .name(SCREEN_DAMAGE)
                            .value(screenCondition(payload))
                            .valueType(CHARACTERISTIC_VALUE_TYPE_STRING)
                            .build();
            DeviceCondition bodyCondition =
                    DeviceCondition.builder()
                            .name(BODY_DAMAGE)
                            .value(bodyCondition(payload))
                            .valueType(CHARACTERISTIC_VALUE_TYPE_STRING)
                            .build();
            DeviceCondition liquidCondition =
                    DeviceCondition.builder()
                            .name(LIQUID_DAMAGE)
                            .value(liquidCondition(payload))
                            .valueType("String")
                            .build();

            deviceConditionList.add(screenCondition);
            deviceConditionList.add(bodyCondition);
            deviceConditionList.add(liquidCondition);

            RelatedParty relatedPart =
                    RelatedParty.builder()
                            .id(orderingStateInfo.getAccountUUID())
                            .role(ROLE_ACCOUNT)
                            .type(TYPE_RELATED_PARTY)
                            .referredType(orderingStateInfo.getAccountType())
                            .build();
            relatedParty.add(relatedPart);
            DeviceAssessment deviceAssessments =
                    DeviceAssessment.builder()
                            .assessmentType(AssessmentType.builder().name(GRADING).build())
                            .characteristics(characteristics)
                            .deviceDetails(
                                    AssessDeviceConditionDetails.builder()
                                            .deviceCondition(deviceConditionList)
                                            .build())
                            .build();

            deviceAssessment.add(deviceAssessments);

            var assessDevice =
                    AssessDevice.builder()
                            .serviceId(orderingStateInfo.getSelectedService().getServiceId())
                            .relatedParty(relatedParty)
                            .deviceAssessment(deviceAssessment)
                            .build();

            UriComponentsBuilder uri =
                    UriComponentsBuilder.fromHttpUrl(config.getServiceIntentBAPIUrl());
            var url = uri.encode().toUriString();
            log.info("[SIBFFServiceImpl] getAssessDevice URl: {}", url);

            return restTemplate
                    .exchange(
                            url,
                            HttpMethod.POST,
                            new HttpEntity<>(assessDevice, header),
                            DeviceAssessmentResponse.class)
                    .getBody();
        } catch (Exception e) {
            return handleErrorResponseForAssessDeviceBAPI(e);
        }
    }

    public String bodyCondition(AssessDeviceConditionPayload payload) {
        if (payload.getBodyCondition().equals("AS_NEW")) {
            return "No";
        } else if (payload.getBodyCondition().equals("GENERAL_WEAR_AND_TEAR")) {
            return "No";
        } else {
            return "Yes";
        }
    }

    public String screenCondition(AssessDeviceConditionPayload payload) {
        if (payload.getScreenCondition().equals("NO_CRACKS")) {
            return "No";
        } else {
            return "Yes";
        }
    }

    public String liquidCondition(AssessDeviceConditionPayload payload) {
        if (payload.getLiquidCondition().equals("NO_LIQUID_DAMAGE")) {
            return "No";
        } else {
            return "Yes";
        }
    }

    public DeviceAssessmentResponse handleErrorResponseForAssessDeviceBAPI(Exception e) {
        if (e instanceof HttpStatusCodeException) {
            final HttpStatusCodeException hsce = (HttpStatusCodeException) e;
            var message = hsce.getResponseBodyAsString();
            log.info("[SIBFFServiceImpl] Error message from service intent BAPI: {}", message);
            DeviceAssessmentResponse errorResponse = getAssessDeviceBAPIErrorResponse(message);
            log.info("[SIBFFServiceImpl] Service Intent Response: {}", errorResponse);
            return errorResponse;
        } else {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), GENERIC_ERROR);
        }
    }

    public DeviceAssessmentResponse getAssessDeviceBAPIErrorResponse(String errorMessage) {
        DeviceAssessmentResponse response;
        try {
            var mapper = new ObjectMapper();
            response = mapper.readValue(errorMessage, DeviceAssessmentResponse.class);
        } catch (JsonProcessingException ex) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "[SIBFFServiceImpl][getAssessDeviceBAPIResponse] Error occurred in reading value from jsonString: "
                            + errorMessage,
                    GENERIC_ERROR);
        }
        return response;
    }

    @Override
    public DeviceAssessResponse verifyImeiDetails(
            VerifyImeiPayload payload, HeadersData headerData) {
        /*
        Processing Logic
           -> Get OSI based on correlation Id
           -> Call the bapi getEligibility(redemption response will check outcome status)
              if true
                 ->  invoke device assessment partial/full imei validation based on input validationtype
              else
                -> throw the error
           -> If success, send success response as per response format and with data received from partial/full validation
              |-->  stamp the caseId, deviceId and other device details(from bapi response) details to OSI.
           -> If failure or errors, send error response.
        */
        DeviceAssessResponse deviceAssessResponse = null;
        var orderingStateInfo = getCacheData(headerData);
        if (orderingStateInfo.getSelectedService() == null
                || orderingStateInfo.getSelectedService().getAssetReferenceId() == null
                || orderingStateInfo.getSelectedService().getAssetReferenceId().isEmpty()) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    Constants.ERR_ORDER_STATE_INFO_SELECTED_SERVICE_NOT_NULL,
                    GENERIC_ERROR);
        }
        ServiceIntentGetEligibilityRequest getEligibilityRequest =
                buildServiceIntentGetEligibilityRequest(orderingStateInfo);
        log.info(String.format("serviceIntentGetEligibilityRequest :%s", getEligibilityRequest));

        ServiceIntentGetEligibilityResponse serviceIntentGetEligibilityResponse =
                invokeGetEligibilityAPI(getEligibilityRequest, headerData);

        if (Boolean.FALSE.equals(
                serviceIntentGetEligibilityResponse
                        .getServices()
                        .get(0)
                        .getOutcomes()
                        .get(0)
                        .getSuccess())) {
            throw new SIBFFException(
                    serviceIntentGetEligibilityResponse.getStatusCode(),
                    Constants.REDEEMPTION_ELIGIBILITY_FALSE_ERROR,
                    GENERIC_ERROR);
        } else {
            log.info("payload ValidationType :" + payload.getValidationType());
            AssessDevice imeiValidateRequest;
            if (VERIFY_IMEI_FULL.equals(payload.getValidationType())) {
                imeiValidateRequest =
                        buildFullImeiRequestForDeviceAssessment(payload, orderingStateInfo);
                deviceAssessResponse = invokeDeviceAssessmentAPI(imeiValidateRequest, headerData);
                stampDeviceDetailsIntoOrderStateInfo(deviceAssessResponse, headerData);
            } else if (VERIFY_IMEI_PARTIAL.equals(payload.getValidationType())) {
                imeiValidateRequest =
                        buildPartialImeiRequestForDeviceAssessment(payload, orderingStateInfo);
                deviceAssessResponse = invokeDeviceAssessmentAPI(imeiValidateRequest, headerData);
                stampDeviceDetailsIntoOrderStateInfo(deviceAssessResponse, headerData);
            }
        }
        return deviceAssessResponse;
    }

    public void stampDeviceDetailsIntoOrderStateInfo(
            DeviceAssessResponse deviceAssessResponse, HeadersData headerData) {
        try {
            String cacheKey =
                    cacheUtil.getCacheKey(
                            headerData.getCorrelationId(),
                            headerData.getClaims(),
                            headerData.getChannel());
            OrderingStateInfo neworderingStateInfo = new OrderingStateInfo();
            RedeemingDeviceDetails redeemingDeviceDetails =
                    deviceAssessResponse.getDeviceAssessment().get(0).getDeviceDetails();
            RedemptionDeviceDetails redemptionDeviceDetails = new RedemptionDeviceDetails();
            redemptionDeviceDetails.setDeviceId(redeemingDeviceDetails.getDeviceId());
            redemptionDeviceDetails.setProductId(redeemingDeviceDetails.getProductId());
            redemptionDeviceDetails.setDeviceMake(redeemingDeviceDetails.getDeviceMake());
            redemptionDeviceDetails.setDeviceModel(redeemingDeviceDetails.getDeviceModel());
            redemptionDeviceDetails.setSize(redeemingDeviceDetails.getSize());
            redemptionDeviceDetails.setTelstraDevice(redeemingDeviceDetails.getTelstraDevice());
            redemptionDeviceDetails.setAutoLockCheck(redeemingDeviceDetails.getAutoLockCheck());
            redemptionDeviceDetails.setColor(redeemingDeviceDetails.getColor());

            if (CASE_ID.equals(
                    deviceAssessResponse
                            .getDeviceAssessment()
                            .get(0)
                            .getOutcomes()
                            .get(0)
                            .getCharacteristics()
                            .get(0)
                            .getName())) {
                redemptionDeviceDetails.setCaseId(
                        deviceAssessResponse
                                .getDeviceAssessment()
                                .get(0)
                                .getOutcomes()
                                .get(0)
                                .getCharacteristics()
                                .get(0)
                                .getValue());
            }
            neworderingStateInfo.setRedemptionDeviceDetails(redemptionDeviceDetails);
            cacheUtil.setCacheData(cacheKey, neworderingStateInfo);
            log.info("Stamped device details into orderstateinfo successfully");
        } catch (Exception e) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    Constants.ERROR_STAMPING_ORDER_STATE_INFO + e.getMessage(),
                    GENERIC_ERROR);
        }
    }

    public DeviceAssessResponse invokeDeviceAssessmentAPI(
            AssessDevice imeiValidateRequest, HeadersData headersData) {
        DeviceAssessResponse deviceAssessResponse = null;
        try {
            log.info(String.format("assessDevice:%s", imeiValidateRequest));
            HttpHeaders header = getHttpHeaders(headersData);
            header.setContentType(MediaType.APPLICATION_JSON);
            UriComponentsBuilder uri =
                    UriComponentsBuilder.fromHttpUrl(config.getDeviceAssessment());
            var url = uri.encode().toUriString();
            deviceAssessResponse =
                    restTemplate
                            .exchange(
                                    url,
                                    HttpMethod.POST,
                                    new HttpEntity<>(imeiValidateRequest, header),
                                    DeviceAssessResponse.class)
                            .getBody();
            log.info("deviceAssessResponse :" + deviceAssessResponse);
            validator.validateVerifyImeiValidationResponse(deviceAssessResponse);
        } catch (Exception e) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage(), GENERIC_ERROR);
        }
        return deviceAssessResponse;
    }

    private AssessDevice buildPartialImeiRequestForDeviceAssessment(
            VerifyImeiPayload requestPayload, OrderingStateInfo orderingStateInfo) {
        List<Characteristic> characteristicList = new ArrayList<>();
        List<RelatedParty> relatedPartyList = new ArrayList<>();
        List<DeviceAssessment> deviceAssessmentList = new ArrayList<>();

        RelatedParty relatedParty =
                RelatedParty.builder()
                        .id(orderingStateInfo.getAccountUUID())
                        .role(ROLE_ACCOUNT)
                        .type(TYPE_RELATED_PARTY)
                        .referredType(orderingStateInfo.getType())
                        .build();
        relatedPartyList.add(relatedParty);

        getPartialAndFullImeiCharacterisitcs(orderingStateInfo, characteristicList);

        DeviceAssessment deviceAssessments =
                DeviceAssessment.builder()
                        .assessmentType(
                                AssessmentType.builder()
                                        .name(Constants.VALIDATE_PARTIAL_IMEI)
                                        .build())
                        .characteristics(characteristicList)
                        .deviceDetails(
                                AssessDeviceConditionDetails.builder()
                                        .deviceId(requestPayload.getDeviceDetails().getDeviceId())
                                        .build())
                        .build();

        deviceAssessmentList.add(deviceAssessments);
        return AssessDevice.builder()
                .serviceId(orderingStateInfo.getSelectedService().getAssetReferenceId())
                .relatedParty(relatedPartyList)
                .deviceAssessment(deviceAssessmentList)
                .build();
    }

    private AssessDevice buildFullImeiRequestForDeviceAssessment(
            VerifyImeiPayload requestPayload, OrderingStateInfo orderingStateInfo) {

        List<Characteristic> characteristicList = new ArrayList<>();
        List<RelatedParty> relatedPartyList = new ArrayList<>();
        List<DeviceAssessment> deviceAssessmentList = new ArrayList<>();

        RelatedParty relatedParty =
                RelatedParty.builder()
                        .id(orderingStateInfo.getAccountUUID())
                        .role(ROLE_ACCOUNT)
                        .type(TYPE_RELATED_PARTY)
                        .referredType(orderingStateInfo.getType())
                        .build();
        relatedPartyList.add(relatedParty);

        getPartialAndFullImeiCharacterisitcs(orderingStateInfo, characteristicList);

        Characteristic dateOfProofCharacteristic =
                Characteristic.builder()
                        .name(Constants.CHARACTERISTIC_NAME_DATE_OF_PURCHASE)
                        .value(requestPayload.getDeviceDetails().getDateOfPurchase())
                        .valueType(Constants.CHARACTERISTIC_VALUE_TYPE_DATE)
                        .build();

        Characteristic typeOfProofCharacteristic =
                Characteristic.builder()
                        .name(CHARACTERISTIC_NAME_DATE_OF_TYPE_OF_PROOF)
                        .value(requestPayload.getDeviceDetails().getTypeOfProof())
                        .valueType(CHARACTERISTIC_VALUE_TYPE_STRING)
                        .build();

        characteristicList.add(typeOfProofCharacteristic);
        characteristicList.add(dateOfProofCharacteristic);

        DeviceAssessment deviceAssessments =
                DeviceAssessment.builder()
                        .assessmentType(
                                AssessmentType.builder().name(Constants.VALIDATE_FULL_IMEI).build())
                        .characteristics(characteristicList)
                        .deviceDetails(
                                AssessDeviceConditionDetails.builder()
                                        .deviceId(requestPayload.getDeviceDetails().getDeviceId())
                                        .build())
                        .build();

        deviceAssessmentList.add(deviceAssessments);
        return AssessDevice.builder()
                .serviceId(orderingStateInfo.getSelectedService().getAssetReferenceId())
                .relatedParty(relatedPartyList)
                .deviceAssessment(deviceAssessmentList)
                .build();
    }

    public List<Characteristic> getPartialAndFullImeiCharacterisitcs(
            OrderingStateInfo orderingStateInfo, List<Characteristic> characteristicList) {

        Characteristic agentIdCharacteristic =
                Characteristic.builder()
                        .name(CHARACTERISTIC_NAME_AGENTID)
                        .value(orderingStateInfo.getAgentCode())
                        .valueType(CHARACTERISTIC_VALUE_TYPE_STRING)
                        .build();

        Characteristic premiseCodeCharacteristic =
                Characteristic.builder()
                        .name(CHARACTERISTIC_NAME_PREMISECODE)
                        .value(orderingStateInfo.getAgentPremiseCode())
                        .valueType(CHARACTERISTIC_VALUE_TYPE_STRING)
                        .build();

        characteristicList.add(agentIdCharacteristic);
        characteristicList.add(premiseCodeCharacteristic);
        return characteristicList;
    }

    private ServiceIntentGetEligibilityRequest buildServiceIntentGetEligibilityRequest(
            OrderingStateInfo orderingStateInfo) {

        BusinessContext businessContext =
                BusinessContext.builder()
                        .family(Constants.PRODUCT_FAMILY_MOBILE)
                        .type(Constants.PRODUCT_TYPE_SERVICE)
                        .subType(Constants.PRODUCT_FAMILY_MOBILE)
                        .businessAction(Constants.REDEEMPTION_ELIGIBILITY)
                        .serviceId(orderingStateInfo.getSelectedService().getAssetReferenceId())
                        .build();
        com.telstra.p2o.serviceintent.bff.dto.bapi.Service service =
                com.telstra.p2o.serviceintent.bff.dto.bapi.Service.builder()
                        .businessContext(businessContext)
                        .build();
        List<com.telstra.p2o.serviceintent.bff.dto.bapi.Service> serviceList = List.of(service);
        return ServiceIntentGetEligibilityRequest.builder()
                .source(orderingStateInfo.getServiceSourceSystem())
                .services(serviceList)
                .build();
    }

    public ServiceIntentGetEligibilityResponse invokeGetEligibilityAPI(
            ServiceIntentGetEligibilityRequest eligibilityRequest, HeadersData headersData) {
        ServiceIntentGetEligibilityResponse serviceIntentGetEligibilityResponse;
        try {
            log.info(String.format("eligibilityRequest:%s", eligibilityRequest));
            HttpHeaders header = getHttpHeaders(headersData);
            header.setContentType(MediaType.APPLICATION_JSON);
            UriComponentsBuilder uri =
                    UriComponentsBuilder.fromHttpUrl(config.getServiceIntentGetEligibilityURL());
            var url = uri.encode().toUriString();
            log.info("url :" + url);
            serviceIntentGetEligibilityResponse =
                    restTemplate
                            .exchange(
                                    url,
                                    HttpMethod.POST,
                                    new HttpEntity<>(eligibilityRequest, header),
                                    ServiceIntentGetEligibilityResponse.class)
                            .getBody();
            log.info(
                    String.format(
                            "serviceIntentGetEligibilityResponse :"
                                    + serviceIntentGetEligibilityResponse));
            validator.validateserviceIntentGetEligibilityResponse(
                    serviceIntentGetEligibilityResponse);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    Constants.GET_ELIGIBILITY_PROCESSING_ERROR + e.getMessage(),
                    GENERIC_ERROR);
        }
        return serviceIntentGetEligibilityResponse;
    }

    public void stampDeviceIntentDetailsIntoOrderingStateInfo(ETCBapiResponse response, HeadersData headersData) {

        try{
        OrderingStateInfo newOrderingStateInfo = new OrderingStateInfo();

            String cacheKey =
                    cacheUtil.getCacheKey(
                            headersData.getCorrelationId(),
                            headersData.getClaims(),
                            headersData.getChannel());
        EtcDetails etcDetails  = new EtcDetails();
        etcDetails.setHasETC(true);

        List<com.telstra.p2o.common.caching.data.RepaymentOptions> repaymentOptions = new ArrayList<>();

        com.telstra.p2o.common.caching.data.RepaymentOptions repaymentOption= new com.telstra.p2o.common.caching.data.RepaymentOptions();

        repaymentOption.setAssetReferenceId(response.getGetETCResponse().getRepaymentOptions().get(0).getAssetReferenceId());
        repaymentOption.setStatus(response.getGetETCResponse().getRepaymentOptions().get(0).getStatus());
        repaymentOption.setTotalInstallments(response.getGetETCResponse().getRepaymentOptions().get(0).getTotalInstallments());
        repaymentOption.setTotalInstallments(response.getGetETCResponse().getRepaymentOptions().get(0).getTotalInstallments());
        repaymentOption.setInstallmentsCompleted(response.getGetETCResponse().getRepaymentOptions().get(0).getInstallmentsCompleted());
        repaymentOption.setInstallmentsLeft(response.getGetETCResponse().getRepaymentOptions().get(0).getInstallmentsLeft());
        repaymentOption.setDeviceName(response.getGetETCResponse().getRepaymentOptions().get(0).getDeviceName());
        repaymentOption.setChargePointId(response.getGetETCResponse().getRepaymentOptions().get(0).getChargePointId());
        repaymentOption.setMonthlyRoAmount(response.getGetETCResponse().getRepaymentOptions().get(0).getMonthlyRoAmount());
        repaymentOption.setRemainingPayoutAmount(response.getGetETCResponse().getRepaymentOptions().get(0).getRemainingPayoutAmount());
        repaymentOption.setRetailPrice(response.getGetETCResponse().getRepaymentOptions().get(0).getRetailPrice());

        repaymentOptions.add(repaymentOption);

        newOrderingStateInfo.getRedemptionDeviceDetails().setExistingEtc(repaymentOptions);

        log.info("newOrderingStateInfo updated --->  {}", newOrderingStateInfo);

        cacheUtil.setCacheData(cacheKey, newOrderingStateInfo);
            log.info("Stamped device intent details into orderstateinfo successfully");
    } catch (Exception e) {
        throw new SIBFFException(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                Constants.ERROR_STAMPING_ORDER_STATE_INFO + e.getMessage(),
                GENERIC_ERROR);
    }
    }
}
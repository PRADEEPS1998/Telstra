package com.telstra.p2o.serviceintent.bff.helper;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.*;
import static java.util.Objects.nonNull;

import com.telstra.p2o.common.caching.data.OrderingStateInfo;
import com.telstra.p2o.common.caching.data.ProductOrder;
import com.telstra.p2o.common.caching.data.ProductOrderItems;
import com.telstra.p2o.common.caching.data.SelectedService;
import com.telstra.p2o.serviceintent.bff.config.SIBFFConfig;
import com.telstra.p2o.serviceintent.bff.constant.CIResponseConstants;
import com.telstra.p2o.serviceintent.bff.constant.CIResponseConstants.PRODUCT_OFFERING_SUBTYPE;
import com.telstra.p2o.serviceintent.bff.constant.CIResponseConstants.PRODUCT_OFFERING_TYPE;
import com.telstra.p2o.serviceintent.bff.constant.CIResponseConstants.PRODUCT_STATUS;
import com.telstra.p2o.serviceintent.bff.constant.CIResponseConstants.SOURCE_SYSTEMS;
import com.telstra.p2o.serviceintent.bff.dto.bapi.CIBAPIResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.CIResponseParameters;
import com.telstra.p2o.serviceintent.bff.dto.bapi.Product;
import com.telstra.p2o.serviceintent.bff.dto.bapi.ProductGroup;
import com.telstra.p2o.serviceintent.bff.dto.bapi.ProductRelationship;
import com.telstra.p2o.serviceintent.bff.dto.bapi.SIBAPIResponse;
import com.telstra.p2o.serviceintent.bff.dto.bapi.SIResponseParameters;
import com.telstra.p2o.serviceintent.bff.dto.bapi.SMFeasibilityEligibilityDetails;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.HeadersData;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import com.telstra.p2o.serviceintent.bff.util.SIBFFCacheUtil;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
public class SIBFFHelper {

    private static final Logger logger = LoggerFactory.getLogger(SIBFFHelper.class);
    @Autowired private SIBFFCacheUtil cacheUtil;

    @Autowired private SIBFFConfig sibffConfig;

    private final List<String> validProductOfferingSubTypes =
            List.of(
                    PRODUCT_OFFERING_SUBTYPE.HANDSET.toString(),
                    PRODUCT_OFFERING_SUBTYPE.MODEM.toString(),
                    PRODUCT_OFFERING_SUBTYPE.TABLET.toString());

    public CIResponseParameters getCIResponseParameters(
            CIBAPIResponse ciBAPIResponse,
            OrderingStateInfo orderingStateInfo,
            OrderingStateInfo newOrderingStateInfo,
            HeadersData headersData) {
        CIResponseParameters ciResponseParameters = new CIResponseParameters();
        List<ProductGroup> productGroupList = ciBAPIResponse.getProductGroup();
        ProductGroup productGroup = null;
        Product planProduct = null;
        logger.info("[SIBFFHelper] ciResponseParameters: {}", ciResponseParameters);
        for (ProductGroup productGroupFromResponse : productGroupList) {
            productGroup = productGroupFromResponse;
            for (Product product : productGroupFromResponse.getProduct()) {
                if (product.getProductOffering() != null
                        && PRODUCT_OFFERING_TYPE
                                .PLAN
                                .toString()
                                .equalsIgnoreCase(product.getProductOffering().getType())) {
                    planProduct = product;
                    logger.info("[SIBFFHelper] planProduct: {}", planProduct);
                }
            }
        }

        if (planProduct == null) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "ProductOffering with type as Plan is not there in customer information BAPI response",
                    GENERIC_ERROR);
        }

        setAssetReferenceId(ciResponseParameters, productGroup, planProduct);
        setDeviceName(ciResponseParameters, productGroup, planProduct);
        ciResponseParameters.setSourceSystem(productGroup.getSourceSystem());
        if (planProduct.getProductOffering() != null)
            ciResponseParameters.setExistingPlanName(planProduct.getProductOffering().getName());
        if (planProduct.getProductCharacteristic() != null)
            ciResponseParameters.setPaymentMode(
                    planProduct.getProductCharacteristic().getPaymentMode());

        ciResponseParameters.setDavinci(
                SOURCE_SYSTEMS
                        .B2CFORCE
                        .toString()
                        .equalsIgnoreCase(productGroup.getSourceSystem()));
        ciResponseParameters.setMICA(
                SOURCE_SYSTEMS.MICA.toString().equalsIgnoreCase(productGroup.getSourceSystem()));
        ciResponseParameters.setPrepaidService(
                PREPAID.equalsIgnoreCase(ciResponseParameters.getPaymentMode()));

        ciResponseParameters.setPlanInCart(orderingStateInfo.isHasPlan());
        if (orderingStateInfo.getProductOrder() != null
                && !ObjectUtils.isEmpty(
                        orderingStateInfo.getProductOrder().getProductOrderItems())) {
            orderingStateInfo
                    .getProductOrder()
                    .getProductOrderItems()
                    .forEach(
                            product -> {
                                if (!ObjectUtils.isEmpty(product.getProductFamily())
                                        && !ObjectUtils.isEmpty(product.getProductType())
                                        && product.getProductFamily()
                                                .equalsIgnoreCase(PRODUCT_FAMILY_MOBILES)
                                        && product.getProductType()
                                                .equalsIgnoreCase(PRODUCT_TYPE_HARDWARE)) {
                                    ciResponseParameters.setDeviceInCart(Boolean.TRUE);
                                    if (!StringUtils.isEmpty(product.getHardwarePurchaseType())
                                            && product.getHardwarePurchaseType()
                                                    .equalsIgnoreCase(REPAYMENT)) {
                                        ciResponseParameters.setDeviceROInCart(Boolean.TRUE);
                                    }
                                }
                            });
        }
        ProductOrder productOrder = orderingStateInfo.getProductOrder();

        for (ProductOrderItems productOrderItems : productOrder.getProductOrderItems()) {
            if (SUBSCRIPTION.equalsIgnoreCase(productOrderItems.getProductType()))
                ciResponseParameters.setSelectedPlanName(productOrderItems.getProductName());
        }

        if (sibffConfig.getValidFlow().contains(headersData.getChannel())) {
            SelectedService selectedService = orderingStateInfo.getSelectedService();
            if (selectedService == null) selectedService = SelectedService.builder().build();
            selectedService.setSourceSystem(productGroup.getSourceSystem());
            logger.info("[SIBFFHelper] SourceSystem : {}", productGroup.getSourceSystem());
            if (planProduct.getProductCharacteristic() != null)
                selectedService.setPaymentMode(
                        planProduct.getProductCharacteristic().getPaymentMode());
            newOrderingStateInfo.setSelectedService(selectedService);
            logger.info("[SIBFFHelper] selectedService(): {}", selectedService);
            if (planProduct.getProductOffering() != null
                    && PRODUCT_OFFERING_ID.equalsIgnoreCase(
                            planProduct.getProductOffering().getId())) {
                newOrderingStateInfo.setHasDUPMigrate(true);
            }
        }
        logger.info("[SIBFFHelper] ciResponseParameters : {}", ciResponseParameters);
        return ciResponseParameters;
    }

    private void setAssetReferenceId(
            CIResponseParameters ciResponseParameters,
            ProductGroup productGroup,
            Product planProduct) {

        if (SOURCE_SYSTEMS.B2CFORCE.toString().equalsIgnoreCase(productGroup.getSourceSystem())
                && PRODUCT_STATUS.ACTIVE.toString().equalsIgnoreCase(planProduct.getStatus())) {

            if (!getProductRelationship(planProduct).isEmpty()) {
                for (ProductRelationship productRelationship :
                        getProductRelationship(planProduct)) {
                    if (!productRelationship.getProduct().isEmpty()) {
                        for (Product serviceProduct : productRelationship.getProduct()) {
                            if (!getProductRelationship(serviceProduct).isEmpty()
                                    && serviceProduct.getProductOffering() != null
                                    && PRODUCT_STATUS
                                            .ACTIVE
                                            .toString()
                                            .equalsIgnoreCase(serviceProduct.getStatus())
                                    && PRODUCT_OFFERING_TYPE
                                            .SERVICE
                                            .toString()
                                            .equalsIgnoreCase(
                                                    serviceProduct
                                                            .getProductOffering()
                                                            .getType())) {
                                for (ProductRelationship productRelationship1 :
                                        getProductRelationship(serviceProduct)) {
                                    if (!productRelationship1.getProduct().isEmpty()) {
                                        for (Product hardwareProduct :
                                                productRelationship1.getProduct()) {
                                            if (!getProductRelationship(hardwareProduct).isEmpty()
                                                    && hardwareProduct.getProductOffering() != null
                                                    && hardwareProduct
                                                                    .getProductOffering()
                                                                    .getType()
                                                            != null
                                                    && PRODUCT_STATUS
                                                            .ACTIVE
                                                            .toString()
                                                            .equalsIgnoreCase(
                                                                    hardwareProduct.getStatus())
                                                    && PRODUCT_OFFERING_TYPE
                                                            .HARDWARE
                                                            .toString()
                                                            .equalsIgnoreCase(
                                                                    hardwareProduct
                                                                            .getProductOffering()
                                                                            .getType())) {
                                                for (ProductRelationship productRelationship2 :
                                                        getProductRelationship(hardwareProduct)) {
                                                    if (!productRelationship2.getProduct().isEmpty()
                                                            && CIResponseConstants
                                                                    .PRODUCT_RELATIONSHIP_TYPE
                                                                    .CONTRACT
                                                                    .toString()
                                                                    .equalsIgnoreCase(
                                                                            productRelationship2
                                                                                    .getType())) {
                                                        for (Product contractProduct :
                                                                productRelationship2.getProduct()) {
                                                            if (contractProduct.getProductOffering()
                                                                            != null
                                                                    && contractProduct
                                                                                    .getProductOffering()
                                                                                    .getType()
                                                                            != null
                                                                    && contractProduct
                                                                                    .getProductOffering()
                                                                                    .getSubtype()
                                                                            != null
                                                                    && PRODUCT_STATUS
                                                                            .ACTIVE
                                                                            .toString()
                                                                            .equalsIgnoreCase(
                                                                                    contractProduct
                                                                                            .getStatus())
                                                                    && PRODUCT_OFFERING_TYPE
                                                                            .CONTRACT
                                                                            .toString()
                                                                            .equalsIgnoreCase(
                                                                                    contractProduct
                                                                                            .getProductOffering()
                                                                                            .getType())
                                                                    && PRODUCT_OFFERING_SUBTYPE
                                                                            .DPC
                                                                            .toString()
                                                                            .equalsIgnoreCase(
                                                                                    contractProduct
                                                                                            .getProductOffering()
                                                                                            .getSubtype())) {
                                                                ciResponseParameters
                                                                        .setContractAssetReferenceId(
                                                                                contractProduct
                                                                                        .getId());
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
                    }
                }
            }
        }
    }

    private void setDeviceName(
            CIResponseParameters ciResponseParameters,
            ProductGroup productGroup,
            Product planProduct) {
        Product deviceProduct;
        Product dupProduct;
        for (ProductRelationship productRelationship : getProductRelationship(planProduct)) {
            for (Product product : productRelationship.getProduct()) {
                if (product.getProductOffering() != null
                        && PRODUCT_STATUS.ACTIVE.toString().equalsIgnoreCase(product.getStatus())
                        && PRODUCT_OFFERING_TYPE
                                .HARDWARE
                                .toString()
                                .equalsIgnoreCase(product.getProductOffering().getType())
                        && validProductOfferingSubTypes.contains(
                                product.getProductOffering().getSubtype())
                        && ObjectUtils.isNotEmpty(product.getProductRelationship())
                        && product.getProductRelationship().stream()
                                .anyMatch(
                                        p ->
                                                ObjectUtils.isNotEmpty(p.getProduct())
                                                        && p.getProduct().stream()
                                                                .anyMatch(
                                                                        offer ->
                                                                                CONTRACT
                                                                                                .equalsIgnoreCase(
                                                                                                        offer.getProductOffering()
                                                                                                                .getType())
                                                                                        && DPC
                                                                                                .equalsIgnoreCase(
                                                                                                        offer.getProductOffering()
                                                                                                                .getSubtype())))) {
                    ciResponseParameters.setROLinked(true);
                    deviceProduct = product;
                    ciResponseParameters.setDeviceName(
                            deviceProduct.getProductOffering().getName());
                    ciResponseParameters.setProductSubType(
                            product.getProductOffering().getSubtype());
                }
                dupProduct = getDUPProduct(productGroup, product);
                logger.info("[SIBFFHelper] dupProduct: {}", dupProduct);
            }
        }
    }

    private List<ProductRelationship> getProductRelationship(Product planProduct) {
        List<ProductRelationship> productRelationshipList = new ArrayList<>();
        if (!(CollectionUtils.isEmpty(planProduct.getProductRelationship()))) {
            productRelationshipList.addAll(planProduct.getProductRelationship());
            for (ProductRelationship relationShip : planProduct.getProductRelationship()) {
                for (Product product : relationShip.getProduct()) {
                    var productRelationship = getProductRelationship(product);
                    if (!(CollectionUtils.isEmpty(productRelationship)))
                        productRelationshipList.addAll(productRelationship);
                }
            }
        }
        return productRelationshipList;
    }

    public Product getDUPProduct(ProductGroup productGroup, Product product) {
        Product dupProduct = null;
        if (PRODUCT_STATUS.ACTIVE.toString().equalsIgnoreCase(product.getStatus())
                && PRODUCT_OFFERING_TYPE
                        .DISCOUNT
                        .toString()
                        .equalsIgnoreCase(product.getProductOffering().getType())
                && ((validProductOfferingSubTypes.contains(
                                        product.getProductOffering().getSubtype())
                                && XC001016739.equalsIgnoreCase(
                                        product.getProductOffering().getId()))
                        || (PRODUCT_OFFERING_SUBTYPE
                                        .NPFT
                                        .toString()
                                        .equalsIgnoreCase(product.getProductOffering().getSubtype())
                                && XC001005417.equalsIgnoreCase(
                                        product.getProductOffering().getId())))
                && !SOURCE_SYSTEMS
                        .B2CFORCE
                        .toString()
                        .equalsIgnoreCase(productGroup.getSourceSystem())) {
            dupProduct =
                    product; // This is added to enable future changes for dup product if and when
            // required
        }
        return dupProduct;
    }

    public String getCIOutcome(
            CIResponseParameters ciResponseParameters, OrderingStateInfo orderingStateInfo) {
        if (ciResponseParameters == null) return CI_OUTCOME.HARDSTOP.toString();
        if (Boolean.TRUE.equals(orderingStateInfo.getFeatureToggle(ADD_RO_TO_EXISTING_SERVICE))
                && nonNull(orderingStateInfo)
                && nonNull(orderingStateInfo.getFlowAction())
                && orderingStateInfo.getFlowAction().toLowerCase().contains(ADD_RO)
                && ciResponseParameters.isDavinci()
                && !ciResponseParameters.isPrepaidService()
                && ciResponseParameters.isROLinked()
                && !ciResponseParameters.isPlanInCart()
                && ciResponseParameters.isDeviceInCart()
                && ciResponseParameters.isDeviceROInCart())
            return CI_OUTCOME.BUY_PHONE_AND_DISCONNECT_RO.toString();
        if (!ciResponseParameters.isDavinci()
                && !ciResponseParameters.isPrepaidService()
                && ciResponseParameters.isPlanInCart())
            return CI_OUTCOME.SERVICE_MIGRATION.toString();
        if (!ciResponseParameters.isDavinci() && ciResponseParameters.isPrepaidService())
            return CI_OUTCOME.REDIRECT_TO_AGORA.toString();
        if (ciResponseParameters.isDavinci()
                && !ciResponseParameters.isPrepaidService()
                && ciResponseParameters.isPlanInCart()
                && !ciResponseParameters.isDeviceInCart()
                && !ciResponseParameters.isDeviceROInCart())
            return CI_OUTCOME.REDIRECT_TO_MYT.toString();
        if (Boolean.TRUE.equals(orderingStateInfo.getFeatureToggle(ADD_RO_TO_EXISTING_SERVICE))
                && ciResponseParameters.isDavinci()
                && !ciResponseParameters.isPrepaidService()
                && ciResponseParameters.isROLinked()
                && ciResponseParameters.isPlanInCart()
                && ciResponseParameters.isDeviceInCart()
                && ciResponseParameters.isDeviceROInCart())
            return CI_OUTCOME.REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO.toString();
        if (ciResponseParameters.isDavinci()
                && !ciResponseParameters.isPrepaidService()
                && !ciResponseParameters.isROLinked()
                && ciResponseParameters.isPlanInCart()
                && ciResponseParameters.isDeviceInCart()
                && ciResponseParameters.isDeviceROInCart())
            return CI_OUTCOME.REMOVE_PLAN_AND_BUY_PHONE.toString();
        if (ciResponseParameters.isDavinci()
                && !ciResponseParameters.isPrepaidService()
                && ciResponseParameters.isPlanInCart()
                && ciResponseParameters.isDeviceInCart()
                && !ciResponseParameters.isDeviceROInCart())
            return CI_OUTCOME.REMOVE_PLAN_AND_BUY_PHONE.toString();

        return CI_OUTCOME.HARDSTOP.toString();
    }

    public SIResponseParameters getSIResponseParameters(SIBAPIResponse siBAPIResponse) {
        SIResponseParameters siResponseParameters = new SIResponseParameters();
        SMFeasibilityEligibilityDetails smFeasibilityEligibilityDetailsResponse =
                siBAPIResponse.getSmFeasibilityEligibilityDetails();

        if (smFeasibilityEligibilityDetailsResponse != null) {
            if (smFeasibilityEligibilityDetailsResponse.getFeasibilityOutcome() != null) {
                var feasibilityOutcome =
                        smFeasibilityEligibilityDetailsResponse.getFeasibilityOutcome();

                siResponseParameters.setFeasible(
                        YES_STRING.equalsIgnoreCase(feasibilityOutcome.getFeasibilityStatus()));
                siResponseParameters.setTONAvailable(
                        TON_AVAILABLE.equalsIgnoreCase(feasibilityOutcome.getIsTON()));
                if (NO_STRING.equalsIgnoreCase(feasibilityOutcome.getFeasibilityStatus())) {
                    siResponseParameters.setReasonForFeasibilityError(
                            feasibilityOutcome.getReason());
                }
            }

            if (smFeasibilityEligibilityDetailsResponse.getEligibilityOutcome() != null) {
                var eligibilityOutcome =
                        smFeasibilityEligibilityDetailsResponse.getEligibilityOutcome();

                siResponseParameters.setEligibile(
                        ELIGIBLE.equalsIgnoreCase(eligibilityOutcome.getEligibilityStatus()));
                if (eligibilityOutcome.getAgoraDeepLink() != null) {
                    siResponseParameters.setAgoraLinkFound(
                            !eligibilityOutcome.getAgoraDeepLink().isBlank());
                }
            }

            if (smFeasibilityEligibilityDetailsResponse.getEtcOutcome() != null) {
                var etcOutcome = smFeasibilityEligibilityDetailsResponse.getEtcOutcome();

                siResponseParameters.setETCSuccess(etcOutcome.isSuccess());
                if (!ObjectUtils.isEmpty(etcOutcome.getHasETC()))
                    siResponseParameters.setHasETC(etcOutcome.getHasETC().booleanValue());
                if (!ObjectUtils.isEmpty(etcOutcome.getIsCIMTransferRODevice()))
                    siResponseParameters.setCIMTransferRODevice(
                            etcOutcome.getIsCIMTransferRODevice().booleanValue());
                if (!ObjectUtils.isEmpty(etcOutcome.getDeviceDetails()))
                    siResponseParameters.setEtcDeviceDetails(etcOutcome.getDeviceDetails());
                if (!ObjectUtils.isEmpty(etcOutcome.getEtcsAmount()))
                    siResponseParameters.setEtcsAmount(etcOutcome.getEtcsAmount());
            }
        }
        siResponseParameters.setFeasibleEligibleAndETCSuccess(
                siResponseParameters.isFeasible()
                        && siResponseParameters.isEligibile()
                        && siResponseParameters.isETCSuccess());

        return siResponseParameters;
    }

    public String getSIOutcome(SIResponseParameters siResponseParameters) {
        if (!siResponseParameters.isFeasibleEligibleAndETCSuccess()
                && siResponseParameters.isAgoraLinkFound())
            return SI_OUTCOME.REDIRECT_TO_AGORA.toString();
        if (siResponseParameters.isFeasibleEligibleAndETCSuccess())
            return SI_OUTCOME.CONTINUE.toString();
        return SI_OUTCOME.HARDSTOP.toString();
    }

    public OrderingStateInfo getCacheData(HeadersData headersData) {
        var cacheKey =
                cacheUtil.getCacheKey(
                        headersData.getCorrelationId(),
                        headersData.getClaims(),
                        headersData.getChannel());
        logger.info("[SIBFFHelper] cache key: {}", cacheKey);
        var orderingStateInfo = cacheUtil.getCacheData(cacheKey);
        logger.info("[SIBFFHelper] OrderingStateInfo from cache: {}", orderingStateInfo);

        cacheUtil.validateCacheData(orderingStateInfo, cacheKey);
        return orderingStateInfo;
    }
}

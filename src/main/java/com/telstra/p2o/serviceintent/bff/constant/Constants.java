package com.telstra.p2o.serviceintent.bff.constant;

import com.fasterxml.jackson.annotation.JsonValue;

public class Constants {

    public static final String CORRELATION_ID_MISSING = "Correlation ID is missing";
    public static final String CACHE_DATA_NOT_FOUND = "Data not found in cache for %s";
    public static final String EMPTY_CI_RESPONSE =
            "Response for customer information from BAPI is null";

    public static final String GENERIC_ERROR = "GENERIC_ERROR";

    public static final String TLS = "TLSv1.2";

    public static final String PRODUCT_OFFERING_ID = "VMP0000202";

    public static final String JKS = "JKS";

    public static final String MICRO_TOKEN_EMPTY = "MicroToken received is empty";

    public static final String X_CORRELATION_ID = "X-Correlation-Id";
    public static final String X_MICRO_TOKEN = "X-Microtoken";
    public static final String X_CHANNEL = "X-Channel";
    public static final String ASSET_REFERENCE_ID = "assetReferenceId";
    public static final String CONTRACT = "CONTRACT";
    public static final String DPC = "DPC";
    public static final String X_SOURCE_SYSTEM = "X-Source-System";
    public static final String SOURCE = "Source";

    public static final String X_FLOW_NAME = "X-Flow-Name";

    public static final String ACCOUNT_UUID = "accountUUID";
    public static final String SERVICE_ID = "serviceId";

    public static final String LEGACY_SYSTEM = "legacysystem";

    public static final String ORDER_REFERENCE_NUMBER = "orderReferenceNumber";

    public static final String PRODUCT_TYPE_SUBSCRIPTION = "Subscription";

    public static final String PRODUCT_FAMILY_MOBILES = "Mobiles";
    public static final String ERROR_SERVICE_INTENT = "Error message from service intent BAPI: {}";

    public static final String PRODUCT_FAMILY_MOBILE = "Mobile";

    public static final String HTTP_STATUS_OK = "200";

    public static final String HTTP_STATUS_INTERNAL_SERVER_ERROR = "500";

    public static final String ERR_ORDER_STATE_INFO_SELECTED_SERVICE_NOT_NULL =
            "OrderStateInfo SelectedService or AssetReferenceId Should Not Be Null or Empty";

    public enum CI_OUTCOME {
        SERVICE_MIGRATION("SERVICE_MIGRATION"),
        REMOVE_PLAN_AND_BUY_PHONE("REMOVE_PLAN_AND_BUY_PHONE"),
        REDIRECT_TO_MYT("REDIRECT_TO_MYT"),
        REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO("REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO"),
        BUY_PHONE_AND_DISCONNECT_RO("BUY_PHONE_AND_DISCONNECT_RO"),
        REDIRECT_TO_AGORA("REDIRECT_TO_AGORA"),
        HARDSTOP("HARDSTOP");
        private final String value;

        CI_OUTCOME(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    public enum SI_OUTCOME {
        CONTINUE("CONTINUE"),
        REDIRECT_TO_AGORA("REDIRECT_TO_AGORA"),
        HARDSTOP("HARDSTOP");
        private final String value;

        SI_OUTCOME(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    public static final String ERROR_MESSAGE = "Error in service intent: {0}";

    public static final String PRODUCT_FAMILY_GENERAL = "General";
    public static final String PRODUCT_TYPE_HARDWARE = "Hardware";
    public static final String PRODUCT_HARDWARE_PURCHASE_TYPE = "Repayment";
    public static final String CIM_TRANSFER_RO_NOT_ALLOWED = "CIM_TRANSFER_RO_NOT_ALLOWED";
    public static final String PRODUCT_SUB_TYPE_STREAMING_DEVICES = "StreamingDevices";
    public static final String QUANTITY = "quantity";
    public static final String REQUEST_PAYLOAD_EMPTY = "Request payload is empty";
    public static final String ACCOUNT_UUID_MISSING =
            "accountUUID in request payload/cache is missing";
    public static final String AES_1006_ERROR_CODE = "1006";
    public static final String AES_1006_ERROR_MESSAGE = "AES API Error";
    public static final String AES_ELIGIBILITY = "AESEligibility";

    public static final String IS_MICA = "isMICA";
    public static final String ADDON = "ADDON";
    public static final String SERVICE = "SERVICE";
    public static final String ACTIVE = "ACTIVE";
    public static final String HARDWARE = "HARDWARE";
    public static final String DEFAULT_SOURCE = "tDotcom";
    public static final String USER_NAME = "username";
    public static final String USER_TYPE = "usertype";

    public static final String XC001016739 = "XC001016739";
    public static final String XC001005417 = "XC001005417";

    public static final String YES_STRING = "YES";
    public static final String NO_STRING = "NO";
    public static final String TON_AVAILABLE = "Y";
    public static final String ELIGIBLE = "Eligible";
    public static final String PREPAID = "Prepaid";
    public static final String SUBSCRIPTION = "Subscription";

    public static final int STATUS_CODE_200 = 200;

    public static final String CART_ACTION_TYPE_ADD_RO_DEVICE = "addRoAndDevice";
    public static final String CART_ACTION_CODE_OC_CRTRUL_0002 = "OC_CRTRUL_0002";

    public static final String CART_ACTION_CODE_OC_CRTRUL_0001 = "OC_CRTRUL_0001";
    public static final String REMOVE_DUP = "removeDUP";
    public static final String ADD_DUP = "addDUP";
    public static final String ENABLE_STAYCONN = "enableStayConn";

    public static final String REMOVE_PRODUCT_FROM_CART = "removeProductFromCart";
    public static final String ADD_PRODUCTS_TO_CART = "addProductsToCart";

    public static final String MBLTDAS_UPPR = "MBLTDAS-UPPR";

    public static final String ASSOCIATED_SERVICE = "associatedService";

    public static final String CCBRCRM = "CCB-RCRM";
    public static final String SIEBEL = "SIEBEL";

    public static final String INVALID_INPUT_DATA = "Invalid inputs are received";
    public static final String REPAYMENT = "Repayment";
    public static final String CART_CONTEXT_ID = "cartContextId";
    public static final String CART_ACTION_CODE_OC_CRTRUL_0006 = "OC_CRTRUL_0006";
    public static final String CART_ACTION_TYPE_ADD_DUP = "addDeviceUpgradeAndProtect";

    // Add Ro to existing Service
    public static final String ADD_RO = "add-ro";
    public static final String CART_ACTION_CODE_OC_CRTRUL_0005 = "OC_CRTRUL_0005";
    public static final String CART_ACTION_CODE_OC_CRTRUL_0009 = " OC_CRTRUL_0009";
    public static final String DISCONNECT = "Disconnect";
    public static final String LINK_RO = "linkRODeviceToService";
    public static final String ADD_RO_TO_EXISTING_SERVICE = "addRoToExistingService";
    public static final String MOBILES = "Mobiles";

    public static final String CASE_ID = "caseId";

    public static final String ROLE_ACCOUNT = "Account";

    public static final String TYPE_RELATED_PARTY = "RelatedParty";

    public static final String GRADING = "grading";

    public static final String SCREEN_DAMAGE = "screenDamage";

    public static final String BODY_DAMAGE = "bodyDamage";

    public static final String LIQUID_DAMAGE = "liquidDamage";

    public static final String NULL_ERR_MSG = " should not be null";

    public static final String VALIDATION_TYPE = "validationType";

    public static final String DEVICE_DETAILS = "deviceDetails";

    public static final String VERIFY_IMEI_PARTIAL = "PARTIAL";

    public static final String VERIFY_IMEI_FULL = "FULL";

    public static final String DATE_OF_PURCHASE = "dateOfPurchase";

    public static final String TYPE_OF_PROOF = "typeOfProof";

    public static final String REDEEMPTION_ELIGIBILITY = "redemptionEligibility";

    public static final String EMPTY_REDEMPTION_ELIGIBILITY_RESPONSE =
            "Response for redemptionEligibility information from BAPI is null";

    public static final Object BAPI_SERIVE_ERROR = "Error Message for service intent from BAPI: ";

    public static final String REDEEMPTION_ELIGIBILITY_FALSE_ERROR =
            "Redemption Eligibility Outcome status false because redemption count>=2 ";

    public static final String DD_MMM_YYYY = "dd-MMM-yyyy";

    public static final String VALIDATE_PARTIAL_IMEI = "validatePartialIMEI";

    public static final String VALIDATE_FULL_IMEI = "validateIMEI";
    public static final String CHARACTERISTIC_VALUE_TYPE_STRING = "String";
    public static final String CHARACTERISTIC_NAME_AGENTID = "agentId";
    public static final String CHARACTERISTIC_NAME_PREMISECODE = "premiseCode";

    public static final String CHARACTERISTIC_NAME_DATE_OF_PURCHASE = "dateOfPurchase";

    public static final String CHARACTERISTIC_NAME_DATE_OF_TYPE_OF_PROOF = "typeOfProof";

    public static final String CHARACTERISTIC_VALUE_TYPE_DATE = "date";

    public static final String EMPTY_VALIDATE_IMEI_RESPONSE =
            "Response for validate IMEI received from BAPI is null";

    public static final String ERR_VALIDATION_TYPE_NOT_MATCHING =
            "Request provided validation type not matching";

    public static final String ERROR_MESSAGE_FOR_BAPI_CALL = "Exception from BAPI: ";

    public static final String ERROR_MESSAGE_OUTCOMES_NOT_NULL =
            "Get Eligibility Outcomes should not be null ";

    public static final String GET_ELIGIBILITY_PROCESSING_ERROR =
            "Invoked Get Eligibility API Processing failed";

    public static final String ERROR_STAMPING_ORDER_STATE_INFO =
            "Device Details Stamping Into OrderStateInfo failed :";

    public static final String BAPI_RESPONSE_SERVICE_DETAIL_NULL =
            "Get Eligibility Response From Bapi have service details is null";

    public static final String SERVICE_ERROR = " Service Error";

    public static final String PRODUCT_TYPE_SERVICE = "Service";
}

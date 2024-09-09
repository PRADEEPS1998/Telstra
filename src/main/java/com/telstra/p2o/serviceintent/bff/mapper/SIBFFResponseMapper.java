package com.telstra.p2o.serviceintent.bff.mapper;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.*;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.CI_OUTCOME.*;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.CI_OUTCOME.REDIRECT_TO_AGORA;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.SI_OUTCOME.*;

import com.telstra.p2o.serviceintent.bff.dto.bapi.*;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.*;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.ETCDetails;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SIBFFResponseMapper {

    public SIBFFResponse createSIBFFResponse(
            CIBAPIResponse ciBAPIResponse,
            RepaymentOptions repaymentOptions,
            String outcome,
            CIResponseParameters ciResponseParameters,
            boolean hasDUP) {
        try {
            if (CI_OUTCOME.REDIRECT_TO_MYT.toString().equals(outcome)) {
                var response = getSIBFFResponse(ciBAPIResponse);
                response.setAction(REDIRECT_TO_MYT.toString());
                if (ciResponseParameters.isROLinked()) {
                    response.setRepaymentOptions(repaymentOptions);
                }
                return response;
            } else if (REDIRECT_TO_AGORA.toString().equals(outcome)) {
                var response = getSIBFFResponse(ciBAPIResponse);
                response.setAction(REDIRECT_TO_AGORA.toString());
                return response;
            } else if (REMOVE_PLAN_AND_BUY_PHONE_AND_DISCONNECT_RO.toString().equals(outcome)
                    || BUY_PHONE_AND_DISCONNECT_RO.toString().equals(outcome)) {
                var response = getSIBFFResponse(ciBAPIResponse);
                response.setAction(outcome);
                response.setSelectedPlanDetails(getSelectedPlanDetails(ciResponseParameters));
                response.setExistingPlanDetails(
                        getExistingPlanDetails(ciResponseParameters, hasDUP));
                response.setRepaymentOptions(repaymentOptions);
                return response;
            } else {
                var response = getSIBFFResponse(ciBAPIResponse);
                response.setAction(REMOVE_PLAN_AND_BUY_PHONE.toString());
                response.setSelectedPlanDetails(getSelectedPlanDetails(ciResponseParameters));
                response.setExistingPlanDetails(
                        getExistingPlanDetails(ciResponseParameters, hasDUP));
                response.setRepaymentOptions(repaymentOptions);
                return response;
            }
        } catch (Exception e) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error while creating BFF response after customer informer call: " + e,
                    GENERIC_ERROR);
        }
    }

    private SIBFFResponse getSIBFFResponse(CIBAPIResponse ciBAPIResponse) {
        return SIBFFResponse.builder()
                .statusCode(ciBAPIResponse.getStatusCode())
                .success(ciBAPIResponse.isSuccess())
                .time(ciBAPIResponse.getTime())
                .build();
    }

    public SIBFFResponse createSIBFFResponseForSIOutcome(
            SIBAPIResponse siBAPIResponse,
            String outcome,
            SIResponseParameters siResponseParameters,
            CIResponseParameters ciResponseParameters,
            boolean hasDUP) {
        try {
            if (SI_OUTCOME.REDIRECT_TO_AGORA.toString().equals(outcome)) {
                return SIBFFResponse.builder()
                        .statusCode(siBAPIResponse.getStatusCode())
                        .success(true)
                        .time(siBAPIResponse.getTime())
                        .isFeasibleEligibleAndETCSuccess(false)
                        .action(REDIRECT_TO_AGORA.toString())
                        .reasonForFeasibilityError(
                                siResponseParameters.getReasonForFeasibilityError())
                        .agoraDeepLink(
                                siBAPIResponse
                                        .getSmFeasibilityEligibilityDetails()
                                        .getEligibilityOutcome()
                                        .getAgoraDeepLink())
                        .build();
            } else {
                return SIBFFResponse.builder()
                        .statusCode(siBAPIResponse.getStatusCode())
                        .success(true)
                        .time(siBAPIResponse.getTime())
                        .action(CONTINUE.toString())
                        .isFeasibleEligibleAndETCSuccess(true)
                        .isTONAvailable(siResponseParameters.isTONAvailable())
                        .stayConn(
                                siBAPIResponse
                                        .getSmFeasibilityEligibilityDetails()
                                        .getFeasibilityOutcome()
                                        .getStayConn())
                        .existingPlanDetails(getExistingPlanDetails(ciResponseParameters, hasDUP))
                        .selectedPlanDetails(getSelectedPlanDetails(ciResponseParameters))
                        .etcDetails(getEtcDetails(siBAPIResponse, siResponseParameters))
                        .build();
            }
        } catch (Exception e) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Error while creating BFF response after service intent call: " + e,
                    GENERIC_ERROR);
        }
    }

    private SelectedPlanDetails getSelectedPlanDetails(CIResponseParameters ciResponseParameters) {
        return SelectedPlanDetails.builder()
                .planName(ciResponseParameters.getSelectedPlanName())
                .build();
    }

    private ExistingPlanDetails getExistingPlanDetails(
            CIResponseParameters ciResponseParameters, boolean hasDUP) {
        return ExistingPlanDetails.builder()
                .planName(ciResponseParameters.getExistingPlanName())
                .deviceName(ciResponseParameters.getDeviceName())
                .isROLinked(ciResponseParameters.isROLinked())
                .sourceSystem(ciResponseParameters.getSourceSystem())
                .hasDUP(hasDUP)
                .isPrepaidService(ciResponseParameters.isPrepaidService())
                .build();
    }

    private ETCDetails getEtcDetails(
            SIBAPIResponse siBAPIResponse, SIResponseParameters siResponseParameters) {
        if (siResponseParameters.isHasETC()) {
            ETCDetails etcDetails =
                    ETCDetails.builder()
                            .hasETC(siResponseParameters.isHasETC())
                            .isCIMTransferRODevice(siResponseParameters.isCIMTransferRODevice())
                            .build();
            if (siBAPIResponse.getSmFeasibilityEligibilityDetails() != null
                    && siBAPIResponse.getSmFeasibilityEligibilityDetails().getEtcOutcome()
                            != null) {
                setEtcDetails(etcDetails, siBAPIResponse, siResponseParameters);
            }
            return etcDetails;
        } else {
            return ETCDetails.builder().hasETC(siResponseParameters.isHasETC()).build();
        }
    }

    private void setDeviceDetails(
            SIResponseParameters siResponseParameters,
            SIBAPIResponse siBAPIResponse,
            ETCDetails etcDetails) {
        if (siResponseParameters.isCIMTransferRODevice()
                && (!siBAPIResponse
                        .getSmFeasibilityEligibilityDetails()
                        .getEtcOutcome()
                        .getDeviceDetails()
                        .isEmpty())
                && siBAPIResponse
                                .getSmFeasibilityEligibilityDetails()
                                .getEtcOutcome()
                                .getDeviceDetails()
                                .get(0)
                        != null) {
            etcDetails.setDeviceDetails(
                    siBAPIResponse
                            .getSmFeasibilityEligibilityDetails()
                            .getEtcOutcome()
                            .getDeviceDetails()
                            .get(0));
        }
    }

    private void setEtcDetails(
            ETCDetails etcDetails,
            SIBAPIResponse siBAPIResponse,
            SIResponseParameters siResponseParameters) {
        setDeviceDetails(siResponseParameters, siBAPIResponse, etcDetails);
        if (siBAPIResponse.getSmFeasibilityEligibilityDetails().getEtcOutcome().getHasETC() != null
                && siBAPIResponse
                                .getSmFeasibilityEligibilityDetails()
                                .getEtcOutcome()
                                .getIsCIMTransferRODevice()
                        != null) {
            etcDetails.setHasETC(
                    siBAPIResponse
                            .getSmFeasibilityEligibilityDetails()
                            .getEtcOutcome()
                            .getHasETC());
            etcDetails.setIsCIMTransferRODevice(
                    siBAPIResponse
                            .getSmFeasibilityEligibilityDetails()
                            .getEtcOutcome()
                            .getIsCIMTransferRODevice());
            setETCAmount(siBAPIResponse, etcDetails);
        }
    }

    private void setETCAmount(SIBAPIResponse siBAPIResponse, ETCDetails etcDetails) {
        if (siBAPIResponse.getSmFeasibilityEligibilityDetails().getEtcOutcome().getEtcsAmount()
                != null) {
            etcDetails.setEtcsAmount(
                    siBAPIResponse
                            .getSmFeasibilityEligibilityDetails()
                            .getEtcOutcome()
                            .getEtcsAmount());
        }
    }
}

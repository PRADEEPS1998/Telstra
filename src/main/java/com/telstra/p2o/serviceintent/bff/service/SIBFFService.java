package com.telstra.p2o.serviceintent.bff.service;

import com.telstra.p2o.serviceintent.bff.dto.bapi.DeviceAssessResponse;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.CacheRequest;
import com.telstra.p2o.serviceintent.bff.dto.bffRequest.VerifyImeiPayload;
import com.telstra.p2o.serviceintent.bff.dto.bffResponse.*;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;

public interface SIBFFService {

    SIBFFResponse getMobileServiceIntentDetails(HeadersData headersData, GetSIPayload payload);

    SIBFFResponse getStreamingServiceIntentDetails(HeadersData headersData) throws SIBFFException;

    CacheBFFResponse cacheBFFResponse(CacheRequest cacheRequest, HeadersData headersData)
            throws SIBFFException;

    DeviceIntentResponse getDeviceIntentDetails(GetSIPayload payload, HeadersData headerData)
            throws SIBFFException;

    AssessDeviceResponse getDeviceAssessDetails(
            AssessDeviceConditionPayload payload, HeadersData headerData) throws SIBFFException;

    DeviceAssessResponse verifyImeiDetails(VerifyImeiPayload payload, HeadersData headerData);
}

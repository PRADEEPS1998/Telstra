package com.telstra.p2o.serviceintent.bff.util;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.CACHE_DATA_NOT_FOUND;
import static com.telstra.p2o.serviceintent.bff.constant.Constants.GENERIC_ERROR;

import com.telstra.p2o.common.caching.data.OrderingStateInfo;
import com.telstra.p2o.common.caching.util.OrderingStateCacheUtil;
import com.telstra.p2o.serviceintent.bff.exception.CacheDataNotFoundException;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SIBFFCacheUtil {

    @Autowired private OrderingStateCacheUtil orderingStateCacheUtil;

    @Value("${enableCacheKeyGeneration}")
    public boolean enableCacheKeyGeneration;

    public String getCacheKey(String correlationId, Map<String, Object> claims, String channel) {
        try {
            return orderingStateCacheUtil.getOrderingStateInfoCacheKey(
                    correlationId, claims, channel, enableCacheKeyGeneration);
        } catch (Exception e) {
            throw new CacheDataNotFoundException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Exception in fetching cache key " + " : " + e.getMessage(),
                    GENERIC_ERROR);
        }
    }

    public OrderingStateInfo getCacheData(String cacheKey) {
        try {
            return orderingStateCacheUtil.get(cacheKey);
        } catch (Exception e) {
            throw new CacheDataNotFoundException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Exception in fetching data from cache for "
                            + cacheKey
                            + " : "
                            + e.getMessage(),
                    GENERIC_ERROR);
        }
    }

    public void setCacheData(String cacheKey, OrderingStateInfo orderingStateInfo) {
        try {
            orderingStateCacheUtil.put(cacheKey, orderingStateInfo);
        } catch (Exception e) {
            throw new SIBFFException(
                    HttpStatus.INTERNAL_SERVER_ERROR.value(),
                    "Exception in setting data into cache for " + cacheKey + " : " + e.getMessage(),
                    GENERIC_ERROR);
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
}

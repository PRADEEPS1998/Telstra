package com.telstra.p2o.serviceintent.bff.util;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;

import com.telstra.p2o.common.caching.data.OrderingStateInfo;
import com.telstra.p2o.common.caching.util.OrderingStateCacheUtil;
import com.telstra.p2o.serviceintent.bff.exception.CacheDataNotFoundException;
import com.telstra.p2o.serviceintent.bff.exception.SIBFFException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

@RunWith(MockitoJUnitRunner.class)
public class SIBFFCacheUtilTest {

    @InjectMocks SIBFFCacheUtil sibffCacheUtil;

    @Mock OrderingStateCacheUtil orderingStateCacheUtil;

    private String correlationId;
    private OrderingStateInfo orderingStateInfo;

    @Before
    public void setup() {
        correlationId = "1234";
        orderingStateInfo = getOrderingStateInfoFromCache(correlationId);
        sibffCacheUtil.enableCacheKeyGeneration = true;
    }

    @Test
    public void getCacheDataTest() throws Exception {
        Mockito.when(orderingStateCacheUtil.get(correlationId)).thenReturn(orderingStateInfo);
        OrderingStateInfo orderingStateInfo1 = sibffCacheUtil.getCacheData(correlationId);
        Assert.assertNotNull(orderingStateInfo1);
    }

    @Test(expected = CacheDataNotFoundException.class)
    public void getCacheDataNoDataFoundTest() throws Exception {
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .when(orderingStateCacheUtil)
                .get(anyString());
        OrderingStateInfo orderingStateInfo1 = sibffCacheUtil.getCacheData(correlationId);
    }

    @Test(expected = CacheDataNotFoundException.class)
    public void getCacheKeyExceptionTest() {
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .when(orderingStateCacheUtil)
                .getOrderingStateInfoCacheKey(anyString(), anyMap(), anyString(), anyBoolean());
        Map<String, Object> claims = new HashMap<>();
        claims.put("b2c_uid", "testuid");
        String actualCacheKey = sibffCacheUtil.getCacheKey(correlationId, claims, "self-serve");
    }

    @Test
    public void getCacheKeyTest() {
        Mockito.when(
                        orderingStateCacheUtil.getOrderingStateInfoCacheKey(
                                anyString(), anyMap(), anyString(), anyBoolean()))
                .thenReturn("testCacheKey");
        Map<String, Object> claims = new HashMap<>();
        claims.put("b2c_uid", "testuid");
        String actualCacheKey = sibffCacheUtil.getCacheKey(correlationId, claims, "self-serve");
        Assert.assertEquals("testCacheKey", actualCacheKey);
    }

    @Test(expected = SIBFFException.class)
    public void saveCacheDataTest() throws Exception {
        Mockito.doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .when(orderingStateCacheUtil)
                .put(anyString(), any(OrderingStateInfo.class));
        sibffCacheUtil.setCacheData("testCacheKey", orderingStateInfo);
    }

    public OrderingStateInfo getOrderingStateInfoFromCache(String correlationId) {
        OrderingStateInfo orderingStateInfo = new OrderingStateInfo();
        orderingStateInfo.setCorrelationId(correlationId);
        orderingStateInfo.setOrderReferenceNumber("H22031223733807");
        orderingStateInfo.setContactUUID("74927b89-c41b-74bb-0be3-638e4adf4854");
        orderingStateInfo.setAccountUUID("2b16e2eb-eed9-78fd-aad2-83b26ab2ccd0");
        orderingStateInfo.setAccountType("Residential");
        orderingStateInfo.setAccountEstablished(true);
        orderingStateInfo.setHasPlan(false);
        return orderingStateInfo;
    }

    @Test(expected = CacheDataNotFoundException.class)
    public void validateCacheDataTest() throws Exception {
        sibffCacheUtil.validateCacheData(null, "testCacheKey");
    }
}

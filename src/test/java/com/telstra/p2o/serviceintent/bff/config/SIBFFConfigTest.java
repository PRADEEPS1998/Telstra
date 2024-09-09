package com.telstra.p2o.serviceintent.bff.config;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SIBFFConfigTest {
    @InjectMocks SIBFFConfig sibffConfig;

    @Test
    public void getRestTemplateTest() {
        sibffConfig.keyStoreFile = "testKey";
        sibffConfig.keyStorePassword = "testPassword";

        var restTemplate = sibffConfig.getRestTemplate();
        Assert.assertNotNull(restTemplate);
    }
}

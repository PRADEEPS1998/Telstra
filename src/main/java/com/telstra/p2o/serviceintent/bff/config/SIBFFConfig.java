package com.telstra.p2o.serviceintent.bff.config;

import static com.telstra.p2o.serviceintent.bff.constant.Constants.*;

import com.telstra.p2o.common.core.validator.UserInfoValidator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Base64;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import lombok.Getter;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Getter
public class SIBFFConfig {

    private static final Logger logger = LoggerFactory.getLogger(SIBFFConfig.class);

    @Value("${telstra.p2o.bapi.url.customerInformation}")
    public String customerInformationURL;

    @Value("${valid.flow}")
    public List<String> validFlow;

    @Value("${telstra.p2o.bapi.url.smEligibilityDetails}")
    public String smEligibilityDetailsURL;

    @Value("${telstra.p2o.bapi.url.serviceIntentGetEligibility}")
    public String serviceIntentGetEligibilityURL;

    @Value("${key-store}")
    public String keyStoreFile;

    @Value("${key-store-password}")
    public String keyStorePassword;

    @Value("${telstra.p2o.bapi.url.getETC}")
    public String getETCUrl;

    @Value("${telstra.p2o.bapi.url.serviceIntentDeviceAssessment}")
    public String serviceIntentBAPIUrl;

    @Value("${telstra.p2o.bapi.url.deviceAssessment}")
    public String deviceAssessment;

    /**
     * Rest template implementaion is overridden as the certificates have to be loaded from Credhub
     * and then injected.
     *
     * @return
     */
    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate(getHttpComponentsClientHttpRequestFactory());
    }

    /**
     * Here we are loading the key store and trust store into SSL context
     *
     * @return
     */
    private ClientHttpRequestFactory getHttpComponentsClientHttpRequestFactory() {

        var factory = new HttpComponentsClientHttpRequestFactory();

        var jksKeyStore = getStore(keyStoreFile, keyStorePassword);

        var sslContext = getSSLContext(jksKeyStore);

        HostnameVerifier hostnameVerifier = new NoopHostnameVerifier();

        var socketFactory =
                new SSLConnectionSocketFactory(
                        sslContext, new String[] {TLS}, null, hostnameVerifier);
        HttpClient httpClient = HttpClients.custom().setSSLSocketFactory(socketFactory).build();

        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(60000);

        return factory;
    }

    /**
     * SSL Context is loaded here. Protocol is set to TLS1.2 and the certificates are loaded into
     * SSL Context.
     *
     * @param jksKeyStore
     * @return
     */
    private SSLContext getSSLContext(KeyStore jksKeyStore) {
        try {
            return SSLContextBuilder.create()
                    .setProtocol(TLS)
                    .loadKeyMaterial(jksKeyStore, keyStorePassword.toCharArray())
                    .build();
        } catch (Exception e) {
            logger.error("Exception in getSSLContext. ", e);
        }
        return null;
    }

    /**
     * Here the certificate content which is in JSON format and Base64 encoded, is Base64 decoded
     * and loaded into java keystore
     *
     * @param certificateContent
     * @param certificatePassword
     * @return
     */
    private KeyStore getStore(String certificateContent, String certificatePassword) {

        try {
            var keyStore = KeyStore.getInstance(JKS);

            byte[] decodedCertificate = Base64.getDecoder().decode(certificateContent);
            InputStream inputStream = new ByteArrayInputStream(decodedCertificate);

            keyStore.load(inputStream, certificatePassword.toCharArray());

            return keyStore;
        } catch (Exception e) {
            logger.error("Exception in  getStore.", e);
        }
        return null;
    }

    @Bean
    public UserInfoValidator getUserInfoValidator() {
        return new UserInfoValidator();
    }

    public String getETCUrl() {
        return getETCUrl;
    }
}

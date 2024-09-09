package com.telstra.p2o.serviceintent.bff;

import com.telstra.p2o.common.ntet.configuration.NtetConfiguration;
import com.telstra.p2o.common.tickethelper.configuration.AspectHelperConfiguration;
import com.telstra.p2o.common.tickethelper.configuration.LoggerHelperConfiguration;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(
        exclude = {
            MongoReactiveAutoConfiguration.class,
            MongoAutoConfiguration.class,
            MongoDataAutoConfiguration.class
        })
@ComponentScan(basePackages = "com.telstra.p2o.common.logging")
@ComponentScan(basePackages = "com.telstra.p2o.common.ntet")
@ComponentScan(basePackages = "com.telstra.p2o.common.tickethelper")
@ComponentScan(basePackages = "com.telstra.p2o.common.caching")
@ComponentScan(basePackages = "com.telstra.p2o.common.core.config")
@ComponentScan(basePackages = "com.telstra.p2o.common.core")
public class P2oServiceIntentBffApplication {

    public static void main(String[] args) {
        final var app =
                new SpringApplication(
                        P2oServiceIntentBffApplication.class,
                        NtetConfiguration.class,
                        AspectHelperConfiguration
                                .class, // Aspect Helper Configuration required to capture method
                        // stats and Ticketable Exceptions thrown by service methods
                        LoggerHelperConfiguration.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args); // run the application
    }
}

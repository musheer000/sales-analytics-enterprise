package com.salesanalytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Entry Point for Sales Analytics & Reporting System
 *
 * Architecture Flow:
 * Client -> Controller -> Service -> Repository -> MySQL Database
 * With Spring Cache layer caching frequently accessed data.
 * @EnableAsync enables async email sending (OTP delivery) without blocking HTTP threads.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class SalesAnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesAnalyticsApplication.class, args);
        System.out.println("=================================================================");
        System.out.println(" Sales Analytics & Reporting System started successfully!");
        System.out.println(" Backend API:  http://localhost:8080/api/categories");
        System.out.println(" Swagger Docs: http://localhost:8080/swagger-ui.html");
        System.out.println(" Database:     MySQL (sales_analytics)");
        System.out.println("=================================================================");
    }
}

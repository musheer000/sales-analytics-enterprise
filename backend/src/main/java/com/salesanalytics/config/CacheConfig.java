package com.salesanalytics.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CacheConfig {

    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_PRODUCTS = "products";
    public static final String CACHE_ANALYTICS_SUMMARY = "analyticsSummary";
    public static final String CACHE_CATEGORY_SALES = "categorySales";
    public static final String CACHE_MONTHLY_REVENUE = "monthlyRevenue";

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(
                CACHE_CATEGORIES,
                CACHE_PRODUCTS,
                CACHE_ANALYTICS_SUMMARY,
                CACHE_CATEGORY_SALES,
                CACHE_MONTHLY_REVENUE
        ));
        return cacheManager;
    }
}

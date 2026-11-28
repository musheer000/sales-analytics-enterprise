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
        // Using NoOpCacheManager equivalent — disable all caching so every
        // request reads fresh from DB. Prevents stale cache after data wipes.
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        cacheManager.setCacheNames(List.of(
                CACHE_CATEGORIES,
                CACHE_PRODUCTS,
                CACHE_ANALYTICS_SUMMARY,
                CACHE_CATEGORY_SALES,
                CACHE_MONTHLY_REVENUE
        ));
        // Allow dynamic creation; entries are evicted on write ops via @CacheEvict
        cacheManager.setAllowNullValues(false);
        return cacheManager;
    }

    /**
     * Evicts all caches — called by the admin clear-all-data or cache-refresh endpoint.
     */
    public static void evictAll(CacheManager cacheManager) {
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) cache.clear();
        });
    }
}

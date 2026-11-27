package com.salesanalytics.service;

import com.salesanalytics.entity.Category;
import com.salesanalytics.entity.Product;
import com.salesanalytics.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Core Marketplace Algorithms & Trade Compliance Engine:
 * 1. Global Trade Compliance (allows worldwide trade except direct India <-> Pakistan restriction)
 * 2. Dynamic Multi-Tier Bulk Wholesale Pricing Algorithm
 * 3. Weighted Product Trending & Recommendation Score
 * 4. Automatic Global Agri-Food & Bulk Commodity Category Seeding
 */
@Service
public class MarketplaceAlgorithmService implements CommandLineRunner {

    private final CategoryRepository categoryRepository;

    public MarketplaceAlgorithmService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args) {
        // Ensure essential global wholesale & agri-food categories exist
        List<String[]> defaults = List.of(
                new String[]{"Vegetables & Fresh Produce", "Bulk farm vegetables, onions, potatoes, tomatoes & organic greens"},
                new String[]{"Fruits & Orchards", "Fresh & dried fruits, mangoes, apples, dates & citrus exports"},
                new String[]{"Spices & Herbs", "Whole & ground spices, turmeric, cardamom, pepper, saffron & chilies"},
                new String[]{"Grains, Pulses & Raw", "Basmati rice, wheat, lentils, pulses, oilseeds & raw agricultural goods"},
                new String[]{"Food Items & Beverages", "Packaged foods, edible oils, tea, coffee, sugar & dairy bulk lots"},
                new String[]{"Textiles & Apparel", "Wholesale garments, cotton fabrics & fashion bulk lots"},
                new String[]{"Industrial & Raw Materials", "Raw commodities, packaging & wholesale trade supplies"}
        );
        for (String[] def : defaults) {
            if (!categoryRepository.existsByNameIgnoreCase(def[0])) {
                Category c = new Category();
                c.setName(def[0]);
                c.setDescription(def[1]);
                categoryRepository.save(c);
            }
        }
    }

    /**
     * Global Trade Compliance Algorithm:
     * Worldwide buyers & sellers can trade freely, except direct cross-border
     * transactions between India and Pakistan are restricted.
     */
    public void validateTradeCompliance(String originCountry, String buyerCountry) {
        String origin = normalizeCountry(originCountry);
        String buyer = normalizeCountry(buyerCountry);
        boolean indiaToPak = "india".equals(origin) && "pakistan".equals(buyer);
        boolean pakToIndia = "pakistan".equals(origin) && "india".equals(buyer);
        if (indiaToPak || pakToIndia) {
            throw new IllegalArgumentException(
                    "Trade Restriction: Direct cross-border trade between India and Pakistan is restricted under platform trade policy.");
        }
    }

    private String normalizeCountry(String country) {
        if (country == null) return "";
        String c = country.trim().toLowerCase();
        if (c.contains("india")) return "india";
        if (c.contains("pakistan")) return "pakistan";
        return c;
    }

    /**
     * Dynamic Multi-Tier Bulk Pricing Algorithm:
     * - Qty < bulkMinQty: Retail price
     * - Qty >= bulkMinQty: Base wholesale price
     * - Qty >= 50 (and >= bulkMinQty): Additional 5% volume discount
     * - Qty >= 200 (and >= bulkMinQty): Additional 10% container/pallet discount
     */
    public Map<String, Object> calculateDynamicBulkPricing(Product p, int qty) {
        BigDecimal retail = p.getPrice();
        BigDecimal baseWholesale = p.getWholesalePrice() != null
                ? p.getWholesalePrice()
                : retail.multiply(new BigDecimal("0.85"));

        BigDecimal effectiveUnitPrice = retail;
        String tierName = "RETAIL";

        if (qty >= p.getBulkMinQty()) {
            effectiveUnitPrice = baseWholesale;
            tierName = "WHOLESALE_TIER_1";
            if (qty >= 200) {
                effectiveUnitPrice = baseWholesale.multiply(new BigDecimal("0.90"));
                tierName = "WHOLESALE_TIER_3_CONTAINER";
            } else if (qty >= 50) {
                effectiveUnitPrice = baseWholesale.multiply(new BigDecimal("0.95"));
                tierName = "WHOLESALE_TIER_2_BULK";
            }
        }

        effectiveUnitPrice = effectiveUnitPrice.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = effectiveUnitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal retailTotal = retail.multiply(BigDecimal.valueOf(qty));
        BigDecimal savings = retailTotal.subtract(total).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return Map.of(
                "unitPrice", effectiveUnitPrice,
                "totalAmount", total,
                "tier", tierName,
                "isWholesale", qty >= p.getBulkMinQty(),
                "savings", savings
        );
    }

    /**
     * Weighted Product Ranking Algorithm:
     * Combines demand (units sold), customer rating, and stock readiness.
     */
    public double computeTrendingScore(long unitsSold, double avgRating, int stockQuantity) {
        double demandScore = Math.min(1.0, unitsSold / 100.0) * 40.0;
        double ratingScore = (avgRating / 5.0) * 35.0;
        double stockScore = stockQuantity > 0 ? Math.min(1.0, stockQuantity / 500.0) * 25.0 : 0.0;
        return Math.round((demandScore + ratingScore + stockScore) * 10.0) / 10.0;
    }
}

package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record BranchSummary(
        String branch,
        BigDecimal revenue,
        BigDecimal discounts,
        long receipts,
        Map<String, BigDecimal> revenueByCategory,
        List<ProductTotal> topProducts,
        Map<PaymentMethod, BigDecimal> revenueByPayment) {
}
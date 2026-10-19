package edu.itc.salesreport.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

public record SaleTransaction(
        String branch,
        LocalDate date,
        String receiptNo,
        String sku,
        String productName,
        String category,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal discount,
        PaymentMethod paymentMethod) {

    public SaleTransaction {
        Objects.requireNonNull(unitPrice, "unitPrice");
        Objects.requireNonNull(discount, "discount");
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice must not be negative");
        }
        if (discount.signum() < 0) {
            throw new IllegalArgumentException("discount must not be negative");
        }
        BigDecimal lineAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        if (discount.compareTo(lineAmount) > 0) {
            throw new IllegalArgumentException("discount exceeds line amount");
        }
    }

    public BigDecimal revenue() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity))
                .subtract(discount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
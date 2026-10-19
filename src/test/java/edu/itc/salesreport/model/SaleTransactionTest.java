package edu.itc.salesreport.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SaleTransactionTest {

    private static SaleTransaction line(int qty, String price,
            String discount) {
        return new SaleTransaction("PNH", LocalDate.of(2026, 9, 1),
                "PNH-000001", "SKU-1001", "Jasmine Rice 5kg", "Grocery",
                qty, new BigDecimal(price), new BigDecimal(discount),
                PaymentMethod.KHQR);
    }

    @Test
    void revenueRoundsHalfUpToTwoDecimals() {
        // 3 x 0.335 = 1.005 -> 1.01 (HALF_UP), not 1.00 (HALF_EVEN)
        assertEquals(new BigDecimal("1.01"),
                line(3, "0.335", "0.00").revenue());
    }

    @Test
    void revenueSubtractsTheDiscount() {
        assertEquals(new BigDecimal("12.90"),
                line(2, "6.50", "0.10").revenue());
    }

    @Test
    void constructorRejectsBrokenInvariants() {
        assertThrows(IllegalArgumentException.class,
                () -> line(0, "6.50", "0.00"));
        assertThrows(IllegalArgumentException.class,
                () -> line(1, "6.50", "-0.01"));
        assertThrows(IllegalArgumentException.class,
                () -> line(1, "6.50", "6.51"));
    }

    @Test
    void branchSummaryCollectionsAreUnmodifiable() {
        var source = new ArrayList<>(List.of(new ProductTotal(
                "SKU-1001", "Jasmine Rice 5kg", new BigDecimal("13.00"), 2)));
        var summary = new BranchSummary("PNH", new BigDecimal("13.00"),
                new BigDecimal("0.00"), 1, Map.of(), source, Map.of());

        assertThrows(UnsupportedOperationException.class,
                () -> summary.topProducts().add(source.get(0)));
        // and a later change of the caller's list must not leak in
        source.clear();
        assertEquals(1, summary.topProducts().size());
    }

    @Test
    void averageBasketIsZeroWithoutReceipts() {
        var empty = new BranchSummary("PNH", new BigDecimal("0.00"),
                new BigDecimal("0.00"), 0, Map.of(), List.of(), Map.of());
        assertEquals(new BigDecimal("0.00"), empty.averageBasket());
    }
}

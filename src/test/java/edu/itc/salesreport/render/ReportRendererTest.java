package edu.itc.salesreport.render;

import edu.itc.salesreport.model.MonthlyReport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportRendererTest {

    private static String text(ReportRenderer r, MonthlyReport report) {
        return new String(r.render(report), StandardCharsets.UTF_8);
    }

    @ParameterizedTest
    @ValueSource(strings = {"txt", "html", "csv"})
    void everyFormatShowsAllBranchesAndTheChainTotal(String ext) {
        var renderer = ReportRenderer.forExtension(ext);
        assertEquals(ext, renderer.extension());

        String out = text(renderer, TestReports.september());

        for (String code : List.of("PNH", "REP", "BTB", "ALL")) {
            assertTrue(out.contains(code), ext + " lacks " + code);
        }
        assertTrue(out.contains("2300.50"), ext + " lacks the chain total");
    }

    @Test
    void csvHasHeaderAndExactLine() {
        String out = text(new CsvReportRenderer(), TestReports.september());
        var lines = out.lines().toList();

        assertEquals("month,branch,revenue,discounts,receipts,average_basket",
                lines.get(0));
        assertTrue(lines.contains("2026-09,REP,800.50,0.00,50,16.01"), out);
        assertEquals(5, lines.size());   // header + 3 branches + chain
    }

    @Test
    void htmlEscapesSpecialCharacters() {
        var report = new MonthlyReport(YearMonth.of(2026, 9),
                List.of(TestReports.branch("A&B<C>", "1.00", 1)),
                TestReports.branch("ALL", "1.00", 1), List.of());

        String out = text(new HtmlReportRenderer(), report);

        assertTrue(out.contains("A&amp;B&lt;C&gt;"), out);
        assertFalse(out.contains("A&B<C>"), out);
    }

    @Test
    void unknownExtensionIsRejected() {
        var e = assertThrows(IllegalArgumentException.class,
                () -> ReportRenderer.forExtension("pdf"));
        assertTrue(e.getMessage().contains("pdf"));
    }

    @Test
    void switchOverTheSealedHierarchyNeedsNoDefault() {
        ReportRenderer r = ReportRenderer.forExtension("csv");
        // compiles only while the switch covers every permitted subtype
        String kind = switch (r) {
            case TextReportRenderer t -> "text";
            case HtmlReportRenderer h -> "html";
            case CsvReportRenderer c -> "csv";
        };
        assertEquals("csv", kind);
    }
}

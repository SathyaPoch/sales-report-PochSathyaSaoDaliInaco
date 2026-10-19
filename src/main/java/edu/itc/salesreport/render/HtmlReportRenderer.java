package edu.itc.salesreport.render;

import edu.itc.salesreport.model.BranchSummary;
import edu.itc.salesreport.model.MonthlyReport;

import java.nio.charset.StandardCharsets;

/** Web page for branch managers: a table with one row per branch + chain. */
public final class HtmlReportRenderer implements ReportRenderer {

    @Override
    public String extension() {
        return "html";
    }

    @Override
    public byte[] render(MonthlyReport report) {
        String title = "Angkor Mart monthly sales " + report.month();
        var sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n")
          .append("<meta charset=\"UTF-8\">\n<title>")
          .append(escape(title)).append("</title>\n</head>\n<body>\n")
          .append("<h1>").append(escape(title)).append("</h1>\n")
          .append("<table>\n<thead><tr><th>Branch</th><th>Revenue</th>")
          .append("<th>Receipts</th><th>Average basket</th></tr></thead>\n")
          .append("<tbody>\n");
        for (BranchSummary b : report.branches()) {
            row(sb, b);
        }
        row(sb, report.chain());
        sb.append("</tbody>\n</table>\n");
        if (!report.anomalies().isEmpty()) {
            sb.append("<h2>Anomalies</h2>\n<ul>\n");
            for (String a : report.anomalies()) {
                sb.append("<li>").append(escape(a)).append("</li>\n");
            }
            sb.append("</ul>\n");
        }
        sb.append("</body>\n</html>\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void row(StringBuilder sb, BranchSummary b) {
        sb.append("<tr><td>").append(escape(b.branch()))
          .append("</td><td>").append(b.revenue())
          .append("</td><td>").append(b.receipts())
          .append("</td><td>").append(b.averageBasket())
          .append("</td></tr>\n");
    }

    /** Escapes the ampersand first, so later entities are not re-escaped. */
    static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
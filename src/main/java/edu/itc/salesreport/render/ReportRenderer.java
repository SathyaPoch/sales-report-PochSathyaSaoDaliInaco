package edu.itc.salesreport.render;

import edu.itc.salesreport.model.MonthlyReport;
import java.io.OutputStream;

public interface ReportRenderer {
    void render(MonthlyReport report, OutputStream outputStream);
}
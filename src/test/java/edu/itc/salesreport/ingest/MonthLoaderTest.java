package edu.itc.salesreport.ingest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthLoaderTest {

    private static final String OK1 =
            "PNH,2026-09-01,PNH-000001,SKU-1001,Jasmine Rice 5kg,Grocery,2,6.50,0.00,KHQR";
    private static final String OK2 =
            "PNH,2026-09-01,PNH-000002,SKU-2001,Mineral Water 1.5L,Beverages,1,0.45,0.00,CASH";
    private static final String BAD =
            "REP,2026-09-01,REP-000001,SKU-1001,Jasmine Rice 5kg,Grocery,two,6.50,0.00,CASH";

    private static void write(Path dir, String name, String... rows)
            throws IOException {
        var lines = new ArrayList<String>();
        lines.add(CsvTransactionParser.HEADER);
        lines.addAll(List.of(rows));
        Files.write(dir.resolve(name), lines);
    }

    private static Path month(Path root) throws IOException {
        return Files.createDirectories(root.resolve("2026-09"));
    }

    @Test
    void publishesOneEventPerFilePlusOneFinalEvent(@TempDir Path root)
            throws IOException {
        Path dir = month(root);
        write(dir, "PNH-2026-09-01.csv", OK1, OK2);
        write(dir, "REP-2026-09-01.csv", OK1, BAD);

        var events = new ArrayList<LoadEvent>();
        var loader = new MonthLoader(new CsvTransactionParser());
        loader.subscribe(events::add);

        var rows = loader.load(YearMonth.of(2026, 9), root);

        assertEquals(3, events.size());
        var first = assertInstanceOf(LoadEvent.FileLoaded.class, events.get(0));
        assertEquals("PNH-2026-09-01.csv", first.file().getFileName().toString());
        assertEquals(2, first.accepted());
        assertEquals(0, first.rejected());
        assertEquals(1, first.filesDone());
        assertEquals(2, first.filesTotal());
        var second = assertInstanceOf(LoadEvent.FileLoaded.class, events.get(1));
        assertEquals("REP-2026-09-01.csv", second.file().getFileName().toString());
        assertEquals(1, second.accepted());
        assertEquals(1, second.rejected());
        var last = assertInstanceOf(LoadEvent.LoadFinished.class, events.get(2));
        assertEquals(3, last.accepted());
        assertEquals(1, last.rejected());

        assertEquals(3, rows.size());
        assertEquals(List.of("REP-2026-09-01.csv:3 bad quantity 'two'"),
                loader.rejectedRows());
    }

    @Test
    void anUnsubscribedListenerReceivesNothing(@TempDir Path root)
            throws IOException {
        write(month(root), "PNH-2026-09-01.csv", OK1);

        var events = new ArrayList<LoadEvent>();
        var loader = new MonthLoader(new CsvTransactionParser());
        Runnable unsubscribe = loader.subscribe(events::add);
        unsubscribe.run();

        loader.load(YearMonth.of(2026, 9), root);

        assertTrue(events.isEmpty());
    }
}


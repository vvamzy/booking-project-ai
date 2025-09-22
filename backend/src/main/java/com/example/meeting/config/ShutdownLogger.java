package com.example.meeting.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

@Component
public class ShutdownLogger {
    private static final Logger log = LoggerFactory.getLogger(ShutdownLogger.class);

    @EventListener
    public void onContextClosed(ContextClosedEvent evt) {
        log.warn("Application context is closing. Capturing stack traces for debugging.");

        Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        pw.println("=== THREAD DUMP START ===");
        for (Map.Entry<Thread, StackTraceElement[]> entry : all.entrySet()) {
            Thread t = entry.getKey();
            pw.printf("\n--- Thread: %s (id=%d) state=%s daemon=%s%n", t.getName(), t.getId(), t.getState(), t.isDaemon());
            for (StackTraceElement ste : entry.getValue()) {
                pw.println("    at " + ste.toString());
            }
        }
        pw.println("=== THREAD DUMP END ===");
        pw.flush();

        log.warn(sw.toString());
    }
}

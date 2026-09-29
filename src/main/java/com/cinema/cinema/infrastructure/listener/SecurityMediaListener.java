package com.cinema.cinema.infrastructure.listener;

import com.cinema.cinema.domain.event.MediaUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class SecurityMediaListener {

    private static final Logger log = LoggerFactory.getLogger(SecurityMediaListener.class);
    private final AtomicInteger processedEventsCount = new AtomicInteger(0);

    @Async
    @EventListener
    public void onMediaUploaded(MediaUploadedEvent event) {
        log.info("Received MediaUploadedEvent asynchronously for file: {}, type: {}, size: {} bytes",
                event.filename(), event.contentType(), event.size());
        processedEventsCount.incrementAndGet();
    }

    public int getProcessedEventsCount() {
        return processedEventsCount.get();
    }
}

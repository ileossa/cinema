package com.cinema.cinema.infrastructure.adapter.out.event;

import com.cinema.cinema.domain.port.out.EventPublisherOut;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringEventPublisherAdapter implements EventPublisherOut {

    private final ApplicationEventPublisher applicationEventPublisher;

    public SpringEventPublisherAdapter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public void publishEvent(Object event) {
        if (event != null && applicationEventPublisher != null) {
            applicationEventPublisher.publishEvent(event);
        }
    }
}

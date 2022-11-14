package org.openmrs.eip.app.logger.observer;

import ch.qos.logback.classic.spi.ILoggingEvent;

public interface EventListener {
    void update(ILoggingEvent event);
}
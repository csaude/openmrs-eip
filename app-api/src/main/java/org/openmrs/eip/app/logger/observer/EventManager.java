package org.openmrs.eip.app.logger.observer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class EventManager {
	
	private Map<String, List<EventListener>> listenersMap = new HashMap<>();
	
	public EventManager(String... appenders) {
		for (String appender : appenders) {
			this.listenersMap.put(appender, new ArrayList<>());
		}
	}
	
	public void subscribe(String appender, EventListener listener) {
		List<EventListener> listeners = listenersMap.get(appender);
		listeners.add(listener);
	}
	
	public void unsubscribe(String appender, EventListener listener) {
		List<EventListener> listeners = listenersMap.get(appender);
		listeners.remove(listener);
	}
	
	public void notify(String appender, ILoggingEvent event) {
		List<EventListener> listeners = listenersMap.get(appender);
		for (EventListener listener : listeners) {
			listener.update(event);
		}
	}
}

package org.openmrs.eip.app.logger;

import org.openmrs.eip.app.logger.observer.EventManager;
import org.openmrs.eip.app.sender.InvalidBinlogOffsetPositionAutoRecovery;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class AppenderNotifier {
	
	private static final EventManager eventManager;
	
	static {
		eventManager = new EventManager(BinlogReaderOffsetErrorsAppender.class.getName());
		
		eventManager.subscribe(BinlogReaderOffsetErrorsAppender.class.getName(),
		    new InvalidBinlogOffsetPositionAutoRecovery());
	}
	
	public static void notify(String appender, ILoggingEvent event) {
		eventManager.notify(appender, event);
	}
}

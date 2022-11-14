package org.openmrs.eip.app.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class BinlogReaderOffsetErrorsAppender extends AppenderBase<ILoggingEvent> {
	
	@Override
	protected void append(ILoggingEvent event) {
		AppenderNotifier.notify(this.getClass().getName(), event);
	}
	
}

package org.openmrs.eip.app.logger;

import org.openmrs.eip.app.sender.InvalidBinlogOffsetPositionAutoRecovery;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class BinlogReaderOffsetErrorsAppender extends AppenderBase<ILoggingEvent> {
	
	private InvalidBinlogOffsetPositionAutoRecovery binlogOffsetPositionAutoRecovery;
	
	public BinlogReaderOffsetErrorsAppender() {
		this.binlogOffsetPositionAutoRecovery = new InvalidBinlogOffsetPositionAutoRecovery();
	}
	
	@Override
	protected void append(ILoggingEvent event) {
		this.binlogOffsetPositionAutoRecovery.accept(event);
	}
	
}

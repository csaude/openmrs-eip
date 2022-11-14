package org.openmrs.eip.app.logger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import io.debezium.connector.mysql.BinlogReader;

public class BinlogReaderOffsetErrorsAppenderFilter extends Filter<ILoggingEvent> {
	
	@Override
	public FilterReply decide(ILoggingEvent event) {
		if (event.getLevel().equals(Level.ERROR) && event.getLoggerName().equals(BinlogReader.class.getName())
		        && containsAll(event.getMessage(), "Error during binlog processing", "offset", "position")) {
			return FilterReply.ACCEPT;
		} else {
			return FilterReply.DENY;
		}
	}
	
	private boolean containsAll(String inputString, String... items) {
		for (String item : items) {
			if (!inputString.contains(item)) {
				return false;
			}
		}
		return true;
	}
}

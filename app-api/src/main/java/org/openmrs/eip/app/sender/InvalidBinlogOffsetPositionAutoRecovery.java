package org.openmrs.eip.app.sender;

import java.time.LocalDateTime;
import java.util.function.Consumer;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.DebeziumOffset;
import org.openmrs.eip.app.management.repository.DebeziumOffsetRepository;
import org.openmrs.eip.component.SyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.ILoggingEvent;

public class InvalidBinlogOffsetPositionAutoRecovery implements Consumer<ILoggingEvent> {
	
	private static final Logger logger = LoggerFactory.getLogger(InvalidBinlogOffsetPositionAutoRecovery.class);
	
	private DebeziumOffsetRepository debeziumOffsetRepository;
	
	public InvalidBinlogOffsetPositionAutoRecovery() {
		this.debeziumOffsetRepository = SyncContext.getBean(DebeziumOffsetRepository.class);
	}
	
	@Override
	public void accept(ILoggingEvent event) {
		logger.info("Starting recovering process. Log Event: {}", event);
		
		DebeziumOffset offset = debeziumOffsetRepository.findFirstByEnabledTrueOrderByDateCreatedDesc();
		int disabledOffsets = debeziumOffsetRepository.disableAllByBinlogFileName(offset.getBinlogFileName(),
		    LocalDateTime.now());
		
		logger.info("{} offsets pointing to the binlog_filename \"{}\" was disabled", disabledOffsets,
		    offset.getBinlogFileName());
		
		logger.info("Ending recovering process. A gracefull shutdown will be executed to complete the process.");
		AppUtils.shutdown();
	}
	
}

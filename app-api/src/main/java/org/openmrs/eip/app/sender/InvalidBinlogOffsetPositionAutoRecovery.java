package org.openmrs.eip.app.sender;

import java.time.LocalDateTime;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.logger.observer.EventListener;
import org.openmrs.eip.app.management.entity.DebeziumOffset;
import org.openmrs.eip.app.management.repository.DebeziumOffsetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ch.qos.logback.classic.spi.ILoggingEvent;

@Component
public class InvalidBinlogOffsetPositionAutoRecovery implements EventListener {
	
	private static DebeziumOffsetRepository debeziumOffsetRepository;
	
	private static final Logger logger = LoggerFactory.getLogger(InvalidBinlogOffsetPositionAutoRecovery.class);
	
	public InvalidBinlogOffsetPositionAutoRecovery() {
	}
	
	@Autowired
	public InvalidBinlogOffsetPositionAutoRecovery(DebeziumOffsetRepository debeziumOffsetRepository) {
		InvalidBinlogOffsetPositionAutoRecovery.debeziumOffsetRepository = debeziumOffsetRepository;
	}
	
	@Override
	public void update(ILoggingEvent event) {
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

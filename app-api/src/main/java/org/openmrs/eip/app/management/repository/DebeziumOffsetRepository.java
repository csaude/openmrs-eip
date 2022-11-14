package org.openmrs.eip.app.management.repository;

import java.time.LocalDateTime;

import org.openmrs.eip.app.management.entity.DebeziumOffset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface DebeziumOffsetRepository extends JpaRepository<DebeziumOffset, Long> {
	
	long countByEnabledTrue();
	
	DebeziumOffset findFirstByEnabledTrueOrderByDateCreatedDesc();
	
	@Query(value = "UPDATE DebeziumOffset SET enabled = false, dateChanged = :dateChanged WHERE binlogFileName = :binlogFileName")
	@Modifying
	@Transactional
	int disableAllByBinlogFileName(@Param("binlogFileName") String binlogFileName,
	        @Param("dateChanged") LocalDateTime dateChanged);
}

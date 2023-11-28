package org.openmrs.eip.component.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.AttributeOverride;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "clinical_summary_usage_report")
@AttributeOverride(name = "id", column = @Column(name = "clinical_summary_usage_report_id"))
public class ClinicalSummaryUsageReport extends BaseChangeableDataEntity {
	
	@Column(name = "report", length = 200)
	private String report;
	
	@Column(name = "health_facility", length = 200)
	private String healthFacility;
	
	@Column(name = "username", length = 200)
	private String username;
	
	@Column(name = "confidential_terms", length = 200)
	private String confidentialTerms;
	
	@Column(name = "app_version", length = 200)
	private String appversion;
	
	@Column(name = "date_opened")
	private LocalDateTime dateOpened;
	
}

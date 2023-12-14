package org.openmrs.eip.component.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "clinicalsummary_usage_report")
public class ClinicalSummaryUsageReport extends BaseChangeableDataEntity {
	
	@Column(name = "report")
	private String report;
	
	@Column(name = "health_facility")
	private String healthFacility;
	
	@Column(name = "user_name")
	private String username;
	
	@Column(name = "confidential_terms")
	private String confidentialTerms;
	
	@Column(name = "app_version")
	private String appversion;
	
	@Column(name = "date_opened")
	private LocalDateTime dateOpened;
	
}

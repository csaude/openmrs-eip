package org.openmrs.eip.component.model;

import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
public class ClinicalSummaryUsageReportModel extends BaseChangeableDataModel {
	
	private String report;
	
	private String healthFacility;
	
	private String username;
	
	private String confidentialTerms;
	
	private String appversion;
	
	private LocalDate dateOpened;
	
	// Getter and Setter methods for each field
	
	public String getReport() {
		return report;
	}
	
	public void setReport(String report) {
		this.report = report;
	}
	
	public String getHealthFacility() {
		return healthFacility;
	}
	
	public void setHealthFacility(String healthFacility) {
		this.healthFacility = healthFacility;
	}
	
	public String getUsername() {
		return username;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public String getConfidentialTerms() {
		return confidentialTerms;
	}
	
	public void setConfidentialTerms(String confidentialTerms) {
		this.confidentialTerms = confidentialTerms;
	}
	
	public String getAppversion() {
		return appversion;
	}
	
	public void setAppversion(String appversion) {
		this.appversion = appversion;
	}
	
	public LocalDate getDateOpened() {
		return dateOpened;
	}
	
	public void setDateOpened(LocalDate dateOpened) {
		this.dateOpened = dateOpened;
	}
}

package org.openmrs.eip.app.management.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "debezium_offset")
public class DebeziumOffset extends AbstractEntity {
	
	public static final long serialVersionUID = 1;
	
	@Column(nullable = false)
	private byte[] data;
	
	@Column(name = "binlog_filename", nullable = false)
	private String binlogFileName;
	
	@Column(name = "date_changed")
	private LocalDateTime dateChanged;
	
	@Column(name = "enabled", nullable = false)
	private Boolean enabled;
	
	public byte[] getData() {
		return data;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public String getBinlogFileName() {
		return binlogFileName;
	}
	
	public void setBinlogFileName(String binlogFileName) {
		this.binlogFileName = binlogFileName;
	}
	
	public LocalDateTime getDateChanged() {
		return dateChanged;
	}
	
	public void setDateChanged(LocalDateTime dateChanged) {
		this.dateChanged = dateChanged;
	}
	
	public Boolean isEnabled() {
		return enabled;
	}
	
	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}
	
	@Override
	public String toString() {
		return "DebeziumOffset [getId()=" + getId() + ", getDateCreated()=" + getDateCreated() + ", binlogFileName="
		        + binlogFileName + ", dateChanged=" + dateChanged + ", enabled=" + enabled + "]";
	}
	
}

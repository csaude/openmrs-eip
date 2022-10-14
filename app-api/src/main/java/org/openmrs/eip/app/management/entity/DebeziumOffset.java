package org.openmrs.eip.app.management.entity;

import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "debezium_offset")
public class DebeziumOffset extends AbstractEntity {
	
	public static final long serialVersionUID = 1;
	
	@Column(name = "date_changed")
	private LocalDateTime dateChanged;
	
	@Column(nullable = false)
	private byte[] data;
	
	public LocalDateTime getDateChanged() {
		return dateChanged;
	}
	
	public void setDateChanged(LocalDateTime dateChanged) {
		this.dateChanged = dateChanged;
	}
	
	public byte[] getData() {
		return data;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	@Override
	public String toString() {
		return "DebeziumOffset [getId()=" + getId() + ", getDateCreated()=" + getDateCreated() + ", dateChanged="
		        + dateChanged + "]";
	}
	
}

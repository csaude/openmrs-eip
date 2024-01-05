package org.openmrs.eip.app.management.entity.sender;

import java.util.Map;

import org.openmrs.eip.app.management.entity.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "sender_event")
public class Event extends AbstractEntity {
	
	private static final long serialVersionUID = 1L;
	
	@Column(name = "table_name", nullable = false, updatable = false, length = 100)
	@NotBlank
	private String tableName;
	
	//The primary key value of the affected row
	@Column(name = "primary_key_id", updatable = false)
	@NotBlank
	private String primaryKeyId;
	
	//Unique identifier for the entity usually a uuid or name for an entity like a privilege that has no uuid
	@Column
	private String identifier;
	
	@Column(nullable = false, updatable = false, length = 1)
	@NotBlank
	private String operation;
	
	@Column(nullable = false, updatable = false)
	@NotNull
	private Boolean snapshot = Boolean.FALSE;
	
	@Column(name = "request_uuid", updatable = false, length = 38)
	private String requestUuid;
	
	@Transient
	private Map beforeState;
	
	@Transient
	private Map afterState;
	
	/**
	 * Gets the identifier
	 *
	 * @return the identifier
	 */
	public String getIdentifier() {
		return identifier;
	}
	
	/**
	 * Sets the identifier
	 *
	 * @param identifier the identifier to set
	 */
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	/**
	 * Gets the primaryKeyId
	 *
	 * @return the primaryKeyId
	 */
	public String getPrimaryKeyId() {
		return primaryKeyId;
	}
	
	/**
	 * Sets the primaryKeyId
	 *
	 * @param primaryKeyId the primaryKeyId to set
	 */
	public void setPrimaryKeyId(String primaryKeyId) {
		this.primaryKeyId = primaryKeyId;
	}
	
	/**
	 * Gets the tableName
	 *
	 * @return the tableName
	 */
	public String getTableName() {
		return tableName;
	}
	
	/**
	 * Sets the tableName
	 *
	 * @param tableName the tableName to set
	 */
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	/**
	 * Gets the operation
	 *
	 * @return the operation
	 */
	public String getOperation() {
		return operation;
	}
	
	/**
	 * Sets the operation
	 *
	 * @param operation the operation to set
	 */
	public void setOperation(String operation) {
		this.operation = operation;
	}
	
	/**
	 * Gets the snapshot
	 *
	 * @return the snapshot
	 */
	public Boolean getSnapshot() {
		return snapshot;
	}
	
	/**
	 * Sets the snapshot
	 *
	 * @param snapshot the snapshot to set
	 */
	public void setSnapshot(Boolean snapshot) {
		this.snapshot = snapshot;
	}
	
	/**
	 * Gets the requestUuid
	 *
	 * @return the requestUuid
	 */
	public String getRequestUuid() {
		return requestUuid;
	}
	
	/**
	 * Sets the requestUuid
	 *
	 * @param requestUuid the requestUuid to set
	 */
	public void setRequestUuid(String requestUuid) {
		this.requestUuid = requestUuid;
	}
	
	/**
	 * Gets the beforeState
	 *
	 * @return the beforeState
	 */
	public Map getBeforeState() {
		return beforeState;
	}
	
	/**
	 * Sets the beforeState
	 *
	 * @param beforeState the beforeState to set
	 */
	public void setBeforeState(Map beforeState) {
		this.beforeState = beforeState;
	}
	
	/**
	 * Gets the afterState
	 *
	 * @return the afterState
	 */
	public Map getAfterState() {
		return afterState;
	}
	
	/**
	 * Sets the afterState
	 *
	 * @param afterState the afterState to set
	 */
	public void setAfterState(Map afterState) {
		this.afterState = afterState;
	}
	
	@Override
	public String toString() {
		return "Event {tableName=" + tableName + ", primaryKeyId=" + primaryKeyId + ", identifier=" + identifier
		        + ", operation=" + operation + ", snapshot=" + snapshot + ", requestUuid=" + requestUuid + "}";
	}
	
}

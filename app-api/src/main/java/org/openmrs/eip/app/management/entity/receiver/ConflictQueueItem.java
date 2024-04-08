package org.openmrs.eip.app.management.entity.receiver;

import java.time.LocalDateTime;
import java.util.Date;

import org.openmrs.eip.app.SyncOperationConverter;
import org.openmrs.eip.app.management.entity.AbstractEntity;
import org.openmrs.eip.component.SyncOperation;
import org.springframework.beans.BeanUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "receiver_conflict_queue")
public class ConflictQueueItem extends AbstractEntity {
	
	public static final long serialVersionUID = 1;
	
	@NotNull
	@Column(name = "model_class_name", nullable = false, updatable = false)
	private String modelClassName;
	
	//Unique identifier for the entity usually a uuid or name for an entity like a privilege that has no uuid
	@NotNull
	@Column(nullable = false, updatable = false)
	private String identifier;
	
	@NotNull
	@Column(nullable = false, updatable = false, length = 1)
	@Convert(converter = SyncOperationConverter.class)
	private SyncOperation operation;
	
	@NotNull
	@Column(name = "is_snapshot", nullable = false, updatable = false)
	private Boolean snapshot = false;
	
	@NotNull
	@Column(name = "entity_payload", columnDefinition = "text", nullable = false, updatable = false)
	private String entityPayload;
	
	@ManyToOne
	@JoinColumn(name = "site_id", updatable = false)
	private SiteInfo site;
	
	@NotNull
	@Column(name = "date_sent_by_sender", nullable = false, updatable = false)
	private LocalDateTime dateSentBySender;
	
	@Column(name = "message_uuid", length = 38, updatable = false)
	private String messageUuid;
	
	@Column(name = "date_received", nullable = false, updatable = false)
	private Date dateReceived;
	
	@Column(name = "sync_version", length = 20, updatable = false)
	@Getter
	@Setter
	private String syncVersion;
	
	public ConflictQueueItem() {
	}
	
	public ConflictQueueItem(SyncMessage syncMessage) {
		BeanUtils.copyProperties(syncMessage, this, "id", "dateCreated");
		setDateCreated(new Date());
	}
	
	public ConflictQueueItem(ReceiverRetryQueueItem retry) {
		BeanUtils.copyProperties(retry, this, "id", "dateCreated");
		setDateCreated(new Date());
	}
	
	/**
	 * Gets the modelClassName
	 *
	 * @return the modelClassName
	 */
	public String getModelClassName() {
		return modelClassName;
	}
	
	/**
	 * Sets the modelClassName
	 *
	 * @param modelClassName the modelClassName to set
	 */
	public void setModelClassName(String modelClassName) {
		this.modelClassName = modelClassName;
	}
	
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
	 * Gets the operation
	 *
	 * @return the operation
	 */
	public SyncOperation getOperation() {
		return operation;
	}
	
	/**
	 * Sets the operation
	 *
	 * @param operation the operation to set
	 */
	public void setOperation(SyncOperation operation) {
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
	 * Gets the entityPayload
	 *
	 * @return the entityPayload
	 */
	public String getEntityPayload() {
		return entityPayload;
	}
	
	/**
	 * Sets the entityPayload
	 *
	 * @param entityPayload the entityPayload to set
	 */
	public void setEntityPayload(String entityPayload) {
		this.entityPayload = entityPayload;
	}
	
	/**
	 * Gets the site
	 *
	 * @return the site
	 */
	public SiteInfo getSite() {
		return site;
	}
	
	/**
	 * Sets the site
	 *
	 * @param site the site to set
	 */
	public void setSite(SiteInfo site) {
		this.site = site;
	}
	
	/**
	 * Gets the dateSentBySender
	 *
	 * @return the dateSentBySender
	 */
	public LocalDateTime getDateSentBySender() {
		return dateSentBySender;
	}
	
	/**
	 * Sets the dateSentBySender
	 *
	 * @param dateSentBySender the dateSentBySender to set
	 */
	public void setDateSentBySender(LocalDateTime dateSentBySender) {
		this.dateSentBySender = dateSentBySender;
	}
	
	/**
	 * Gets the messageUuid
	 *
	 * @return the messageUuid
	 */
	public String getMessageUuid() {
		return messageUuid;
	}
	
	/**
	 * Sets the messageUuid
	 *
	 * @param messageUuid the messageUuid to set
	 */
	public void setMessageUuid(String messageUuid) {
		this.messageUuid = messageUuid;
	}
	
	/**
	 * Gets the dateReceived
	 *
	 * @return the dateReceived
	 */
	public Date getDateReceived() {
		return dateReceived;
	}
	
	/**
	 * Sets the dateReceived
	 *
	 * @param dateReceived the dateReceived to set
	 */
	public void setDateReceived(Date dateReceived) {
		this.dateReceived = dateReceived;
	}
	
	@Override
	public String toString() {
		return "ConflictQueueItem {identifier=" + identifier + ", modelClassName=" + modelClassName + ", operation="
		        + operation + ", snapshot=" + snapshot + ", site=" + site + ", dateSentBySender=" + dateSentBySender
		        + ", messageUuid=" + messageUuid + ", dateReceived=" + dateReceived + "}";
	}
	
}

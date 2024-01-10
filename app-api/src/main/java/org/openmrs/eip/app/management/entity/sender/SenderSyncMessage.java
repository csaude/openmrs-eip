package org.openmrs.eip.app.management.entity.sender;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.openmrs.eip.app.management.entity.AbstractEntity;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sender_sync_message")
public class SenderSyncMessage extends AbstractEntity {
	
	private static final long serialVersionUID = 1L;
	
	public enum SenderSyncMessageStatus {
		NEW, SENT
	}
	
	@OneToOne(optional = false, cascade = CascadeType.REMOVE)
	@JoinColumn(name = "event_id", unique = true, nullable = false, updatable = false)
	@NotNull
	@Getter
	@Setter
	private Event event;
	
	@NotNull
	@Column(name = "message_uuid", length = 38, nullable = false, unique = true, updatable = false)
	private String messageUuid;
	
	@Column(name = "sync_data", columnDefinition = "text")
	private String data;
	
	@NotNull
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	@Access(AccessType.FIELD)
	private SenderSyncMessageStatus status = SenderSyncMessageStatus.NEW;
	
	@Column(name = "date_sent")
	@Access(AccessType.FIELD)
	private Date dateSent;
	
	public String getMessageUuid() {
		return messageUuid;
	}
	
	public void setMessageUuid(String messageUuid) {
		this.messageUuid = messageUuid;
	}
	
	public String getData() {
		return data;
	}
	
	public void setData(String data) {
		this.data = data;
	}
	
	public SenderSyncMessageStatus getStatus() {
		return status;
	}
	
	public Date getDateSent() {
		return dateSent;
	}
	
	public void markAsSent(LocalDateTime dateSent) {
		this.status = SenderSyncMessageStatus.SENT;
		this.dateSent = Date.from(dateSent.atZone(ZoneId.systemDefault()).toInstant());
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " {id=" + getId() + ", event=" + event + "}";
	}
	
}

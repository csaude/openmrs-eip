package org.openmrs.eip.app.management.entity.sender;

import org.openmrs.eip.app.management.entity.BaseRetryQueueItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "sender_retry_queue")
public class SenderRetryQueueItem extends BaseRetryQueueItem {
	
	public static final long serialVersionUID = 1;
	
	@OneToOne(optional = false, cascade = CascadeType.ALL)
	@JoinColumn(name = "event_id", unique = true, nullable = false, updatable = false)
	@NotNull
	private Event event;
	
	/**
	 * Gets the event
	 *
	 * @return the event
	 */
	public Event getEvent() {
		return event;
	}
	
	/**
	 * Sets the event
	 *
	 * @param event the event to set
	 */
	public void setEvent(Event event) {
		this.event = event;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " {attemptCount=" + getAttemptCount() + ", event=" + event + "}";
	}
	
}

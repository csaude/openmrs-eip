package org.openmrs.eip.app.management.entity.sender;

import org.openmrs.eip.app.management.entity.AbstractEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "debezium_event_queue")
public class DebeziumEvent extends AbstractEntity {
	
	private static final long serialVersionUID = -1884382844867650350L;
	
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
		return getClass().getSimpleName() + " {id=" + getId() + ", event=" + event + "}";
	}
	
}

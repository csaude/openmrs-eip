package org.openmrs.eip.app.management.entity.receiver;

import org.openmrs.eip.app.management.entity.AbstractEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@MappedSuperclass
public abstract class BaseUnSyncedEntity extends AbstractEntity {
	
	@Column(name = "table_name", nullable = false, updatable = false, length = 100)
	@NotBlank
	@Getter
	@Setter
	private String tableName;
	
	@NotBlank
	@Column(nullable = false, updatable = false)
	@Getter
	@Setter
	private String identifier;
	
	@ManyToOne(optional = false)
	@JoinColumn(name = "site_id", nullable = false, updatable = false)
	@Getter
	@Setter
	private SiteInfo site;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "last_seen_queue")
	@Getter
	@Setter
	private ReceiverQueue lastSeenQueue;
	
}

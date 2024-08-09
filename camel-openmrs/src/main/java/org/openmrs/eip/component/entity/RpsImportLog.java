package org.openmrs.eip.component.entity;

import java.time.LocalDateTime;

import org.openmrs.eip.component.entity.light.UserLight;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "esaudefeatures_rps_import_log")
@AttributeOverride(name = "id", column = @Column(name = "rps_import_log_id"))
public class RpsImportLog extends BaseEntity {
	
	@Transient
	private UserLight creator;
	
	@NotNull
	@Column(name = "patient_uuid", nullable = false)
	private String patient;
	
	@NotNull
	@Column(name = "health_facility", nullable = false)
	private String healthFacility;
	
	@NotNull
	@Column(name = "importer_uuid", nullable = false)
	private String importer;
	
	@NotNull
	@Column(name = "importer_username", nullable = false)
	private String importerUsername;
	
	@NotNull
	@Column(name = "date_imported")
	private LocalDateTime dateImported;
	
	@Override
	public boolean wasModifiedAfter(BaseEntity entity) {
		return false;
	}
	
}

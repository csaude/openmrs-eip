package org.openmrs.eip.component.entity.light;

import java.time.LocalDateTime;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "gaac")
@AttributeOverride(name = "id", column = @Column(name = "gaac_id"))
public class GaacLight extends VoidableLightEntity {
	
	@NotNull
	@Column(name = "name")
	private String name;
	
	@NotNull
	@Column(name = "start_date")
	protected LocalDateTime startDate;
	
	@NotNull
	@ManyToOne
	@JoinColumn(name = "affinity_type")
	private GaacAffinityTypeLight affinityType;
	
	@NotNull
	@ManyToOne
	@JoinColumn(name = "location_id")
	private LocationLight location;
}

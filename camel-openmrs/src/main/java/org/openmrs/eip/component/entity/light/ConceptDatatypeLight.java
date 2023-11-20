package org.openmrs.eip.component.entity.light;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "concept_datatype")
@AttributeOverride(name = "id", column = @Column(name = "concept_datatype_id"))
public class ConceptDatatypeLight extends RetireableLightEntity {
	
	@NotNull
	@Column(name = "name")
	private String name;
}

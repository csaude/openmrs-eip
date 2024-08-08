package org.openmrs.eip.component.model;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RpsImportLogModel extends BaseModel {
	
	private String patientUuid;
	
	private String healthFacility;
	
	private String importerUuid;
	
	private String importerUsername;
	
	private LocalDateTime dateCreated;
	
}

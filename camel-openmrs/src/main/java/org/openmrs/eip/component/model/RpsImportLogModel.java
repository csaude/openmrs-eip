package org.openmrs.eip.component.model;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RpsImportLogModel extends BaseModel {
	
	private String patient;
	
	private String healthFacility;
	
	private String importer;
	
	private String importerUsername;
	
	private LocalDateTime dateImported;
	
}

package org.openmrs.eip.component.service.impl;

import org.openmrs.eip.component.entity.RpsImportLog;
import org.openmrs.eip.component.mapper.EntityToModelMapper;
import org.openmrs.eip.component.mapper.ModelToEntityMapper;
import org.openmrs.eip.component.model.RpsImportLogModel;
import org.openmrs.eip.component.repository.SyncEntityRepository;
import org.openmrs.eip.component.service.AbstractEntityService;
import org.openmrs.eip.component.service.TableToSyncEnum;
import org.springframework.stereotype.Service;

@Service
public class RpsImportLogService extends AbstractEntityService<RpsImportLog, RpsImportLogModel> {
	
	public RpsImportLogService(final SyncEntityRepository<RpsImportLog> repository,
	    final EntityToModelMapper<RpsImportLog, RpsImportLogModel> entityToModelMapper,
	    final ModelToEntityMapper<RpsImportLogModel, RpsImportLog> modelToEntityMapper) {
		
		super(repository, entityToModelMapper, modelToEntityMapper);
	}
	
	@Override
	public TableToSyncEnum getTableToSync() {
		return TableToSyncEnum.ROS_IMPORT_LOG;
	}
	
}

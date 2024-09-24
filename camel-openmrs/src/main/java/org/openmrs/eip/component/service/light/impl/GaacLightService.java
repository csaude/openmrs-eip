package org.openmrs.eip.component.service.light.impl;

import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.entity.light.GaacAffinityTypeLight;
import org.openmrs.eip.component.entity.light.GaacLight;
import org.openmrs.eip.component.entity.light.LocationLight;
import org.openmrs.eip.component.repository.OpenmrsRepository;
import org.openmrs.eip.component.service.light.AbstractLightService;
import org.openmrs.eip.component.service.light.LightService;
import org.springframework.stereotype.Service;

@Service
public class GaacLightService extends AbstractLightService<GaacLight> {
	
	private LightService<GaacAffinityTypeLight> affinityTypeService;
	
	private LightService<LocationLight> locationService;
	
	public GaacLightService(OpenmrsRepository<GaacLight> repository, LightService<GaacAffinityTypeLight> affinityTypeService,
	    LightService<LocationLight> locationService) {
		super(repository);
		this.affinityTypeService = affinityTypeService;
		this.locationService = locationService;
	}
	
	@Override
	protected GaacLight createPlaceholderEntity(final String uuid) {
		GaacLight gaac = new GaacLight();
		gaac.setAffinityType(affinityTypeService.getOrInitPlaceholderEntity());
		gaac.setLocation(locationService.getOrInitPlaceholderEntity());
		gaac.setDateCreated(DEFAULT_DATE);
		gaac.setCreator(SyncContext.getAppUser().getId());
		gaac.setName(DEFAULT_STRING);
		gaac.setStartDate(DEFAULT_DATE);
		return gaac;
	}
}

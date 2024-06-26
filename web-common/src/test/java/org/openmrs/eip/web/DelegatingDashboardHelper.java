package org.openmrs.eip.web;

import java.util.List;

import org.apache.camel.ProducerTemplate;
import org.openmrs.eip.component.SyncOperation;
import org.openmrs.eip.web.controller.DashboardHelper;

public class DelegatingDashboardHelper extends BaseDashboardHelper {
	
	private DashboardHelper delegate;
	
	public DelegatingDashboardHelper(ProducerTemplate producerTemplate) {
		super(producerTemplate);
	}
	
	@Override
	public String getCategorizationProperty(String entityType) {
		return delegate.getCategorizationProperty(entityType);
	}
	
	@Override
	public List<String> getCategories(String entityName) {
		return delegate.getCategories(entityName);
	}
	
	@Override
	public Integer getCount(String entityType, String category, SyncOperation op) {
		return delegate.getCount(entityType, category, op);
	}
	
}

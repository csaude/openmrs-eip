package org.openmrs.eip.web;

import org.apache.camel.ProducerTemplate;

public class TestDashboardHelper extends BaseDashboardHelper {
	
	private boolean isReceiver;
	
	public TestDashboardHelper(ProducerTemplate producerTemplate, boolean isReceiver) {
		super(producerTemplate);
		this.isReceiver = isReceiver;
	}
	
	@Override
	public String getCategorizationProperty(String entityType) {
		if (isReceiver) {
			return "modelClassName";
		}
		
		return "event.tableName";
	}
	
}

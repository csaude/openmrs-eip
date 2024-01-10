package org.openmrs.eip.web.sender;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.web.BaseDashboardHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(SyncProfiles.SENDER)
public class SenderDashboardHelper extends BaseDashboardHelper {
	
	protected CamelContext camelContext;
	
	@Autowired
	public SenderDashboardHelper(CamelContext camelContext, ProducerTemplate producerTemplate) {
		super(producerTemplate);
		this.camelContext = camelContext;
	}
	
	@Override
	public String getCategorizationProperty(String entityType) {
		return "event.tableName";
	}
	
}

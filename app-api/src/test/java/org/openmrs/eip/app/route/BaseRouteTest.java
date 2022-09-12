package org.openmrs.eip.app.route;

import org.junit.Before;
import org.openmrs.eip.BaseDbBackedCamelTest;
import org.openmrs.eip.TestConstants;
import org.openmrs.eip.component.Constants;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "logging.level.org.apache.camel.reifier.RouteReifier=WARN")
@TestPropertySource(properties = Constants.PROP_URI_ERROR_HANDLER + "=" + TestConstants.URI_ERROR_HANDLER)
public abstract class BaseRouteTest extends BaseDbBackedCamelTest {
	
	protected static final String ACTIVEMQ_IMAGE = "cnocorch/activemq-artemis";
	protected static final int ACTIVEMQ_PORT = 61616;
	
	public abstract String getTestRouteFilename();
	
	public abstract String getAppFolderName();
	
	@Before
	public void setupBaseRouteTest() throws Exception {
		if (getTestRouteFilename() != null) {
			loadRoute(getTestRouteFilename() + ".xml");
		}
	}
	
	protected void loadRoute(String routeFilename) throws Exception {
		loadXmlRoutes(getAppFolderName(), routeFilename);
	}
	
}

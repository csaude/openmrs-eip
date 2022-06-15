package org.openmrs.eip;

import static org.openmrs.eip.BaseDbBackedCamelTest.mysqlContainer;

import org.openmrs.eip.app.SyncConstants;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.env.MapPropertySource;

import com.mysql.jdbc.Driver;

/**
 * Test BeanPostProcessor that sets the dead letter uri for tests and injects the OpenMRS datasource
 * properties values after the {@link org.testcontainers.containers.MySQLContainer} has been started
 * and available. This is necessary primarily for setting the MySQL port and the jdbc url.
 */
public class AppPropertiesBeanPostProcessor implements BeanPostProcessor {
	
	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (SyncConstants.CUSTOM_PROP_SOURCE_BEAN_NAME.equals(beanName)) {
			MapPropertySource propSource = (MapPropertySource) bean;
			propSource.getSource().put("openmrs.db.port", BaseDbBackedCamelTest.mysqlPort);
			propSource.getSource().put("openmrs.db.host", "localhost");
			propSource.getSource().put("openmrs.db.name", mysqlContainer.getDatabaseName());
			propSource.getSource().put("spring.openmrs-datasource.jdbcUrl", mysqlContainer.getJdbcUrl() + "?useSSL=false");
			propSource.getSource().put("spring.openmrs-datasource.driverClassName", Driver.class.getName());
			propSource.getSource().put("spring.openmrs-datasource.username", "root");
			propSource.getSource().put("spring.openmrs-datasource.password", mysqlContainer.getPassword());
		}
		
		return bean;
	}
	
}

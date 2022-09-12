package org.openmrs.eip.app.management;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Makes application properties available for Liquibase ChangeSet classes (non spring beans)
 */
@Component(LiquibasePropertiesHelper.NAME)
public class LiquibasePropertiesHelper {
	
	public static final String NAME = "liquibasePropertiesHelper";
	
	private static Environment environment;
	
	public LiquibasePropertiesHelper(Environment environment) {
		LiquibasePropertiesHelper.environment = environment;
	}
	
	public static String getProperty(String key) {
		return environment.getProperty(key);
	}
	
}

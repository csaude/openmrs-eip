package org.openmrs.eip.app.config;

import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;

import liquibase.integration.spring.SpringLiquibase;

public class BeanAwareSpringLiquibase extends SpringLiquibase {
	
	private static ResourceLoader applicationContext;
	
	public static String getProperty(String key) throws Exception {
		if (ApplicationContext.class.isInstance(applicationContext)) {
			return ((ApplicationContext) applicationContext).getEnvironment().getProperty(key);
		} else {
			throw new Exception("Resource loader is not an instance of ApplicationContext");
		}
	}
	
	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		super.setResourceLoader(resourceLoader);
		applicationContext = resourceLoader;
	}
}

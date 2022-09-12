package org.openmrs.eip;

import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@EnableAutoConfiguration(exclude = { JmsAutoConfiguration.class })
@ComponentScan(excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = RouteBuilder.class) })
public class TestConfig {
	
	@Bean
	public DeadLetterChannelBuilder shutdownErrorHandler() {
		DeadLetterChannelBuilder builder = new DeadLetterChannelBuilder(TestConstants.URI_ERROR_HANDLER);
		builder.setUseOriginalMessage(true);
		return builder;
	}
	
}

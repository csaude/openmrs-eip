package org.openmrs.eip.app;

import java.security.Security;
import java.time.LocalDateTime;

import javax.annotation.PostConstruct;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.apache.camel.builder.NoErrorHandlerBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.openmrs.eip.app.config.JpaCamelConf;
import org.openmrs.eip.app.config.ManagementDataSourceConfig;
import org.openmrs.eip.app.config.OpenmrsDataSourceConfig;
import org.openmrs.eip.component.camel.StringToLocalDateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = "org.openmrs.eip", exclude = JmsAutoConfiguration.class)
@Import({ ManagementDataSourceConfig.class, OpenmrsDataSourceConfig.class, JpaCamelConf.class })
public class SyncApplication {
	
	protected static final Logger log = LoggerFactory.getLogger(SyncApplication.class);
	
	private CamelContext camelContext;
	
	public SyncApplication(final CamelContext camelContext) {
		this.camelContext = camelContext;
	}
	
	public static void main(final String[] args) {
		SpringApplication.run(SyncApplication.class, args);
	}
	
	@PostConstruct
	private void addTypeConverter() {
		camelContext.getTypeConverterRegistry().addTypeConverter(LocalDateTime.class, String.class,
		    new StringToLocalDateTimeConverter());
	}
	
	@PostConstruct
	private void addBCProvider() {
		Security.addProvider(new BouncyCastleProvider());
	}
	
	/**
	 * Bean to handle messages in error and re-route them to another route
	 *
	 * @return deadLetterChannelBuilder
	 */
	@Bean
	public DeadLetterChannelBuilder deadLetterChannelBuilder() {
		DeadLetterChannelBuilder builder = new DeadLetterChannelBuilder("direct:dlc");
		builder.setUseOriginalMessage(true);
		return builder;
	}
	
	@Bean("noErrorHandler")
	public NoErrorHandlerBuilder getNoErrorHandler() {
		return new NoErrorHandlerBuilder();
	}
	
	@Bean
	public DeadLetterChannelBuilder shutdownErrorHandler() {
		DeadLetterChannelBuilder builder = new DeadLetterChannelBuilder("direct:shutdown-route");
		builder.setUseOriginalMessage(true);
		return builder;
	}
	
}

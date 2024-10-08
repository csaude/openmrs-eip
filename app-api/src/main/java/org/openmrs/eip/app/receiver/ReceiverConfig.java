package org.openmrs.eip.app.receiver;

import static org.openmrs.eip.app.SyncConstants.PROP_SITE_PARALLEL_SIZE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.BEAN_NAME_SITE_EXECUTOR;
import static org.openmrs.eip.app.receiver.ReceiverConstants.DEFAULT_SITE_PARALLEL_SIZE;
import static org.openmrs.eip.component.Constants.CUSTOM_PROP_SOURCE_BEAN_NAME;
import static org.openmrs.eip.component.Constants.PROP_URI_ERROR_HANDLER;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.camel.builder.DeadLetterChannelBuilder;
import org.openmrs.eip.app.management.service.ReceiverReconcileService;
import org.openmrs.eip.app.receiver.reconcile.FullIndexerScheduler;
import org.openmrs.eip.app.receiver.reconcile.ReconcileScheduler;
import org.openmrs.eip.app.receiver.task.FullIndexer;
import org.openmrs.eip.component.SyncProfiles;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Profile(SyncProfiles.RECEIVER)
public class ReceiverConfig {
	
	@Bean(CUSTOM_PROP_SOURCE_BEAN_NAME)
	public PropertySource getReceiverPropertySource(ConfigurableEnvironment env) {
		PropertySource customPropSource = new MapPropertySource("receiverPropSource", new HashMap());
		env.getPropertySources().addLast(customPropSource);
		
		return customPropSource;
	}
	
	@Bean(ReceiverConstants.ERROR_HANDLER_REF)
	@DependsOn(CUSTOM_PROP_SOURCE_BEAN_NAME)
	public DeadLetterChannelBuilder getInBoundErrorHandler() {
		DeadLetterChannelBuilder builder = new DeadLetterChannelBuilder("{{" + PROP_URI_ERROR_HANDLER + "}}");
		builder.useOriginalMessage();
		return builder;
	}
	
	@Bean(BEAN_NAME_SITE_EXECUTOR)
	public ScheduledThreadPoolExecutor getSiteExecutor(@Value("${" + PROP_SITE_PARALLEL_SIZE + ":"
	        + DEFAULT_SITE_PARALLEL_SIZE + "}") int parallelSiteSize) {
		
		return (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(parallelSiteSize);
	}
	
	@Bean
	public ReconcileScheduler reconcileScheduler(ReceiverReconcileService service) {
		return new ReconcileScheduler(service);
	}
	
	@Bean
	public FullIndexerScheduler fullIndexerScheduler(FullIndexer indexer) {
		return new FullIndexerScheduler(indexer);
	}
	
}

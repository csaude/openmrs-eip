package org.openmrs.eip.app.receiver;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.openmrs.eip.app.SyncConstants.BEAN_NAME_SYNC_EXECUTOR;
import static org.openmrs.eip.app.SyncConstants.DEFAULT_DELAY_PRUNER;
import static org.openmrs.eip.app.SyncConstants.PROP_ARCHIVES_MAX_AGE_DAYS;
import static org.openmrs.eip.app.SyncConstants.PROP_DELAY_MSG_RECONCILER;
import static org.openmrs.eip.app.SyncConstants.PROP_DELAY_PRUNER;
import static org.openmrs.eip.app.SyncConstants.PROP_DELAY_RECONCILER;
import static org.openmrs.eip.app.SyncConstants.PROP_DELAY_RETRY_TASK;
import static org.openmrs.eip.app.SyncConstants.PROP_INITIAL_DELAY_MSG_RECONCILER;
import static org.openmrs.eip.app.SyncConstants.PROP_INITIAL_DELAY_PRUNER;
import static org.openmrs.eip.app.SyncConstants.PROP_INITIAL_DELAY_RECONCILER;
import static org.openmrs.eip.app.SyncConstants.PROP_INITIAL_DELAY_RETRY_TASK;
import static org.openmrs.eip.app.SyncConstants.PROP_PRUNER_ENABLED;
import static org.openmrs.eip.app.receiver.ReceiverConstants.BEAN_NAME_SITE_EXECUTOR;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_ARCHIVE_DISABLED;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_DELAY_ARCHIVE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_DELAY_JMS_MSG_TASK;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_DISABLED_SITES;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_ENABLED_SITES;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_INITIAL_DELAY_ARCHIVE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_INITIAL_DELAY_JMS_MSG_TASK;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_JMS_LISTENER_DISABLED;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_JMS_TASK_DISABLED;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_SITE_DISABLED_TASKS;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_SITE_TASK_DELAY;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_SITE_TASK_INITIAL_DELAY;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.BaseCamelListener;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.receiver.reconcile.ReceiverReconcileMsgTask;
import org.openmrs.eip.app.receiver.reconcile.ReceiverReconcileTask;
import org.openmrs.eip.app.receiver.task.ReceiverRetryTask;
import org.openmrs.eip.app.receiver.task.Synchronizer;
import org.openmrs.eip.component.Constants;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.SyncProfiles;
import org.openmrs.eip.component.entity.User;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.repository.UserRepository;
import org.openmrs.eip.component.repository.light.UserLightRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

@Component
@Profile(SyncProfiles.RECEIVER)
public class ReceiverCamelListener extends BaseCamelListener {
	
	protected static final Logger log = LoggerFactory.getLogger(ReceiverCamelListener.class);
	
	private static final int DEFAULT_INITIAL_DELAY_SYNC = 5000;
	
	private static final int DEFAULT_DELAY = 300000;
	
	private static final int DEFAULT_DELAY_JMS_MSG_TASK = 30000;
	
	private ScheduledThreadPoolExecutor siteExecutor;
	
	@Value("${" + PROP_INITIAL_DELAY_JMS_MSG_TASK + ":" + DEFAULT_INITIAL_DELAY_SYNC + "}")
	private long initialDelayMsgTsk;
	
	@Value("${" + PROP_DELAY_JMS_MSG_TASK + ":" + DEFAULT_DELAY_JMS_MSG_TASK + "}")
	private long delayMsgTask;
	
	@Value("${" + PROP_SITE_TASK_INITIAL_DELAY + ":" + DEFAULT_INITIAL_DELAY_SYNC + "}")
	private long siteTaskInitialDelay;
	
	@Value("${" + PROP_SITE_TASK_DELAY + ":" + DEFAULT_DELAY + "}")
	private long siteTaskDelay;
	
	@Value("${" + PROP_SITE_DISABLED_TASKS + ":}")
	private List<SiteChildTaskType> disabledTaskTypes;
	
	@Value("${" + PROP_INITIAL_DELAY_PRUNER + ":" + (DEFAULT_INITIAL_DELAY_SYNC + 55000) + "}")
	private long initialDelayPruner;
	
	@Value("${" + PROP_DELAY_PRUNER + ":" + DEFAULT_DELAY_PRUNER + "}")
	private long delayPruner;
	
	@Value("${" + PROP_PRUNER_ENABLED + ":false}")
	private boolean prunerEnabled;
	
	@Value("${" + PROP_ARCHIVES_MAX_AGE_DAYS + ":}")
	private Integer archivesMaxAgeInDays;
	
	@Value("${" + PROP_INITIAL_DELAY_RECONCILER + ":" + DEFAULT_INITIAL_DELAY_SYNC + "}")
	private long initDelayReconciler;
	
	@Value("${" + PROP_DELAY_RECONCILER + ":" + DEFAULT_DELAY + "}")
	private long delayReconciler;
	
	@Value("${" + PROP_INITIAL_DELAY_MSG_RECONCILER + ":" + DEFAULT_INITIAL_DELAY_SYNC + "}")
	private long initDelayMsgReconciler;
	
	@Value("${" + PROP_DELAY_MSG_RECONCILER + ":" + DEFAULT_DELAY + "}")
	private long delayMsgReconciler;
	
	@Value("${" + PROP_JMS_TASK_DISABLED + ":false}")
	private boolean jmsTaskDisabled;
	
	@Value("${" + PROP_ENABLED_SITES + ":}")
	private List<String> enabledSiteIdentifiers;
	
	@Value("${" + PROP_DISABLED_SITES + ":}")
	private List<String> disabledSiteIdentifiers;
	
	@Value("${" + PROP_INITIAL_DELAY_RETRY_TASK + ":" + DEFAULT_INITIAL_DELAY_SYNC + "}")
	private long initialDelayRetryTask;
	
	@Value("${" + PROP_DELAY_RETRY_TASK + ":" + DEFAULT_DELAY + "}")
	private long delayRetryTask;
	
	@Value("${" + ReceiverConstants.PROP_FULL_INDEXER_CRON + ":-}")
	private String fullIndexerCron;
	
	@Value("${" + PROP_ARCHIVE_DISABLED + ":false}")
	private boolean archiveTaskDisabled;
	
	@Value("${" + PROP_INITIAL_DELAY_ARCHIVE + ":60000}")
	private long initialDelayArchiveTask;
	
	@Value("${" + PROP_DELAY_ARCHIVE + ":" + DEFAULT_DELAY + "}")
	private long delayArchiveTask;
	
	private static List<SiteParentTask> siteTasks;
	
	@Value("${" + PROP_JMS_LISTENER_DISABLED + ":false}")
	private boolean msgListenerDisabled;
	
	public ReceiverCamelListener(@Qualifier(BEAN_NAME_SITE_EXECUTOR) ScheduledThreadPoolExecutor siteExecutor,
	    @Qualifier(BEAN_NAME_SYNC_EXECUTOR) ThreadPoolExecutor syncExecutor) {
		super(syncExecutor);
		this.siteExecutor = siteExecutor;
	}
	
	@Override
	public void applicationStarted() {
		if (!enabledSiteIdentifiers.isEmpty() && !disabledSiteIdentifiers.isEmpty()) {
			throw new EIPException(
			        "You can only set " + PROP_ENABLED_SITES + " or " + PROP_DISABLED_SITES + " but not both");
		}
		
		log.info("Loading OpenMRS user account");
		String username = SyncContext.getBean(Environment.class).getProperty(Constants.PROP_OPENMRS_USER);
		if (StringUtils.isBlank(username)) {
			throw new EIPException("No value set for application property: " + Constants.PROP_OPENMRS_USER);
		}
		
		UserRepository userRepo = SyncContext.getBean(UserRepository.class);
		User exampleUser = new User();
		exampleUser.setUsername(username);
		Example<User> example = Example.of(exampleUser, ExampleMatcher.matching().withIgnoreCase());
		Optional<User> optional = userRepo.findOne(example);
		if (!optional.isPresent()) {
			log.error("No user found with username: " + username);
			AppUtils.shutdown();
		}
		
		UserLightRepository userLightRepo = SyncContext.getBean(UserLightRepository.class);
		SyncContext.setAppUser(userLightRepo.findById(optional.get().getId()).get());
		
		log.info("Loading OpenMRS admin user account");
		exampleUser = new User();
		exampleUser.setUsername("admin");
		example = Example.of(exampleUser, ExampleMatcher.matching().withIgnoreCase());
		optional = userRepo.findOne(example);
		if (!optional.isPresent()) {
			log.error("No admin user found");
			AppUtils.shutdown();
		}
		
		SyncContext.setAdminUser(userLightRepo.findById(optional.get().getId()).get());
		
		Collection<SiteInfo> sites = ReceiverContext.getSites().stream().filter(s -> !s.getDisabled())
		        .collect(Collectors.toList());
		if (!enabledSiteIdentifiers.isEmpty()) {
			sites = sites.stream().filter(s -> enabledSiteIdentifiers.contains(s.getIdentifier()))
			        .collect(Collectors.toList());
		} else if (!disabledSiteIdentifiers.isEmpty()) {
			sites = sites.stream().filter(s -> !disabledSiteIdentifiers.contains(s.getIdentifier()))
			        .collect(Collectors.toList());
		}
		
		log.info("There are {} enabled sites", sites.size());
		
		if (log.isDebugEnabled()) {
			log.debug("Enabled sites: " + sites);
		}
		
		log.info("Starting tasks");
		
		startTasks();
		startSiteParentTasks(sites);
		
		if (prunerEnabled) {
			if (archivesMaxAgeInDays == null) {
				log.error(PROP_ARCHIVES_MAX_AGE_DAYS + " is required when " + PROP_PRUNER_ENABLED + " is set to true");
				AppUtils.shutdown();
			}
			
			log.info("Pruning sync archives older than " + archivesMaxAgeInDays + " days");
			
			startPrunerTask();
		}
		
		if (!msgListenerDisabled) {
			log.info("Starting JMS message listener");
			SyncContext.getBean(MessageListenerContainer.class).start();
		}
	}
	
	@Override
	public void applicationStopped() {
		if (siteTasks != null) {
			siteTasks.forEach(task -> {
				AppUtils.shutdownExecutor(task.getChildExecutor(),
				    task.getSiteInfo().getName() + " " + ReceiverConstants.CHILD_TASK_NAME, true);
			});
		}
		
		AppUtils.shutdownExecutor(siteExecutor, ReceiverConstants.PARENT_TASK_NAME, false);
	}
	
	private void startSiteParentTasks(Collection<SiteInfo> sites) {
		List<Class<? extends Runnable>> disabledTaskClasses = disabledTaskTypes.stream().map(t -> t.getTaskClass())
		        .collect(Collectors.toList());
		
		siteTasks = new ArrayList(sites.size());
		
		if (hasAtLeastOneEnabledTask(disabledTaskClasses, !"-".equals(fullIndexerCron))) {
			sites.stream().forEach(site -> {
				SiteParentTask t = new SiteParentTask(site, disabledTaskClasses, !"-".equals(fullIndexerCron));
				siteExecutor.scheduleWithFixedDelay(t, siteTaskInitialDelay, siteTaskDelay, MILLISECONDS);
				siteTasks.add(t);
			});
		} else {
			log.warn("All tasks are disabled. No siteTask will be created");
		}
	}
	
	private void startTasks() {
		if (!jmsTaskDisabled) {
			log.info("Starting JMS task...");
			ReceiverJmsMessageTask jmsTask = new ReceiverJmsMessageTask();
			siteExecutor.scheduleWithFixedDelay(jmsTask, initialDelayMsgTsk, delayMsgTask, MILLISECONDS);
		}
		
		ReceiverRetryTask retryTask = SyncContext.getBean(ReceiverRetryTask.class);
		siteExecutor.scheduleWithFixedDelay(retryTask, initialDelayRetryTask, delayRetryTask, MILLISECONDS);
		ReceiverReconcileTask recTask = new ReceiverReconcileTask();
		siteExecutor.scheduleWithFixedDelay(recTask, initDelayReconciler, delayReconciler, MILLISECONDS);
		ReceiverReconcileMsgTask recMsgTask = new ReceiverReconcileMsgTask();
		siteExecutor.scheduleWithFixedDelay(recMsgTask, initDelayMsgReconciler, delayMsgReconciler, MILLISECONDS);
		if (!archiveTaskDisabled) {
			log.info("Starting Archive task...");
			SyncedMessageArchiver archiver = new SyncedMessageArchiver();
			siteExecutor.scheduleWithFixedDelay(archiver, initialDelayArchiveTask, delayArchiveTask, MILLISECONDS);
		}
	}
	
	private void startPrunerTask() {
		ReceiverArchivePruningTask pruner = new ReceiverArchivePruningTask(archivesMaxAgeInDays);
		siteExecutor.scheduleWithFixedDelay(pruner, initialDelayPruner, delayPruner, MILLISECONDS);
	}
	
	private boolean hasAtLeastOneEnabledTask(List<Class<? extends Runnable>> disabledTaskClasses, boolean fullIndexEnabled) {
		if (!disabledTaskClasses.contains(Synchronizer.class)) {
			return true;
		}
		if (!fullIndexEnabled) {
			if (!disabledTaskClasses.contains(CacheEvictor.class)) {
				return true;
			}
			if (!disabledTaskClasses.contains(SearchIndexUpdater.class)) {
				return true;
			}
		}
		if (!disabledTaskClasses.contains(SyncResponseSender.class)) {
			return true;
		}
		if (!disabledTaskClasses.contains(SyncedMessageDeleter.class)) {
			return true;
		}
		return false;
	}
}

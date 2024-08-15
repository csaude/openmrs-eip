package org.openmrs.eip.app.receiver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_DISABLED_SITES;
import static org.openmrs.eip.app.receiver.ReceiverConstants.PROP_ENABLED_SITES;
import static org.powermock.reflect.Whitebox.setInternalState;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.receiver.SiteInfo;
import org.openmrs.eip.app.receiver.reconcile.ReceiverReconcileMsgTask;
import org.openmrs.eip.app.receiver.reconcile.ReceiverReconcileTask;
import org.openmrs.eip.app.receiver.task.ReceiverRetryTask;
import org.openmrs.eip.component.Constants;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.entity.User;
import org.openmrs.eip.component.entity.light.UserLight;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.repository.UserRepository;
import org.openmrs.eip.component.repository.light.UserLightRepository;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Example;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SyncContext.class, ReceiverContext.class, AppUtils.class, ReceiverUtils.class })
public class ReceiverCamelListenerTest {
	
	private static final String TEST_OPENMRS_USERNAME = "openmrs_user";
	
	private static final Long TEST_OPENMRS_USER_ID = 101L;
	
	@Mock
	private Environment mockEnv;
	
	@Mock
	private UserRepository mockUserRepo;
	
	@Mock
	private UserLightRepository mockUserLightRepo;
	
	@Mock
	private ScheduledThreadPoolExecutor mockSiteExecutor;
	
	@Mock
	private ThreadPoolExecutor mockSyncExecutor;
	
	@Mock
	private ReceiverRetryTask mockRetryTask;
	
	private ReceiverCamelListener listener;
	
	private long testInitialDelay = 2;
	
	private long testDelay = 3;
	
	@Before
	public void setup() {
		PowerMockito.mockStatic(SyncContext.class);
		PowerMockito.mockStatic(ReceiverContext.class);
		PowerMockito.mockStatic(AppUtils.class);
		PowerMockito.mockStatic(ReceiverUtils.class);
		when(SyncContext.getBean(Environment.class)).thenReturn(mockEnv);
		User testAppUser = new User();
		testAppUser.setId(TEST_OPENMRS_USER_ID);
		when(mockEnv.getProperty(Constants.PROP_OPENMRS_USER)).thenReturn(TEST_OPENMRS_USERNAME);
		when(SyncContext.getBean(UserRepository.class)).thenReturn(mockUserRepo);
		when(mockUserRepo.findOne(any(Example.class))).thenReturn(Optional.of(testAppUser));
		when(SyncContext.getBean(UserLightRepository.class)).thenReturn(mockUserLightRepo);
		when(mockUserLightRepo.findById(TEST_OPENMRS_USER_ID)).thenReturn(Optional.of(new UserLight()));
		listener = new ReceiverCamelListener(mockSiteExecutor, mockSyncExecutor);
		setInternalState(BaseSiteRunnable.class, "initialized", true);
		setInternalState(BaseReceiverSyncPrioritizingTask.class, "initialized", true);
		setInternalState(listener, "siteTaskInitialDelay", testInitialDelay);
		setInternalState(listener, "siteTaskDelay", testDelay);
		setInternalState(listener, "disabledTaskTypes", Collections.emptyList());
		setInternalState(listener, "initialDelayPruner", testInitialDelay);
		setInternalState(listener, "delayPruner", testDelay);
		setInternalState(listener, "initialDelayRetryTask", testInitialDelay);
		setInternalState(listener, "delayRetryTask", testDelay);
		setInternalState(listener, "jmsTaskDisabled", false);
		setInternalState(listener, "enabledSiteIdentifiers", Collections.emptyList());
		setInternalState(listener, "disabledSiteIdentifiers", Collections.emptyList());
	}
	
	@After
	public void tearDown() {
		setInternalState(BaseSiteRunnable.class, "initialized", false);
		setInternalState(BaseReceiverSyncPrioritizingTask.class, "initialized", false);
	}
	
	@Test
	public void applicationStarted_shouldOnlyStartSiteTasksForSitesThatAreNotDisabled() {
		SiteInfo siteInfo1 = new SiteInfo();
		siteInfo1.setIdentifier("site1");
		siteInfo1.setDisabled(true);
		SiteInfo siteInfo2 = new SiteInfo();
		final String siteIdentifier2 = "site2";
		siteInfo2.setIdentifier(siteIdentifier2);
		siteInfo2.setDisabled(false);
		final String siteIdentifier3 = "site3";
		SiteInfo siteInfo3 = new SiteInfo();
		siteInfo3.setDisabled(false);
		siteInfo3.setIdentifier(siteIdentifier3);
		setInternalState(listener, "disabledSiteIdentifiers", Collections.singletonList(siteIdentifier3));
		Collection<SiteInfo> sites = Stream.of(siteInfo1, siteInfo2, siteInfo3).collect(Collectors.toList());
		when(ReceiverContext.getSites()).thenReturn(sites);
		setInternalState(listener, "initialDelayMsgTsk", testInitialDelay);
		setInternalState(listener, "delayMsgTask", testDelay);
		setInternalState(listener, "initDelayReconciler", testInitialDelay);
		setInternalState(listener, "delayReconciler", testDelay);
		setInternalState(listener, "initDelayMsgReconciler", testInitialDelay);
		setInternalState(listener, "delayMsgReconciler", testDelay);
		setInternalState(listener, "fullIndexerCron", "-");
		
		listener.applicationStarted();
		
		Mockito.verify(mockSiteExecutor).scheduleWithFixedDelay(any(SiteParentTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
		Mockito.verify(mockSiteExecutor).scheduleWithFixedDelay(any(ReceiverJmsMessageTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
		Mockito.verify(mockSiteExecutor).scheduleWithFixedDelay(any(ReceiverReconcileTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
		Mockito.verify(mockSiteExecutor).scheduleWithFixedDelay(any(ReceiverReconcileMsgTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
		ArgumentCaptor<SiteParentTask> captor = ArgumentCaptor.forClass(SiteParentTask.class);
		verify(mockSiteExecutor, times(5)).scheduleWithFixedDelay(captor.capture(), eq(testInitialDelay), eq(testDelay),
		    eq(TimeUnit.MILLISECONDS));
		Assert.assertNotNull(Whitebox.getInternalState(captor.getAllValues().get(4), "evictor"));
		Assert.assertNotNull(Whitebox.getInternalState(captor.getAllValues().get(4), "updater"));
	}
	
	@Test
	public void applicationStarted_shouldOnlyStartSiteConsumersForSitesThatAreNotDisabled() {
		final String siteIdentifier = "site2";
		SiteInfo site = new SiteInfo();
		site.setIdentifier(siteIdentifier);
		site.setDisabled(false);
		Collection<SiteInfo> sites = Collections.singletonList(site);
		when(ReceiverContext.getSites()).thenReturn(sites);
		setInternalState(listener, "prunerEnabled", true);
		setInternalState(listener, "archivesMaxAgeInDays", 1);
		
		listener.applicationStarted();
		
		verify(mockSiteExecutor).scheduleWithFixedDelay(any(SiteParentTask.class), eq(testInitialDelay), eq(testDelay),
		    eq(TimeUnit.MILLISECONDS));
		
		verify(mockSiteExecutor).scheduleWithFixedDelay(any(ReceiverArchivePruningTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
	}
	
	@Test
	public void applicationStarted_shouldOnlyStartSiteTasksForEnabledSitesViaAppProperty() {
		final String siteIdentifier1 = "site1";
		final String siteIdentifier2 = "site2";
		SiteInfo siteInfo1 = new SiteInfo();
		siteInfo1.setIdentifier(siteIdentifier1);
		siteInfo1.setDisabled(false);
		SiteInfo siteInfo2 = new SiteInfo();
		siteInfo2.setIdentifier(siteIdentifier2);
		siteInfo2.setDisabled(false);
		SiteInfo siteInfo3 = new SiteInfo();
		siteInfo3.setDisabled(false);
		siteInfo3.setIdentifier("site3");
		setInternalState(listener, "enabledSiteIdentifiers", Arrays.asList(siteIdentifier1, siteIdentifier2));
		Collection<SiteInfo> sites = Stream.of(siteInfo1, siteInfo2, siteInfo3).collect(Collectors.toList());
		when(ReceiverContext.getSites()).thenReturn(sites);
		setInternalState(listener, "initialDelayMsgTsk", testInitialDelay);
		setInternalState(listener, "delayMsgTask", testDelay);
		setInternalState(listener, "initDelayReconciler", testInitialDelay);
		setInternalState(listener, "delayReconciler", testDelay);
		setInternalState(listener, "initDelayMsgReconciler", testInitialDelay);
		setInternalState(listener, "delayMsgReconciler", testDelay);
		
		listener.applicationStarted();
		
		Mockito.verify(mockSiteExecutor, times(2)).scheduleWithFixedDelay(any(SiteParentTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
		Mockito.verify(mockSiteExecutor).scheduleWithFixedDelay(any(ReceiverJmsMessageTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
		Mockito.verify(mockSiteExecutor).scheduleWithFixedDelay(any(ReceiverReconcileTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
		Mockito.verify(mockSiteExecutor).scheduleWithFixedDelay(any(ReceiverReconcileMsgTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
	}
	
	@Test
	public void applicationStarted_shouldFailIfEnabledAndDisabledSitesAreBothSet() {
		setInternalState(listener, "enabledSiteIdentifiers", Arrays.asList("site1"));
		setInternalState(listener, "disabledSiteIdentifiers", Arrays.asList("site2"));
		Exception thrown = Assert.assertThrows(EIPException.class, () -> {
			listener.applicationStarted();
		});
		
		Assert.assertEquals("You can only set " + PROP_ENABLED_SITES + " or " + PROP_DISABLED_SITES + " but not both",
		    thrown.getMessage());
	}
	
	@Test
	public void applicationStopped_shouldCleanUpWhenApplicationContextIsStopped() {
		when(mockSyncExecutor.isTerminated()).thenReturn(true);
		final String siteName1 = "task 1";
		final String siteName2 = "task 2";
		SiteParentTask mockTask1 = Mockito.mock(SiteParentTask.class);
		SiteInfo site1 = new SiteInfo();
		site1.setName(siteName1);
		when(mockTask1.getSiteInfo()).thenReturn(site1);
		when(mockTask1.getChildExecutor()).thenReturn(mockSyncExecutor);
		SiteParentTask mockTask2 = Mockito.mock(SiteParentTask.class);
		SiteInfo site2 = new SiteInfo();
		site2.setName(siteName2);
		when(mockTask2.getSiteInfo()).thenReturn(site2);
		when(mockTask2.getChildExecutor()).thenReturn(mockSyncExecutor);
		setInternalState(ReceiverCamelListener.class, "siteTasks", Arrays.asList(mockTask1, mockTask2));
		
		listener.applicationStopped();
		
		PowerMockito.verifyStatic(AppUtils.class);
		AppUtils.shutdownExecutor(mockSyncExecutor, siteName1 + " " + ReceiverConstants.CHILD_TASK_NAME, true);
		PowerMockito.verifyStatic(AppUtils.class);
		AppUtils.shutdownExecutor(mockSyncExecutor, siteName2 + " " + ReceiverConstants.CHILD_TASK_NAME, true);
		PowerMockito.verifyStatic(AppUtils.class);
		AppUtils.shutdownExecutor(mockSiteExecutor, ReceiverConstants.PARENT_TASK_NAME, false);
	}
	
	@Test
	public void applicationStarted_shouldNotStartJmsTaskIfDisabled() {
		setInternalState(listener, "jmsTaskDisabled", true);
		setInternalState(listener, "initDelayReconciler", testInitialDelay);
		setInternalState(listener, "delayReconciler", testDelay);
		setInternalState(listener, "initDelayMsgReconciler", testInitialDelay);
		setInternalState(listener, "delayMsgReconciler", testDelay);
		
		listener.applicationStarted();
		
		Mockito.verify(mockSiteExecutor, never()).scheduleWithFixedDelay(any(ReceiverJmsMessageTask.class), anyLong(),
		    anyLong(), any());
		Mockito.verify(mockSiteExecutor).scheduleWithFixedDelay(any(ReceiverReconcileTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
		Mockito.verify(mockSiteExecutor).scheduleWithFixedDelay(any(ReceiverReconcileMsgTask.class), eq(testInitialDelay),
		    eq(testDelay), eq(TimeUnit.MILLISECONDS));
	}
	
	@Test
	public void applicationStarted_shouldNotSetEvictorAndUpdatorWhenFullIndexIsEnabled() {
		SiteInfo site = new SiteInfo();
		site.setIdentifier("site1");
		site.setDisabled(false);
		Collection<SiteInfo> sites = Collections.singletonList(site);
		when(ReceiverContext.getSites()).thenReturn(sites);
		setInternalState(listener, "initialDelayMsgTsk", testInitialDelay);
		setInternalState(listener, "delayMsgTask", testDelay);
		setInternalState(listener, "initDelayReconciler", testInitialDelay);
		setInternalState(listener, "delayReconciler", testDelay);
		setInternalState(listener, "initDelayMsgReconciler", testInitialDelay);
		setInternalState(listener, "delayMsgReconciler", testDelay);
		setInternalState(listener, "fullIndexerCron", "* * * * * *");
		
		listener.applicationStarted();
		
		ArgumentCaptor<SiteParentTask> captor = ArgumentCaptor.forClass(SiteParentTask.class);
		verify(mockSiteExecutor, times(5)).scheduleWithFixedDelay(captor.capture(), eq(testInitialDelay), eq(testDelay),
		    eq(TimeUnit.MILLISECONDS));
		Assert.assertNull(Whitebox.getInternalState(captor.getAllValues().get(4), "evictor"));
		Assert.assertNull(Whitebox.getInternalState(captor.getAllValues().get(4), "updater"));
	}
	
}

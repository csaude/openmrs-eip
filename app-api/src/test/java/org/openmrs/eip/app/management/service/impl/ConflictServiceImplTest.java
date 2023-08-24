package org.openmrs.eip.app.management.service.impl;

import static java.time.LocalDateTime.of;
import static java.time.Month.AUGUST;
import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.openmrs.eip.app.receiver.ConflictResolution.ResolutionDecision.IGNORE_NEW;
import static org.openmrs.eip.app.receiver.ConflictResolution.ResolutionDecision.MERGE;
import static org.openmrs.eip.app.receiver.ReceiverConstants.FIELD_RETIRED;
import static org.openmrs.eip.app.receiver.ReceiverConstants.FIELD_VOIDED;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.xml.ws.Holder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.openmrs.eip.app.management.entity.receiver.ConflictQueueItem;
import org.openmrs.eip.app.management.service.ReceiverService;
import org.openmrs.eip.app.receiver.ConflictCacheEvictingProcessor;
import org.openmrs.eip.app.receiver.ConflictResolution;
import org.openmrs.eip.app.receiver.ConflictResolution.ResolutionDecision;
import org.openmrs.eip.app.receiver.ConflictSearchIndexUpdatingProcessor;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.model.BaseDataModel;
import org.openmrs.eip.component.model.BaseMetadataModel;
import org.openmrs.eip.component.model.BaseModel;
import org.openmrs.eip.component.model.ObservationModel;
import org.openmrs.eip.component.model.PersonModel;
import org.openmrs.eip.component.model.ProviderModel;
import org.openmrs.eip.component.model.VisitModel;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class ConflictServiceImplTest {
	
	private ConflictServiceImpl service;
	
	@Mock
	private ReceiverService mockReceiverService;
	
	@Mock
	private ConflictCacheEvictingProcessor evictProcessor;
	
	@Mock
	private ConflictSearchIndexUpdatingProcessor indexUpdateProcessor;
	
	@Before
	public void setup() {
		service = new ConflictServiceImpl(null, null, null, mockReceiverService, null, null, evictProcessor,
		        indexUpdateProcessor);
	}
	
	@Test
	public void resolve_shouldFailIfConflictIsNull() {
		Throwable thrown = Assert.assertThrows(EIPException.class,
		    () -> service.resolve(new ConflictResolution(null, IGNORE_NEW)));
		assertEquals("Conflict is required", thrown.getMessage());
	}
	
	@Test
	public void resolve_shouldFailIfResolutionIsNull() {
		Throwable thrown = Assert.assertThrows(EIPException.class,
		    () -> service.resolve(new ConflictResolution(new ConflictQueueItem(), null)));
		assertEquals("Resolution is required", thrown.getMessage());
	}
	
	@Test
	public void resolve_shouldMoveTheItemToTheArchivesIfDecisionIsSetToIgnoreNew() {
		service = Mockito.spy(service);
		ConflictQueueItem conflict = new ConflictQueueItem();
		Holder<Boolean> holder = new Holder();
		Mockito.doAnswer(invocation -> {
			holder.value = true;
			return null;
		}).when(service).moveToArchiveQueue(conflict);
		
		service.resolve(new ConflictResolution(conflict, IGNORE_NEW));
		
		assertTrue(holder.value);
	}
	
	@Test
	public void resolve_shouldUpdateTheHashAndMoveTheItemToTheRetryQueueIfDecisionIsSetToSyncNew() {
		final String modelClassName = PersonModel.class.getName();
		final String uuid = "person-uuid";
		service = Mockito.spy(service);
		ConflictQueueItem conflict = new ConflictQueueItem();
		conflict.setModelClassName(modelClassName);
		conflict.setIdentifier(uuid);
		Holder<Boolean> holder = new Holder();
		Mockito.doAnswer(invocation -> {
			holder.value = true;
			return null;
		}).when(service).moveToRetryQueue(conflict, "Moved from conflict queue after conflict resolution");
		
		service.resolve(new ConflictResolution(conflict, ResolutionDecision.SYNC_NEW));
		
		Mockito.verify(mockReceiverService).updateHash(modelClassName, uuid);
		assertTrue(holder.value);
	}
	
	@Test
	public void resolve_shouldFailForAMergeResolutionAndNoSyncedPropertiesSpecified() {
		Throwable thrown = Assert.assertThrows(EIPException.class,
		    () -> service.resolve(new ConflictResolution(new ConflictQueueItem(), MERGE)));
		assertEquals("No properties to sync specified for merge resolution decision", thrown.getMessage());
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldSkipIfVoidedFieldIsNotSynced() {
		BaseDataModel dbModel = Mockito.mock(BaseDataModel.class);
		BaseDataModel newModel = Mockito.mock(BaseDataModel.class);
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, Collections.emptySet());
		
		verifyNoInteractions(dbModel);
		verifyNoInteractions(newModel);
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldSkipIfRetiredFieldIsNotSynced() {
		BaseMetadataModel dbModel = Mockito.mock(BaseMetadataModel.class);
		BaseMetadataModel newModel = Mockito.mock(BaseMetadataModel.class);
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, Collections.emptySet());
		
		verifyNoInteractions(dbModel);
		verifyNoInteractions(newModel);
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldSkipForAModelThatIsNeitherDataNorMetadata() {
		BaseModel dbModel = Mockito.mock(BaseModel.class);
		BaseModel newModel = Mockito.mock(BaseModel.class);
		Set<String> syncedProps = new HashSet<>();
		syncedProps.add(FIELD_VOIDED);
		syncedProps.add(FIELD_RETIRED);
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, syncedProps);
		
		verifyNoInteractions(dbModel);
		verifyNoInteractions(newModel);
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldReplaceDbFieldsIfTheNewStateIsVoidedAndDbStateIsNot() {
		final String newUser = "User(user-uuid)";
		final LocalDateTime newDate = LocalDateTime.now();
		final String newReason = "test";
		VisitModel dbModel = new VisitModel();
		VisitModel newModel = new VisitModel();
		newModel.setVoided(true);
		newModel.setVoidedByUuid(newUser);
		newModel.setDateVoided(newDate);
		newModel.setVoidReason(newReason);
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, singleton(FIELD_VOIDED));
		
		assertEquals(newUser, dbModel.getVoidedByUuid());
		assertEquals(newDate, dbModel.getDateVoided());
		assertEquals(newReason, dbModel.getVoidReason());
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldNotReplaceVoidFieldsWithEmptyData() {
		final String dbUser = "db-User(user-uuid)";
		final String dbReason = "db-test";
		VisitModel dbModel = new VisitModel();
		dbModel.setVoidedByUuid(dbUser);
		dbModel.setVoidReason(dbReason);
		VisitModel newModel = new VisitModel();
		newModel.setVoided(true);
		newModel.setVoidReason(" ");
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, singleton(FIELD_VOIDED));
		
		assertEquals(dbUser, dbModel.getVoidedByUuid());
		assertEquals(dbReason, dbModel.getVoidReason());
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldSkipIfTheNewStateIsVoidedButDateVoidedIsBeforeThatFromTheDb() {
		final String dbUser = "db-User(user-uuid)";
		final LocalDateTime dbDate = of(2023, AUGUST, 23, 00, 00, 01);
		final String dbReason = "db-test";
		VisitModel dbModel = new VisitModel();
		dbModel.setVoidedByUuid(dbUser);
		dbModel.setDateVoided(dbDate);
		dbModel.setVoidReason(dbReason);
		VisitModel newModel = new VisitModel();
		newModel.setVoided(true);
		newModel.setVoidedByUuid("User(user-uuid)");
		newModel.setDateVoided(of(2023, AUGUST, 23, 00, 00, 00));
		newModel.setVoidReason("test");
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, singleton(FIELD_VOIDED));
		
		assertEquals(dbUser, dbModel.getVoidedByUuid());
		assertEquals(dbDate, dbModel.getDateVoided());
		assertEquals(dbReason, dbModel.getVoidReason());
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldClearDbFieldsIfTheNewStateIsNotVoided() {
		VisitModel dbModel = new VisitModel();
		dbModel.setVoidedByUuid("db-User(user-uuid)");
		dbModel.setDateVoided(of(2023, AUGUST, 23, 00, 00, 00));
		dbModel.setVoidReason("db-test");
		VisitModel newModel = new VisitModel();
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, singleton(FIELD_VOIDED));
		
		assertNull(dbModel.getVoidedByUuid());
		assertNull(dbModel.getDateVoided());
		assertNull(dbModel.getVoidReason());
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldReplaceDbFieldsIfTheNewStateIsRetiredAndDbStateIsNot() {
		final String newUser = "User(user-uuid)";
		final LocalDateTime newDate = LocalDateTime.now();
		final String newReason = "test";
		ProviderModel dbModel = new ProviderModel();
		ProviderModel newModel = new ProviderModel();
		newModel.setRetired(true);
		newModel.setRetiredByUuid(newUser);
		newModel.setDateRetired(newDate);
		newModel.setRetireReason(newReason);
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, singleton(FIELD_RETIRED));
		
		assertEquals(newUser, dbModel.getRetiredByUuid());
		assertEquals(newDate, dbModel.getDateRetired());
		assertEquals(newReason, dbModel.getRetireReason());
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldNotReplaceRetireFieldsWithEmptyData() {
		final String dbUser = "db-User(user-uuid)";
		final String dbReason = "db-test";
		ProviderModel dbModel = new ProviderModel();
		dbModel.setRetiredByUuid(dbUser);
		dbModel.setRetireReason(dbReason);
		ProviderModel newModel = new ProviderModel();
		newModel.setRetired(true);
		newModel.setRetireReason(" ");
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, singleton(FIELD_RETIRED));
		
		assertEquals(dbUser, dbModel.getRetiredByUuid());
		assertEquals(dbReason, dbModel.getRetireReason());
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldSkipIfTheNewStateIsRetiredButDateRetiredIsBeforeThatFromTheDb() {
		final String dbUser = "db-User(user-uuid)";
		final LocalDateTime dbDate = of(2023, AUGUST, 23, 00, 00, 01);
		final String dbReason = "db-test";
		ProviderModel dbModel = new ProviderModel();
		dbModel.setRetiredByUuid(dbUser);
		dbModel.setDateRetired(dbDate);
		dbModel.setRetireReason(dbReason);
		ProviderModel newModel = new ProviderModel();
		newModel.setRetired(true);
		newModel.setRetiredByUuid("User(user-uuid)");
		newModel.setDateRetired(of(2023, AUGUST, 23, 00, 00, 00));
		newModel.setRetireReason("test");
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, singleton(FIELD_RETIRED));
		
		assertEquals(dbUser, dbModel.getRetiredByUuid());
		assertEquals(dbDate, dbModel.getDateRetired());
		assertEquals(dbReason, dbModel.getRetireReason());
	}
	
	@Test
	public void mergeVoidOrRetireProperties_shouldClearDbFieldsIfTheNewStateIsNotRetired() {
		ProviderModel dbModel = new ProviderModel();
		dbModel.setRetiredByUuid("db-User(user-uuid)");
		dbModel.setDateRetired(of(2023, AUGUST, 23, 00, 00, 00));
		dbModel.setRetireReason("db-test");
		ProviderModel newModel = new ProviderModel();
		
		service.mergeVoidOrRetireProperties(dbModel, newModel, singleton(FIELD_RETIRED));
		
		assertNull(dbModel.getRetiredByUuid());
		assertNull(dbModel.getDateRetired());
		assertNull(dbModel.getRetireReason());
	}
	
	@Test
	public void mergeAuditProperties_shouldSkipForAModelThatIsNotAuditable() {
		ObservationModel dbModel = Mockito.mock(ObservationModel.class);
		ObservationModel newModel = Mockito.mock(ObservationModel.class);
		
		service.mergeAuditProperties(dbModel, newModel);
		
		verifyNoInteractions(dbModel);
		verifyNoInteractions(newModel);
	}
	
	@Test
	public void mergeAuditProperties_shouldReplaceDbFieldsWithTheNewStateForData() {
		final String newUser = "User(user-uuid)";
		final LocalDateTime newDate = LocalDateTime.now();
		VisitModel dbModel = new VisitModel();
		VisitModel newModel = new VisitModel();
		newModel.setChangedByUuid(newUser);
		newModel.setDateChanged(newDate);
		
		service.mergeAuditProperties(dbModel, newModel);
		
		assertEquals(newUser, dbModel.getChangedByUuid());
		assertEquals(newDate, dbModel.getDateChanged());
	}
	
	@Test
	public void mergeAuditProperties_shouldSkipIfTheNewDateChangedIsBeforeThatFromTheDbForData() {
		final String dbUser = "db-User(user-uuid)";
		final LocalDateTime dbDate = of(2023, AUGUST, 23, 00, 00, 01);
		VisitModel dbModel = new VisitModel();
		dbModel.setChangedByUuid(dbUser);
		dbModel.setDateChanged(dbDate);
		VisitModel newModel = new VisitModel();
		newModel.setChangedByUuid("User(user-uuid)");
		newModel.setDateChanged(of(2023, AUGUST, 23, 00, 00, 00));
		
		service.mergeAuditProperties(dbModel, newModel);
		
		assertEquals(dbUser, dbModel.getChangedByUuid());
		assertEquals(dbDate, dbModel.getDateChanged());
	}
	
	@Test
	public void mergeAuditProperties_shouldNotReplaceChangedByWithEmptyDataForData() {
		final String dbUser = "db-User(user-uuid)";
		VisitModel dbModel = new VisitModel();
		dbModel.setChangedByUuid(dbUser);
		VisitModel newModel = new VisitModel();
		newModel.setChangedByUuid(" ");
		
		service.mergeAuditProperties(dbModel, newModel);
		
		assertEquals(dbUser, dbModel.getChangedByUuid());
	}
	
	@Test
	public void mergeAuditProperties_shouldReplaceDbFieldsWithTheNewStateMetadata() {
		final String newUser = "User(user-uuid)";
		final LocalDateTime newDate = LocalDateTime.now();
		ProviderModel dbModel = new ProviderModel();
		ProviderModel newModel = new ProviderModel();
		newModel.setChangedByUuid(newUser);
		newModel.setDateChanged(newDate);
		
		service.mergeAuditProperties(dbModel, newModel);
		
		assertEquals(newUser, dbModel.getChangedByUuid());
		assertEquals(newDate, dbModel.getDateChanged());
	}
	
	@Test
	public void mergeAuditProperties_shouldSkipIfTheNewDateChangedIsBeforeThatFromTheDbForMetadata() {
		final String dbUser = "db-User(user-uuid)";
		final LocalDateTime dbDate = of(2023, AUGUST, 23, 00, 00, 01);
		ProviderModel dbModel = new ProviderModel();
		dbModel.setChangedByUuid(dbUser);
		dbModel.setDateChanged(dbDate);
		ProviderModel newModel = new ProviderModel();
		newModel.setChangedByUuid("User(user-uuid)");
		newModel.setDateChanged(of(2023, AUGUST, 23, 00, 00, 00));
		
		service.mergeAuditProperties(dbModel, newModel);
		
		assertEquals(dbUser, dbModel.getChangedByUuid());
		assertEquals(dbDate, dbModel.getDateChanged());
	}
	
	@Test
	public void mergeAuditProperties_shouldNotReplaceChangedByWithEmptyDataForMetadata() {
		final String dbUser = "db-User(user-uuid)";
		ProviderModel dbModel = new ProviderModel();
		dbModel.setChangedByUuid(dbUser);
		ProviderModel newModel = new ProviderModel();
		newModel.setChangedByUuid(" ");
		
		service.mergeAuditProperties(dbModel, newModel);
		
		assertEquals(dbUser, dbModel.getChangedByUuid());
	}
	
}

package org.openmrs.eip.component.service;

import org.openmrs.eip.component.entity.*;
import org.openmrs.eip.component.exception.EIPException;
import org.openmrs.eip.component.management.hash.entity.*;
import org.openmrs.eip.component.model.*;

import java.util.Arrays;
import java.util.stream.Stream;

public enum TableToSyncEnum {
	
	PERSON(Person.class, PersonModel.class, PersonHash.class),
	
	PATIENT(Patient.class, PatientModel.class, PatientHash.class),
	
	VISIT(Visit.class, VisitModel.class, VisitHash.class),
	
	ENCOUNTER(Encounter.class, EncounterModel.class, EncounterHash.class),
	
	OBS(Observation.class, ObservationModel.class, ObsHash.class),
	
	PERSON_ATTRIBUTE(PersonAttribute.class, PersonAttributeModel.class, PersonAttributeHash.class),
	
	PATIENT_PROGRAM(PatientProgram.class, PatientProgramModel.class, PatientProgramHash.class),
	
	PATIENT_STATE(PatientState.class, PatientStateModel.class, PatientStateHash.class),
	
	CONCEPT_ATTRIBUTE(ConceptAttribute.class, ConceptAttributeModel.class, null),
	
	LOCATION_ATTRIBUTE(LocationAttribute.class, AttributeModel.class, null),
	
	PROVIDER_ATTRIBUTE(ProviderAttribute.class, AttributeModel.class, null),
	
	VISIT_ATTRIBUTE(VisitAttribute.class, VisitAttributeModel.class, VisitAttributeHash.class),
	
	CONCEPT(Concept.class, ConceptModel.class, null),
	
	LOCATION(Location.class, LocationModel.class, null),
	
	ENCOUNTER_DIAGNOSIS(EncounterDiagnosis.class, EncounterDiagnosisModel.class, EncounterDiagnosisHash.class),
	
	CONDITION(Condition.class, ConditionModel.class, ConditionHash.class),
	
	PERSON_NAME(PersonName.class, PersonNameModel.class, PersonNameHash.class),
	
	ALLERGY(Allergy.class, AllergyModel.class, AllergyHash.class),
	
	PERSON_ADDRESS(PersonAddress.class, PersonAddressModel.class, PersonAddressHash.class),
	
	PATIENT_IDENTIFIER(PatientIdentifier.class, PatientIdentifierModel.class, PatientIdentifierHash.class),
	
	ORDERS(Order.class, OrderModel.class, OrderHash.class),
	
	DRUG_ORDER(DrugOrder.class, DrugOrderModel.class, DrugOrderHash.class),
	
	TEST_ORDER(TestOrder.class, TestOrderModel.class, TestOrderHash.class),
	
	USERS(User.class, UserModel.class, UserHash.class),
	
	RELATIONSHIP(Relationship.class, RelationshipModel.class, RelationshipHash.class),
	
	PROVIDER(Provider.class, ProviderModel.class, ProviderHash.class),
	
	ENCOUNTER_PROVIDER(EncounterProvider.class, EncounterProviderModel.class, EncounterProviderHash.class),
	
	GAAC(Gaac.class, GaacModel.class, GaacHash.class),
	
	GAAC_MEMBER(GaacMember.class, GaacMemberModel.class, GaacMemberHash.class),
	
	GAAC_FAMILY(GaacFamily.class, GaacFamilyModel.class, GaacFamilyHash.class),
	
	GAAC_FAMILY_MEMBER(GaacFamilyMember.class, GaacFamilyMemberModel.class, GaacFamilyMemberHash.class),
	
	CLINICAL_SUMMARY_USAGE_REPORT(ClinicalSummaryUsageReport.class, ClinicalSummaryUsageReportModel.class,
	        ClinicalSummaryUsageReportHash.class);
	
	private Class<? extends BaseEntity> entityClass;
	
	private Class<? extends BaseModel> modelClass;
	
	private Class<? extends BaseHashEntity> hashClass;
	
	TableToSyncEnum(final Class<? extends BaseEntity> entityClass, final Class<? extends BaseModel> modelClass,
	    Class<? extends BaseHashEntity> hashClass) {
		this.entityClass = entityClass;
		this.modelClass = modelClass;
		this.hashClass = hashClass;
	}
	
	public Class<? extends BaseEntity> getEntityClass() {
		return entityClass;
	}
	
	public Class<? extends BaseModel> getModelClass() {
		return modelClass;
	}
	
	public Class<? extends BaseHashEntity> getHashClass() {
		return hashClass;
	}
	
	public static TableToSyncEnum getTableToSyncEnum(final String tableToSync) {
		return valueOf(tableToSync.toUpperCase());
	}
	
	public static TableToSyncEnum getTableToSyncEnum(final Class<? extends BaseModel> tableToSyncClass) {
		return Arrays.stream(values()).filter(e -> e.getModelClass().equals(tableToSyncClass)).findFirst()
		        .orElseThrow(() -> new EIPException("No enum found for model class " + tableToSyncClass));
	}
	
	public static Class<? extends BaseModel> getModelClass(final BaseEntity entity) {
		return Stream.of(values()).filter(e -> e.getEntityClass().equals(entity.getClass())).findFirst()
		        .map(TableToSyncEnum::getModelClass).orElseThrow(
		            () -> new EIPException("No model class found corresponding to entity class " + entity.getClass()));
	}
	
	public static Class<? extends BaseEntity> getEntityClass(final BaseModel model) {
		return Stream.of(values()).filter(e -> e.getModelClass().equals(model.getClass())).findFirst()
		        .map(TableToSyncEnum::getEntityClass).orElseThrow(
		            () -> new EIPException("No entity class found corresponding to model class " + model.getClass()));
	}
	
	public static Class<? extends BaseHashEntity> getHashClass(BaseModel model) {
		return Stream.of(values()).filter(e -> e.getModelClass().equals(model.getClass())).findFirst()
		        .map(TableToSyncEnum::getHashClass)
		        .orElseThrow(() -> new EIPException("No hash class found corresponding to has class " + model.getClass()));
	}
	
	public static TableToSyncEnum getTableToSyncEnumByModelClassName(String modelClassName) {
		return Arrays.stream(values()).filter(e -> e.getModelClass().getName().equals(modelClassName)).findFirst()
		        .orElseThrow(() -> new EIPException("No enum found for model class name " + modelClassName));
	}
}

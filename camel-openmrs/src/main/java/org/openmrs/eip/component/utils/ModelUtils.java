package org.openmrs.eip.component.utils;

import org.json.JSONObject;
import org.openmrs.eip.component.entity.light.ConceptLight;
import org.openmrs.eip.component.entity.light.EncounterTypeLight;
import org.openmrs.eip.component.entity.light.LightEntity;
import org.openmrs.eip.component.entity.light.LocationLight;
import org.openmrs.eip.component.entity.light.OrderTypeLight;
import org.openmrs.eip.component.entity.light.PatientIdentifierTypeLight;
import org.openmrs.eip.component.entity.light.PersonAttributeTypeLight;
import org.openmrs.eip.component.entity.light.ProviderAttributeTypeLight;
import org.openmrs.eip.component.entity.light.RelationshipTypeLight;
import org.openmrs.eip.component.entity.light.VisitAttributeTypeLight;
import org.openmrs.eip.component.entity.light.VisitTypeLight;
import org.openmrs.eip.component.mapper.operations.DecomposedUuid;

import java.util.Optional;
import java.util.Set;

public final class ModelUtils {
	
	private static final Set<Class<?>> METADATA_TYPES = Set.of(PatientIdentifierTypeLight.class, ConceptLight.class,
	    LocationLight.class, EncounterTypeLight.class, OrderTypeLight.class, ProviderAttributeTypeLight.class,
	    PersonAttributeTypeLight.class, RelationshipTypeLight.class, VisitAttributeTypeLight.class, VisitTypeLight.class);
	
	private ModelUtils() {
	}
	
	/**
	 * Takes a uuid as a parameter formatted as follows: org.openmrs.package.classname(uuid) and returns
	 * an Optional of DecomposedUuid as a result
	 * 
	 * @param fullUuid the uuid as a string
	 * @return a decomposedUuid
	 */
	public static Optional<DecomposedUuid> decomposeUuid(final String fullUuid) {
		if (fullUuid == null) {
			return Optional.empty();
		}
		int openingParenthesisIndex = fullUuid.indexOf('(');
		int closingParenthesisIndex = fullUuid.indexOf(')');
		String entityTypeName = fullUuid.substring(0, openingParenthesisIndex);
		String uuid = fullUuid.substring(openingParenthesisIndex + 1, closingParenthesisIndex);
		
		return Optional.of(new DecomposedUuid(entityTypeName, uuid));
	}
	
	/**
	 * Extracts the uuid from the JSON body located at the given property name
	 * 
	 * @param body JSON body message
	 * @param uuidPropertyName the field name of the uuid
	 * @return the uuid
	 */
	public static String extractUuid(final String body, final String uuidPropertyName) {
		Optional<DecomposedUuid> decomposedUuid = decomposeUuid(
		    new JSONObject(body).getJSONObject("model").getString(uuidPropertyName));
		
		return decomposedUuid.map(DecomposedUuid::getUuid).orElse(null);
	}
	
	public static boolean isMetadataEntity(LightEntity entity) {
		if (entity != null) {
			return METADATA_TYPES.contains(entity.getClass());
		}
		return false;
	}
}

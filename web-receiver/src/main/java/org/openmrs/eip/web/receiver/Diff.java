package org.openmrs.eip.web.receiver;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.bouncycastle.util.Objects;
import org.openmrs.eip.app.receiver.ReceiverConstants;
import org.openmrs.eip.component.model.BaseModel;
import org.springframework.beans.BeanUtils;

import lombok.Getter;
import lombok.ToString;

/**
 * Represents the diff between the current receiver database and incoming states of an entity.
 */
@ToString(exclude = { "currentState", "newState" })
public class Diff {
	
	@Getter
	private Set<String> exclusions = ReceiverConstants.MERGE_EXCLUDE_FIELDS;
	
	@Getter
	private BaseModel currentState;
	
	@Getter
	private BaseModel newState;
	
	@Getter
	private Set<String> properties;
	
	@Getter
	private Set<String> additions;
	
	@Getter
	private Set<String> modifications;
	
	@Getter
	private Set<String> removals;
	
	private Diff(BaseModel currentState, BaseModel newState, Set<String> properties, Set<String> additions,
	    Set<String> modifications, Set<String> removals) {
		this.currentState = currentState;
		this.newState = newState;
		this.properties = properties;
		this.additions = additions;
		this.modifications = modifications;
		this.removals = removals;
	}
	
	/**
	 * Creates an instance containing the diff between the current and new models
	 * 
	 * @param currentModel the current state from the receiver database
	 * @param newModel the new incoming state from the remote site
	 * @return instance
	 * @throws Exception
	 */
	public static Diff createInstance(BaseModel currentModel, BaseModel newModel) throws Exception {
		Set<String> properties = new HashSet<>();
		Set<String> additions = new HashSet<>();
		Set<String> modifications = new HashSet<>();
		Set<String> removals = new HashSet<>();
		
		PropertyDescriptor[] descriptors = BeanUtils.getPropertyDescriptors(newModel.getClass());
		for (PropertyDescriptor descriptor : descriptors) {
			if (descriptor.getName().equals("class")) {
				continue;
			}
			
			properties.add(descriptor.getName());
			Object currentValue = PropertyUtils.getProperty(currentModel, descriptor.getName());
			Object newValue = PropertyUtils.getProperty(newModel, descriptor.getName());
			if (currentValue == null && newValue != null) {
				additions.add(descriptor.getName());
			} else if (newValue == null && currentValue != null) {
				removals.add(descriptor.getName());
			} else if (!Objects.areEqual(currentValue, newValue)) {
				modifications.add(descriptor.getName());
			}
		}
		
		return new Diff(currentModel, newModel, properties, additions, modifications, removals);
	}
	
}

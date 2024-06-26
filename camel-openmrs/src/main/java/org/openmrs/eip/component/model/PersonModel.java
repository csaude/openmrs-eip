package org.openmrs.eip.component.model;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PersonModel extends BaseChangeableDataModel {
	
	private String gender;
	
	private LocalDate birthdate;
	
	private boolean birthdateEstimated;
	
	private boolean dead;
	
	private LocalDate deathDate;
	
	private String causeOfDeathUuid;
	
	private boolean deathdateEstimated;
	
	private LocalTime birthtime;
	
	private String causeOfDeathNonCoded;
	
	/**
	 * Gets the gender
	 *
	 * @return the gender
	 */
	public String getGender() {
		return gender;
	}
	
	/**
	 * Sets the gender
	 *
	 * @param gender the gender to set
	 */
	public void setGender(String gender) {
		this.gender = gender;
	}
	
	/**
	 * Gets the birthdate
	 *
	 * @return the birthdate
	 */
	public LocalDate getBirthdate() {
		return birthdate;
	}
	
	/**
	 * Sets the birthdate
	 *
	 * @param birthdate the birthdate to set
	 */
	public void setBirthdate(LocalDate birthdate) {
		this.birthdate = birthdate;
	}
	
	/**
	 * Gets the birthdateEstimated
	 *
	 * @return the birthdateEstimated
	 */
	public boolean isBirthdateEstimated() {
		return birthdateEstimated;
	}
	
	/**
	 * Sets the birthdateEstimated
	 *
	 * @param birthdateEstimated the birthdateEstimated to set
	 */
	public void setBirthdateEstimated(boolean birthdateEstimated) {
		this.birthdateEstimated = birthdateEstimated;
	}
	
	/**
	 * Gets the dead
	 *
	 * @return the dead
	 */
	public boolean isDead() {
		return dead;
	}
	
	/**
	 * Sets the dead
	 *
	 * @param dead the dead to set
	 */
	public void setDead(boolean dead) {
		this.dead = dead;
	}
	
	/**
	 * Gets the deathDate
	 *
	 * @return the deathDate
	 */
	public LocalDate getDeathDate() {
		return deathDate;
	}
	
	/**
	 * Sets the deathDate
	 *
	 * @param deathDate the deathDate to set
	 */
	public void setDeathDate(LocalDate deathDate) {
		this.deathDate = deathDate;
	}
	
	/**
	 * Gets the causeOfDeathUuid
	 *
	 * @return the causeOfDeathUuid
	 */
	public String getCauseOfDeathUuid() {
		return causeOfDeathUuid;
	}
	
	/**
	 * Sets the causeOfDeathUuid
	 *
	 * @param causeOfDeathUuid the causeOfDeathUuid to set
	 */
	public void setCauseOfDeathUuid(String causeOfDeathUuid) {
		this.causeOfDeathUuid = causeOfDeathUuid;
	}
	
	/**
	 * Gets the deathdateEstimated
	 *
	 * @return the deathdateEstimated
	 */
	public boolean isDeathdateEstimated() {
		return deathdateEstimated;
	}
	
	/**
	 * Sets the deathdateEstimated
	 *
	 * @param deathdateEstimated the deathdateEstimated to set
	 */
	public void setDeathdateEstimated(boolean deathdateEstimated) {
		this.deathdateEstimated = deathdateEstimated;
	}
	
	/**
	 * Gets the birthtime
	 *
	 * @return the birthtime
	 */
	public LocalTime getBirthtime() {
		return birthtime;
	}
	
	/**
	 * Sets the birthtime
	 *
	 * @param birthtime the birthtime to set
	 */
	public void setBirthtime(LocalTime birthtime) {
		this.birthtime = birthtime;
	}
	
	/**
	 * Gets the causeOfDeathNonCoded
	 *
	 * @return the causeOfDeathNonCoded
	 */
	public String getCauseOfDeathNonCoded() {
		return causeOfDeathNonCoded;
	}
	
	/**
	 * Sets the causeOfDeathNonCoded
	 *
	 * @param causeOfDeathNonCoded the causeOfDeathNonCoded to set
	 */
	public void setCauseOfDeathNonCoded(String causeOfDeathNonCoded) {
		this.causeOfDeathNonCoded = causeOfDeathNonCoded;
	}
	
}

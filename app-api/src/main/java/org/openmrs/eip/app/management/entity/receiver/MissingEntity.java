package org.openmrs.eip.app.management.entity.receiver;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "missing_entity")
public class MissingEntity extends BaseUnSyncedEntity {}

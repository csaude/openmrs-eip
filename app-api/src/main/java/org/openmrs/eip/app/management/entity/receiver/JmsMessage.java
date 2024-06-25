/*
 * Copyright (C) Amiyul LLC - All Rights Reserved
 *
 * This source code is protected under international copyright law. All rights
 * reserved and protected by the copyright holder.
 *
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holder. If you encounter this file and do not have
 * permission, please contact the copyright holder and delete this file.
 */
package org.openmrs.eip.app.management.entity.receiver;

import org.openmrs.eip.app.AppUtils;
import org.openmrs.eip.app.management.entity.AbstractEntity;
import org.openmrs.eip.component.model.SyncModel;
import org.openmrs.eip.component.utils.JsonUtils;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "jms_msg")
public class JmsMessage extends AbstractEntity {
	
	public enum MessageType {
		
		SYNC,
		
		RECONCILE
		
	}
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Getter
	@Setter
	private Long id;
	
	@Column(name = "site_id", updatable = false)
	@Getter
	@Setter
	private String siteId;
	
	@Enumerated(EnumType.STRING)
	@Column(name = "msg_type", nullable = false, length = 50)
	@Getter
	@Setter
	@NotNull
	private MessageType type;
	
	@Lob
	@Basic(fetch = FetchType.LAZY)
	@Column(columnDefinition = "mediumblob", nullable = false, updatable = false)
	@NotNull
	@Getter
	@Setter
	private byte[] body;
	
	@Column(name = "msg_id", length = 38, unique = true, updatable = false)
	@Getter
	@Setter
	private String messageId;
	
	@Column(name = "sync_version", length = 20, updatable = false)
	@Getter
	@Setter
	private String syncVersion;
	
	@Transient
	private SyncModel syncModel;
	
	/**
	 * Gets the sync model
	 *
	 * @return the sync model in case of a sync message otherwise null
	 */
	public SyncModel getSyncModel() {
		if (syncModel == null && getType() == MessageType.SYNC) {
			syncModel = JsonUtils.unmarshalBytes(getBody(), SyncModel.class);
		}
		
		return syncModel;
	}
	
	@Override
	public String toString() {
		if (getType() == MessageType.RECONCILE) {
			return super.toString();
		}
		
		SyncModel m = getSyncModel();
		return "{site=" + siteId + ", model=" + AppUtils.getSimpleName(m.getTableToSyncModelClass().getName()) + ", uuid="
		        + m.getModel().getUuid() + ", op=" + m.getMetadata().getOperation() + ", snapshot="
		        + m.getMetadata().getSnapshot() + ", msgUuid=" + m.getMetadata().getMessageUuid() + "}";
	}
	
}

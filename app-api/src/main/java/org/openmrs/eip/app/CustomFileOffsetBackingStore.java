package org.openmrs.eip.app;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.connect.json.JsonConverterConfig;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.openmrs.eip.app.management.entity.DebeziumOffset;
import org.openmrs.eip.app.management.repository.DebeziumOffsetRepository;
import org.openmrs.eip.app.util.DebeziumOffsetUtil;
import org.openmrs.eip.component.SyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.config.Instantiator;
import io.debezium.embedded.EmbeddedEngine;

/**
 * Custom OffsetBackingStore class that:
 * <ol>
 * <li>Saves the offset to the database
 * <li>Only saves it if no exception was encountered while processing a source record read by
 * debezium from the MySQL binlog to ensure no binlog entry goes unprocessed.
 */
public class CustomFileOffsetBackingStore extends MemoryOffsetBackingStore {
	
	protected static final Logger log = LoggerFactory.getLogger(CustomFileOffsetBackingStore.class);
	
	private static boolean disabled = false;
	
	private static boolean paused = false;
	
	private String engineName;
	
	private Converter valueConverter;
	
	private DebeziumOffsetRepository debeziumOffsetRepository;
	
	@Override
	public synchronized void start() {
		super.start();
		this.debeziumOffsetRepository = SyncContext.getBean(DebeziumOffsetRepository.class);
		load();
	}
	
	private void load() {
		long offsets = debeziumOffsetRepository.countByEnabledTrue();
		
		if (offsets > 0) {
			log.info("Loading offset from the database");
			DebeziumOffset offset = debeziumOffsetRepository.findFirstByEnabledTrueOrderByDateCreatedDesc();
			
			if (log.isDebugEnabled()) {
				log.debug("Loaded offset: {}", offset);
			}
			
			Map<byte[], byte[]> raw = (Map<byte[], byte[]>) SerializationUtils.deserialize(offset.getData());
			data = DebeziumOffsetUtil.offsetRawToData(raw);
			
		} else {
			log.info("No offset was found in the database");
		}
	}
	
	/**
	 * @see {@link io.debezium.embedded.EmbeddedEngine#EmbeddedEngine()}
	 */
	@Override
	public void configure(WorkerConfig config) {
		Map<String, Object> originals = config.originals();
		
		this.engineName = (String) originals.get(EmbeddedEngine.ENGINE_NAME.name());
		String valueConverterClassName = (String) originals.get(WorkerConfig.VALUE_CONVERTER_CLASS_CONFIG);
		
		Map<String, Object> valueConverterOriginalConfs = config
		        .originalsWithPrefix(WorkerConfig.INTERNAL_VALUE_CONVERTER_CLASS_CONFIG + ".", true);
		valueConverterOriginalConfs.put(JsonConverterConfig.SCHEMAS_ENABLE_CONFIG, false);
		
		this.valueConverter = Instantiator.getInstance(valueConverterClassName, () -> this.getClass().getClassLoader(),
		    null);
		this.valueConverter.configure(valueConverterOriginalConfs, false);
	}
	
	public synchronized static void enable() {
		disabled = false;
		if (log.isDebugEnabled()) {
			log.debug("Enabled saving of offsets");
		}
	}
	
	public synchronized static void disable() {
		disabled = true;
		if (log.isDebugEnabled()) {
			log.debug("Disabled saving of offsets");
		}
	}
	
	public synchronized static boolean isDisabled() {
		return disabled;
	}
	
	public synchronized static boolean isPaused() {
		return paused;
	}
	
	public synchronized static void pause() {
		paused = true;
		if (log.isDebugEnabled()) {
			log.debug("Pause saving of offsets");
		}
	}
	
	public synchronized static void unpause() {
		paused = false;
		if (log.isDebugEnabled()) {
			log.debug("Removing pause on saving of offsets");
		}
	}
	
	@Override
	protected void save() {
		synchronized (CustomFileOffsetBackingStore.class) {
			if (disabled || paused) {
				if (paused) {
					if (log.isDebugEnabled()) {
						log.debug("Skipping saving of offset because it is paused");
					}
				} else {
					log.warn("Skipping saving of offset because an error was encountered while processing a source record");
				}
				
				return;
			}
			
			if (log.isDebugEnabled()) {
				log.debug("Saving binlog offset");
			}
			
			doSave();
		}
	}
	
	protected void doSave() {
		if (log.isDebugEnabled()) {
			log.debug("Saving offset to the database");
		}
		
		DebeziumOffset offset = new DebeziumOffset();
		offset.setDateCreated(new Date());
		offset.setData(SerializationUtils.serialize((Serializable) DebeziumOffsetUtil.offsetDataToRaw(data)));
		offset.setBinlogFileName(DebeziumOffsetUtil.getOffsetBinlogFilename(data, valueConverter, engineName));
		offset.setEnabled(Boolean.TRUE);
		
		offset = debeziumOffsetRepository.save(offset);
		
		if (log.isDebugEnabled()) {
			log.debug("Offset saved to the database: {}", offset);
		}
		
	}
	
}

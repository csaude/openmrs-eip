package org.openmrs.eip.app;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.connect.runtime.WorkerConfig;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.openmrs.eip.app.management.entity.DebeziumOffset;
import org.openmrs.eip.app.management.repository.DebeziumOffsetRepository;
import org.openmrs.eip.app.util.DebeziumOffsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.debezium.config.Instantiator;
import io.debezium.embedded.EmbeddedEngine;

@Component
public class CustomDatabaseOffsetBackingStore extends MemoryOffsetBackingStore {
	
	private static final Logger log = LoggerFactory.getLogger(CustomDatabaseOffsetBackingStore.class);
	
	private static DebeziumOffsetRepository debeziumOffsetRepository;
	
	private String engineName;
	
	private Converter valueConverter;
	
	public CustomDatabaseOffsetBackingStore() {
	}
	
	@Autowired
	public CustomDatabaseOffsetBackingStore(DebeziumOffsetRepository debeziumOffsetRepository) {
		CustomDatabaseOffsetBackingStore.debeziumOffsetRepository = debeziumOffsetRepository;
	}
	
	@Override
	public synchronized void start() {
		super.start();
		load();
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
		valueConverterOriginalConfs.put("schemas.enable", false);
		
		this.valueConverter = Instantiator.getInstance(valueConverterClassName, () -> this.getClass().getClassLoader(),
		    null);
		this.valueConverter.configure(valueConverterOriginalConfs, false);
	}
	
	/**
	 * @see
	 */
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
	
	@Override
	public void save() {
		CustomOffsetBackingStore.save(() -> {
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
			
			return true;
		});
	}
	
	public static void enable() {
		CustomOffsetBackingStore.enable();
	}
	
	public static void disable() {
		CustomOffsetBackingStore.disable();
	}
	
	public static boolean isDisabled() {
		return CustomOffsetBackingStore.isDisabled();
	}
	
	public static boolean isPaused() {
		return CustomOffsetBackingStore.isPaused();
	}
	
	public static void pause() {
		CustomOffsetBackingStore.pause();
	}
	
	public static void unpause() {
		CustomOffsetBackingStore.unpause();
	}
	
}

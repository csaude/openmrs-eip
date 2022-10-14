package org.openmrs.eip.app;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.kafka.connect.storage.MemoryOffsetBackingStore;
import org.openmrs.eip.app.management.entity.DebeziumOffset;
import org.openmrs.eip.app.management.repository.DebeziumOffsetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CustomDatabaseOffsetBackingStore extends MemoryOffsetBackingStore {
	
	private static final Logger log = LoggerFactory.getLogger(CustomDatabaseOffsetBackingStore.class);
	
	private DebeziumOffset offset;
	
	private static DebeziumOffsetRepository debeziumOffsetRepository;
	
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
	
	private void load() {
		long offsets = debeziumOffsetRepository.count();
		
		if (offsets == 1) {
			log.info("Loading offset from the database");
			offset = debeziumOffsetRepository.findAll().get(0);
			
			if (log.isDebugEnabled()) {
				log.debug("Loaded offset: {}", offset);
			}
			
			Map<byte[], byte[]> raw = (Map<byte[], byte[]>) SerializationUtils.deserialize(offset.getData());
			data = new HashMap<>();
			for (Map.Entry<byte[], byte[]> mapEntry : raw.entrySet()) {
				ByteBuffer key = (mapEntry.getKey() != null) ? ByteBuffer.wrap(mapEntry.getKey()) : null;
				ByteBuffer value = (mapEntry.getValue() != null) ? ByteBuffer.wrap(mapEntry.getValue()) : null;
				data.put(key, value);
			}
			
		} else if (offsets > 1) {
			throw new RuntimeException("At most one offset is allowed in the debezium_offset table. Found: " + offsets);
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
			
			if (offset == null) {
				offset = new DebeziumOffset();
				offset.setDateCreated(new Date());
			} else {
				offset.setDateChanged(LocalDateTime.now());
			}
			
			offset.setData(getOffsetData());
			
			offset = debeziumOffsetRepository.save(offset);
			
			if (log.isDebugEnabled()) {
				log.debug("Offset saved to the database: {}", offset);
			}
			
			return true;
		});
	}
	
	private byte[] getOffsetData() {
		Map<byte[], byte[]> raw = new HashMap<>();
		for (Map.Entry<ByteBuffer, ByteBuffer> mapEntry : data.entrySet()) {
			byte[] key = (mapEntry.getKey() != null) ? mapEntry.getKey().array() : null;
			byte[] value = (mapEntry.getValue() != null) ? mapEntry.getValue().array() : null;
			raw.put(key, value);
		}
		
		return SerializationUtils.serialize((Serializable) raw);
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

package org.openmrs.eip.app.sender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openmrs.eip.app.CustomDatabaseOffsetBackingStore;
import org.openmrs.eip.app.management.entity.DebeziumOffset;
import org.openmrs.eip.app.management.repository.DebeziumOffsetRepository;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;

import ch.qos.logback.classic.Level;

public class CustomDatabaseOffsetBackingStoreTest extends BaseSenderTest {
	
	@Autowired
	private DebeziumOffsetRepository debeziumOffsetRepository;
	
	@Autowired
	private CustomDatabaseOffsetBackingStore customDatabaseOffsetBackingStore;
	
	@After
	public void reset() {
		customDatabaseOffsetBackingStore.stop();
		Whitebox.setInternalState(customDatabaseOffsetBackingStore, "offset", ((DebeziumOffset) null));
		Whitebox.setInternalState(customDatabaseOffsetBackingStore, "data", new HashMap<>());
	}
	
	@Test
	public void shouldThrowExceptionIfMoreThanOneOffsetExistIdDatabase() {
		DebeziumOffset offset1 = new DebeziumOffset();
		offset1.setDateCreated(new Date());
		offset1.setData(SerializationUtils.serialize((Serializable) Collections.emptyMap()));
		debeziumOffsetRepository.save(offset1);
		
		DebeziumOffset offset2 = new DebeziumOffset();
		offset2.setDateCreated(new Date());
		offset2.setData(SerializationUtils.serialize((Serializable) Collections.emptyMap()));
		debeziumOffsetRepository.save(offset2);
		
		Exception thrown = Assert.assertThrows(RuntimeException.class, () -> customDatabaseOffsetBackingStore.start());
		assertEquals("At most one offset is allowed in the debezium_offset table. Found: 2", thrown.getMessage());
	}
	
	@Test
	public void shouldCreateANewOffset() {
		assertThat(debeziumOffsetRepository.count()).isEqualTo(0);
		
		customDatabaseOffsetBackingStore.start();
		assertMessageLogged(Level.INFO, "No offset was found in the database");
		
		customDatabaseOffsetBackingStore.save();
		assertThat(debeziumOffsetRepository.count()).isEqualTo(1);
		
		DebeziumOffset offset = debeziumOffsetRepository.findAll().get(0);
		assertThat(offset.getDateCreated()).isNotNull();
		assertThat(offset.getDateChanged()).isNull();
		assertThat(offset.getData()).isNotNull();
		
		Object data = SerializationUtils.deserialize(offset.getData());
		assertThat(data).isInstanceOf(Map.class);
	}
	
	@Test
	public void shouldUpdateOffset() {
		assertThat(debeziumOffsetRepository.count()).isEqualTo(0);
		
		customDatabaseOffsetBackingStore.start();
		
		customDatabaseOffsetBackingStore.save();
		assertThat(debeziumOffsetRepository.count()).isEqualTo(1);
		
		DebeziumOffset offset = debeziumOffsetRepository.findAll().get(0);
		assertThat(offset.getDateChanged()).isNull();
		
		customDatabaseOffsetBackingStore.save();
		assertThat(debeziumOffsetRepository.count()).isEqualTo(1);
		
		offset = debeziumOffsetRepository.findAll().get(0);
		assertThat(offset.getDateChanged()).isNotNull();
	}
	
	@Test
	public void shouldLoadExistingOffset() {
		final String A_KEY = "aKey";
		final String A_VALUE = "aValue";
		
		DebeziumOffset offset1 = new DebeziumOffset();
		offset1.setDateCreated(new Date());
		offset1.setDateChanged(LocalDateTime.now());
		
		Map<byte[], byte[]> dataToSave = new HashMap<>();
		dataToSave.put(A_KEY.getBytes(), A_VALUE.getBytes());
		
		offset1.setData(SerializationUtils.serialize((Serializable) dataToSave));
		debeziumOffsetRepository.save(offset1);
		
		customDatabaseOffsetBackingStore.start();
		assertMessageLogged(Level.INFO, "Loading offset from the database");
		
		Map<ByteBuffer, ByteBuffer> loadedData = Whitebox.getInternalState(customDatabaseOffsetBackingStore, "data");
		assertThat(loadedData).isNotNull();
		assertThat(loadedData).hasSize(1);
		
		Entry<ByteBuffer, ByteBuffer> entry = loadedData.entrySet().iterator().next();
		byte[] key = entry.getKey().array();
		byte[] value = entry.getValue().array();
		
		assertThat(key).isEqualTo(A_KEY.getBytes());
		assertThat(value).isEqualTo(A_VALUE.getBytes());
	}
	
}

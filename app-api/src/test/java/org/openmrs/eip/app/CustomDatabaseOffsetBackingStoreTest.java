package org.openmrs.eip.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.openmrs.eip.app.management.entity.DebeziumOffset;
import org.openmrs.eip.app.management.repository.DebeziumOffsetRepository;
import org.openmrs.eip.app.sender.BaseSenderTest;
import org.powermock.reflect.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;

public class CustomDatabaseOffsetBackingStoreTest extends BaseSenderTest {
	
	@Spy
	private CustomFileOffsetBackingStore store;
	
	@Autowired
	private DebeziumOffsetRepository debeziumOffsetRepository;
	
	@After
	public void reset() {
		store.stop();
		Whitebox.setInternalState(store, "data", new HashMap<>());
	}
	
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		Whitebox.setInternalState(CustomFileOffsetBackingStore.class, "paused", false);
		Whitebox.setInternalState(CustomFileOffsetBackingStore.class, "disabled", false);
	}
	
	@Test
	public void save_shouldNotSaveOffsetsIfTheStoreIsPaused() {
		Whitebox.setInternalState(CustomFileOffsetBackingStore.class, "paused", true);
		
		store.save();
		
		Mockito.verify(store, Mockito.never()).doSave();
	}
	
	@Test
	public void save_shouldNotSaveOffsetsIfTheStoreIsDisabled() {
		Whitebox.setInternalState(CustomFileOffsetBackingStore.class, "paused", true);
		
		store.save();
		
		Mockito.verify(store, Mockito.never()).doSave();
	}
	
	@Test
	public void save_shouldSaveOffsetsIfTheStoreIsNotPausedAndIsNotDisabled() throws Exception {
		Mockito.doNothing().when(store).doSave();
		
		store.save();
		
		Mockito.verify(store).doSave();
	}
	
	@Test
	public void shouldCreateANewOffset() {
		assertThat(debeziumOffsetRepository.count()).isEqualTo(0);
		
		store.start();
		
		store.save();
		assertThat(debeziumOffsetRepository.count()).isEqualTo(1);
		
		DebeziumOffset offset = debeziumOffsetRepository.findAll().get(0);
		assertThat(offset.getDateCreated()).isNotNull();
		assertThat(offset.getData()).isNotNull();
		assertThat(offset.getBinlogFileName()).isNotNull();
		assertThat(offset.isEnabled()).isTrue();
		
		Object data = SerializationUtils.deserialize(offset.getData());
		assertThat(data).isInstanceOf(Map.class);
	}
	
	@Test
	public void shouldLoadExistingOffset() {
		final String A_KEY = "aKey";
		final String A_VALUE = "aValue";
		
		DebeziumOffset offset1 = new DebeziumOffset();
		offset1.setDateCreated(new Date());
		offset1.setBinlogFileName("bin1");
		offset1.setEnabled(Boolean.TRUE);
		
		Map<byte[], byte[]> dataToSave = new HashMap<>();
		dataToSave.put(A_KEY.getBytes(), A_VALUE.getBytes());
		
		offset1.setData(SerializationUtils.serialize((Serializable) dataToSave));
		debeziumOffsetRepository.save(offset1);
		
		store.start();
		
		Map<ByteBuffer, ByteBuffer> loadedData = Whitebox.getInternalState(store, "data");
		assertThat(loadedData).isNotNull();
		assertThat(loadedData).hasSize(1);
		
		Entry<ByteBuffer, ByteBuffer> entry = loadedData.entrySet().iterator().next();
		byte[] key = entry.getKey().array();
		byte[] value = entry.getValue().array();
		
		assertThat(key).isEqualTo(A_KEY.getBytes());
		assertThat(value).isEqualTo(A_VALUE.getBytes());
	}
	
	@Test
	public void shouldDisableAllByBinlogFileName() {
		final String A_KEY = "aKey";
		final String A_VALUE = "aValue";
		final String BINLOG_FILENAME_1 = "bin1";
		final String BINLOG_FILENAME_2 = "bin2";
		
		DebeziumOffset offset1 = new DebeziumOffset();
		offset1.setDateCreated(new Date());
		offset1.setBinlogFileName(BINLOG_FILENAME_1);
		offset1.setEnabled(Boolean.TRUE);
		
		DebeziumOffset offset2 = new DebeziumOffset();
		offset2.setDateCreated(new Date());
		offset2.setBinlogFileName(BINLOG_FILENAME_1);
		offset2.setEnabled(Boolean.TRUE);
		
		DebeziumOffset offset3 = new DebeziumOffset();
		offset3.setDateCreated(new Date());
		offset3.setBinlogFileName(BINLOG_FILENAME_2);
		offset3.setEnabled(Boolean.TRUE);
		
		Map<byte[], byte[]> dataToSave = new HashMap<>();
		dataToSave.put(A_KEY.getBytes(), A_VALUE.getBytes());
		
		byte[] serialized = SerializationUtils.serialize((Serializable) dataToSave);
		offset1.setData(serialized);
		offset2.setData(serialized);
		offset3.setData(serialized);
		
		assertThat(debeziumOffsetRepository.countByEnabledTrue()).isEqualTo(0);
		
		debeziumOffsetRepository.save(offset1);
		debeziumOffsetRepository.save(offset2);
		debeziumOffsetRepository.save(offset3);
		
		assertThat(debeziumOffsetRepository.countByEnabledTrue()).isEqualTo(3);
		
		debeziumOffsetRepository.disableAllByBinlogFileName(BINLOG_FILENAME_1, LocalDateTime.now());
		
		assertThat(debeziumOffsetRepository.countByEnabledTrue()).isEqualTo(1);
	}
	
}

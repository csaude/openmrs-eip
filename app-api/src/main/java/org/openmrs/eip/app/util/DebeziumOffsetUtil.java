package org.openmrs.eip.app.util;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.connect.data.SchemaAndValue;
import org.apache.kafka.connect.storage.Converter;

public class DebeziumOffsetUtil {
	
	public static Map<ByteBuffer, ByteBuffer> offsetRawToData(Map<byte[], byte[]> raw) {
		Map<ByteBuffer, ByteBuffer> data = new HashMap<>();
		for (Map.Entry<byte[], byte[]> mapEntry : raw.entrySet()) {
			ByteBuffer key = (mapEntry.getKey() != null) ? ByteBuffer.wrap(mapEntry.getKey()) : null;
			ByteBuffer value = (mapEntry.getValue() != null) ? ByteBuffer.wrap(mapEntry.getValue()) : null;
			data.put(key, value);
		}
		return data;
	}
	
	public static Map<byte[], byte[]> offsetDataToRaw(Map<ByteBuffer, ByteBuffer> data) {
		Map<byte[], byte[]> raw = new HashMap<>();
		for (Map.Entry<ByteBuffer, ByteBuffer> mapEntry : data.entrySet()) {
			byte[] key = (mapEntry.getKey() != null) ? mapEntry.getKey().array() : null;
			byte[] value = (mapEntry.getValue() != null) ? mapEntry.getValue().array() : null;
			raw.put(key, value);
		}
		return raw;
	}
	
	/**
	 * @see {@link org.apache.kafka.connect.storage.OffsetStorageReaderImpl#offsets(java.util.Collection)}
	 */
	public static String getOffsetBinlogFilename(Map<ByteBuffer, ByteBuffer> data, Converter valueConverter,
	        String engineName) {
		for (Map.Entry<ByteBuffer, ByteBuffer> rawEntry : data.entrySet()) {
			SchemaAndValue deserializedSchemaAndValue = valueConverter.toConnectData(engineName,
			    rawEntry.getValue() != null ? rawEntry.getValue().array() : null);
			
			Map<String, Object> value = (Map<String, Object>) deserializedSchemaAndValue.value();
			
			if (value != null && value.containsKey("file")) {
				return (String) value.get("file");
			}
		}
		
		return null;
	}
}

package org.openmrs.eip.app;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.DebeziumException;
import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.relational.history.AbstractFileBasedSchemaHistory;
import io.debezium.relational.history.HistoryRecord;
import io.debezium.relational.history.HistoryRecordComparator;
import io.debezium.relational.history.SchemaHistory;
import io.debezium.relational.history.SchemaHistoryException;
import io.debezium.relational.history.SchemaHistoryListener;
import io.debezium.util.Collect;
import io.debezium.util.Loggings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;

public class CustomHistoryFileStore extends AbstractFileBasedSchemaHistory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomHistoryFileStore.class);
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	public static final Field FIELD_PATH = Field.create(SchemaHistory.CONFIGURATION_FIELD_PREFIX_STRING + "file.name")
	        .withDescription("The path to the file will be" + "use to record the database schema history").required();
	
	private static Collection<Field> ALL_FIELDS = Collect.arrayListOf(FIELD_PATH);
	
	private Path path;
	
	private Path tempPath;
	
	@Override
	public void configure(Configuration config, HistoryRecordComparator comparator, SchemaHistoryListener listener,
	                      boolean useCatalogBeforeSchema) {
		if (!config.validateAndRecord(ALL_FIELDS, LOGGER::error)) {
			throw new DebeziumException("Error configuring " + getClass().getSimpleName());
		}
		
		if (running.get()) {
			throw new SchemaHistoryException("Database schema history already initialized to " + path);
		}
		
		super.configure(config, comparator, listener, useCatalogBeforeSchema);
		path = Paths.get(config.getString(FIELD_PATH));
		tempPath = Paths.get(config.getString(FIELD_PATH) + ".tmp");
	}
	
	@Override
	protected void doStoreRecord(HistoryRecord record) {
		try {
			LOGGER.trace("Storing record into database history: {}", record);
			records.add(record);
			String line = documentWriter.write(record.document());
			
			// Write to a temporary file atomically
			try (FileChannel channel = FileChannel.open(tempPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
			        FileLock lock = channel.lock()) {
				try (BufferedWriter writer = Files.newBufferedWriter(tempPath, StandardOpenOption.APPEND)) {
					writer.append(line);
					writer.newLine();
					writer.flush();
				}
				// Atomically rename temp file to target
				Files.move(tempPath, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
			catch (IOException e) {
				throw new SchemaHistoryException("Unable to write to history file " + path + ": " + e.getMessage(), e);
			}
		}
		catch (IOException e) {
			Loggings.logErrorAndTraceRecord(logger, record, "Failed to convert record to string", e);
		}
	}
	
	@Override
	protected void doStart() {
		// Validate file integrity before reading
		if (storageExists()) {
			
			try (BufferedReader reader = Files.newBufferedReader(path)) {
				String line;
				while ((line = reader.readLine()) != null) {
					try {
						OBJECT_MAPPER.readTree(line); // Validate JSON
					}
					catch (JsonParseException e) {
						LOGGER.info("Corrupted history file detected at {}: {}", path, e.getMessage(), e);
						this.recreateHistoryFile();
					}
				}
			}
			catch (IOException e) {
				throw new SchemaHistoryException("Can't retrieve file with schema history", e);
			}
			// End of file validation
			
			try {
				toHistoryRecord(Files.newInputStream(path));
			}
			catch (IOException e) {
				throw new SchemaHistoryException("Failed to read history file " + path, e);
			}
		}
	}
	
	private void recreateHistoryFile() {
	}
	
	@Override
	public boolean storageExists() {
		return Files.exists(path);
	}
	
	@Override
	public boolean exists() {
		boolean exists = false;
		if (storageExists()) {
			try {
				exists = Files.size(path) > 0;
			}
			catch (IOException e) {
				logger.error("Unable to determine if history file empty " + path, e);
			}
		}
		return exists;
	}
	
	@Override
	public void initializeStorage() {
		try {
			if (path.getParent() != null && !Files.exists(path.getParent())) {
				Files.createDirectories(path.getParent());
			}
			Files.createFile(path);
		}
		catch (IOException e) {
			throw new SchemaHistoryException("Unable to create history file at " + path + ": " + e.getMessage(), e);
		}
	}
	
	@Override
	public void stop() {
		super.stop();
		// Ensure any pending writes are flushed
		try {
			if (Files.exists(tempPath)) {
				Files.delete(tempPath);
			}
		}
		catch (IOException e) {
			logger.warn("Failed to clean up temporary history file {}", tempPath, e);
		}
	}
	
	@Override
	public String toString() {
		return "robust file " + (path != null ? path : "(unstarted)");
	}
	
}

package org.openmrs.eip.app.management.sender;

import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.sql.PreparedStatement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.connect.json.JsonConverter;
import org.apache.kafka.connect.storage.Converter;
import org.apache.kafka.connect.storage.OffsetUtils;
import org.apache.kafka.connect.util.SafeObjectInputStream;
import org.openmrs.eip.app.util.DebeziumOffsetUtil;
import org.openmrs.eip.component.SyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import io.debezium.config.Instantiator;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class MigrateDebeziumOffsetFileToDatabaseChangeSet implements CustomTaskChange {
	
	private static final String DEBEZIUM_OFFSET_FILENAME = "debezium.offsetFilename";
	
	private static final Logger log = LoggerFactory.getLogger(MigrateDebeziumOffsetFileToDatabaseChangeSet.class);
	
	private static final String ERROR_MSG = "Failed to migrate debezium offset file to the database";
	
	@Override
	public void execute(Database database) throws CustomChangeException {
		log.info("Running " + getClass().getSimpleName());
		
		try {
			JdbcConnection conn = (JdbcConnection) database.getConnection();
			
			String offsetPath = SyncContext.getBean(Environment.class).getProperty(DEBEZIUM_OFFSET_FILENAME);
			if (StringUtils.isBlank(offsetPath)) {
				log.info("No \"{}\" property was found. Skipping migration process.", DEBEZIUM_OFFSET_FILENAME);
				return;
			}
			
			File file = new File(offsetPath);
			if (!file.exists()) {
				log.info("No offset file was found in \"{}\". Skipping migration process.", file.getAbsolutePath());
				return;
			}
			
			migrateFileToDatabase(file, conn);
		}
		catch (Exception e) {
			throw new CustomChangeException(ERROR_MSG, e);
		}
	}
	
	private void migrateFileToDatabase(File file, JdbcConnection conn) throws Exception {
		try (SafeObjectInputStream is = new SafeObjectInputStream(new FileInputStream(file))) {
			Object offsetData = is.readObject();
			OffsetUtils.validateFormat(offsetData);
			
			final String SQL = "INSERT INTO debezium_offset (data, binlog_filename, date_created, enabled) VALUES (?,?,?,?)";
			
			try (PreparedStatement statement = conn.prepareStatement(SQL)) {
				statement.setObject(1, offsetData);
				statement.setObject(2, getOffsetBinlogFilename(offsetData));
				statement.setObject(3, new Date());
				statement.setObject(4, Boolean.TRUE);
				
				int rows = statement.executeUpdate();
				if (rows != 1) {
					throw new CustomChangeException(ERROR_MSG);
				}
			}
		}
		
		FileUtils.deleteQuietly(file);
	}
	
	private String getOffsetBinlogFilename(Object offsetData) {
		Converter valueConverter = Instantiator.getInstance(JsonConverter.class.getName(),
		    () -> this.getClass().getClassLoader(), null);
		Map<String, Object> confs = new HashMap<>();
		confs.put("schemas.enable", false);
		valueConverter.configure(confs, false);
		
		Map<ByteBuffer, ByteBuffer> data = DebeziumOffsetUtil.offsetRawToData((Map<byte[], byte[]>) offsetData);
		
		return DebeziumOffsetUtil.getOffsetBinlogFilename(data, valueConverter, "extract");
	}
	
	@Override
	public String getConfirmationMessage() {
		return getClass().getSimpleName() + " completed successfully";
	}
	
	@Override
	public void setUp() throws SetupException {
		
	}
	
	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {
	}
	
	@Override
	public ValidationErrors validate(Database database) {
		return null;
	}
	
}

package org.openmrs.eip.app.management.sender;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import org.apache.kafka.connect.errors.ConnectException;
import org.apache.kafka.connect.util.SafeObjectInputStream;
import org.openmrs.eip.app.config.BeanAwareSpringLiquibase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.DatabaseException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class MigrateDebeziumOffsetFileToDatabaseChangeSet implements CustomTaskChange {
	
	private static final Logger log = LoggerFactory.getLogger(MigrateDebeziumOffsetFileToDatabaseChangeSet.class);
	
	private static final String ERROR_MSG = "Failed to migrate debezium offset file to the database";
	
	@Override
	public void execute(Database database) throws CustomChangeException {
		log.info("Running " + getClass().getSimpleName());
		
		try {
			JdbcConnection conn = (JdbcConnection) database.getConnection();
			
			File file = new File(BeanAwareSpringLiquibase.getProperty("debezium.offsetFilename"));
			
			migrateFileToDatabase(file, conn);
		}
		catch (Exception e) {
			throw new CustomChangeException(ERROR_MSG, e);
		}
	}
	
	private void migrateFileToDatabase(File file, JdbcConnection conn)
	        throws CustomChangeException, DatabaseException, SQLException {
		try (SafeObjectInputStream is = new SafeObjectInputStream(new FileInputStream(file))) {
			final String SQL = "INSERT INTO debezium_offset (data, date_created) VALUES (?,?)";
			
			try (PreparedStatement statement = conn.prepareStatement(SQL)) {
				statement.setBlob(1, is);
				statement.setObject(2, new Date());
				
				int rows = statement.executeUpdate();
				if (rows != 1) {
					throw new CustomChangeException(ERROR_MSG);
				}
			}
		}
		catch (FileNotFoundException | EOFException e) {
			log.info("No debezium offset file was found for migration. {}", file.getPath());
		}
		catch (IOException e) {
			throw new ConnectException(e);
		}
		
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

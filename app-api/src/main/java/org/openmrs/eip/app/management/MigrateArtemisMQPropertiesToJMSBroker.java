package org.openmrs.eip.app.management;

import static org.openmrs.eip.app.management.LiquibasePropertiesHelper.getProperty;

import java.sql.PreparedStatement;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public class MigrateArtemisMQPropertiesToJMSBroker implements CustomTaskChange {
	
	private static final Logger log = LoggerFactory.getLogger(MigrateArtemisMQPropertiesToJMSBroker.class);
	
	private static final String ERROR_MSG = "Failed to migrate Artemis MQ properties from application.properties file to jms_broker table";
	
	@Override
	public void execute(Database database) throws CustomChangeException {
		log.info("Running " + getClass().getSimpleName());
		
		if (getProperty("spring.artemis.host") == null) {
			log.info("No 'spring.artemis.*' configurations found. Skipping migration.");
			return;
		}
		
		try {
			JdbcConnection conn = (JdbcConnection) database.getConnection();
			
			final String SQL = "INSERT INTO jms_broker (identifier, name, host, port,"
			        + " username, password, date_created) VALUES (?,?,?,?,?,?,?)";
			
			try (PreparedStatement s = conn.prepareStatement(SQL)) {
				s.setString(1, "default");
				s.setString(2, "default");
				s.setString(3, getProperty("spring.artemis.host"));
				s.setString(4, getProperty("spring.artemis.port"));
				s.setString(5, getProperty("spring.artemis.user"));
				s.setString(6, getProperty("spring.artemis.password"));
				s.setObject(7, new Date());
				
				int rows = s.executeUpdate();
				if (rows != 1) {
					throw new CustomChangeException(ERROR_MSG);
				}
			}
		}
		catch (Exception e) {
			throw new CustomChangeException(ERROR_MSG, e);
		}
	}
	
	@Override
	public String getConfirmationMessage() {
		return getClass().getSimpleName() + " completed successfully.";
	}
	
	@Override
	public void setUp() throws SetupException {
		
	}
	
	@Override
	public void setFileOpener(ResourceAccessor resourceAccessor) {
		
	}
	
	@Override
	public ValidationErrors validate(Database database) {
		//		ValidationErrors errors = new ValidationErrors();
		//		try {
		//			JdbcConnection conn = (JdbcConnection) database.getConnection();
		//			
		//			final String SQL = "SELECT COUNT(*) FROM jms_broker";
		//			
		//			try (PreparedStatement s = conn.prepareStatement(SQL)) {
		//				int rows = s.executeUpdate();
		//				if (rows != 0) {
		//					errors.addWarning("There is already data in jms_broker table");
		//				}
		//			}
		//		}
		//		catch (Exception e) {
		//			errors.addError(ERROR_MSG + " : " + e.getMessage());
		//		}
		//		
		//		return errors;
		
		return null;
	}
	
}

package org.openmrs.eip.app.management;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.management.entity.sender.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.SetupException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;

public abstract class BaseItemMigrationChangeSet implements CustomTaskChange {
	
	private static final Logger LOG = LoggerFactory.getLogger(BaseItemMigrationChangeSet.class);
	
	private static final String TABLE = "sender_event";
	
	private static final String SOURCE_TABLE_PLACEHOLDER = "$sourceTable";
	
	private static final String INSERT_QUERY = "INSERT INTO " + TABLE
	        + " (table_name,identifier,operation,snapshot,request_uuid,primary_key_id) VALUES(?,?,?,?,?,?)";
	
	private static final String UPDATE_QUERY = "UPDATE " + SOURCE_TABLE_PLACEHOLDER + " SET event_id = ? WHERE id = ?";
	
	private String sourceTable;
	
	private boolean includePrimaryKey;
	
	private String query;
	
	@Override
	public void execute(Database database) throws CustomChangeException {
		LOG.info("Migrating items in " + sourceTable + " to the " + TABLE + " table");
		
		List<String> columns = new ArrayList<>(
		        List.of("id", "table_name", "identifier", "operation", "snapshot", "request_uuid"));
		if (includePrimaryKey) {
			columns.add("primary_key_id");
		}
		
		query = "SELECT " + StringUtils.join(columns, ",") + " FROM LIMIT 100" + sourceTable;
		try {
			int migrationCount = 0;
			List<Event> events = getNextBatch(getConnection(database));
			while (!events.isEmpty()) {
				migrationCount += events.size();
				for (Event event : events) {
					migrateItem(event, getConnection(database));
				}
				
				events = getNextBatch(getConnection(database));
			}
			
			LOG.info("Successfully migrated " + migrationCount + " item(s) in " + sourceTable);
		}
		catch (Exception e) {
			throw new CustomChangeException("An error occurred while to migrating items in " + sourceTable, e);
		}
	}
	
	private JdbcConnection getConnection(Database db) {
		return (JdbcConnection) db.getConnection();
	}
	
	protected void migrateItem(Event event, JdbcConnection connection) throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Migrating item with id: " + event.getId() + " in " + sourceTable + " table");
		}
		
		boolean commit = connection.getAutoCommit();
		try (PreparedStatement insertStmt = connection.prepareStatement(INSERT_QUERY, Statement.RETURN_GENERATED_KEYS);
		        PreparedStatement updateStmt = connection.prepareStatement(UPDATE_QUERY)) {
			
			connection.setAutoCommit(false);
			
			insertStmt.setString(1, event.getTableName());
			insertStmt.setString(2, event.getIdentifier());
			insertStmt.setString(3, event.getOperation());
			insertStmt.setBoolean(4, event.getSnapshot());
			insertStmt.setString(5, event.getRequestUuid());
			insertStmt.setString(6, event.getPrimaryKeyId());
			int insertCount = insertStmt.executeUpdate();
			if (insertCount != 1) {
				throw new CustomChangeException("Failed to insert row into " + TABLE + " associated to the item with id: "
				        + event.getId() + " in " + sourceTable + " table");
			}
			
			try (ResultSet rs = insertStmt.getGeneratedKeys()) {
				if (!rs.next()) {
					throw new CustomChangeException("No event id returned after inserting row into " + TABLE
					        + " associated to the item with id: " + event.getId() + " in " + sourceTable + " table");
				}
				
				rs.last();
				if (rs.getRow() != 1) {
					throw new CustomChangeException("Found multiple auto generated keys after inserting row into " + TABLE
					        + " associated to the item with id: " + event.getId() + " in " + sourceTable + " table");
				}
				
				updateStmt.setLong(1, rs.getLong(1));
			}
			
			updateStmt.setLong(2, event.getId());
			int updateCount = updateStmt.executeUpdate();
			if (updateCount != 1) {
				throw new CustomChangeException(
				        "Failed to set event_id for item with id: " + event.getId() + " in " + sourceTable + " table");
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Committing transaction to migrate event associated to the item with id: " + event.getId() + " in "
				        + sourceTable + " table");
			}
			
			connection.commit();
			
			if (LOG.isDebugEnabled()) {
				LOG.debug("Successfully migrated item with id: " + event.getId() + " in " + sourceTable + " table");
			}
		}
		finally {
			connection.setAutoCommit(commit);
		}
	}
	
	private List<Event> getNextBatch(JdbcConnection connection) throws Exception {
		List<Event> events = new ArrayList<>();
		try (Statement s = connection.createStatement(); ResultSet rs = s.executeQuery(query)) {
			while (rs.next()) {
				Event event = new Event();
				event.setId(rs.getLong(1));
				event.setTableName(rs.getString(2));
				event.setIdentifier(rs.getString(3));
				event.setOperation(rs.getString(4));
				event.setSnapshot(rs.getBoolean(5));
				event.setRequestUuid(rs.getString(6));
				if (includePrimaryKey) {
					event.setPrimaryKeyId(rs.getString(7));
				}
				events.add(event);
			}
		}
		
		return events;
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

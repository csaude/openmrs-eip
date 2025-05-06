package org.openmrs.eip.app;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.debezium.DebeziumException;
import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.document.Array;
import io.debezium.document.Document;
import io.debezium.relational.Column;
import io.debezium.relational.Table;
import io.debezium.relational.TableEditor;
import io.debezium.relational.TableId;
import io.debezium.relational.history.AbstractFileBasedSchemaHistory;
import io.debezium.relational.history.HistoryRecord;
import io.debezium.relational.history.HistoryRecordComparator;
import io.debezium.relational.history.SchemaHistory;
import io.debezium.relational.history.SchemaHistoryException;
import io.debezium.relational.history.SchemaHistoryListener;
import io.debezium.util.Collect;
import io.debezium.util.Loggings;
import org.openmrs.eip.app.sender.BinlogUtils;
import org.openmrs.eip.component.SyncContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hibernate.type.SqlTypes.isCharacterType;
import static org.openmrs.eip.app.sender.SenderConstants.PROP_DBZM_DB_PASSWORD;
import static org.openmrs.eip.app.sender.SenderConstants.PROP_DBZM_DB_USER;
import static org.openmrs.eip.component.Constants.PROP_DEBEZIUM_OFFSET_FILE_NAME;
import static org.openmrs.eip.component.Constants.PROP_OPENMRS_DB_HOST;
import static org.openmrs.eip.component.Constants.PROP_OPENMRS_DB_NAME;
import static org.openmrs.eip.component.Constants.PROP_OPENMRS_DB_PORT;

@Component
public class CustomHistoryFileStore extends AbstractFileBasedSchemaHistory {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomHistoryFileStore.class);
	
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	
	public static final Field FIELD_PATH = Field.create(SchemaHistory.CONFIGURATION_FIELD_PREFIX_STRING + "file.filename")
	        .withDescription("The path to the file will be" + "use to record the database schema history").required();
	
	private static Collection<Field> ALL_FIELDS = Collect.arrayListOf(FIELD_PATH);
	
	private Path path;
	
	@Override
	protected void doStoreRecord(HistoryRecord record) {
		try {
			LOGGER.trace("Storing record into database history: {}", record);
			records.add(record);
			String line = documentWriter.write(record.document());
			
			try (BufferedWriter historyWriter = Files.newBufferedWriter(path, StandardOpenOption.APPEND)) {
				try {
					historyWriter.append(line);
					historyWriter.newLine();
				}
				catch (IOException e) {
					Loggings.logErrorAndTraceRecord(logger, record, "Failed to add record to history at {}", path, e);
				}
			}
			catch (IOException e) {
				throw new SchemaHistoryException("Unable to create writer for history file " + path + ": " + e.getMessage(),
				        e);
			}
		}
		catch (IOException e) {
			Loggings.logErrorAndTraceRecord(logger, record, "Failed to convert record to string", e);
		}
	}
	
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
	}
	
	@Override
	protected void doStart() {
		
		try {
			this.validateHistoryFile();
			toHistoryRecord(Files.newInputStream(path));
		}
		catch (IOException e) {
			throw new SchemaHistoryException("Failed to read history file " + path, e);
		}
	}
	
	/***
	 * This validates the history file, if it is invalid will trigger a JsonParseException
	 * and @recreateHistoryFile
	 */
	private void validateHistoryFile() throws IOException {
		if (storageExists()) {
			try (BufferedReader reader = Files.newBufferedReader(path)) {
				String line;
				while ((line = reader.readLine()) != null) {
					try {
						OBJECT_MAPPER.readTree(line);
					}
					catch (JsonParseException e) {
						LOGGER.info("Corrupted history file detected at {}: {}", path, e.getMessage(), e);
						this.recreateHistoryFile();
					}
				}
			}
		}
	}
	
	private void recreateHistoryFile() {
		LOGGER.info("Starting the process to recreate history File {}", path);
		try {
			if (Files.exists(path)) {
				Files.delete(path);
			}
			
			LOGGER.debug("Recreating clean history file at {}", path);
			initializeStorage();
			this.rebuildSchemaFromDatabase();
			LOGGER.info("History file recreated successfully at {}", path);
		}
		catch (IOException e) {
			throw new SchemaHistoryException("Failed to recreate history file at " + path + ": " + e.getMessage(), e);
		}
	}
	
	public void rebuildSchemaFromDatabase() {
		
		Environment env = SyncContext.getBean(Environment.class);
		final String host = env.getProperty(PROP_OPENMRS_DB_HOST);
		final String port = env.getProperty(PROP_OPENMRS_DB_PORT);
		final String user = env.getProperty(PROP_DBZM_DB_USER);
		final String password = env.getProperty(PROP_DBZM_DB_PASSWORD);
		final String databaseName = env.getProperty(PROP_OPENMRS_DB_NAME);
		final Map<String, ?> offset = readCurrentOffset();
		final String url = "jdbc:mysql://" + host + ":" + port + BinlogUtils.URL_QUERY;
		
		try (Connection conn = DriverManager.getConnection(url, user, password)) {
			DatabaseMetaData meta = conn.getMetaData();
			ResultSet rs = meta.getTables(databaseName, null, "%", new String[] { "TABLE" });
			Set<TableId> tableIdsToWatch = new HashSet<>();
			
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				tableIdsToWatch.add(new TableId(databaseName, null, tableName));
			}
			
			// Create header of history file
			this.createHistoryFileHeader(tableIdsToWatch, offset, databaseName);
			
			for (TableId tableId : tableIdsToWatch) {
				Table table = buildTableSchema(meta, tableId, conn);
				if (table == null) {
					LOGGER.debug("Failed to build schema for table: {}", tableId);
					continue;
				}
				
				String ddl = getTableDDL(conn, tableId);
				if (ddl == null) {
					LOGGER.debug("Failed to retrieve DDL for table: {}", tableId);
					continue;
				}
				
				this.createHistoryRecord(conn, tableId, ddl, offset, table, databaseName);
				LOGGER.debug("Stored history record for table: {}", tableId);
			}
			
			this.generateAnalyseSource(tableIdsToWatch, offset);
			rs.close();
			
			LOGGER.debug("Actual tables in {}: {}", databaseName, tableIdsToWatch);
		}
		catch (Exception e) {
			throw new SchemaHistoryException("Failed to rebuild schema from database: " + e.getMessage(), e);
		}
	}
	
	private static String getServerName() {
		Environment env = SyncContext.getBean(Environment.class);
		return env.getProperty(PROP_OPENMRS_DB_HOST);
	}
	
	private void createHistoryRecord(Connection conn, TableId tableId, String ddl, Map<String, ?> offset, Table table,
	                                 String databaseName)
	    throws SQLException {
		// Create history record
		Document historyDoc = Document.create();
		historyDoc.set("source", Document.create("server", getServerName()));
		historyDoc.set("position", createPositionDocument(offset, "INITIAL"));
		historyDoc.set("ts_ms", System.currentTimeMillis());
		historyDoc.set("databaseName", databaseName);
		historyDoc.set("ddl", ddl);
		
		// Create tableChanges
		Array tableChanges = Array.create();
		
		Document tableChange = Document.create();
		tableChange.set("type", "CREATE");
		tableChange.set("id", tableId.toDoubleQuotedString());
		Document tableDoc = Document.create();
		tableDoc.set("defaultCharsetName", getTableCharset(conn, tableId));
		tableDoc.set("primaryKeyColumnNames", Array.create(table.primaryKeyColumnNames()));
		tableDoc.set("columns", Array.create(serializeColumns(table)));
		tableDoc.set("attributes", Array.create());
		tableChange.set("table", tableDoc);
		tableChange.set("comment", null);
		
		tableChanges.add(tableChange);
		historyDoc.set("tableChanges", tableChanges);
		
		HistoryRecord historyRecord = new HistoryRecord(historyDoc);
		doStoreRecord(historyRecord);
	}
	
	private void createHistoryFileHeader(Set<TableId> tableIdsToWatch, Map<String, ?> offset, String databaseName) {
		storeServerSettings(databaseName, offset);
		this.generateDropSource(tableIdsToWatch, offset);
		this.generateDropDatabase(databaseName, offset);
		this.generateCreateDatabase(databaseName, offset);
		this.generateUseDatabase(databaseName, offset);
		
	}
	
	private void generateDropDatabase(String databaseName, Map<String, ?> offsetMap) {
		
		Document historyDoc = createBaseHistoryDoc(offsetMap, getServerName(), databaseName, "INITIAL");
		historyDoc.set("ddl", "DROP DATABASE IF EXISTS `" + databaseName + "`");
		historyDoc.set("tableChanges", Array.create());
		
		storeHistoryRecord(historyDoc);
	}
	
	private void generateCreateDatabase(String databaseName, Map<String, ?> offsetMap) {
		Document historyDoc = createBaseHistoryDoc(offsetMap, getServerName(), databaseName, "INITIAL");
		historyDoc.set("ddl", "CREATE DATABASE `" + databaseName + "` CHARSET utf8mb3 COLLATE utf8mb3_general_ci");
		historyDoc.set("tableChanges", Array.create());
		
		storeHistoryRecord(historyDoc);
	}
	
	private void generateUseDatabase(String databaseName, Map<String, ?> offsetMap) {
		Document historyDoc = createBaseHistoryDoc(offsetMap, getServerName(), databaseName, "INITIAL");
		historyDoc.set("ddl", "USE `" + databaseName + "`");
		historyDoc.set("tableChanges", Array.create());
		
		storeHistoryRecord(historyDoc);
	}
	
	private void generateAnalyseSource(Set<TableId> tableIdsToWatch, Map<String, ?> offsetMap) {
		tableIdsToWatch.forEach(tableId -> {
			Document historyDoc = createBaseHistoryDoc(offsetMap, getServerName(), tableId.catalog(), false);
			historyDoc.set("ddl", "ANALYZE TABLE " + tableId.identifier());
			historyDoc.set("tableChanges", Array.create());
			
			storeHistoryRecord(historyDoc);
		});
	}
	
	private void generateDropSource(Set<TableId> tableIdsToWatch, Map<String, ?> offsetMap) {
		tableIdsToWatch.forEach(tableId -> {
			Document historyDoc = createBaseHistoryDoc(offsetMap, getServerName(), tableId.catalog(), "INITIAL");
			String tableSchema = "`" + tableId.catalog() + "`.`" + tableId.table() + "`";
			
			historyDoc.set("ddl", "DROP TABLE IF EXISTS " + tableSchema);
			Document tableChange = Document.create();
			tableChange.setString("type", "DROP");
			tableChange.set("id", tableSchema);
			
			historyDoc.set("tableChanges", Array.create(List.of(tableChange)));
			
			storeHistoryRecord(historyDoc);
		});
	}
	
	private void storeHistoryRecord(Document historyDoc) {
		HistoryRecord historyRecord = new HistoryRecord(historyDoc);
		doStoreRecord(historyRecord);
	}
	
	private Document createBaseHistoryDoc(Map<String, ?> offsetMap, String server, String databaseName,
	                                      Object snapshotValue) {
		Document historyDoc = Document.create();
		historyDoc.set("source", Document.create("server", server));
		historyDoc.set("position", createPositionDocument(offsetMap, snapshotValue));
		historyDoc.set("ts_ms", System.currentTimeMillis());
		historyDoc.set("databaseName", databaseName);
		return historyDoc;
	}
	
	private Document createPositionDocument(Map<String, ?> offset, Object snapshotValue) {
		Document position = Document.create();
		if (offset.isEmpty()) {
			position.set("snapshot", snapshotValue);
		} else {
			position.set("ts_sec", System.currentTimeMillis() / 1000);
			position.set("file", offset.get("file"));
			position.set("pos", offset.get("pos"));
			position.set("snapshot", "INITIAL");
		}
		return position;
	}
	
	private void storeServerSettings(String databaseName, Map<String, ?> offset) {
		Document historyDoc = Document.create();
		historyDoc.set("source", Document.create("server", getServerName()));
		historyDoc.set("position", this.createPositionDocument(offset, "INITIAL"));
		historyDoc.set("ts_ms", System.currentTimeMillis());
		historyDoc.set("databaseName", databaseName);
		historyDoc.set("ddl", "SET character_set_server=utf8mb4, collation_server=utf8mb4_0900_ai_ci");
		historyDoc.set("tableChanges", Document.create());
		
		HistoryRecord historyRecord = new HistoryRecord(historyDoc);
		doStoreRecord(historyRecord);
		LOGGER.info("Stored server-level settings record");
	}
	
	private boolean isLengthApplicable(String typeName) {
		return typeName.equals("CHAR") || typeName.equals("VARCHAR") || typeName.equals("TINYINT")
		        || typeName.equals("SMALLINT") || typeName.equals("INTEGER") || typeName.equals("BIGINT");
	}
	
	private Table buildTableSchema(DatabaseMetaData meta, TableId tableId, Connection connection) throws SQLException {
		TableEditor editor = Table.editor().tableId(tableId);
		
		try (ResultSet rs = meta.getColumns(tableId.catalog(), tableId.schema(), tableId.table(), "%")) {
			while (rs.next()) {
				String columnName = rs.getString("COLUMN_NAME");
				int sqlType = rs.getInt("DATA_TYPE");
				String typeName = rs.getString("TYPE_NAME");
				int length = rs.getInt("COLUMN_SIZE");
				boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
				String defaultValue = rs.getString("COLUMN_DEF");
				String charsetName = fetchColumnCharset(connection, tableId, columnName);
				boolean autoIncremented = "YES".equals(rs.getString("IS_AUTOINCREMENT"));
				
				// Only set charset for character-based types
				if (!isCharacterType(sqlType)) {
					charsetName = null;
				}
				
				if (sqlType == Types.TIMESTAMP && typeName.equals("TIMESTAMP")) {
					sqlType = Types.TIMESTAMP_WITH_TIMEZONE;
				}
				
				// Handle TINYINT default value to avoid Short type issue
				if (sqlType == Types.TINYINT) {
					sqlType = Types.BIT;
					defaultValue = "0";
				}
				
				editor.addColumn(Column.editor().name(columnName).jdbcType(sqlType).type(typeName).length(length)
				        .optional(nullable).autoIncremented(autoIncremented).defaultValueExpression(defaultValue)
				        .charsetName(charsetName).create());
			}
		}
		
		// Fetch primary keys
		try (ResultSet rs = meta.getPrimaryKeys(tableId.catalog(), tableId.schema(), tableId.table())) {
			List<String> pkColumns = new ArrayList<>();
			while (rs.next()) {
				pkColumns.add(rs.getString("COLUMN_NAME"));
			}
			editor.setPrimaryKeyNames(pkColumns);
		}
		
		return editor.create();
	}
	
	private String fetchColumnCharset(Connection conn, TableId tableId, String columnName) throws SQLException {
		String sql = """
		        SELECT CHARACTER_SET_NAME
		        FROM information_schema.columns
		        WHERE table_schema = ?
		          AND table_name = ?
		          AND column_name = ?
		        """;
		
		try (PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, tableId.catalog());
			stmt.setString(2, tableId.table());
			stmt.setString(3, columnName);
			
			try (ResultSet rs = stmt.executeQuery()) {
				if (rs.next()) {
					return rs.getString("CHARACTER_SET_NAME");
				}
			}
		}
		return null;
	}
	
	private String getTableDDL(Connection conn, TableId tableId) throws SQLException {
		try (Statement stmt = conn.createStatement();
		        ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE " + tableId.catalog() + "." + tableId.table());) {
			if (rs.next()) {
				return rs.getString("Create Table");
			}
		}
		return null;
	}
	
	private String getTableCharset(Connection conn, TableId tableId) throws SQLException {
		try (Statement stmt = conn.createStatement();
		        ResultSet rs = stmt.executeQuery(
		            "SHOW TABLE STATUS FROM `" + tableId.catalog() + "` WHERE NAME = '" + tableId.table() + "'")) {
			if (rs.next()) {
				String collation = rs.getString("Collation");
				if (collation != null) {
					if (collation.startsWith("utf8mb4")) {
						return "utf8mb4";
					} else if (collation.startsWith("latin1")) {
						return "latin1";
					} else if (collation.startsWith("utf8mb3")) {
						return "utf8mb3";
					} else if (collation.startsWith("utf8")) {
						return "utf8";
					}
				}
			}
		}
		return "utf8mb3";
	}
	
	private List<Document> serializeColumns(Table table) {
		List<Document> columns = new ArrayList<>();
		for (Column column : table.columns()) {
			Document colDoc = Document.create();
			colDoc.set("name", column.name());
			colDoc.set("jdbcType", column.jdbcType());
			colDoc.set("typeName", column.typeName());
			colDoc.set("typeExpression", column.typeName());
			colDoc.set("charsetName", column.charsetName());
			if (column.length() != 0 && isLengthApplicable(column.typeName()))
				colDoc.set("length", column.length());
			colDoc.set("position", column.position());
			colDoc.set("optional", column.isOptional());
			colDoc.set("autoIncremented", column.isAutoIncremented());
			colDoc.set("generated", column.isAutoIncremented());
			colDoc.set("comment", column.comment() == null ? null : column.comment());
			colDoc.set("hasDefaultValue", false);
			column.defaultValueExpression().ifPresent(value -> {
				colDoc.setString("defaultValueExpression", value);
				colDoc.set("hasDefaultValue", true);
			});
			colDoc.set("enumValues", Array.create(column.enumValues()));
			
			if (column.jdbcType() == Types.BIT) {
				colDoc.set("jdbcType", 5);
				colDoc.set("typeName", "TINYINT");
				colDoc.set("typeExpression", "TINYINT");
				colDoc.set("length", 1);
			}
			
			if (column.name().equals("uuid")) {
				colDoc.set("hasDefaultValue", false);
			}
			
			columns.add(colDoc);
		}
		return columns;
	}
	
	private static Map<String, ?> readCurrentOffset() {
		Environment env = SyncContext.getBean(Environment.class);
		final String offsetPath = env.getProperty(PROP_DEBEZIUM_OFFSET_FILE_NAME);
		
		if (offsetPath == null) {
			throw new DebeziumException("Debezium offset filename not set");
		}
		Path offsetFilePath = Paths.get(offsetPath);
		
		if (!Files.exists(offsetFilePath)) {
			return Collections.emptyMap();
		}
		
		if (!Files.exists(offsetFilePath)) {
			throw new IllegalArgumentException("Offset file path does not exist at: " + offsetFilePath);
		}
		
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(offsetFilePath.toFile()))) {
			Object obj = ois.readObject();
			
			Map<?, ?> offsetMap = (Map<?, ?>) obj;
			ObjectMapper objectMapper = new ObjectMapper();
			
			for (Map.Entry<?, ?> entry : offsetMap.entrySet()) {
				Object value = entry.getValue();
				
				if (value instanceof byte[]) {
					String json = new String((byte[]) value, StandardCharsets.UTF_8);
					return objectMapper.readValue(json, Map.class);
				}
			}
			return Collections.emptyMap();
		}
		catch (Exception e) {
			throw new RuntimeException("An error occurred trying to read offset file", e);
		}
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
				exists = true;
			}
			catch (IOException e) {
				logger.error("Unable to determine if history file empty {}", path, e);
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
	public String toString() {
		return "robust file " + (path != null ? path : "(unstarted)");
	}
}

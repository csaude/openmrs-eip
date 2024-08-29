package org.openmrs.eip.app;

import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang3.StringUtils;
import org.openmrs.eip.app.management.entity.AbstractEntity;
import org.openmrs.eip.component.SyncContext;
import org.openmrs.eip.component.exception.EIPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Table;

/**
 * Base class for tasks that operate by moving items in a batch from one table to another.
 */
public abstract class BaseMovingTask<T extends AbstractEntity> extends BaseQueueTask<T> {
	
	protected static final Logger LOG = LoggerFactory.getLogger(BaseMovingTask.class);
	
	private static final String PLACE_HOLDER_IDS = "IDS";

    private static DataSource dataSource;
	
	private String deleteQuery;
	
	@Override
	public void process(List<T> items) throws Exception {
		if (dataSource == null) {
			this.dataSource = SyncContext.getBean(SyncConstants.MGT_DATASOURCE_NAME);
		}
		
		if (deleteQuery == null) {
			deleteQuery = "DELETE FROM " + getSourceTableName() + " WHERE id IN (" + PLACE_HOLDER_IDS + ")";
		}
		
		try (Connection conn = dataSource.getConnection();
		        PreparedStatement insertStmt = conn.prepareStatement(getInsertQuery());
		        Statement deleteStmt = conn.createStatement()) {
			boolean autoCommit = conn.getAutoCommit();
			try {
				conn.setAutoCommit(false);
				int count = items.size();
				List<Long> ids = new ArrayList<>(count);
				for (T item : items) {
					addItem(insertStmt, item);
					ids.add(item.getId());
					insertStmt.addBatch();
				}
				
				if (log.isDebugEnabled()) {
					log.debug("Saving items in batch of {}", count);
				}
				
				int[] rows = insertStmt.executeBatch();
				if (rows.length != count) {
					throw new Exception("Expected " + count + " sync items to be inserted but was " + rows.length);
				}
				
				if (log.isDebugEnabled()) {
					log.debug("Removing items in batch of {}", count);
				}
				
				final String delQuery = deleteQuery.replace(PLACE_HOLDER_IDS, StringUtils.join(ids, ","));
				int deleted = deleteStmt.executeUpdate(delQuery);
				if (deleted != count) {
					throw new Exception("Expected " + count + " items to be deleted but was " + deleted);
				}
				
				conn.commit();
			}
			catch (Throwable t) {
				conn.rollback();
				throw new EIPException("An error occurred while processing a batch of items", t);
			}
			finally {
				conn.setAutoCommit(autoCommit);
			}
		}
		
	}
	
	private String getSourceTableName() {
		Class<T> persistentClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass())
		        .getActualTypeArguments()[0];
		return persistentClass.getAnnotation(Table.class).name();
	}
	
	/**
	 * Gets the parameterized insert query.
	 * 
	 * @return the query
	 */
	protected abstract String getInsertQuery();
	
	/**
	 * Adds the item to the prepared insert statement.
	 * 
	 * @param insertStatement prepared insert statement
	 * @param item the item to add
	 */
	protected abstract void addItem(PreparedStatement insertStatement, T item) throws SQLException;
	
}

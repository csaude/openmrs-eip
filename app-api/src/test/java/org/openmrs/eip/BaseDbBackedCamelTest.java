package org.openmrs.eip;

import static org.openmrs.eip.app.SyncConstants.MGT_TX_MGR;

import javax.sql.DataSource;

import org.openmrs.eip.app.SyncConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for tests for routes that require access to the management and OpenMRS databases.
 */
@Import({ TestDBConfig.class })
@Transactional(transactionManager = MGT_TX_MGR)
@TestExecutionListeners(value = { ResetDbsTestExecutionListener.class, SqlScriptsTestExecutionListener.class,
        TransactionalTestExecutionListener.class })
@TestPropertySource(properties = "spring.jpa.properties.hibernate.hbm2ddl.auto=update")
@TestPropertySource(properties = "spring.mngt-datasource.driverClassName=org.h2.Driver")
@TestPropertySource(properties = "spring.mngt-datasource.jdbcUrl=jdbc:h2:mem:test;DB_CLOSE_DELAY=30;LOCK_TIMEOUT=10000")
@TestPropertySource(properties = "spring.mngt-datasource.username=sa")
@TestPropertySource(properties = "spring.mngt-datasource.password=test")
@TestPropertySource(properties = "spring.mngt-datasource.dialect=org.hibernate.dialect.H2Dialect")
@TestPropertySource(properties = "spring.mngt-datasource.connection-timeout=300000")
@TestPropertySource(properties = "spring.openmrs-datasource.connection-timeout=300000")
public abstract class BaseDbBackedCamelTest extends BaseCamelTest {
	
	@Autowired
	@Qualifier(SyncConstants.MGT_DATASOURCE_NAME)
	protected DataSource mngtDataSource;
	
	@Autowired
	@Qualifier(SyncConstants.OPENMRS_DATASOURCE_NAME)
	protected DataSource openmrsDataSource;
	
}

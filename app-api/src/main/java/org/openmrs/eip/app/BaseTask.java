package org.openmrs.eip.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for tasks
 */
public abstract class BaseTask implements Task {
	
	protected static final Logger log = LoggerFactory.getLogger(BaseTask.class);
	
	@Override
	public boolean skip() {
		return false;
	}
	
	@Override
	public void run() {
		final String originalThreadName = Thread.currentThread().getName();
		
		try {
			Thread.currentThread().setName(Thread.currentThread().getName() + ":" + getTaskName());
			if (skip()) {
				if (log.isTraceEnabled()) {
					log.trace("Skipping");
				}
				
				return;
			}
			
			if (AppUtils.isShuttingDown()) {
				if (log.isDebugEnabled()) {
					log.debug("Skipping run because the application is stopping");
				}
				
				return;
			}
			
			beforeStart();
			
			if (log.isDebugEnabled()) {
				log.debug("Starting");
			}
			
			try {
				doRun();
			}
			catch (Throwable t) {
				if (!AppUtils.isShuttingDown()) {
					String msg = "An error has been encountered";
					if (log.isDebugEnabled()) {
						log.error(msg, t);
					} else {
						log.warn(msg);
					}
				}
			}
			
			beforeStop();
			
			if (log.isDebugEnabled()) {
				log.debug("Stopping");
			}
		}
		finally {
			Thread.currentThread().setName(originalThreadName);
		}
	}
	
	/**
	 * Called before the task starts running
	 */
	public void beforeStart() {
	}
	
	/**
	 * Called before the task stops running
	 */
	public void beforeStop() {
	}
	
	/**
	 * Gets the logical task name
	 *
	 * @return the task name
	 */
	public abstract String getTaskName();
	
	/**
	 * Subclasses should add their implementation logic in this method
	 * 
	 * @throws Exception
	 */
	public abstract void doRun() throws Exception;
	
}

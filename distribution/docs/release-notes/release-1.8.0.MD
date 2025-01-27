# Release Notes - DB Sync Version 1.8.0

The documentation for the property can be found in
the [receiver application.properties file](../receiver/application.properties)

#### New Properties

```
jms.send.batch.disabled
jms.send.batch.size
```
            
<h2>        New Features
</h2>
<ul>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-623'>EC-623</a>] -         Receiver should process sync Jms messages in parallel where applicable
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-677'>EC-677</a>] -         Add support for sync of esaudefeatures_rps_import_log table
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-679'>EC-679</a>] -         Add a task to clear entire cache and update search index
</li>
</ul>
                                                                                   
<h2>        Improvement
</h2>
<ul>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-483'>EC-483</a>] -         Add support for batch sync messages
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-648'>EC-648</a>] -         Add subclass of BaseSyncProcessor to sync a merged conflict
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-681'>EC-681</a>] -         Truncate uuids and dates in UI
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-683'>EC-683</a>] -         Sender should squash events for the same entity
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-685'>EC-685</a>] -         Debezium and deleted entity instance should be saved in a single transaction
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-706'>EC-706</a>] -         Tasks should stop after processing each batch
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-707'>EC-707</a>] -         Process JMS messages in batches
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-708'>EC-708</a>] -         Last sync date for a site should be updated on receiving a message
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-720'>EC-720</a>] -         Index frequently searched columns in receiver_retry_queue and conflict_queue_item tables
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-723'>EC-723</a>] -         Archive and prune synced items in batches
</li>
</ul>

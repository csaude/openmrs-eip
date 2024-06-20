# Release Notes - DB Sync Version 1.7.0

<h2>        New Feature
</h2>
<ul>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-458'>EC-458</a>] -         Receiver retry queue should support parallel processing
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-633'>EC-633</a>] -         Add receiver reconciliation UI
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-634'>EC-634</a>] -         Add sender reconciliation UI
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-660'>EC-660</a>] -         Add dbsync version on sync messages payloads
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-661'>EC-661</a>] -         Add support for OpenMRS 2.6
</li>
</ul>

<h2>        Bug
</h2>
<ul>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-637'>EC-637</a>] -         Errors are randomly thrown when processing a sync message
</li>
</ul>

<h2>        Improvement
</h2>
<ul>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-477'>EC-477</a>] -         Refactor SiteMessageConsumer to extend BaseRunnable
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-639'>EC-639</a>] -         Add missing tests
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-643'>EC-643</a>] -         Convert OpenmrsLoadProducer from a camel endpoint to a regular spring bean
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-645'>EC-645</a>] -         Convert receiver dbsync route to a spring bean
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-646'>EC-646</a>] -         Convert message processor route to a spring bean
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-647'>EC-647</a>] -         Convert receiver retry route to a spring bean
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-659'>EC-659</a>] -         Add mandatory date_received column to receiver_sync_msg table
</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-675'>EC-675</a>] -         Add dbsync version to ReconciliationMessage
</li>
</ul>

# Release Notes - DB Sync Version 1.6.7

The documentation for the property can be found in
the [receiver application.properties file](../receiver/application.properties)

#### New Properties

```
archive.task.disabled
archive.task.initial.delay
archive.task.delay
```

#### Property Modifications

`ARCHIVER` has been removed from the possible allowed values for the `site.disabled.tasks` and was replaced by the new
properties.

<h2>Bug</h2>
<ul>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-724'>EC-724</a>] - DB sync  fails to start when activeMQ is down</li>
<li>[<a href='https://jira.fgh.org.mz/browse/EC-727'>EC-727</a>] - Fix full indexer for older OpenMRS versions</li>
</ul>
    
    

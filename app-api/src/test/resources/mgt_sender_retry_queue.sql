INSERT INTO sender_event (id, table_name, primary_key_id, operation, snapshot, event_date, date_created)
VALUES  (11, 'person', '1', 'c', 0, null, '2020-06-27 00:00:00.001'),
        (12, 'person', '1', 'u', 0, null, '2020-06-27 00:00:00.002'),
        (13, 'person', '1', 'u', 0, null, '2020-06-27 00:00:00.003'),
        (14, 'orders', '1', 'c', 0, null, '2020-06-27 00:00:00.000');


INSERT INTO sender_retry_queue (id, event_id, attempt_count, exception_type, message, date_created)
VALUES  (1, 11, 1, 'org.openmrs.eip.component.exception.EIPException', 'Testing', '2020-06-27 00:00:00.001'),
        (2, 12, 1, 'org.openmrs.eip.component.exception.EIPException', 'Testing', '2020-06-27 00:00:00.002'),
        (3, 13, 1, 'org.apache.activemq.artemis.api.core.ActiveMQNativeIOError', 'Testing', '2020-06-27 00:00:00.003'),
        (4, 14, 1, 'org.apache.activemq.artemis.api.core.ActiveMQNativeIOError', null, '2020-06-27 00:00:00.000');

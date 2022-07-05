INSERT INTO sender_retry_queue (id, table_name, primary_key_id, operation, attempt_count, exception_type, message, snapshot, date_created)
VALUES  (1, 'person', '1', 'c', 1, 'java.lang.Exception', 'Testing', 0, '2020-06-27 00:00:00'),
        (2, 'person', '1', 'u', 1, 'java.lang.Exception', 'Testing', 0, '2020-06-27 00:00:00'),
        (3, 'patient', '1', 'u', 1, 'java.lang.Exception', 'Testing', 0, '2020-06-27 00:00:00'),
        (4, 'orders', '1', 'c', 1, 'java.lang.Exception', null, 0, '2020-06-27 00:00:00');

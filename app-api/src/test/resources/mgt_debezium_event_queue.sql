INSERT INTO sender_event (id, table_name, primary_key_id, operation, snapshot, event_date, date_created)
VALUES (2, 'visit', '1', 'c', 0, '2020-06-28 00:00:00.001', '2020-06-28 00:00:00.001'),
       (1, 'encounter', '1', 'c', 0, '2020-06-28 00:00:00.001', '2020-06-28 00:00:00.001'),
       (3, 'patient', '101', 'u', 0, '2020-06-28 00:00:00.000', '2020-06-28 00:00:00.000');

INSERT INTO debezium_event_queue (id, event_id, date_created)
VALUES (2, 2, '2020-06-28 00:00:00.001'),
       (1, 1, '2020-06-28 00:00:00.001'),
       (3, 3, '2020-06-28 00:00:00.000');

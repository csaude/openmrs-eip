INSERT INTO sender_event (id, table_name, identifier, operation, snapshot, request_uuid, event_date, date_created)
VALUES (2, 'person', 'abfd940e-32dc-491f-8038-a8f3afe3e35b', 'c', 0, null, '2020-05-28 00:00:00.005', '2023-01-09 00:13:00.000'),
       (1, 'person', 'abfd940e-32dc-491f-8038-a8f3afe3e35b', 'u', 0, null, '2020-05-28 00:00:00.006', '2023-01-09 00:13:00.000'),
       (3, 'person', 'abfd940e-32dc-491f-8038-a8f3afe3e35b', 'u', 0, null, '2020-05-28 00:00:00.007', '2023-01-09 00:13:00.000'),
       (4, 'person', 'abfd940e-32dc-491f-8038-a8f3afe3e35b', 'u', 0, null, '2020-05-28 00:00:00.008', '2023-01-09 00:13:00.000');


INSERT INTO sender_sync_message (id, event_id, message_uuid, sync_data, status, date_sent, date_created)
VALUES (2, 2, '26beb8bd-287c-47f2-9786-a7b98c933c04', '{}', 'NEW', null, '2020-06-28 00:00:00.001'),
       (1, 1, '16beb8bd-287c-47f2-9786-a7b98c933c04', '{}', 'NEW', null, '2020-06-28 00:00:00.001'),
       (3, 3, '36beb8bd-287c-47f2-9786-a7b98c933c04', '{}', 'NEW', null, '2020-06-28 00:00:00.000'),
       (4, 4, '46beb8bd-287c-47f2-9786-a7b98c933c04', '{}', 'SENT', '2020-06-28 00:00:00.004', '2020-06-28 00:00:00.000');

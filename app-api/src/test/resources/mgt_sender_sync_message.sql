INSERT INTO jms_broker 
(id, identifier, name, host, port, username, password, disabled, date_created)
VALUES (1, 'default', 'default', 'localhost', '1234', 'user', 'pass123', false, NOW());

INSERT INTO sender_sync_message 
(id, table_name, identifier, operation, snapshot, message_uuid, request_uuid, status, jms_broker_id, date_created)
VALUES (2, 'person', 'abfd940e-32dc-491f-8038-a8f3afe3e35b', 'c', 0, '26beb8bd-287c-47f2-9786-a7b98c933c04', null, 'NEW', 1, '2020-06-28 00:00:00.001'),
       (1, 'person', 'abfd940e-32dc-491f-8038-a8f3afe3e35b', 'u', 0, '16beb8bd-287c-47f2-9786-a7b98c933c04', null, 'NEW', 1, '2020-06-28 00:00:00.001'),
       (3, 'person', 'abfd940e-32dc-491f-8038-a8f3afe3e35b', 'u', 0, '36beb8bd-287c-47f2-9786-a7b98c933c04', null, 'NEW', 1, '2020-06-28 00:00:00.000'),
       (4, 'person', 'abfd940e-32dc-491f-8038-a8f3afe3e35b', 'u', 0, '46beb8bd-287c-47f2-9786-a7b98c933c04', null, 'SENT', 1, '2020-06-28 00:00:00.000');

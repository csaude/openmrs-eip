INSERT INTO receiver_retry_queue (id, model_class_name, identifier, entity_payload, is_snapshot, site_id, attempt_count, exception_type, message, date_sent_by_sender, message_uuid, date_received, date_created)
VALUES  (1, 'org.openmrs.eip.component.model.PersonModel', 'uuid-1', '{}', 0, 1, 1, 'java.lang.Exception', 'Testing', '2022-06-16 00:00:00', '1dfd940e-32dc-491f-8038-a8f3afe3e36e', '2022-06-16 00:01:00', '2022-06-16 00:01:01'),
        (2, 'org.openmrs.eip.component.model.PersonModel', 'uuid-1', '{}', 0, 1, 1, 'java.lang.Exception', 'Testing', '2022-06-16 00:00:00', '2dfd940e-32dc-491f-8038-a8f3afe3e36e', '2022-06-16 00:01:00', '2022-06-16 00:01:02'),
        (3, 'org.openmrs.eip.component.model.PatientModel', 'uuid-1', '{}', 0, 1, 1, 'java.lang.Exception', 'Testing', '2022-06-16 00:00:00', '3dfd940e-32dc-491f-8038-a8f3afe3e36e', '2022-06-16 00:01:00', '2022-06-16 00:01:03'),
        (4, 'org.openmrs.eip.component.model.PersonModel', 'uuid-2', '{}', 0, 1, 1, 'java.lang.Exception', '', '2022-06-16 00:00:00', '4dfd940e-32dc-491f-8038-a8f3afe3e36e', '2022-06-16 00:01:00', '2022-06-16 00:01:00'),
        (5, 'org.openmrs.eip.component.model.OrderModel', 'uuid-1', '{}', 0, 1, 1, 'java.lang.Exception', null, '2022-06-16 00:00:00', '5dfd940e-32dc-491f-8038-a8f3afe3e36e', '2022-06-16 00:01:00', '2022-06-16 00:01:05');

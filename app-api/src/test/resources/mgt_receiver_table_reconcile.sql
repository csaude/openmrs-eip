INSERT INTO receiver_table_reconcile (id, site_reconcile_id, table_name, row_count, remote_start_date, processed_count, last_batch_received, completed, date_created)
VALUES (1, 1, 'person', 10, '2024-02-07 00:04:00', 10, 0, 1, '2024-02-07 00:00:00'),
       (2, 1, 'visit', 10, '2024-02-07 00:04:00', 10, 0, 1, '2024-02-07 00:00:00'),
       (3, 1, 'encounter', 10, '2024-02-07 00:04:00', 10, 0, 1, '2024-02-07 00:00:00'),
       (4, 2, 'person', 50, '2024-02-07 00:02:00', 0, 0, 0, '2024-02-07 00:00:00'),
       (5, 3, 'person', 70, '2024-02-07 00:04:00', 10, 0, 0, '2024-02-07 00:00:00'),
       (6, 4, 'obs', 10, '2024-02-07 00:04:00', 10, 0, 1, '2024-02-07 00:00:00'),
       (7, 5, 'person', 10, '2024-02-07 00:04:00', 10, 0, 1, '2024-02-07 00:00:00'),
       (8, 5, 'VISIT', 10, '2024-02-07 00:04:00', 10, 0, 1, '2024-02-07 00:00:00'),
       (9, 5, 'encounter', 10, '2024-02-07 00:04:00', 9, 0, 0, '2024-02-07 00:00:00');

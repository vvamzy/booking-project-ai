INSERT INTO room (id, name, capacity, location, status) VALUES
(1, 'Executive Boardroom', 20, 'Floor 10 West Wing', 'AVAILABLE'),
(2, 'Innovation Lab', 15, 'Floor 8 North Wing', 'AVAILABLE'),
(3, 'Collaboration Space', 12, 'Floor 6 East Wing', 'AVAILABLE'),
(4, 'Focus Room Alpha', 4, 'Floor 4 South Wing', 'AVAILABLE'),
(5, 'Conference Room Beta', 30, 'Floor 12 Central', 'AVAILABLE'),
(6, 'Brainstorm Pod', 6, 'Floor 7 Creative Zone', 'AVAILABLE'),
(7, 'Digital Studio', 10, 'Floor 9 Tech Hub', 'AVAILABLE'),
(8, 'Training Room', 25, 'Floor 3 Learning Center', 'AVAILABLE');

INSERT INTO equipment (id, name, type, status) VALUES
(1, 'Interactive Whiteboard', 'DISPLAY', 'AVAILABLE'),
(2, '4K Video Conferencing', 'VIDEO', 'AVAILABLE'),
(3, 'Surround Sound System', 'AUDIO', 'AVAILABLE'),
(4, 'Wireless Presenter', 'CONTROL', 'AVAILABLE'),
(5, '360° Conference Camera', 'VIDEO', 'AVAILABLE'),
(6, 'Digital Drawing Tablet', 'INPUT', 'AVAILABLE'),
(7, 'Smart Display', 'DISPLAY', 'AVAILABLE'),
(8, 'Wireless Microphone Set', 'AUDIO', 'AVAILABLE'),
(9, 'Document Camera', 'VIDEO', 'AVAILABLE'),
(10, 'Mobile Collaboration Cart', 'FURNITURE', 'AVAILABLE');

INSERT INTO room_equipment (room_id, equipment_id) VALUES
-- Executive Boardroom
(1, 1), -- Interactive Whiteboard
(1, 2), -- 4K Video Conferencing
(1, 3), -- Surround Sound
(1, 4), -- Wireless Presenter

-- Innovation Lab
(2, 1), -- Interactive Whiteboard
(2, 6), -- Digital Drawing Tablet
(2, 7), -- Smart Display
(2, 8), -- Wireless Microphone Set

-- Collaboration Space
(3, 5), -- 360° Conference Camera
(3, 7), -- Smart Display
(3, 8), -- Wireless Microphone Set
(3, 10), -- Mobile Collaboration Cart

-- Focus Room Alpha
(4, 7), -- Smart Display
(4, 4), -- Wireless Presenter

-- Conference Room Beta
(5, 1), -- Interactive Whiteboard
(5, 2), -- 4K Video Conferencing
(5, 3), -- Surround Sound
(5, 4), -- Wireless Presenter
(5, 8), -- Wireless Microphone Set

-- Brainstorm Pod
(6, 1), -- Interactive Whiteboard
(6, 6), -- Digital Drawing Tablet
(6, 10), -- Mobile Collaboration Cart

-- Digital Studio
(7, 2), -- 4K Video Conferencing
(7, 5), -- 360° Conference Camera
(7, 6), -- Digital Drawing Tablet
(7, 9), -- Document Camera

-- Training Room
(8, 1), -- Interactive Whiteboard
(8, 2), -- 4K Video Conferencing
(8, 3), -- Surround Sound
(8, 4), -- Wireless Presenter
(8, 9); -- Document Camera
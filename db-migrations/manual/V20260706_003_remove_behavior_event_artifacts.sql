-- Remove retired behavior-event artifacts that belonged to the removed AI advice feature.
-- Keep this as an additive migration so historical migration files remain immutable.

DROP TABLE IF EXISTS behavior_event;

-- V010：开放域级共享番茄钟。新增字段均可空，普通域和现有队列不受影响。
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY, applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER //
DROP PROCEDURE IF EXISTS sz_v010_columns//
CREATE PROCEDURE sz_v010_columns()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sz_zone' AND COLUMN_NAME='pomodoro_preset'
  ) THEN
    ALTER TABLE sz_zone ADD COLUMN pomodoro_preset VARCHAR(16) DEFAULT NULL AFTER state_version;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sz_zone' AND COLUMN_NAME='pomodoro_started_at'
  ) THEN
    ALTER TABLE sz_zone ADD COLUMN pomodoro_started_at DATETIME(6) DEFAULT NULL AFTER pomodoro_preset;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sz_zone' AND COLUMN_NAME='pomodoro_period_index'
  ) THEN
    ALTER TABLE sz_zone ADD COLUMN pomodoro_period_index INT DEFAULT NULL AFTER pomodoro_started_at;
  END IF;
END//
DELIMITER ;

CALL sz_v010_columns();

-- 历史时段配置继续可用；无法识别具体预设时标记为 LEGACY，并沿用域创建时间。
UPDATE sz_zone z
SET z.pomodoro_preset = COALESCE(z.pomodoro_preset, 'LEGACY'),
    z.pomodoro_started_at = COALESCE(z.pomodoro_started_at, z.created_at),
    z.pomodoro_period_index = COALESCE(z.pomodoro_period_index, 0)
WHERE EXISTS (SELECT 1 FROM sz_zone_period p WHERE p.zone_id = z.id);

INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('V010',NOW(6));
DROP PROCEDURE sz_v010_columns;

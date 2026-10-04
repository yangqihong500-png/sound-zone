-- V011：新增可空的公开头像地址；公开用户 ID 继续复用既有唯一 name 字段，不改写历史用户。
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY, applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER //
DROP PROCEDURE IF EXISTS sz_v011_column//
CREATE PROCEDURE sz_v011_column()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sz_user' AND COLUMN_NAME='avatar_url'
  ) THEN
    ALTER TABLE sz_user ADD COLUMN avatar_url VARCHAR(512) DEFAULT NULL AFTER avatar_color;
  END IF;
END//
DELIMITER ;

CALL sz_v011_column();
INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('V011',NOW(6));
DROP PROCEDURE sz_v011_column;

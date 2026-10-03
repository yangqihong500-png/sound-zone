-- V009：新增可空的用户主页背景地址；现有头像、资料和图片保持原样。
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY, applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER //
DROP PROCEDURE IF EXISTS sz_v009_column//
CREATE PROCEDURE sz_v009_column()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='sz_user' AND COLUMN_NAME='cover_url'
  ) THEN
    ALTER TABLE sz_user ADD COLUMN cover_url VARCHAR(512) DEFAULT NULL AFTER avatar_color;
  END IF;
END//
DELIMITER ;

CALL sz_v009_column();
INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('V009',NOW(6));
DROP PROCEDURE sz_v009_column;

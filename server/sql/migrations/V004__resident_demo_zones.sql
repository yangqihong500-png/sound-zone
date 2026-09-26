-- V004：为演示环境增加常驻域标记。历史域默认 false，业务数据不改写。
-- 脚本可重复执行；演示数据由应用按稳定标识幂等写入。
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY, applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DELIMITER //
DROP PROCEDURE IF EXISTS sz_v004_column//
CREATE PROCEDURE sz_v004_column(IN table_name_arg VARCHAR(64), IN column_name_arg VARCHAR(64), IN definition_arg TEXT)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_arg AND COLUMN_NAME=column_name_arg
  ) THEN
    SET @sz_ddl=CONCAT('ALTER TABLE `',table_name_arg,'` ADD COLUMN `',column_name_arg,'` ',definition_arg);
    PREPARE sz_stmt FROM @sz_ddl; EXECUTE sz_stmt; DEALLOCATE PREPARE sz_stmt;
  END IF;
END//
DELIMITER ;
CALL sz_v004_column('sz_zone','demo_resident','BIT(1) NOT NULL DEFAULT b''0''');
-- 旧版可能仍是缺少 STOPPED 的 ENUM；转换为当前 schema 的 VARCHAR，保留所有现有值。
ALTER TABLE sz_queue_item MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'QUEUED';
INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('V004',NOW(6));
DROP PROCEDURE sz_v004_column;

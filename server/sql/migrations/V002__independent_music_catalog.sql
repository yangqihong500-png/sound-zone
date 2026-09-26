-- V002：独立 App 曲库来源与授权记录。只新增可空列和查询索引，不改写历史曲目。
-- 执行时显式选择数据库；MySQL DDL 会隐式提交，脚本可重复执行。
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY, applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DELIMITER //
DROP PROCEDURE IF EXISTS sz_v002_column//
CREATE PROCEDURE sz_v002_column(IN table_name_arg VARCHAR(64),IN column_name_arg VARCHAR(64),IN definition_arg TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_arg AND COLUMN_NAME=column_name_arg) THEN
    SET @sz_ddl=CONCAT('ALTER TABLE `',table_name_arg,'` ADD COLUMN `',column_name_arg,'` ',definition_arg);
    PREPARE sz_stmt FROM @sz_ddl; EXECUTE sz_stmt; DEALLOCATE PREPARE sz_stmt;
  END IF;
END//
DROP PROCEDURE IF EXISTS sz_v002_index//
CREATE PROCEDURE sz_v002_index(IN table_name_arg VARCHAR(64),IN index_name_arg VARCHAR(64),IN definition_arg TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_arg AND INDEX_NAME=index_name_arg) THEN
    SET @sz_ddl=CONCAT('ALTER TABLE `',table_name_arg,'` ADD ',definition_arg);
    PREPARE sz_stmt FROM @sz_ddl; EXECUTE sz_stmt; DEALLOCATE PREPARE sz_stmt;
  END IF;
END//
DELIMITER ;
CALL sz_v002_column('sz_track','attribution','VARCHAR(256) DEFAULT NULL');
CALL sz_v002_column('sz_track','license_reference','VARCHAR(512) DEFAULT NULL');
CALL sz_v002_index('sz_track','idx_track_source_external','KEY idx_track_source_external(source,external_id)');
INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('V002',NOW(6));
DROP PROCEDURE sz_v002_column;
DROP PROCEDURE sz_v002_index;

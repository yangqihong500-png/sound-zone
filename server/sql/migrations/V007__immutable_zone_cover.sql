-- V007：域封面在创建时确定并锁定；历史域用最早入队歌曲的封面平滑补齐。
-- 仅新增可空列并回填可确定的数据，不覆盖已有域封面。
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY, applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DELIMITER //
DROP PROCEDURE IF EXISTS sz_v007_column//
CREATE PROCEDURE sz_v007_column(IN table_name_arg VARCHAR(64), IN column_name_arg VARCHAR(64), IN definition_arg TEXT)
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

CALL sz_v007_column('sz_zone','cover_url','VARCHAR(512) DEFAULT NULL AFTER cover_color');

UPDATE sz_zone z
SET z.cover_url = (
  SELECT t.cover_url
  FROM sz_queue_item q
  JOIN sz_track t ON t.id=q.track_id
  WHERE q.zone_id=z.id AND t.cover_url IS NOT NULL AND t.cover_url<>''
  ORDER BY q.created_at ASC,q.id ASC
  LIMIT 1
)
WHERE z.cover_url IS NULL;

INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('V007',NOW(6));
DROP PROCEDURE sz_v007_column;

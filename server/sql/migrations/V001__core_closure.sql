-- V001：已有 MySQL 8 业务库增量迁移；先备份，停旧写流量，再执行。
-- 执行时显式选择数据库。本脚本不包含 USE，不删除业务字段或历史记录。
-- DDL 会隐式提交；逐项幂等处理，部分失败修复后可重跑。
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY, applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
DELIMITER //
DROP PROCEDURE IF EXISTS sz_v001_column//
CREATE PROCEDURE sz_v001_column(IN table_name_arg VARCHAR(64),IN column_name_arg VARCHAR(64),IN definition_arg TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_arg AND COLUMN_NAME=column_name_arg) THEN
    SET @sz_ddl=CONCAT('ALTER TABLE `',table_name_arg,'` ADD COLUMN `',column_name_arg,'` ',definition_arg);
    PREPARE sz_stmt FROM @sz_ddl; EXECUTE sz_stmt; DEALLOCATE PREPARE sz_stmt;
  END IF;
END//
DROP PROCEDURE IF EXISTS sz_v001_index//
CREATE PROCEDURE sz_v001_index(IN table_name_arg VARCHAR(64),IN index_name_arg VARCHAR(64),IN definition_arg TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME=table_name_arg AND INDEX_NAME=index_name_arg) THEN
    SET @sz_ddl=CONCAT('ALTER TABLE `',table_name_arg,'` ADD ',definition_arg);
    PREPARE sz_stmt FROM @sz_ddl; EXECUTE sz_stmt; DEALLOCATE PREPARE sz_stmt;
  END IF;
END//
DROP PROCEDURE IF EXISTS sz_v001_fk//
CREATE PROCEDURE sz_v001_fk(IN table_name_arg VARCHAR(64),IN fk_name_arg VARCHAR(64),IN definition_arg TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.TABLE_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND TABLE_NAME=table_name_arg AND CONSTRAINT_NAME=fk_name_arg) THEN
    SET @sz_ddl=CONCAT('ALTER TABLE `',table_name_arg,'` ADD CONSTRAINT `',fk_name_arg,'` ',definition_arg);
    PREPARE sz_stmt FROM @sz_ddl; EXECUTE sz_stmt; DEALLOCATE PREPARE sz_stmt;
  END IF;
END//
DELIMITER ;
CALL sz_v001_column('sz_user','host_subject','VARCHAR(128) DEFAULT NULL');
CALL sz_v001_column('sz_track','cover_url','VARCHAR(512) DEFAULT NULL');
CALL sz_v001_column('sz_zone','password_hash','VARCHAR(256) DEFAULT NULL');
CALL sz_v001_column('sz_zone','state_version','BIGINT NOT NULL DEFAULT 0');
CALL sz_v001_column('sz_zone','last_activity_at','DATETIME(6) DEFAULT NULL');
CALL sz_v001_column('sz_zone_member','last_seen_at','DATETIME(6) DEFAULT NULL');
CALL sz_v001_column('sz_moment','queue_item_id','BIGINT DEFAULT NULL');
CALL sz_v001_column('sz_moment','moderation_status','VARCHAR(16) NOT NULL DEFAULT ''PENDING''');
CALL sz_v001_column('sz_moment','training_consent','BIT(1) NOT NULL DEFAULT b''0''');
CALL sz_v001_column('sz_moment','consented_at','DATETIME(6) DEFAULT NULL');
CALL sz_v001_column('sz_moment','withdrawn_at','DATETIME(6) DEFAULT NULL');
CALL sz_v001_column('sz_feedback_event','queue_item_id','BIGINT DEFAULT NULL');
CALL sz_v001_index('sz_user','uk_user_host_subject','UNIQUE KEY uk_user_host_subject(host_subject)');
CALL sz_v001_index('sz_moment','uk_moment_queue_item','UNIQUE KEY uk_moment_queue_item(queue_item_id)');
CALL sz_v001_index('sz_queue_item','idx_queue_fifo','KEY idx_queue_fifo(zone_id,status,created_at,id)');
CALL sz_v001_index('sz_queue_item','idx_queue_cooldown','KEY idx_queue_cooldown(zone_id,user_id,created_at,id)');
CALL sz_v001_index('sz_moment','idx_moment_feed','KEY idx_moment_feed(zone_id,status,moderation_status,created_at,id)');

CREATE TABLE IF NOT EXISTS sz_auth_session (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL, token_hash VARCHAR(64) NOT NULL,
  expires_at DATETIME(6) NOT NULL, created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_session_token (token_hash),
  CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES sz_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS sz_track_collection (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL, track_id BIGINT NOT NULL, created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_collection_user_track (user_id,track_id),
  CONSTRAINT fk_collection_user FOREIGN KEY(user_id) REFERENCES sz_user(id),
  CONSTRAINT fk_collection_track FOREIGN KEY(track_id) REFERENCES sz_track(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS sz_queue_like (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL, queue_item_id BIGINT NOT NULL, created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_like_user_item (user_id,queue_item_id),
  CONSTRAINT fk_like_user FOREIGN KEY(user_id) REFERENCES sz_user(id),
  CONSTRAINT fk_like_item FOREIGN KEY(queue_item_id) REFERENCES sz_queue_item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS sz_moment_reaction (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL, moment_id BIGINT NOT NULL, type VARCHAR(16) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_reaction_user_moment (user_id,moment_id),
  CONSTRAINT fk_reaction_user FOREIGN KEY(user_id) REFERENCES sz_user(id),
  CONSTRAINT fk_reaction_moment FOREIGN KEY(moment_id) REFERENCES sz_moment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS sz_report (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL, zone_id BIGINT NOT NULL, reason VARCHAR(500) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'OPEN', created_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_report_user FOREIGN KEY(user_id) REFERENCES sz_user(id),
  CONSTRAINT fk_report_zone FOREIGN KEY(zone_id) REFERENCES sz_zone(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS sz_activity_event (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL, zone_id BIGINT DEFAULT NULL, item_id BIGINT DEFAULT NULL,
  type VARCHAR(32) NOT NULL, duration_seconds BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  KEY idx_activity_day (created_at,type), KEY idx_activity_user (user_id,type,created_at),
  CONSTRAINT fk_activity_user FOREIGN KEY(user_id) REFERENCES sz_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS sz_training_export_item (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  batch_id VARCHAR(36) NOT NULL, moment_id BIGINT NOT NULL, created_at DATETIME(6) NOT NULL,
  UNIQUE KEY uk_export_batch_moment(batch_id,moment_id),
  CONSTRAINT fk_export_moment FOREIGN KEY(moment_id) REFERENCES sz_moment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CALL sz_v001_fk('sz_moment','fk_moment_queue','FOREIGN KEY(queue_item_id) REFERENCES sz_queue_item(id)');
CALL sz_v001_fk('sz_feedback_event','fk_feedback_queue','FOREIGN KEY(queue_item_id) REFERENCES sz_queue_item(id)');
-- 不猜测旧图片属于哪次上传，不把旧图片自动认定为已授权训练或已审核。
-- 窗口只由已有行为推导；旧成员以首次新服务心跳清理时刻开始宽限。
UPDATE sz_zone z SET last_activity_at=GREATEST(z.created_at,
  COALESCE((SELECT MAX(q.created_at) FROM sz_queue_item q WHERE q.zone_id=z.id),z.created_at),
  COALESCE((SELECT MAX(m.created_at) FROM sz_moment m WHERE m.zone_id=z.id),z.created_at))
WHERE last_activity_at IS NULL;
-- 原事件只提供收藏新增信号；旧前端取消未落库，历史意图无法可靠还原，不自动补收藏状态。
-- 旧队列 likes 无点赞用户，不捏造关系；保留历史基数，新点赞在此基础上增减。
INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('V001',NOW(6));
DROP PROCEDURE sz_v001_column;
DROP PROCEDURE sz_v001_index;
DROP PROCEDURE sz_v001_fk;

-- V005：新增关注关系内的一对一文字私信。仅新增表，不改写现有用户和关注数据。
-- 可重复执行；回滚应用版本时保留该表，暂停私信入口即可。
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY, applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sz_direct_message (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  sender_id    BIGINT       NOT NULL,
  recipient_id BIGINT       NOT NULL,
  body         VARCHAR(500) NOT NULL,
  created_at   DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_message_sender_recipient (sender_id,recipient_id,created_at,id),
  KEY idx_message_recipient_sender (recipient_id,sender_id,created_at,id),
  CONSTRAINT fk_message_sender FOREIGN KEY(sender_id) REFERENCES sz_user(id),
  CONSTRAINT fk_message_recipient FOREIGN KEY(recipient_id) REFERENCES sz_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('V005',NOW(6));

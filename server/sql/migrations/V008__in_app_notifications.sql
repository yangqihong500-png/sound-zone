-- V008：站内通知中心。仅新增表，不改写既有业务数据。
-- 未读私信、域邀请、歌曲开播与歌曲点赞可在应用内统一查看。
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY, applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sz_notification (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  recipient_id  BIGINT       NOT NULL,
  actor_id      BIGINT       DEFAULT NULL,
  type          VARCHAR(32)  NOT NULL,
  title         VARCHAR(160) NOT NULL,
  body          VARCHAR(300) NOT NULL,
  zone_id       BIGINT       DEFAULT NULL,
  queue_item_id BIGINT       DEFAULT NULL,
  message_id    BIGINT       DEFAULT NULL,
  event_count   INT          NOT NULL DEFAULT 1,
  read_at       DATETIME(6)  DEFAULT NULL,
  created_at    DATETIME(6)  NOT NULL,
  updated_at    DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_notification_recipient_unread (recipient_id,read_at,updated_at,id),
  KEY idx_notification_aggregate (recipient_id,type,queue_item_id,actor_id,read_at),
  CONSTRAINT fk_notification_recipient FOREIGN KEY(recipient_id) REFERENCES sz_user(id),
  CONSTRAINT fk_notification_actor FOREIGN KEY(actor_id) REFERENCES sz_user(id),
  CONSTRAINT fk_notification_zone FOREIGN KEY(zone_id) REFERENCES sz_zone(id),
  CONSTRAINT fk_notification_queue FOREIGN KEY(queue_item_id) REFERENCES sz_queue_item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('V008',NOW(6));

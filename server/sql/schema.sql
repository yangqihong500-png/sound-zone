-- ============================================================================
-- 同频 SoundZone · MySQL 8 表结构
-- 说明：本脚本由 JPA 实体（server/src/main/java/com/soundzone/**/entity/*.java）
--       1:1 推导生成，与 Hibernate 6 的命名策略（camelCase→snake_case）一致。
--
-- 使用方式（二选一）：
--   A) 让后端 JPA 自动建表（推荐）：application.yml 中 ddl-auto=update，
--      后端启动即自动创建全部表，无需手动执行本脚本。
--   B) 手动建表：在 Sequel Ace 中执行本脚本后，再将 ddl-auto 改为 validate。
--
-- 字段命名对照：avatarColor→avatar_color, listenerCount→listener_count,
--              createdAt→created_at, imageUrl→image_url 等。
-- ============================================================================

-- 业务库由 docker-compose 的 MYSQL_DATABASE 自动创建；此处兜底
CREATE DATABASE IF NOT EXISTS soundzone
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE soundzone;

-- ----------------------------------------------------------------------------
-- 1. 用户表（User）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sz_user (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  name         VARCHAR(32)  NOT NULL,
  avatar_color VARCHAR(16)  NOT NULL DEFAULT '#8C9BAB',
  created_at   DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 2. 曲目表（Track）—— 音源抽象层的本地映射
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sz_track (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  title        VARCHAR(128) NOT NULL,
  artist       VARCHAR(64)  NOT NULL,
  cover_color  VARCHAR(16)  DEFAULT NULL,
  duration_sec INT          NOT NULL DEFAULT 240,
  source       VARCHAR(16)  NOT NULL DEFAULT 'MOCK',
  external_id  VARCHAR(64)  DEFAULT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 曲目标签（Track.tags @ElementCollection，五类标签：语言/年代/风格/场景/情绪）
CREATE TABLE IF NOT EXISTS sz_track_tags (
  track_id BIGINT      NOT NULL,
  tag      VARCHAR(32) NOT NULL,
  PRIMARY KEY (track_id, tag),
  CONSTRAINT fk_track_tags_track FOREIGN KEY (track_id) REFERENCES sz_track (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 3. 域表（Zone）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sz_zone (
  id             BIGINT      NOT NULL AUTO_INCREMENT,
  name           VARCHAR(64) NOT NULL,
  scene          VARCHAR(32) NOT NULL,
  host_id        BIGINT      NOT NULL,
  cover_color    VARCHAR(16) DEFAULT '#A8B8C8',
  status         VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE / ENDED
  visibility     VARCHAR(16) NOT NULL DEFAULT 'PUBLIC',   -- PUBLIC / PRIVATE
  password       VARCHAR(32) DEFAULT NULL,
  invite_code    VARCHAR(32) DEFAULT NULL,
  filter_mode    VARCHAR(16) NOT NULL DEFAULT 'BAN',      -- BAN / ALLOW
  listener_count INT         NOT NULL DEFAULT 0,
  created_at     DATETIME(6) NOT NULL,
  ended_at       DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_zone_host FOREIGN KEY (host_id) REFERENCES sz_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 域风格标签（Zone.tags，展示用）
CREATE TABLE IF NOT EXISTS sz_zone_tags (
  zone_id BIGINT      NOT NULL,
  tag     VARCHAR(32) NOT NULL,
  PRIMARY KEY (zone_id, tag),
  CONSTRAINT fk_zone_tags_zone FOREIGN KEY (zone_id) REFERENCES sz_zone (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 域过滤标签（Zone.filterTags：BAN 黑名单 / ALLOW 白名单）
CREATE TABLE IF NOT EXISTS sz_zone_filter_tags (
  zone_id BIGINT      NOT NULL,
  tag     VARCHAR(32) NOT NULL,
  PRIMARY KEY (zone_id, tag),
  CONSTRAINT fk_zone_filter_zone FOREIGN KEY (zone_id) REFERENCES sz_zone (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 4. 番茄钟时段（ZonePeriod）—— 决议 D3
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sz_zone_period (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  zone_id      BIGINT      NOT NULL,
  order_index  INT         NOT NULL,
  duration_min INT         NOT NULL,
  type         VARCHAR(16) NOT NULL,          -- FOCUS / BREAK
  PRIMARY KEY (id),
  CONSTRAINT fk_zone_period_zone FOREIGN KEY (zone_id) REFERENCES sz_zone (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 时段白名单标签（ZonePeriod.allowedTags）
CREATE TABLE IF NOT EXISTS sz_zone_period_tags (
  period_id BIGINT      NOT NULL,
  tag       VARCHAR(32) NOT NULL,
  PRIMARY KEY (period_id, tag),
  CONSTRAINT fk_period_tags_period FOREIGN KEY (period_id) REFERENCES sz_zone_period (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 5. 域成员（ZoneMember）—— 同频人数 + 全员退出自动消失
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sz_zone_member (
  id        BIGINT      NOT NULL AUTO_INCREMENT,
  zone_id   BIGINT      NOT NULL,
  user_id   BIGINT      NOT NULL,
  joined_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_zone_member (zone_id, user_id),
  CONSTRAINT fk_member_zone FOREIGN KEY (zone_id) REFERENCES sz_zone (id),
  CONSTRAINT fk_member_user FOREIGN KEY (user_id) REFERENCES sz_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 6. 上传队列条目（QueueItem）—— FIFO 播放顺序（决议 D3）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sz_queue_item (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  zone_id    BIGINT      NOT NULL,
  track_id   BIGINT      NOT NULL,
  user_id    BIGINT      NOT NULL,             -- 上传者（requester）
  likes      INT         NOT NULL DEFAULT 0,   -- 点赞数（仅互动信号）
  status     VARCHAR(16) NOT NULL DEFAULT 'QUEUED', -- PLAYING/QUEUED/PRESET/PLAYED/REMOVED
  created_at DATETIME(6) NOT NULL,             -- 上传时间 = FIFO 排序依据
  started_at DATETIME(6) DEFAULT NULL,
  played_at  DATETIME(6) DEFAULT NULL,
  PRIMARY KEY (id),
  KEY idx_queue_zone_status (zone_id, status),
  CONSTRAINT fk_queue_zone  FOREIGN KEY (zone_id)  REFERENCES sz_zone (id),
  CONSTRAINT fk_queue_track FOREIGN KEY (track_id) REFERENCES sz_track (id),
  CONSTRAINT fk_queue_user  FOREIGN KEY (user_id)  REFERENCES sz_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 7. 图片分享（Moment）—— (场景, 图, 关联歌曲) 三元组（决议 D6）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sz_moment (
  id         BIGINT       NOT NULL AUTO_INCREMENT,
  zone_id    BIGINT       NOT NULL,
  user_id    BIGINT       NOT NULL,             -- 上传者
  text       VARCHAR(500) DEFAULT NULL,
  image_url  VARCHAR(512) DEFAULT NULL,
  color      VARCHAR(16)  DEFAULT NULL,
  track_id   BIGINT       DEFAULT NULL,         -- 关联歌曲
  status     VARCHAR(16)  NOT NULL DEFAULT 'NORMAL', -- NORMAL / WITHDRAWN
  created_at DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_moment_zone (zone_id),
  CONSTRAINT fk_moment_zone  FOREIGN KEY (zone_id)  REFERENCES sz_zone (id),
  CONSTRAINT fk_moment_user  FOREIGN KEY (user_id)  REFERENCES sz_user (id),
  CONSTRAINT fk_moment_track FOREIGN KEY (track_id) REFERENCES sz_track (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 8. 反馈事件（FeedbackEvent）—— 收藏/点赞/emoji，归属上传者（决议 D7）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sz_feedback_event (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  zone_id      BIGINT      NOT NULL,
  track_id     BIGINT      NOT NULL,
  from_user_id BIGINT      NOT NULL,           -- 反馈发起者（听众）
  to_user_id   BIGINT      NOT NULL,           -- 反馈归属者（该曲上传者）
  type         VARCHAR(16) NOT NULL,           -- COLLECT/LIKE/EMOJI_HEART/EMOJI_LAUGH
  created_at   DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_feedback_zone (zone_id),
  KEY idx_feedback_to_user (to_user_id),
  CONSTRAINT fk_fb_zone  FOREIGN KEY (zone_id)      REFERENCES sz_zone (id),
  CONSTRAINT fk_fb_track FOREIGN KEY (track_id)     REFERENCES sz_track (id),
  CONSTRAINT fk_fb_from  FOREIGN KEY (from_user_id) REFERENCES sz_user (id),
  CONSTRAINT fk_fb_to    FOREIGN KEY (to_user_id)   REFERENCES sz_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ----------------------------------------------------------------------------
-- 9. 关注关系（Follow）—— 轻入口关注上传者（决议 D7）
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sz_follow (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  follower_id BIGINT      NOT NULL,            -- 关注者
  followee_id BIGINT      NOT NULL,            -- 被关注者
  created_at  DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_follow (follower_id, followee_id),
  CONSTRAINT fk_follow_follower FOREIGN KEY (follower_id) REFERENCES sz_user (id),
  CONSTRAINT fk_follow_followee FOREIGN KEY (followee_id) REFERENCES sz_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- V003：图片即时展示。仅修改新记录的数据库默认值，历史状态及图片记录原样保留。
-- 应用服务本身也会显式写入 APPROVED；脚本可重复执行。
ALTER TABLE sz_moment ALTER COLUMN moderation_status SET DEFAULT 'APPROVED';
INSERT IGNORE INTO sz_schema_migration(version, applied_at) VALUES ('V003', NOW(6));

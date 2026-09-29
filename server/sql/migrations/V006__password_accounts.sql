-- V006：为独立 App 增加用户名/密码账号；游客和宿主用户数据保持不变，可在注册时平滑绑定。
CREATE TABLE IF NOT EXISTS sz_schema_migration (
  version VARCHAR(32) PRIMARY KEY, applied_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sz_user_credential (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  user_id       BIGINT       NOT NULL,
  login_name    VARCHAR(32)  NOT NULL,
  password_hash VARCHAR(256) NOT NULL,
  created_at    DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_credential_user (user_id),
  UNIQUE KEY uk_credential_login (login_name),
  CONSTRAINT fk_credential_user FOREIGN KEY(user_id) REFERENCES sz_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO sz_schema_migration(version,applied_at) VALUES('V006',NOW(6));

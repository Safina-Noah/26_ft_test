CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  github_id BIGINT NOT NULL UNIQUE,
  login VARCHAR(100) NOT NULL,
  nickname VARCHAR(100) NOT NULL,
  avatar_url VARCHAR(1000),
  profile_ready BOOLEAN NOT NULL DEFAULT FALSE,
  access_token TEXT NOT NULL,
  refresh_token TEXT,
  token_expires_at TIMESTAMP NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE projects (
  id VARCHAR(36) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  github_id BIGINT NOT NULL,
  owner_login VARCHAR(100) NOT NULL,
  repo_name VARCHAR(200) NOT NULL,
  description TEXT,
  branch_name VARCHAR(200) NOT NULL,
  repo_type VARCHAR(10) NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE(user_id, github_id),
  FOREIGN KEY(user_id) REFERENCES users(id)
);
CREATE TABLE analysis_jobs (
  id VARCHAR(36) PRIMARY KEY,
  project_id VARCHAR(36) NOT NULL,
  status VARCHAR(30) NOT NULL,
  stage VARCHAR(40) NOT NULL,
  attempts INT NOT NULL DEFAULT 0,
  sha VARCHAR(64),
  active_project VARCHAR(36) UNIQUE,
  claim_token VARCHAR(36),
  lease_until TIMESTAMP NULL,
  next_run TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  error_code VARCHAR(60),
  error_message TEXT,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY(project_id) REFERENCES projects(id)
);
CREATE INDEX jobs_queue ON analysis_jobs(status, next_run);
CREATE TABLE project_analyses (
  id VARCHAR(36) PRIMARY KEY,
  project_id VARCHAR(36) NOT NULL,
  job_id VARCHAR(36) NOT NULL UNIQUE,
  commit_sha VARCHAR(64) NOT NULL,
  schema_version VARCHAR(20) NOT NULL,
  payload LONGTEXT NOT NULL,
  sources LONGTEXT NOT NULL,
  coverage LONGTEXT NOT NULL,
  languages LONGTEXT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY(project_id) REFERENCES projects(id)
);
CREATE TABLE feature_writings (
  analysis_id VARCHAR(36) NOT NULL,
  feature_id VARCHAR(100) NOT NULL,
  body LONGTEXT,
  note LONGTEXT,
  revision BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY(analysis_id, feature_id),
  FOREIGN KEY(analysis_id) REFERENCES project_analyses(id)
);
CREATE TABLE images (
  id VARCHAR(36) PRIMARY KEY,
  user_id BIGINT NOT NULL,
  project_id VARCHAR(36),
  image_kind VARCHAR(20) NOT NULL,
  original_name VARCHAR(240) NOT NULL,
  content_type VARCHAR(50) NOT NULL,
  byte_size BIGINT NOT NULL,
  width_px INT NOT NULL,
  height_px INT NOT NULL,
  created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  FOREIGN KEY(user_id) REFERENCES users(id),
  FOREIGN KEY(project_id) REFERENCES projects(id)
);
CREATE TABLE image_contents (
  image_id VARCHAR(36) PRIMARY KEY,
  bytes LONGBLOB NOT NULL,
  FOREIGN KEY(image_id) REFERENCES images(id)
);

ALTER TABLE analysis_jobs ADD COLUMN snapshot LONGTEXT;
ALTER TABLE project_analyses ADD COLUMN complete BOOLEAN NOT NULL DEFAULT TRUE;
CREATE TABLE analysis_sections (
  job_id VARCHAR(36) NOT NULL,
  section_key VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  attempts INT NOT NULL DEFAULT 0,
  payload LONGTEXT,
  error_message TEXT,
  next_run TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY(job_id, section_key),
  FOREIGN KEY(job_id) REFERENCES analysis_jobs(id)
);

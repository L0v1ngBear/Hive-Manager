-- XXL-JOB 2.4.1 stores the default admin/123456 password as an MD5 hash
-- in its bundled doc/db/tables_xxl_job.sql.
-- Keep this repair narrow: only reset admin when the password is still the
-- incorrect SHA-256 default from an earlier Hive migration.

USE `xxl_job`;

UPDATE `xxl_job_user`
SET `password` = 'e10adc3949ba59abbe56e057f20f883e'
WHERE `username` = 'admin'
  AND `password` = '8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92';

INSERT INTO `xxl_job_user` (`username`, `password`, `role`, `permission`)
SELECT 'admin', 'e10adc3949ba59abbe56e057f20f883e', 1, NULL
WHERE NOT EXISTS (
  SELECT 1 FROM `xxl_job_user` WHERE `username` = 'admin'
);

-- Cree l'utilisateur du cours, pour une installation MySQL existante.
-- A executer en root, avant 02-student-tracker.sql.
--
-- Le mot de passe est celui publie par l'enonce. Il ne protege rien : cet
-- utilisateur n'a de droits que sur une base de demonstration, sur un poste.
CREATE USER IF NOT EXISTS 'springstudent'@'%' IDENTIFIED BY 'springstudent';
GRANT ALL PRIVILEGES ON `student_tracker`.* TO 'springstudent'@'%';
FLUSH PRIVILEGES;

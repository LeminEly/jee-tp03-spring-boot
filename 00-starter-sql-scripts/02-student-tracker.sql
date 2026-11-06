CREATE DATABASE IF NOT EXISTS `student_tracker`;
USE `student_tracker`;

CREATE TABLE `student` (
  `id`         INT NOT NULL AUTO_INCREMENT,
  `first_name` VARCHAR(45),
  `last_name`  VARCHAR(45),
  `email`      VARCHAR(45),
  PRIMARY KEY (`id`)
);

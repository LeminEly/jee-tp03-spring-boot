package com.tp3.cruddemo;

import com.tp3.cruddemo.dao.StudentDAO;
import com.tp3.cruddemo.entity.Student;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CruddemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(CruddemoApplication.class, args);
	}

	@Bean
	public CommandLineRunner commandLineRunner(StudentDAO studentDAO) {
		return runner -> {
			createSeveralStudents(studentDAO);
			updateEmails(studentDAO);
			deleteAllStudents(studentDAO);
		};
	}

	private void createSeveralStudents(StudentDAO studentDAO) {
		studentDAO.save(new Student("Paul", "Doe", "paul@test.com"));
		studentDAO.save(new Student("Mary", "Public", "mary@test.com"));
		studentDAO.save(new Student("Bonita", "Applebum", "bonita@test.com"));
	}

	private void updateEmails(StudentDAO studentDAO) {
		int n = studentDAO.updateEmailByLastName("unknown@test.com", "Doe");
		System.out.println("Updated row count: " + n);
	}

	private void deleteAllStudents(StudentDAO studentDAO) {
		int n = studentDAO.deleteAll();
		System.out.println("Deleted row count: " + n);
	}
}

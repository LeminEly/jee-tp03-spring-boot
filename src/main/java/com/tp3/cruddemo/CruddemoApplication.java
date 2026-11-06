package com.tp3.cruddemo;

import com.tp3.cruddemo.dao.StudentDAO;
import com.tp3.cruddemo.entity.Student;
import java.util.List;
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
			findStudentsByLastName(studentDAO);
		};
	}

	private void createSeveralStudents(StudentDAO studentDAO) {
		studentDAO.save(new Student("Paul", "Doe", "paul@test.com"));
		studentDAO.save(new Student("Mary", "Public", "mary@test.com"));
		studentDAO.save(new Student("Bonita", "Applebum", "bonita@test.com"));
	}

	private void findStudentsByLastName(StudentDAO studentDAO) {
		List<Student> theStudents = studentDAO.findByLastName("Doe");
		theStudents.forEach(System.out::println);
	}
}

package com.tp3.cruddemo;

import com.tp3.cruddemo.dao.StudentDAO;
import com.tp3.cruddemo.entity.Student;
import com.tp3.cruddemo.service.StudentService;
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
	public CommandLineRunner commandLineRunner(StudentDAO studentDAO, StudentService studentService) {
		return args -> {
			String action = (args.length > 0) ? args[0] : "read";
			switch (action) {
				case "create" -> createStudent(studentDAO);
				case "read" -> readStudents(studentDAO);
				case "update" -> updateStudent(studentDAO);
				case "delete" -> deleteStudent(studentDAO);
				case "create-many" -> createSeveralStudents(studentDAO);
				case "update-emails" -> updateEmails(studentDAO);
				case "delete-all" -> deleteAllStudents(studentDAO);
				case "find-named" -> findByLastNameNamed(studentDAO);
				case "enroll-batch" -> enrollBatch(studentService, studentDAO);
				default -> System.out.println("Action inconnue: " + action);
			}
		};
	}

	private void createStudent(StudentDAO studentDAO) {
		Student tempStudent = new Student("Paul", "Doe", "paul@test.com");
		studentDAO.save(tempStudent);
		System.out.println("Saved student. Generated id: " + tempStudent.getId());
	}

	private void createSeveralStudents(StudentDAO studentDAO) {
		Student[] students = {
				new Student("Mary", "Public", "mary@test.com"),
				new Student("Bonita", "Applebum", "bonita@test.com"),
				new Student("John", "Doe", "john@test.com")
		};
		for (Student student : students) {
			studentDAO.save(student);
			System.out.println("Saved student. Generated id: " + student.getId());
		}
	}

	private void readStudents(StudentDAO studentDAO) {
		System.out.println("findById(1): " + studentDAO.findById(1));

		System.out.println("findAll, sorted by last name:");
		studentDAO.findAll().forEach(System.out::println);

		System.out.println("findByLastName(\"Doe\"):");
		studentDAO.findByLastName("Doe").forEach(System.out::println);
	}

	private void updateStudent(StudentDAO studentDAO) {
		int studentId = 1;
		Student myStudent = studentDAO.findById(studentId);
		if (myStudent == null) {
			System.out.println("No student with id " + studentId);
			return;
		}
		myStudent.setFirstName("John");
		studentDAO.update(myStudent);
		System.out.println("Updated student: " + studentDAO.findById(studentId));
	}

	private void deleteStudent(StudentDAO studentDAO) {
		studentDAO.delete(1);
		System.out.println("After delete, findById(1) = " + studentDAO.findById(1));
	}

	private void updateEmails(StudentDAO studentDAO) {
		int n = studentDAO.updateEmailByLastName("unknown@test.com", "Doe");
		System.out.println("Updated row count: " + n);
	}

	private void deleteAllStudents(StudentDAO studentDAO) {
		int n = studentDAO.deleteAll();
		System.out.println("Deleted row count: " + n);
	}

	private void findByLastNameNamed(StudentDAO studentDAO) {
		System.out.println("Named query Student.findByLastName(\"Doe\"):");
		studentDAO.findByLastNameNamed("Doe").forEach(System.out::println);
	}

	private void enrollBatch(StudentService studentService, StudentDAO studentDAO) {
		int before = studentDAO.findAll().size();
		List<Student> batch = List.of(
				new Student("Ada", "Lovelace", "ada@test.com"),
				new Student("Nobody", null, "nobody@test.com"));
		try {
			studentService.enrollAll(batch);
		} catch (IllegalArgumentException e) {
			System.out.println("Batch refused: " + e.getMessage());
		}
		System.out.println("Rows before: " + before + ", rows after: " + studentDAO.findAll().size());
	}
}

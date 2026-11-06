package com.tp3.cruddemo.dao;

import static org.assertj.core.api.Assertions.assertThat;

import com.tp3.cruddemo.entity.Student;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

// Ni @Transactional ni @DataJpaTest : chaque methode du DAO doit ouvrir sa propre
// transaction, comme en production. Une transaction de test la masquerait.
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
class StudentDAOTest {

	@Autowired
	private StudentDAO studentDAO;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void emptyTable() {
		jdbc.update("DELETE FROM student");
	}

	@Test
	void saveWritesTheRowAndGivesTheGeneratedIdBackToTheObject() {
		Student student = new Student("Paul", "Doe", "paul@test.com");
		assertThat(student.getId()).isZero();

		studentDAO.save(student);

		assertThat(student.getId()).isPositive();
		assertThat(jdbc.queryForObject("SELECT first_name FROM student WHERE id = ?", String.class,
				student.getId())).isEqualTo("Paul");
	}

	@Test
	void findByIdReturnsTheStudent() {
		Student student = new Student("Paul", "Doe", "paul@test.com");
		studentDAO.save(student);

		assertThat(studentDAO.findById(student.getId()).getFirstName()).isEqualTo("Paul");
	}

	@Test
	void findByIdReturnsNullWhenTheIdDoesNotExist() {
		assertThat(studentDAO.findById(999)).isNull();
	}

	@Test
	void findAllIsSortedByLastName() {
		studentDAO.save(new Student("Paul", "Doe", "paul@test.com"));
		studentDAO.save(new Student("Bonita", "Applebum", "bonita@test.com"));
		studentDAO.save(new Student("Mary", "Public", "mary@test.com"));

		assertThat(studentDAO.findAll()).extracting(Student::getLastName)
				.containsExactly("Applebum", "Doe", "Public");
	}

	@Test
	void findByLastNameKeepsOnlyThatName() {
		studentDAO.save(new Student("Paul", "Doe", "paul@test.com"));
		studentDAO.save(new Student("John", "Doe", "john@test.com"));
		studentDAO.save(new Student("Mary", "Public", "mary@test.com"));

		assertThat(studentDAO.findByLastName("Doe")).extracting(Student::getFirstName)
				.containsExactlyInAnyOrder("Paul", "John");
	}

	@Test
	void aNamedParameterTreatsAnInjectionAttemptAsAPlainValue() {
		studentDAO.save(new Student("Paul", "Doe", "paul@test.com"));

		assertThat(studentDAO.findByLastName("x' OR '1'='1")).isEmpty();
	}

	@Test
	void updateWritesTheChangeOfADetachedStudent() {
		Student student = new Student("Paul", "Doe", "paul@test.com");
		studentDAO.save(student);

		Student detached = studentDAO.findById(student.getId());
		detached.setFirstName("John");
		studentDAO.update(detached);

		assertThat(studentDAO.findById(student.getId()).getFirstName()).isEqualTo("John");
	}

	@Test
	void deleteRemovesTheRow() {
		Student student = new Student("Paul", "Doe", "paul@test.com");
		studentDAO.save(student);

		studentDAO.delete(student.getId());

		assertThat(studentDAO.findById(student.getId())).isNull();
	}

	@Test
	void deleteOfAnUnknownIdDoesNothing() {
		studentDAO.save(new Student("Paul", "Doe", "paul@test.com"));

		studentDAO.delete(999);

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM student", Integer.class)).isEqualTo(1);
	}
}

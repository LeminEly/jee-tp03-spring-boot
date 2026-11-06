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
}

package com.tp3.cruddemo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tp3.cruddemo.dao.StudentDAO;
import com.tp3.cruddemo.entity.Student;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

// Le meme lot, la meme faute, deux resultats : c'est l'interet d'une transaction
// au niveau du service plutot que du DAO.
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
class StudentServiceTest {

	@Autowired
	private StudentService studentService;

	@Autowired
	private StudentDAO studentDAO;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void emptyTable() {
		jdbc.update("DELETE FROM student");
	}

	private List<Student> batchWithAFaultyLastStudent() {
		return List.of(new Student("Paul", "Doe", "paul@test.com"), new Student("Mary", null, "mary@test.com"));
	}

	private int rows() {
		return jdbc.queryForObject("SELECT COUNT(*) FROM student", Integer.class);
	}

	@Test
	void throughTheServiceTheWholeBatchIsRolledBack() {
		assertThatThrownBy(() -> studentService.enrollAll(batchWithAFaultyLastStudent()))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(rows()).isZero();
	}

	@Test
	void throughTheDaoAloneTheFirstStudentStaysWritten() {
		assertThatThrownBy(() -> {
			for (Student student : batchWithAFaultyLastStudent()) {
				if (student.getLastName() == null) {
					throw new IllegalArgumentException("Student without a last name: " + student);
				}
				studentDAO.save(student);
			}
		}).isInstanceOf(IllegalArgumentException.class);

		assertThat(rows()).isEqualTo(1);
	}
}

package com.tp3.cruddemo.dao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tp3.cruddemo.entity.Student;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TransactionRequiredException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

// Fige ce que REPONSES.md affirme sur JPA : une reponse se rejoue au lieu de se croire.
@SpringBootTest
@TestPropertySource(locations = "classpath:application.properties")
class JpaBehaviourTest {

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private StudentDAO studentDAO;

	@Autowired
	private TransactionTemplate transaction;

	@Autowired
	private JdbcTemplate jdbc;

	@BeforeEach
	void emptyTable() {
		jdbc.update("DELETE FROM student");
	}

	// Question 2
	@Test
	void aWriteWithoutTransactionIsRefused() {
		assertThatThrownBy(() -> entityManager.persist(new Student("Paul", "Doe", "paul@test.com")))
				.isInstanceOf(TransactionRequiredException.class);
	}

	// Question 2
	@Test
	void aReadWithoutTransactionWorks() {
		Student student = new Student("Paul", "Doe", "paul@test.com");
		studentDAO.save(student);

		assertThat(entityManager.find(Student.class, student.getId())).isNotNull();
	}

	// Question 3
	@Test
	void mergeReturnsAManagedCopyAndLeavesItsArgumentDetached() {
		Student student = new Student("Paul", "Doe", "paul@test.com");
		studentDAO.save(student);

		transaction.executeWithoutResult(status -> {
			Student managed = entityManager.merge(student);

			assertThat(managed).isNotSameAs(student);
			assertThat(entityManager.contains(managed)).isTrue();
			assertThat(entityManager.contains(student)).isFalse();
		});
	}

	// Question 6 : la meme faute, levee par le DAO puis par l'EntityManager nu.
	@Test
	void repositoryTranslatesJpaExceptionsIntoSpringOnes() {
		Student existing = new Student("Paul", "Doe", "paul@test.com");
		studentDAO.save(existing);

		assertThatThrownBy(() -> studentDAO.save(existing))
				.isInstanceOf(DataAccessException.class);

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> entityManager.persist(existing)))
				.isInstanceOf(PersistenceException.class)
				.isNotInstanceOf(DataAccessException.class);
	}

	// Question 7
	@Test
	void removeRefusesAnEntityThatIsNotManaged() {
		Student existing = new Student("Paul", "Doe", "paul@test.com");
		studentDAO.save(existing);
		Student sameId = new Student();
		sameId.setId(existing.getId());

		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> entityManager.remove(sameId)))
				.isInstanceOf(IllegalArgumentException.class);

		assertThat(studentDAO.findById(existing.getId())).isNotNull();
	}

	// Commentaire de StudentDAOImpl.delete
	@Test
	void removeOfNullIsRefused() {
		assertThatThrownBy(() -> transaction.executeWithoutResult(status -> entityManager.remove(null)))
				.isInstanceOf(IllegalArgumentException.class);
	}
}

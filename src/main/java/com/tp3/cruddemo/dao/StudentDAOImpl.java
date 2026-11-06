package com.tp3.cruddemo.dao;

import com.tp3.cruddemo.entity.Student;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class StudentDAOImpl implements StudentDAO {

	private EntityManager entityManager;

	@Autowired
	public StudentDAOImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	@Transactional
	public void save(Student theStudent) {
		entityManager.persist(theStudent);
	}

	@Override
	public Student findById(Integer id) {
		return entityManager.find(Student.class, id);
	}

	@Override
	public List<Student> findAll() {
		TypedQuery<Student> theQuery =
				entityManager.createQuery("FROM Student ORDER BY lastName", Student.class);
		return theQuery.getResultList();
	}

	@Override
	public List<Student> findByLastName(String theLastName) {
		TypedQuery<Student> theQuery = entityManager.createQuery(
				"FROM Student WHERE lastName = :theData", Student.class);
		theQuery.setParameter("theData", theLastName);
		return theQuery.getResultList();
	}

	@Override
	@Transactional
	public void update(Student theStudent) {
		entityManager.merge(theStudent);
	}

	@Override
	@Transactional
	public void delete(Integer id) {
		Student theStudent = entityManager.find(Student.class, id);
		// Id inexistant : remove(null) leverait une IllegalArgumentException. Le resultat
		// voulu, aucune ligne avec cet id, est deja atteint : il n'y a rien a faire.
		if (theStudent != null) {
			entityManager.remove(theStudent);
		}
	}
}

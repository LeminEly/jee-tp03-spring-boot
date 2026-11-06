package com.tp3.cruddemo.service;

import com.tp3.cruddemo.dao.StudentDAO;
import com.tp3.cruddemo.entity.Student;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentServiceImpl implements StudentService {

	private StudentDAO studentDAO;

	@Autowired
	public StudentServiceImpl(StudentDAO studentDAO) {
		this.studentDAO = studentDAO;
	}

	// La transaction s'ouvre ici, et chaque save du DAO la rejoint au lieu d'ouvrir
	// la sienne (propagation REQUIRED, le defaut). Une exception annule donc tout le
	// lot, y compris les etudiants deja enregistres.
	@Override
	@Transactional
	public void enrollAll(List<Student> theStudents) {
		for (Student student : theStudents) {
			if (student.getLastName() == null || student.getLastName().isBlank()) {
				throw new IllegalArgumentException("Student without a last name: " + student);
			}
			studentDAO.save(student);
		}
	}
}

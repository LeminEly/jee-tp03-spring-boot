package com.tp3.cruddemo.service;

import com.tp3.cruddemo.entity.Student;
import java.util.List;

public interface StudentService {

	// Tout ou rien : si un etudiant est refuse, aucun n'est enregistre.
	void enrollAll(List<Student> theStudents);
}

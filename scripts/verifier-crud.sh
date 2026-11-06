#!/usr/bin/env bash
# Joue toutes les actions de l'archive contre le vrai MySQL, sur une table vide,
# et verifie chaque resultat. Les tests tournent sur H2 : ce script est la seule
# preuve que le CRUD marche sur la base de l'enonce. L'integration continue le
# rejoue a chaque poussee.
#
# Attention : il vide la table student.
# Quand : archive construite (./mvnw package) et base demarree.
# docker compose up -d --wait
set -uo pipefail
cd "$(dirname "$0")/.."

export SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL:-jdbc:mysql://localhost:3307/student_tracker}
# validate : le demarrage echoue si l'entite et la table divergent.
export SPRING_JPA_HIBERNATE_DDL_AUTO=validate

JAR=target/cruddemo-0.0.1-SNAPSHOT.jar

sql() {
	docker compose exec -T -e MYSQL_PWD=springstudent mysql \
		mysql -N -u springstudent student_tracker -e "$1"
}

verifier() { # $1 = action, $2 = texte attendu dans la sortie
	sortie=$(java -jar "$JAR" "$1" 2>&1)
	code=$?
	if [ "$code" -ne 0 ] || ! grep -qF "$2" <<< "$sortie"; then
		echo "ECHEC $1 : code $code, attendu « $2 »" >&2
		echo "$sortie" | tail -30 >&2
		exit 1
	fi
	echo "ok    $1 : $2"
}

sql 'TRUNCATE TABLE student'
verifier create "Saved student. Generated id: 1"
verifier create-many "Saved student. Generated id: 4"
verifier read "Student{id=3, firstName='Bonita', lastName='Applebum'"
verifier update "Updated student: Student{id=1, firstName='John'"
verifier update-emails "Updated row count: 2"
verifier find-named "Student{id=4, firstName='John', lastName='Doe'"
verifier enroll-batch "Rows before: 4, rows after: 4"
verifier delete "After delete, findById(1) = null"
verifier delete "After delete, findById(1) = null"
verifier update "No student with id 1"
verifier delete-all "Deleted row count: 3"
verifier inconnue "Action inconnue: inconnue"

restant=$(sql 'SELECT COUNT(*) FROM student')
if [ "$restant" != "0" ]; then
	echo "ECHEC : $restant lignes restent apres delete-all" >&2
	exit 1
fi
echo "ok    table vide"

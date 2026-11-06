#!/usr/bin/env bash
# Montre une action du CRUD contre MySQL, avec les logs SQL d'Hibernate.
#
#   scripts/demo-crud.sh create    capture 01-create.png  (id genere, INSERT et valeurs liees)
#   scripts/demo-crud.sh read      capture 02-read.png
#   scripts/demo-crud.sh update    capture 03-update.png  (table avant et apres)
#   scripts/demo-crud.sh delete    capture 04-delete.png  (table avant et apres)
#
# Dans cet ordre : create vide la table pour que le premier id soit 1, et les
# actions suivantes travaillent sur ce que create a ecrit.
#
# Les lignes de log gardent le nom du journal et le message. L'horodatage et le
# numero de processus sont retires : ils elargiraient la capture sans rien prouver.
set -uo pipefail
cd "$(dirname "$0")/.."

action=${1:-}
case "$action" in
	create | read | update | delete) ;;
	*)
		echo "Usage : $0 create|read|update|delete" >&2
		exit 2
		;;
esac

if [ -f .sdkmanrc ]; then
	version=$(sed -n 's/^java=//p' .sdkmanrc)
	candidat="${SDKMAN_DIR:-$HOME/.sdkman}/candidates/java/$version"
	if [ -x "$candidat/bin/java" ]; then
		export JAVA_HOME="$candidat"
		export PATH="$JAVA_HOME/bin:$PATH"
	fi
fi

JAR=target/cruddemo-0.0.1-SNAPSHOT.jar
export SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL:-jdbc:mysql://localhost:3307/student_tracker}

docker compose up -d --wait >/dev/null 2>&1 || {
	echo "La base ne demarre pas : docker compose up -d --wait" >&2
	exit 1
}
[ -f "$JAR" ] || ./mvnw -q -B package -DskipTests

table() {
	echo "\$ mysql student_tracker -e 'SELECT * FROM student'"
	docker compose exec -T -e MYSQL_PWD=springstudent mysql \
		mysql -t -u springstudent student_tracker -e 'SELECT * FROM student'
	echo
}

lancer() { # $1 = action, $2 = texte que la sortie doit contenir
	echo "\$ java -jar $JAR $1"
	sortie=$(java -jar "$JAR" "$1" 2>&1)
	code=$?
	sed -E 's/^[0-9T:.+-]+Z? +(DEBUG|TRACE|INFO|WARN|ERROR) +[0-9]+ --- \[ *[^]]*\] /\1 /; s/ {2,}: / : /' <<< "$sortie"
	echo
	# Une capture ne vaut que si elle montre ce qu'elle pretend montrer.
	# Registre du socle, A.39.
	if [ "$code" -ne 0 ] || ! grep -qF "$2" <<< "$sortie"; then
		echo "ATTENTION : code $code, ou « $2 » absent. Ne pas capturer." >&2
		exit 1
	fi
}

case "$action" in
	create)
		echo "===== 5.1 : create, persist et id genere ====="
		docker compose exec -T -e MYSQL_PWD=springstudent mysql \
			mysql -u springstudent student_tracker -e 'TRUNCATE TABLE student'
		echo "(table videe, pour que le premier id genere soit 1)"
		echo
		lancer create "Saved student. Generated id: 1"
		table
		;;
	read)
		echo "===== 5.2 a 5.4 : read, find, JPQL trie, parametre nomme ====="
		# create n'a ecrit qu'un etudiant : la lecture triee a besoin de plusieurs.
		# Une seule fois, pour que le script puisse etre relance sans tout dupliquer.
		lignes=$(docker compose exec -T -e MYSQL_PWD=springstudent mysql \
			mysql -N -u springstudent student_tracker -e 'SELECT COUNT(*) FROM student')
		if [ "$lignes" -le 1 ]; then
			java -jar "$JAR" create-many > /dev/null 2>&1
			echo "(trois etudiants ajoutes par l'action create-many)"
			echo
		fi
		lancer read "findByLastName(\"Doe\"):"
		;;
	update)
		echo "===== 5.5 : update, merge ====="
		table
		lancer update "Updated student: Student{id=1, firstName='John'"
		table
		;;
	delete)
		echo "===== 5.6 : delete, remove ====="
		table
		lancer delete "After delete, findById(1) = null"
		table
		;;
esac

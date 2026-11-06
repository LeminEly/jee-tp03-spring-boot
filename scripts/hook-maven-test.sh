#!/usr/bin/env bash
# Porte de compilation et de tests. Appelee par pre-commit avant le commit.
# Refuse de compiler avec le mauvais JDK : un JRE suffit a lancer, pas a
# compiler, et un JDK trop ancien echoue sur « release version 25 not supported ».
set -euo pipefail
cd "$(dirname "$0")/.."

attendu=$(sed -n 's/^java=//p' .sdkmanrc)
majeure=${attendu%%.*}

candidat="${SDKMAN_DIR:-$HOME/.sdkman}/candidates/java/$attendu"
if [ -x "$candidat/bin/javac" ]; then
	export JAVA_HOME="$candidat"
	export PATH="$JAVA_HOME/bin:$PATH"
fi

if ! command -v javac >/dev/null 2>&1; then
	echo "javac introuvable. Un JRE ne suffit pas : il faut un JDK." >&2
	echo "Attendu : $attendu. Dans le depot : sdk env" >&2
	exit 1
fi

trouvee=$(javac -version 2>&1 | awk '{print $2}' | cut -d. -f1)
if [ "$trouvee" != "$majeure" ]; then
	echo "javac $trouvee alors que le projet exige $majeure." >&2
	echo "Dans le depot : sdk env" >&2
	exit 1
fi

exec ./mvnw -q -B test

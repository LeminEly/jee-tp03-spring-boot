#!/usr/bin/env bash
# Porte sur le message de commit. Appelee par pre-commit au stage commit-msg.
# Refuse tout message qui ne suit pas la convention du projet.
set -euo pipefail

fichier=$1
message=$(sed -n '1p' "$fichier")

# Les messages engendres par git lui-meme passent sans controle : on ne les
# ecrit pas, et les reecrire casserait le rebase ou la fusion en cours.
case "$message" in
	Merge\ *|Revert\ *|fixup!\ *|squash!\ *) exit 0 ;;
esac

motif='^(feat|fix|refactor|chore|docs|test|sec|build|ci|perf|revert)(\([a-z0-9._-]+\))?: [a-z].{0,60}$'

if ! printf '%s' "$message" | grep -qE "$motif"; then
	cat >&2 <<'AIDE'
Message de commit refuse.

Attendu : type(portee facultative): description en minuscule
Types    : feat fix refactor chore docs test sec build ci perf revert
Longueur : 61 caracteres au plus apres les deux points

Exemples :
  feat(5.1): student entity and save
  docs: add the ddl-auto table to the answers
AIDE
	printf 'Recu     : %s\n' "$message" >&2
	exit 1
fi

# Securite

## Signaler un probleme

Ce depot porte un travail pratique academique. Il n'a ni utilisateur reel, ni
donnee reelle, ni exposition reseau. Pour signaler un probleme, ouvrir une issue
sur le depot.

## Le mot de passe en clair

`application.properties` et `compose.yaml` contiennent `springstudent` comme
utilisateur et comme mot de passe. Ce n'est pas un secret : ce sont les
identifiants publies par l'enonce. Ils ouvrent une base de demonstration, jetable,
qui ne contient que des etudiants fictifs.

Consequence a connaitre, et a ne pas transposer : sur un vrai projet, ces valeurs
viendraient de l'environnement ou d'un coffre, et jamais du depot.

## La base de demonstration

`compose.yaml` publie le port sur `127.0.0.1` uniquement, jamais sur toutes les
interfaces. Le mot de passe `root` est tire au hasard par l'image et n'est connu
de personne : rien dans le projet n'en a besoin.

## Le balayage de secrets

Il tourne en pre-commit et sur **tout l'historique** en integration continue.
Une porte qui ne sert jamais reste une porte : elle sert le jour ou quelqu'un
colle une vraie valeur par inadvertance.

## Chaine d'approvisionnement

- La distribution Maven telechargee par le wrapper est verrouillee par empreinte
  (`distributionSha256Sum`).
- L'image MySQL est epinglee par empreinte dans `compose.yaml`.
- Les actions GitHub sont epinglees par empreinte de commit.
- Le binaire gitleaks de l'integration continue est verifie par empreinte.
- Aucune mise a jour automatique de dependances n'est activee. Sur un depot qui
  va etre note, une demande de fusion automatique modifierait le code rendu.

## Portee

Le travail se fait sur un poste local. Aucun deploiement, aucun hebergement.

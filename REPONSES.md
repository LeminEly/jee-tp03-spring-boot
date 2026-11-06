# Reponses : TP 03, Hibernate et JPA

Chaque affirmation de ce document a ete mesuree sur MySQL 8.4 ou est figee par
un test. Le test est nomme a cote de la reponse : `./mvnw test` la rejoue.

## Questions de la section 6

### 1. JPA, Hibernate, Spring Data JPA

- **JPA** (Jakarta Persistence) est une **specification** : des interfaces et des
  annotations (`EntityManager`, `@Entity`, `@Id`). Elle n'execute rien.
- **Hibernate** est une **implementation** de JPA. C'est lui qui genere le SQL,
  gere le contexte de persistance et parle au pilote JDBC.
- **Spring Data JPA**, apporte par `spring-boot-starter-data-jpa`, est
  l'**orchestration** : il assemble le tout et configure seul le `DataSource`
  (HikariCP), l'`EntityManagerFactory` et le gestionnaire de transactions.

Le code du DAO n'importe que `jakarta.persistence.*`. Il ne connait pas
Hibernate, et changerait d'implementation sans etre modifie.

### 2. Pourquoi les ecritures sont `@Transactional` et pas les lectures

JPA refuse d'ecrire hors transaction. Un `persist` sans transaction leve
`TransactionRequiredException` : rien n'est ecrit
(`JpaBehaviourTest.aWriteWithoutTransactionIsRefused`).

Une lecture, elle, fonctionne sans transaction declaree
(`aReadWithoutTransactionWorks`). En ajouter une ne changerait rien au resultat,
et ouvrirait une transaction pour rien.

La transaction donne aux ecritures leur garantie : tout est valide au `commit`,
ou tout est annule au `rollback` si une exception survient.

### 3. `persist` contre `merge`

| Methode | Pour quoi | Cas typique |
| --- | --- | --- |
| `persist(e)` | `INSERT` d'une nouvelle entite, sans id. L'objet passe devient gere, et recoit l'id genere | Creation |
| `merge(e)` | Recopie l'etat d'une entite detachee, avec id, sur une instance geree, puis `UPDATE` au `commit`. Renvoie cette instance geree | Mise a jour |

`merge` renvoie **une autre instance** que celle recue. L'objet passe en
parametre reste detache : le modifier ensuite n'a aucun effet en base
(`mergeReturnsAManagedCopyAndLeavesItsArgumentDetached`).

Mesure en 5.5 : l'objet lu par `findById` est deja detache quand on le modifie,
car la lecture s'est faite sans transaction. Seul l'appel a `update`, donc a
`merge`, produit l'`UPDATE`.

### 4. JPQL contre SQL

JPQL porte sur le **modele objet** : la classe `Student` et ses champs
(`lastName`). SQL porte sur la **base** : la table `student` et ses colonnes
(`last_name`). `FROM Student WHERE lastName = :theData` designe donc la classe
et le champ.

Hibernate traduit. Log mesure en 5.4 :

```
select s1_0.id,s1_0.email,s1_0.first_name,s1_0.last_name from student s1_0 where s1_0.last_name=?
```

Ecrire `FROM student` ou `first_name` dans une requete JPQL echoue : ni cette
classe ni ce champ n'existent.

### 5. Le parametre nomme contre la concatenation

Avec `:theData`, la valeur n'entre jamais dans le texte de la requete. Elle part
a part, comme parametre lie (`binding parameter (1:VARCHAR) <- [Doe]` dans les
logs). Quoi qu'elle contienne, elle reste une valeur et ne devient jamais du
code.

Une concatenation insere la valeur dans le texte. `x' OR '1'='1` y deviendrait
une condition toujours vraie, et la requete rendrait toute la table. Passe en
parametre nomme, ce meme texte ne trouve rien, car aucun nom de famille ne
s'ecrit ainsi (`StudentDAOTest.aNamedParameterTreatsAnInjectionAttemptAsAPlainValue`).

### 6. Ce que `@Repository` ajoute a `@Component`

La **traduction des exceptions**. Les exceptions propres a JPA ou au pilote sont
converties en `DataAccessException` de Spring, une hierarchie unique quelle que
soit la technologie de persistance.

Prouve par `repositoryTranslatesJpaExceptionsIntoSpringOnes` : la meme faute
(enregistrer deux fois le meme etudiant) arrive en `DataAccessException` quand
elle passe par le DAO, et en `PersistenceException` brute quand elle passe par
l'`EntityManager` nu. Remplacer `@Repository` par `@Component` sur
`StudentDAOImpl` fait echouer ce test : mesure faite.

### 7. Pourquoi pas `entityManager.remove(new Student(5))`

`remove` n'accepte qu'une entite **geree** par le contexte de persistance. Un
objet cree par `new` n'en fait pas partie, meme avec le bon id : JPA leve
`IllegalArgumentException` et ne supprime rien
(`removeRefusesAnEntityThatIsNotManaged`).

Il faut d'abord **lire** l'entite avec `find`, dans la meme transaction, puis la
supprimer. C'est ce que fait `StudentDAOImpl.delete`. Pour un id inexistant,
`find` rend `null`, et `remove(null)` leverait aussi `IllegalArgumentException`
(`removeOfNullIsRefused`). Le DAO teste donc le `null` et ne fait rien : le
resultat voulu, aucune ligne avec cet id, est deja atteint. Mesure en 5.6 : un
second `delete` sur le meme id se termine normalement.

### 8. `spring.jpa.hibernate.ddl-auto`

Chaque valeur a ete mesuree sur MySQL, en partant du script SQL de l'enonce.

| Valeur | Comportement mesure |
| --- | --- |
| `none` | Ne touche pas au schema. Si la table ne correspond pas a l'entite, l'application demarre, puis echoue a la premiere requete (`Unknown column 's1_0.email'`) |
| `validate` | Compare l'entite et la table, n'ecrit rien. Table conforme : demarrage normal. Colonne `email` supprimee : refus de demarrer, `Schema validation: missing column [email]` |
| `update` | Ajoute ce qui manque (`alter table student add column email`) et garde les donnees. Mais il **modifie** aussi l'existant : `first_name` et `last_name` passent de `varchar(45)` a `varchar(255)` |
| `create` | `drop table if exists student` puis `create table` au demarrage : la ligne presente avant le lancement a disparu |
| `create-drop` | Comme `create`, puis `drop table` a l'arret. La table n'existe plus apres l'execution |

En production : `validate`, avec des migrations versionnees (Flyway, Liquibase)
pour faire evoluer le schema. `validate` refuse de demarrer sur un schema qui
diverge, avant qu'une requete n'echoue chez un utilisateur. `none` convient
aussi, mais ne detecte rien.

Jamais `create` ni `create-drop` : chaque redemarrage efface toutes les donnees.
Et pas `update` non plus : la mesure montre qu'il modifie des colonnes existantes
sans le dire, et il ne sait ni renommer, ni supprimer, ni migrer des donnees.

L'etat livre est `none`, comme le demande l'enonce.

## Mesures des exercices

| Ex. | Mesure sur MySQL |
| --- | --- |
| 5.1 | `Saved student. Generated id: 1`, puis `2` au lancement suivant : l'id `AUTO_INCREMENT` est reinjecte dans l'objet |
| 5.3 | Sans `ORDER BY` : ordre d'insertion. `ORDER BY lastName` : Applebum, Doe, Public. `DESC` : l'inverse |
| 5.5 | `update student set email=?,first_name=?,last_name=? where id=?` : `merge` reecrit toutes les colonnes, pas seulement celle modifiee |
| 5.6 | Deux `delete` de suite sur l'id 1 : le second ne leve rien |
| 5.7 | Mise a jour en masse sur `Doe` : `Updated row count: 1`. Puis `Deleted row count: 3`, table vide. Une seule requete chacune |

`scripts/verifier-crud.sh` rejoue toutes les actions contre MySQL et verifie
chaque resultat. L'integration continue le lance a chaque poussee.

## Extensions de la section 9

### 1. Schema genere par Hibernate

Mesure avec `ddl-auto=create` :

```
create table student (id integer not null auto_increment, email varchar(255),
  first_name varchar(255), last_name varchar(255), primary key (id)) engine=InnoDB
```

Trois ecarts avec le script de l'enonce : les chaines font 255 caracteres et non
45 (`@Column` sans `length` vaut 255), les colonnes sont dans l'ordre
alphabetique et non celui du script, et le moteur `InnoDB` est explicite. La
table fonctionne, mais ce n'est pas celle du script : le schema doit rester sous
controle d'un script ou d'une migration, pas de l'ORM.

### 2. `@NamedQuery`

`Student.findByLastName` est declaree sur l'entite et appelee par
`findByLastNameNamed`. Meme SQL, meme resultat que la requete en ligne de 5.4
(`theNamedQueryGivesTheSameResultAsTheInlineOne`).

La difference mesuree porte sur le **moment** ou une erreur est detectee. Une
faute dans la `@NamedQuery` (`lastNam`) empeche l'application de demarrer, meme
pour l'action `create`, qui ne s'en sert pas. La meme faute dans la requete en
ligne laisse `create` reussir, et n'echoue qu'a l'appel de `read`. La requete
nommee est verifiee au demarrage, la requete en ligne seulement quand on l'execute.

### 3. Couche service

`StudentService.enrollAll` enregistre un lot d'etudiants, **tout ou rien**. La
transaction s'ouvre dans le service, et chaque `save` du DAO la rejoint au lieu
d'ouvrir la sienne (propagation `REQUIRED`, le defaut).

Le meme lot, avec un dernier etudiant refuse (sans nom de famille) :

- par le service, aucune ligne n'est ecrite (`StudentServiceTest`) ;
- par le DAO seul, le premier etudiant reste en base, car chaque `save` a deja
  valide sa propre transaction.

Mesure sur MySQL avec l'action `enroll-batch` : l'`INSERT` du premier etudiant
part bien, puis il est annule avec tout le lot.

L'interet : une transaction doit couvrir une **operation metier** complete, et
seul le service sait ou elle commence et ou elle finit. Le DAO ne voit qu'une
ecriture a la fois.

Ecart assume : l'enonce propose de **deplacer** `@Transactional` du DAO vers le
service. Il est ici garde aux deux niveaux. Le DAO reste ainsi correct quand on
l'appelle directement, comme le fait le pilote des exercices, et le bareme note
« `@Transactional` au bon endroit » sur le DAO. Les deux niveaux ne s'opposent
pas : le DAO rejoint la transaction du service quand elle existe.

## Ecarts assumes

- **Base sur le port 3307, dans un conteneur.** Sur le poste de developpement,
  3306 est occupe par un autre projet. `application.properties` garde l'URL de
  l'enonce, sur 3306. Les scripts la surchargent par `SPRING_DATASOURCE_URL`.
  Une installation MySQL ordinaire n'a besoin d'aucun changement.
- **Tests sur H2, pas sur MySQL.** Les tests vident la table avant chaque cas :
  sur MySQL, un simple `./mvnw package` effacerait les donnees des captures. La
  fidelite a MySQL est verifiee a part, par `scripts/verifier-crud.sh`, en local
  et en integration continue.
- **Des tests**, au-dela du `contextLoads` genere : ils figent les reponses
  ci-dessus pour qu'elles se rejouent au lieu de se croire.
- **Actions supplementaires du pilote** (`create-many`, `update-emails`,
  `delete-all`, `find-named`, `enroll-batch`), a cote des quatre de l'enonce,
  pour que chaque methode du DAO soit appelable sans modifier le code.

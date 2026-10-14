# TP6 : Simuler un conflit de réservation concurrent et démontrer l'optimistic locking avec `@Version`

## 1. Objectif

Comprendre le verrouillage optimiste en JPA : l'annotation `@Version` permet à Hibernate de détecter que deux transactions ont modifié la même réservation. La deuxième transaction échoue avec une `OptimisticLockException` au lieu d'écraser la modification de la première. Le TP simule ce conflit et met en place une stratégie de retry.

## 2. Outils utilisés

- Java 11 (ou supérieur)
- Maven
- JPA 2.2 et Hibernate 5.6.15
- H2 (base en mémoire)
- JUnit 5
- IntelliJ IDEA

## 3. Structure du projet

```
reservation-locking
├── pom.xml
└── src
    ├── main
    │   ├── java/com/example
    │   │   ├── model
    │   │   │   └── Reservation
    │   │   ├── service
    │   │   │   ├── ReservationService
    │   │   │   └── ReservationServiceImpl
    │   │   ├── ConcurrentReservationSimulator
    │   │   ├── ReservationRetryHandler
    │   │   └── ConcurrentReservationWithRetry
    │   └── resources/META-INF
    │       └── persistence.xml
    └── test/java/com/example
        └── OptimisticLockingTest
```

<img width="338" height="440" alt="forma4" src="https://github.com/user-attachments/assets/6dcd5e29-73a3-47fa-a454-83934216fa01" />


## 4. Ce qu'on a fait

1. Création du projet Maven (`groupId` `com.example`, `artifactId` `reservation-locking`).
2. Ajout des dépendances : JPA, Hibernate, H2 (et JUnit 5 pour les tests).
3. Configuration de l'unité de persistance `reservationPU` dans `META-INF/persistence.xml` (H2 en mémoire, tables créées automatiquement, requêtes SQL affichées).
4. Création de l'entité `Reservation` avec un champ `@Version` :

```java
@Version
private int version;
```

5. Création du service `ReservationService` / `ReservationServiceImpl` (création, modification, recherche, liste).
6. Simulation d'un conflit concurrent avec `ConcurrentReservationSimulator`.
7. Exécution et analyse du conflit.
8. Gestion des conflits avec un retry (`ReservationRetryHandler`).
9. Test de la stratégie avec `ConcurrentReservationWithRetry` et des tests JUnit.

## 5. Simulation du conflit

Deux « utilisateurs » chargent la même réservation (version 0) :

| Étape | Action | Résultat |
|-------|--------|----------|
| 1 | Création de la réservation `Salle A` | version = 0 |
| 2 | Transaction 1 et transaction 2 lisent la réservation | les deux voient version = 0 |
| 3 | Transaction 1 passe la salle à `Salle B` | succès, version = 1 |
| 4 | Transaction 2 passe la salle à `Salle C` avec la version 0 | **conflit** : `OptimisticLockException` |

Lors de chaque mise à jour, Hibernate vérifie que la version en base est toujours celle qui a été lue. Pour la transaction 1, la version en base est bien 0 : la modification réussit et la version passe à 1. Pour la transaction 2, la version 0 n'existe plus en base : aucune ligne n'est modifiée, et Hibernate lève l'exception.

Au final, la modification de la transaction 1 n'est pas écrasée : la réservation reste en `Salle B`, version 1.

<img width="773" height="140" alt="forma1" src="https://github.com/user-attachments/assets/f556c817-d32b-466e-8450-cb192d1b61ca" />


## 6. Gestion du conflit avec retry

`ReservationRetryHandler.modifierAvecRetry` applique la modification. En cas de conflit, il recharge la dernière version depuis la base, réapplique la même modification et réessaie (3 tentatives au maximum). La transaction 2 réussit alors au deuxième essai : la salle devient `Salle C` et la version passe à 2.

<img width="733" height="117" alt="forma2" src="https://github.com/user-attachments/assets/8665072f-fb51-4fc6-8120-ffa7db8d0dc9" />


## 7. Tests JUnit

`OptimisticLockingTest` vérifie trois comportements :

- la version est incrémentée après une modification ;
- une version périmée provoque une `OptimisticLockException`, sans écraser la modification de la transaction 1 ;
- le retry réussit après un conflit.

<img width="505" height="219" alt="forma3" src="https://github.com/user-attachments/assets/c45f8c51-0901-4e9d-bf7b-d9849c31b5a9" />


## 8. Remarque sur le code du TP

Dans `modifierReservation`, l'énoncé fait un `em.find(...)` puis recopie les champs. Cette version ne détecte aucun conflit, car `find` charge la **dernière** version depuis la base et la version périmée de l'objet reçu n'est jamais comparée. Nous utilisons donc `em.merge(reservation)` : Hibernate compare la version de l'objet détaché à celle de la base et lève l'exception si elles diffèrent. De même, le retry de l'énoncé perdait la modification de l'utilisateur après le rechargement ; notre version prend la modification en paramètre et la réapplique sur l'objet rechargé.

## 9. Explication du verrouillage optimiste

- Le verrouillage optimiste suppose que les conflits sont rares : il ne bloque aucune ligne.
- Chaque mise à jour incrémente le champ `@Version`.
- Au moment de l'écriture, Hibernate vérifie que la version en base est celle qui a été lue. Sinon, une autre transaction est passée avant, aucune ligne n'est modifiée et une `OptimisticLockException` est levée.
- Avantages : bonne scalabilité, pas de blocage ni de deadlock.
- Limite : le conflit est détecté après coup, il faut donc le gérer (retry ou message à l'utilisateur).

## 10. Lancer le projet

1. Ouvrir le dossier dans IntelliJ (projet Maven) et recharger Maven.
2. Exécuter `ConcurrentReservationSimulator` : la transaction 2 échoue avec un conflit.
3. Exécuter `ConcurrentReservationWithRetry` : la transaction 2 réussit après un retry.
4. Lancer les tests : `mvn test`. Aucune base à installer, H2 fonctionne en mémoire.

## 11. Conclusion

L'annotation `@Version` suffit à protéger une réservation contre les modifications concurrentes : la deuxième transaction est refusée au lieu d'écraser la première. Une stratégie de retry permet de reprendre automatiquement la modification sur la dernière version des données.

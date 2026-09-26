---
sessionId: session-260926-174713-1gws
---

# Requirements

### Overview & Goals

L'objectif est d'harmoniser l'API Cinéma et de corriger les vulnérabilités et incohérences identifiées (tarification non
sécurisée, absence d'authentification JWT native, expiration de session non transmise par le backend, routes REST
redondantes et métadonnées films incomplètes). L'intégration Stripe Dev est explicitement exclue du périmètre pour faire
l'objet d'un plan dédié.

Cette démarche maximise l'utilité globale du système : elle protège l'intégrité financière en éliminant la falsification
des prix, sécurise l'identité utilisateur par JWT, garantit une synchronisation temporelle fiable du panier, et offre
une expérience fluide aux développeurs front-end et aux utilisateurs.

### Synthèse des Anomalies & Réponses Apportées

1. **Vulnérabilité de falsification de prix (`unitPrice` dans `BookingItemRequest`)** :
    - *Problème* : L'API accepte `unitPrice` fourni par le client lors de la création d'une réservation, permettant à un
      utilisateur malveillant d'injecter un montant arbitraire (ex. `0.01`).
    - *Correction* : Supprimer/ignorer le champ `unitPrice` de `BookingItemRequest` et charger obligatoirement le prix
      unitaire officiel depuis la séance (`Screening`) côté domaine backend.
2. **Absence d'authentification native JWT et gestion de session dans le flux** :
    - *Correction* : Mettre en place un service et un contrôleur d'authentification (`/api/v1/auth/login`,
      `/api/v1/auth/register`, `/api/v1/auth/logout`) délivrant un token JWT. Lors de la validation de l'étape
      d'authentification dans le tunnel de vente, le backend valide le JWT transmis :
        - Si le JWT est valide : le flux est validé et renvoie le lien vers la page de paiement avec le panier
          temporaire actif.
        - Si le JWT est absent ou invalide : le flux renvoie le lien de redirection vers la page de login (`/login`).
3. **Calcul et transmission de l'expiration de session dans `BookingFlowResponse`** :
    - *Correction* : Le backend calcule côté métier l'échéance exacte `now() + 15 minutes` lors du blocage des places et
      la transmet via le champ `expiresAt` dans `BookingFlowResponse`. Aucun calcul de délai n'est délégué au front-end.
4. **Informations manquantes sur le modèle `MovieResponse` & absence de `GET /api/v1/movies/{id}`** :
    - *Correction* : Ajout des champs `synopsis` et `trailerUrl` dans le modèle de domaine `Movie`, l'entité JPA
      `MovieEntity`, la migration Flyway et le DTO `MovieResponse`, ainsi que l'exposition de `GET /api/v1/movies/{id}`
      via `GetMovieByIdUseCase`.
5. **Redondance des routes utilisateurs pour la consultation des réservations** :
    - *Correction* : Suppression de la route redondante `/api/v1/bookings/user/{userId}` pour conserver la route
      canonique REST `/api/v1/users/{id}/bookings`.

### Scope

- **In Scope** :
    - Sécurisation absolue des prix : récupération obligatoire du `unitPrice` depuis `Screening` et nettoyage de
      `BookingItemRequest`.
    - Contrôleur d'authentification native (`AuthController`), émission et vérification des tokens JWT.
    - Mise à jour du flux de réservation (`BookingFlowController` / `BookingService`) pour vérifier le JWT et renvoyer
      les liens de redirection appropriés (`paymentPageUrl` vs `loginUrl`).
    - Calcul métier côté serveur de `expiresAt` (`now + 15 min`) et transmission dans `BookingFlowResponse`.
    - Enrichissement du catalogue de films (`synopsis`, `trailerUrl`), migration Flyway et route unitaire
      `GET /api/v1/movies/{id}`.
    - Suppression de la route dupliquée `GET /api/v1/bookings/user/{userId}`.
    - Tests unitaires, de sécurité et d'intégration Spring MVC (`MockMvc`).
- **Out of Scope** :
    - Intégration Stripe Dev (PaymentIntent, clientSecret, webhook) — reportée à un plan ultérieur dédié.
    - Système complexe d'autorisation RBAC multi-rôles au-delà du modèle utilisateur standard.

### User Stories

- **En tant qu'acheteur**, je veux m'authentifier avec mes identifiants pour obtenir un jeton JWT sécurisé et poursuivre
  mon achat de façon fluide.
- **En tant qu'acheteur non connecté dans le tunnel**, je veux être redirigé vers la page de connexion avec préservation
  de mon panier temporaire, puis vers le paiement une fois authentifié.
- **En tant qu'acheteur dans le panier**, je veux voir le compte à rebours précis des 15 minutes calculé de manière
  fiable par le serveur pour finaliser ma commande avant expiration.
- **En tant que gestionnaire du cinéma**, je veux que les tarifs des billets soient strictement fixés par le système
  pour empêcher toute tentative de manipulation des prix.
- **En tant que cinéphile**, je veux consulter la fiche complète d'un film avec synopsis et bande-annonce
  (`GET /api/v1/movies/{id}`).

### Functional Requirements

- **FR-1 : Intégrité tarifaire serveur** : La réservation directe et le tunnel de vente doivent obligatoirement dériver
  le tarif unitaire depuis la séance sélectionnée (`Screening.unitPrice`). Tout prix fourni par le client est ignoré.
- **FR-2 : Authentification JWT native** :
    - `POST /api/v1/auth/login` vérifie email et mot de passe, puis retourne `AuthResponse` contenant le token JWT, l'ID
      utilisateur, l'email et le nom.
    - `POST /api/v1/auth/register` permet l'inscription et génère un JWT valide.
- **FR-3 : Validation du JWT dans le tunnel de réservation** :
    - `POST /api/v1/booking-flow/{flowSessionId}/authenticate` accepte le token JWT.
    - Si le JWT est valide : valide la session de réservation pour l'utilisateur extrait du token, bloque les sièges
      avec expiration à 15 min et retourne `paymentPageUrl` (ex. `/checkout?session={flowSessionId}`).
    - Si le JWT est absent ou non valide : renvoie l'état `LOGIN_REQUIRED` avec `loginUrl` (ex.
      `/login?redirect=/booking/{flowSessionId}`).
- **FR-4 : Horodatage d'expiration serveur** : `BookingFlowResponse` inclut `expiresAt` calculé par le backend
  (`LocalDateTime.now().plusMinutes(15)`).
- **FR-5 : Fiche film enrichie** : `GET /api/v1/movies/{id}` et `GET /api/v1/movies` retournent `synopsis` et
  `trailerUrl`.
- **FR-6 : Nettoyage REST** : Seul `GET /api/v1/users/{id}/bookings` reste exposé pour consulter les réservations
  utilisateur.

# Technical Design

### Current Implementation

- `BookingItemRequest.java` : Expose un champ `BigDecimal unitPrice` directement pris en compte dans le domaine sans
  validation vis-à-vis de la séance.
- `BookingService.java` : Calcule le total en additionnant `item.subTotal()` fourni par la requête sans vérifier le prix
  enregistré dans la table `screenings`. Utilise une constante `DEFAULT_UNIT_PRICE` dans le tunnel de vente si non
  défini dans le contexte.
- `BookingFlowController.java` & `BookingFlowContext.java` : L'endpoint `/authenticate` attend un simple `userId` brut
  sans validation cryptographique de session ou de JWT.
- `BookingFlowResponse.java` : Ne dispose pas du champ `expiresAt`, forçant le front-end à estimer l'expiration.
- `BookingController.java` & `UserController.java` : Duplication entre `GET /api/v1/bookings/user/{userId}` et
  `GET /api/v1/users/{id}/bookings`.

### Key Decisions

1. **Immunisation contre la falsification de prix** :
    - *Choix* : Supprimer `unitPrice` de `BookingItemRequest` (ou le déprécier et l'ignorer totalement). Lors de la
      création d'une réservation, `BookingService` charge l'entité `Screening` via
      `ScreeningRepositoryOut.findById(screeningId)` et applique `screening.unitPrice()`.
    - *Rationale* : Zéro confiance envers les données de prix transmises par le client. Règle fondamentale de sécurité
      e-commerce.
2. **Authentification JWT et redirection dans le tunnel de vente** :
    - *Choix* : Utiliser un composant `JwtTokenProvider` (génération HMAC-SHA256, extraction du `userId` et `email`,
      validation de signature et d'expiration). Le contrôleur `BookingFlowController` transmet le token JWT au port
      applicatif. Le modèle de réponse `BookingFlowResponse` expose des attributs d'orientation : `paymentPageUrl` (si
      authentifié) et `loginUrl` (si connexion requise).
    - *Rationale* : Respecte l'architecture hexagonale et permet au front-end d'orienter immédiatement l'utilisateur
      vers la bonne vue sans logique métier locale.
3. **Calcul d'expiration exclusivement serveur** :
    - *Choix* : `BookingService` calcule `expiresAt = LocalDateTime.now().plusMinutes(15)` dès la transition vers le
      blocage des places et la création de la réservation PENDING. Ce timestamp est propagé dans
      `BookingFlowResponse.expiresAt`.
    - *Rationale* : Horloge faisant foi unique côté backend, éliminant tout décalage d'heure locale sur le terminal de
      l'utilisateur.
4. **Exclusion de Stripe Dev** :
    - *Choix* : Conservation du contrat existant de confirmation de paiement simple (`paymentToken` / simulation) sans
      introduire de complexité `PaymentIntent` ou `clientSecret` dans ce cycle de développement.
    - *Rationale* : Alignement strict sur la directive de reporter l'intégration Stripe à un lot distinct.

### Architecture Diagram

```mermaid
graph LR
    subgraph Client [Frontend Client]
        AuthView[Page Login / Register]
        TunnelView[Tunnel de Réservation]
        PayView[Page Paiement]
    end

    subgraph API [Web Adapters]
        AuthCtrl[AuthController]
        FlowCtrl[BookingFlowController]
        MovieCtrl[MovieController]
        UserCtrl[UserController]
    end

    subgraph Domain [Domain Services & Ports]
        AuthServ[AuthService / JwtProvider]
        BookServ[BookingService]
        MovieServ[MovieService]
    end

    subgraph Infrastructure [Persistence & External]
        ScreeningRepo[(Screening Repository)]
        BookingRepo[(Booking Repository)]
        MovieRepo[(Movie Repository)]
    end

    AuthView -->|POST /auth/login| AuthCtrl
    AuthCtrl --> AuthServ
    AuthServ -->|Issue JWT Token| Client
    TunnelView -->|POST /booking - flow/ . . . /authenticate (JWT )|FlowCtrl
FlowCtrl --> AuthServ
FlowCtrl --> BookServ

BookServ -->|Fetch Official unitPrice|ScreeningRepo
BookServ -->|Save Pending with expiresAt|BookingRepo

FlowCtrl -->|Valid JWT -> paymentPageUrl|PayView
FlowCtrl -->|Invalid / Missing JWT - > loginUrl|AuthView

UserCtrl -->|GET /users/ { id } /bookings|BookServ
MovieCtrl -->|GET /movies/ { id }| MovieServ
MovieServ --> MovieRepo
```

### Data Models / Contracts

#### 1. DTOs d'Authentification & JWT

```java
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}

public record RegisterRequest(
        @NotBlank String email,
        @NotBlank String fullName,
        @NotBlank String password
) {
}

public record AuthResponse(
        Long userId,
        String email,
        String fullName,
        String token
) {
}
```

#### 2. DTOs du Flux de Réservation & Sécurité Tarifaire

```java
public record BookingItemRequest(
        Long screeningId,
        int seatCount
        // unitPrice supprimé pour empêcher la manipulation client
) {
}

public record BookingFlowStepRequest(
        Long movieId,
        Long screeningId,
        Integer seatCount,
        String token,
        String paymentToken
) {
}

public record BookingFlowResponse(
        String flowSessionId,
        BookingFlowStateName state,
        Long selectedMovieId,
        Long selectedScreeningId,
        Integer selectedSeatCount,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        Long authenticatedUserId,
        Long bookingId,
        LocalDateTime expiresAt,
        String paymentPageUrl,
        String loginUrl,
        String qrCodeUrl
) {
}
```

#### 3. DTO Film Enrichi

```java
public record MovieResponse(
        Long id,
        String title,
        String description,
        String synopsis,
        int durationMinutes,
        String posterUrl,
        String trailerUrl
) {
}
```

### Components & Files Affected

- `../src/main/resources/db/migration/V3__add_movie_synopsis_and_trailer.sql` *(Nouveau)* : Ajout des colonnes
  `synopsis` et `trailer_url`.
- `../src/main/java/com/cinema/cinema/domain/model/Movie.java` *(Modifié)* : Ajout de `synopsis` et `trailerUrl`.
- `../src/main/java/com/cinema/cinema/infrastructure/adapter/in/web/dto/BookingItemRequest.java` *(Modifié)* : Retrait
  du champ `unitPrice`.
- `../src/main/java/com/cinema/cinema/domain/service/BookingService.java` *(Modifié)* :
    - Résolution du prix unitaire sécurisé depuis `Screening` lors de `createBooking` et dans le flux de réservation.
    - Calcul explicite de `expiresAt = now().plusMinutes(15)`.
    - Intégration de la validation de jeton JWT pour authentifier la session.
- `../src/main/java/com/cinema/cinema/domain/service/AuthService.java` / `JwtTokenProvider.java` *(Nouveau)* : Gestion
  du cycle de vie JWT (génération, parsing, validation).
- `../src/main/java/com/cinema/cinema/infrastructure/adapter/in/web/AuthController.java` *(Nouveau)* : Endpoints
  `/api/v1/auth/login`, `/register`, `/logout`.
- `../src/main/java/com/cinema/cinema/infrastructure/adapter/in/web/BookingFlowController.java` *(Modifié)* : Gestion de
  l'authentification par JWT, génération des URLs `paymentPageUrl` et `loginUrl`.
- `../src/main/java/com/cinema/cinema/infrastructure/adapter/in/web/dto/BookingFlowResponse.java` *(Modifié)* : Ajout de
  `expiresAt`, `totalAmount`, `paymentPageUrl`, `loginUrl`.
- `../src/main/java/com/cinema/cinema/infrastructure/adapter/in/web/BookingController.java` *(Modifié)* : Suppression de
  la route `GET /user/{userId}`.
- `../src/main/java/com/cinema/cinema/infrastructure/adapter/in/web/MovieController.java` *(Modifié)* : Ajout de
  l'endpoint `GET /api/v1/movies/{id}`.

# Testing

### Validation Approach

La validation s'appuiera sur des tests unitaires de domaine, des tests de sécurité contre la falsification de prix et
des tests d'intégration Spring MVC (`MockMvc`).

### Key Scenarios

1. **Sécurité Tarifaire & Intégrité** :
    - Tentative de réservation avec payload altéré : vérification que le montant facturé correspond à
      `seatCount * screening.unitPrice` et non à la valeur injectée.
2. **Authentification JWT & Redirections** :
    - `POST /api/v1/auth/login` avec identifiants valides -> 200 OK avec JWT.
    - `POST /api/v1/booking-flow/{flowSessionId}/authenticate` avec JWT valide -> passage à l'état `AUTHENTICATED` /
      `PAYMENT`, présence de `paymentPageUrl: "/checkout?session=..."` et de `expiresAt`.
    - `POST /api/v1/booking-flow/{flowSessionId}/authenticate` sans JWT ou avec JWT expiré/altéré -> retour de l'état
      `LOGIN_REQUIRED` avec `loginUrl: "/login?redirect=..."`.
3. **Calcul d'expiration Serveur** :
    - Validation que `BookingFlowResponse.expiresAt()` est calculé à `now() + 15 min` dès la réservation PENDING.
4. **Catalogue de films enrichi** :
    - `GET /api/v1/movies/{id}` -> 200 OK avec `synopsis` et `trailerUrl`.
    - `GET /api/v1/movies/999` -> 404 Not Found.
5. **Suppression de route redondante** :
    - `GET /api/v1/users/{id}/bookings` -> 200 OK avec la liste des réservations.
    - `GET /api/v1/bookings/user/{id}` -> 404 Not Found (route éliminée).

### Test Changes

- `../src/test/java/com/cinema/cinema/domain/BookingPriceSecurityTest.java` *(Nouveau)*
- `../src/test/java/com/cinema/cinema/infrastructure/adapter/in/web/AuthControllerTest.java` *(Nouveau)*
- `../src/test/java/com/cinema/cinema/infrastructure/adapter/in/web/BookingFlowControllerTest.java` *(Modifié)*
- `../src/test/java/com/cinema/cinema/infrastructure/adapter/in/web/MovieControllerTest.java` *(Modifié)*
- `../src/test/java/com/cinema/cinema/infrastructure/adapter/in/web/BookingControllerTest.java` *(Modifié)*

# Delivery Steps

### ✓ Step 1: Sécurisation du calcul des prix et élimination de la falsification tarifaire

Supprimer la dépendance au prix unitaire envoyé par le client et garantir le calcul serveur depuis les données de
séance.

- Modifier `BookingItemRequest` pour supprimer ou neutraliser le champ `unitPrice`.
- Mettre à jour `BookingService.createBooking` et le flux de réservation pour récupérer obligatoirement le tarif
  unitaire officiel depuis `ScreeningRepositoryOut` / `Screening.unitPrice()`.
- Mettre à jour `BookingFlowContext` pour stocker et appliquer le tarif réel de la séance sélectionnée au lieu d'un
  fallback arbitraire.
- Ajouter des tests unitaires et de sécurité vérifiant qu'aucun prix falsifié ne peut être enregistré.

### ✓ Step 2: Implémentation de l'authentification native JWT

Fournir le service et les contrôleurs d'authentification pour délivrer et valider les jetons JWT.

- Développer le composant de gestion de tokens JWT (`JwtTokenProvider`) pour signer et valider les jetons contenant les
  informations utilisateur.
- Créer le service applicatif `AuthService` avec vérification des identifiants et hachage/sécurité des mots de passe.
- Implémenter `AuthController` avec les endpoints `POST /api/v1/auth/login`, `POST /api/v1/auth/register` et
  `POST /api/v1/auth/logout`.
- Créer les DTOs `LoginRequest`, `RegisterRequest` et `AuthResponse`.
- Ajouter les tests unitaires et d'intégration Spring MVC (`AuthControllerTest`).

### ✓ Step 3: Validation JWT, redirection dynamique et calcul d'expiration dans le tunnel de réservation

Intégrer la vérification du token JWT dans le flux de réservation avec calcul serveur du délai d'expiration de 15
minutes.

- Modifier l'étape d'authentification de `BookingFlowController` (`/authenticate`) pour accepter le token JWT transmis
  par le client.
- Dans `BookingService`, vérifier le JWT : en cas de validité, associer l'utilisateur, créer la réservation temporaire
  avec `expiresAt = now().plusMinutes(15)` et renvoyer l'URL vers la page de paiement (`paymentPageUrl`).
- En cas de token manquant ou invalide, renvoyer l'état `LOGIN_REQUIRED` avec l'URL de redirection vers la page de
  connexion (`loginUrl`).
- Enrichir `BookingFlowResponse` avec `expiresAt`, `totalAmount`, `paymentPageUrl` et `loginUrl`.
- Mettre à jour `BookingFlowControllerTest` et `BookingServiceTest` pour couvrir les différents scénarios
  d'authentification et d'expiration.

### ✓ Step 4: Enrichissement des métadonnées des films et nettoyage des routes REST

Compléter la fiche film avec synopsis et bande-annonce et supprimer la duplication de routes utilisateur.

- Créer la migration Flyway `V3__add_movie_synopsis_and_trailer.sql` pour ajouter les colonnes `synopsis` et
  `trailer_url` à la table `movies`.
- Mettre à jour `Movie`, `MovieEntity` et `MovieResponse` pour intégrer `synopsis` et `trailerUrl`.
- Créer le use case `GetMovieByIdUseCase`, l'implémenter dans `MovieService` et exposer `GET /api/v1/movies/{id}` dans
  `MovieController`.
- Supprimer l'endpoint redondant `GET /api/v1/bookings/user/{userId}` de `BookingController` au profit de
  `GET /api/v1/users/{id}/bookings`.
- Mettre à jour les tests associés (`MovieControllerTest`, `BookingControllerTest`, `UserControllerTest`).
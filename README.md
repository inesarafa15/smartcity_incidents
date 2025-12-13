# Plateforme de Gestion d'Incidents - Ville Intelligente

Application web Spring Boot permettant aux citoyens de signaler des incidents urbains, aux agents municipaux de gérer ces incidents dans leur département, et aux administrateurs de superviser le système et générer des rapports.

## Architecture

Le projet suit une architecture en couches et feature-based :

```
com.smartcity.incident_management
├── config                  # Configuration Spring (Security, Web, Mail, WebSocket)
├── controllers             # Contrôleurs Spring MVC pour Thymeleaf
├── dto                     # Data Transfer Objects pour formulaires et réponses
├── entities                # Entités JPA conformes au diagramme UML
├── enums                   # Tous les enums (RoleType, StatutIncident, etc.)
├── exceptions              # Exceptions personnalisées
├── repository              # Spring Data JPA Repositories
├── security                # UserDetails, UserDetailsService, SecurityUtils
├── services.citoyen        # Services pour la logique métier citoyen
├── services.municipalite   # Services pour la logique métier agents municipaux
├── services.utilisateur    # Services communs, gestion utilisateurs, départements et rapports
├── validations             # Validations personnalisées
└── templates               # Templates Thymeleaf (auth, incidents, admin, dashboard)
```

## Technologies utilisées

- **Backend**: Spring Boot 4.0.0
- **Base de données**: MySQL
- **Sécurité**: Spring Security avec BCrypt
- **Vues**: Thymeleaf
- **Build**: Maven

## Entités principales

1. **Utilisateur**: Citoyens, agents municipaux, administrateurs
2. **Departement**: Départements municipaux (Infrastructure, Propreté, Sécurité, etc.)
3. **Quartier**: Quartiers de la ville
4. **Incident**: Incidents signalés avec workflow (Signalé → Pris en charge → En résolution → Résolu → Clôturé)
5. **Photo**: Photos associées aux incidents
6. **Notification**: Notifications pour les utilisateurs
7. **Rapport**: Rapports générés par les administrateurs

## Fonctionnalités

### Pour les citoyens
- Inscription et authentification
- Signalement d'incidents avec photos et géolocalisation
- Consultation de l'historique de leurs incidents
- Réception de notifications sur l'état de leurs incidents

### Pour les agents municipaux
- Visualisation des incidents de leur département
- Prise en charge d'incidents
- Gestion du workflow des incidents (Pris en charge → En résolution → Résolu)
- Consultation des incidents assignés

### Pour les administrateurs
- Gestion des utilisateurs (création d'agents, activation/désactivation)
- Gestion des départements et quartiers
- Génération de rapports analytiques
- Supervision du système

## Configuration

### Base de données

Modifier `src/main/resources/application.properties` :

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/smartcity_incidents?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=VOTRE_MOT_DE_PASSE
```

### Upload de fichiers

Les photos sont stockées dans le dossier `uploads/` à la racine du projet. Ce dossier sera créé automatiquement lors du premier upload.

## Démarrage

1. Créer la base de données MySQL :
```sql
CREATE DATABASE smartcity_incidents;
```

2. Configurer les paramètres de connexion dans `application.properties`

3. Lancer l'application :
```bash
mvn spring-boot:run
```

4. Accéder à l'application : http://localhost:8080

## Rôles et permissions

- **CITOYEN**: Accès limité à ses propres incidents
- **AGENT_MUNICIPAL**: Gestion des incidents de son département uniquement
- **ADMINISTRATEUR**: Accès total au système
- **SUPER_ADMIN**: Accès total (réservé pour l'administration système)

## Workflow des incidents

1. **SIGNALE**: Incident créé par un citoyen
2. **PRIS_EN_CHARGE**: Un agent municipal a pris en charge l'incident
3. **EN_RESOLUTION**: Intervention en cours
4. **RESOLU**: Incident résolu, en attente de confirmation
5. **CLOTURE**: Incident clôturé après confirmation du citoyen

## Sécurité

- Authentification basée sur Spring Security
- Mots de passe hachés avec BCrypt
- Protection CSRF activée
- Validation des uploads (taille, type de fichier)
- Autorisations basées sur les rôles (@PreAuthorize)

## Tests

Les tests unitaires et d'intégration peuvent être ajoutés dans le package `src/test/java/com/smartcity/incident_management/`.

## Auteur

Développé dans le cadre du cours de Développement Web Avancé 3INLOG.



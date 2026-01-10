# Guide CI/CD - Incident Management

Ce document explique le fonctionnement du pipeline d'Intégration Continue (CI) et de Déploiement Continu (CD) mis en place pour le projet **Incident Management**.

## 🚀 Vue d'ensemble du Workflow

Le pipeline est géré par **GitHub Actions** et se déclenche automatiquement lors d'un `push` ou d'une `pull_request` sur les branches principales (`dev`, `main`).

### Fichier de configuration
Le workflow est défini dans : `.github/workflows/ci.yml`

### Étapes du Pipeline (CI)

1.  **Checkout Code** : Récupération du code source depuis le dépôt.
2.  **Set up JDK 17** : Installation de l'environnement Java 17 (Temurin).
3.  **Maven Build & Test** :
    *   Compilation du projet.
    *   Exécution des tests unitaires et d'intégration (`mvn verify`).
    *   **Arrêt immédiat** si un test échoue.
4.  **Upload Coverage Report** :
    *   Génération et sauvegarde du rapport de couverture de code (JaCoCo).
    *   Disponible dans les artefacts GitHub même en cas d'échec des tests.
5.  **Upload Application JAR** :
    *   Sauvegarde du fichier `.jar` compilé (uniquement si le build réussit).
6.  **Build Docker Image** :
    *   Construction de l'image Docker pour vérifier que le `Dockerfile` est valide.

## 🧪 Tests de Démonstration

Pour cette démonstration, 2 cas de tests spécifiques ont été utilisés pour démontrer le cycle "Fail then Success" :

### ❌ Scénario Échec (Fail)
Ces tests sont conçus pour casser le pipeline et prouver que la CI protège la branche principale.

1.  **`LogicFailTest`** : Test unitaire contenant une erreur de logique mathématique.
    *   *Scénario :* `50 + 49` est comparé à `100`.
    *   *Erreur attendue :* `expected: <100> but was: <99>`.
2.  **`CiDemoIntegrationTest`** : Test d'intégration Spring Boot.
    *   *Scénario :* Charge le contexte applicatif et lance une assertion `fail()` explicite.
    *   *But :* Démontrer qu'une erreur d'intégration bloque aussi la production de l'artefact JAR.

### ✅ Scénario Succès (Success)
Pour valider le pipeline, ces deux tests doivent être corrigés :

1.  **Correction `LogicFailTest`** : Remplacer `50 + 49` par `50 + 50`.
2.  **Correction `CiDemoIntegrationTest`** : Commenter ou supprimer la ligne `fail(...)`.

---

## 🛠️ Démonstration CI : Scénario Échec puis Succès

Nous allons simuler un cycle de développement où un bug est introduit, détecté par la CI, puis corrigé.

🔴 **Résultat attendu :**
*   Allez dans l'onglet **Actions** de GitHub.
*   Le workflow échoue à l'étape `Maven Build`.
*   L'artefact `jacoco-report` est généré.
*   L'artefact `incident-management-jar` **n'est pas** généré.

### Étape 3 : Corriger et Valider (Success)

1.  **Corrigez `LogicFailTest.java`** : Changez le calcul pour obtenir 100.
2.  **Corrigez `CiDemoIntegrationTest.java`** : Commentez la ligne `fail(...)`.
3.  Envoyez le correctif :

🟢 **Résultat attendu :**
*   Le workflow redémarre et passe au vert.
*   L'artefact `incident-management-jar` est disponible en téléchargement.

---

## 🐳 Commandes Docker

Le projet peut être exécuté localement via Docker, indépendamment de la CI.

### Lancer l'application
```powershell
docker-compose-dev up --build
```
L'application sera accessible sur `http://localhost:8080`.

### Arrêter l'application
```powershell
docker-compose down
```

## 📊 Analyser la Couverture de Code (Local)

Pour générer le rapport JaCoCo sans passer par GitHub :
```powershell
mvn clean verify
```
Le rapport HTML sera disponible dans : `target/site/jacoco/index.html`

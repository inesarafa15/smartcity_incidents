# Étape 1: Build de l'application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copier le pom.xml et télécharger les dépendances
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier le code source et builder
COPY src ./src
RUN mvn clean package -DskipTests

# Étape 2: Image finale pour exécuter l'application
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copier le JAR depuis l'étape de build
COPY --from=build /app/target/*.jar app.jar

# Créer le dossier uploads
RUN mkdir -p /app/uploads

# Exposer le port
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]
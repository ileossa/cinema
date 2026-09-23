# Étape 1 : Construction de l'application (Build Stage)
FROM eclipse-temurin:24-jdk-alpine AS builder

WORKDIR /app

# Copie des fichiers de configuration Maven pour optimiser le cache des couches Docker
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Donner les droits d'exécution au wrapper Maven
RUN chmod +x ./mvnw

# Téléchargement des dépendances hors-ligne pour exploiter le cache Docker
RUN ./mvnw dependency:go-offline -B

# Copie du code source et compilation du projet
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Étape 2 : Image d'exécution légère et sécurisée (Runtime Stage)
FROM eclipse-temurin:24-jre-alpine

WORKDIR /app

# Création d'un utilisateur non-root pour des raisons de sécurité
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copie du jar compilé depuis l'étape de build
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

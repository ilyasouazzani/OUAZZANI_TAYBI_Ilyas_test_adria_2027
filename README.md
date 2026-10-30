# Plateforme bancaire — ENSET / ADRIA

## Structure
- account-service : gestion des comptes (port prévu : 8081).
- transaction-service : gestion des transactions (8082).
- discovery-service : registre Eureka (8761).
- config-service : configuration centralisée (8888).
- gateway-service : point d'entrée (8080).
- ai-service : assistant Spring AI (8083).

## Prérequis
Java 17 et Maven 3.9.

## Configuration locale
Copier .env.example vers .env si nécessaire, puis renseigner OPENAI_API_KEY
uniquement dans .env. Le fichier .env est ignoré par Git.
Les application.properties chargent ce fichier depuis la racine du projet
ou depuis le dossier parent lors d'une exécution dans un module.
Une variable d'environnement OPENAI_API_KEY reste prioritaire.

## Vérification
Depuis la racine : mvn clean package

## État : étape 1
Le parent Maven centralise Java, Spring Boot, Spring Cloud et Spring AI.
Les six modules contiennent leur point d'entrée et leur configuration.
Les dépendances métier et techniques seront ajoutées aux étapes suivantes.
Les services ne proposent pas encore de serveur HTTP ni de fonctionnalités métier.

## Certificats Maven sous Windows
Si Maven signale « PKIX path building failed » alors que Windows reconnaît
le certificat, exécuter dans PowerShell :
```powershell
$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
mvn clean package
```
Ce réglage concerne le terminal courant et utilise les certificats Windows.

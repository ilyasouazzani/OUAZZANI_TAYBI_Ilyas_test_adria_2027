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

## Structure Maven
Le parent Maven centralise Java, Spring Boot, Spring Cloud et Spring AI.
Les six modules contiennent leur point d'entrée et leur configuration.

## État : étape 3
account-service et transaction-service proposent les API REST de création et
de consultation des comptes et transactions, ainsi que les virements.
Les bases H2 sont distinctes et en mémoire. OpenFeign utilise la découverte
Eureka ; ACCOUNT_SERVICE_URL permet une URL directe pour un test isolé.
Le Config Server fournit bank.transaction.max-amount=1000 et est requis au
démarrage des services métier.

Les contrôleurs délèguent à une couche service validée. Les contrôles portent
sur le montant, le plafond, l'existence et l'état du compte, la devise et le
solde disponible. Les virements modifient les deux soldes dans une transaction
locale d'account-service. Il n'y a pas de transaction distribuée entre services.

Les instructions de lancement, requêtes REST et captures à réaliser sont dans
[docs/etape-02-rapport.md](docs/etape-02-rapport.md).
Eureka et Config Server sont implémentés. Resilience4J protège les appels Feign
avec circuit breaker, retry des lectures et fallback HTTP 503.
Les instructions de démarrage et les captures de cette étape sont dans
[docs/etape-03-rapport.md](docs/etape-03-rapport.md).
Gateway, MCP, GraphQL, Kafka et l'agent restent à implémenter.

## Certificats Maven sous Windows
Si Maven signale « PKIX path building failed » alors que Windows reconnaît
le certificat, exécuter dans PowerShell :
```powershell
$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
mvn clean package
```
Ce réglage concerne le terminal courant et utilise les certificats Windows.

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
Copier .env.example vers .env si nécessaire, puis renseigner GEMINI_API_KEY
uniquement dans .env. Le fichier .env est ignoré par Git.
Les application.properties chargent ce fichier depuis la racine du projet
ou depuis le dossier parent lors d'une exécution dans un module.
Une variable d'environnement GEMINI_API_KEY reste prioritaire.

## Vérification
Depuis la racine : mvn clean package

## Structure Maven
Le parent Maven centralise Java, Spring Boot, Spring Cloud et Spring AI.
Les six modules contiennent leur point d'entrée et leur configuration.

## Périmètre prioritaire final : parties A à F
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
Les deux services exposent MCP sur /mcp avec le transport Streamable HTTP.
transaction-service publie après commit sur bank.transactions.created, avec
accountId comme clé ; son consumer journalise uniquement les messages.
Les instructions Kafka et MCP sont dans
[docs/etape-04-rapport.md](docs/etape-04-rapport.md).
ai-service découvre les outils des deux serveurs MCP et expose
POST /api/assistant/chat et GET /api/assistant/tools.
Pour un test LLM réel, configurer GEMINI_API_KEY et AI_CHAT_MODEL=google-genai dans
le .env privé, puis redémarrer ai-service. Sans modèle activé, le chat retourne
une erreur 503 explicite. Voir [docs/etape-05-rapport.md](docs/etape-05-rapport.md).
Gateway et les connecteurs GraphQL sont implémentés. GraphiQL est disponible
sur /graphiql dans chaque service métier. Gateway route REST, GraphQL et MCP.
Les schémas d'architecture et de classes sont dans docs/architecture.mmd et
docs/diagramme-classes.mmd. Le rapport PDF et les preuves finales sont dans docs/.

## Lancement A-F
Construire les JAR avant de lancer les applications : mvn clean package.
Lancer Kafka avec docker compose -f docker-compose.kafka.yml up -d, puis
exécuter chaque commande ci-dessous dans un terminal distinct à la racine :

```powershell
java -Xmx192m -jar discovery-service/target/discovery-service-0.0.1-SNAPSHOT.jar
java -Xmx192m -jar config-service/target/config-service-0.0.1-SNAPSHOT.jar
java -Xmx192m -jar account-service/target/account-service-0.0.1-SNAPSHOT.jar
java -Xmx192m -jar transaction-service/target/transaction-service-0.0.1-SNAPSHOT.jar
java -Xmx192m -jar ai-service/target/ai-service-0.0.1-SNAPSHOT.jar
java -Xmx192m -jar gateway-service/target/gateway-service-0.0.1-SNAPSHOT.jar
```

Attendre les messages de démarrage de l'infrastructure avant les services métier,
puis attendre l'enregistrement et la propagation du registre Eureka avant les
appels par Gateway. Le dossier docs contient les requêtes de démonstration.
Les données H2 sont réinitialisées au redémarrage. Les parties G, H et I restent
hors du périmètre prioritaire A-F demandé en fin de session.

## Certificats Maven sous Windows
Si Maven signale « PKIX path building failed » alors que Windows reconnaît
le certificat, exécuter dans PowerShell :
```powershell
$env:MAVEN_OPTS='-Djavax.net.ssl.trustStoreType=Windows-ROOT -Djavax.net.ssl.trustStore=NONE'
mvn clean package
```
Ce réglage concerne le terminal courant et utilise les certificats Windows.

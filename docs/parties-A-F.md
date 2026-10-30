# Parties A à F — état et explications

## A — Préparation
Le parent Maven contient six modules. Les secrets restent dans .env, ignoré
par Git. Le dépôt fourni par le candidat est utilisé. Le code est versionné dans GitHub. Les démarches Google Sheet et Classroom
restent à la charge du candidat.

## B — Conception
Le diagramme de classes est dans diagramme-classes.mmd et l'architecture dans
architecture.mmd. Chaque microservice possède sa propre base. Les relations
entre services utilisent des UUID et des appels réseau, sans jointure JPA.

## C — Services fonctionnels
REST, GraphQL et MCP délèguent aux mêmes services. Le montant doit être positif
et respecter le plafond ; compte actif, devise et solde sont contrôlés. Les
virements débitent et créditent les comptes dans une transaction locale.
Le montant GraphQL est transmis comme chaîne décimale pour préserver sa précision.
Les montants et paramètres sont encore validés par la couche service.

## D — Infrastructure
Gateway route REST, GraphQL et MCP vers les noms Eureka. Config Server partage
bank.transaction.max-amount=1000. Eureka gère l'enregistrement et la découverte.
Les URL explicites des services sont utilisables pour un test isolé de Gateway.

## E — Agent
Spring AI découvre les outils MCP des deux services. À la demande du candidat,
Gemini remplace OpenAI ; ce choix s'écarte de la configuration initiale.
La clé GEMINI_API_KEY est uniquement dans .env et lue via application.properties.
Un modèle désactivé ou indisponible produit une erreur 503, jamais une réponse
de conversation inventée. Voir les preuves finales pour le statut réel du LLM.

## F — Kafka
L'événement minimal de sept champs est publié après commit JPA sur
bank.transactions.created, avec accountId comme clé. Le consumer journalise
sans modifier les données. Les tests couvrent un broker embarqué et une
exécution avec le broker Docker lancé par le candidat.

## Captures complémentaires à prendre
1. GraphiQL : http://localhost:8081/graphiql, requête accounts.
2. GraphiQL transactions : http://localhost:8082/graphiql, requête transactions.
3. Gateway : http://localhost:8080/api/accounts avec réponse des comptes.
4. Agent : GET /api/assistant/tools, puis POST /api/assistant/chat et réponse réelle.
5. Consumer Kafka : log Received TransactionCreated, avec clé et JSON visibles.
Les captures déjà reçues figurent dans docs/captures et seront intégrées au PDF.

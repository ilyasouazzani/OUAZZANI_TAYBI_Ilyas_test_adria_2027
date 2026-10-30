# Captures complémentaires pour le rapport A-F

Les neuf captures déjà reçues sont intégrées dans rapport-A-F.pdf.
Les services de démonstration sont laissés ouverts sur les ports ci-dessous.

## 1. Agent Gemini - priorité
Ouvrir http://localhost:18083/assistant-test.html?accountId=175968ad-f112-416c-9c47-aef217232eb4
Cliquer sur Envoyer. Capturer la question, HTTP 200 et la réponse 1250 EUR.
Nom : 05-02-agent.png. Ne montrer aucune clé API.

## 2. Outils MCP de l’agent
Sur la même page, cliquer sur Lister les outils MCP.
Capturer HTTP 200, le nombre 7 et des outils des deux services.
Nom : 05-01-tools.png.

## 3. GraphQL comptes
Ouvrir http://localhost:18081/graphiql
Saisir puis exécuter :
```graphql
{ accounts { id customerReference status currency balance } }
```
Capturer la requête et la réponse. Nom : C-graphql-comptes.png.

## 4. GraphQL transactions
Ouvrir http://localhost:18082/graphiql
Saisir puis exécuter :
```graphql
{ transactions { id accountId amount currency createdAt } }
```
La session actuelle dédiée à l’agent peut avoir un historique vide : la capture
prouve alors seulement la consultation. Les tests antérieurs ont validé une
mutation et un débit. Nom : C-graphql-transactions.png.

## 5. Gateway
Ouvrir http://localhost:18080/api/accounts et activer l’impression élégante.
Capturer l’URL et le compte AGENT-DEMO avec son solde de 1250 EUR.
Nom : D-gateway.png.

## 6. Kafka
Le journal réel du dernier test complet est conservé dans
verification-finale-kafka.txt, avec topic, clé accountId et JSON de l’événement.
Capturer la ligne Received TransactionCreated. Nom : F-consumer-kafka.png.
Le broker doit être actif pour produire une nouvelle démonstration ; ne pas
présenter une réponse enregistrée comme un nouveau message reçu en direct.

Envoyer les captures pour leur ajout au PDF. Aucune capture manquante n’a été fabriquée.

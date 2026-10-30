# Étape 5 — Agent Spring AI connecté aux outils MCP

## Réponse pour le rapport

ai-service expose POST /api/assistant/chat. Un ChatClient Spring AI utilise
un modèle Gemini et les ToolCallbacks découverts sur les serveurs MCP
d'account-service et de transaction-service. Les connexions utilisent le
transport Streamable HTTP. Aucun accès direct aux repositories métier ni
duplication des règles bancaires n'est ajouté dans l'assistant.

Le prompt système demande de consulter les outils pour obtenir les données,
de ne pas inventer d'identifiants ou de soldes et de demander les paramètres
manquants. Les débits et virements doivent correspondre à une demande explicite
de l'utilisateur. Une erreur ne doit pas conduire à répéter une écriture ou à
annoncer un succès sans résultat confirmé par l'outil.

GET /api/assistant/tools permet de vérifier les définitions réellement
découvertes par le client MCP. Le provider est également transmis au ChatClient
afin que le modèle puisse demander l'exécution de ces opérations.

## Configuration de la clé

La clé est lue depuis GEMINI_API_KEY via application.properties. Le fichier
.env reste exclu de Git et .env.example contient une clé vide. Pour activer le
modèle pendant le test local, renseigner le fichier .env privé :

```properties
GEMINI_API_KEY=valeur_privee_a_ne_pas_partager
AI_CHAT_MODEL=google-genai
GEMINI_MODEL=gemini-flash-lite-latest
```

Ne pas copier une vraie clé dans le rapport, une capture, GitHub ou le chat.
GEMINI_MODEL est modifiable si un autre modèle compatible est disponible sur
le compte. Le coût et l'accès au modèle dépendent du compte Google AI Studio utilisé.

Par défaut AI_CHAT_MODEL vaut none : la découverte MCP reste possible, mais
l'endpoint de conversation renvoie HTTP 503 avec LLM disabled. Ce comportement
permet de vérifier la connectivité sans inventer une réponse de LLM lorsque
la clé n'est pas renseignée. Après modification du .env, redémarrer ai-service.

## Démarrage

Lancer Eureka, Config Server, Kafka et les deux services métier, puis :

```powershell
java -jar ai-service/target/ai-service-0.0.1-SNAPSHOT.jar
```

Les URL MCP peuvent être adaptées par ACCOUNT_MCP_URL et TRANSACTION_MCP_URL.
Leurs valeurs par défaut sont http://localhost:8081 et http://localhost:8082.

## Test REST

```http
GET http://localhost:8083/api/assistant/tools
```

```http
POST http://localhost:8083/api/assistant/chat
Content-Type: application/json

{"message":"Consulte le solde du compte UUID_DU_COMPTE et indique sa devise."}
```

Pour démontrer une écriture, utiliser uniquement un compte de test, indiquer
explicitement le compte, le montant, la devise et les autres paramètres requis,
puis comparer la réponse avec l'API REST et les logs Kafka. La requête de chat
est autonome : l'historique conversationnel n'est pas conservé par cette étape.

## Captures à prendre manuellement

| Fichier | Écran | Éléments à montrer |
| --- | --- | --- |
| 05-01-tools.png | GET /api/assistant/tools | Outils des deux microservices réellement découverts, avec leurs noms et schémas |
| 05-02-agent.png | POST /api/assistant/chat avec modèle activé | Question sur un compte de test et réponse réelle du modèle |
| 05-03-verification.png | GET du solde par l'API REST métier | Correspondance avec la réponse de l'assistant |
| 05-04-tests.png | Résultat Maven des tests ai-service | Validation d'entrée, erreurs explicites et récupération des définitions d'outils |

Si le modèle n'est pas activé, une capture HTTP 503 illustre uniquement le
traitement de configuration manquante. Elle ne remplace pas une conversation
réelle et ne doit pas être présentée comme une validation du LLM.

## Explication courte avec mes mots

Le LLM choisit les outils utiles, mais les services métier exécutent réellement
les opérations et gardent leurs validations. L'assistant ne calcule pas un solde
à partir de suppositions. La découverte des outils peut être testée sans clé ;
la génération de réponses exige une clé et un modèle accessibles.

## Référence technique

[Client MCP Spring AI et intégration ToolCallbackProvider](https://docs.spring.io/spring-ai/reference/1.1/api/mcp/mcp-client-boot-starter-docs.html)

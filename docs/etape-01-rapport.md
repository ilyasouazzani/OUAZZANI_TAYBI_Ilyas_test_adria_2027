# Étape 1 — Structure du projet

## Réponse pour le rapport
Le projet est organisé en un parent Maven et six modules : account-service,
transaction-service, discovery-service, config-service, gateway-service et
ai-service. Le parent centralise les versions des dépendances et permet de
construire tous les modules avec une seule commande. Chaque module possède
un point d'entrée Spring Boot et un fichier application.properties.

## Explication courte
Cette organisation sépare les responsabilités des services tout en gardant
une configuration de compilation commune. La clé OpenAI est lue depuis
OPENAI_API_KEY via application.properties. Le fichier .env reste local et
est exclu de Git ; .env.example documente la variable sans contenir de secret.

## Captures à prendre
1. Arborescence dans l'IDE : les six modules et le pom.xml parent visibles.
2. pom.xml parent : modules, Java 17 et versions centralisées visibles.
3. Terminal après mvn clean package : Reactor Summary avec les sept lignes
   SUCCESS et BUILD SUCCESS.
4. Terminal après git check-ignore .env : la sortie .env visible.
   Montrer aussi .env.example avec OPENAI_API_KEY vide.
   Ne jamais capturer une vraie clé.

## Limite à ce stade
Seul le squelette est réalisé. Eureka, les endpoints et les autres fonctions
ne sont pas encore implémentés. La compilation ne prouve pas leur fonctionnement.

## Vérification réalisée
La commande Maven clean package a terminé avec BUILD SUCCESS pour le parent
et les six modules. Les six JAR exécutables ont été générés.
Aucun test métier n'est présent à cette étape de structure.
Le résumé réel de compilation est conservé dans verification-etape-01.txt.
git check-ignore .env a confirmé que le fichier secret est exclu.

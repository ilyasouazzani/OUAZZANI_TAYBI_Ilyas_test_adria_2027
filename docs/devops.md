# Exécution locale
mvn -B verify
docker compose config --quiet
docker compose up -d --build

Dockerfiles : une image Java 17 par module. Les JAR doivent être compilés avant le build Docker.
Le démarrage utilise depends_on et restart:on-failure ; service_started ne garantit pas la disponibilité HTTP.
Le pipeline nécessite un runner bank-demo préconfiguré, Docker et accès au registre. Il est livré comme extrait schématique, sans déploiement exécuté.
Les bases H2 sont en mémoire : cette composition est une démonstration, les données disparaissent au redémarrage.
Rollback : remettre IMAGE_TAG au SHA précédent puis docker compose pull et docker compose up -d --no-build.

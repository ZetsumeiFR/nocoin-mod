# Commandes utiles - NOCOIN

## Build & Run
```bash
# Compiler le mod
./gradlew build

# Lancer le client Minecraft en mode développement
./gradlew runClient

# Lancer le serveur en mode développement
./gradlew runServer

# Générer les ressources
./gradlew runData

# Nettoyer et reconstruire
./gradlew clean build
```

## IDE Setup
```bash
# Pour IntelliJ IDEA
./gradlew genIntellijRuns

# Pour Eclipse
./gradlew genEclipseRuns

# Rafraîchir les dépendances
./gradlew --refresh-dependencies
```

## Fichiers de configuration
- `config/nocoin-common.toml` : Configuration principale
- `config/nocoin_shop.json` : Articles de la boutique
- `config/nocoin_gacha.json` : Configuration du gacha

## Raccourci clavier in-game
- **N** : Ouvrir le menu NOCOIN

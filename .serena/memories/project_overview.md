# NOCOIN - Projet Overview

## Purpose
**NOCOIN** est un mod Minecraft Forge (1.20.1) qui ajoute un système de monnaie virtuelle complet pour serveurs Minecraft.

## Fonctionnalités principales
- **Monnaie virtuelle NOCOIN** : Gagnée en tuant des mobs
- **Système de Gacha** : Tirage de récompenses avec 3 raretés (3★, 4★, 5★)
- **Boutique serveur** : Achat d'items contre des NOCOIN
- **Magasins joueurs** : Créer sa propre boutique pour vendre aux autres
- **Leaderboard** : Classement des joueurs les plus riches

## Tech Stack
- **Langage** : Java 17
- **Framework** : Minecraft Forge 47.4.10+ (Minecraft 1.20.1)
- **Build System** : Gradle avec ForgeGradle plugin
- **Mappings** : Official Mojang mappings

## Architecture
- `com.zetsumei.nocoin` : Package racine
- Sous-packages : `block`, `capability`, `client`, `command`, `entity`, `event`, `gacha`, `item`, `leaderboard`, `network`, `shop`

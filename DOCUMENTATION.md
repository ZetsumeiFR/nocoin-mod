# NOCOIN - Documentation Complète

## Table des matières
1. [Présentation](#présentation)
2. [Installation](#installation)
3. [Système de monnaie](#système-de-monnaie)
4. [Commandes](#commandes)
5. [Système de Gacha](#système-de-gacha)
6. [Boutique](#boutique)
7. [Magasin Joueur](#magasin-joueur)
8. [Leaderboard](#leaderboard)
9. [Blocs et Items](#blocs-et-items)
10. [Configuration](#configuration)
11. [Raccourcis clavier](#raccourcis-clavier)

---

## Présentation

**NOCOIN** est un mod Minecraft Forge pour la version **1.20.1** qui ajoute un système de monnaie virtuelle complet. Les joueurs gagnent des NOCOIN en tuant des mobs et peuvent les dépenser dans diverses boutiques, machines à gacha, et entre eux.

### Fonctionnalités principales
- **Monnaie virtuelle** : Les NOCOIN sont gagnés en éliminant des mobs
- **Système de Gacha** : Tirage de récompenses avec différentes raretés
- **Boutique serveur** : Achat d'items contre des NOCOIN
- **Magasins joueurs** : Créez votre propre boutique pour vendre aux autres joueurs
- **Leaderboard** : Classement des joueurs les plus riches
- **Persistance** : Les soldes sont sauvegardés même après la mort

---

## Installation

1. Installer **Minecraft Forge 47.4.10** ou supérieur pour Minecraft 1.20.1
2. Télécharger le fichier JAR du mod depuis `build/libs/`
3. Placer le fichier dans le dossier `mods` de votre installation Minecraft
4. Lancer le jeu

### Compilation depuis les sources
```bash
./gradlew build
```
Le JAR sera généré dans `build/libs/`.

---

## Système de monnaie

### Comment gagner des NOCOIN

Les NOCOIN sont obtenus en tuant des mobs. Le montant varie selon le type de créature :

| Type de mob | Récompense |
|-------------|------------|
| Mobs hostiles (zombies, squelettes, etc.) | 1-3 NOCOIN (aléatoire) |
| Mobs passifs (vaches, cochons, etc.) | 0 NOCOIN (par défaut) |
| Ender Dragon | 1000 NOCOIN |
| Wither | 500 NOCOIN |
| Warden | 200 NOCOIN |
| Elder Guardian | 100 NOCOIN |

> Les récompenses apparaissent dans la barre d'action avec le format `+X NOCOIN`.

### Persistance

- Les NOCOIN sont **conservés après la mort** du joueur
- Le solde est sauvegardé dans les données du joueur
- Synchronisation automatique client/serveur

---

## Commandes

### Commandes joueur

#### `/nocoin` ou `/nocoin balance`
Affiche votre solde actuel de NOCOIN.

#### `/nocoin pay <joueur> <montant>`
Transfère des NOCOIN à un autre joueur.
- `<joueur>` : Nom du joueur destinataire
- `<montant>` : Nombre de NOCOIN à envoyer (minimum 1)

**Exemple :**
```
/nocoin pay Steve 100
```

#### `/nocoin shop`
Ouvre l'interface graphique de la boutique.

#### `/nocoin shop list`
Affiche la liste des articles disponibles dans le chat avec leurs prix.

#### `/nocoin shop buy <id>`
Achète un article de la boutique par son ID.

**Exemple :**
```
/nocoin shop buy 0
```

### Commandes administrateur (niveau op 2)

#### `/nocoin balance <joueur>`
Consulte le solde d'un autre joueur.

#### `/nocoin add <joueur> <montant>`
Ajoute des NOCOIN au solde d'un joueur.

#### `/nocoin remove <joueur> <montant>`
Retire des NOCOIN du solde d'un joueur.

#### `/nocoin set <joueur> <montant>`
Définit le solde exact d'un joueur.

#### `/nocoin shop admin add <item_id> <prix> [nom]`
Ajoute un article à la boutique.
- `<item_id>` : ID Minecraft de l'item (ex: `minecraft:diamond`)
- `<prix>` : Prix en NOCOIN
- `[nom]` : Nom d'affichage personnalisé (optionnel)

**Exemples :**
```
/nocoin shop admin add minecraft:diamond 500
/nocoin shop admin add minecraft:netherite_ingot 2000 Lingot de Nétherite
```

#### `/nocoin shop admin remove <id>`
Supprime un article de la boutique par son ID.

#### `/nocoin shop admin modify <id> price <nouveau_prix>`
Modifie le prix d'un article existant.

#### `/nocoin shop admin modify <id> name <nouveau_nom>`
Modifie le nom d'affichage d'un article.

#### `/nocoin shop admin reload`
Recharge la boutique depuis le fichier de configuration.

#### `/nocoin shop admin clear`
Vide complètement la boutique.

---

## Système de Gacha

Le système de Gacha permet aux joueurs de tenter leur chance pour obtenir des **récompenses** de différentes raretés.

### Raretés

| Rareté | Symbole | Probabilité | Couleur |
|--------|---------|-------------|---------|
| 5 étoiles | ★★★★★ | 3% | Or |
| 4 étoiles | ★★★★ | 15% | Violet |
| 3 étoiles | ★★★ | 82% | Bleu |

### Comment jouer

1. **Obtenir une Clé Gacha** : Achetez-la auprès du **Vendeur Gacha** (PNJ)
2. **Utiliser la Machine à Gacha** : Interagissez avec le bloc **Machine à Gacha**
3. **Tirer** : Utilisez votre clé pour effectuer un tirage

> Les récompenses du gacha peuvent être configurées via les commandes administrateur.

### Commandes Gacha (admin - op niveau 2)

#### `/gacha`
Affiche l'aide des commandes gacha.

#### `/gacha list [rareté]`
Liste toutes les récompenses ou filtre par rareté.
- Raretés possibles : `three_star`, `four_star`, `five_star`

#### `/gacha add <item> <rareté> <nom>`
Ajoute une récompense au gacha.

**Exemple :**
```
/gacha add minecraft:diamond five_star Diamant Légendaire
```

#### `/gacha addweight <item> <rareté> <poids> <nom>`
Ajoute une récompense avec un poids spécifique.

#### `/gacha remove <item>`
Retire une récompense du gacha.

#### `/gacha setweight <item> <poids>`
Modifie le poids d'une récompense (influence la probabilité au sein de sa rareté).

#### `/gacha setrarity <item> <rareté>`
Change la rareté d'une récompense.

#### `/gacha rates`
Affiche les probabilités actuelles de chaque rareté.

#### `/gacha setrates <5★%> <4★%> <3★%>`
Modifie les probabilités de rareté (doit totaliser 100%).

**Exemple :**
```
/gacha setrates 5.0 15.0 80.0
```

#### `/gacha info <item>`
Affiche les détails d'une récompense (nom, rareté, poids, probabilité effective).

#### `/gacha reload`
Recharge la configuration depuis le fichier JSON.

#### `/gacha clear`
Vide toutes les récompenses du gacha.

---

## Boutique

La boutique serveur permet aux joueurs d'acheter des items avec leurs NOCOIN.

### Interface graphique

Appuyez sur **N** (par défaut) pour ouvrir le menu NOCOIN, puis cliquez sur "Boutique".

Alternativement, utilisez `/nocoin shop`.

### Fonctionnement

- Les articles sont définis par les administrateurs
- Chaque article a un ID unique, un prix et une quantité
- L'achat est instantané si vous avez assez de NOCOIN
- Les articles sont donnés dans l'inventaire (ou droppés si plein)

### Gestion (admin)

Les articles sont sauvegardés dans `config/nocoin_shop.json` et peuvent être gérés :
- Via les commandes `/nocoin shop admin`
- En modifiant directement le fichier JSON puis `/nocoin shop admin reload`

---

## Magasin Joueur

Le bloc **Magasin Joueur** permet à chaque joueur de créer sa propre boutique.

### Placement

1. Craftez ou obtenez un bloc **Player Shop**
2. Placez-le dans le monde
3. Vous devenez automatiquement le propriétaire

### Interface propriétaire

En tant que propriétaire, cliquez sur votre magasin pour :
- Définir le nom de votre boutique
- Ajouter des offres de vente
- Gérer les stocks
- Supprimer des offres

### Interface client

Les autres joueurs peuvent :
- Voir les offres disponibles
- Acheter des items avec leurs NOCOIN

### Caractéristiques

- **Lumière** : Le magasin émet une légère lumière (niveau 5)
- **Redstone** : Signal proportionnel au nombre d'offres actives
- **Sécurité** : Seul le propriétaire peut gérer les offres

---

## Leaderboard

Le bloc **Leaderboard** affiche le classement des joueurs les plus riches en NOCOIN.

### Fonctionnalités

- Affichage holographique 3D du top 10
- Mise à jour automatique toutes les 5 secondes
- Émission de lumière dorée (niveau 10)
- Clic pour ouvrir l'écran détaillé du classement

### Placement

Idéal pour être placé au spawn du serveur pour que tous les joueurs puissent voir le classement.

---

## Blocs et Items

### Items

| Item | ID | Description |
|------|-----|-------------|
| Clé Gacha | `nocoin:gacha_key` | Permet d'effectuer un tirage dans la machine à gacha |

### Blocs

| Bloc | ID | Description |
|------|-----|-------------|
| Machine à Gacha | `nocoin:gacha_machine` | Utilisez vos clés pour effectuer des tirages |
| Leaderboard | `nocoin:leaderboard` | Affiche le classement des joueurs |

### Entités

| Entité | Description |
|--------|-------------|
| Vendeur Gacha | PNJ qui vend des Clés Gacha contre des NOCOIN |

Le Vendeur Gacha est :
- **Invulnérable** (sauf en mode créatif)
- **Persistant** (ne disparaît jamais)
- **Statique** (ne se déplace pas)

---

## Configuration

Le fichier de configuration principal est `config/nocoin-common.toml`.

### Options disponibles

```toml
# Minimum de NOCOIN droppé par les mobs hostiles
defaultMonsterDropMin = 1

# Maximum de NOCOIN droppé par les mobs hostiles
defaultMonsterDropMax = 3

# NOCOIN droppé par les mobs passifs
defaultPassiveDrops = 0

# Prix d'une Clé Gacha en NOCOIN
gachaKeyPrice = 50

# Drops personnalisés par mob (format: 'minecraft:entity_id=montant')
customMobDrops = [
    "minecraft:ender_dragon=1000",
    "minecraft:wither=500",
    "minecraft:elder_guardian=100",
    "minecraft:warden=200"
]

# Articles de la boutique (format: 'minecraft:item_id;prix;quantité;nom')
shopItems = []
```

### Fichiers de données

| Fichier | Description |
|---------|-------------|
| `config/nocoin-common.toml` | Configuration principale |
| `config/nocoin_shop.json` | Articles de la boutique (persisté) |
| `config/nocoin_gacha.json` | Configuration du gacha (récompenses et probabilités) |

---

## Raccourcis clavier

| Touche | Action |
|--------|--------|
| **N** | Ouvrir le menu NOCOIN (portefeuille) |

Le raccourci est configurable dans les options de contrôles de Minecraft sous la catégorie "NOCOIN".

---

## Résumé des flux

### Gagner des NOCOIN
```
Tuer un mob → Calcul de la récompense → +NOCOIN au joueur → Notification dans l'action bar
```

### Acheter dans la boutique
```
/nocoin shop → Sélectionner un article → Vérification du solde → -NOCOIN → +Item
```

### Système Gacha
```
Acheter une Clé (Vendeur) → Utiliser la Machine → Tirage rareté → Tirage item → Récompense
```

### Commerce entre joueurs
```
/nocoin pay <joueur> <montant> → Vérification solde → -NOCOIN émetteur → +NOCOIN destinataire
```

---

## Support technique

- **Version Minecraft** : 1.20.1
- **Version Forge** : 47.4.10+
- **Java** : 17

Pour signaler un bug ou proposer une fonctionnalité, créez une issue sur le dépôt du projet.

---

*Documentation générée pour NOCOIN - Mod de monnaie virtuelle pour Minecraft*

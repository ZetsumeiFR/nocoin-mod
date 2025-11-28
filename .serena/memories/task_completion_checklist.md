# Checklist de complétion de tâche - NOCOIN

## Avant de considérer une tâche terminée

### 1. Compilation
```bash
./gradlew build
```
Vérifier qu'il n'y a pas d'erreurs de compilation.

### 2. Tests en jeu
```bash
./gradlew runClient
```
Tester manuellement la fonctionnalité dans le jeu.

### 3. Vérifications
- [ ] Le code compile sans erreurs
- [ ] Les imports sont corrects (pas de `*` imports)
- [ ] Les textures/assets nécessaires sont présents
- [ ] Les traductions sont ajoutées dans `lang/fr_fr.json`
- [ ] Les packets réseau sont enregistrés si nécessaires
- [ ] Les registries sont mis à jour si nouveaux blocs/items

### 4. Fichiers à vérifier selon le type de changement

#### Nouveau Block
- `ModBlocks.java` : Enregistrement
- `blockstates/*.json` : Blockstate
- `models/block/*.json` : Modèle
- `models/item/*.json` : Modèle item
- `textures/block/*.png` : Texture

#### Nouveau Screen
- `client/screen/*.java` : Classe écran
- Si packets : `network/*.java` + enregistrement dans `NocoinNetworkHandler`
- Handler client dans `client/` si nécessaire

#### Nouveau Item
- `ModItems.java` : Enregistrement
- `models/item/*.json` : Modèle
- `textures/item/*.png` : Texture

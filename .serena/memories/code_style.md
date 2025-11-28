# Code Style - NOCOIN

## Java Conventions
- **Encoding** : UTF-8
- **Java Version** : 17
- **Naming** : camelCase pour méthodes/variables, PascalCase pour classes

## Forge/Minecraft Patterns
- **Registries** : Via `DeferredRegister` (voir `ModBlocks`, `ModItems`, `ModEntities`)
- **Networking** : SimpleChannel avec packets (`NocoinNetworkHandler`)
- **Capabilities** : Pattern AttachCapabilitiesEvent (voir `NocoinCapability`)
- **Screens** : Extension de `Screen` pour GUI clients

## Structure des fichiers
- `block/` : Classes de blocs et block entities
- `client/` : Code côté client (screens, renderers, handlers)
- `command/` : Commandes serveur
- `event/` : Event handlers Forge
- `gacha/` : Système de gacha (manager, rewards, rarity)
- `network/` : Packets réseau client/serveur
- `shop/` : Système de boutique

## Conventions documentaires
- Javadoc en français pour les classes principales
- Commentaires inline en français

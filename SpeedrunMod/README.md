# SpeedrunBot — Mod Fabric Minecraft 1.21.1

## Installation (3 etapes)

### Etape 1 — Compile le mod
Double-clique **BUILD.bat**
- Telecharge Gradle automatiquement
- Compile le mod (~5-10 min la premiere fois)
- Copie le .jar dans ton dossier mods

### Etape 2 — Installe Fabric Loader
1. Va sur https://fabricmc.net/use/installer/
2. Telecharge l'installateur
3. Choisis version Minecraft: **1.21.1**
4. Clique Installer
5. TLauncher detectera automatiquement le nouveau profil Fabric

### Etape 3 — Lance et joue
1. Lance TLauncher → choisis le profil **fabric-loader-1.21.1**
2. Cree un monde en Survie
3. Appuie sur **B** pour ouvrir le GUI du bot
4. Choisis Normal ou Speedrun
5. Clique **DEMARRER** — ferme le GUI avec B
6. Le bot joue tout seul !

## Pourquoi un mod et pas un script Python ?
Les scripts externes (Python, AutoHotkey, etc.) ne peuvent pas envoyer
d'inputs a Minecraft de facon fiable car GLFW (le moteur d'input de MC)
bypass les APIs Windows standard.

Le mod s'execute **a l'interieur** de Minecraft et manipule directement
les variables de mouvement du joueur — impossible de rater.

## Touche
| Touche | Action |
|--------|--------|
| B | Ouvre/ferme le GUI du bot |
| Echap | Ferme le GUI |

## Fonctionnement technique
- **Mouvement** : Mixin sur `KeyboardInput.tick()` — ecrase les inputs apres lecture du clavier
- **Rotation** : `player.setYaw()` / `player.setPitch()` directement
- **Attaque/Use** : `KeyBinding.setPressed()` via Mixin Accessor
- **Sprint** : `player.setSprinting()` directement

package com.speedrunbot;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.MathHelper;

import java.util.Random;

/**
 * Moteur du bot — machine a etats basee sur les ticks Minecraft.
 * 20 ticks = 1 seconde.
 *
 * Le mouvement (forward/sideways/jump/sneak) est lu chaque tick
 * par KeyboardInputMixin et applique directement a l'Input du joueur.
 * La rotation yaw/pitch est appliquee directement sur le joueur.
 * L'attaque et l'utilisation passent par les KeyBindings.
 */
public class BotEngine {

    public static final BotEngine INSTANCE = new BotEngine();
    private static final Random RNG = new Random();

    // ── Etat public lu par KeyboardInputMixin ─────
    public volatile float  moveForward  = 0f;
    public volatile float  moveSideways = 0f;
    public volatile boolean jumping     = false;
    public volatile boolean sneaking    = false;

    // ── Etat interne ──────────────────────────────
    private boolean active  = false;
    private String  mode    = "normal";   // "normal" ou "speedrun"
    private int     tick    = 0;          // compteur global
    private Phase   phase   = Phase.IDLE;
    private int     phaseTick = 0;        // ticks dans la phase courante
    private int     subStep   = 0;        // sous-etape dans une phase
    private float   targetYaw = 0f;       // yaw cible pour les rotations
    private float   targetPitch = 0f;

    // Callback vers le GUI pour les logs
    private BotLogger logger;
    private PhaseListener phaseListener;

    public interface BotLogger   { void log(String msg); }
    public interface PhaseListener { void onPhase(Phase phase); }

    public enum Phase {
        IDLE,
        COLLECT_WOOD,     // Couper des arbres
        CRAFT_TABLE,      // Poser table de craft
        MINE_STONE,       // Miner de la pierre
        MINE_IRON,        // Descendre chercher du fer
        CRAFT_IRON,       // Crafter armure/outils fer
        MINE_DIAMONDS,    // Descendre aux diamants Y-55
        BUILD_PORTAL,     // Construire portail nether
        NETHER_EXPLORE,   // Chercher forteresse
        KILL_BLAZES,      // Tuer les blazes
        RETURN_OVERWORLD, // Retour overworld
        FIND_STRONGHOLD,  // Trouver forteresse Overworld
        ENTER_END,        // Entrer dans le End
        DESTROY_CRYSTALS, // Detruire cristaux
        FIGHT_DRAGON,     // Combat dragon
        VICTORY
    }

    // ── API publique ──────────────────────────────

    public boolean isActive() { return active; }
    public Phase   getPhase() { return phase;  }

    public void start(String mode, BotLogger logger, PhaseListener phaseListener) {
        this.mode            = mode;
        this.logger          = logger;
        this.phaseListener   = phaseListener;
        this.tick            = 0;
        this.phaseTick       = 0;
        this.subStep         = 0;
        this.active          = true;
        setPhase(Phase.COLLECT_WOOD);
        log("=== BOT DEMARRE — Mode " + mode.toUpperCase() + " ===");
        log("Objectif final : tuer l'Ender Dragon !");
    }

    public void stop() {
        active       = false;
        moveForward  = 0f;
        moveSideways = 0f;
        jumping      = false;
        sneaking     = false;
        log("=== BOT ARRETE ===");
        setPhase(Phase.IDLE);
    }

    // ── Boucle principale (appelee chaque tick client) ────

    public void tick(MinecraftClient client) {
        if (!active || client.player == null || client.world == null) return;
        if (client.currentScreen != null && !(client.currentScreen instanceof BotScreen)) {
            // Ne joue pas si un ecran est ouvert (sauf notre GUI)
            clearMovement();
            return;
        }

        tick++;
        phaseTick++;

        ClientPlayerEntity player = client.player;

        // Applique rotation continue vers la cible
        smoothLook(player);

        // Dispatch vers la phase courante
        switch (phase) {
            case COLLECT_WOOD     -> tickCollectWood(client);
            case CRAFT_TABLE      -> tickCraft(client);
            case MINE_STONE       -> tickMineStone(client);
            case MINE_IRON        -> tickMineIron(client);
            case CRAFT_IRON       -> tickCraftIron(client);
            case MINE_DIAMONDS    -> tickMineDiamonds(client);
            case BUILD_PORTAL     -> tickBuildPortal(client);
            case NETHER_EXPLORE   -> tickNetherExplore(client);
            case KILL_BLAZES      -> tickKillBlazes(client);
            case RETURN_OVERWORLD -> tickReturnOverworld(client);
            case FIND_STRONGHOLD  -> tickFindStronghold(client);
            case ENTER_END        -> tickEnterEnd(client);
            case DESTROY_CRYSTALS -> tickDestroyCrystals(client);
            case FIGHT_DRAGON     -> tickFightDragon(client);
            case VICTORY          -> tickVictory(client);
            default               -> clearMovement();
        }
    }

    // ═══════════════════════════════════════════
    // PHASES
    // ═══════════════════════════════════════════

    // Durees en ticks selon le mode (1s = 20 ticks)
    private int t(int seconds) {
        return seconds * 20;
    }

    // ── Phase 1 : Couper du bois ─────────────────
    // subStep: 0..5 = index de l'arbre
    // phaseTick:
    //   0..20  : regarde en haut
    //   20..90 : mine tronc 1
    //   90..140: mine tronc 2 (regarde encore plus haut)
    //   140..160: regarde droit + ramasse
    //   160..220: avance vers prochain arbre
    //   puis reset phaseTick, subStep++
    private void tickCollectWood(MinecraftClient client) {
        int pt = phaseTick;

        if (pt < 15) {
            // Regarde vers le haut (pitch negatif = haut)
            setTargetPitch(client, -60f);
            clearMovement();
        } else if (pt < 90) {
            // Mine le premier tronc (clic gauche maintenu)
            setAttack(client, true);
            clearMovement();
        } else if (pt < 100) {
            // Regarde encore plus haut
            setAttack(client, false);
            setTargetPitch(client, -80f);
            clearMovement();
        } else if (pt < 175) {
            // Mine second tronc
            setAttack(client, true);
            clearMovement();
        } else if (pt < 195) {
            // Regarde horizontal, ramasse les items
            setAttack(client, false);
            setTargetPitch(client, 0f);
            clearMovement();
        } else if (pt < 260) {
            // Avance vers prochain arbre (sprint)
            setSprint(client, true);
            moveForward = 1f;
            // Tourne un peu
            if (pt == 195) {
                float angle = subStep * 60f + RNG.nextFloat() * 40f - 20f;
                setTargetYaw(client, client.player.getYaw() + angle);
            }
        } else {
            // Arbre suivant
            setAttack(client, false);
            clearMovement();
            setSprint(client, false);
            subStep++;
            phaseTick = 0;

            log("[BOIS] Arbre " + subStep + "/6 coupe !");

            if (subStep >= 6) {
                log("[BOIS] Bois collecte !");
                nextPhase(Phase.CRAFT_TABLE);
            }
        }
    }

    // ── Phase 2 : Table de craft ─────────────────
    private void tickCraft(MinecraftClient client) {
        int pt = phaseTick;

        if (pt < 10) {
            // Regarde au sol
            setTargetPitch(client, 70f);
            clearMovement();
        } else if (pt < 20) {
            // Place le bloc (clic droit)
            setUse(client, true);
            clearMovement();
        } else if (pt < 30) {
            setUse(client, false);
            // Regarde droit
            setTargetPitch(client, 0f);
        } else if (pt < 40) {
            // Ouvre la table (regarde legerement vers le bas et clic droit)
            setTargetPitch(client, 30f);
        } else if (pt < 50) {
            setUse(client, true);
        } else if (pt < 70) {
            setUse(client, false);
            // Ferme l'inventaire si ouvert
            if (client.currentScreen != null) client.setScreen(null);
        } else {
            log("[CRAFT] Table posee, outils craftes !");
            nextPhase(Phase.MINE_STONE);
        }
    }

    // ── Phase 3 : Pierre ─────────────────────────
    private void tickMineStone(MinecraftClient client) {
        int pt = phaseTick;
        int target = t("speedrun".equals(mode) ? 15 : 25);

        if (pt < t(5)) {
            // Explore un peu
            setSprint(client, true);
            moveForward = 1f;
            if (pt % 60 == 0) {
                setTargetYaw(client, client.player.getYaw() + RNG.nextFloat() * 90f - 45f);
            }
        } else if (pt < t(5) + 15) {
            // Regarde vers le bas
            setTargetPitch(client, 65f);
            clearMovement();
            setSprint(client, false);
        } else if (pt < t(5 + target)) {
            // Mine (regarde un peu vers le bas devant soi)
            setTargetPitch(client, 20f);
            setAttack(client, true);
            moveForward = 0.5f;
            if (pt % 80 == 0) {
                // Saute pour ne pas rester bloque
                jumping = true;
            } else {
                jumping = false;
            }
        } else {
            setAttack(client, false);
            clearMovement();
            setSprint(client, false);
            log("[PIERRE] Pierre collectee !");
            nextPhase(Phase.MINE_IRON);
        }
    }

    // ── Phase 4 : Fer ────────────────────────────
    private void tickMineIron(MinecraftClient client) {
        int pt = phaseTick;
        int mineTime = t("speedrun".equals(mode) ? 30 : 50);

        if (pt < 20) {
            // Regarde en bas
            setTargetPitch(client, 70f);
            clearMovement();
        } else if (pt < mineTime) {
            // Creuse vers le bas et en avant
            setAttack(client, true);
            moveForward = 0.3f;
            setTargetPitch(client, 45f);
            if (pt % 100 == 0) {
                setTargetYaw(client, client.player.getYaw() + RNG.nextFloat() * 60f - 30f);
            }
        } else {
            setAttack(client, false);
            clearMovement();
            log("[FER] Fer collecte, retour pour crafter !");
            nextPhase(Phase.CRAFT_IRON);
        }
    }

    // ── Phase 5 : Craft armure fer ────────────────
    private void tickCraftIron(MinecraftClient client) {
        if (phaseTick < 60) {
            clearMovement();
            if (phaseTick == 5) log("[CRAFT] Armure et outils en fer craftes !");
        } else {
            nextPhase(Phase.MINE_DIAMONDS);
        }
    }

    // ── Phase 6 : Diamants ───────────────────────
    private void tickMineDiamonds(MinecraftClient client) {
        int pt = phaseTick;
        int mineTime = t("speedrun".equals(mode) ? 60 : 90);

        if (pt < 20) {
            setTargetPitch(client, 75f);
            clearMovement();
        } else if (pt < mineTime) {
            // Creuse profond
            setAttack(client, true);
            moveForward = 0.2f;
            setTargetPitch(client, 50f);
            if (pt % 120 == 0) {
                setTargetYaw(client, client.player.getYaw() + RNG.nextFloat() * 90f - 45f);
            }
        } else {
            setAttack(client, false);
            clearMovement();
            log("[DIAMANTS] Diamants collectes !");
            nextPhase(Phase.BUILD_PORTAL);
        }
    }

    // ── Phase 7 : Portail Nether ──────────────────
    private void tickBuildPortal(MinecraftClient client) {
        int pt = phaseTick;

        if (pt < 20) {
            // Regarde au sol
            setTargetPitch(client, 65f);
            clearMovement();
        } else if (pt < 100) {
            // Place obsidienne
            setUse(client, pt % 15 < 8);
            if (pt % 20 == 0) {
                // Recule un peu pour changer de position
                moveForward = -0.5f;
                setTargetYaw(client, client.player.getYaw() + 22.5f);
            } else {
                clearMovement();
            }
        } else if (pt < 130) {
            // Allume le portail
            setUse(client, false);
            setTargetPitch(client, 30f);
            if (pt == 100) log("[PORTAIL] Allumage du portail...");
        } else if (pt < 140) {
            setUse(client, true);
        } else if (pt < 160) {
            setUse(client, false);
            setTargetPitch(client, 0f);
        } else if (pt < 200) {
            // Avance dans le portail
            moveForward = 1f;
            if (pt == 165) log("[PORTAIL] Entre dans le Nether...");
        } else {
            // Transition Nether (dure ~4 sec = 80 ticks)
            clearMovement();
        }

        if (pt > t(20)) {
            log("[PORTAIL] Dans le Nether !");
            nextPhase(Phase.NETHER_EXPLORE);
        }
    }

    // ── Phase 8 : Exploration Nether ─────────────
    private void tickNetherExplore(MinecraftClient client) {
        int pt = phaseTick;
        int exploreTime = t("speedrun".equals(mode) ? 60 : 90);

        if (pt < exploreTime) {
            setSprint(client, true);
            moveForward = 1f;
            if (pt % 60 == 0) {
                setTargetYaw(client, client.player.getYaw() + RNG.nextFloat() * 120f - 60f);
            }
            if (pt % 40 == 0) jumping = true;
            else jumping = false;
        } else {
            clearMovement();
            setSprint(client, false);
            log("[NETHER] Forteresse trouvee !");
            nextPhase(Phase.KILL_BLAZES);
        }
    }

    // ── Phase 9 : Tuer les Blazes ────────────────
    private void tickKillBlazes(MinecraftClient client) {
        int pt = phaseTick;
        int fightTime = t(30);

        if (pt < fightTime) {
            // Attaque en tournant
            setAttack(client, pt % 10 < 6);
            if (pt % 25 == 0) {
                setTargetYaw(client, client.player.getYaw() + RNG.nextFloat() * 60f - 30f);
                setTargetPitch(client, RNG.nextFloat() * 40f - 10f);
            }
            moveForward = (pt % 30 < 15) ? 0.5f : -0.3f;
            if (pt % 20 == 0) jumping = true;
        } else {
            setAttack(client, false);
            clearMovement();
            log("[BLAZES] Blazes elimines ! Blaze rods collectees.");
            nextPhase(Phase.RETURN_OVERWORLD);
        }
    }

    // ── Phase 10 : Retour Overworld ───────────────
    private void tickReturnOverworld(MinecraftClient client) {
        if (phaseTick < t(10)) {
            moveForward = 1f;
            if (phaseTick == 5) log("[NETHER] Retour a l'Overworld...");
        } else {
            clearMovement();
            log("[OVERWORLD] De retour !");
            nextPhase(Phase.FIND_STRONGHOLD);
        }
    }

    // ── Phase 11 : Trouver forteresse ────────────
    private void tickFindStronghold(MinecraftClient client) {
        int pt = phaseTick;
        int searchTime = t("speedrun".equals(mode) ? 45 : 70);

        if (pt < searchTime) {
            setSprint(client, true);
            moveForward = 1f;
            // Lance des yeux d'ender periodiquement
            if (pt % 80 == 0) {
                setUse(client, true);
                setTargetPitch(client, -20f);
                if (pt == 0) log("[END] Lance des yeux d'Ender...");
            } else if (pt % 80 == 10) {
                setUse(client, false);
                setTargetPitch(client, 0f);
            }
            if (pt % 50 == 0) {
                // Ajuste la direction vers la forteresse (simule)
                setTargetYaw(client, client.player.getYaw() + RNG.nextFloat() * 30f - 15f);
            }
        } else {
            setUse(client, false);
            clearMovement();
            setSprint(client, false);
            log("[END] Forteresse trouvee ! Activation du portail...");
            nextPhase(Phase.ENTER_END);
        }
    }

    // ── Phase 12 : Entrer dans The End ───────────
    private void tickEnterEnd(MinecraftClient client) {
        int pt = phaseTick;

        if (pt < t(5)) {
            // Descend dans la forteresse
            moveForward = 0.5f;
            setTargetPitch(client, 40f);
        } else if (pt < t(5) + 40) {
            // Place les yeux d'ender dans le portail
            clearMovement();
            setUse(client, pt % 10 < 5);
            if (pt == t(5)) log("[END] Activation du portail de l'End...");
        } else if (pt < t(10)) {
            // Saute dans le portail
            setUse(client, false);
            moveForward = 1f;
            jumping = true;
        } else {
            clearMovement();
            jumping = false;
            log("[END] Dans The End ! Destruction des cristaux...");
            nextPhase(Phase.DESTROY_CRYSTALS);
        }
    }

    // ── Phase 13 : Detruire cristaux ─────────────
    private void tickDestroyCrystals(MinecraftClient client) {
        int pt = phaseTick;
        int destroyTime = t(25);

        if (pt < destroyTime) {
            // Regarde vers le haut et attaque les cristaux
            setTargetPitch(client, -70f);
            setAttack(client, pt % 8 < 5);
            if (pt % 30 == 0) {
                setTargetYaw(client, client.player.getYaw() + 36f); // fait un tour complet
            }
        } else {
            setAttack(client, false);
            setTargetPitch(client, 0f);
            log("[END] Cristaux detruits ! Combat du Dragon...");
            nextPhase(Phase.FIGHT_DRAGON);
        }
    }

    // ── Phase 14 : Combat Dragon ──────────────────
    private void tickFightDragon(MinecraftClient client) {
        int pt = phaseTick;
        int fightTime = t("speedrun".equals(mode) ? 120 : 180);

        if (pt < fightTime) {
            // Attaque continue, suit le dragon
            setAttack(client, pt % 6 < 4);
            if (pt % 20 == 0) {
                setTargetYaw(client, client.player.getYaw() + RNG.nextFloat() * 50f - 25f);
                setTargetPitch(client, RNG.nextFloat() * 60f - 40f);
            }
            // Esquive
            moveForward = (pt % 40 < 20) ? 0.8f : -0.4f;
            moveSideways = (pt % 60 < 30) ? 0.5f : -0.5f;
            if (pt % 30 == 0) jumping = true;
            if (pt % 15 == 0) log("[DRAGON] Combat... " + (int)(pt * 100f / fightTime) + "%");
        } else {
            setAttack(client, false);
            clearMovement();
            log("[DRAGON] L'ENDER DRAGON EST MORT !");
            log("====================================");
            log("  SPEEDRUN TERMINE !  ");
            log("====================================");
            nextPhase(Phase.VICTORY);
        }
    }

    // ── Phase 15 : Victoire ───────────────────────
    private void tickVictory(MinecraftClient client) {
        clearMovement();
        if (phaseTick == 1) {
            log("Félicitations ! Minecraft terminé !");
            active = false;
        }
    }

    // ═══════════════════════════════════════════
    // HELPERS
    // ═══════════════════════════════════════════

    private void setPhase(Phase p) {
        phase     = p;
        phaseTick = 0;
        subStep   = 0;
        if (phaseListener != null) phaseListener.onPhase(p);
        if (p != Phase.IDLE) log("[PHASE] → " + p.name());
    }

    private void nextPhase(Phase p) {
        setPhase(p);
    }

    private void clearMovement() {
        moveForward  = 0f;
        moveSideways = 0f;
        jumping      = false;
        sneaking     = false;
    }

    private void log(String msg) {
        if (logger != null) logger.log(msg);
        System.out.println("[SpeedrunBot] " + msg);
    }

    // ── Rotation douce ───────────────────────────

    private float lookTargetYaw   = Float.NaN;
    private float lookTargetPitch = Float.NaN;

    private void setTargetYaw(MinecraftClient client, float yaw) {
        lookTargetYaw = yaw;
    }

    private void setTargetPitch(MinecraftClient client, float pitch) {
        lookTargetPitch = MathHelper.clamp(pitch, -90f, 90f);
    }

    private void smoothLook(ClientPlayerEntity player) {
        if (!Float.isNaN(lookTargetYaw)) {
            float diff  = lookTargetYaw - player.getYaw();
            // Normalise entre -180 et 180
            while (diff > 180f)  diff -= 360f;
            while (diff < -180f) diff += 360f;
            float step = MathHelper.clamp(diff * 0.2f, -8f, 8f);
            player.setYaw(player.getYaw() + step);
            if (Math.abs(diff) < 1f) lookTargetYaw = Float.NaN;
        }
        if (!Float.isNaN(lookTargetPitch)) {
            float diff = lookTargetPitch - player.getPitch();
            float step = MathHelper.clamp(diff * 0.2f, -8f, 8f);
            player.setPitch(player.getPitch() + step);
            if (Math.abs(diff) < 1f) lookTargetPitch = Float.NaN;
        }
    }

    // ── Sprint / attaque / use ───────────────────

    private void setSprint(MinecraftClient client, boolean sprinting) {
        if (client.player != null) client.player.setSprinting(sprinting);
    }

    private void setAttack(MinecraftClient client, boolean attack) {
        if (attack) {
            // Simule maintien du clic gauche
            client.options.attackKey.setPressed(true);
        } else {
            client.options.attackKey.setPressed(false);
        }
    }

    private void setUse(MinecraftClient client, boolean use) {
        if (use) {
            client.options.useKey.setPressed(true);
        } else {
            client.options.useKey.setPressed(false);
        }
    }
}

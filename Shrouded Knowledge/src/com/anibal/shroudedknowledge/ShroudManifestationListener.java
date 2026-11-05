package com.anibal.shroudedknowledge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;
import com.fs.starfarer.api.fleet.FleetMemberAPI;

import java.util.List;

public class ShroudManifestationListener implements FleetEventListener {

    private static final String DWELLER_FACTION_ID = "dweller";

    public ShroudManifestationListener() {
    }

    @Override
    public void reportFleetDespawnedToListener(
            CampaignFleetAPI fleet,
            FleetDespawnReason reason,
            Object param) {
    }

    @Override
    public void reportBattleOccurred(
            CampaignFleetAPI fleet,
            CampaignFleetAPI primaryWinner,
            BattleAPI battle) {

        Global.getLogger(ShroudManifestationListener.class).info(
                "[Shrouded Knowledge] reportBattleOccurred EJECUTADO"
        );

        if (primaryWinner == null || battle == null) {
            Global.getLogger(ShroudManifestationListener.class).info(
                    "[Shrouded Knowledge] ERROR: winner o battle es null"
            );
            return;
        }

        ShroudedKnowledgeManager manager =
                ShroudedKnowledgeManager.getInstance();

        if (manager == null) {
            Global.getLogger(ShroudManifestationListener.class).info(
                    "[Shrouded Knowledge] ERROR: manager es null"
            );
            return;
        }

        boolean playerWon = battle.isPlayerSide(
                battle.getSideFor(primaryWinner)
        );

        Global.getLogger(ShroudManifestationListener.class).info(
                "[Shrouded Knowledge] ¿Ganó el jugador?: " + playerWon
        );

        if (!playerWon) {
            return;
        }

        List<CampaignFleetAPI> defeatedFleets =
                battle.getOtherSideSnapshotFor(primaryWinner);

        Global.getLogger(ShroudManifestationListener.class).info(
                "[Shrouded Knowledge] Flotas del bando enemigo: "
                        + (defeatedFleets == null ? "null" : defeatedFleets.size())
        );

        if (defeatedFleets == null) {
            return;
        }

        for (CampaignFleetAPI defeatedFleet : defeatedFleets) {

            if (defeatedFleet == null) {
                continue;
            }

            String fleetId = defeatedFleet.getId();

            String factionId = defeatedFleet.getFaction() == null
                    ? "null"
                    : defeatedFleet.getFaction().getId();

            Global.getLogger(ShroudManifestationListener.class).info(
                    "[Shrouded Knowledge] Flota encontrada: "
                            + fleetId + " | Facción: " + factionId
            );

            if (!DWELLER_FACTION_ID.equals(factionId)) {
                continue;
            }

            boolean defeated = battle.wasFleetDefeated(
                    defeatedFleet,
                    primaryWinner
            );

            Global.getLogger(ShroudManifestationListener.class).info(
                    "[Shrouded Knowledge] ¿Flota Shroud derrotada?: " + defeated
            );

            if (!defeated) {
                continue;
            }

            if (manager.hasFleetBeenRewarded(fleetId)) {
                Global.getLogger(ShroudManifestationListener.class).info(
                        "[Shrouded Knowledge] Flota ya recompensada: " + fleetId
                );
                continue;
            }

            int points = calculateThreatPoints(defeatedFleet);

            Global.getLogger(ShroudManifestationListener.class).info(
                    "[Shrouded Knowledge] Puntos calculados: " + points
            );

            if (points <= 0) {
                Global.getLogger(ShroudManifestationListener.class).info(
                        "[Shrouded Knowledge] ERROR: composición no reconocida"
                );
                continue;
            }

            if (!manager.markFleetRewarded(fleetId)) {
                continue;
            }

            manager.unlockKnowledge();

            manager.addThreatPoints(
                    points,
                    "Victoria contra una manifestación Shroud"
            );

            Global.getLogger(ShroudManifestationListener.class).info(
                    "[Shrouded Knowledge] RECOMPENSA APLICADA: "
                            + fleetId + " | Puntos: +" + points
            );
        }
    }

    private int calculateThreatPoints(CampaignFleetAPI fleet) {

        int tendrils = 0;
        int eyes = 0;
        int maelstroms = 0;
        int maws = 0;

        // Obtener la composición de la flota antes de la batalla.
        List<FleetMemberAPI> members =
                fleet.getFleetData().getSnapshot();

        Global.getLogger(ShroudManifestationListener.class).info(
                "[Shrouded Knowledge] Naves en snapshot: "
                        + (members == null ? "null" : members.size())
        );

        // Si el snapshot no está disponible, intentar con los miembros actuales.
        if (members == null || members.isEmpty()) {
            members = fleet.getFleetData().getMembersListCopy();

            Global.getLogger(ShroudManifestationListener.class).info(
                    "[Shrouded Knowledge] Naves actuales como alternativa: "
                            + members.size()
            );
        }

        for (FleetMemberAPI member : members) {

            String hullId = member.getHullSpec().getRestoredToHullId();

            Global.getLogger(ShroudManifestationListener.class).info(
                    "[Shrouded Knowledge] Hull ID: " + hullId
            );

            if ("shrouded_tendril".equals(hullId)) {
                tendrils++;

            } else if ("shrouded_eye".equals(hullId)) {
                eyes++;

            } else if ("shrouded_maelstrom".equals(hullId)) {
                maelstroms++;

            } else if ("shrouded_maw".equals(hullId)) {
                maws++;
            }
        }

        Global.getLogger(ShroudManifestationListener.class).info(
                "[Shrouded Knowledge] Composición detectada: "
                        + "Tendrils=" + tendrils
                        + " | Eyes=" + eyes
                        + " | Maelstroms=" + maelstroms
                        + " | Maws=" + maws
        );

        int combined = eyes + maelstroms;

        // LOW
        if (maws == 0
                && eyes == 1
                && tendrils >= 6 && tendrils <= 8
                && maelstroms >= 1 && maelstroms <= 2) {
            return 20;
        }

        // MEDIUM
        if (maws == 1
                && eyes == 1
                && tendrils >= 9 && tendrils <= 12
                && maelstroms >= 1 && maelstroms <= 2) {
            return 30;
        }

        // HIGH
        if (maws == 1
                && eyes >= 2 && eyes <= 3
                && tendrils >= 11 && tendrils <= 14
                && combined >= 3 && combined <= 5) {
            return 50;
        }

        // EXTREME
        if (maws == 2
                && eyes >= 2 && eyes <= 3
                && tendrils >= 12 && tendrils <= 15
                && combined >= 3 && combined <= 5) {
            return 50;
        }

        return 0;
    }

}


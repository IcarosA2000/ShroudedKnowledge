package com.anibal.shroudedknowledge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.BattleAPI;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.listeners.FleetEventListener;

import java.util.List;
import java.util.Map;

public class ShroudedDwellerAttackListener implements FleetEventListener {

    public static final String STATION_FLEET_ID_KEY =
            "shroudedKnowledge_targetStationFleetId";

    public static final String STATION_DEFEATED_KEY =
            "shroudedKnowledge_targetStationDefeated";



    @Override
    public void reportFleetDespawnedToListener(
            CampaignFleetAPI fleet,
            FleetDespawnReason reason,
            Object param) {

        if (fleet == null) return;

        // Comprobar que sea nuestra flota élite.
        if (!Boolean.TRUE.equals(
                fleet.getMemoryWithoutUpdate().get(
                        ShroudEliteFleetFactory.FLEET_MARKER))) {
            return;
        }

        // Solo nos interesa que desaparezca por derrota.
        if (reason != FleetDespawnReason.DESTROYED_BY_BATTLE
                && reason != FleetDespawnReason.NO_MEMBERS) {
            return;
        }

        Object savedData = Global.getSector()
                .getPersistentData()
                .get(ShroudedKnowledgeManager.DATA_KEY);

        if (!(savedData instanceof Map)) return;

        @SuppressWarnings("unchecked")
        Map<String, Object> data =
                (Map<String, Object>) savedData;

        // Confirmar que sea la flota registrada para este ataque.
        Object savedFleetId =
                data.get(ShroudedDwellerAttackIntel.FLEET_ID_KEY);

        if (!(savedFleetId instanceof String)) return;

        if (!fleet.getId().equals(savedFleetId)) return;

        String status = String.valueOf(
                data.get(ShroudedDwellerAttackIntel.STATUS_KEY));

        // Solo procesar ataques que ya llegaron.
        if (!ShroudedDwellerAttackIntel.STATUS_ARRIVED.equals(status)
                && !ShroudedDwellerAttackIntel.STATUS_BOMBARDING.equals(status)) {
            return;
        }

        // Si el bombardeo ya comenzó, no cancelar la operación.
        if (ShroudedDwellerAttackIntel.STATUS_BOMBARDING.equals(status)) {
            return;
        }

        // Si la estación ya fue neutralizada, conservar el ataque.
        if (Boolean.TRUE.equals(data.get(STATION_DEFEATED_KEY))) {

            Global.getLogger(ShroudedDwellerAttackListener.class).info(
                    "[Shrouded Knowledge] The elite fleet was defeated, "
                            + "but the station had already been neutralized. "
                            + "The bombing may continue."
            );

            return;
        }

        // Si no había estación registrada, permitir que el manager
        // gestione la transición al bombardeo.
        Object savedStationId = data.get(STATION_FLEET_ID_KEY);

        if (!(savedStationId instanceof String)) {
            return;
        }

        // La flota élite fue derrotada antes de neutralizar la estación.
        data.put(
                ShroudedDwellerAttackIntel.STATUS_KEY,
                ShroudedDwellerAttackIntel.STATUS_DEFEATED
        );

        Global.getLogger(ShroudedDwellerAttackListener.class).info(
                "[Shrouded Knowledge] The strike fleet has been defeated."
        );
    }

    @Override
    public void reportBattleOccurred(
            CampaignFleetAPI fleet,
            CampaignFleetAPI primaryWinner,
            BattleAPI battle) {

        if (battle == null || primaryWinner == null) {
            return;
        }

        Object savedData = Global.getSector()
                .getPersistentData()
                .get(ShroudedKnowledgeManager.DATA_KEY);

        if (!(savedData instanceof Map)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data =
                (Map<String, Object>) savedData;

        // Comprobar que alguna flota de nuestra expedición
        // participó en el combate.
        List<CampaignFleetAPI> bothSides = battle.getBothSides();

        if (bothSides == null || bothSides.isEmpty()) {
            return;
        }

        boolean attackerParticipated = false;

        for (CampaignFleetAPI participant : bothSides) {

            if (isAttackFleet(participant, data)) {
                attackerParticipated = true;
                break;
            }
        }

        if (!attackerParticipated) {
            return;
        }

        // Recuperar el ID de la estación objetivo.
        Object savedStationId = data.get(STATION_FLEET_ID_KEY);

        if (!(savedStationId instanceof String)) {
            return;
        }

        String stationFleetId = (String) savedStationId;

        // Buscar la estación entre los participantes del lado de estación.
        List<CampaignFleetAPI> stationSide = battle.getStationSide();

        // No todos los combates tienen una estación involucrada.
        if (stationSide == null || stationSide.isEmpty()) {
            return;
        }

        for (CampaignFleetAPI station : stationSide) {

            if (station == null) {
                continue;
            }

            if (!stationFleetId.equals(station.getId())) {
                continue;
            }

            // No importa si ganó la flota élite o el jugador.
            // Lo importante es que la estación haya sido derrotada.
            if (battle.wasFleetDefeated(station, primaryWinner)) {

                data.put(STATION_DEFEATED_KEY, true);

                Global.getLogger(ShroudedDwellerAttackListener.class).info(
                        "[Shrouded Knowledge] ¡The orbital station "
                                + "of the target colony has been defeated!"
                );
            }

            break;
        }
    }

    private boolean isAttackFleet(
            CampaignFleetAPI fleet,
            Map<String, Object> data) {

        if (fleet == null) {
            return false;
        }

        // Comprobar flota élite.
        Object eliteId = data.get(
                ShroudedDwellerAttackIntel.FLEET_ID_KEY
        );

        if (eliteId instanceof String
                && fleet.getId().equals(eliteId)) {
            return true;
        }

        // Comprobar flotas de apoyo.
        Object savedSupports = data.get(
                "shroudedKnowledge_supportFleetIds"
        );

        if (savedSupports instanceof List<?>) {

            List<?> supportIds = (List<?>) savedSupports;

            for (Object id : supportIds) {

                if (id instanceof String
                        && fleet.getId().equals(id)) {
                    return true;
                }
            }
        }

        return false;
    }
}

package com.anibal.shroudedknowledge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.DwellerCMD;
import com.fs.starfarer.api.impl.campaign.rulecmd.DwellerCMD.DwellerStrength;

import java.util.Random;

public class ShroudedDwellerAttackFactory {

    public static final String FLEET_MARKER =
            "$shroudedKnowledge_attackFleet";

    public static final String FLEET_TYPE_KEY =
            "$shroudedKnowledge_manifestationType";

    private ShroudedDwellerAttackFactory() {
    }

    /**
     * Crea la fuerza principal de la crisis.
     * 1 = Lesser Manifestation
     * 2 = Greater Manifestation
     * 3 = Transcendent Manifestation
     */
    public static CampaignFleetAPI createFleet(
            int manifestationType,
            Random random) {

        if (random == null) {
            random = new Random();
        }

        DwellerStrength strength;

        switch (manifestationType) {
            case 1:
                strength = DwellerStrength.LOW;
                break;

            case 2:
                strength = DwellerStrength.MEDIUM;
                break;

            case 3:
                strength = DwellerStrength.HIGH;
                break;

            default:
                throw new IllegalArgumentException(
                        "Tipo de manifestación no válido: "
                                + manifestationType
                );
        }

        CampaignFleetAPI fleet =
                DwellerCMD.createDwellerFleet(strength, random);
        if (fleet != null) {
            Global.getLogger(ShroudedDwellerAttackFactory.class).info(
                    "[Shrouded Knowledge] Naves generadas: "
                            + fleet.getFleetData().getMembersListCopy().size()
                            + ", fuerza solicitada: " + strength
            );
        }

        if (fleet == null || fleet.isEmpty()) {
            Global.getLogger(
                    ShroudedDwellerAttackFactory.class
            ).error(
                    "[Shrouded Knowledge] No se pudo crear "
                            + "la manifestación Shrouded."
            );

            return null;
        }

        fleet.setName("Shrouded Dweller Destroyer Fleet");

        fleet.getMemoryWithoutUpdate().set(
                FLEET_MARKER,
                true
        );

        fleet.getMemoryWithoutUpdate().set(
                FLEET_TYPE_KEY,
                manifestationType
        );

        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                true
        );

        return fleet;
    }

    /**
     * Coloca la expedición en el sistema objetivo,
     * a una distancia configurable de la colonia.
     */
    public static boolean deployNearColony(
            CampaignFleetAPI fleet,
            SectorEntityToken colony,
            float distance,
            Random random) {

        if (fleet == null || colony == null) {
            return false;
        }

        LocationAPI location =
                colony.getContainingLocation();

        if (location == null) {
            return false;
        }

        if (random == null) {
            random = new Random();
        }

        location.addEntity(fleet);

        float angle = random.nextFloat() * 360f;

        float x = colony.getLocation().x
                + (float) Math.cos(Math.toRadians(angle))
                * distance;

        float y = colony.getLocation().y
                + (float) Math.sin(Math.toRadians(angle))
                * distance;

        fleet.setLocation(x, y);

        Global.getLogger(
                ShroudedDwellerAttackFactory.class
        ).info(
                "[Shrouded Knowledge] Expedición desplegada "
                        + "en el sistema de "
                        + colony.getName()
                        + ". Distancia: "
                        + distance
        );

        return true;
    }
}

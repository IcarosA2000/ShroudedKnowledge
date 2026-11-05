package com.anibal.shroudedknowledge;

import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.rulecmd.DwellerCMD;

public class PrecursorShroudFleetFactory {

    public static final String FLEET_MARKER =
            "$shroudedKnowledge_precursorFleet";

    /**
     * Crea una de las tres variantes de Precursor Shroud.
     * Variante 1: añade entre 1 y 2 Shrouded Eye.
     * Variante 2: añade entre 3 y 4 Shrouded Eye.
     * Variante 3: añade entre 5 y 6 Shrouded Eye.
     */
    public static CampaignFleetAPI createFleet(
            int variant,
            Random random) {

        if (random == null) {
            throw new IllegalArgumentException(
                    "Random no puede ser null."
            );
        }

        if (variant < 1 || variant > 3) {
            throw new IllegalArgumentException(
                    "La variante debe ser 1, 2 o 3."
            );
        }

        // Partimos de una flota Shrouded de fuerza extrema.
        CampaignFleetAPI fleet = DwellerCMD.createDwellerFleet(
                DwellerCMD.DwellerStrength.EXTREME,
                random
        );

        if (fleet == null) {
            return null;
        }

        int minEyes;
        int maxEyes;

        if (variant == 1) {
            minEyes = 1;
            maxEyes = 2;
        } else if (variant == 2) {
            minEyes = 3;
            maxEyes = 4;
        } else {
            minEyes = 5;
            maxEyes = 6;
        }

        // Añadimos más Shrouded Eye usando la API vanilla.
        DwellerCMD.addShips(
                fleet,
                minEyes,
                maxEyes,
                random,
                ShipRoles.DWELLER_EYE
        );

        // Actualizamos los datos de la flota.
        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getFleetData().sort();

        // Identidad y facción.
        fleet.setName("! Precursor Shroud");
        fleet.getMemoryWithoutUpdate().set(
                FLEET_MARKER,
                true
        );

        fleet.getMemoryWithoutUpdate().set(
                "$shroudedKnowledge_precursorVariant",
                String.valueOf(variant)
        );

        // Conservamos el comportamiento hostil de los Shrouded.
        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                true
        );

        Global.getLogger(
                PrecursorShroudFleetFactory.class
        ).info(
                "[Shrouded Knowledge] Precursor Shroud creado. "
                        + "Variante: " + variant
                        + ", facción: " + Factions.DWELLER
        );

        return fleet;
    }
}


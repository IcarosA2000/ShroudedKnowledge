package com.anibal.shroudedknowledge;

import java.util.List;
import java.util.Random;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickParams;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.ShipRolePick;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.ShipRoles;
import com.fs.starfarer.api.impl.campaign.rulecmd.DwellerCMD;

public class ShroudEliteFleetFactory {

    public static final String FLEET_MARKER =
            "$shroudedKnowledge_eliteFleet";

    public static CampaignFleetAPI createFleet(
            int manifestation, Random random) {

        if (random == null) {
            throw new IllegalArgumentException(
                    "Random no puede ser null.");
        }

        if (manifestation < 1 || manifestation > 3) {
            throw new IllegalArgumentException(
                    "La manifestación debe ser 1, 2 o 3.");
        }

        CampaignFleetAPI fleet = Global.getFactory()
                .createEmptyFleet(
                        Factions.DWELLER,
                        "Shrouded Elite",
                        true);

        if (fleet == null) {
            return null;
        }

        int minEyes;
        int maxEyes;
        int minMaws;
        int maxMaws;

        switch (manifestation) {
            case 1:
                minEyes = 6;
                maxEyes = 8;
                minMaws = 2;
                maxMaws = 3;
                break;

            case 2:
                minEyes = 8;
                maxEyes = 10;
                minMaws = 3;
                maxMaws = 4;
                break;

            default:
                minEyes = 10;
                maxEyes = 12;
                minMaws = 4;
                maxMaws = 5;
                break;
        }

        addShipsChecked(
                fleet, minEyes, maxEyes,
                random, ShipRoles.DWELLER_EYE);

        addShipsChecked(
                fleet, minMaws, maxMaws,
                random, ShipRoles.DWELLER_MAW);

        fleet.getFleetData().setSyncNeeded();
        fleet.getFleetData().syncIfNeeded();
        fleet.getFleetData().sort();

        List<FleetMemberAPI> members =
                fleet.getFleetData().getMembersListCopy();

        // Recuperar el CR de todas las naves.
        for (FleetMemberAPI member : members) {
            member.getRepairTracker().setCR(
                    member.getRepairTracker().getMaxCR());
        }

        fleet.setName("Shrouded Elite");

        MemoryAPI memory = fleet.getMemoryWithoutUpdate();
        memory.set(FLEET_MARKER, true);
        memory.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true);
        memory.set(MemFlags.MAY_GO_INTO_ABYSS, true);

        Global.getLogger(ShroudEliteFleetFactory.class).info(
                "[Shrouded Knowledge] Flota élite creada. "
                        + "Manifestación: " + manifestation
                        + ", naves: " + members.size());

        return fleet;
    }

    private static void addShipsChecked(
            CampaignFleetAPI fleet,
            int min,
            int max,
            Random random,
            String role) {

        FactionAPI faction =
                Global.getSector().getFaction(Factions.DWELLER);

        ShipPickParams params =
                new ShipPickParams(ShipPickMode.ALL);

        params.blockFallback = true;
        params.maxFP = 1000000;

        int requested = min + random.nextInt(max - min + 1);
        int added = 0;

        for (int i = 0; i < requested; i++) {
            List<ShipRolePick> picks =
                    faction.pickShip(role, params, null, random);

            if (picks == null || picks.isEmpty()) {
                Global.getLogger(ShroudEliteFleetFactory.class).warn(
                        "[Shrouded Knowledge] No ship was found "
                                + "para el rol " + role);
                continue;
            }

            for (ShipRolePick pick : picks) {
                if (pick == null || pick.variantId == null) {
                    continue;
                }

                fleet.getFleetData().addFleetMember(
                        pick.variantId);

                added++;
            }
        }

        Global.getLogger(ShroudEliteFleetFactory.class).info(
                "[Shrouded Knowledge] Rol " + role
                        + ": solicitadas " + requested
                        + ", añadidas " + added);
    }
}

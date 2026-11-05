package com.anibal.shroudedknowledge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CampaignEventListener.FleetDespawnReason;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;

import java.util.Map;

public class PrecursorFleetListener extends BaseCampaignEventListener {

    public PrecursorFleetListener() {
        super(false);
    }

    @Override
    public void reportFleetDespawned(
            CampaignFleetAPI fleet,
            FleetDespawnReason reason,
            Object param) {

        if (fleet == null) {
            return;
        }

        if (!Boolean.TRUE.equals(
                fleet.getMemoryWithoutUpdate().get(
                        PrecursorShroudFleetFactory.FLEET_MARKER))) {
            return;
        }

        if (reason != FleetDespawnReason.DESTROYED_BY_BATTLE
                && reason != FleetDespawnReason.NO_MEMBERS) {
            return;
        }

        String variantText = fleet.getMemoryWithoutUpdate().getString(
                "$shroudedKnowledge_precursorVariant"
        );

        if (variantText == null) {
            return;
        }

        int variant;

        try {
            variant = Integer.parseInt(variantText);
        } catch (NumberFormatException ex) {
            return;
        }

        if (variant < 1 || variant > 3) {
            return;
        }

        Object saved = Global.getSector()
                .getPersistentData()
                .get(ShroudedKnowledgeManager.DATA_KEY);

        if (!(saved instanceof Map<?, ?>)) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) saved;

        String key = "precursor" + variant + "Destroyed";

        if (Boolean.TRUE.equals(data.get(key))) {
            return;
        }

        data.put(key, true);

        Global.getLogger(PrecursorFleetListener.class).info(
                "[Shrouded Knowledge] Precursor " + variant
                        + " defeated in combat."
        );

        boolean allDestroyed =
                Boolean.TRUE.equals(data.get("precursor1Destroyed"))
                        && Boolean.TRUE.equals(data.get("precursor2Destroyed"))
                        && Boolean.TRUE.equals(data.get("precursor3Destroyed"));

        if (allDestroyed
                && !Boolean.TRUE.equals(
                data.get("precursorVictoryMessageShown"))) {

            data.put("precursorVictoryMessageShown", true);

            Global.getSector().getCampaignUI().addMessage(
                    "For the moment, you have defeated the Precursors.."
            );

            Global.getLogger(PrecursorFleetListener.class).info(
                    "[Shrouded Knowledge] The three Precursor fleets "
                            + "they have been defeated."
            );
        }
    }
}


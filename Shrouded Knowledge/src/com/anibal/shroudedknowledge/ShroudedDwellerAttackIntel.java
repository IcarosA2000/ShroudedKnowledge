package com.anibal.shroudedknowledge;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Set;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class ShroudedDwellerAttackIntel extends BaseIntelPlugin {

    public static final String INTEL_KEY =
            "anibal_shrouded_knowledge_attack_intel";

    public static final String TARGET_MARKET_KEY =
            "shroudedKnowledge_attackTargetMarket";

    public static final String MANIFESTATION_KEY =
            "shroudedKnowledge_attackManifestation";

    public static final String ARRIVAL_DAYS_KEY =
            "shroudedKnowledge_attackArrivalDays";

    public static final String STATUS_KEY =
            "shroudedKnowledge_attackStatus";

    public static final String FLEET_ID_KEY =
            "shroudedKnowledge_attackFleetId";

    public static final String STATUS_PREPARING = "preparing";
    public static final String STATUS_ARRIVED = "arrived";
    public static final String STATUS_DEFEATED = "defeated";
    public static final String STATUS_BOMBARDING = "bombarding";
    public static final String STATUS_COMPLETED = "completed";

    @Override
    protected String getName() {
        return "Shrouded Dweller Attack";
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName("factions", "dweller");
    }

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public boolean hasSmallDescription() {
        return true;
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags =
                new LinkedHashSet<String>(super.getIntelTags(map));

        tags.add(Tags.INTEL_MAJOR_EVENT);
        return tags;
    }

    @Override
    public void createSmallDescription(
            TooltipMakerAPI info, float width, float height) {

        Color highlight = Misc.getHighlightColor();

        Object data = Global.getSector()
                .getPersistentData()
                .get(ShroudedKnowledgeManager.DATA_KEY);

        if (!(data instanceof java.util.Map)) {
            info.addPara(
                    "The Shrouded are preparing an expedition "
                            + "against a important colony.",
                    10f);
            return;
        }

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> saved =
                (java.util.Map<String, Object>) data;

        String marketId = getString(saved, TARGET_MARKET_KEY);
        int manifestation = getInt(saved, MANIFESTATION_KEY, 1);
        float arrivalDays = getFloat(saved, ARRIVAL_DAYS_KEY, 0f);
        String status = getString(saved, STATUS_KEY);

        MarketAPI market = null;

        if (marketId != null) {
            market = Global.getSector()
                    .getEconomy()
                    .getMarket(marketId);
        }

        info.addPara(
                "A demonstration by the Shrouded is headed "
                        + "against a player's colony.",
                10f);

        info.addSectionHeading(
                "Threat assessment",
                Misc.getBasePlayerColor(),
                Misc.getDarkPlayerColor(),
                com.fs.starfarer.api.ui.Alignment.MID,
                10f);

        info.addPara(
                "Type of demonstration: %s",
                10f,
                highlight,
                getManifestationName(manifestation));

        info.addPara(
                "Attacking faction: Shrouded Dwellers",
                5f);

        if (market != null) {
            info.addPara(
                    "Target colony: %s",
                    5f,
                    highlight,
                    market.getName());
        } else {
            info.addPara(
                    "Target colony: unknown",
                    5f);
        }

        info.addPara(
                "Shipment status: %s",
                5f,
                highlight,
                getStatusName(status));

        if (STATUS_PREPARING.equals(status)) {
            info.addPara(
                    "Estimated time until arrival: %s days",
                    5f,
                    highlight,
                    String.valueOf(Math.max(0, (int) Math.ceil(arrivalDays))));
        } else if (STATUS_ARRIVED.equals(status)) {
            info.addPara(
                    "The expedition has appeared in the target system.. "
                            + "Their defenses have yet to be defeated.",
                    5f);
        } else if (STATUS_BOMBARDING.equals(status)) {
            info.addPara(
                    "The attacking forces have overcome the defenses. "
                            + "The settlement is in danger of being bombed.",
                    5f);
        } else if (STATUS_DEFEATED.equals(status)) {
            info.addPara(
                    "The expedition has been defeated. "
                            + "The colony is no longer threatened by this attack.",
                    5f);
        } else if (STATUS_COMPLETED.equals(status)) {
            info.addPara(
                    "The operation against the colony has concluded.",
                    5f);
        }
    }

    private String getManifestationName(int type) {
        switch (type) {
            case 1:
                return "Lesser Manifestation";
            case 2:
                return "Greater Manifestation";
            case 3:
                return "Transcendent Manifestation";
            default:
                return "Manifestación desconocida";
        }
    }

    private String getStatusName(String status) {
        if (STATUS_PREPARING.equals(status)) {
            return "Preparing for the incursion";
        }
        if (STATUS_ARRIVED.equals(status)) {
            return "Facing the defenses";
        }
        if (STATUS_BOMBARDING.equals(status)) {
            return "Preparing the bombardment";
        }
        if (STATUS_DEFEATED.equals(status)) {
            return "Defeated expedition";
        }
        if (STATUS_COMPLETED.equals(status)) {
            return "Operation completed";
        }
        return "Unknown status";
    }

    private String getString(
            java.util.Map<String, Object> data, String key) {

        Object value = data.get(key);
        return value instanceof String ? (String) value : null;
    }

    private int getInt(
            java.util.Map<String, Object> data,
            String key,
            int fallback) {

        Object value = data.get(key);

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return fallback;
    }

    private float getFloat(
            java.util.Map<String, Object> data,
            String key,
            float fallback) {

        Object value = data.get(key);

        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }

        return fallback;
    }
}



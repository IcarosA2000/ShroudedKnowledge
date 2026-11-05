package com.anibal.shroudedknowledge;

import java.awt.Color;
import java.util.LinkedHashSet;
import java.util.Set;

import com.anibal.shroudedknowledge.ui.ThreatProgressBar;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.CustomPanelAPI;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class ShroudedKnowledgeIntel extends BaseIntelPlugin {

    private static final int FIRST_THRESHOLD = 150;
    private static final int FINAL_THRESHOLD = 300;

    private static final String SPRITE_CATEGORY = "shrouded_knowledge";
    private static final String SPRITE_NAME = "dweller";

    private String getText(String id) {
        return Global.getSettings().getString(
                "shrouded_knowledge",
                id
        );
    }

    private ShroudedKnowledgeManager getManager() {
        return ShroudedKnowledgeManager.getInstance();
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = new LinkedHashSet<>(super.getIntelTags(map));
        tags.add(Tags.INTEL_MAJOR_EVENT);
        return tags;
    }

    @Override
    protected String getName() {
        return getText("intel_name");
    }

    @Override
    public String getIcon() {
        return Global.getSettings().getSpriteName(
                SPRITE_CATEGORY,
                SPRITE_NAME
        );
    }

    @Override
    public boolean isImportant() {
        return true;
    }

    @Override
    public void createSmallDescription(
            TooltipMakerAPI info,
            float width,
            float height) {

        ShroudedKnowledgeManager manager = getManager();

        int threatPoints = manager != null
                ? manager.getThreatPoints()
                : 0;

        boolean preparationStarted = manager != null
                && manager.isPreparationStarted();

        boolean firstPhaseActivated = manager != null
                && manager.isFirstPhaseActivated();

        boolean attackStarted = manager != null
                && manager.isAttackStarted();

        Color highlight = Misc.getHighlightColor();
        Color negative = Misc.getNegativeHighlightColor();

        float contentWidth = Math.max(100f, width - 20f);

        // Main image
        float imageWidth = Math.min(contentWidth, 410f);
        float imageHeight = imageWidth * 256f / 410f;

        info.addImage(
                getIcon(),
                imageWidth,
                imageHeight,
                10f
        );

        // Header
        info.addTitle(getText("intel_title"));

        info.addPara(
                getText("event_type"),
                negative,
                8f
        );

        info.addPara(
                getText("intel_description"),
                10f
        );

        // Threat level
        info.addSectionHeading(
                getText("threat_level"),
                Alignment.MID,
                15f
        );

        info.addPara(
                getText("threat_progress"),
                5f
        );

        CustomPanelAPI progressPanel =
                Global.getSettings().createCustom(
                        contentWidth,
                        20f,
                        new ThreatProgressBar()
                );

        info.addCustom(progressPanel, 5f);

        info.addPara(
                "%s / %s",
                5f,
                highlight,
                String.valueOf(threatPoints),
                String.valueOf(FINAL_THRESHOLD)
        );

        float progress = Math.min(
                1f,
                Math.max(0f, threatPoints / (float) FINAL_THRESHOLD)
        );

        info.addPara(
                getText("progress_complete"),
                5f,
                highlight,
                String.valueOf(Math.round(progress * 100f))
        );

        // Threat milestones
        info.addSectionHeading(
                getText("threat_milestones"),
                Alignment.MID,
                15f
        );

        // First manifestation
        info.addPara(
                getText("first_milestone"),
                5f,
                highlight,
                String.valueOf(FIRST_THRESHOLD)
        );

        if (firstPhaseActivated) {

            info.addPara(
                    getText("status_deployed"),
                    negative,
                    5f
            );

            info.addPara(
                    getText("description_deployed"),
                    5f
            );

        } else if (preparationStarted) {

            info.addPara(
                    getText("status_preparation"),
                    highlight,
                    5f
            );

            info.addPara(
                    getText("description_preparation"),
                    5f
            );

        } else {

            info.addPara(
                    getText("status_pending"),
                    5f
            );

            info.addPara(
                    getText("description_first_pending"),
                    5f
            );
        }

        // Shrouded Dweller expedition
        info.addSectionHeading(
                " ",
                Alignment.MID,
                10f
        );

        info.addPara(
                getText("dweller_milestone"),
                5f,
                highlight,
                String.valueOf(FINAL_THRESHOLD)
        );

        if (attackStarted) {

            info.addPara(
                    getText("status_expedition_started"),
                    negative,
                    5f
            );

            info.addPara(
                    getText("description_expedition_started"),
                    5f
            );

        } else {

            info.addPara(
                    getText("status_pending"),
                    5f
            );

            info.addPara(
                    getText("description_expedition_pending"),
                    5f
            );
        }

        // Final note
        info.addPara(
                getText("final_note"),
                Misc.getGrayColor(),
                15f
        );
    }
}






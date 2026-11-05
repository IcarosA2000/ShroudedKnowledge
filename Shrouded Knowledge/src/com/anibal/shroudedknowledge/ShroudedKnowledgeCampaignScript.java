package com.anibal.shroudedknowledge;

import com.fs.starfarer.api.EveryFrameScript;
import com.fs.starfarer.api.Global;

public class ShroudedKnowledgeCampaignScript
        implements EveryFrameScript {

    private final ShroudedKnowledgeManager manager;
    private boolean testExecuted = false;

    public ShroudedKnowledgeCampaignScript(
            ShroudedKnowledgeManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean runWhilePaused() {
        return false;
    }




    @Override
    public void advance(float amount) {
        manager.advance(amount);

        if (!testExecuted) {
            testExecuted = true;

            manager.testThreatPoints();

            Global.getLogger(
                    ShroudedKnowledgeCampaignScript.class
            ).info(
                    "[Shrouded Knowledge] Test executed. "
                            + "Current points: "
                            + manager.getThreatPoints()

            );
        }
    }
}




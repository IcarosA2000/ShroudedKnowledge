package com.anibal.shroudedknowledge;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;

public class ShroudedKnowledgePlugin extends BaseModPlugin {

    private ShroudedKnowledgeManager manager;

    @Override
    public void onGameLoad(boolean newGame) {
        super.onGameLoad(newGame);

        SectorAPI sector = Global.getSector();

        if (sector == null) {
            return;
        }

        // Recuperar o inicializar el manager.
        manager = new ShroudedKnowledgeManager();

        // Comprobar si el conocimiento ya fue desbloqueado.
        // Si lo fue en una partida anterior, asegurar que el Intel exista.
        if (manager.isKnowledgeUnlocked()) {
            manager.ensureMainIntelExists();
        }

        // Registrar el script de campaña.
        sector.addTransientScript(
                new ShroudedKnowledgeCampaignScript(manager)
        );

        Global.getLogger(ShroudedKnowledgePlugin.class).info(
                "Shrouded Knowledge: plugin loaded successfully."
        );
    }
}





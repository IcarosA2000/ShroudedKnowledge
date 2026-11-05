package com.anibal.shroudedknowledge;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.ai.FleetAssignmentDataAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.MarketCMD;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.Industry;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.campaign.FactionAPI;

import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ShroudedKnowledgeManager {

    private static ShroudedKnowledgeManager instance;

    private static final String BOMBARDMENT_EXECUTED_KEY =
            "shroudedKnowledge_bombardmentExecuted";

    private static final String ATTACK_FACTION_ID_KEY =
            "shroudedKnowledge_attackFactionId";

    private static final String SUPPORT_FLEET_IDS_KEY =
            "shroudedKnowledge_supportFleetIds";

    private static final String SUPPORT_FLEET_MARKER =
            "$shroudedKnowledge_supportFleet";

    public static final String DATA_KEY =
            "anibal_shrouded_knowledge_data";

    private static final String KNOWLEDGE_UNLOCKED_KEY =
            "shroudedKnowledge_unlocked";

    private static final String REWARDED_FLEETS_KEY =
            "shroudedKnowledge_rewardedFleets";

    public static final String MAIN_INTEL_KEY =
            "anibal_shrouded_knowledge_intel";

    private static final int FIRST_THRESHOLD = 150;
    private static final int FINAL_THRESHOLD = 300;

    private static final float PREPARATION_DAYS = 20f;

    private static final float ATTACK_ARRIVAL_DAYS = 12f;

    private static final String ATTACK_STARTED_KEY =
            "shroudedKnowledge_attackStarted";

    private static final String ATTACK_ELAPSED_KEY =
            "shroudedKnowledge_attackElapsedDays";

    private static final String FIRST_PHASE_KEY =
            "firstPhaseActivated";

    private static final String PREPARATION_STARTED_KEY =
            "precursorPreparationStarted";

    private static final String PREPARATION_ELAPSED_KEY =
            "precursorPreparationElapsedDays";

    private final Map<String, Object> data;

    private float elapsedDays = 0f;

    public static ShroudedKnowledgeManager getInstance() {
        return instance;
    }

    @SuppressWarnings("unchecked")
    public ShroudedKnowledgeManager() {

        instance = this;

        SectorAPI sector = Global.getSector();

        Object savedData = sector.getPersistentData().get(DATA_KEY);

        if (savedData instanceof Map<?, ?>) {
            data = (Map<String, Object>) savedData;

            if (!(data.get("threatPoints") instanceof Number)) {
                data.put("threatPoints", 0);
            }
        } else {
            data = new HashMap<>();

            data.put("threatPoints", 0);
            data.put(FIRST_PHASE_KEY, false);
            data.put(PREPARATION_STARTED_KEY, false);
            data.put(PREPARATION_ELAPSED_KEY, 0f);

            sector.getPersistentData().put(DATA_KEY, data);
        }

        // Inicializar datos de conocimiento.
        if (!data.containsKey(KNOWLEDGE_UNLOCKED_KEY)) {
            data.put(KNOWLEDGE_UNLOCKED_KEY, false);
        }

        if (!(data.get(REWARDED_FLEETS_KEY) instanceof Set<?>)) {
            data.put(REWARDED_FLEETS_KEY, new HashSet<String>());
        }

        if (!data.containsKey(FIRST_PHASE_KEY)) {
            data.put(FIRST_PHASE_KEY, false);
        }

        if (!data.containsKey(PREPARATION_STARTED_KEY)) {
            data.put(PREPARATION_STARTED_KEY, false);
        }

        if (!(data.get(PREPARATION_ELAPSED_KEY) instanceof Number)) {
            data.put(PREPARATION_ELAPSED_KEY, 0f);
        }

        // Registrar PrecursorFleetListener.
        boolean listenerRegistered = false;

        for (Object listener : sector.getAllListeners()) {
            if (listener instanceof PrecursorFleetListener) {
                listenerRegistered = true;
                break;
            }
        }

        if (!listenerRegistered) {
            sector.addListener(new PrecursorFleetListener());
        }

        // Registrar ShroudedDwellerAttackListener.
        boolean attackListenerRegistered = false;

        for (Object listener : sector.getListenerManager().getListeners(
                com.fs.starfarer.api.campaign.listeners.FleetEventListener.class)) {

            if (listener instanceof ShroudedDwellerAttackListener) {
                attackListenerRegistered = true;
                break;
            }
        }

        if (!attackListenerRegistered) {
            sector.getListenerManager().addListener(
                    new ShroudedDwellerAttackListener(), true);
        }

        // Registrar ShroudManifestationListener.
        boolean manifestationListenerRegistered = false;

        Global.getLogger(ShroudedKnowledgeManager.class).info(
                "[Shrouded Knowledge] ShroudManifestationListener registrado: "
                        + manifestationListenerRegistered
        );

        for (Object listener : sector.getListenerManager().getListeners(
                com.fs.starfarer.api.campaign.listeners.FleetEventListener.class)) {

            if (listener instanceof ShroudManifestationListener) {
                manifestationListenerRegistered = true;
                break;
            }
        }

        if (!manifestationListenerRegistered) {
            sector.getListenerManager().addListener(
                    new ShroudManifestationListener(), true);
        }
    }

    public int getThreatPoints() {
        Object value = data.get("threatPoints");

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        return 0;
    }

    public boolean isKnowledgeUnlocked() {
        return Boolean.TRUE.equals(data.get(KNOWLEDGE_UNLOCKED_KEY));
    }

    public void unlockKnowledge() {
        if (!isKnowledgeUnlocked()) {
            data.put(KNOWLEDGE_UNLOCKED_KEY, true);

            Global.getLogger(ShroudedKnowledgeManager.class).info(
                    "[Shrouded Knowledge] Knowledge unlocked."
            );
        }

        ensureMainIntelExists();
    }

    public void ensureMainIntelExists() {
        SectorAPI sector = Global.getSector();

        if (sector == null || !isKnowledgeUnlocked()) {
            return;
        }

        Object existing = sector.getPersistentData().get(MAIN_INTEL_KEY);

        if (existing instanceof ShroudedKnowledgeIntel) {
            return;
        }

        ShroudedKnowledgeIntel intel = new ShroudedKnowledgeIntel();

        sector.getIntelManager().addIntel(intel);

        sector.getPersistentData().put(MAIN_INTEL_KEY, intel);
    }

    public boolean hasFleetBeenRewarded(String fleetId) {
        if (fleetId == null) {
            return true;
        }

        Object saved = data.get(REWARDED_FLEETS_KEY);

        if (!(saved instanceof Set<?>)) {
            return false;
        }

        return ((Set<?>) saved).contains(fleetId);
    }

    @SuppressWarnings("unchecked")
    public boolean markFleetRewarded(String fleetId) {
        if (fleetId == null) {
            return false;
        }

        Object saved = data.get(REWARDED_FLEETS_KEY);

        Set<String> rewardedFleets;

        if (saved instanceof Set<?>) {
            rewardedFleets = (Set<String>) saved;
        } else {
            rewardedFleets = new HashSet<>();
            data.put(REWARDED_FLEETS_KEY, rewardedFleets);
        }

        return rewardedFleets.add(fleetId);
    }

    public void addThreatPoints(int points, String reason) {
        if (points <= 0) {
            return;
        }

        int updatedPoints = Math.min(
                FINAL_THRESHOLD,
                getThreatPoints() + points
        );

        data.put("threatPoints", updatedPoints);

        Global.getLogger(ShroudedKnowledgeManager.class).info(
                "[Shrouded Knowledge] +" + points
                        + " points: " + reason
                        + ". Threat: " + updatedPoints
                        + "/" + FINAL_THRESHOLD
        );

        checkThreatLevel();
    }

    private void checkThreatLevel() {
        if (getThreatPoints() >= FIRST_THRESHOLD) {
            beginPreparationPeriod();
        }

        if (getThreatPoints() >= FINAL_THRESHOLD) {
            if (!Boolean.TRUE.equals(data.get("maximumThreatLogged"))) {
                data.put("maximumThreatLogged", true);

                Global.getLogger(ShroudedKnowledgeManager.class).info(
                        "[Shrouded Knowledge] Maximum threat reached."
                );
            }

            beginDwellerAttack();
        }
    }

    /**
     * Inicia una única cuenta regresiva de 20 días.
     */
    private void beginPreparationPeriod() {
        if (Boolean.TRUE.equals(data.get(FIRST_PHASE_KEY))) {
            return;
        }

        if (Boolean.TRUE.equals(data.get(PREPARATION_STARTED_KEY))) {
            return;
        }

        data.put(PREPARATION_STARTED_KEY, true);
        data.put(PREPARATION_ELAPSED_KEY, 0f);

        Global.getLogger(ShroudedKnowledgeManager.class).info(
                "[Shrouded Knowledge] The player has "
                        + (int) PREPARATION_DAYS
                        + " days to prepare."
        );

        SectorAPI sector = Global.getSector();

        if (sector.getCampaignUI() != null) {
            sector.getCampaignUI().addMessage(
                    "Shrouded Knowledge: Several Shrouded fleets "
                            + "They have found your position. You have "
                            + (int) PREPARATION_DAYS
                            + " days to prepare before their arrival."
            );
        }
    }

    /**
     * Avanza la cuenta regresiva y despliega las flotas.
     */
    private void advancePreparationPeriod() {
        if (!Boolean.TRUE.equals(data.get(PREPARATION_STARTED_KEY))) {
            return;
        }

        if (Boolean.TRUE.equals(data.get(FIRST_PHASE_KEY))) {
            return;
        }

        Object savedElapsed = data.get(PREPARATION_ELAPSED_KEY);

        float preparationElapsed = 0f;

        if (savedElapsed instanceof Number) {
            preparationElapsed = ((Number) savedElapsed).floatValue();
        }

        preparationElapsed += 1f;

        data.put(PREPARATION_ELAPSED_KEY, preparationElapsed);

        Global.getLogger(ShroudedKnowledgeManager.class).info(
                "[Shrouded Knowledge] Preparation: "
                        + (int) preparationElapsed
                        + "/" + (int) PREPARATION_DAYS
                        + " días."
        );

        if (preparationElapsed >= PREPARATION_DAYS) {
            activatePrecursorFleets();
        }
    }

    /**
     * Despliega las tres flotas una vez terminado el plazo.
     */
    private void activatePrecursorFleets() {
        if (Boolean.TRUE.equals(data.get(FIRST_PHASE_KEY))) {
            return;
        }

        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();

        if (playerFleet == null
                || playerFleet.getContainingLocation() == null) {
            return;
        }

        LocationAPI location = playerFleet.getContainingLocation();
        Random random = new Random();

        int createdFleets = 0;

        for (int variant = 1; variant <= 3; variant++) {
            String fleetIdKey = "precursor" + variant + "FleetId";

            Object savedId = data.get(fleetIdKey);

            if (savedId instanceof String) {
                SectorEntityToken existingEntity =
                        sector.getEntityById((String) savedId);

                if (existingEntity instanceof CampaignFleetAPI) {
                    CampaignFleetAPI existingFleet =
                            (CampaignFleetAPI) existingEntity;

                    if (Boolean.TRUE.equals(
                            existingFleet.getMemoryWithoutUpdate().get(
                                    PrecursorShroudFleetFactory.FLEET_MARKER))) {

                        createdFleets++;
                        continue;
                    }
                }
            }

            CampaignFleetAPI fleet =
                    PrecursorShroudFleetFactory.createFleet(
                            variant, random
                    );

            if (fleet == null) {
                Global.getLogger(
                        ShroudedKnowledgeManager.class
                ).error(
                        "[Shrouded Knowledge] Could not be created "
                                + "Precursor Shroud, variant "
                                + variant
                );
                continue;
            }

            fleet.getStats().getFleetwideMaxBurnMod().modifyFlat(
                    "shrouded_knowledge_precursor_burn",
                    8f,
                    "Precursor Shroud"
            );

            float angle = (variant - 1) * 120f
                    + random.nextFloat() * 20f;

            float distance = 500f + random.nextFloat() * 300f;

            float x = playerFleet.getLocation().x
                    + (float) Math.cos(Math.toRadians(angle)) * distance;

            float y = playerFleet.getLocation().y
                    + (float) Math.sin(Math.toRadians(angle)) * distance;

            location.addEntity(fleet);
            fleet.setLocation(x, y);

            fleet.getMemoryWithoutUpdate().set(
                    MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                    true
            );

            fleet.addAssignment(
                    FleetAssignment.INTERCEPT,
                    playerFleet,
                    9999f,
                    "hunting the player"
            );

            data.put(fleetIdKey, fleet.getId());

            createdFleets++;

            Global.getLogger(
                    ShroudedKnowledgeManager.class
            ).info(
                    "[Shrouded Knowledge] Precursor Shroud "
                            + variant + " desplegado. ID: "
                            + fleet.getId()
            );
        }

        if (createdFleets == 3) {
            data.put(FIRST_PHASE_KEY, true);

            if (sector.getCampaignUI() != null) {
                sector.getCampaignUI().addMessage(
                        "Shrouded Knowledge: The Precursor fleets "
                                + "They have arrived. Survive."
                );
            }

            Global.getLogger(
                    ShroudedKnowledgeManager.class
            ).info(
                    "[Shrouded Knowledge] The three Precursor fleets "
                            + "have been deployed."
            );
        } else {
            Global.getLogger(
                    ShroudedKnowledgeManager.class
            ).warn(
                    "[Shrouded Knowledge] Solo se encontraron o "
                            + "crearon " + createdFleets
                            + " de las 3 flotas. Se reintentará."
            );
        }
    }

    private void maintainPrecursorPursuit() {
        if (!Boolean.TRUE.equals(data.get(FIRST_PHASE_KEY))) {
            return;
        }

        SectorAPI sector = Global.getSector();
        CampaignFleetAPI playerFleet = sector.getPlayerFleet();

        if (playerFleet == null
                || playerFleet.getContainingLocation() == null) {
            return;
        }

        LocationAPI playerLocation =
                playerFleet.getContainingLocation();

        Random random = new Random();

        for (int variant = 1; variant <= 3; variant++) {
            String prefix = "precursor" + variant;

            if (Boolean.TRUE.equals(
                    data.get(prefix + "Destroyed"))) {
                continue;
            }

            Object savedId = data.get(prefix + "FleetId");

            if (!(savedId instanceof String)) {
                continue;
            }

            SectorEntityToken entity =
                    sector.getEntityById((String) savedId);

            if (!(entity instanceof CampaignFleetAPI)) {
                continue;
            }

            CampaignFleetAPI fleet = (CampaignFleetAPI) entity;

            if (!Boolean.TRUE.equals(
                    fleet.getMemoryWithoutUpdate().get(
                            PrecursorShroudFleetFactory.FLEET_MARKER))) {
                continue;
            }

            if (fleet.isDespawning()
                    || fleet.getContainingLocation() == null
                    || fleet.getBattle() != null) {
                continue;
            }

            LocationAPI oldLocation = fleet.getContainingLocation();

            if (oldLocation != playerLocation) {
                oldLocation.removeEntity(fleet);
                playerLocation.addEntity(fleet);

                float angle = random.nextFloat() * 360f;
                float distance = 600f + random.nextFloat() * 300f;

                float x = playerFleet.getLocation().x
                        + (float) Math.cos(Math.toRadians(angle)) * distance;

                float y = playerFleet.getLocation().y
                        + (float) Math.sin(Math.toRadians(angle)) * distance;

                fleet.setLocation(x, y);

                Global.getLogger(
                        ShroudedKnowledgeManager.class
                ).info(
                        "[Shrouded Knowledge] Precursor "
                                + variant
                                + " performed an extradimensional jump."
                );
            }

            fleet.getMemoryWithoutUpdate().set(
                    MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                    true
            );

            FleetAssignmentDataAPI current =
                    fleet.getCurrentAssignment();

            if (current != null
                    && current.getAssignment() == FleetAssignment.INTERCEPT
                    && current.getTarget() == playerFleet) {
                continue;
            }

            fleet.clearAssignments();

            fleet.addAssignment(
                    FleetAssignment.INTERCEPT,
                    playerFleet,
                    9999f,
                    "hunting the player"
            );

            Global.getLogger(
                    ShroudedKnowledgeManager.class
            ).info(
                    "[Shrouded Knowledge] Persecution reinstated: "
                            + "Precursor " + variant
            );
        }
    }

    public void advance(float amount) {
        elapsedDays += Global.getSector()
                .getClock()
                .convertToDays(amount);

        if (elapsedDays < 1f) {
            return;
        }

        int wholeDays = (int) elapsedDays;
        elapsedDays -= wholeDays;

        for (int i = 0; i < wholeDays; i++) {
            boolean preparationWasActive =
                    Boolean.TRUE.equals(data.get(PREPARATION_STARTED_KEY));

            checkThreatLevel();

            if (preparationWasActive) {
                advancePreparationPeriod();
            }

            advanceDwellerAttack();
            maintainPrecursorPursuit();
        }
    }

    private MarketAPI selectMainColony() {
        MarketAPI bestMarket = null;
        double bestScore = -1d;

        for (MarketAPI market :
                Global.getSector().getEconomy().getMarketsCopy()) {

            if (market == null || !market.isPlayerOwned()) {
                continue;
            }

            if (market.getPrimaryEntity() == null
                    || market.getStarSystem() == null) {
                continue;
            }

            double sizeScore = market.getSize() * 1000d;
            double incomeScore = Math.max(0d, market.getNetIncome());

            double score = sizeScore + incomeScore;

            if (score > bestScore) {
                bestScore = score;
                bestMarket = market;
            }
        }

        return bestMarket;
    }

    private int chooseManifestation(MarketAPI market) {
        if (market == null) {
            return 1;
        }

        int size = market.getSize();
        double income = Math.max(0d, market.getNetIncome());

        if (size >= 8 || income >= 100000d) {
            return 3;
        }

        if (size >= 6 || income >= 50000d) {
            return 2;
        }

        return 1;
    }

    public void ensureAttackIntelExists() {
        SectorAPI sector = Global.getSector();

        if (sector == null) {
            return;
        }

        Object existing = sector.getPersistentData().get(
                ShroudedDwellerAttackIntel.INTEL_KEY);

        if (existing instanceof ShroudedDwellerAttackIntel) {
            return;
        }

        ShroudedDwellerAttackIntel intel =
                new ShroudedDwellerAttackIntel();

        sector.getIntelManager().addIntel(intel);

        sector.getPersistentData().put(
                ShroudedDwellerAttackIntel.INTEL_KEY,
                intel);
    }

    private void beginDwellerAttack() {
        if (Boolean.TRUE.equals(data.get(ATTACK_STARTED_KEY))) {
            return;
        }

        MarketAPI target = selectMainColony();

        if (target == null) {
            Global.getLogger(ShroudedKnowledgeManager.class).warn(
                    "[Shrouded Knowledge] No colony was found "
                            + "of the player for Phase 2."
            );
            return;
        }

        int manifestation = chooseManifestation(target);

        data.put(ATTACK_STARTED_KEY, true);
        data.put(
                ShroudedDwellerAttackIntel.TARGET_MARKET_KEY,
                target.getId()
        );
        data.put(
                ShroudedDwellerAttackIntel.MANIFESTATION_KEY,
                manifestation
        );
        data.put(
                ShroudedDwellerAttackIntel.ARRIVAL_DAYS_KEY,
                ATTACK_ARRIVAL_DAYS
        );
        data.put(
                ShroudedDwellerAttackIntel.STATUS_KEY,
                ShroudedDwellerAttackIntel.STATUS_PREPARING
        );
        data.put(ATTACK_ELAPSED_KEY, 0f);

        ensureAttackIntelExists();

        if (Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(
                    "Shrouded Knowledge: a ... has been detected "
                            + "extradimensional manifestation. "
                            + "An important colony is in danger."
            );
        }

        Global.getLogger(ShroudedKnowledgeManager.class).info(
                "[Shrouded Knowledge] Phase 2 initiated. Objective: "
                        + target.getName()
                        + ", manifestation: " + manifestation
                        + ", estimated arrival: " + ATTACK_ARRIVAL_DAYS
                        + " days."
        );
    }

    private void advanceDwellerAttack() {

        if (!Boolean.TRUE.equals(data.get(ATTACK_STARTED_KEY))) {
            return;
        }

        String status = (String) data.get(
                ShroudedDwellerAttackIntel.STATUS_KEY
        );

        if (ShroudedDwellerAttackIntel.STATUS_ARRIVED.equals(status)) {
            advanceOrbitalDefense();
            return;
        }

        if (ShroudedDwellerAttackIntel.STATUS_BOMBARDING.equals(status)) {
            executeSaturationBombardment();
            return;
        }

        if (!ShroudedDwellerAttackIntel.STATUS_PREPARING.equals(status)) {
            return;
        }

        Object savedElapsed = data.get(ATTACK_ELAPSED_KEY);

        float elapsed = savedElapsed instanceof Number
                ? ((Number) savedElapsed).floatValue()
                : 0f;

        elapsed += 1f;
        data.put(ATTACK_ELAPSED_KEY, elapsed);

        float remaining = Math.max(0f, ATTACK_ARRIVAL_DAYS - elapsed);

        data.put(
                ShroudedDwellerAttackIntel.ARRIVAL_DAYS_KEY,
                remaining
        );

        if (elapsed >= ATTACK_ARRIVAL_DAYS) {
            deployDwellerAttack();
        }
    }

    private void deployDwellerAttack() {
        if (!Boolean.TRUE.equals(data.get(ATTACK_STARTED_KEY))) {
            return;
        }

        String status = (String) data.get(
                ShroudedDwellerAttackIntel.STATUS_KEY
        );

        if (!ShroudedDwellerAttackIntel.STATUS_PREPARING.equals(status)) {
            return;
        }

        SectorAPI sector = Global.getSector();

        Object savedMarketId = data.get(
                ShroudedDwellerAttackIntel.TARGET_MARKET_KEY
        );

        if (!(savedMarketId instanceof String)) {
            Global.getLogger(ShroudedKnowledgeManager.class).error(
                    "[Shrouded Knowledge] No colony ID saved."
            );
            return;
        }

        MarketAPI target = sector.getEconomy().getMarket(
                (String) savedMarketId
        );

        if (target == null || target.getPrimaryEntity() == null
                || target.getStarSystem() == null) {
            Global.getLogger(ShroudedKnowledgeManager.class).error(
                    "[Shrouded Knowledge] Could not be recovered "
                            + "the target colony or its system."
            );
            return;
        }

        CampaignFleetAPI stationFleet = Misc.getStationFleet(target);

        if (stationFleet != null) {
            data.put(
                    ShroudedDwellerAttackListener.STATION_FLEET_ID_KEY,
                    stationFleet.getId()
            );

            data.put(
                    ShroudedDwellerAttackListener.STATION_DEFEATED_KEY,
                    false
            );

            Global.getLogger(ShroudedKnowledgeManager.class).info(
                    "[Shrouded Knowledge] Orbital station registered. ID: "
                            + stationFleet.getId()
            );

        } else {
            data.remove(
                    ShroudedDwellerAttackListener.STATION_FLEET_ID_KEY
            );

            data.put(
                    ShroudedDwellerAttackListener.STATION_DEFEATED_KEY,
                    false
            );

            Global.getLogger(ShroudedKnowledgeManager.class).info(
                    "[Shrouded Knowledge]No fleet was found "
                            + "of an operational orbital station."
            );
        }

        Object savedFleetId = data.get(
                ShroudedDwellerAttackIntel.FLEET_ID_KEY
        );

        if (savedFleetId instanceof String) {
            SectorEntityToken existing = sector.getEntityById(
                    (String) savedFleetId
            );

            if (existing instanceof CampaignFleetAPI) {
                CampaignFleetAPI existingFleet =
                        (CampaignFleetAPI) existing;

                if (Boolean.TRUE.equals(
                        existingFleet.getMemoryWithoutUpdate().get(
                                ShroudEliteFleetFactory.FLEET_MARKER))) {

                    data.put(
                            ShroudedDwellerAttackIntel.STATUS_KEY,
                            ShroudedDwellerAttackIntel.STATUS_ARRIVED
                    );

                    ensureAttackIntelExists();
                    return;
                }
            }
        }

        Object savedType = data.get(
                ShroudedDwellerAttackIntel.MANIFESTATION_KEY
        );

        int manifestation = savedType instanceof Number
                ? ((Number) savedType).intValue()
                : 1;

        Random random = new Random();

        CampaignFleetAPI fleet =
                ShroudEliteFleetFactory.createFleet(
                        manifestation,
                        random
                );

        if (fleet == null) {
            Global.getLogger(ShroudedKnowledgeManager.class).error(
                    "[Shrouded Knowledge] Could not be created "
                            + "the strike fleet."
            );
            return;
        }

        data.put(
                ATTACK_FACTION_ID_KEY,
                fleet.getFaction().getId()
        );

        LocationAPI system = target.getStarSystem();
        SectorEntityToken colonyEntity = target.getPrimaryEntity();

        SectorEntityToken attackTarget = stationFleet != null
                ? stationFleet
                : colonyEntity;

        system.addEntity(fleet);

        float angle = random.nextFloat() * 360f;
        float distance = 1500f + random.nextFloat() * 500f;

        float x = colonyEntity.getLocation().x
                + (float) Math.cos(Math.toRadians(angle)) * distance;

        float y = colonyEntity.getLocation().y
                + (float) Math.sin(Math.toRadians(angle)) * distance;

        fleet.setLocation(x, y);

        fleet.getMemoryWithoutUpdate().set(
                MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                true
        );

        fleet.addAssignment(
                FleetAssignment.INTERCEPT,
                attackTarget,
                9999f,
                stationFleet != null
                        ? "Attacking orbital station"
                        : "Attacking colony"
        );

        int supportCount = calculateSupportFleets(
                target,
                manifestation
        );

        java.util.List<String> supportIds =
                new java.util.ArrayList<>();

        for (int i = 0; i < supportCount; i++) {

            int supportManifestation =
                    manifestation >= 3 ? 2 : 1;

            CampaignFleetAPI supportFleet =
                    ShroudedDwellerAttackFactory.createFleet(
                            supportManifestation,
                            random
                    );

            if (supportFleet == null) {
                Global.getLogger(
                        ShroudedKnowledgeManager.class
                ).warn(
                        "[Shrouded Knowledge] Could not be created "
                                + "the reinforcement " + (i + 1)
                );
                continue;
            }

            supportFleet.setName(
                    "Shrouded Dweller Support Fleet"
            );

            supportFleet.getMemoryWithoutUpdate().set(
                    SUPPORT_FLEET_MARKER,
                    true
            );

            boolean deployed =
                    ShroudedDwellerAttackFactory.deployNearColony(
                            supportFleet,
                            colonyEntity,
                            1800f + random.nextFloat() * 500f,
                            random
                    );

            if (!deployed) {
                Global.getLogger(
                        ShroudedKnowledgeManager.class
                ).warn(
                        "[Shrouded Knowledge] Could not be deployed "
                                + "the reinforcement " + (i + 1)
                );

                continue;
            }

            supportFleet.getMemoryWithoutUpdate().set(
                    MemFlags.MEMORY_KEY_MAKE_HOSTILE,
                    true
            );

            supportFleet.addAssignment(
                    FleetAssignment.INTERCEPT,
                    attackTarget,
                    9999f,
                    "supporting the Dweller assault"
            );

            supportIds.add(supportFleet.getId());

            Global.getLogger(
                    ShroudedKnowledgeManager.class
            ).info(
                    "[Shrouded Knowledge] reinforcement "
                            + (i + 1) + "/" + supportCount
                            + " displayed. ID: "
                            + supportFleet.getId()
            );
        }

        data.put(SUPPORT_FLEET_IDS_KEY, supportIds);

        Global.getLogger(
                ShroudedKnowledgeManager.class
        ).info(
                "[Shrouded Knowledge] Full shipment: "
                        + "1 flota élite y "
                        + supportIds.size()
                        + " support fleets."
        );

        data.put(
                ShroudedDwellerAttackIntel.FLEET_ID_KEY,
                fleet.getId()
        );

        data.put(
                ShroudedDwellerAttackIntel.STATUS_KEY,
                ShroudedDwellerAttackIntel.STATUS_ARRIVED
        );

        data.put(
                ShroudedDwellerAttackIntel.ARRIVAL_DAYS_KEY,
                0f
        );

        ensureAttackIntelExists();

        if (sector.getCampaignUI() != null) {
            sector.getCampaignUI().addMessage(
                    "Shrouded Knowledge: a manifestation "
                            + "An extradimensional entity has appeared in the system. "
                            + "of " + target.getName() + "."
            );
        }

        Global.getLogger(ShroudedKnowledgeManager.class).info(
                "[Shrouded Knowledge] Expedition deployed. "
                        + "Colony: " + target.getName()
                        + ", fleet: " + fleet.getId()
                        + ", manifestation: " + manifestation
        );
    }

    private int calculateSupportFleets(
            MarketAPI market,
            int manifestation) {

        if (market == null) {
            return 0;
        }

        int size = market.getSize();
        double income = Math.max(0d, market.getNetIncome());

        double difficulty = size + income / 50000d;

        if (manifestation == 1) {
            return difficulty >= 8d ? 1 : 0;
        }

        if (manifestation == 2) {
            return difficulty >= 10d ? 2 : 1;
        }

        return difficulty >= 12d ? 3 : 2;
    }

    private void advanceOrbitalDefense() {

        Object savedMarketId = data.get(
                ShroudedDwellerAttackIntel.TARGET_MARKET_KEY
        );

        if (!(savedMarketId instanceof String)) {
            return;
        }

        MarketAPI target = Global.getSector()
                .getEconomy()
                .getMarket((String) savedMarketId);

        if (target == null) {
            return;
        }

        Object savedStationId = data.get(
                ShroudedDwellerAttackListener.STATION_FLEET_ID_KEY
        );

        if (!(savedStationId instanceof String)) {

            data.put(
                    ShroudedDwellerAttackListener.STATION_DEFEATED_KEY,
                    true
            );

            data.put(
                    ShroudedDwellerAttackIntel.STATUS_KEY,
                    ShroudedDwellerAttackIntel.STATUS_BOMBARDING
            );

            Global.getLogger(ShroudedKnowledgeManager.class).info(
                    "[Shrouded Knowledge] The colony didn't have "
                            + "an orbital station. It can be started"
                            + "the next phase."
            );

            return;
        }

        if (!Boolean.TRUE.equals(
                data.get(ShroudedDwellerAttackListener.STATION_DEFEATED_KEY))) {
            return;
        }

        Industry station = Misc.getStationIndustry(target);

        if (station != null && station.getDisruptedDays() <= 0f) {
            return;
        }

        data.put(
                ShroudedDwellerAttackIntel.STATUS_KEY,
                ShroudedDwellerAttackIntel.STATUS_BOMBARDING
        );

        Global.getLogger(ShroudedKnowledgeManager.class).info(
                "[Shrouded Knowledge] Orbital defenses neutralized. "
                        + "The expedition can begin the bombardment."
        );

        if (Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(
                    "Shrouded Knowledge: Orbital defenses "
                            + "have been neutralized. The colony "
                            + "it is exposed."
            );
        }
    }

    private void executeSaturationBombardment() {

        if (Boolean.TRUE.equals(data.get(BOMBARDMENT_EXECUTED_KEY))) {
            return;
        }

        Object savedMarketId = data.get(
                ShroudedDwellerAttackIntel.TARGET_MARKET_KEY
        );

        if (!(savedMarketId instanceof String)) {
            Global.getLogger(ShroudedKnowledgeManager.class).error(
                    "[Shrouded Knowledge] Colony ID not found."
            );
            return;
        }

        MarketAPI target = Global.getSector()
                .getEconomy()
                .getMarket((String) savedMarketId);

        if (target == null || target.getPrimaryEntity() == null) {
            Global.getLogger(ShroudedKnowledgeManager.class).error(
                    "[Shrouded Knowledge] The target colony does not exist."
            );
            return;
        }

        Object savedFactionId = data.get(ATTACK_FACTION_ID_KEY);

        if (!(savedFactionId instanceof String)) {
            Global.getLogger(ShroudedKnowledgeManager.class).error(
                    "[Shrouded Knowledge] The attacking faction was not found."
            );
            return;
        }

        FactionAPI attackFaction = Global.getSector().getFaction(
                (String) savedFactionId
        );

        if (attackFaction == null) {
            Global.getLogger(ShroudedKnowledgeManager.class).error(
                    "[Shrouded Knowledge] The attacking faction could not be retrieved."
            );
            return;
        }

        MarketCMD marketCMD = new MarketCMD(target.getPrimaryEntity());

        marketCMD.doBombardment(
                attackFaction,
                MarketCMD.BombardType.SATURATION
        );

        data.put(BOMBARDMENT_EXECUTED_KEY, true);

        data.put(
                ShroudedDwellerAttackIntel.STATUS_KEY,
                ShroudedDwellerAttackIntel.STATUS_COMPLETED
        );

        Global.getLogger(ShroudedKnowledgeManager.class).info(
                "[Shrouded Knowledge] ¡Saturation bombardment complete! "
                        + "Colony: " + target.getName()
        );

        if (Global.getSector().getCampaignUI() != null) {
            Global.getSector().getCampaignUI().addMessage(
                    "Shrouded Knowledge: The colony "
                            + target.getName()
                            + " it has been subjected to saturation bombing."
            );
        }
    }


    public void testThreatPoints() {
        addThreatPoints(1, "Test");
    }


    public boolean isPreparationStarted() {
        return Boolean.TRUE.equals(data.get(PREPARATION_STARTED_KEY));
    }

    public boolean isFirstPhaseActivated() {
        return Boolean.TRUE.equals(data.get(FIRST_PHASE_KEY));
    }

    public boolean isAttackStarted() {
        return Boolean.TRUE.equals(data.get(ATTACK_STARTED_KEY));
    }
}












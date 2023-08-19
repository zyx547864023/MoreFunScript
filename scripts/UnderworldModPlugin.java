package real_combat.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PersonImportance;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.ShipAIConfig;
import com.fs.starfarer.api.combat.ShipAIPlugin;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import com.fs.starfarer.api.impl.campaign.procgen.ProcgenUsedNames;
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator;
import com.thoughtworks.xstream.XStream;
import data.scripts.campaign.UW_MarketRiggerScript;
import data.scripts.campaign.econ.UW_CabalInfluence;
import data.scripts.campaign.events.UW_EventManager;
import data.scripts.campaign.fleets.UW_CabalFleetManager;
import data.scripts.campaign.fleets.UW_PalaceFleet;
import data.scripts.campaign.intel.bar.CabalBarEventCreator;
import data.scripts.campaign.submarkets.UW_CabalMarketPlugin;
import data.scripts.campaign.submarkets.UW_ScrapyardMarketPlugin;
import data.scripts.util.UW_Util;
import data.scripts.world.underworld.UW_DickersonAssignmentAI;
import data.scripts.world.underworld.UW_DickersonFleetManager;
import data.scripts.world.underworld.UW_Styx;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Level;
import org.json.JSONException;
import org.json.JSONObject;

public class UnderworldModPlugin extends BaseModPlugin {

    // Both of these are used only when LunaLib is not available.
    private static boolean isStarlightCabalEnabled = true;
    private static float cabalFleetFactor = 1f;

    public static boolean hasGraphicsLib = false;
    public static boolean hasMagicLib = false;
    public static boolean hasSWP = false;
    public static boolean hasIndEvo = false;
    public static boolean isExerelin = false;

    private static final String SETTINGS_FILE = "UNDERWORLD_OPTIONS.ini";

    private static final Set<String> CURSED = new HashSet<>();

    static {
        CURSED.add("uw_boss_cancer");
        CURSED.add("uw_boss_corruption");
        CURSED.add("uw_boss_cyst");
        CURSED.add("uw_boss_disease");
        CURSED.add("uw_boss_malignancy");
        CURSED.add("uw_boss_metastasis");
        CURSED.add("uw_boss_pustule");
        CURSED.add("uw_boss_tumor");
        CURSED.add("uw_boss_ulcer");
    }

    public static void syncUnderworldScripts() {
        if (!Global.getSector().hasScript(UW_CabalFleetManager.class)) {
            Global.getSector().addScript(new UW_CabalFleetManager());
        }
        syncCabalMarkets();

        if (!Global.getSector().hasScript(UW_MarketRiggerScript.class)) {
            Global.getSector().addScript(new UW_MarketRiggerScript());
        }
        if (!Global.getSector().hasScript(UW_PalaceFleet.class)) {
            Global.getSector().addScript(new UW_PalaceFleet());
        }
    }

    public static void syncUnderworldScriptsExerelin() {
        if (!Global.getSector().hasScript(UW_CabalFleetManager.class)) {
            Global.getSector().addScript(new UW_CabalFleetManager());
        }
        syncCabalMarkets();

        if (!Global.getSector().hasScript(UW_MarketRiggerScript.class)) {
            Global.getSector().addScript(new UW_MarketRiggerScript());
        }
        if (!Global.getSector().hasScript(UW_PalaceFleet.class)) {
            Global.getSector().addScript(new UW_PalaceFleet());
        }
    }

    private static void initUnderworldRelationships(SectorAPI sector) {
        FactionAPI tritachyon = sector.getFaction(Factions.TRITACHYON);
        FactionAPI pirates = sector.getFaction(Factions.PIRATES);
        FactionAPI kol = sector.getFaction(Factions.KOL);
        FactionAPI church = sector.getFaction(Factions.LUDDIC_CHURCH);
        FactionAPI path = sector.getFaction(Factions.LUDDIC_PATH);
        FactionAPI player = sector.getFaction(Factions.PLAYER);
        FactionAPI cabal = sector.getFaction("cabal");

        List<FactionAPI> allFactions = sector.getAllFactions();
        for (FactionAPI curFaction : allFactions) {
            if (curFaction == cabal || curFaction.isNeutralFaction()) {
                continue;
            }
            cabal.setRelationship(curFaction.getId(), RepLevel.HOSTILE);
        }

        cabal.setRelationship(tritachyon.getId(), RepLevel.FAVORABLE);
        cabal.setRelationship("blackrock_driveyards", RepLevel.NEUTRAL);
        cabal.setRelationship("exigency", RepLevel.SUSPICIOUS);
        cabal.setRelationship("exipirated", RepLevel.SUSPICIOUS);
        cabal.setRelationship("templars", RepLevel.SUSPICIOUS);
        cabal.setRelationship(pirates.getId(), RepLevel.SUSPICIOUS);
        cabal.setRelationship(kol.getId(), RepLevel.VENGEFUL);
        cabal.setRelationship(church.getId(), RepLevel.VENGEFUL);
        cabal.setRelationship(path.getId(), RepLevel.VENGEFUL);
        player.setRelationship(cabal.getId(), -0.65f);
    }

    private static void loadSettingsFile() throws IOException, JSONException {
        JSONObject settings = Global.getSettings().loadJSON(SETTINGS_FILE);

        isStarlightCabalEnabled = settings.getBoolean("starlightCabal");
        cabalFleetFactor = (float) settings.getDouble("cabalFleetFactor");
    }

    private static void syncCabalMarkets() {
        String cabalMarkets[] = new String[]{"port_tse", "tibicena"};
        if (isStarlightCabalEnabled()) {
            Global.getSector().getFaction("cabal").setShowInIntelTab(true);
            for (String marketStr : cabalMarkets) {
                MarketAPI market = Global.getSector().getEconomy().getMarket(marketStr);
                if (market == null) {
                    // Handles non-Corvus Mode Nexerelin
                    continue;
                }
                if (!market.hasCondition("cabal_influence")) {
                    market.addCondition("cabal_influence");
                    market.addSubmarket("uw_cabalmarket");
                    UW_Util.setMarketInfluence(market, "cabal");
                }
            }
        } else {
            Global.getSector().getFaction("cabal").setShowInIntelTab(false);
            for (String marketStr : cabalMarkets) {
                MarketAPI market = Global.getSector().getEconomy().getMarket(marketStr);
                if (market == null) {
                    continue;
                }
                if (market.hasCondition("cabal_influence")) {
                    market.removeCondition("cabal_influence");
                    market.removeSubmarket("uw_cabalmarket");
                    UW_Util.removeMarketInfluence(market, "cabal");
                }
            }
        }
    }

    public static boolean isStarlightCabalEnabled() {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            return LunaSettings.getBoolean("underworld", "isStarlightCabalEnabled");
        }
        return isStarlightCabalEnabled;
    }

    public static float getCabalFleetFactor() {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            return LunaSettings.getFloat("underworld", "cabalFleetFactor");
        }
        return cabalFleetFactor;
    }

    @Override
    public void configureXStream(XStream x) {
        x.alias("UW_CabalFleetManager", UW_CabalFleetManager.class);
        x.alias("UW_CabalInfluence", UW_CabalInfluence.class);
        x.alias("UW_CabalMarketPlugin", UW_CabalMarketPlugin.class);
        x.alias("UW_Styx", UW_Styx.class);
        x.alias("UW_DickersonFleetManager", UW_DickersonFleetManager.class);
        x.alias("UW_DickersonAssignmentAI", UW_DickersonAssignmentAI.class);
        x.alias("UW_ScrapyardMarketPlugin", UW_ScrapyardMarketPlugin.class);
        x.alias("UW_EventManager", UW_EventManager.class);
        x.alias("UW_PalaceFleet", UW_PalaceFleet.class);
    }

    @Override
    public void onApplicationLoad() throws Exception {
        hasGraphicsLib = Global.getSettings().getModManager().isModEnabled("shaderLib");
        if (hasGraphicsLib) {
            UW_ModPluginAlt.initGraphicsLib();
        }

        hasMagicLib = Global.getSettings().getModManager().isModEnabled("MagicLib");
        isExerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        hasSWP = Global.getSettings().getModManager().isModEnabled("swp");
        hasIndEvo = Global.getSettings().getModManager().isModEnabled("IndEvo");

        try {
            loadSettingsFile();
        } catch (IOException | JSONException e) {
            Global.getLogger(UnderworldModPlugin.class).log(Level.ERROR, "Failed to load settings file! " + e.getMessage());
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        if (isExerelin) {
            syncUnderworldScriptsExerelin();
        } else {
            syncUnderworldScripts();
        }
        BarEventManager bar = BarEventManager.getInstance();
        if (!bar.hasEventCreator(CabalBarEventCreator.class)) {
            bar.addEventCreator(new CabalBarEventCreator());
        }
    }

    @Override
    public void onNewGame() {
        initUnderworldRelationships(Global.getSector());

        ProcgenUsedNames.notifyUsed("Styx");
        ProcgenUsedNames.notifyUsed("Arigato");
        ProcgenUsedNames.notifyUsed("Roboto");

        if (isExerelin) {
            syncUnderworldScriptsExerelin();
        } else {
            syncUnderworldScripts();
        }
        Global.getSector().addScript(new UW_EventManager());

        UW_ModPluginAlt.initUW();
    }

    @Override
    public void onNewGameAfterProcGen() {
        UW_Styx.generatePt2(Global.getSector());
    }

    @Override
    public void onNewGameAfterEconomyLoad() {
        ImportantPeopleAPI ip = Global.getSector().getImportantPeople();
        UW_Styx.dickersonifyBaseStaff();

        MarketAPI market = Global.getSector().getEconomy().getMarket("uw_arigato");
        if (market != null) {
            PersonAPI admin = market.getFaction().createRandomPerson();
            admin.setPostId(Ranks.POST_ADMINISTRATOR);
            admin.setRankId(Ranks.SPACE_ADMIRAL);
            admin.getName().setLast("Dickerson");
            admin.setPortraitSprite("graphics/uw/portraits/uw_dickerson_extended_family.png");
            admin.setImportanceAndVoice(PersonImportance.HIGH, StarSystemGenerator.random);

            admin.getStats().setSkillLevel(Skills.INDUSTRIAL_PLANNING, 1);

            for (PersonAPI p : market.getPeopleCopy()) {
                if (Ranks.POST_ADMINISTRATOR.equals(p.getPostId())) {
                    market.removePerson(p);
                    ip.removePerson(p);
                    market.getCommDirectory().removePerson(p);
                    break;
                }
            }

            market.setAdmin(admin);
            market.getCommDirectory().addPerson(admin, 0);
            market.addPerson(admin);
        }
    }

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        if (ship.isFighter()) {
            return null;
        }

        String hullId = ship.getHullSpec().getBaseHullId();
        if (!CURSED.contains(hullId)) {
            return null;
        }

        ShipAIConfig config = new ShipAIConfig();
        config.backingOffWhileNotVentingAllowed = false;
        config.burnDriveIgnoreEnemies = true;
        config.personalityOverride = Personalities.RECKLESS;

        return new PluginPick<>(Global.getSettings().createDefaultShipAI(ship, config), PickPriority.MOD_SPECIFIC);
    }
}

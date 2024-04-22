package real_combat;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import exerelin.campaign.SectorManager;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Level;
import org.json.JSONException;
import real_combat.ai.RC_Drone_borerAI;
import real_combat.campaign.RC_CampaignPlugin;
//import real_combat.campaign.missions.intel.bar.events.FamousShipBarEventCreator;
import real_combat.combat.RC_ComboEveryFrameCombatPlugin;
import real_combat.combat.RC_MonsterBallEveryFrameCombatPlugin;
import real_combat.combat.RC_SmartAIEveryFrameCombatPlugin;
import real_combat.weapons.ai.*;
import real_combat.world.RCWorldGen;

import java.io.IOException;
import java.io.InputStream;


public class RCModPlugin extends BaseModPlugin {
    public static final String RC_MONSTERBALL_ID = "reaper_torp_ball";
    public static final String RC_MONSTER_BALL_SHOOTER_ID = "monster_ball_shooter";
    public static final String RC_MONSTER_BALL_SHOOTER_BIG_ID = "monster_ball_shooter_big";
    public static final String RC_CAMOUFLAGE_NET_ID = "camouflage_net";
    public static final String RC_CAMOUFLAGE_NET_MRM_ID = "camouflage_net_mrm";
    public static final String RC_KUNAI_ID = "kunai_mrm";
    public static final String RC_EXPLOSIVE_CHAIN_ID = "explosive_chain_mrm";
    @Override
    public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
        if (RC_MONSTER_BALL_SHOOTER_ID.contentEquals(weapon.getId())
        ||RC_MONSTER_BALL_SHOOTER_BIG_ID.contentEquals(weapon.getId())
        ) {
            return new PluginPick<AutofireAIPlugin>(new RC_MonsterBallShoterAI(weapon),
                    CampaignPlugin.PickPriority.MOD_SET);
        }
        if (RC_CAMOUFLAGE_NET_ID.contentEquals(weapon.getId())
        ) {
            return new PluginPick<AutofireAIPlugin>(new RC_CamouflageNetShoterAI(weapon),
                    CampaignPlugin.PickPriority.MOD_SET);
        }
        return null;
    }

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        if (RC_MONSTERBALL_ID.contentEquals(missile.getProjectileSpecId())) {
            return new PluginPick<MissileAIPlugin>(new RC_MonsterBallAI(missile, launchingShip),
                    PickPriority.MOD_SET);
        }
        if (RC_CAMOUFLAGE_NET_MRM_ID.contentEquals(missile.getProjectileSpecId())) {
            return new PluginPick<MissileAIPlugin>(new RC_CamouflageNetAI(missile, launchingShip),
                    PickPriority.MOD_SET);
        }
        if (RC_KUNAI_ID.contentEquals(missile.getProjectileSpecId())) {
            return new PluginPick<MissileAIPlugin>(new RC_KunaiAI(missile, launchingShip),
                    PickPriority.MOD_SET);
        }
        if (RC_EXPLOSIVE_CHAIN_ID.contentEquals(missile.getProjectileSpecId())) {
            return new PluginPick<MissileAIPlugin>(new RC_ExplosiveChainAI(missile, launchingShip),
                    PickPriority.MOD_SET);
        }
        return null;
    }

    @Override
    public PluginPick<ShipAIPlugin> pickShipAI(FleetMemberAPI member, ShipAPI ship) {
        return ship.getHullSpec().getHullId().startsWith("RC_drone_borer") ? new PluginPick(new RC_Drone_borerAI(ship), PickPriority.MOD_SPECIFIC) : null;
    }

    @Override
    public void onApplicationLoad() {
        try {
            RC_ComboEveryFrameCombatPlugin.reloadSettings();
        } catch (IOException | JSONException e) {
            Global.getLogger(RCModPlugin.class).log(Level.ERROR, "ComboKey load failed: " + e.getMessage());
        }
        try {
            RC_MonsterBallEveryFrameCombatPlugin.reloadSettings();
        } catch (Exception e) {
            Global.getLogger(RCModPlugin.class).log(Level.ERROR, "ComboKey load failed: " + e.getMessage());
        }
        try {
            RC_SmartAIEveryFrameCombatPlugin.reloadSettings();
        } catch (Exception e) {
            Global.getLogger(RCModPlugin.class).log(Level.ERROR, "ComboKey load failed: " + e.getMessage());
        }
    }

    @Override
    public void onGameLoad(boolean newGame) {
        /*
        BarEventManager bar = BarEventManager.getInstance();
        if (!bar.hasEventCreator(FamousShipBarEventCreator.class)) {
            bar.addEventCreator(new FamousShipBarEventCreator());
        }        */
        Global.getSector().registerPlugin(new RC_CampaignPlugin());
    }

    public static boolean isWarningEnabled = true;
    public static boolean isRangeDamageEnabled = true;
    public static boolean isEscortAccelerationEnabled = true;
    public static boolean isQEEnabled = true;
    public static boolean isSmartAIEnabled = true;
    public static boolean isSightRadiusEnabled = true;
    public static boolean isEasyMode = true;
    public static final String MOD_ID = "MoreFun";
    public static boolean isWarningEnabled() {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            return LunaSettings.getBoolean(MOD_ID, "isWarningEnabled");
        }
        return isWarningEnabled;
    }
    public static boolean isRangeDamageEnabled() {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            return LunaSettings.getBoolean(MOD_ID, "isRangeDamageEnabled");
        }
        return isRangeDamageEnabled;
    }
    public static boolean isEscortAccelerationEnabled() {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            return LunaSettings.getBoolean(MOD_ID, "isEscortAccelerationEnabled");
        }
        return isEscortAccelerationEnabled;
    }
    public static boolean isQEEnabled() {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            return LunaSettings.getBoolean(MOD_ID, "isQEEnabled");
        }
        return isQEEnabled;
    }

    public static boolean isSmartAIEnabled() {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            return LunaSettings.getBoolean(MOD_ID, "isSmartAIEnabled");
        }
        return isSmartAIEnabled;
    }

    public static boolean isSightRadiusEnabled() {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            return LunaSettings.getBoolean(MOD_ID, "isSightRadiusEnabled");
        }
        return isSightRadiusEnabled;
    }

    public static boolean isEasyMode() {
        if (Global.getSettings().getModManager().isModEnabled("lunalib")) {
            return LunaSettings.getBoolean(MOD_ID, "isEasyMode");
        }
        return isEasyMode;
    }

    @Override
    public void onNewGame() {
        //Nex compatibility setting, if there is no nex or corvus mode(Nex), just generate the system
        boolean haveNexerelin = Global.getSettings().getModManager().isModEnabled("nexerelin");
        if (!haveNexerelin || SectorManager.getCorvusMode()) {
            new RCWorldGen().generate(Global.getSector());
        }
    }
}

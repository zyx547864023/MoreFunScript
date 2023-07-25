package real_combat;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import org.apache.log4j.Level;
import org.json.JSONException;
import real_combat.ai.RC_Drone_borerAI;
import real_combat.campaign.RC_CampaignPlugin;
import real_combat.campaign.missions.intel.bar.events.FamousShipBarEventCreator;
import real_combat.combat.RC_ComboEveryFrameCombatPlugin;
import real_combat.combat.RC_MonsterBallEveryFrameCombatPlugin;
import real_combat.weapons.ai.RC_MonsterBallAI;
import real_combat.weapons.ai.RC_MonsterBallShoterAI;

import java.io.IOException;
import java.io.InputStream;


public class RCModPlugin extends BaseModPlugin {
    public static final String RC_MONSTERBALL_ID = "reaper_torp_ball";
    public static final String RC_MONSTER_BALL_SHOOTER_ID = "monster_ball_shooter";
    public static final String RC_MONSTER_BALL_SHOOTER_BIG_ID = "monster_ball_shooter_big";

    @Override
    public PluginPick<AutofireAIPlugin> pickWeaponAutofireAI(WeaponAPI weapon) {
        if (RC_MONSTER_BALL_SHOOTER_ID.contentEquals(weapon.getId())
        ||RC_MONSTER_BALL_SHOOTER_BIG_ID.contentEquals(weapon.getId())
        ) {
            return new PluginPick<AutofireAIPlugin>(new RC_MonsterBallShoterAI(weapon),
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
    }

    @Override
    public void onGameLoad(boolean newGame) {
        BarEventManager bar = BarEventManager.getInstance();
        if (!bar.hasEventCreator(FamousShipBarEventCreator.class)) {
            bar.addEventCreator(new FamousShipBarEventCreator());
        }
        Global.getSector().registerPlugin(new RC_CampaignPlugin());
    }
}

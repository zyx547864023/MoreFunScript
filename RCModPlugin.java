package real_combat;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager;
import org.apache.log4j.Level;
import org.json.JSONException;
import real_combat.campaign.RC_CampaignPlugin;
import real_combat.campaign.missions.intel.bar.events.FamousShipBarEventCreator;
import real_combat.combat.RC_ComboEveryFrameCombatPlugin;
import real_combat.weapons.ai.RC_MonsterBallAI;

import java.io.IOException;


public class RCModPlugin extends BaseModPlugin {
    public static final String RC_MONSTERBALL_ID = "reaper_torp_ball";

    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        if (RC_MONSTERBALL_ID.contentEquals(missile.getProjectileSpecId())) {
            return new PluginPick<MissileAIPlugin>(new RC_MonsterBallAI(missile, launchingShip),
                    PickPriority.MOD_SET);
        }
        return null;
    }

    @Override
    public void onApplicationLoad() {
        try {
            RC_ComboEveryFrameCombatPlugin.reloadSettings();
        } catch (IOException | JSONException e) {
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

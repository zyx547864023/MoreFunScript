package real_combat;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.CampaignPlugin.PickPriority;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import real_combat.weapons.ai.RC_MonsterBallAI;


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
}

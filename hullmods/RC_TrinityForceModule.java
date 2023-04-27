package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;

import java.util.List;
import java.util.Map;

/**
 * 合体船的速度计算
 */
public class RC_TrinityForceModule extends BaseHullMod {
    private String ID = "RC_TrinityForceModule";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(ID, 0);
        if(stats.getFleetMember()!=null) {
            if(stats.getFleetMember().getVariant()!=null) {
                try {
                    //if(!stats.getFleetMember().getFleetData().getFleet().isPlayerFleet()) {
                    //如果船没装在模块上
                    //stats.getFleetMember().getVariant().removePermaMod(ID);
                    //}
                }
                catch (Exception e)
                {
                    Global.getLogger(this.getClass()).info(e);
                }

            }
        }
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
    }

    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        ship.getMutableStats().getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(ID, 0);
    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.isStationModule();
    }
    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return null;
    }
}

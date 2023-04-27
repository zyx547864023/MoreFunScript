package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.VariantSource;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 合体船的速度计算
 */
public class RC_TrinityForceCore extends BaseHullMod {
    private String ID = "RC_TrinityForceCore";
    private String RC_TRINITYFORCEMODULE = "RC_TrinityForceModule";
    private String CONVERTED_FIGHTERBAY = "converted_fighterbay";
    //private String CONVERTED_HANGAR = "converted_hangar";
    private static String MAX_SPEED = "MAX_SPEED";
    private static String MAX_TURN_RATE = "MAX_TURN_RATE";
    private static String MAX_TURN_ACCELERATION = "MAX_TURN_ACCELERATION";
    private static String MAX_ACCELERATION = "MAX_TURN_ACCELERATION";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getChildModulesCopy()!=null) {
            List<String> slotIds = ship.getVariant().getModuleSlots();
            for (String s:slotIds)
            {
                ShipVariantAPI variant =  ship.getVariant().getModuleVariant(s);
                //模块组件
                variant.addPermaMod(RC_TRINITYFORCEMODULE);
                //内置禁用甲板
                variant.addPermaMod(CONVERTED_FIGHTERBAY);
                //扩编改为内置
                variant.removeMod(HullMods.EXPANDED_DECK_CREW);
                /*
                if(variant.hasHullMod(HullMods.EXPANDED_DECK_CREW)) {
                    variant.removeMod(HullMods.EXPANDED_DECK_CREW);
                    variant.addPermaMod(HullMods.EXPANDED_DECK_CREW);
                }
                 */
                //移除改装机库
                variant.removeMod("converted_hangar");
                variant.removePermaMod("converted_hangar");
            }
            /*
            for (ShipAPI m:ship.getChildModulesCopy())
            {
                ship.setMass(ship.getMass()-m.getMass());
            }
             */
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {
        if(stats.getFleetMember()!=null)
        {
            float deploymentPointsCost = stats.getFleetMember().getHullSpec().getSuppliesPerMonth();
            if(stats.getFleetMember().getVariant()!=null)
            {
                if (stats.getFleetMember().getVariant()!=null) {
                    try {
                        List<String> slotIds = stats.getFleetMember().getVariant().getModuleSlots();
                        for (String s : slotIds) {
                            //stats.getFleetMember().getVariant().setModuleVariant(s,Global.getSettings().getVariant("onslaught_Standard"));
                            ShipVariantAPI variant = stats.getFleetMember().getVariant().getModuleVariant(s);
                            /*
                            if ("WS0002".equals(s)) {
                                stats.getFleetMember().getVariant().setSource(VariantSource.REFIT);
                                variant.setSource(VariantSource.REFIT);
                                ShipVariantAPI newVariant = Global.getSettings().getVariant("astral_Strike");
                                newVariant.setSource(VariantSource.REFIT);
                                stats.getFleetMember().getVariant().setModuleVariant(s, newVariant);
                            }
                            else if("WS0001".equals(s)) {
                                stats.getFleetMember().getVariant().setSource(VariantSource.REFIT);
                                variant.setSource(VariantSource.REFIT);
                                ShipVariantAPI newVariant = Global.getSettings().getVariant("odyssey_Balanced");
                                newVariant.setSource(VariantSource.REFIT);
                                stats.getFleetMember().getVariant().setModuleVariant(s, newVariant);
                            }
                            */
                            deploymentPointsCost += variant.getHullSpec().getSuppliesPerMonth();
                        }
                    }
                    catch (Exception e)
                    {

                    }
                }
            }
            stats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(ID, deploymentPointsCost/stats.getFleetMember().getHullSpec().getSuppliesPerMonth());
        }
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

    }

    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) {return;}
        //修改转速和最高速度跟最慢的模块一致
        if (ship.isShipWithModules()&&ship.isAlive()) {
            Map<String, Object> customData = ship.getCustomData();
            //修改转速和最高速度跟最慢的模块一致
            if (customData.get(MAX_SPEED)==null)
            {
                if(ship.getFluxTracker().getCurrFlux()>0) {
                    ship.setCustomData(MAX_SPEED, ship.getMutableStats().getMaxSpeed().getBaseValue());
                }
                else
                {
                    ship.setCustomData(MAX_SPEED, ship.getMutableStats().getMaxSpeed().getBaseValue()-50);
                }
                ship.setCustomData(MAX_TURN_RATE, ship.getMutableStats().getMaxTurnRate().getBaseValue());
                ship.setCustomData(MAX_TURN_ACCELERATION, ship.getMutableStats().getTurnAcceleration().getBaseValue());
                ship.setCustomData(MAX_ACCELERATION, ship.getMutableStats().getAcceleration().getBaseValue());
            }
            float minSpeed = 999f;
            float minTurnRate = 999f;
            float minTurnAcceleration = 999f;
            float minAcceleration = 999f;

            ShipAPI minShip = null;
            for (ShipAPI m:ship.getChildModulesCopy())
            {
                if (m.isAlive()&&!m.isPhased())
                {
                    MutableShipStatsAPI mstats = m.getMutableStats();
                    //修改部署点与
                    mstats.getDynamic().getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyPercent(ID, -200f);

                    if (mstats.getMaxSpeed().getBaseValue()<minSpeed){
                        if (m.getFluxTracker().getCurrFlux()>0) {
                            minSpeed = mstats.getMaxSpeed().getBaseValue();
                        }
                        else
                        {
                            minSpeed = mstats.getMaxSpeed().getBaseValue()-50;
                        }
                        minShip = m;
                    }
                    if (mstats.getMaxTurnRate().getBaseValue()<minTurnRate) {
                        minTurnRate = mstats.getMaxTurnRate().getBaseValue();
                    }
                    if (mstats.getMaxTurnRate().getBaseValue()<minTurnAcceleration)
                    {
                        minTurnAcceleration = mstats.getTurnAcceleration().getBaseValue();
                    }
                    if (mstats.getAcceleration().getBaseValue()<minAcceleration)
                    {
                        minAcceleration = mstats.getAcceleration().getBaseValue();
                    }
                }
            }

            MutableShipStatsAPI stats = ship.getMutableStats();

            if (minSpeed != 999f)
            {
                if (minShip!=null) {
                    stats.getMaxSpeed().setBaseValue(minShip.getMutableStats().getMaxSpeed().getBaseValue());
                }
                stats.getMaxTurnRate().setBaseValue(minTurnRate);
                stats.getTurnAcceleration().setBaseValue(minTurnAcceleration);
                stats.getAcceleration().setBaseValue(minAcceleration);
            }
            else {
                if (customData.get(MAX_SPEED)!=null) {
                    stats.getMaxSpeed().setBaseValue((Float) customData.get(MAX_SPEED) + 50);
                    stats.getMaxTurnRate().setBaseValue((Float) customData.get(MAX_TURN_RATE));
                    stats.getTurnAcceleration().setBaseValue((Float) customData.get(MAX_TURN_ACCELERATION));
                    stats.getAcceleration().setBaseValue((Float) customData.get(MAX_ACCELERATION));
                }
            }
        }
    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }
    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return null;
    }
}

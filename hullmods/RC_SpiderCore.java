package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.FighterOPCostModifier;
import com.fs.starfarer.api.combat.listeners.WeaponOPCostModifier;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.HullMods;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.loading.WeaponSpecAPI;
import com.fs.starfarer.api.loading.WingRole;
import real_combat.ai.RC_FighterAI;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 合体船的速度计算
 */
public class RC_SpiderCore extends BaseHullMod {
    public static String ID = "RC_SpiderCore";
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (ship.getChildModulesCopy()!=null) {
            List<String> slotIds = ship.getVariant().getModuleSlots();
            for (String s : slotIds) {
                ShipVariantAPI variant = ship.getVariant().getModuleVariant(s);
                //扩编改为内置
                variant.removePermaMod("RC_TrinityForceModule");
                variant.removePermaMod("converted_fighterbay");
            }
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

    }

    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        if (!ship.isPullBackFighters()) {
            for (FighterWingAPI w : ship.getAllWings()) {
                if (WingRole.FIGHTER.equals(w.getRole())) {
                    for (ShipAPI f:w.getWingMembers()) {
                        if (f.getCustomData().get("RC_FighterAI")==null&&f.isAlive()) {
                            f.setCustomData("RC_FighterAI",f.getShipAI());
                            f.setShipAI(new RC_FighterAI(f));
                        }
                    }
                }
            }
        }
        for (ShipAPI m : ship.getChildModulesCopy()) {
            if (m.getCustomData().get(ID)==null) {
                m.setCustomData(ID,ship);
            }
            if (!m.isPullBackFighters()) {
                for (FighterWingAPI w : m.getAllWings()) {
                    if (WingRole.FIGHTER.equals(w.getRole())) {
                        for (ShipAPI f:w.getWingMembers()) {
                            if (f.getCustomData().get("RC_FighterAI")==null&&f.isAlive()) {
                                f.setCustomData("RC_FighterAI",f.getShipAI());
                                f.setShipAI(new RC_FighterAI(f));
                            }
                        }
                    }
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

package real_combat.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.loading.WeaponSpecAPI;

import java.util.HashSet;
import java.util.Set;

/**
 * 通用运维
 */
public class RC_OPEX extends BaseHullMod {
    public static String ID = "RC_OPEX";
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {
        Set<WeaponSpecAPI> weaponSet = new HashSet<>();
        int weaponCount = 0;
        for (WeaponGroupSpec g:stats.getVariant().getWeaponGroups()) {
            for (String s:g.getSlots()) {
                weaponSet.add(stats.getVariant().getWeaponSpec(s));
                weaponCount++;
            }
        }
        if (weaponCount!=0&&weaponSet.size()!=0) {
            stats.getCRLossPerSecondPercent().modifyMult(id, 1 - ((float)weaponCount-(float)weaponSet.size()) / (float)weaponCount);
            stats.getPeakCRDuration().modifyMult(id, 1 + ((float)weaponCount-(float)weaponSet.size()) / (float)weaponCount);
        }
        Set<String> wingSet = new HashSet<>();
        int wingCount = 0;
        for (String w:stats.getVariant().getWings()) {
            wingCount++;
            wingSet.add(w);
        }
        if (wingCount!=0&&wingSet.size()!=0) {
            stats.getFighterRefitTimeMult().modifyMult(id, 1 - ((float)wingCount-(float)wingSet.size()) / (float)wingCount);
        }
    }

    @Override
    public void advanceInCampaign(FleetMemberAPI member, float amount) {

    }

    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {

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

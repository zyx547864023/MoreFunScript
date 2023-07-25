package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import org.lazywizard.lazylib.combat.DefenseUtils;

import java.util.Map;

/**
 */
public class RC_Fantasy extends BaseHullMod {
    private String ID = "RC_Fantasy";
    private static float HP_FIX = 1f;
    private static float ARMOR_FIX = 0.1f;
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if(ship!=null) {
            if(ship.getVariant()!=null) {
                try {
                    for (String h:ship.getVariant().getHullMods()) {
                        if (!h.equals(ID)) {
                            ship.getVariant().removePermaMod(h);
                        }
                    }

                }
                catch (Exception e)
                {
                    Global.getLogger(this.getClass()).info(e);
                }

            }
        }
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {
        if(stats.getFleetMember()!=null) {
            if(stats.getFleetMember().getVariant()!=null) {
                try {
                    for (String h:stats.getFleetMember().getVariant().getHullMods()) {
                        if (!h.equals(ID)) {
                            stats.getFleetMember().getVariant().removePermaMod(h);
                        }
                    }

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
        float hpFix = HP_FIX * amount;
        if (ship.getHitpoints() < ship.getMaxHitpoints() - hpFix) {
            ship.setHitpoints(ship.getHitpoints() + hpFix);
        } else {
            ship.setHitpoints(ship.getMaxHitpoints());
        }
        int count = 0;
        float[][] grid = ship.getArmorGrid().getGrid();
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                count++;
            }
        }
        float maxArmor = count * ship.getArmorGrid().getArmorRating() / 15f;
        float armorFix = ARMOR_FIX * maxArmor * amount;
        while (armorFix > 1 / 100) {
            org.lwjgl.util.Point point = DefenseUtils.getMostDamagedArmorCell(ship);
            if (point != null && ship.getArmorGrid().getArmorRating() / 15f - ship.getArmorGrid().getArmorValue(point.getX(), point.getY()) > 1) {
                if (ship.getArmorGrid().getArmorRating() / 15f > ship.getArmorGrid().getArmorValue(point.getX(), point.getY()) + armorFix) {
                    ship.getArmorGrid().setArmorValue(point.getX(), point.getY(), ship.getArmorGrid().getArmorValue(point.getX(), point.getY()) + armorFix);
                    armorFix = 0;
                } else {
                    armorFix -= ship.getArmorGrid().getArmorRating() / 15f - ship.getArmorGrid().getArmorValue(point.getX(), point.getY());
                    ship.getArmorGrid().setArmorValue(point.getX(), point.getY(), ship.getArmorGrid().getArmorRating() / 15f);
                }
                ship.syncWithArmorGridState();
                ship.syncWeaponDecalsWithArmorDamage();
            } else {
                break;
            }

        }
    }


    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return ship.getHullSpec().getHullName().contains("blue_eyes");
    }
    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        return null;
    }
}

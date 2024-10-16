package real_combat.hullmods;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.WingRole;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import real_combat.ai.RC_BomberAttackAI;

/**
 * 轰炸机以友军作为跳板进攻
 */
public class RC_BomberCore extends BaseHullMod {
    public static String ID = "RC_BomberCore";
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {

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
                if (WingRole.BOMBER.equals(w.getRole())) {
                    for (ShipAPI f : w.getWingMembers()) {
                        if (f.getCustomData().get(RC_BomberAttackAI.ID) == null && f.isAlive()) {
                            int ammo = 0;
                            for (WeaponAPI wp : f.getAllWeapons()) {
                                if (wp.usesAmmo()) {
                                    ammo += wp.getAmmo();
                                }
                            }
                            if (ammo > 0) {
                                f.setCustomData(RC_BomberAttackAI.ID, f.getShipAI());
                                f.setShipAI(new RC_BomberAttackAI(f));
                            }
                        }
                    }
                }
            }
        }
        for (ShipAPI m : ship.getChildModulesCopy()) {
            if (m.getCustomData().get(ID)==null) {
                m.setCustomData(ID,ship);
            }
            m.setPullBackFighters(ship.isPullBackFighters());
            if (!m.isPullBackFighters()) {
                for (FighterWingAPI w : m.getAllWings()) {
                    if (WingRole.BOMBER.equals(w.getRole())) {
                        for (ShipAPI f : w.getWingMembers()) {
                            if (f.getCustomData().get(RC_BomberAttackAI.ID) == null && f.isAlive()) {
                                int ammo = 0;
                                for (WeaponAPI wp : f.getAllWeapons()) {
                                    if (wp.usesAmmo()) {
                                        ammo += wp.getAmmo();
                                    }
                                }
                                if (ammo > 0) {
                                    f.setCustomData(RC_BomberAttackAI.ID, f.getShipAI());
                                    f.setShipAI(new RC_BomberAttackAI(f));
                                }
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
    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {

    }
}

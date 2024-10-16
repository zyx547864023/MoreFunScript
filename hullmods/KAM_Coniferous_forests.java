package real_combat.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import org.lazywizard.lazylib.combat.AIUtils;

public class KAM_Coniferous_forests extends BaseHullMod {
    public static String ID = "KAM_Coniferous_forests";
    public static final float RANGE = 3000f;
    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        for (ShipAPI e:AIUtils.getEnemiesOnMap(ship)) {
            if (e.isAlive()) {
                e.getMutableStats().getSystemCooldownBonus().unmodify(ID);
                e.getMutableStats().getSystemRangeBonus().unmodify(ID);
                e.getMutableStats().getSystemUsesBonus().unmodify(ID);
            }
        }
        for (ShipAPI e:AIUtils.getNearbyEnemies(ship,RANGE)) {
            if (e.isAlive()&& e.getPhaseCloak()!=null) {
                if (e.getPhaseCloak().isActive()) {
                    e.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,null,0);
                    e.getPhaseCloak().forceState(ShipSystemAPI.SystemState.OUT,0f);
                    e.getPhaseCloak().deactivate();
                }
            }
            e.getMutableStats().getSystemCooldownBonus().modifyMult(ID,9999F);
            e.getMutableStats().getSystemRangeBonus().modifyMult(ID,0F);
            e.getMutableStats().getSystemUsesBonus().modifyMult(ID,0F);
            if (e.getSystem()!=null) {
                e.getSystem().deactivate();
            }
        }
    }
}

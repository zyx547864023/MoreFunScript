package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * 导航
 */
public class RC_Navigation extends BaseHullMod {
    private String ID = "RC_Navigation";
    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        CombatEngineAPI engine =  Global.getCombatEngine();
        if (!ship.isAlive()) {
            return;
        }
        for (ShipAPI s:engine.getShips()) {
            s.getMutableStats().getMaxSpeed().unmodifyPercent(ID);
            if (!s.isAlive()||s.equals(ship)) {
                continue;
            }
            if (MathUtils.getShortestRotation(VectorUtils.getAngle(new Vector2f(0,0),s.getVelocity()),VectorUtils.getAngle(s.getLocation(),ship.getLocation()))<90f)
            {
                s.getMutableStats().getMaxSpeed().modifyPercent(ID,10f);
                if (s.equals(engine.getPlayerShip())) {
                    engine.maintainStatusForPlayerShip(ID, Global.getSettings().getSpriteName("ui", "icon_tactical_cr_bonus"), "导航通信", "+10% 最高速度", false);
                }
            }
        }
    }
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return true;
    }
    public String getUnapplicableReason(ShipAPI ship) {
        return null;
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize,
                                               MutableShipStatsAPI stats, String id) {
    }
}

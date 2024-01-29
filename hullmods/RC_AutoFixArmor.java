package real_combat.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.Point;

import java.util.Map;

public class RC_AutoFixArmor extends BaseHullMod {
    protected IntervalUtil tracker = new IntervalUtil(1f, 1f);
    private static final float ARMOR_FIX = 0.01f;
    @Override
    public void advanceInCombat(final ShipAPI ship, float amount) {
        CombatEngineAPI engine = Global.getCombatEngine();
        if (engine.isPaused()) {return;}
        if (!ship.isAlive()) {return;}
        tracker.advance(amount);
        if (tracker.intervalElapsed()) {
            int count = 0;
            float[][] grid = ship.getArmorGrid().getGrid();
            for (int x = 0; x < grid.length; x++) {
                for (int y = 0; y < grid[x].length; y++) {
                    count++;
                }
            }
            float maxArmor = count * ship.getArmorGrid().getArmorRating() / 15f;
            float armorFix = ARMOR_FIX * maxArmor;
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
                } else {
                    break;
                }
            }
        }
    }
}

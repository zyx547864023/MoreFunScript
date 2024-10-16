package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.combat.DefenseUtils;

import java.util.EnumSet;

/**
 *
 */

public class AutoFIxArmor extends BaseShipSystemScript {
    private final static String ID = "AutoFIxArmor";
    private ShipAPI ship;
    private Boolean init=false;
    CombatEngineAPI engine = Global.getCombatEngine();
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        try {
            //如果附近有自己的Spy
            if (!init) {
                init = true;
                Global.getCombatEngine().addLayeredRenderingPlugin(new AutoFIxArmorCombatPlugin(ship));
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (engine == null) return;
        if(engine.isPaused()) {return;}
        if (ship == null) {
            return;
        }
        try {

        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        return null;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        return null;
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        float nowArmor = 0;
        float maxArmor = 0;
        int count = 0;
        float[][] grid = ship.getArmorGrid().getGrid();
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                nowArmor += ship.getArmorGrid().getArmorValue(x, y);
                count++;
            }
        }
        maxArmor = count * ship.getArmorGrid().getArmorRating() / 15f;
        //获取装甲最少
        if (nowArmor < maxArmor-1) {
            return true;
        }
        return false;
    }

    public class AutoFIxArmorCombatPlugin extends BaseCombatLayeredRenderingPlugin {
        private static final float ARMOR_FIX = 0.1f;
        private final ShipAPI ship;
        private CombatEngineAPI engine = Global.getCombatEngine();
        public AutoFIxArmorCombatPlugin(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (engine == null) return;
            if (engine.isPaused()) {return;}
            if (!ship.isAlive()) {return;}
            if (ship.getSystem().isActive()) {
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
                    } else {
                        break;
                    }
                }
            }
        }

        public void getOut(CombatEntityAPI s){

        }

        public void init(CombatEngineAPI engine) {

        }

        @Override
        public float getRenderRadius() {
            return 1000000f;
        }

        @Override
        public EnumSet<CombatEngineLayers> getActiveLayers() {
            return EnumSet.of(CombatEngineLayers.ABOVE_SHIPS_LAYER);
        }

        @Override
        public void render(CombatEngineLayers layer, ViewportAPI viewport) {

        }

        @Override
        public boolean isExpired() {
            return false;
        }
    }
}
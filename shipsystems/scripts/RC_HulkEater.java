package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.hullmods.RC_SpiderCore;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 *  使用一次
 *  一个残骸缓慢移动到 船位置 然后颜色变浅 不可逆 不可中断 吞噬之后增加舰载机恢复速度 质量/100 = 舰载机恢复速度 质量/10 = buff持续时间
 *
 */

public class RC_HulkEater extends BaseShipSystemScript {
    private final static String ID = "RC_HulkEater";
    private final static String IS_ON = "IS_ON";
    private final static String WHO_CATCH = "WHO_CATCH";
    private final static float BUFF_DENOMINATOR = 100f;
    private final static float TIME_DENOMINATOR = 10f;
    private float timer = 0;
    private float buff = 0;
    private boolean init = false;
    private ShipAPI hulk;
    private ShipAPI ship;
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
            if (hulk == null) {
                //搜寻船周围的残骸和陨石
                float minDistance = ship.getCollisionRadius() * 1.5f;
                for (ShipAPI s : engine.getShips()) {
                    if (s.isHulk()) {
                        float distance = MathUtils.getDistance(ship, s);
                        if (distance < minDistance) {
                            if (s.getCustomData().get(WHO_CATCH) != null && ship.getCustomData().get(RC_SpiderCore.ID) != null) {
                                if (s.getCustomData().get(WHO_CATCH).equals(ship.getCustomData().get(RC_SpiderCore.ID))) {
                                    minDistance = distance;
                                    hulk = s;
                                }
                            }
                        }
                    }
                }
                if (!init) {
                    init = true;
                    Global.getCombatEngine().addLayeredRenderingPlugin(new RC_HulkEaterCombatPlugin(ship));
                }
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
        if (hulk == null) {
            return true;
        }
        return false;
    }

    public class RC_HulkEaterCombatPlugin extends BaseCombatLayeredRenderingPlugin {
        private final ShipAPI ship;
        private CombatEngineAPI engine = Global.getCombatEngine();
        public RC_HulkEaterCombatPlugin(ShipAPI ship) {
            this.ship = ship;
        }

        @Override
        public void advance(float amount) {
            if (engine == null) return;
            if (engine.isPaused()) {return;}
            if (!ship.isAlive()) {return;}

            //搜寻船周围的残骸和陨石
            if (hulk!=null) {
                hulk.setCustomData(WHO_CATCH,ship);
                hulk.getSpriteAPI().setAlphaMult(hulk.getSpriteAPI().getAlphaMult()-amount);

                if (hulk.getSpriteAPI().getAlphaMult()<0&&timer==0) {
                    hulk.getSpriteAPI().setAlphaMult(0);
                    timer = hulk.getMass()/TIME_DENOMINATOR;
                    ship.getMutableStats().getFighterRefitTimeMult().modifyMult(ID,BUFF_DENOMINATOR/hulk.getMass());
                }

                if (timer>0) {
                    timer-=amount;
                }
                else {
                    timer = 0;
                    hulk = null;
                    ship.getMutableStats().getFighterRefitTimeMult().unmodifyMult(ID);
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
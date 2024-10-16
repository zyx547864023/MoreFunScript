package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.ai.RC_PhaseSpyOnFighterAI;

import java.awt.*;
import java.util.List;

/**
 *  使用一次
 *  一个残骸缓慢移动到 船位置 然后颜色变浅 不可逆 不可中断 吞噬之后增加舰载机恢复速度 质量/100 = 舰载机恢复速度 质量/10 = buff持续时间
 *
 */

public class RC_SpyOn extends BaseShipSystemScript {
    private final static String ID = "RC_spy";
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
                int count = 0;
                List<ShipAPI> ships = AIUtils.getNearbyAllies(ship, 500f);
                for (ShipAPI f : ships) {
                    if (ID.equals(f.getHullSpec().getBaseHullId())&&f.getWing()!=null) {
                        if (f.getCustomData().get(ID) == null&&ship.equals(f.getWing().getSourceShip())) {
                            if (!f.isWingLeader()) {
                                f.setCustomData(ID, true);
                                f.setShipAI(new RC_PhaseSpyOnFighterAI(f, ship, new Vector2f(f.getLocation()), MathUtils.getPoint(f.getLocation(), 21000, VectorUtils.getAngle(new Vector2f(0,0),ship.getVelocity()))));
                                if (ship.equals(engine.getPlayerShip())) {
                                    float targetAngle = VectorUtils.getAngle(ship.getLocation(), ship.getMouseTarget());
                                    f.setShipAI(new RC_PhaseSpyOnFighterAI(f, ship, new Vector2f(f.getLocation()), MathUtils.getPoint(f.getLocation(), 2000, targetAngle)));
                                }
                                engine.spawnEmpArcVisual(ship.getLocation(), ship, f.getLocation(), f, 10f, Color.magenta, Color.white);
                                init = true;
                                count++;
                                return;
                            }
                        }
                    }
                }
                if (count==0) {
                    for (ShipAPI f : ships) {
                        if (ID.equals(f.getHullSpec().getBaseHullId())&&f.getWing()!=null) {
                            if (f.getCustomData().get(ID) == null&&ship.equals(f.getWing().getSourceShip())) {
                                f.setCustomData(ID, true);
                                f.setShipAI(new RC_PhaseSpyOnFighterAI(f, ship, new Vector2f(f.getLocation()), MathUtils.getPoint(f.getLocation(), 2000, VectorUtils.getAngle(new Vector2f(0,0),ship.getVelocity()))));
                                if (ship.equals(engine.getPlayerShip())) {
                                    float targetAngle = VectorUtils.getAngle(ship.getLocation(), ship.getMouseTarget());
                                    f.setShipAI(new RC_PhaseSpyOnFighterAI(f, ship, new Vector2f(f.getLocation()), MathUtils.getPoint(f.getLocation(), 2000, targetAngle)));
                                }
                                engine.spawnEmpArcVisual(ship.getLocation(), ship, f.getLocation(), f, 10f, Color.magenta, Color.white);
                                init = true;
                                return;
                            }
                        }
                    }
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
            init = false;
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
        //如果附近有自己的Spy
        List<ShipAPI> ships= AIUtils.getNearbyAllies(ship,500f);
        for (ShipAPI f:ships) {
            if (ID.equals(f.getHullSpec().getBaseHullId())&&f.getWing()!=null) {
                if (f.getCustomData().get(ID) == null&&ship.equals(f.getWing().getSourceShip())) {
                    return true;
                }
            }
        }
        return false;
    }
}
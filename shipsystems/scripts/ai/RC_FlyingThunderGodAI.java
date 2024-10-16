package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.shipsystems.scripts.RC_FlyingThunderGod;

public class RC_FlyingThunderGodAI implements ShipSystemAIScript {
    private final static String ID = "RC_FlyingThunderGodAI";
    private CombatEngineAPI engine;
    private ShipAPI ship;
    float timer;
    private ShipwideAIFlags flags;
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine)
    {
        this.ship = ship;
        this.engine = engine;
        timer=0f;
        this.flags=flags;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        if (!ship.isAlive()) {
            return;
        }
        if (ship.getSystem().isActive() || ship.getSystem().isStateActive() || ship.getSystem().isCoolingDown() || ship.getSystem().isChargeup() || ship.getSystem().isChargedown()) {
            return;
        }
        MissileAPI missile = (MissileAPI) ship.getCustomData().get(RC_FlyingThunderGod.ID);
        //如果没有导弹使用
        if (missile==null) {
            /*
            if (ship.getShipTarget()!=null) {
                ship.useSystem();
            }
            */
            return;
        }
        else {
            /*
            if (missile.isFizzling() || missile.isFading() || missile.didDamage() || missile.isExpired()) {
                missile = null;
                ship.getCustomData().remove(RC_FlyingThunderGod.ID);
                return;
            }
            */
            //如果有导弹 幅能水平好
            if (ship.getFluxLevel() < 0.7f) {
                //导弹在屁股
                if (ship.getShipTarget() != null) {
                    Vector2f targetLocation = ship.getShipTarget().getLocation();
                    Vector2f missileLocation = missile.getLocation();
                    float toTargetAngle = VectorUtils.getAngle(missileLocation, targetLocation);
                    //到达屁股
                    if (Math.abs(MathUtils.getShortestRotation(toTargetAngle, ship.getShipTarget().getFacing())) <= 60
                    && MathUtils.getDistance(targetLocation,missileLocation) <= 300
                    ) {
                        ship.useSystem();
                    }
                }
            } else {
                //导弹附近没有敌人
                if (AIUtils.getNearbyEnemies(missile, 500f).size() <= 1) {
                    //ship.useSystem();
                }
            }
        }
    }
}

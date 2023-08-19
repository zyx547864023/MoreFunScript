package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.combat.entities.Ship;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.hullmods.RC_SpiderCore;

import java.util.ArrayList;
import java.util.List;

public class RC_AcceleratingFieldAI implements ShipSystemAIScript {

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
        /*
        boolean isNoEnemy = true;
        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI s : ships) {
            float range = MathUtils.getDistance(ship, s);
            if (range<=ship.getCollisionRadius()*15&&s.getOwner()!=ship.getOwner()&&s.isAlive()&!s.isFighter()) {
                isNoEnemy = false;
                break;
            }
        }
        */
        ShipAPI enemy = AIUtils.getNearestEnemy(ship);
        ShipAPI motherShip = null;
        if (ship.getCustomData().get(RC_SpiderCore.ID)!=null) {
            motherShip = (ShipAPI) ship.getCustomData().get(RC_SpiderCore.ID);
        }
        if (motherShip!=null) {
            if (motherShip.getShipTarget()!=null) {
                if (motherShip.getShipTarget().getOwner()!=ship.getOwner()) {
                    enemy = motherShip.getShipTarget();
                }
            }
        }
        if (enemy!=null) {
            float distanceShipToEnemy = MathUtils.getDistance(ship, enemy);
            List<CombatEntityAPI> other = new ArrayList<>();
            List<ShipAPI> ships = engine.getShips();
            //List<CombatEntityAPI> asteroids = engine.getAsteroids();
            float minDistance = distanceShipToEnemy;
            for (ShipAPI s : ships) {
                if ((s.getOwner() == ship.getOwner() || s.getOwner() == 100)&&!s.isFighter()&&!s.isStationModule()) {
                    float distance = MathUtils.getDistance(s, ship);
                    if (minDistance > distance) {
                        other.add(s);
                    }
                }
            }
            for (CombatEntityAPI o : other) {
                float shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), o.getLocation());
                float shipToEnemyAngle = VectorUtils.getAngle(ship.getLocation(), enemy.getLocation());
                Vector2f otherRoundPoint = MathUtils.getPoint(o.getLocation(), o.getCollisionRadius(), shipToOtherAngle + 90);
                float missileToOtherRoundPoint = VectorUtils.getAngle(ship.getLocation(), otherRoundPoint);
                if (Math.abs(MathUtils.getShortestRotation(shipToEnemyAngle, shipToOtherAngle))
                        < Math.abs(MathUtils.getShortestRotation(missileToOtherRoundPoint, shipToOtherAngle))
                ) {
                    return;
                }
            }
        }
        //
        if (!ship.getSystem().isActive()&&!ship.getSystem().isStateActive()&&!ship.getSystem().isCoolingDown()&&!ship.getSystem().isChargeup()&&!ship.getSystem().isChargedown()
        &&ship.areAnyEnemiesInRange()
        ) {
            ship.useSystem();
        }
    }
}

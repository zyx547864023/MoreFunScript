package real_combat.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.util.RC_Util;

public class RC_MonsterBallAI extends RC_BaseMissile {
    private static final float TARGET_ACQUISITION_RANGE = 3600f;
    private static final float RADIUS_DIVIDE_2 = 2f;
    private static final float SPEED_DIVIDE_2 = 2f;
    private static final String ID="monster_ball_shooter_sec";
    private CombatEngineAPI engine = Global.getCombatEngine();
    public RC_MonsterBallAI(MissileAPI missile, ShipAPI launchingShip) {
        super(missile, launchingShip);
    }


    private static List<ShipAPI> getSortedDirectShipTargets(ShipAPI launchingShip) {
        List<ShipAPI> directTargets = RC_Util.getShipsWithinRange(launchingShip.getMouseTarget(), 300f);
        if (!directTargets.isEmpty()) {
            Collections.sort(directTargets, new CollectionUtils.SortEntitiesByDistance(launchingShip.getMouseTarget()));
        }
        return directTargets;
    }

    @Override
    public void advance(float amount) {
        float maxSpeed = missile.getMaxSpeed();
        if (missile.isFizzling() || missile.isFading()) {
            //登陆舱
            CombatEntityAPI landing = Global.getCombatEngine().spawnProjectile(
                    missile.getSource(),
                    null,
                    ID,
                    missile.getLocation(),
                    missile.getFacing(),
                    missile.getVelocity()
            );
            engine.removeObject(missile);
            return;
        }

        assignMissileToShipTarget(launchingShip);

        //如果没有找到目标
        if (!acquireTarget(amount)) {
            if (missile.getVelocity().length() >= (maxSpeed / SPEED_DIVIDE_2)) {
                missile.giveCommand(ShipCommand.DECELERATE);
            } else {
                missile.giveCommand(ShipCommand.ACCELERATE);
            }
            return;
        }

        //如果目标间有东西阻挡 导弹 到 other 的坐标角度和 导弹到other圆上的夹角 差值 大于 导弹到目标的夹角
        //AIUtils.getNearestEnemy(missile);
        CombatEntityAPI other = null;
        List<ShipAPI> ships = engine.getShips();
        List<CombatEntityAPI> asteroids = engine.getAsteroids();
        float minDistance = TARGET_ACQUISITION_RANGE;
        for (ShipAPI s:ships)
        {
            if (s.getOwner()!=missile.getOwner())
            {
                float distance = MathUtils.getDistance(s, missile);
                if (minDistance>distance)
                {
                    other = s;
                    minDistance = distance;
                }
            }
        }
        for (CombatEntityAPI a:asteroids) {
            float distance = MathUtils.getDistance(a, missile);
            if (minDistance>distance)
            {
                other = a;
                minDistance = distance;
            }
        }

        if (other!=null&&target!=null) {
            if (!other.getLocation().equals(target.getLocation()) && other.getOwner() != missile.getOwner()) {
                float radius = other.getCollisionRadius();
                float distance = MathUtils.getDistance(other, missile);
                //如果在碰撞范围内
                if (distance < radius * 4) {
                    float missileToOtherAngle = VectorUtils.getAngle(missile.getLocation(), other.getLocation());
                    float missileToShipAngle = VectorUtils.getAngle(missile.getLocation(), target.getLocation());
                    Vector2f otherRoundPoint = MathUtils.getPoint(other.getLocation(), radius * 2.5f, missileToOtherAngle + 90);
                    float missileToOtherRoundPoint = VectorUtils.getAngle(missile.getLocation(), otherRoundPoint);
                    if (Math.abs(MathUtils.getShortestRotation(missileToShipAngle, missileToOtherAngle))
                            < Math.abs(MathUtils.getShortestRotation(missileToOtherRoundPoint, missileToOtherAngle))
                    ) {
                        float missileFacing = MathUtils.clampAngle(missile.getFacing());
                        if (MathUtils.getShortestRotation(missileFacing, missileToOtherAngle) > 0) {
                            missileToOtherAngle = missileToOtherAngle - 90;
                            Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), radius * 2.5f, missileToOtherAngle);
                            missileToOtherAngle = VectorUtils.getAngle(missile.getLocation(), targetPoint);
                            float mi = Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount, missileToOtherAngle));
                            missileCommandNoSheild(mi, missileToOtherAngle, amount);
                        } else {
                            missileToOtherAngle = missileToOtherAngle + 90;
                            Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), radius * 2.5f, missileToOtherAngle);
                            missileToOtherAngle = VectorUtils.getAngle(missile.getLocation(), targetPoint);
                            float mi = Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount, missileToOtherAngle));
                            missileCommandNoSheild(mi, missileToOtherAngle, amount);
                        }
                        missile.giveCommand(ShipCommand.ACCELERATE);
                        return;
                    }
                }
            }
        }

        //如果导弹和目标之间有护盾就转向
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if(ship.getOwner()==missile.getOwner()) {return;}
            float shipToMissileAngle = VectorUtils.getAngle(ship.getLocation(),missile.getLocation());
            float missileToShipAngle = VectorUtils.getAngle(missile.getLocation(),ship.getLocation());
            float missileFacing = MathUtils.clampAngle(missile.getFacing());
            //当与目标距离小于护盾半径的时候时直线加速接近
            float radius = ship.getCollisionRadius();
            if (ship.getShield()!=null)
            {
                radius = ship.getShield().getRadius();
            }
            float mi = Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount,missileToShipAngle));
            //Global.getLogger(this.getClass()).info(mi);
            if(ship.getShield()!=null)
            {
                if(ship.getShield().isOn()) {
                    float distance = MathUtils.getDistance(ship,missile);
                    if(distance>radius * 4)
                    {
                        //Global.getLogger(this.getClass()).info("ZJJJ");
                        missileCommandNoSheild(mi, missileToShipAngle, amount);
                        missile.giveCommand(ShipCommand.ACCELERATE);
                        return;
                    }

                    float shieldAngle = ship.getShield().getFacing();
                    float shieldAcc = ship.getShield().getActiveArc();
                    //如果面前有盾没有盾
                    if(Math.abs(MathUtils.getShortestRotation(shipToMissileAngle,shieldAngle))>(shieldAcc/RADIUS_DIVIDE_2)+1)
                    {
                        missileCommandNoSheild(mi, missileToShipAngle, amount);
                        //此处需要矢量制动
                        //Vector2f speedSub = Vector2f.sub(missile.getVelocity(), ship.getVelocity(), (Vector2f)null);
                        //Vector2f wantSpeed = MathUtils.getPoint(new Vector2f(0,0),missile.getMaxSpeed(),missileToShipAngle);
                        //float wantAngle = VectorUtils.getAngle(speedSub,wantSpeed);
                        //mi =  Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount ,wantAngle));
                        //missileCommandNoSheild(mi, wantAngle,  amount);
                    }
                    else {
                        //Global.getLogger(this.getClass()).info("QMYD");
                        //顺势转开
                        if (MathUtils.getShortestRotation(missileFacing, missileToShipAngle) > 0) {
                            missileToShipAngle = missileToShipAngle-90;
                            Vector2f targetPoint = MathUtils.getPointOnCircumference(ship.getLocation(),radius*2.5f,missileToShipAngle);
                            missileToShipAngle = VectorUtils.getAngle(missile.getLocation(),targetPoint);
                            mi = Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount,missileToShipAngle));
                            missileCommandNoSheild(mi, missileToShipAngle, amount);
                        } else {
                            missileToShipAngle = missileToShipAngle+90;
                            Vector2f targetPoint = MathUtils.getPointOnCircumference(ship.getLocation(),radius*2.5f,missileToShipAngle);
                            missileToShipAngle = VectorUtils.getAngle(missile.getLocation(),targetPoint);
                            mi = Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount,missileToShipAngle));
                            missileCommandNoSheild(mi, missileToShipAngle, amount);
                        }
                    }
                }
                else {
                    missileCommandNoSheild(mi, missileToShipAngle, amount);
                }
            }
            else {
                missileCommandNoSheild(mi, missileToShipAngle, amount);
            }
            missile.giveCommand(ShipCommand.ACCELERATE);
        }
    }

    @Override
    protected void assignMissileToShipTarget(ShipAPI launchingShip) {
        if (isTargetValid(launchingShip.getShipTarget())) {
            setTarget(launchingShip.getShipTarget());
        }
    }

    @Override
    protected CombatEntityAPI findBestTarget() {
        CombatEntityAPI closest = null;
        float distance, closestDistance = TARGET_ACQUISITION_RANGE;
        List<ShipAPI> ships = AIUtils.getNearbyEnemies(missile,missile.getMaxRange());
        int shipSize = ships.size();
        for (int i = 0; i < shipSize; i++) {
            ShipAPI tmp = ships.get(i);
            if (!isTargetValid(tmp)) {
                continue;
            }
            distance = MathUtils.getDistance(tmp, missile);
            if (distance < closestDistance) {
                closest = tmp;
                closestDistance = distance;
            }
        }
        return closest;
    }

    @Override
    protected CombatEntityAPI getMouseTarget(ShipAPI launchingShip) {
        ListIterator<ShipAPI> iter = getSortedDirectShipTargets(launchingShip).listIterator();
        while (iter.hasNext()) {
            ShipAPI tmp = iter.next();
            if (isTargetValid(tmp)) {
                return tmp;
            }
        }
        return null;
    }

    @Override
    protected boolean isTargetValid(CombatEntityAPI target) {
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            return !((ship.getOwner() == missile.getOwner())||ship.isFighter()||!ship.isAlive());
        }
        return false;
    }

    private void missileCommandNoSheild(float mi,float missileToShipAngle, float amount){
        if( mi - Math.abs(missile.getAngularVelocity() * amount) > missile.getMaxTurnRate() * amount )
        {
            if (MathUtils.getShortestRotation(MathUtils.clampAngle(missile.getFacing()), missileToShipAngle) > 0) {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            } else {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            }
        }
        else
        {
            if (missile.getAngularVelocity() > 1)
            {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            }
            else if((missile.getAngularVelocity() < -1))
            {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            }
        }
    }
}

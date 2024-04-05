package real_combat.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.ai.RC_BaseShipAI;
import real_combat.combat.RC_MonsterBallEveryFrameCombatPlugin;
import real_combat.util.RC_Util;

import java.util.*;

/**
 * 绕几圈然后打中
 */
public class RC_ExplosiveChainAI extends RC_BaseMissile {
    private static final float TARGET_ACQUISITION_RANGE = 3600f;
    private static final float RADIUS_DIVIDE_2 = 2f;
    private static final float SPEED_DIVIDE_2 = 2f;
    private static final float MIN_MULT = 2.5f;

    private static final float CIRCLE = 2f;
    private float now_circle = 0;
    private CombatEntityAPI oldTarget = null;
    private static final String ID="kunai";
    private CombatEngineAPI engine = Global.getCombatEngine();
    public RC_ExplosiveChainAI(MissileAPI missile, ShipAPI launchingShip) {
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
        if (missile.isFading()) {
            return;
        }
        float maxSpeed = missile.getMaxSpeed();

        assignMissileToShipTarget(launchingShip);

        //如果没有找到目标
        if (launchingShip.getWeaponGroupFor(missile.getWeapon())==null) {
            if (!acquireTarget(amount)) {
                if (missile.getVelocity().length() >= (maxSpeed / SPEED_DIVIDE_2)) {
                    missile.giveCommand(ShipCommand.DECELERATE);
                } else {
                    missile.giveCommand(ShipCommand.ACCELERATE);
                }
                return;
            }
        }
        else if (launchingShip.getWeaponGroupFor(missile.getWeapon()).isAutofiring()) {
            if (!acquireTarget(amount)) {
                if (missile.getVelocity().length() >= (maxSpeed / SPEED_DIVIDE_2)) {
                    missile.giveCommand(ShipCommand.DECELERATE);
                } else {
                    missile.giveCommand(ShipCommand.ACCELERATE);
                }
                return;
            }
        }
        else {
            float missileToShipAngle = VectorUtils.getAngle(missile.getLocation(),launchingShip.getMouseTarget());
            float missileFacing = MathUtils.clampAngle(missile.getFacing());
            float mi = Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount,missileToShipAngle));
            missileCommandNoSheild(mi, missileToShipAngle, amount);
            missile.giveCommand(ShipCommand.ACCELERATE);
            return;
        }

        //如果目标间有东西阻挡 导弹 到 other 的坐标角度和 导弹到other圆上的夹角 差值 大于 导弹到目标的夹角
        CombatEntityAPI other = null;
        Set<ShipAPI> ships = RC_BaseShipAI.getEnemiesOnMapNotFighter(missile,RC_BaseShipAI.getHulksOnMap(new HashSet<ShipAPI>()));
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
                            Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), radius * 2f, missileToOtherAngle);
                            missileToOtherAngle = VectorUtils.getAngle(missile.getLocation(), targetPoint);
                            float mi = Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount, missileToOtherAngle));
                            missileCommandNoSheild(mi, missileToOtherAngle, amount);
                        } else {
                            missileToOtherAngle = missileToOtherAngle + 90;
                            Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), radius * 2f, missileToOtherAngle);
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

            float distance = MathUtils.getDistance(ship,missile);
            if(distance>radius * 4)
            {
                missileCommandNoSheild(mi, missileToShipAngle, amount);
                missile.giveCommand(ShipCommand.ACCELERATE);
                return;
            }
            float mult = 1f;
            if (target instanceof ShipAPI) {
                mult = ((ShipAPI) target).getHullSize().ordinal();
            }
            //如果绕圈不足
            if(missile.getFlightTime()>=missile.getMaxFlightTime()*0.8f)
            {
                missileCommandNoSheild(mi, missileToShipAngle, amount);
            }
            else {
                now_circle+=amount;
                //顺势转开
                if (MathUtils.getShortestRotation(missileFacing, missileToShipAngle) > 0) {
                    missileToShipAngle = missileToShipAngle-90;
                    Vector2f targetPoint = MathUtils.getPointOnCircumference(ship.getLocation(),radius*2f,missileToShipAngle);
                    missileToShipAngle = VectorUtils.getAngle(missile.getLocation(),targetPoint);
                    mi = Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount,missileToShipAngle));
                    missileCommandNoSheild(mi, missileToShipAngle, amount);
                } else {
                    missileToShipAngle = missileToShipAngle+90;
                    Vector2f targetPoint = MathUtils.getPointOnCircumference(ship.getLocation(),radius*2f,missileToShipAngle);
                    missileToShipAngle = VectorUtils.getAngle(missile.getLocation(),targetPoint);
                    mi = Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount,missileToShipAngle));
                    missileCommandNoSheild(mi, missileToShipAngle, amount);
                }
            }
            missile.giveCommand(ShipCommand.ACCELERATE);
        }
    }

    @Override
    protected void assignMissileToShipTarget(ShipAPI launchingShip) {
        engine = Global.getCombatEngine();
        if (engine.getPlayerShip()!=null) {
            ShipAPI player = engine.getPlayerShip();
            if (player.getOwner() == launchingShip.getOwner()&&player.getShipTarget()!=null) {
                if (isTargetValid(player.getShipTarget())) {
                    float mult = RC_MonsterBallEveryFrameCombatPlugin.getMultWithOutMarines(player.getShipTarget());
                    if (mult > MIN_MULT) {
                        setTarget(player.getShipTarget());
                    }
                }
            }
        }
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
        if (closest!=null) {
            if (!closest.equals(oldTarget)) {
                oldTarget = closest;
                now_circle = 0f;
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
                if (!tmp.equals(oldTarget)) {
                    oldTarget = tmp;
                    now_circle = 0f;
                }
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

package real_combat.ai;

import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 * 护卫舰AI
 * 不对着屁股不进攻
 */
public class RC_FrigateAI extends RC_BaseShipAI {
    private final static String ID = "RC_RallyTaskForceAI";

    public RC_FrigateAI(ShipAPI ship) {
        super(ship);
    }

    /**
     * @param amount
     */

    public void advance(float amount) {
            super.advance(amount);
    }

    @Override
    public void useDriveSystem(Vector2f targetLocation) {
        if (target==null) {
            return;
        }
        float distance = MathUtils.getDistance(ship.getLocation(),target.getLocation());
        float angle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(),target.getLocation()),target.getFacing()));
        if (angle>30||distance<maxWeaponRange) {
            return;
        }

        super.useDriveSystem(targetLocation);
    }

    /**
     *
     * @param target
     * @param other
     * @param amount
     */
    @Override
    public void flyToTarget(ShipAPI target, ShipAPI other, float amount) {
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        if (target!=null) {
            if (Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(),target.getLocation()), target.getFacing())) > 30) {
                //原理最近的敌人
                if (nearestBiggerEnemy != null && nearestBiggerAlly != null) {
                    if (nearestBiggerEnemy.minDistance > minWeaponRange) {
                        move(shipFacing, VectorUtils.getAngle(ship.getLocation(), nearestBiggerAlly.ship.getLocation()));
                        return;
                    }
                }
            }
        }
        super.flyToTarget(target,other,amount);
        /*
        Vector2f targetLocation = target.getLocation();
        float distance = MathUtils.getDistance(target,ship);
        float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetLocation);
        if (distance>minWeaponRange) {
            if (other != null && !ship.equals(other) && !target.equals(other)) {
                float shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), other.getLocation());
                Vector2f otherRoundPoint = MathUtils.getPoint(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle + 90);
                float missileToOtherRoundPoint = VectorUtils.getAngle(ship.getLocation(), otherRoundPoint);
                if (Math.abs(MathUtils.getShortestRotation(shipToTargetAngle, shipToOtherAngle))
                        < Math.abs(MathUtils.getShortestRotation(missileToOtherRoundPoint, shipToOtherAngle))
                ) {
                    //shift(missileToOtherAngle,target.getFacing());
                    float missileFacing = MathUtils.clampAngle(ship.getFacing());
                    if (MathUtils.getShortestRotation(missileFacing, shipToOtherAngle) > 0) {
                        shipToOtherAngle = shipToOtherAngle - 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle);
                        shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        move(shipFacing, shipToOtherAngle);
                    } else {
                        shipToOtherAngle = shipToOtherAngle + 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle);
                        shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        move(shipFacing, shipToOtherAngle);
                    }
                    return;
                }
            }
            //如果距离比较远就加速
            //距离很近那就减速和飞船同步
            float toTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetLocation);
            distance = MathUtils.getDistance(target.getLocation(), ship.getLocation());
            if (distance < minWeaponRange) {
                toTargetAngle = toTargetAngle + 180;
            }

            move(shipFacing, toTargetAngle);
        }
        //如果距离很近不在屁股
        else {
            //如果血量少远离
            if (target.getHitpoints() / target.getMaxHitpoints() < 0.3f) {
                float toTargetAngle = VectorUtils.getAngle(targetLocation, ship.getLocation());
                move(shipFacing, toTargetAngle);
                return;
            }
            if (other != null && !ship.equals(other) && !target.equals(other)) {
                float shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), other.getLocation());
                Vector2f otherRoundPoint = MathUtils.getPoint(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle + 90);
                float missileToOtherRoundPoint = VectorUtils.getAngle(ship.getLocation(), otherRoundPoint);
                if (Math.abs(MathUtils.getShortestRotation(shipToTargetAngle, shipToOtherAngle))
                        < Math.abs(MathUtils.getShortestRotation(missileToOtherRoundPoint, shipToOtherAngle))
                ) {
                    //shift(missileToOtherAngle,target.getFacing());
                    float missileFacing = MathUtils.clampAngle(ship.getFacing());
                    if (MathUtils.getShortestRotation(missileFacing, shipToOtherAngle) > 0) {
                        shipToOtherAngle = shipToOtherAngle - 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle);
                        shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        move(shipFacing, shipToOtherAngle);
                    } else {
                        shipToOtherAngle = shipToOtherAngle + 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle);
                        shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        move(shipFacing, shipToOtherAngle);
                    }
                    return;
                }
            }
            if (Math.abs(MathUtils.getShortestRotation(shipFacing,target.getFacing()))>30) {
                shift(shipFacing,target.getFacing());
            }
            else {
                //距离很近那就减速和飞船同步
                float toTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetLocation);
                distance = MathUtils.getDistance(target.getLocation(), ship.getLocation());
                if (distance < minWeaponRange) {
                    toTargetAngle = toTargetAngle + 180;
                }
                move(shipFacing, toTargetAngle);
            }
        }

         */
    }
}

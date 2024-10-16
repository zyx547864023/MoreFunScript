package real_combat.ai;

import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;

/**
 * 护卫舰AI
 * 不对着屁股不进攻
 */
public class RC_FrigateAI extends RC_BaseShipAI {
    private final static String ID = "RC_FrigateAI";

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
    public void useDriveSystem() {
        if (target==null) {
            return;
        }
        /*
        float distance = MathUtils.getDistance(ship.getLocation(),target.getLocation());
        float angle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(),target.getLocation()),target.getFacing()));
        if (angle>30||distance<maxWeaponRange) {
            return;
        }
        */
        super.useDriveSystem();
    }

    /**
     * 有敌人 面相自己 远离到最远攻击距离
     * @param target
     * @param other
     * @param amount
     */
    @Override
    public void flyToTarget(ShipAPI target, ShipAPI other, float amount) {
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        //如果敌人看着我我就后退
        if (target!=null) {
            //如果没瞄准配股就远离
            if (Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(),target.getLocation()), target.getFacing())) > 45
                    ||Math.abs(MathUtils.getShortestRotation(shipFacing, target.getFacing())) > 30) {
                //原理最近的敌人
                if (nearestBiggerShip != null && nearestBiggerAlly != null) {
                    if (nearestBiggerShip.minDistance > minWeaponRange&&nearestBiggerAlly.minDistance > nearestBiggerAlly.ship.getCollisionRadius()) {
                        //if (Math.abs(MathUtils.getShortestRotation(nearestBiggerShip.ship.getFacing(),VectorUtils.getAngle(nearestBiggerShip.ship.getLocation(),ship.getLocation())))<30&&nearestBiggerShip.minDistance<maxWeaponRange) {
                        RC_BaseAIAction.move(ship, shipFacing, VectorUtils.getAngle(ship.getLocation(), nearestBiggerAlly.ship.getLocation()));
                        return;
                    }
                }
            }
        }
        super.flyToTarget(target,other,amount);
    }

    /**
     * 对于小船来说碰撞是致命的 远离所有 靠近半径的船 最优先
     */
    @Override
    public void beforeFlyToTarget() {
        //与最近的船保持一个半径距离
        if (nearestShip!=null) {
            if (nearestShip.minDistance < (nearestShip.ship.getCollisionRadius())) {
                RC_BaseAIAction.move(ship, ship.getFacing(), VectorUtils.getAngle(nearestShip.ship.getLocation(), ship.getLocation()));
                isDodge = true;
            }
        }
        //与最近的敌人保持最短攻击距离
        if (nearestBiggerEnemy!=null) {
            if (nearestBiggerEnemy.minDistance < minWeaponRange) {
                RC_BaseAIAction.move(ship, ship.getFacing(), VectorUtils.getAngle(nearestBiggerEnemy.ship.getLocation(), ship.getLocation()));
                isDodge = true;
            }
            else if (nearestBiggerEnemy.minDistance < maxWeaponRange) {
                if (Math.abs(MathUtils.getShortestRotation(nearestBiggerEnemy.ship.getFacing(),  VectorUtils.getAngle(nearestBiggerEnemy.ship.getLocation(), ship.getLocation()))) < 30) {
                    RC_BaseAIAction.move(ship, ship.getFacing(), VectorUtils.getAngle(nearestBiggerEnemy.ship.getLocation(), ship.getLocation()));
                    isDodge = true;
                }
            }
        }
        if (target!=null&&nearestBiggerAlly!=null) {
            if (MathUtils.getDistance(target,ship)>minWeaponRange&&target!=nearestBiggerAlly.ship&&MathUtils.getDistance(nearestBiggerAlly.ship,ship)<nearestBiggerAlly.ship.getCollisionRadius()+ship.getCollisionRadius()) {
                //远离队友且飞向敌人
                float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
                float shipToNearestShipAngle = VectorUtils.getAngle(nearestBiggerAlly.ship.getLocation(), ship.getLocation());
                RC_BaseAIAction.move(ship, ship.getFacing(), (shipToTargetAngle+shipToNearestShipAngle)/2f);
                isDodge = true;
            }
        }
    }

    @Override
    public void turn(float amount) {
        if (nearestBiggerShip!=null) {
            RC_BaseAIAction.turn(ship, nearestBiggerShip.ship.getLocation(), amount);
        }
        else
        {
            super.turn(amount);
        }
    }
}

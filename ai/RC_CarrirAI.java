package real_combat.ai;

import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 * 航母AI
 *
 */
public class RC_CarrirAI extends RC_BaseShipAI {
    private final static String ID = "RC_CarrirAI";

    public RC_CarrirAI(ShipAPI ship) {
        super(ship);
    }

    @Override
    public void flyToTarget(ShipAPI target, ShipAPI other, float amount) {
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        //如果距离很远直接
        Vector2f targetLocation = target.getLocation();
        float distance = MathUtils.getDistance(target,ship);
        float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetLocation);
        if (distance>minWingRange) {
            if (other != null && !ship.equals(other) && !target.equals(other)) {
                float shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), other.getLocation());
                Vector2f otherRoundPoint = MathUtils.getPoint(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle + 90);
                float missileToOtherRoundPoint = VectorUtils.getAngle(ship.getLocation(), otherRoundPoint);
                if (Math.abs(MathUtils.getShortestRotation(shipToTargetAngle, shipToOtherAngle))
                        < Math.abs(MathUtils.getShortestRotation(missileToOtherRoundPoint, shipToOtherAngle))
                ) {
                    float missileFacing = MathUtils.clampAngle(ship.getFacing());
                    if (MathUtils.getShortestRotation(missileFacing, shipToOtherAngle) > 0) {
                        shipToOtherAngle = shipToOtherAngle - 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle);
                        shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        RC_BaseAIAction.move(ship, shipFacing, shipToOtherAngle);
                    } else {
                        shipToOtherAngle = shipToOtherAngle + 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle);
                        shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        RC_BaseAIAction.move(ship, shipFacing, shipToOtherAngle);
                    }
                    return;
                }
            }
            //如果距离比较远就加速
            float toTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetLocation);
            RC_BaseAIAction.move(ship, shipFacing, toTargetAngle);
        }
        //如果距离很近不在屁股
        else {
            //如果血量少远离
            if (target.getHitpoints() / target.getMaxHitpoints() < 0.3f) {
                float toTargetAngle = VectorUtils.getAngle(targetLocation, ship.getLocation());
                RC_BaseAIAction.move(ship, shipFacing, toTargetAngle);
                return;
            }
            if (other != null && !ship.equals(other) && !target.equals(other)) {
                float shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), other.getLocation());
                Vector2f otherRoundPoint = MathUtils.getPoint(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle + 90);
                float missileToOtherRoundPoint = VectorUtils.getAngle(ship.getLocation(), otherRoundPoint);
                if (Math.abs(MathUtils.getShortestRotation(shipToTargetAngle, shipToOtherAngle))
                        < Math.abs(MathUtils.getShortestRotation(missileToOtherRoundPoint, shipToOtherAngle))
                ) {
                    float missileFacing = MathUtils.clampAngle(ship.getFacing());
                    if (MathUtils.getShortestRotation(missileFacing, shipToOtherAngle) > 0) {
                        shipToOtherAngle = shipToOtherAngle - 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle);
                        shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        RC_BaseAIAction.move(ship, shipFacing, shipToOtherAngle);
                    } else {
                        shipToOtherAngle = shipToOtherAngle + 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), other.getCollisionRadius() * 2.5f, shipToOtherAngle);
                        shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        RC_BaseAIAction.move(ship, shipFacing, shipToOtherAngle);
                    }
                    return;
                }
            }

            //在这过程中会撞到其他船
            if (
                    Math.abs(MathUtils.getShortestRotation(shipFacing,target.getFacing()))>30
            ) {
                RC_BaseAIAction.shift(ship, shipFacing,target.getFacing());
            }
            else {
                //如果速度相差太大需要减速
                Vector2f targetV = target.getVelocity();
                Vector2f shipV = ship.getVelocity();
                //看速度夹角 如果夹角
                float vdl = shipV.length() - targetV.length();
                Vector2f vd = Vector2f.sub(shipV,targetV,null);
                float vda = VectorUtils.getAngle(new Vector2f(0,0),vd);
                //速度 插值 大于 30 开始调整速度
                float maxVdl = 100;
                if (ShipAPI.HullSize.FIGHTER.equals(target.getHullSize())) {
                    maxVdl = maxVdl/30;
                }
                else if (ShipAPI.HullSize.FRIGATE.equals(target.getHullSize())) {
                    maxVdl = maxVdl/20;
                }
                else if (ShipAPI.HullSize.DESTROYER.equals(target.getHullSize())){
                    maxVdl = maxVdl/10;
                }
                else if (ShipAPI.HullSize.CRUISER.equals(target.getHullSize())){
                    maxVdl = maxVdl/5;
                }
                if (vdl>maxVdl) {
                    //获取速度插值
                    float accelerate = Math.abs(MathUtils.getShortestRotation(shipFacing,vda));
                    if (accelerate<45) {
                        ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, (Object) null, 0);
                    }
                    float backwards = Math.abs(MathUtils.getShortestRotation(shipFacing + 180f,vda));
                    if (backwards<45) {
                        ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
                    }
                    float strafeLeft = Math.abs(MathUtils.getShortestRotation(shipFacing - 90f,vda));
                    if (strafeLeft<45) {
                        ship.giveCommand(ShipCommand.STRAFE_LEFT, (Object) null, 0);
                    }
                    float strafeRight = Math.abs(MathUtils.getShortestRotation(shipFacing + 90f,vda));
                    if (strafeRight<45) {
                        ship.giveCommand(ShipCommand.STRAFE_RIGHT, (Object) null, 0);
                    }
                }
                else {
                    //距离很近那就减速和飞船同步
                    float toTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetLocation);
                    distance = MathUtils.getDistance(target.getLocation(), ship.getLocation());
                    if (distance < (target.getCollisionRadius()  + ship.getCollisionRadius() * 2)) {
                        toTargetAngle = toTargetAngle + 180;
                    }
                    RC_BaseAIAction.move(ship, shipFacing, toTargetAngle);
                }
            }
        }
    }

    @Override
    public float getWeight(ShipAPI target) {
        return super.getWeight(target);
    }
}

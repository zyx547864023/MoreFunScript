package real_combat.ai;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;
import java.util.Set;

public class RC_BaseAIAction {

    public static void move(ShipAPI ship, float shipFacing, float toTargetAngle) {
        float accelerate = Math.abs(MathUtils.getShortestRotation(shipFacing, toTargetAngle));
        if (accelerate < 45) {
            ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
        }
        float backwards = Math.abs(MathUtils.getShortestRotation(shipFacing + 180f, toTargetAngle));
        if (backwards < 45) {
            ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, (Object) null, 0);
        }
        float strafeLeft = Math.abs(MathUtils.getShortestRotation(shipFacing - 90f, toTargetAngle));
        if (strafeLeft < 45) {
            ship.giveCommand(ShipCommand.STRAFE_LEFT, (Object) null, 0);
        }
        float strafeRight = Math.abs(MathUtils.getShortestRotation(shipFacing + 90f, toTargetAngle));
        if (strafeRight < 45) {
            ship.giveCommand(ShipCommand.STRAFE_RIGHT, (Object) null, 0);
        }
    }


    /**
     *
     * @param needTurnAngle
     * @param toTargetAngle
     * @param amount
     */
    public static void turn(ShipAPI ship, float needTurnAngle, float toTargetAngle, float amount){
        if( needTurnAngle - Math.abs(ship.getAngularVelocity() * amount) > ship.getMaxTurnRate() * amount )
        {
            if (MathUtils.getShortestRotation(MathUtils.clampAngle(ship.getFacing()), toTargetAngle) > 0) {
                ship.giveCommand(ShipCommand.TURN_LEFT, (Object)null ,0);
            } else {
                ship.giveCommand(ShipCommand.TURN_RIGHT, (Object)null ,0);
            }
        }
        else {
            if (ship.getAngularVelocity() > 1) {
                ship.giveCommand(ShipCommand.TURN_RIGHT, (Object) null, 0);
            } else if ((ship.getAngularVelocity() < -1)) {
                ship.giveCommand(ShipCommand.TURN_LEFT, (Object) null, 0);
            }
        }
    }


    public static void shift(ShipAPI ship, float toTargetAngle,float targetFace){
        if (MathUtils.getShortestRotation(MathUtils.clampAngle(targetFace), toTargetAngle) > 0) {
            ship.giveCommand(ShipCommand.STRAFE_LEFT, (Object)null ,0);
        } else {
            ship.giveCommand(ShipCommand.STRAFE_RIGHT, (Object)null ,0);
        }
    }

    public static void turn(ShipAPI ship, Vector2f targetLocation, float amount){
        //如果距离比较远就加速
        //距离很近那就减速和飞船同步
        float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
        if( needTurnAngle - Math.abs(ship.getAngularVelocity() * amount) > ship.getMaxTurnRate() * amount )
        {
            if (MathUtils.getShortestRotation(MathUtils.clampAngle(ship.getFacing()), toTargetAngle) > 0) {
                ship.giveCommand(ShipCommand.TURN_LEFT, (Object)null ,0);
            } else {
                ship.giveCommand(ShipCommand.TURN_RIGHT, (Object)null ,0);
            }
        }
        else {
            if (ship.getAngularVelocity() > 1) {
                ship.giveCommand(ShipCommand.TURN_RIGHT, (Object) null, 0);
            } else if ((ship.getAngularVelocity() < -1)) {
                ship.giveCommand(ShipCommand.TURN_LEFT, (Object) null, 0);
            }
        }
    }
}

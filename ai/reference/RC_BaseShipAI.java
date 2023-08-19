//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package real_combat.ai.reference;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.Iterator;

public abstract class RC_BaseShipAI implements ShipAIPlugin {
    protected ShipAPI ship;
    protected float dontFireUntil = 0.0F;
    protected IntervalUtil circumstanceEvaluationTimer = new IntervalUtil(0.05F, 0.15F);
    static final float DEFAULT_FACING_THRESHHOLD = 3.0F;

    public boolean mayFire() {
        return this.dontFireUntil <= Global.getCombatEngine().getTotalElapsedTime(false);
    }

    public void evaluateCircumstances() {

    }

    public boolean isFacing(CombatEntityAPI target) {
        return this.isFacing(target.getLocation(), 3.0F);
    }

    public boolean isFacing(CombatEntityAPI target, float threshholdDegrees) {
        return this.isFacing(target.getLocation(), threshholdDegrees);
    }

    public boolean isFacing(Vector2f point) {
        return this.isFacing(point, 3.0F);
    }

    public boolean isFacing(Vector2f point, float threshholdDegrees) {
        return Math.abs(this.getAngleTo(point)) <= threshholdDegrees;
    }

    public float getAngleTo(CombatEntityAPI entity) {
        return this.getAngleTo(entity.getLocation());
    }

    public float getAngleTo(Vector2f point) {
        float angleTo = VectorUtils.getAngle(this.ship.getLocation(), point);
        return MathUtils.getShortestRotation(this.ship.getFacing(), angleTo);
    }

    public ShipCommand strafe(float degreeAngle, boolean strafeAway) {
        float angleDif = MathUtils.getShortestRotation(this.ship.getFacing(), degreeAngle);
        if (!strafeAway && Math.abs(angleDif) < 3.0F || strafeAway && Math.abs(angleDif) > 177.0F) {
            return null;
        } else {
            ShipCommand direction = angleDif > 0.0F ^ strafeAway ? ShipCommand.STRAFE_LEFT : ShipCommand.STRAFE_RIGHT;
            this.ship.giveCommand(direction, (Object)null, 0);
            return direction;
        }
    }

    public ShipCommand strafe(Vector2f location, boolean strafeAway) {
        return this.strafe(VectorUtils.getAngle(this.ship.getLocation(), location), strafeAway);
    }

    public ShipCommand strafe(CombatEntityAPI entity, boolean strafeAway) {
        return this.strafe(entity.getLocation(), strafeAway);
    }

    public ShipCommand strafeToward(float degreeAngle) {
        return this.strafe(degreeAngle, false);
    }

    public ShipCommand strafeToward(Vector2f location) {
        return this.strafeToward(VectorUtils.getAngle(this.ship.getLocation(), location));
    }

    public ShipCommand strafeToward(CombatEntityAPI entity) {
        return this.strafeToward(entity.getLocation());
    }

    public ShipCommand strafeAway(float degreeAngle) {
        return this.strafe(degreeAngle, true);
    }

    public ShipCommand strafeAway(Vector2f location) {
        return this.strafeAway(VectorUtils.getAngle(this.ship.getLocation(), location));
    }

    public ShipCommand strafeAway(CombatEntityAPI entity) {
        return this.strafeAway(entity.getLocation());
    }

    public ShipCommand turn(float degreeAngle, boolean turnAway) {
        float angleDif = MathUtils.getShortestRotation(this.ship.getFacing(), degreeAngle);
        float secondsTilDesiredFacing = angleDif / this.ship.getAngularVelocity();
        if (secondsTilDesiredFacing > 0.0F) {
            float turnAcc = this.ship.getMutableStats().getTurnAcceleration().getModifiedValue();
            float rotValWhenAt = Math.abs(this.ship.getAngularVelocity()) - secondsTilDesiredFacing * turnAcc;
            if (rotValWhenAt > 0.0F) {
                turnAway = !turnAway;
            }
        }

        ShipCommand direction = angleDif > 0.0F ^ turnAway ? ShipCommand.TURN_LEFT : ShipCommand.TURN_RIGHT;
        this.ship.giveCommand(direction, (Object)null, 0);
        return direction;
    }

    public ShipCommand turn(Vector2f location, boolean strafeAway) {
        return this.turn(VectorUtils.getAngle(this.ship.getLocation(), location), strafeAway);
    }

    public ShipCommand turn(CombatEntityAPI entity, boolean strafeAway) {
        return this.turn(entity.getLocation(), strafeAway);
    }

    public ShipCommand turnToward(float degreeAngle) {
        return this.turn(degreeAngle, false);
    }

    public ShipCommand turnToward(Vector2f location) {
        return this.turnToward(VectorUtils.getAngle(this.ship.getLocation(), location));
    }

    public ShipCommand turnToward(CombatEntityAPI entity) {
        return this.turnToward(entity.getLocation());
    }

    public ShipCommand turnAway(float degreeAngle) {
        return this.turn(degreeAngle, true);
    }

    public ShipCommand turnAway(Vector2f location) {
        return this.turnAway(VectorUtils.getAngle(this.ship.getLocation(), location));
    }

    public ShipCommand turnAway(CombatEntityAPI entity) {
        return this.turnAway(entity.getLocation());
    }

    public void accelerate() {
        this.ship.giveCommand(ShipCommand.ACCELERATE, (Object)null, 0);
    }

    public void accelerateBackward() {
        this.ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, (Object)null, 0);
    }

    public void decelerate() {
        this.ship.giveCommand(ShipCommand.DECELERATE, (Object)null, 0);
    }

    public void turnLeft() {
        this.ship.giveCommand(ShipCommand.TURN_LEFT, (Object)null, 0);
    }

    public void turnRight() {
        this.ship.giveCommand(ShipCommand.TURN_RIGHT, (Object)null, 0);
    }

    public void strafeLeft() {
        this.ship.giveCommand(ShipCommand.STRAFE_LEFT, (Object)null, 0);
    }

    public void strafeRight() {
        this.ship.giveCommand(ShipCommand.STRAFE_RIGHT, (Object)null, 0);
    }

    public void vent() {
        this.ship.giveCommand(ShipCommand.VENT_FLUX, (Object)null, 0);
    }

    public boolean useSystem() {
        boolean canDo = AIUtils.canUseSystemThisFrame(this.ship);
        if (canDo) {
            this.ship.giveCommand(ShipCommand.USE_SYSTEM, (Object)null, 0);
        }

        return canDo;
    }

    public void toggleDefenseSystem() {
        this.ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, (Object)null, 0);
    }

    public void fireSelectedGroup(Vector2f at) {
        this.ship.giveCommand(ShipCommand.FIRE, at, 0);
    }

    public void toggleAutofire(int group) {
        this.ship.giveCommand(ShipCommand.TOGGLE_AUTOFIRE, (Object)null, group);
    }

    public void selectWeaponGroup(int group) {
        this.ship.giveCommand(ShipCommand.SELECT_GROUP, (Object)null, group);
    }

    public RC_BaseShipAI(ShipAPI ship) {
        this.ship = ship;
    }

    public void advance(float amount) {
        this.circumstanceEvaluationTimer.advance(amount);
        if (this.circumstanceEvaluationTimer.intervalElapsed()) {
            this.evaluateCircumstances();
        }

    }

    public void forceCircumstanceEvaluation() {
        this.evaluateCircumstances();
    }

    public boolean needsRefit() {
        return false;
    }

    public void setDoNotFireDelay(float amount) {
        this.dontFireUntil = amount + Global.getCombatEngine().getTotalElapsedTime(false);
    }
}

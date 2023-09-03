package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

/**
 * 1、1000范围如果范围内有友军继续往前走
 * 2、有敌人逃跑
 * 3、有子弹相位
 */
public class RC_PhaseSpyOnFighterAI implements ShipAIPlugin {
    private final static String ID = "RC_PhaseSpyOnFighterAI";
    private CombatEngineAPI engine = Global.getCombatEngine();
    private ShipwideAIFlags AIFlags = new ShipwideAIFlags();
    private ShipAPI motherShip;
    private Vector2f startLocation;
    private Vector2f targetLocation;
    private boolean isStartLanding = false;
    private ShipAPI ship;
    protected float dontFireUntil = 0.0F;
    public RC_PhaseSpyOnFighterAI(ShipAPI ship, ShipAPI motherShip, Vector2f startLocation, Vector2f targetLocation) {
        this.ship = ship;
        this.motherShip = motherShip;
        this.targetLocation = targetLocation;
        this.startLocation = startLocation;
    }
    public boolean mayFire() {
        return this.dontFireUntil <= Global.getCombatEngine().getTotalElapsedTime(false);
    }
    public void evaluateCircumstances() {

    }
    @Override
    public void setDoNotFireDelay(float amount) {
        this.dontFireUntil = amount + Global.getCombatEngine().getTotalElapsedTime(false);
    }

    @Override
    public void forceCircumstanceEvaluation() {
        this.evaluateCircumstances();
    }
    @Override
    public boolean needsRefit() {
        return false;
    }
    /**
     * 40CR以下不修
     * 不修的时候返回母船
     * 母船炸了
     * @param amount
     */
    public void advance(float amount) {
        try {
            if (engine == null) return;
            if (engine.isPaused()) {return;}
            if (!ship.isAlive()) {return;}
            flyToTarget(amount);
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public ShipwideAIFlags getAIFlags() {
        return this.AIFlags;
    }

    public void cancelCurrentManeuver() {

    }

    public ShipAIConfig getConfig() {
        return null;
    }

    /**
     * 飞向目标
     *  百分比扣除CR
     */
    private void flyToTarget(float amount) {
        //首先判断周围有没有非友军子弹
        if (ship.getShield()!=null||ship.getPhaseCloak()!=null) {
            usePhase();
        }
        //首先判断周围有没有敌人
        if (ship.areAnyEnemiesInRange()) {
            ship.resetDefaultAI();
            ship.removeCustomData("RC_spy");
            /*
            ShipAPI enemy = AIUtils.getNearestEnemy(ship);
            float toTargetAngle = VectorUtils.getAngle(enemy.getLocation(),ship.getLocation());
            float shipFacing = MathUtils.clampAngle(ship.getFacing());
            float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
            turn(needTurnAngle, toTargetAngle, amount);
            ship.giveCommand(ShipCommand.ACCELERATE, (Object)null ,0);

             */
        }
        else {
            //与目标的距离
            float distance = MathUtils.getDistance(targetLocation,ship.getLocation());
            if(distance>50) {
                float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
                float shipFacing = MathUtils.clampAngle(ship.getFacing());
                float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
                turn(needTurnAngle, toTargetAngle, amount);
                ship.giveCommand(ShipCommand.ACCELERATE, (Object)null ,0);
            }
            else {
                ship.giveCommand(ShipCommand.DECELERATE, (Object)null ,0);
                //范围内有别的友   军
                ShipAPI f = AIUtils.getNearestAlly(ship);
                distance = MathUtils.getDistance(f,ship);
                if (distance<1000) {
                    float toTargetAngle = VectorUtils.getAngle(startLocation,targetLocation);
                    targetLocation = MathUtils.getPoint(targetLocation,1000,toTargetAngle);
                    int count = 0;
                    if (targetLocation.x>engine.getMapWidth()/2) {
                        targetLocation.setX(engine.getMapWidth()/2);
                        count++;
                    }
                    else if (targetLocation.x<-engine.getMapWidth()/2){
                        targetLocation.setX(-engine.getMapWidth()/2);
                        count++;
                    }

                    if (targetLocation.y>engine.getMapHeight()/2) {
                        targetLocation.setY(-engine.getMapHeight()/2);
                        count++;
                    }
                    else if (targetLocation.y<-engine.getMapHeight()/2){
                        targetLocation.setY(-engine.getMapHeight()/2);
                        count++;
                    }

                    if (count>=1) {
                        ship.removeCustomData("RC_spy");
                        ship.resetDefaultAI();
                    }
                }
            }
        }
    }

    private void turn(float needTurnAngle, float toTargetAngle, float amount){
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

    private void usePhase(){
        float useRange = ship.getCollisionRadius();
        //如果周围很多子弹
        int count = 0;
        boolean isProjectileMany = false;
        List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
        for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
            float range = MathUtils.getDistance(ship, damagingProjectile);
            if(range<=useRange&&damagingProjectile.getOwner()!=ship.getOwner()) {
                isProjectileMany = true;
                break;
            }
        }

        if(isProjectileMany) {
            if (!ship.isPhased()) {
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,null, 0);
            }
        }
        else {
            if (ship.isPhased()&&ship.getFluxTracker().getCurrFlux()>ship.getMaxFlux()/2) {
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,null, 0);
            }
        }
    }
}

package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.*;

public class RC_ModulesFighterAI implements ShipAIPlugin {
    private final static String ID = "RC_ModulesFighterAI";
    private CombatEngineAPI engine = Global.getCombatEngine();
    private ShipwideAIFlags AIFlags = new ShipwideAIFlags();
    private FighterLaunchBayAPI fighterLaunchBay;
    private ShipAPI motherShip;
    private ShipAPI backShip;
    private Vector2f landingLocation;
    private boolean isStartLanding = false;
    private ShipAPI ship;
    protected float dontFireUntil = 0.0F;
    public RC_ModulesFighterAI(ShipAPI ship,ShipAPI motherShip, ShipAPI backShip, FighterLaunchBayAPI fighterLaunchBay,Vector2f landingLocation) {
        this.ship = ship;
        this.motherShip = motherShip;
        this.backShip = backShip;
        this.fighterLaunchBay = fighterLaunchBay;
        this.landingLocation = landingLocation;
    }
    public RC_ModulesFighterAI(ShipAPI ship,ShipAPI motherShip) {
        this.ship = ship;
        this.motherShip = motherShip;
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
        Vector2f targetLocation = motherShip.getLocation();
        if (motherShip.isPhased())
        {
            targetLocation = backShip.getLocation();
        }
        if(landingLocation!=null) {
            targetLocation = landingLocation;
        }

        //如果距离比较远就加速
        //距离很近那就减速和飞船同步
        float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
        float radius = ship.getCollisionRadius()*2.5f;
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
        float distance = MathUtils.getDistance(ship,targetLocation);
        turn(needTurnAngle, toTargetAngle, amount);
        if(distance>radius)
        {
            if(isStartLanding) {
                ship.abortLanding();
                isStartLanding = false;
            }
            else {
                if (needTurnAngle<10) {
                    ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
                }
                else {
                    ship.giveCommand(ShipCommand.DECELERATE, (Object) null, 0);
                }
            }
        }
        //如果很近那就围绕飞船转圈
        else{
            if (ship.getVelocity().length()>ship.getMaxSpeed()/8) {
                ship.giveCommand(ShipCommand.DECELERATE, (Object) null, 0);
            }
            //开始播放动画
            if(!isStartLanding) {
                ship.beginLandingAnimation(motherShip);
                isStartLanding = true;
                //ship.getMutableStats().getMaxSpeed().modifyFlat(ID,motherShip.getMaxSpeed()+ship.getMaxSpeed());
            }
            if(ship.isFinishedLanding())
            {
                if(fighterLaunchBay!=null){
                    FighterWingAPI.ReturningFighter land = ship.getWing().getReturnData(ship);
                    land = new FighterWingAPI.ReturningFighter(ship,fighterLaunchBay);
                    fighterLaunchBay.land(ship);
                }
                else
                {
                    float height = Global.getCombatEngine().getMapHeight();
                    float width = Global.getCombatEngine().getMapWidth();
                    Vector2f newLocation = new Vector2f(ship.getOwner() == 0 ? -width * 4 : width * 4, ship.getOwner() == 0 ? -height * 4 : height * 4);
                    ship.setPhased(true);
                    //ship.resetDefaultAI();
                    ship.getLocation().set(newLocation);
                    //engine.removeObject(ship);
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
}

package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;
import java.util.List;

/**
 * 飞到目标船后面
 * 找到最高伤害武器且没有瘫痪的武器
 * 到武器的背后一飞机远
 *
 */
public class RC_CounterWeaponFighterAI extends RC_BaseShipAI {
    public final static String ID = "RC_CounterWeaponFighterAI";
    private ShipAPI target;
    protected IntervalUtil tracker = new IntervalUtil(1.0f, 2.0f);
    private float timer = 0f;
    private Stage stage = Stage.GO_TO_TARGET;
    private float maxDistance = 9999F;
    private float minDistance = 0F;

    public RC_CounterWeaponFighterAI(ShipAPI ship) {
        super(ship);
    }
    public static enum Stage {
        GO_TO_TARGET,
        GO_TO_TARGET_BACK,
        GO_TO_WEAPON_BACK
    }
    /**
     * @param amount
     */
    @Override
    public void advance(float amount) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        if (!ship.isAlive()) {return;}
        //super.advance(amount);
        try {
            //获取飞机的半径
            FighterWingAPI wing = ship.getWing();
            if (wing != null) {
                if (wing.getSourceShip().isPullBackFighters()) {
                    ship.resetDefaultAI();
                    ship.removeCustomData(ID);
                    return;
                }
                if (target==null) {
                    target = wing.getSourceShip().getShipTarget();
                }
                if (target!=null) {
                    if (!target.isAlive()) {
                        target = null;
                    }
                    else if (MathUtils.getDistance(target,wing.getSourceShip())>wing.getRange()) {
                        target = null;
                    }
                }
                if (target==null) {
                    ShipAPI enemy =  AIUtils.getNearestEnemy(ship);
                    if (enemy!=null) {
                        if (enemy.isAlive()) {
                            target = enemy;
                        }
                    }
                }
                if (target==null) {
                    target = wing.getSourceShip();
                }

                if (ship.getSystem() != null && !ship.getFluxTracker().isOverloaded()) {
                    boolean hasAIScript = false;
                    if (ship.getSystem().getSpecAPI() != null) {
                        if (ship.getSystem().getSpecAPI().getAIScript() != null) {
                            hasAIScript = true;
                        }
                    }
                    if (hasAIScript && target != null) {
                        ship.getSystem().getSpecAPI().getAIScript().advance(amount, ship.getLocation(), ship.getLocation(), target);
                    } else {
                        useSystem();
                    }
                }

                if (ship.getPhaseCloak()!=null) {
                    usePhase(amount);
                }
                else if (ship.getShield()!=null) {
                    useShield(amount);
                }
                else if (stage==Stage.GO_TO_TARGET){
                    float minDistance = ship.getMaxSpeed();
                    DamagingProjectileAPI targetProjectile = null;
                    List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
                    for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
                        float range = MathUtils.getDistance(ship, damagingProjectile);
                        if (range <= minDistance+damagingProjectile.getVelocity().length() && damagingProjectile.getOwner() != ship.getOwner() && !damagingProjectile.isFading() && !damagingProjectile.isExpired()) {
                            minDistance = range;
                            targetProjectile = damagingProjectile;
                        }
                    }
                    if (targetProjectile != null) {
                        if (dodge(targetProjectile)) {
                            turn(amount);
                            return;
                        }
                    }
                }
                if (target!=null) {
                    //飞过去
                    setStage(target, amount);
                    flyToTargetNew(target, amount);
                    return;
                }
            }
            else {
                if (target==null) {
                    ShipAPI enemy =  AIUtils.getNearestEnemy(ship);
                    if (enemy!=null) {
                        if (enemy.isAlive()) {
                            target = enemy;
                        }
                    }
                }
                else if (!target.isAlive()) {
                    target = null;
                    return;
                }

                if (ship.getSystem() != null && !ship.getFluxTracker().isOverloaded()) {
                    boolean hasAIScript = false;
                    if (ship.getSystem().getSpecAPI() != null) {
                        if (ship.getSystem().getSpecAPI().getAIScript() != null) {
                            hasAIScript = true;
                        }
                    }
                    if (hasAIScript && target != null) {
                        ship.getSystem().getSpecAPI().getAIScript().advance(amount, ship.getLocation(), ship.getLocation(), target);
                    } else {
                        useSystem();
                    }
                }

                float minDistance = ship.getMaxSpeed();
                DamagingProjectileAPI targetProjectile = null;
                List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
                for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
                    float range = MathUtils.getDistance(ship, damagingProjectile);
                    if (range <= minDistance+damagingProjectile.getVelocity().length() && damagingProjectile.getOwner() != ship.getOwner() && !damagingProjectile.isFading() && !damagingProjectile.isExpired()) {
                        minDistance = range;
                        targetProjectile = damagingProjectile;
                    }
                }
                if (targetProjectile != null) {
                    if(dodge(targetProjectile)){
                        turn(amount);
                        return;
                    }
                }
                if (target!=null) {
                    //飞过去
                    flyToTargetNew(target, amount);
                    return;
                }
            }

            ShipAPI motherShip = null;
            float mindistance = 999999f;
            for (ShipAPI a:RC_BaseShipAI.getAlliesOnMapNotFighter(ship,new HashSet<ShipAPI>())) {
                if (MathUtils.getDistance(a,ship)<mindistance&&!a.isFighter()) {
                    motherShip = a;
                    mindistance = MathUtils.getDistance(a,ship);
                }
            }
            if (motherShip!=null) {
                flyToTarget(motherShip, amount);
                if (MathUtils.getDistance(ship.getLocation(), motherShip.getLocation()) < motherShip.getCollisionRadius()) {
                    if (ship.getPhaseCloak() != null) {
                        if (ship.isPhased()) {
                            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    /**
     * 飞向目标 阶段1
     * 靠近目标后 飞向目标 屁股 阶段2
     * 到达目标屁股后面 速度差值 采用平移的方式取消 阶段3
     */
    public void flyToTarget(ShipAPI target, float amount) {
        Vector2f targetLocation = target.getLocation();
        //如果距离比较远就加速
        //距离很近那就减速和飞船同步
        float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
        float distance = MathUtils.getDistance(target.getLocation(),ship.getLocation());
        float range = target.getCollisionRadius()*1.5f;
        float minDistance = 300f;
        if (target.getCollisionRadius()*3f>300f) {
            minDistance = target.getCollisionRadius()*3f;
        }
        if(distance>minDistance)
        {
            if (!target.getFluxTracker().isOverloaded()||!target.getFluxTracker().isVenting()) {
                if (MathUtils.getShortestRotation(shipFacing, toTargetAngle) > 0) {
                    toTargetAngle = toTargetAngle-90;
                    Vector2f targetPoint = MathUtils.getPointOnCircumference(target.getLocation(),range,toTargetAngle);
                    toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetPoint);
                    float mi = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount,toTargetAngle));
                    RC_BaseAIAction.turn(ship, mi, toTargetAngle, amount);
                } else {
                    toTargetAngle = toTargetAngle+90;
                    Vector2f targetPoint = MathUtils.getPointOnCircumference(target.getLocation(),range,toTargetAngle);
                    toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetPoint);
                    float mi = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount,toTargetAngle));
                    RC_BaseAIAction.turn(ship, mi, toTargetAngle, amount);
                }
            }
            else {
                RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
            }
            ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
            if (needTurnAngle<10f) {
                for (int groupNumber=0;groupNumber<ship.getWeaponGroupsCopy().size();groupNumber++)
                {
                    ship.getWeaponGroupsCopy().get(groupNumber).toggleOn();
                }
            }
            else {
                for (int groupNumber=0;groupNumber<ship.getWeaponGroupsCopy().size();groupNumber++)
                {
                    ship.getWeaponGroupsCopy().get(groupNumber).toggleOff();
                }
            }
        }
        //如果很近那就围绕飞船转圈
        else{
            RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
            if (!target.getFluxTracker().isOverloaded()||!target.getFluxTracker().isVenting()) {
                if (Math.abs(MathUtils.getShortestRotation(toTargetAngle,target.getFacing()))>30) {
                    RC_BaseAIAction.shift(ship, toTargetAngle, target.getFacing());
                }
                else {
                    if (ship.getVelocity().length()>ship.getMaxSpeed()/2) {
                        ship.giveCommand(ShipCommand.DECELERATE, (Object) null, 0);
                    }
                    else {
                        ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
                    }
                }
            }
            if (distance<=target.getCollisionRadius()*1.5f) {
                ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, (Object) null, 0);
            }
            if (needTurnAngle<10f) {
                for (int groupNumber=0;groupNumber<ship.getWeaponGroupsCopy().size();groupNumber++)
                {
                    ship.getWeaponGroupsCopy().get(groupNumber).toggleOn();
                }
            }
            else {
                for (int groupNumber=0;groupNumber<ship.getWeaponGroupsCopy().size();groupNumber++)
                {
                    ship.getWeaponGroupsCopy().get(groupNumber).toggleOff();
                }
            }
        }
    }

    /**
     *
     * @param target
     * @param amount
     */
    public void setStage(ShipAPI target, float amount) {
        Vector2f targetLocation = target.getLocation();
        float range = target.getCollisionRadius();
        float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
        if (minDistance==0f&&maxDistance==9999f) {
            for (WeaponAPI w : ship.getAllWeapons()) {
                if (w.getRange() < minDistance) {
                    minDistance = w.getRange();
                }
                if (w.getRange() > maxDistance) {
                    maxDistance = w.getRange();
                }
            }
        }
        //如果距离太远的话先到船的侧边
        float distance = MathUtils.getDistance(target.getLocation(),ship.getLocation());
        if (distance>maxDistance||distance>range*1.5) {
            stage = Stage.GO_TO_TARGET;
        }
        //如果够近了就去后面
        else if (Math.abs(MathUtils.getShortestRotation(toTargetAngle,target.getFacing()))>30&&stage != Stage.GO_TO_TARGET_BACK) {
            stage = Stage.GO_TO_TARGET_BACK;
        }
        else if (Math.abs(MathUtils.getShortestRotation(toTargetAngle,target.getFacing()))<=30&&stage == Stage.GO_TO_TARGET_BACK) {
            stage = Stage.GO_TO_WEAPON_BACK;
        }
    }


    /**
     * 飞向目标 阶段1
     * 靠近目标后 飞向目标 屁股 阶段2
     * 到达目标屁股后面 速度差值 采用平移的方式取消
     * 假设飞机 垂直 速度180差度
     * 速度插值最大30
     */
    public void flyToTargetNew(ShipAPI target, float amount) {
        Vector2f targetLocation = target.getLocation();
        //如果距离比较远就加速
        //距离很近那就减速和飞船同步
        float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
        float distance = MathUtils.getDistance(target.getLocation(),ship.getLocation());
        float range = target.getCollisionRadius();
        if (stage==Stage.GO_TO_TARGET) {
            //绕圈飞 直到飞到屁股
            if (MathUtils.getShortestRotation(shipFacing, toTargetAngle) > 0) {
                toTargetAngle = toTargetAngle - 90;
                Vector2f targetPoint = MathUtils.getPointOnCircumference(target.getLocation(), range * 1.5f, toTargetAngle);
                toTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                float mi = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
                RC_BaseAIAction.turn(ship, mi, toTargetAngle, amount);
            } else {
                toTargetAngle = toTargetAngle + 90;
                Vector2f targetPoint = MathUtils.getPointOnCircumference(target.getLocation(), range * 1.5f, toTargetAngle);
                toTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                float mi = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
                RC_BaseAIAction.turn(ship, mi, toTargetAngle, amount);
            }
            ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
        }
        else if (stage==Stage.GO_TO_TARGET_BACK) {
            RC_BaseAIAction.shift(ship, ship.getFacing(),target.getFacing());
            //直接向着目标加速
            if (distance>minDistance) {
                ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
            }
            else if (distance<range) {
                ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, (Object) null, 0);
            }
            RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
        }
        else {
            WeaponAPI max = null;
            WeaponAPI.WeaponSize size = WeaponAPI.WeaponSize.SMALL;
            float damage = 0f;
            for (WeaponAPI w:target.getAllWeapons()) {
                if (w.getType()!= WeaponAPI.WeaponType.MISSILE&&!w.isDisabled()&&w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD)) {
                    if (w.getDamage().getDpsDuration()>damage) {
                        damage = w.getDamage().getDpsDuration();
                        max = w;
                    }
                }
            }
            if (max==null) {
                RC_BaseAIAction.shift(ship, ship.getFacing(),target.getFacing());
                //直接向着目标加速
                if (distance>minDistance) {
                    ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
                }
                else if (distance<range) {
                    ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, (Object) null, 0);
                }
                RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
            }
            else {
                targetLocation = MathUtils.getPoint(max.getLocation(),ship.getCollisionRadius(),max.getCurrAngle()+180);
                toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
                float needTurnAngleNew = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
                RC_BaseAIAction.move(ship, shipFacing, toTargetAngle);
                RC_BaseAIAction.turn(ship, needTurnAngleNew, toTargetAngle, amount);
            }
        }
        if (needTurnAngle<10f) {
            for (int groupNumber=0;groupNumber<ship.getWeaponGroupsCopy().size();groupNumber++)
            {
                ship.getWeaponGroupsCopy().get(groupNumber).toggleOn();
            }
        }
        else {
            for (int groupNumber=0;groupNumber<ship.getWeaponGroupsCopy().size();groupNumber++)
            {
                ship.getWeaponGroupsCopy().get(groupNumber).toggleOff();
            }
        }
    }
}

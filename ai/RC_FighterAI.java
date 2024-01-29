package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.prototype.entities.Ship;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lazywizard.lazylib.combat.DefenseUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * 闪避 扫描附近的子弹 子弹速度和舰船速度夹角 小于 子弹到舰船
 * 护盾控制 相位控制 阻尼控制
 * 进攻
 * 护卫 转向 最近的导弹武器开火 和母舰保持速度
 */
public class RC_FighterAI extends RC_BaseShipAI {
    private final static String ID = "RC_FighterAI";
    private ShipAPI target;
    protected IntervalUtil tracker = new IntervalUtil(1.0f, 2.0f);
    private float timer = 0f;

    public RC_FighterAI(ShipAPI ship) {
        super(ship);
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
                    //ship.setShipAI((ShipAIPlugin) ship.getCustomData().get(ID));
                    //ship.removeCustomData(ID);
                    //return;
                    target = wing.getSourceShip();
                }

                if (ship.getPhaseCloak()!=null) {
                    usePhase(amount);
                }
                else if (ship.getShield()!=null) {
                    useShield(amount);
                }
                else {
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
                }
                if (target!=null) {
                    //飞过去
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
            for (ShipAPI a:AIUtils.getAlliesOnMap(ship)) {
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
        /*
        engine.addFloatingTextAlways(ship.getLocation(), "进攻", 25f, Color.RED, ship,
                1, 1, amount,0, 0, 1f);
         */
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
     * 飞向目标 阶段1
     * 靠近目标后 飞向目标 屁股 阶段2
     * 到达目标屁股后面 速度差值 采用平移的方式取消
     * 假设飞机 垂直 速度180差度
     * 速度插值最大30
     */

    public void flyToTargetNew(ShipAPI target, float amount) {
        //engine.addFloatingTextAlways(ship.getLocation(), "进攻", 25f, Color.RED, ship,
        //        1, 1, amount,0, 0, 1f);
        Vector2f targetLocation = target.getLocation();
        //如果距离比较远就加速
        //距离很近那就减速和飞船同步
        float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
        float distance = MathUtils.getDistance(target.getLocation(),ship.getLocation());
        float range = target.getCollisionRadius();
        float minDistance = 9999;
        float maxDistance = 0;
        for (WeaponAPI w:ship.getAllWeapons()) {
            if (w.getRange()<minDistance) {
                minDistance = w.getRange();
            }
            if (w.getRange()>maxDistance) {
                maxDistance = w.getRange();
            }
        }
        if (distance>maxDistance||target.getOwner()==ship.getOwner()||target.getOwner()==100) {
            //每到屁股
            if (Math.abs(MathUtils.getShortestRotation(toTargetAngle,target.getFacing()))>30) {
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
                return;
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
        RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
        //到达屁股
        if (Math.abs(MathUtils.getShortestRotation(toTargetAngle,target.getFacing()))<=30) {
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
                //减速完成以后要面向目标 然后漂移
                if (Math.abs(MathUtils.getShortestRotation(target.getFacing(),ship.getFacing()))>10) {
                    RC_BaseAIAction.shift(ship, ship.getFacing(),target.getFacing());
                }
            }
            if (needTurnAngle<10) {
                //直接向着目标加速
                if (distance>minDistance) {
                    ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
                }
                else if (distance<range) {
                    ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, (Object) null, 0);
                }
            }
        }
        else {
            RC_BaseAIAction.shift(ship, ship.getFacing(),target.getFacing());
            //直接向着目标加速
            if (distance>minDistance) {
                ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
            }
            else if (distance<range) {
                ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, (Object) null, 0);
            }
        }
    }
}

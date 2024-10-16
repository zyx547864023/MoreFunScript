package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

/**
 * 闪避 扫描附近的子弹 子弹速度和舰船速度夹角 小于 子弹到舰船
 * 护盾控制 相位控制 阻尼控制
 * 进攻
 * 护卫 转向 最近的导弹武器开火 和母舰保持速度
 */
public class RC_BomberAttackAI extends RC_BaseShipAI {
    public final static String ID = "RC_BomberAttackAI";
    public final static String CATCHED = "CATCHED";
    private CombatEntityAPI target;
    private CombatEntityAPI ally;
    protected IntervalUtil tracker = new IntervalUtil(1.0f, 2.0f);
    private float timer = 0f;

    public RC_BomberAttackAI(ShipAPI ship) {
        super(ship);
    }

    /**
     * 是否周围有友军舰船 【重置AI】
     * 母舰是否选取攻击目标 以及 放飞飞机 【重置AI】
     * 与目标距离 是否 进入射程 【重置AI】
     * 目标距离 友军距离 相加 最小 【向友军移动】
     * 到了友军 就面相目标 否则 面向 友军
     *
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
                if (wing.getSourceShip().isPullBackFighters()||!wing.getSourceShip().isAlive()) {
                    ship.resetDefaultAI();
                    ship.removeCustomData(ID);
                    return;
                }
                if (MathUtils.getDistance(ship.getLocation(),wing.getSourceShip().getLocation())>wing.getRange()) {
                    ship.resetDefaultAI();
                    ship.removeCustomData(ID);
                    return;
                }
                if (wing.getSourceShip().getShipTarget()==null) {
                    ship.resetDefaultAI();
                    ship.removeCustomData(ID);
                    return;
                }
                target = wing.getSourceShip().getShipTarget();
                if (MathUtils.getDistance(ship.getLocation(),target.getLocation())>wing.getRange()) {
                    ship.resetDefaultAI();
                    ship.removeCustomData(ID);
                    return;
                }
                int ammo = 0;
                float minWeaponRange = wing.getRange();
                for (WeaponAPI w:ship.getAllWeapons()) {
                    if (w.usesAmmo()) {
                        ammo+=w.getAmmo();
                    }
                    if (w.getRange()<minWeaponRange) {
                        minWeaponRange = w.getRange();
                    }
                }
                if (ammo==0) {
                    ship.resetDefaultAI();
                    ship.removeCustomData(ID);
                    return;
                }
                //目标周围没有友军 找掩体
                float toTargetDistance = MathUtils.getDistance(ship.getLocation(),target.getLocation()) - 1f; //略小于这样就会查到
                List<ShipAPI> allyList = AIUtils.getNearbyEnemies(target,toTargetDistance);
                float minAngle = 90f;
                for (ShipAPI a:allyList) {
                    if (!a.isFighter()) {
                        float aToTargetAngle = VectorUtils.getAngle(a.getLocation(),target.getLocation());
                        float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(),target.getLocation());
                        if (minAngle > Math.abs(MathUtils.getShortestRotation(aToTargetAngle,shipToTargetAngle))) {
                            minAngle = Math.abs(MathUtils.getShortestRotation(aToTargetAngle,shipToTargetAngle));
                            ally = a;
                        }
                    }
                }
                if (ally==null) {
                    ship.resetDefaultAI();
                    ship.removeCustomData(ID);
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
                        if (target instanceof ShipAPI) {
                            ship.getSystem().getSpecAPI().getAIScript().advance(amount, ship.getLocation(), ship.getLocation(), (ShipAPI) target);
                        }
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
                //向友军进发
                if (ally!=null) {
                    //如果离友军很远绕行
                    if (MathUtils.getDistance(ship,ally)>0) {
                        //如果角度插值大于5
                        float shipFacing = MathUtils.clampAngle(ship.getFacing());
                        float aToTargetAngle = VectorUtils.getAngle(ally.getLocation(),target.getLocation());
                        float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(),target.getLocation());
                        float angle = Math.abs(MathUtils.getShortestRotation(aToTargetAngle,shipToTargetAngle));
                        float toTargetAngle = shipToTargetAngle;
                        if (angle > 5) {
                            //ship绕目标行走
                            if (MathUtils.getShortestRotation(aToTargetAngle,shipToTargetAngle)>0) {
                                toTargetAngle = shipToTargetAngle+90;
                            }
                            else {
                                toTargetAngle = shipToTargetAngle-90;
                            }
                            float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
                            RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
                            ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
                        }
                        else {
                            toTargetAngle = VectorUtils.getAngle(ship.getLocation(),ally.getLocation());
                            float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
                            RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
                            ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
                        }
                    }
                    else {
                        float shipFacing = MathUtils.clampAngle(ship.getFacing());
                        float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(),target.getLocation());
                        float toTargetAngle = shipToTargetAngle;
                        float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
                        RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
                        ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
                    }
                }
            }
            else {
                ship.resetDefaultAI();
                ship.removeCustomData(ID);
                return;
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
     * 到达目标屁股后面 速度差值 采用平移的方式取消
     * 假设飞机 垂直 速度180差度
     * 速度插值最大30
     */

    public void flyToTargetNew(CombatEntityAPI target, float amount) {
        Vector2f targetLocation = target.getLocation();
        //如果距离比较远就加速
        if (MathUtils.getDistance(target.getLocation(),ship.getLocation())<target.getCollisionRadius()) {
            if (ship.getVelocity().length()>ship.getMaxSpeed()/2) {
                ship.giveCommand(ShipCommand.DECELERATE, (Object) null, 0);
            }
            else {
                ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
            }
        }
        else {
            ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
        }
        //距离很近那就减速和飞船同步
        float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));

        RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
        //到达屁股
        RC_BaseAIAction.move(ship,ship.getFacing(),toTargetAngle);
    }

    /**
     * 对于小船来说碰撞是致命的 远离所有 靠近半径的船 最优先
     */
    @Override
    public void beforeFlyToTarget() {
        //与最近的敌人保持最短攻击距离
        if (nearestEnemy!=null) {
            if (nearestEnemy.minDistance < minWeaponRange) {
                RC_BaseAIAction.move(ship, ship.getFacing(), VectorUtils.getAngle(nearestEnemy.ship.getLocation(), ship.getLocation()));
                isDodge = true;
            }
        }
    }
}

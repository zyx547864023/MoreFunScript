package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.shipsystems.scripts.RC_AsteroidArm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 闪避 扫描附近的子弹 子弹速度和舰船速度夹角 小于 子弹到舰船
 * 护盾控制 相位控制 阻尼控制
 * 进攻
 * 护卫 转向 最近的导弹武器开火 和母舰保持速度
 */
public class RC_BomberAI extends RC_BaseShipAI {
    public final static String ID = "RC_BomberAI";
    public final static String CATCHED = "CATCHED";
    private CombatEntityAPI target;
    protected IntervalUtil tracker = new IntervalUtil(1.0f, 2.0f);
    private float timer = 0f;

    public RC_BomberAI(ShipAPI ship) {
        super(ship);
    }

    /**
     * 躲避敌人并寻找陨石
     * 如果已经找到陨石重置
     * 如果母舰找回重置
     * 速度变更为速度*质量/陨石+自己质量
     *
     * 如果屏幕内没有陨石则重置AI
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
                if (!wing.getSourceShip().isPullBackFighters()||!wing.getSourceShip().isAlive()) {
                    ship.resetDefaultAI();
                    ship.removeCustomData(ID);
                    return;
                }
                if (MathUtils.getDistance(ship.getLocation(),wing.getSourceShip().getLocation())>wing.getRange()) {
                    ship.resetDefaultAI();
                    ship.removeCustomData(ID);
                    return;
                }
                int ammo = 0;
                for (WeaponAPI w:ship.getAllWeapons()) {
                    if (w.usesAmmo()) {
                        ammo+=w.getAmmo();
                    }
                }
                if (ammo==0) {
                    ship.resetDefaultAI();
                    ship.removeCustomData(ID);
                    return;
                }
                if (target==null) {
                    float minDistance = wing.getRange();
                    for (ShipAPI s : engine.getShips()) {
                        if (s.isHulk()&&s.getCustomData().get(RC_AsteroidArm.WHO_CATCH)==null&&s.getCollisionRadius()>ship.getCollisionRadius()) {
                            float distance = MathUtils.getDistance(s,ship);
                            if (distance<minDistance) {
                                minDistance = distance;
                                target = s;
                            }
                        }
                    }
                    for (CombatEntityAPI a : engine.getAsteroids()) {
                        float distance = MathUtils.getDistance(a,ship);
                        if (distance<minDistance&&a.getCustomData().get(RC_AsteroidArm.WHO_CATCH)==null&&a.getCollisionRadius()>ship.getCollisionRadius()) {
                            minDistance = distance;
                            target = a;
                        }
                    }
                }
                if (target==null) {
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
                if (target!=null) {
                    target.setCustomData(RC_AsteroidArm.WHO_CATCH, ship);
                    beforeFlyToTarget();
                    if (isDodge&&nearestEnemy!=null) {
                        RC_BaseAIAction.turn(ship, nearestEnemy.ship.getLocation(), amount);
                    }
                    //飞过去
                    if (!isDodge) {
                        flyToTargetNew(target, amount);
                    }
                    if (MathUtils.getDistance(ship.getLocation(),target.getLocation())<5f) {
                        ship.getCustomData().put(ID+CATCHED,MathUtils.getShortestRotation(ship.getFacing(),target.getFacing()));
                        Set<CombatEntityAPI> hulkList = new HashSet<>();
                        if (engine.getCustomData().get("HULK_LIST")!=null) {
                            hulkList = (Set<CombatEntityAPI>) engine.getCustomData().get("HULK_LIST");
                        }
                        hulkList.add(target);
                        engine.getCustomData().put("HULK_LIST",hulkList);
                        ship.getLocation().set(target.getLocation());
                        ship.resetDefaultAI();
                        ship.removeCustomData(ID);
                        return;
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

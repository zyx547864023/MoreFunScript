package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.ai.RC_AIContants;
import real_combat.ai.RC_BaseAIAction;
import real_combat.entity.RC_NeedDrawLine;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * TODO
 * 对自己最大威胁 目标和最大威胁对象不一致时 远离最大威胁对象
 * 残血 有残血优先进攻残血
 *
 * 进攻目标
 * 一定是 射程内
 * 残血优先
 * 过载优先
 * 比自己弱优先
 * 全射程内优先
 *
 * 先循环 全射程内的 舰船
 * 有没有过载的 有没有 残血的 设置为目标
 * 再找 有没有 比自己弱的
 * 再找 最近的
 *
 * 为空
 *
 * 循环最远射程的内的
 * 有没有过载的 有没有 残血的 设置为目标
 * 再找 有没有 比自己弱的
 * 再找 最近的
 *
 * 如果还没有 就 最近的敌人
 *
 *
 * 移动对象
 * 攻击对象
 * 路径上的遮挡 如果other 自己人 或者 死人 直接面相other的环绕点地方移动
 *
 *
 * 移动目标 v2 没有minWeapon近的时候 面相移动目标
 * 面相目标 v2
 * 实际目标 ship
 *
 * 优先解决 最边缘的 舰船 不对 判断x差值和y差值 取差值最大的一方
 *
 */
public class RC_BaseShipAI implements ShipAIPlugin {
    public CombatEngineAPI engine = Global.getCombatEngine();
    public ShipAPI ship;
    public ShipAPI oldTarget = null;
    public ShipAPI target;

    public Vector2f moveLocation;
    public Vector2f faceLocation;

    public ShipAPI leader = null;
    public boolean isLeader = false;
    public boolean isDodge = false;
    public int beTargetCount = 0;
    /**
     * 变为不是leader或者死亡时释放team成员 更换目标也需要
     */
    public Set<ShipAPI> teamList = new HashSet<>();
    protected float dontFireUntil = 0.0F;
    private ShipwideAIFlags AIFlags = new ShipwideAIFlags();
    protected IntervalUtil tracker = new IntervalUtil(1.0f, 2.0f);
    private Map<WeaponGroupAPI,List<WeaponAPI>> weaponGroup = new HashMap<>();
    private Map<WeaponGroupSpec,List<String>> weaponGroupSpec = new HashMap<>();

    public float maxWeaponRange = 0.0F;
    public float minWeaponRange = 9999F;
    public float minWingRange = 9999F;
    float maxDamage = 0;

    float maxFleetPoint = 0;

    float myFleetPoint = 0;
    float enemyFleetPoint = 0;

    //一次循环找到所有目标
    //最近的目标
    ShipAndDistance nearestEnemy = null;
    ShipAndDistance nearestEnemyNotFighter =null;
    //最近且比自己大的目标
    ShipAndDistance nearestBiggerEnemy = null;
    //最近的船
    ShipAndDistance nearestShip = null;
    //最近且比自己大的船
    ShipAndDistance nearestBiggerShip = null;
    ShipAndDistance nearestBiggerAlly = null;
    //
    ShipAndDistance backBiggerAlly = null;
    //挡路的船
    ShipAndDistance other = null;
    //比自己大的船
    List<ShipAPI> biggerList = new ArrayList<>();
    List<ShipAPI> biggerEnemyInRangeList = new ArrayList<>();
    List<ShipAPI> enemyList = new ArrayList<>();
    List<ShipAPI> fighterList = new ArrayList<>();
    List<ShipAPI> coverList = new ArrayList<>();

    List<ShipAPI> myFighterList = new ArrayList<>();
    Set<MissileAPI> missileList = new HashSet<>();
    Set<WeaponAPI> missileWeaponList = new HashSet<>();
    Set<WeaponAPI> disabledWeaponList = new HashSet<>();

    Set<BeamAPI> mayHitBeam = new HashSet<>();

    Set<CombatEntityAPI> mayHitProj = new HashSet<>();

    Set<ShipAPI> allyList = new HashSet<>();

    //用来装载屁股后卫
    //ShipAPI backShip;
    //ShipAPI leftShip;
    //ShipAPI rightShip;

    //最大非导弹伤害武器 保证主武器在射程内

    public RC_BaseShipAI(ShipAPI ship) {
        this.ship = ship;
    }
    //
    /**
     * 面向最近且大于自己且在攻击范围内的敌人
     * 面向最近的敌人
     * 移动向着大于自己的敌人
     *
     * @param amount
     */
    @Override
    public void advance(float amount) {
        if (engine == null) return;
        if (engine.getCustomData().get("RC_SmartAIEveryFrameCombatPluginenemyList")!=null) {
            allyList = (Set<ShipAPI>) engine.getCustomData().get("RC_SmartAIEveryFrameCombatPluginenemyList");
        }
        if (!ship.isAlive()) {
            //刷新可用队友
            allyList.remove(ship);
            //把队友重新加回队友池子
            allyList.addAll(teamList);
            refreshHullSize();
            //这里有put
            engine.getCustomData().put("RC_SmartAIEveryFrameCombatPluginenemyList", allyList);

            if (target!=null) {
                int count = 0;
                if (target.getCustomData().get("findBestTarget") != null) {
                    count = (int) target.getCustomData().get("findBestTarget");
                    count--;
                }
                target.setCustomData("findBestTarget",count);
            }
            return;
        }

        if (ship.getCustomData().get("setWeaponGroup") == null) {
            setWeaponGroup();
            //这里有put
            ship.getCustomData().put("setWeaponGroup", "setWeaponGroup");
        }
        if (ship.getCustomData().get("setWeaponGroup") != null) {
            weaponController();
        }

        vent();
        refreshHullSize();
        setLeader();
        findBestTarget();
        releaseTeam();
        findAlly();
        drawLine();

        if (target == null) {
            Global.getLogger(this.getClass()).info("怎么可能1");
        } else if (target.isHulk() || !target.isAlive()) {
            Global.getLogger(this.getClass()).info("怎么可能2");
        }

        //要分开写阻尼 TODO
        if (ship.getPhaseCloak() != null) {
            usePhase(amount);
        } else if (ship.getShield() != null) {
            useShield(amount);
        }
        //如果屁股后面有
        if (!isDodge) {
            //如果没有发现任何敌人
            if (target != null) {
                turn(amount);
            }
            if (!isDodge) {
                beforeFlyToTarget();
            }
            if (target != null && !isDodge) {
                if (other == null) {
                    flyToTarget(target, null, amount);
                } else if (other.ship.isFighter()) {
                    flyToTarget(target, null, amount);
                } else {
                    flyToTarget(target, other.ship, amount);
                }
            } else {
                //Global.getLogger(this.getClass()).info("怎么可能3");
            }
        } else {
            Global.getLogger(this.getClass()).info("怎么可能4");
        }

        isDodge = false;

        pullBackFighters();

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

    }

    /**
     * 优化算法 targetFacing 和 targetToSihp 差值
     * @param dodgeTarget 闪避目标
     * @return
     */
    public boolean dodge(DamagingProjectileAPI dodgeTarget) {
        float dodgeTargetFacing = dodgeTarget.getFacing();
        float dodgeTargetToShip = VectorUtils.getAngle(dodgeTarget.getLocation(),ship.getLocation());
        float newAngle = dodgeTargetToShip - 90;
        if (MathUtils.getShortestRotation(dodgeTargetFacing, dodgeTargetToShip)>0) {
            newAngle = dodgeTargetToShip + 90;
        }
        if (mayHit(dodgeTarget,ship.getCollisionRadius()*3f)
        ) {
            float shipFacing = MathUtils.clampAngle(ship.getFacing());
            float accelerate = MathUtils.getShortestRotation(shipFacing,newAngle);
            if (accelerate<45) {
                ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, (Object) null, 0);
            }
            float backwards = MathUtils.getShortestRotation(shipFacing + 180f,newAngle);
            if (backwards<45) {
                ship.giveCommand(ShipCommand.ACCELERATE, (Object) null, 0);
            }
            float strafeLeft = MathUtils.getShortestRotation(shipFacing - 90f,newAngle);
            if (strafeLeft<45) {
                ship.giveCommand(ShipCommand.STRAFE_LEFT, (Object) null, 0);
            }
            float strafeRight = MathUtils.getShortestRotation(shipFacing + 90f,newAngle);
            if (strafeRight<45) {
                ship.giveCommand(ShipCommand.STRAFE_RIGHT, (Object) null, 0);
            }
            return true;
        }
        return false;
    }

    /**
     * 给飞机用radius大一些
     * @param dodgeTarget
     * @param radius
     * @return
     */
    public boolean mayHit(DamagingProjectileAPI dodgeTarget,float radius) {
        if (dodgeTarget.getOwner() == ship.getOwner() || dodgeTarget.isFading() || dodgeTarget.isExpired()){
            return false;
        }
        float dodgeTargetFacing = dodgeTarget.getFacing();
        float dodgeTargetToShip = VectorUtils.getAngle(dodgeTarget.getLocation(),ship.getLocation());
        float newAngle = dodgeTargetToShip - 90;
        if (MathUtils.getShortestRotation(dodgeTargetFacing, dodgeTargetToShip)>0) {
            newAngle = dodgeTargetToShip + 90;
        }
        Vector2f shipRoundPoint = MathUtils.getPoint(ship.getLocation(), radius, newAngle);
        float dodgeTargetToShipRoundPoint = VectorUtils.getAngle(dodgeTarget.getLocation(), shipRoundPoint);
        if (Math.abs(MathUtils.getShortestRotation(dodgeTargetFacing, dodgeTargetToShip))
                < Math.abs(MathUtils.getShortestRotation(dodgeTargetToShip, dodgeTargetToShipRoundPoint))
        ) {
            if (dodgeTarget.getDamage().getDamage()>ship.getHitpoints()/10) {
                mayHitProj.add(dodgeTarget);
            }
            if (dodgeTarget instanceof MissileAPI) {
                missileList.add((MissileAPI) dodgeTarget);
            }
            return true;
        }
        return false;
    }
    /**
     * 给飞机用radius大一些
     * @param beam
     * @param radius
     * @return
     */
    public boolean mayHit(BeamAPI beam,float radius) {

        float dodgeTargetFacing = VectorUtils.getAngle(beam.getFrom(),beam.getTo());
        float dodgeTargetToShip = VectorUtils.getAngle(beam.getFrom(),ship.getLocation());
        float newAngle = dodgeTargetToShip - 90;
        if (MathUtils.getShortestRotation(dodgeTargetFacing, dodgeTargetToShip)>0) {
            newAngle = dodgeTargetToShip + 90;
        }
        Vector2f shipRoundPoint = MathUtils.getPoint(ship.getLocation(), radius, newAngle);
        float dodgeTargetToShipRoundPoint = VectorUtils.getAngle(beam.getFrom(), shipRoundPoint);
        if (Math.abs(MathUtils.getShortestRotation(dodgeTargetFacing, dodgeTargetToShip))
                < Math.abs(MathUtils.getShortestRotation(dodgeTargetToShip, dodgeTargetToShipRoundPoint))
        ) {
            if (beam.getDamage().getDamage()>ship.getHitpoints()/10) {
                mayHitBeam.add(beam);
            }
            return true;
        }
        return false;
    }

    /**
     *

     */
    public void usePhase(float amount){
        if (nearestBiggerShip!=null) {
            if (((nearestBiggerShip.ship.getHitpoints()/nearestBiggerShip.ship.getHitpoints()<0.1f&&nearestBiggerShip.minDistance<(ship.getCollisionRadius()+target.getCollisionRadius())))) {
                if (!ship.isPhased())
                {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                    return;
                }
            }
        }
        mayHitProj.clear();
        mayHitBeam.clear();
        //如果周围很多子弹
        int count = 0;
        boolean isProjectileMany = false;
        List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
        for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
            if(mayHit(damagingProjectile,ship.getCollisionRadius())){
                isProjectileMany = true;
                break;
            }
        }
        if (!isProjectileMany) {
            for (BeamAPI b : engine.getBeams()) {
                if(mayHit(b,ship.getCollisionRadius())){
                    isProjectileMany = true;
                    break;
                }
            }
        }
        if(isProjectileMany) {
            if (!ship.isPhased()) {
                if (ship.getFluxLevel()<0.9f) {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                }
            }
            else {
                if (ship.getFluxLevel()>0.9f) {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,null, 0);
                }
            }
        }
        else {
            if (ship.isPhased()&&ship.getFluxLevel()>0.3f) {
                tracker.advance(amount);
                if (tracker.intervalElapsed()) {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                }
            }
        }
    }

    /**
     * 需要检索考虑优化
     *
     */
    public void useShield(float amount){
        mayHitProj.clear();
        mayHitBeam.clear();
        Vector2f mouseTarget = null;
        //护盾角度
        ShieldAPI shield = ship.getShield();
        float shieldArc = shield.getArc();
        float shieldActiveArc = shield.getActiveArc();
        //护盾如果不能转动那就只能船转动了
        ShieldAPI.ShieldType shieldType = shield.getType();

        if (nearestBiggerShip!=null) {
            if ((nearestBiggerShip.ship.getHitpoints()/nearestBiggerShip.ship.getHitpoints()<0.1f&&nearestBiggerShip.minDistance<nearestBiggerShip.ship.getCollisionRadius())) {
                ship.getMouseTarget().set(nearestBiggerShip.ship.getLocation());
                mouseTarget = nearestBiggerShip.ship.getLocation();
                //
                if (ShieldAPI.ShieldType.OMNI.equals(shieldType)) {
                    //指向弧度中间
                    ship.getMouseTarget().set(mouseTarget);
                    //转不过去关盾
                    if (Math.abs(MathUtils.getShortestRotation(shield.getFacing(), VectorUtils.getAngle(ship.getLocation(), mouseTarget))) > 120f) {
                        if (shield.isOn()) {
                            //if (shieldArc / 3 <= shieldActiveArc) {
                            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, mouseTarget, 0);
                            return;
                            //}
                        }
                    }
                    if (shieldActiveArc == 0) {
                        shield.forceFacing(VectorUtils.getAngle(ship.getLocation(), mouseTarget));
                    }
                    if (ship.getShield().isOff() && ship.getFluxLevel() < 0.9f) {
                        ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, ship.getMouseTarget(), 0);
                        return;
                    }
                }
                //如果能覆盖开盾
                else {
                    if (Math.abs(MathUtils.getShortestRotation(shield.getFacing(),VectorUtils.getAngle(ship.getLocation(),mouseTarget)))<shieldArc/2) {
                        if (ship.getShield().isOff() && ship.getFluxLevel() < 0.9f) {
                            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, ship.getMouseTarget(), 0);
                            return;
                        }
                    }
                }
            }
        }
        float shipCollisionRadius = ship.getCollisionRadius();
        //如果周围很多子弹
        int count = 0;
        boolean isProjectileMany = false;
        DamagingProjectileAPI maxProjectile = null;
        maxDamage = 0;
        float mindistance = 9999;
        List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
        List<DamagingProjectileAPI> mayHitProjectiles = new ArrayList<>();
        List<DamagingProjectileAPI> engineHitProjectiles = new ArrayList<>();
        List<Vector2f> allEngineHit = new ArrayList<>();
        List<Vector2f> allMayHit = new ArrayList<>();
        for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
            //飞向玩家的子弹数量
            //飞向玩家最大伤害的子弹 最大威胁
            //飞向玩家的子弹分布夹角 非动能子弹
            //子弹确定会命中
            if (mayHit(damagingProjectile,shipCollisionRadius) && damagingProjectile.getOwner()!=ship.getOwner()&&!damagingProjectile.isFading()&&!damagingProjectile.isExpired()) {
                if (!DamageType.KINETIC.equals(damagingProjectile.getDamageType())||ship.getHitpoints()/ship.getMaxHitpoints()<0.2f) {
                    if (damagingProjectile.getDamage().getDamage()>maxDamage) {
                        maxDamage = damagingProjectile.getDamage().getDamage();
                        maxProjectile = damagingProjectile;
                    }
                    else if (damagingProjectile.getDamage().getDamage()==maxDamage&&MathUtils.getDistance(ship,damagingProjectile)<mindistance) {
                        mindistance = MathUtils.getDistance(ship,damagingProjectile);
                        maxProjectile = damagingProjectile;
                    }
                    mayHitProjectiles.add(damagingProjectile);
                    allMayHit.add(damagingProjectile.getLocation());
                    isProjectileMany = true;
                }
                //屁股子弹优先防护
                float damagingProjectileToShip = VectorUtils.getAngle(damagingProjectile.getLocation(),ship.getLocation());
                if (Math.abs(MathUtils.getShortestRotation(damagingProjectileToShip,ship.getFacing()))<30) {
                    engineHitProjectiles.add(damagingProjectile);
                    allEngineHit.add(damagingProjectile.getLocation());
                    isProjectileMany = true;
                }
            }
        }
        BeamAPI maxBeam = null;
        List<BeamAPI> mayHitBeams = new ArrayList<>();
        List<BeamAPI> engineHitBeams = new ArrayList<>();
        for (BeamAPI b:engine.getBeams()) {
            if(mayHit(b,shipCollisionRadius) && b.getSource().getOwner()!=ship.getOwner()) {
                if (!DamageType.KINETIC.equals(b.getDamage().getType())) {
                    if (b.getDamage().getDamage() > maxDamage) {
                        maxDamage = b.getDamage().getDamage();
                        maxBeam = b;
                    }
                    mayHitBeams.add(b);
                    allMayHit.add(b.getFrom());
                    isProjectileMany = true;
                }
                //屁股子弹优先防护
                float damagingProjectileToShip = VectorUtils.getAngle(b.getFrom(), ship.getLocation());
                if (Math.abs(MathUtils.getShortestRotation(damagingProjectileToShip, ship.getFacing())) < 30) {
                    engineHitBeams.add(b);
                    allEngineHit.add(b.getFrom());
                    isProjectileMany = true;
                }
            }
        }
        //覆盖弧度
        float maxArc = 0;
        Vector2f left = null;
        Vector2f right = null;
        if (allEngineHit.size()==1) {
            if (maxBeam!=null) {
                left = maxBeam.getFrom();
            }
            if (maxProjectile!=null) {
                right = maxProjectile.getLocation();
            }
        }
        for (Vector2f eo:allEngineHit) {
            for (Vector2f ei:allEngineHit) {
                if (!ei.equals(eo)) {
                    float miToShip = VectorUtils.getAngle(ei,ship.getLocation());
                    float moToShip = VectorUtils.getAngle(eo,ship.getLocation());
                    float arc = Math.abs(MathUtils.getShortestRotation(miToShip,moToShip));
                    if (arc>maxArc) {
                        maxArc = arc;
                        left = eo;
                        right = ei;
                    }
                }
            }
        }
        if (left==null&&right==null) {
            if (allMayHit.size()==1) {
                if (maxBeam!=null) {
                    left = maxBeam.getFrom();
                }
                if (maxProjectile!=null) {
                    right = maxProjectile.getLocation();
                }
            }
            for (Vector2f mo:allMayHit) {
                for (Vector2f mi:allMayHit) {
                    if (!mi.equals(mo)) {
                        float miToShip = VectorUtils.getAngle(mi, ship.getLocation());
                        float moToShip = VectorUtils.getAngle(mo, ship.getLocation());
                        float arc = Math.abs(MathUtils.getShortestRotation(miToShip, moToShip));
                        if (arc > maxArc) {
                            maxArc = arc;
                            left = mo;
                            right = mi;
                        }
                    }
                }
            }
        }

        //弧度中心
        if (left!=null&&right!=null) {
            float leftToShipAngle = VectorUtils.getAngle(ship.getLocation(),left);
            float rightToShipAngle = VectorUtils.getAngle(ship.getLocation(),right);
            float da = MathUtils.getShortestRotation(leftToShipAngle,rightToShipAngle);
            mouseTarget = MathUtils.getPoint(ship.getLocation(),shipCollisionRadius*3,leftToShipAngle+da/2);
        }
        else if(left!=null){
            mouseTarget = left;
        }
        else if(right!=null){
            mouseTarget = right;
        }
        if (mouseTarget==null) {
            ShipAPI near = null;
            mindistance = 50f;
            for (ShipAPI s:engine.getShips()) {
                float distance = MathUtils.getDistance(s,ship);
                if (distance<mindistance&&!s.isFighter()) {
                    mindistance = distance;
                    near = s;
                }
            }
            if (near!=null) {
                mouseTarget = near.getLocation();
            }
        }
        if (mouseTarget==null) {
            tracker.advance(amount);
            if (tracker.intervalElapsed()) {
                if (shield.isOn()) {
                    //if (shieldArc / 3 <= shieldActiveArc) {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                    //}
                }
            }
            return;
        }

        //如果护盾可移动 且 护盾无法覆盖所有子弹
        if (ShieldAPI.ShieldType.OMNI.equals(shieldType)) {
            //指向弧度中间
            ship.getMouseTarget().set(mouseTarget);
            if (shieldActiveArc<maxArc) {
                if (Math.abs(MathUtils.getShortestRotation(shield.getFacing(), VectorUtils.getAngle(ship.getLocation(), mouseTarget))) > 120f) {
                    if (shield.isOn()) {
                        //if (shieldArc / 3 <= shieldActiveArc) {
                        ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, mouseTarget, 0);
                        return;
                        //}
                    }
                }
            }
            if (shieldActiveArc==0) {
                shield.forceFacing(VectorUtils.getAngle(ship.getLocation(),mouseTarget));
            }
        }
        //固护盾的话如果left 或者 right 在 护盾范围内则开护盾
        else if(ShieldAPI.ShieldType.FRONT.equals(shieldType)) {
            isProjectileMany = false;
            float shieldFacing = shield.getFacing();
            if (left!=null) {
                float leftAngle = VectorUtils.getAngle(ship.getLocation(), left);
                if (Math.abs(MathUtils.getShortestRotation(leftAngle,shieldFacing))<shieldArc/2) {
                    isProjectileMany = true;
                }
            }
            if (right!=null) {
                float rightAngle = VectorUtils.getAngle(ship.getLocation(), right);
                if (Math.abs(MathUtils.getShortestRotation(rightAngle,shieldFacing))<shieldArc/2) {
                    isProjectileMany = true;
                }
            }
        }

        if(isProjectileMany) {
            if (shield.isOff()&&ship.getFluxLevel()<0.9f)
            {
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,mouseTarget, 0);
            }
            if (shield.isOn()&&ship.getFluxLevel()>0.9f) {
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,mouseTarget, 0);
            }
        }
        else {
            tracker.advance(amount);
            if (tracker.intervalElapsed()) {
                if (shield.isOn()) {
                    //if (shieldArc / 3 <= shieldActiveArc) {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK, null, 0);
                    //}
                }
            }
        }
    }

    /**
     * 保存原来的weaponGroup
     * 点防武器一组
     * 无限导弹一组
     * 打盾一组
     * 打甲一组
     */
    public void setWeaponGroup(){
        /**
         * 4重分组
         */
        if (ship.getWeaponGroupsCopy().size()>=4) {
            List<WeaponGroupAPI> list = ship.getWeaponGroupsCopy();
            List<WeaponAPI> old = new ArrayList<>();
            for (WeaponGroupAPI g : ship.getWeaponGroupsCopy()) {
                for (WeaponAPI w : g.getWeaponsCopy()) {
                    old.add(w);
                }
                if (weaponGroup.get(g) == null&&old.size()!=0) {
                    //这里有put
                    weaponGroup.put(g, old);
                }
            }

            for (WeaponGroupAPI g : ship.getWeaponGroupsCopy()) {
                while (g.getWeaponsCopy().size() != 0) {
                    g.removeWeapon(0);
                }
            }

            for (WeaponAPI w : ship.getAllWeapons()) {
                //pd武器一类
                if (w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD) || w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD_ALSO)) {
                    list.get(3).addWeaponAPI(w);
                }
                //无限导弹
                else if (WeaponAPI.WeaponType.MISSILE.equals(w.getType())&&!w.usesAmmo()) {
                    list.get(3).addWeaponAPI(w);
                }
                //导弹
                else if (WeaponAPI.WeaponType.MISSILE.equals(w.getType())&&w.usesAmmo()) {
                    list.get(2).addWeaponAPI(w);
                }
                //打盾
                else if (DamageType.KINETIC.equals(w.getDamageType())) {
                    list.get(1).addWeaponAPI(w);
                }
                //打甲
                else {
                    list.get(0).addWeaponAPI(w);
                }
            }
        }
    }

    public void resetWeaponGroup(){
        if (ship.getWeaponGroupsCopy().size()>=4) {
            for (WeaponGroupAPI g : ship.getWeaponGroupsCopy()) {
                if (weaponGroup.get(g)!=null) {
                    for (WeaponAPI w:weaponGroup.get(g)) {
                        g.addWeaponAPI(w);
                    }
                }
            }
        }
    }

    /**
     * PD开火
     * 无限导弹开火
     * emp武器 emp伤害>普通伤害
     * 目标开盾的时候
     * 动能武器必须要有已经冷却的才能开火
     *
     * 血量满血的时候没盾动能武器不打
     *
     *
     */
    public void weaponController(){
        maxWeaponRange=0;
        minWeaponRange=9999;
        float maxMissileRange = 0;
        boolean isControl = false;
        if (ship.getWeaponGroupsCopy().size()>=4) {
            List<WeaponGroupAPI> list = ship.getWeaponGroupsCopy();
            if (target != null) {
                float flux = ship.getFluxLevel();
                float targetFlux = target.getFluxLevel();
                //幅能充足 敌方幅能爆炸
                if (flux<0.5f||targetFlux>0.7f||target.getFluxTracker().isOverloadedOrVenting()) {
                    for (WeaponGroupAPI g : ship.getWeaponGroupsCopy()) {
                        for (WeaponAPI w:g.getWeaponsCopy()) {
                            //if (!w.isDisabled()||(w.usesAmmo()&&w.getAmmo()>0)) {
                            if (WeaponAPI.WeaponType.MISSILE.equals(w.getType())) {
                                if (maxMissileRange < w.getRange()) {
                                    maxMissileRange = w.getRange();
                                }
                            }
                            else {
                                if (maxWeaponRange < w.getRange()) {
                                    maxWeaponRange = w.getRange();
                                }
                            }
                            if (minWeaponRange > w.getRange()&&(!(w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD)&&w.getSize().equals(WeaponAPI.WeaponSize.SMALL)))) {
                                minWeaponRange = w.getRange();
                            }
                            //}
                        }
                        g.toggleOn();
                    }
                    isControl = true;
                }
                else {
                    boolean isShieldOn = false;
                    boolean isPhaseCloak = false;
                    boolean isDamper = false;
                    boolean kineticFiring = false;
                    boolean hpIsFull = true;
                    boolean hpIsDanger = false;
                    boolean no0 = false;
                    boolean no1 = false;
                    boolean no2 = false;
                    boolean no3 = false;
                    int countDisabled = 0;
                    if (list.get(3).getWeaponsCopy().size() == 0) {
                        no3 = true;
                    } else for (WeaponAPI w : list.get(3).getWeaponsCopy()) {
                        if (w.isDisabled() || w.usesAmmo() && w.getAmmo() == 0) {
                            countDisabled++;
                        } else {
                            break;
                        }
                    }
                    if (countDisabled == weaponGroup.size()) {
                        no3 = true;
                    }
                    countDisabled = 0;
                    if (list.get(2).getWeaponsCopy().size() == 0) {
                        no2 = true;
                    } else for (WeaponAPI w : list.get(2).getWeaponsCopy()) {
                        if (w.isDisabled() || w.usesAmmo() && w.getAmmo() == 0) {
                            countDisabled++;
                        } else {
                            break;
                        }
                    }
                    if (countDisabled == weaponGroup.size()) {
                        no2 = true;
                    }
                    countDisabled = 0;
                    if (list.get(0).getWeaponsCopy().size() == 0) {
                        no0 = true;
                    } else for (WeaponAPI w : list.get(0).getWeaponsCopy()) {
                        if (w.isDisabled() || w.usesAmmo() && w.getAmmo() == 0) {
                            countDisabled++;
                        } else {
                            break;
                        }
                    }
                    if (countDisabled == weaponGroup.size()) {
                        no0 = true;
                    }
                    countDisabled = 0;
                    if (list.get(1).getWeaponsCopy().size() == 0) {
                        kineticFiring = true;
                        no1 = true;
                    } else {
                        for (WeaponAPI w : list.get(1).getWeaponsCopy()) {
                            if (w.isDisabled() || w.usesAmmo() && w.getAmmo() == 0) {
                                countDisabled++;
                            } else {
                                break;
                            }
                        }
                        if (countDisabled == weaponGroup.size()) {
                            no1 = true;
                        }
                        for (WeaponAPI w : list.get(1).getWeaponsCopy()) {
                            if (w.isFiring()) {
                                kineticFiring = true;
                                break;
                            }
                        }
                    }

                    //有护盾
                    if (target.getShield() != null) {
                        if (target.getShield().isOn()) {
                            isShieldOn = true;
                        }
                    }
                    //有相位
                    if (target.getPhaseCloak() != null) {
                        //相位开启
                        if (target.getPhaseCloak().isActive()) {
                            //相位
                            if ("phasecloak".equals(target.getPhaseCloak().getId())) {
                                isPhaseCloak = true;
                            } else if ("damper".equals(target.getPhaseCloak().getId())) {
                                isDamper = true;
                            }
                        }
                    }

                    if (target.getHitpoints() / target.getMaxHitpoints() > 0.2f) {
                        hpIsDanger = true;
                    }
                    if (target.getHitpoints() / target.getMaxHitpoints() < 0.9f) {
                        hpIsFull = false;
                    }

                    if (isShieldOn || !hpIsFull || (no0 && no2 && no3)) {
                        list.get(1).toggleOn();
                    } else {
                        list.get(1).toggleOff();
                    }
                    if ((isShieldOn && kineticFiring) || !isShieldOn || !isDamper || isPhaseCloak || (no1 && no2 && no3)) {
                        list.get(0).toggleOn();
                    } else {
                        list.get(0).toggleOff();
                    }
                    if (!hpIsDanger || (no0 && no1 && no3) || flux >= 0.5f) {
                        list.get(2).toggleOn();
                    } else {
                        list.get(2).toggleOff();
                    }
                    list.get(3).toggleOn();
                    isControl = true;
                }
            }
        }
        missileWeaponList.clear();
        disabledWeaponList.clear();
        for (WeaponGroupAPI g : ship.getWeaponGroupsCopy()) {
            for (WeaponAPI w:g.getWeaponsCopy()) {
                if (w.getType().equals(WeaponAPI.WeaponType.MISSILE))
                {
                    missileWeaponList.add(w);
                }
                if (w.isDisabled()) {
                    disabledWeaponList.add(w);
                }
                if (!w.isDisabled()||(w.usesAmmo()&&w.getAmmo()>0)) {
                    if (WeaponAPI.WeaponType.MISSILE.equals(w.getType())) {
                        if (maxMissileRange < w.getRange()) {
                            maxMissileRange = w.getRange();
                        }
                    }
                    else {
                        if (maxWeaponRange < w.getRange()) {
                            maxWeaponRange = w.getRange();
                        }
                    }
                    if (minWeaponRange > w.getRange()) {
                        minWeaponRange = w.getRange();
                    }
                }
            }
            if (!isControl) {
                g.toggleOn();
            }
        }
        if (maxWeaponRange==0){
            maxWeaponRange = maxMissileRange;
        }
        if (minWeaponRange==9999) {
            Global.getLogger(this.getClass()).info("怎么可能5");
        }
    }

    public void useSystem(){
        if (RC_AIContants.driveSystemId.contains(ship.getSystem().getId())){
            useDriveSystem();
        }
        else if (RC_AIContants.pdSystemId.contains(ship.getSystem().getId())){
            usePdSystem();
        }
        else if (RC_AIContants.statusSystemId.contains(ship.getSystem().getId())){
            useStatusSystem();
        }
        else if (RC_AIContants.carrierSystemId.contains(ship.getSystem().getId())){
            useCarrirSystem();
        }
        else if (RC_AIContants.specialSystemId.contains(ship.getSystem().getId())){
            useSpecialSystem();
        }

    }
    public void usePdSystem(){
        if (missileList.size()>ship.getHullSize().ordinal()*2) {
            ship.useSystem();
        }
        else if (ship.getFluxLevel()>0.5f&&missileList.size()>0) {
            ship.useSystem();
        }
    }
    public void useStatusSystem(){
        if (target!=null) {
            if (!ship.getSystem().isActive()) {
                //转向
                float distance = MathUtils.getDistance(ship, target);
                float angle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(), target.getLocation()), ship.getFacing()));
                if ("highenergyfocus".equals(ship.getSystem().getId())||
                        "ammofeed".equals(ship.getSystem().getId())
                ) {
                    if (distance < minWeaponRange && angle < 20) {
                        //statusSystemId.add("highenergyfocus");//高能聚焦系统
                        //必须转向后使用
                        //statusSystemId.add("ammofeed");//加速填弹器
                        ship.useSystem();
                    }
                }
                else if ("entropyamplifier".equals(ship.getSystem().getId())
                ) {
                    if (distance < ship.getSystem().getSpecAPI().getRange(ship.getMutableStats()) && target.isAlive() && !target.isFighter()) {
                        //statusSystemId.add("entropyamplifier");//熵放大器
                        if (ship.getMouseTarget()!=null) {
                            ship.getMouseTarget().set(target.getLocation());
                        }
                        ship.useSystem();
                    }
                }
                else if ("acausaldisruptor".equals(ship.getSystem().getId())
                ) {
                    if (distance < ship.getSystem().getSpecAPI().getRange(ship.getMutableStats()) && target.isAlive() && !target.isFighter()) {
                        if (!target.getFluxTracker().isOverloadedOrVenting()) {
                            //statusSystemId.add("acausaldisruptor");//量子干扰
                            if (ship.getMouseTarget()!=null) {
                                ship.getMouseTarget().set(target.getLocation());
                            }
                            ship.useSystem();
                        }
                    }
                }
                else if ("temporalshell".equals(ship.getSystem().getId())
                ) {
                    if (ship.getFluxLevel() < 0.4 && distance < minWeaponRange) {
                        //statusSystemId.add("temporalshell");//时流之壳
                        ship.useSystem();
                    }
                }
                else if ("forgevats".equals(ship.getSystem().getId())
                ) {
                    if (ship.getFluxLevel() < 0.4) {
                        for (WeaponAPI w : missileWeaponList) {
                            if (w.getMaxAmmo() != 0 && w.getAmmo() == 0) {
                                //statusSystemId.add("forgevats");//导弹自动工厂
                                ship.useSystem();
                                break;
                            }
                        }
                    }
                }
                else if ("fastmissileracks".equals(ship.getSystem().getId())
                ) {
                    if (ship.getFluxLevel() < 0.4) {
                        for (WeaponAPI w : missileWeaponList) {
                            if (w.getCooldownRemaining() > 9f) {
                                //statusSystemId.add("fastmissileracks");//高速导弹挂架
                                ship.useSystem();
                                break;
                            }
                        }
                    }
                }
            }
        }
    }
    public void useCarrirSystem() {
        if ("targetingfeed".equals(ship.getSystem().getId())
                || "reservewing".equals(ship.getSystem().getId())
        ) {
            if (target!=null) {
                float distance = MathUtils.getDistance(ship, target);
                if (distance<minWingRange&&!ship.isPullBackFighters()) {
                    //carrierSystemId.add("targetingfeed");//目标馈送系统
                    //carrierSystemId.add("reservewing");//后备部署
                    ship.useSystem();
                }
            }
        }
        else if("recalldevice".equals(ship.getSystem().getId())) {
            boolean needUse = true;
            for (ShipAPI f:myFighterList) {
                for (WeaponAPI w:f.getAllWeapons()) {
                    if (w.getType().equals(WeaponAPI.WeaponType.MISSILE)&&w.getAmmo()>0){
                        needUse = false;
                        break;
                    }
                }
                if (!needUse) {
                    break;
                }
            }
            if (needUse) {
                //carrierSystemId.add("recalldevice");//召回装置
                ship.useSystem();
            }
        }
    }

    public void useSpecialSystem(){
        if ("damper_omega".equals(ship.getSystem().getId()))
        {
            if (disabledWeaponList.size()!=0) {
                //specialSystemId.add("damper_omega");//熵抑制器
                ship.useSystem();
            }
            else {
                if (ship.getEngineController()!=null)
                    if (ship.getEngineController().getShipEngines()!=null) {
                        for (ShipEngineControllerAPI.ShipEngineAPI e:ship.getEngineController().getShipEngines()) {
                            if (e.isDisabled()) {
                                ship.useSystem();
                                break;
                            }
                        }
                    }
            }
        }
        else if ("mine_strike".equals(ship.getSystem().getId())) {
            // specialSystemId.add("mine_strike");//空雷突袭
            //屁股下法
            //围棋下发
            //速度方向下发
            //护盾方向下发
        }
        else if ("drone_strike".equals(ship.getSystem().getId())) {
            //specialSystemId.add("drone_strike");//终结指令
            if (target!=null) {
                if (target.isAlive()&&(target.getFluxLevel()>0.9f||target.getFluxTracker().isOverloadedOrVenting())){
                    float distance = MathUtils.getDistance(ship, target);
                    if (distance<minWingRange) {
                        ship.useSystem();
                    }
                }
            }
        }
        else if ("drone_strike".equals(ship.getSystem().getId())) {
            //specialSystemId.add("drone_strike");//终结指令
            if (target!=null) {
                if (target.isAlive()&&(target.getFluxLevel()>0.9f||target.getFluxTracker().isOverloadedOrVenting())){
                    float distance = MathUtils.getDistance(ship, target);
                    if (distance<minWingRange) {
                        ship.useSystem();
                    }
                }
            }
        }
        else if ("damper".equals(ship.getSystem().getId())) {
            //specialSystemId.add("damper");//阻尼力场
            //mayHit很多则使用 要看伤害
            if (mayHitBeam.size()+mayHitProj.size()>0) {
                ship.useSystem();
            }
        }
        else if ("phaseteleporter".equals(ship.getSystem().getId())) {
            //specialSystemId.add("phaseteleporter");//相位传送
            if (target!=null) {
                float distance = MathUtils.getDistance(ship, target);
                Vector2f targetBack = MathUtils.getPoint(target.getLocation(),target.getCollisionRadius()+ship.getCollisionRadius()*2, target.getFacing()+180);
                float backDistance = MathUtils.getDistance(ship, targetBack);
                if (backDistance<ship.getSystem().getSpecAPI().getRange(ship.getMutableStats())
                        &&distance>backDistance
                        &&ship.getFluxLevel()<0.5f
                ) {
                    if (ship.getMouseTarget()!=null) {
                        ship.setShipTarget(target);
                        ship.getMouseTarget().set(target.getLocation());
                        ship.useSystem();
                    }
                }
            }
        }
        else if ("displacer".equals(ship.getSystem().getId())) {
            //直接使用
            //specialSystemId.add("displacer");//闪现
            //使用掉头
            if (target!=null) {
                if (Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), target.getLocation()))) > 120f) {
                    ship.getMouseTarget().set(target.getLocation());
                    //shipSystem.getTargetLoc().set(targetLocation);
                    ship.useSystem();
                }
            }
        }
        else if ("mote_control".equals(ship.getSystem().getId())) {
            //导弹多且自己福能多则不使用
            //specialSystemId.add("mote_control");//光尘吸引场
        }
        else if ("fortressshield".equals(ship.getSystem().getId())) {
            //specialSystemId.add("fortressshield");//堡垒护盾
            //如果武器损坏过多使用
            //如果最大伤害>2000使用
            //否则关闭
            if (ship.getShield()!=null) {
                if (ship.getShield().isOn()) {
                    if (!ship.getSystem().isActive()) {
                        if (maxDamage>2000) {
                            ship.useSystem();
                        }
                    }
                    else {
                        if (maxDamage<=2000) {
                            ship.useSystem();
                        }
                    }
                }
            }
        }
    }

    // * 判断技能 闪现displacer相位传送phaseteleporter烈焰驱动器burndrive等离子爆裂驱动器microburn等离子爆裂驱动器 - 欧米伽microburn_omega等离子爆裂驱动器 - 欧米伽maneuveringjets
    // * 烈焰喷射inferniuminjector等离子推进器plasmajets
    // 如果是在屁股后面可以尽可能接近
    public void useDriveSystem(){
        if (target==null) {
            return;
        }
        ShipSystemAPI shipSystem = ship.getSystem();
        float distance = MathUtils.getDistance(ship.getLocation(),target.getLocation());
        if (other!=null) {
            //大于自己才关闭
            if (other.ship.getOwner()==ship.getOwner()&&other.minDistance<20*ship.getHullSize().ordinal()) {
                //如果other是自己人已经过载或者引擎宕机
                boolean stop = true;

                if (other.ship.getEngineController()!=null) {
                    int ecount = 0;
                    for (ShipEngineControllerAPI.ShipEngineAPI e : other.ship.getEngineController().getShipEngines()) {
                        if (e.isDisabled()) {
                            ecount++;
                        }
                    }
                    if (ecount==other.ship.getEngineController().getShipEngines().size()) {
                        stop = false;
                    }
                }
                if (stop) {
                    if (shipSystem.isActive()) {
                        ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
                    }
                    return;
                }
            }
            else if (other.ship.getOwner()!=ship.getOwner()&&other.minDistance<minWeaponRange) {
                if (shipSystem.isActive()) {
                    ship.giveCommand(ShipCommand.USE_SYSTEM,null,0);
                }
                return;
            }
        }
        //这里要注意 distance 是和target的距离 小船 需要更多距离 大船需要更少距离
        //如果是在屁股后面可以尽可能接近
        if (Math.abs(MathUtils.getShortestRotation(target.getFacing(),VectorUtils.getAngle(target.getLocation(),ship.getLocation())))<90) {
            if (ship.getHullSize().compareTo(target.getHullSize()) >= 0) {
                if (distance < ship.getCollisionRadius() + target.getCollisionRadius() * 2) {
                    if (shipSystem.isActive()) {
                        ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
                    }
                    return;
                }
            } else {
                if (distance < maxWeaponRange) {
                    if (shipSystem.isActive()) {
                        ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
                    }
                    return;
                }
            }
        }

        if (ship.getHitpoints()/ship.getMaxHitpoints()<0.2f) {
            if (shipSystem.isActive()) {
                ship.giveCommand(ShipCommand.USE_SYSTEM,null,0);
            }
            return;
        }
        if (disabledWeaponList.size()/ship.getAllWeapons().size()>0.5) {
            if (shipSystem.isActive()) {
                ship.giveCommand(ShipCommand.USE_SYSTEM,null,0);
            }
            return;
        }
        if (ship.getFluxLevel()>0.9f) {
            if (shipSystem.isActive()) {
                ship.giveCommand(ShipCommand.USE_SYSTEM,null,0);
            }
            return;
        }
        //防止靠近太近比自己大的船
        if (nearestBiggerShip != null) {
            if (Math.abs(MathUtils.getShortestRotation(nearestBiggerShip.ship.getFacing(),VectorUtils.getAngle(nearestBiggerShip.ship.getLocation(),ship.getLocation())))<90) {
                if (nearestBiggerShip.minDistance < minWeaponRange && Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), nearestBiggerShip.ship.getLocation()))) < 90) {
                    if (shipSystem.isActive()) {
                        ship.giveCommand(ShipCommand.USE_SYSTEM, null, 0);
                    }
                    return;
                }
            }
        }
        if (shipSystem.isActive()) {
            return;
        }
        if (faceLocation!=null) {
            if (Math.abs(MathUtils.getShortestRotation(ship.getFacing(), VectorUtils.getAngle(ship.getLocation(), faceLocation))) < 10f) {
                ship.useSystem();
                return;
            }
        }
        else if (Math.abs(MathUtils.getShortestRotation(ship.getFacing(),VectorUtils.getAngle(ship.getLocation(),target.getLocation()))) < 10f) {
            ship.useSystem();
        }
    }

    public void vent() {
        if (target!=null) {
            if (!target.isAlive()) {
                if (ship.getCurrFlux()/ship.getMutableStats().getFluxDissipation().getModifiedValue()<3) {
                    if (!ship.getFluxTracker().isVenting()) {
                        ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                    }
                }
                //待测试
                if (target.getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP)) {
                    if (!ship.getFluxTracker().isVenting()&&nearestBiggerAlly==null) {
                        ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                    }
                }
            }
        }
        //幅能超过30%
        if (ship.getFluxLevel()<0.3f) {
            return;
        }
        //比自己等级大的船或同等级的船 两船之间是否 存在 己方 比自己等级大的船或同等级的船
        //先获取自己范围内敌方大船 连线 线段是否与范围内 己方大船相交 参考获取遮挡物
        //只要有一个没遮挡
        int count = 0;
        List<ShipAPI> moduleCopy = ship.getChildModulesCopy();
        for (ShipAPI e:enemyList) {
            float shipToEAngle = VectorUtils.getAngle(ship.getLocation(), e.getLocation());
            float eDistance = MathUtils.getDistance(ship,e);
            for (ShipAPI f:coverList) {
                if (moduleCopy.indexOf(f)<0) {
                    continue;
                }
                float fDistance = MathUtils.getDistance(ship,f);
                if ((f.getOwner()==ship.getOwner()||f.getOwner()==100)&&(f.getHullSize().compareTo(ship.getHullSize())>=0||f.getHullSize()==ship.getHullSize().smaller(false))&&eDistance>=fDistance) {
                    float radius = f.getCollisionRadius();
                    float shipToFAngle = VectorUtils.getAngle(ship.getLocation(),f.getLocation());
                    Vector2f fRoundPoint = MathUtils.getPoint(f.getLocation(), radius * 2.5f, shipToFAngle + 90);
                    float shipToFRoundPoint = VectorUtils.getAngle(ship.getLocation(), fRoundPoint);
                    if (Math.abs(MathUtils.getShortestRotation(shipToEAngle, shipToFAngle))
                            < Math.abs(MathUtils.getShortestRotation(shipToFRoundPoint, shipToFAngle))
                    ) {
                        count++;
                    }
                }
            }
        }
        if (count>enemyList.size()) {
            if (!ship.getFluxTracker().isVenting()) {
                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }
        }
    }

    public void pullBackFighters() {
        List<FighterWingAPI> wings = ship.getAllWings();
        if (wings.size()==0) {
            return;
        }
        if (!ship.areAnyEnemiesInRange()) {
            if (!ship.isPullBackFighters()) {
                ship.giveCommand(ShipCommand.PULL_BACK_FIGHTERS,null,0);
            }
            return;
        }
        int nowCount = 0;
        int allCount = 0;
        for (FighterWingAPI w:wings) {
            allCount+=w.getSpec().getNumFighters();
            nowCount+=w.getWingMembers().size();
        }
        if (nowCount<allCount/2) {
            if (!ship.isPullBackFighters()) {
                ship.giveCommand(ShipCommand.PULL_BACK_FIGHTERS,null,0);
            }
        }
        else {
            if (ship.isPullBackFighters()) {
                ship.giveCommand(ShipCommand.PULL_BACK_FIGHTERS,null,0);
            }
        }
    }

    /**
     * 永远面对最近且舰船等级大于等于自己的敌人
     */
    public void faceToEnmey() {

    }

    @Override
    public void setDoNotFireDelay(float amount) {
        this.dontFireUntil = amount + Global.getCombatEngine().getTotalElapsedTime(false);
    }

    @Override
    public void forceCircumstanceEvaluation() {

    }

    @Override
    public boolean needsRefit() {
        return false;
    }

    @Override
    public ShipwideAIFlags getAIFlags() {
        return this.AIFlags;
    }

    @Override
    public void cancelCurrentManeuver() {

    }

    @Override
    public ShipAIConfig getConfig() {
        return null;
    }

    public static class ShipAndDistance{
        public ShipAPI ship;
        public float minDistance;
        public ShipAndDistance(ShipAPI ship,float minDistance)
        {
            this.ship = ship;
            this.minDistance = minDistance;
        }
    }

    public class CombatEntityAndDistance{
        public CombatEntityAPI combatEntity;
        public float minDistance;
        public CombatEntityAndDistance(CombatEntityAPI combatEntity,float minDistance)
        {
            this.combatEntity = combatEntity;
            this.minDistance = minDistance;
        }
    }

    public ShipAndDistance findNearestShip(ShipAndDistance shipAndDistance,ShipAPI s) {
        float distance = MathUtils.getDistance(s, ship);
        if (shipAndDistance.minDistance>distance)
        {
            shipAndDistance.ship = s;
            shipAndDistance.minDistance = distance;
        }
        return shipAndDistance;
    }

    public ShipAndDistance findNearestAlly(ShipAndDistance shipAndDistance,ShipAPI s) {
        float distance = MathUtils.getDistance(s, target);
        if (shipAndDistance.minDistance>distance)
        {
            shipAndDistance.ship = s;
            shipAndDistance.minDistance = distance;
        }
        return shipAndDistance;
    }

    public boolean isOtherTrue(ShipAPI target, ShipAPI other) {
        float radius = other.getCollisionRadius() + ship.getCollisionRadius();
        if (other.getShield()!=null) {
            if (other.getShield().isOn()) {
                radius = other.getShield().getRadius() + ship.getCollisionRadius();
            }
        }
        float distance = MathUtils.getDistance(other, ship);
        //如果在碰撞范围内
        if (distance < radius) {
            float shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), other.getLocation());
            float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
            Vector2f otherRoundPoint = MathUtils.getPoint(other.getLocation(), radius, shipToOtherAngle + 90);
            float shipToOtherRoundPoint = VectorUtils.getAngle(ship.getLocation(), otherRoundPoint);
            if (Math.abs(MathUtils.getShortestRotation(shipToTargetAngle, shipToOtherAngle))
                    < Math.abs(MathUtils.getShortestRotation(shipToOtherRoundPoint, shipToOtherAngle))
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * TODO 如果差速很大则向目标屁股移动
     *
     * 距离 大于 最小攻击距离的时候 向目标直飞
     * 如果途中有other 绕行
     *
     * 没到屁股 绕行
     * 到屁股 减速保持屁股
     * 太近后退 如果小于对方舰船半径后退
     * 否则都是平移
     *
     *
     *
     * @param target
     * @param other
     * @param amount
     */
    public void flyToTarget(ShipAPI target, ShipAPI other, float amount) {
        float shipFacing = MathUtils.clampAngle(ship.getFacing());
        //如果距离很远直接
        Vector2f targetLocation = target.getLocation();
        float distance = MathUtils.getDistance(target,ship);
        float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetLocation);
        if (distance>minWeaponRange) {
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
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), (other.getCollisionRadius()* 2f+ship.getCollisionRadius()) , shipToOtherAngle);
                        shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        RC_BaseAIAction.move(ship, shipFacing, shipToOtherAngle);
                    } else {
                        shipToOtherAngle = shipToOtherAngle + 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), (other.getCollisionRadius()* 2f+ship.getCollisionRadius()) , shipToOtherAngle);
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
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), (other.getCollisionRadius()* 2f+ship.getCollisionRadius()) , shipToOtherAngle);
                        shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        RC_BaseAIAction.move(ship, shipFacing, shipToOtherAngle);
                    } else {
                        shipToOtherAngle = shipToOtherAngle + 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(other.getLocation(), (other.getCollisionRadius()* 2f+ship.getCollisionRadius()) , shipToOtherAngle);
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
                if (distance < target.getCollisionRadius()) {//*2 + ship.getCollisionRadius()
                    ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS,null,0);
                }
                else {
                    ship.giveCommand(ShipCommand.ACCELERATE,null,0);
                }
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
                    if (distance < target.getCollisionRadius()*2 + ship.getCollisionRadius()*2) {
                        toTargetAngle = toTargetAngle + 180;
                    }
                    RC_BaseAIAction.move(ship, shipFacing, toTargetAngle);
                }
            }
        }
    }

    /**
     * 如果敌人在视野内 则向敌人移动 否则 ？
     * 向最大的队友移动？
     * 向地图出生点移动？
     *
     * 当船是leader时才查询
     */
    public void findBestTarget() {
        if (minWeaponRange==9999) {
            Global.getLogger(this.getClass()).info("怎么可能0");
        }
        minWingRange = 9999F;
        enemyFleetPoint = 0;
        myFleetPoint = 0;
        //绕开使用
        nearestEnemy = null;
        nearestEnemyNotFighter = null;
        nearestShip = null;
        //最近的船 开盾使用
        nearestBiggerShip = null;
        //名字没改暂时使用
        nearestBiggerEnemy = null;
        nearestBiggerAlly = null;
        backBiggerAlly = null;
        //最近的超载目标
        ShipAndDistance minOverLoad = null;
        enemyList.clear();
        fighterList.clear();
        coverList.clear();
        myFighterList.clear();
        biggerList.clear();
        biggerEnemyInRangeList.clear();
        beTargetCount = 0;
        for (ShipAPI s :engine.getShips()) {
            if (s!=ship) {
                if (!s.isFighter()) {
                    if (ship.equals(s.getShipTarget())) {
                        beTargetCount++;
                    }
                    if(s.getHullSize().compareTo(ship.getHullSize())>=0||s.getOwner()==ship.getOwner()) {
                        biggerList.add(s);
                        if (s.getOwner()!=ship.getOwner()&&s.getOwner()!=100&&MathUtils.getDistance(s,ship)<minWeaponRange&&s.isAlive()) {
                            biggerEnemyInRangeList.add(s);
                        }
                    }
                    else if(s.getHullSize().compareTo(ship.getHullSize())==0){
                        if (s.getOwner()!=ship.getOwner()&&s.getOwner()!=100&&MathUtils.getDistance(s,ship)<minWeaponRange&&s.isAlive()) {
                            biggerEnemyInRangeList.add(s);
                        }
                    }
                    if (nearestShip == null) {
                        nearestShip = new ShipAndDistance(s, MathUtils.getDistance(s, ship));
                    } else {
                        nearestShip = findNearestShip(nearestShip, s);
                    }
                    if ((s.getOwner()==ship.getOwner()||s.getOwner()==100)&&(s.getHullSize().compareTo(ship.getHullSize())>=0||s.getHullSize()==ship.getHullSize().smaller(false))) {
                        coverList.add(s);
                    }
                    if (s.getOwner()==ship.getOwner()&&s.isAlive()&&!s.isFighter()) {
                        myFleetPoint += s.getHullSpec().getFleetPoints();
                    }
                }
                else {
                    if (!s.isHulk() && s.isAlive() && s.getWing()!=null) {
                        if (s.getWing().getSourceShip()!=null) {
                            if (s.getWing().getSourceShip().equals(ship)) {
                                myFighterList.add(s);
                                if (s.getWing().getRange()<minWingRange) {
                                    minWingRange = s.getWing().getRange();
                                }
                            }
                        }
                    }
                }
                if (!s.isHulk() && s.isAlive() && s.getOwner()!=100&&s.getOwner()!=ship.getOwner()) {
                    if (!s.isFighter()) {
                        enemyFleetPoint += s.getHullSpec().getFleetPoints();

                        if (nearestBiggerShip == null) {
                            nearestBiggerShip = new ShipAndDistance(s, MathUtils.getDistance(s, ship));
                        } else {
                            nearestBiggerShip = findNearestShip(nearestBiggerShip, s);
                        }

                        if (nearestBiggerEnemy == null) {
                            nearestBiggerEnemy = new ShipAndDistance(s, MathUtils.getDistance(s, ship));
                        } else if (s.getHullSize().compareTo(ship.getHullSize())>=0){
                            nearestBiggerEnemy = new ShipAndDistance(s, MathUtils.getDistance(s, ship));
                        }
                        if (nearestEnemyNotFighter == null) {
                            nearestEnemyNotFighter = new ShipAndDistance(s, MathUtils.getDistance(s, ship));
                        } else {
                            nearestEnemyNotFighter = findNearestShip(nearestEnemyNotFighter, s);
                        }
                    }
                    else {
                        fighterList.add(s);
                    }
                    if (nearestEnemy == null) {
                        nearestEnemy = new ShipAndDistance(s, MathUtils.getDistance(s, ship));
                    } else {
                        nearestEnemy = findNearestShip(nearestEnemy, s);
                    }
                }
                else {
                    if (!s.isHulk() && s.isAlive() && s.getHullSize().compareTo(ship.getHullSize())>0) {
                        if (nearestBiggerAlly == null) {
                            nearestBiggerAlly = new ShipAndDistance(s, MathUtils.getDistance(s, ship));
                        } else {
                            nearestBiggerAlly = findNearestShip(nearestBiggerAlly, s);
                        }
                    }
                    if (!s.isHulk() && s.isAlive() && s.getHullSize().compareTo(ship.getHullSize())>=0) {
                        //屁股后面有船 并且 面对自己屁股
                        float sToShip = VectorUtils.getAngle(s.getLocation(), ship.getLocation());
                        if (Math.abs(MathUtils.getShortestRotation(sToShip, s.getFacing())) < 45
                                &&Math.abs(MathUtils.getShortestRotation(s.getFacing(), ship.getFacing())) < 30
                            //&&MathUtils.getDistance(ship,s)<s.getCollisionRadius()+ship.getCollisionRadius()+ship.getMaxSpeed()
                        ) {
                            if (backBiggerAlly == null) {
                                backBiggerAlly = new ShipAndDistance(s, MathUtils.getDistance(s, ship));
                            } else {
                                backBiggerAlly = findNearestShip(backBiggerAlly, s);
                            }
                        }
                    }
                }

                float distance = MathUtils.getDistance(s,ship);
                if (!s.isHulk() && s.isAlive() && distance<minWeaponRange&&!s.isFighter()&&s.getOwner()!=100&&s.getOwner()!=ship.getOwner()&&!s.isFighter()) {
                    if (minOverLoad == null) {
                        if (getWeight(s)>0) {
                            minOverLoad = new ShipAndDistance(s, getWeight(s));
                        }
                    }
                    else if(minOverLoad.minDistance<getWeight(s)){
                        minOverLoad = new ShipAndDistance(s, getWeight(s));
                    }

                }

                if (!s.isHulk() && s.isAlive() && distance<maxWeaponRange&&!s.isFighter()&&s.getOwner()!=100&&s.getOwner()!=ship.getOwner()&&!s.isFighter()) {
                    if (s.getHullSize().compareTo(ship.getHullSize())>=0&&!s.getFluxTracker().isOverloadedOrVenting()&&Math.abs(MathUtils.getShortestRotation(s.getFacing(),VectorUtils.getAngle(s.getLocation(),ship.getLocation())))<90) {
                        enemyList.add(s);
                    }
                }
            }
        }

        if (isLeader) {
            if (target != null) {
                int count = 0;
                if (target.getCustomData().get("findBestTarget") != null) {
                    count = (int) target.getCustomData().get("findBestTarget");
                    count--;
                }
                target.setCustomData("findBestTarget", count);
            }
        }

        oldTarget = target;

        target = null;

        //跟随队长
        if (!isLeader) {
            if (ship.getCustomData().get("RC_BaseShipAI_lederTarget")!=null) {
                target = (ShipAPI) ship.getCustomData().get("RC_BaseShipAI_lederTarget");
            }
        }

        if (target!=null) {
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }else if (getWeight(target)<=0){
                target = null;
            }else if (MathUtils.getDistance(target,ship)>minWeaponRange) {
                target = null;
            }
        }

        if (target==null) {
            if (minOverLoad!=null) {
                target = minOverLoad.ship;
                if (target.isHulk()||!target.isAlive()) {
                    target = null;
                }
                else if (getWeight(target)<=0){
                    target = null;
                }
            }

            if (nearestBiggerEnemy!=null&&target==null) {
                target = nearestBiggerEnemy.ship;
                if (target.isHulk()||!target.isAlive()) {
                    target = null;
                }else if (getWeight(target)<=0){
                    target = null;
                }
            }

            if (nearestEnemy!=null&&target==null){
                target = nearestEnemy.ship;
            }
        }
        other = null;
        //找到target开始找other
        if (target!=null) {
            //
            if (isLeader) {
                int count = 0;
                if (target.getCustomData().get("findBestTarget") != null) {
                    count = (int) target.getCustomData().get("findBestTarget");
                    count++;
                }
                target.setCustomData("findBestTarget", count);
            }

            Set<ShipAPI> others = new HashSet<>();
            others.addAll(biggerList);
            others.remove(target);
            Set<ShipAPI> removes = new HashSet<>();
            for (ShipAPI o:others) {
                if(!isOtherTrue(target,o)) {
                    removes.add(o);
                }
            }
            others.removeAll(removes);
            ShipAndDistance nearstOther = null;
            for (ShipAPI o:others) {
                if (nearstOther==null) {
                    nearstOther = new ShipAndDistance(o,MathUtils.getDistance(o,ship));
                }
                else {
                    nearstOther= findNearestShip(nearstOther,o);
                }
            }

            if (nearstOther!=null) {
                other = nearstOther;
            }
        }
        ship.setShipTarget(target);
    }

    /**
     * 距离优先
     * 距离内幅能优先 过载优先
     * 角度优先
     *
     *
     * 同等级船优先
     * 目标数量少优先
     * @param target
     * @return
     */

    public float getWeight(ShipAPI target) {
        float weight = 1f;

        float distance = MathUtils.getDistance(ship,target);
        if (distance>maxWeaponRange) {
            //不要追距离外少血的小船
            weight /= distance * distance;
        }
        else {
            weight *= maxWeaponRange - distance;
        }

        //是否过载或者散福能 + 福能
        if (target.getCurrFlux()!=0) {
            weight+=target.getCurrFlux();
            if (target.getFluxTracker().isOverloadedOrVenting()) {
                weight *= target.getHullSize().ordinal();
            }
            if (target.getFluxLevel()>0.7f) {
                //舰船强度 处以
                weight += target.getHullSpec().getFleetPoints();
            }
        }

        //舰船屁股插值 处以
        float angle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(),target.getLocation()),target.getFacing()));
        if (angle>=120) {
            weight /= angle*ship.getHullSize().ordinal();
        }
        else if (angle>=30) {

        }
        else {
            weight*=30-angle;
        }

        if (target.getHullSize().compareTo(ship.getHullSize())>0) {
            if (target.getFluxLevel()>0.7f) {
                weight *= 1 + target.getFluxLevel();
            }

            //福能+
            //血量剩余 比例 处以
            if (target.getHitpoints()!=0) {
                //weight = weight * target.getMaxHitpoints() / target.getHitpoints();
            }
            else {
                return 0;
            }
        }

        if (target.getCustomData().get("findBestTarget") != null) {
            int count = (int) target.getCustomData().get("findBestTarget");
            if (count>1) {
                //weight/=(teamList.size()+1) * ship.getHullSpec().getFleetPoints() * myFleetPoint/enemyFleetPoint;//count*2
                weight=1;
            }
            else {
                weight *= target.getHullSize().ordinal();
            }
        }
        if (Math.abs(target.getHullSpec().getFleetPoints()-ship.getHullSpec().getFleetPoints())!=0) {
            weight /= Math.abs(target.getHullSpec().getFleetPoints()-ship.getHullSpec().getFleetPoints());
        }
        else {
            weight *= 999;
        }
        if (ship.getHullSize()!=target.getHullSize()) {
            weight /= Math.abs(ship.getHullSize().ordinal()-target.getHullSize().ordinal());
        }
        else {
            weight *= 99;
        }
        return weight;
    }

    public void useSystemRun() {

    }

    public void releaseTeam() {
        if (isLeader) {
            if (oldTarget!=null&&oldTarget!=target) {
                allyList.addAll(teamList);
                for (ShipAPI t:teamList) {
                    t.getCustomData().remove("RC_BaseShipAI_lederTarget");
                }
                teamList.clear();
                refreshHullSize();
                //这里有put
                engine.getCustomData().put("RC_SmartAIEveryFrameCombatPluginenemyList", allyList);
            }
        }
    }

    /**
     * 是否具备leader资格 leader资格必须是最大级别的船 选一个leader 之后 将 leader 移除list
     * 开始选择 target
     * 开始选择 队友 并移除list 队友足够
     * 选择leader 选择队友
     *
     * 如果更换target要重新选择队友
     *
     * 最终将整个list 遍历完
     * 怎样让所有AI公用一个list
     */
    public void findAlly() {
        if (isLeader&&target!=null) {
            float fleetPoint = ship.getHullSpec().getFleetPoints();
            float targetPoint = target.getHullSpec().getFleetPoints();
            for (ShipAPI t : teamList) {
                fleetPoint += t.getHullSpec().getFleetPoints();
            }
            //Global.getLogger(this.getClass()).info(teamList.size());
            int mult = target.getHullSize().ordinal() ;
            if (mult > 3) {
                //mult = 3;
            }
            if (fleetPoint >= targetPoint * mult && teamList.size() >= 2) {
                return;
            }
            ShipAndDistance bestAlly = null;
            ShipAPI.HullSize hullsize = ship.getHullSize();
            while (bestAlly == null) {
                //这里应该找合适的组队
                for (ShipAPI a : allyList) {
                    if (a.getHullSize() == hullsize) {
                        if (bestAlly == null) {
                            bestAlly = new ShipAndDistance(a, MathUtils.getDistance(target, a));
                            if (bestAlly.minDistance > maxWeaponRange) {
                                bestAlly = null;
                            }
                        } else {
                            if (findNearestAlly(bestAlly, a).minDistance < maxWeaponRange) {
                                bestAlly = findNearestAlly(bestAlly, a);
                            }
                        }
                    }
                }
                hullsize = hullsize.smaller(true);
                if (hullsize == ShipAPI.HullSize.FIGHTER) {
                    break;
                }
            }
            if (bestAlly != null) {
                teamList.add(bestAlly.ship);
                allyList.remove(bestAlly.ship);
                refreshHullSize();
                //这里有put
                engine.getCustomData().put("RC_SmartAIEveryFrameCombatPluginenemyList", allyList);
                bestAlly.ship.getCustomData().put("RC_BaseShipAI_lederTarget", target);
                bestAlly.ship.setCustomData("leader",ship);
            }
        }
    }

    //当我是池子里最大的船时我有机会成为leader
    //如果成为leader则移除队友池并重新设置最大船?
    public void setLeader() {
        if (!isLeader&&allyList.contains(ship)&&ship.getHullSpec().getFleetPoints()>=maxFleetPoint&&allyList.size()>2) {
            isLeader = true;
            ship.removeCustomData("leader");
            allyList.remove(ship);
            refreshHullSize();
            //这里有put
            engine.getCustomData().put("RC_SmartAIEveryFrameCombatPluginenemyList", allyList);
        }
        else if (isLeader&&ship.getHullSpec().getFleetPoints()<maxFleetPoint) {
            isLeader = false;
            allyList.add(ship);
            refreshHullSize();
            //这里有put
            engine.getCustomData().put("RC_SmartAIEveryFrameCombatPluginenemyList", allyList);
        }
        else if (isLeader&&teamList.size()>0){
            int dead = 0;
            List<ShipAPI> remove = new ArrayList<>();
            for (ShipAPI t:teamList) {
                if (!t.isAlive()) {
                    dead++;
                }
            }
            if (dead==teamList.size()) {
                isLeader = false;
                allyList.add(ship);
                refreshHullSize();
                //这里有put
                engine.getCustomData().put("RC_SmartAIEveryFrameCombatPluginenemyList", allyList);
            }
            teamList.remove(remove);
        }
        else if (isLeader&&teamList.size()==0){
            isLeader = false;
            allyList.add(ship);
            refreshHullSize();
            //这里有put
            engine.getCustomData().put("RC_SmartAIEveryFrameCombatPluginenemyList", allyList);
        }
    }

    public void refreshHullSize() {
        float old = maxFleetPoint;
        maxFleetPoint = 0;
        for (ShipAPI a:allyList) {
            if (a.getHullSpec().getFleetPoints()>maxFleetPoint) {
                maxFleetPoint = a.getHullSpec().getFleetPoints();
            }
        }
        //这里有put
        if(old!=maxFleetPoint) {
            engine.getCustomData().put("RC_SmartAIEveryFrameCombatPluginmaxHullSzie", maxFleetPoint);
        }
    }

    public void drawLine() {
        Map<ShipAPI, RC_NeedDrawLine> allDrawShip = (Map<ShipAPI, RC_NeedDrawLine>) engine.getCustomData().get("RC_RepairCombatLayeredRenderingPlugin");
        if(allDrawShip==null)
        {
            allDrawShip = new HashMap<>();
        }
        if (target!=null) {
            if (target.isAlive()) {
                RC_NeedDrawLine thisDrawShip = allDrawShip.get(target);
                if (thisDrawShip == null) {
                    thisDrawShip = new RC_NeedDrawLine(target, 0, new ArrayList<Vector2f>(), new ArrayList<Vector2f>(), Color.PINK);
                }
                thisDrawShip.startList.add(ship.getLocation());
                thisDrawShip.endList.add(target.getLocation());
                thisDrawShip.color = Color.PINK;
                //这里有put
                allDrawShip.put(target, thisDrawShip);
            }
        }
        RC_NeedDrawLine teamDrawShip = allDrawShip.get(ship);
        if(teamDrawShip==null)
        {
            teamDrawShip = new RC_NeedDrawLine(target,0,new ArrayList<Vector2f>(),new ArrayList<Vector2f>(), Color.cyan);
        }
        for (ShipAPI t:teamList) {
            if (t.isAlive()) {
                teamDrawShip.startList.add(t.getLocation());
                teamDrawShip.endList.add(ship.getLocation());
            }
        }
        teamDrawShip.color = Color.cyan;
        //这里有put
        allDrawShip.put(ship,teamDrawShip);
        engine.getCustomData().put("RC_RepairCombatLayeredRenderingPlugin", allDrawShip);
    }

    /**
     * 避让爆炸船只
     *
     * 避让友军
     *
     * 避免进入包围圈
     *
     * 躲避子弹
     */

    public void beforeFlyToTarget () {
        //让开 如果比目标大 应该是移动到 目标侧边 反之移动到队友侧边
        if (backBiggerAlly != null) {
            if (target!=null) {
                //if (backBiggerAlly.ship.getShipTarget()==target) {
                if (ship.getHullSize().compareTo(target.getHullSize()) >= 0) {
                    //移动到目标侧边
                    float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
                    float backBiggerAllyToTargetAngle = VectorUtils.getAngle(backBiggerAlly.ship.getLocation(), target.getLocation());

                    if (MathUtils.getShortestRotation(backBiggerAllyToTargetAngle, shipToTargetAngle) < 0) {
                        backBiggerAllyToTargetAngle = backBiggerAllyToTargetAngle - 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(target.getLocation(), (target.getCollisionRadius() * 2f + ship.getCollisionRadius() * 2f), backBiggerAllyToTargetAngle);
                        backBiggerAllyToTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        RC_BaseAIAction.move(ship, ship.getFacing(), backBiggerAllyToTargetAngle);
                    } else {
                        backBiggerAllyToTargetAngle = backBiggerAllyToTargetAngle + 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(target.getLocation(), (target.getCollisionRadius() * 2f + ship.getCollisionRadius() * 2f), backBiggerAllyToTargetAngle);
                        backBiggerAllyToTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        RC_BaseAIAction.move(ship, ship.getFacing(), backBiggerAllyToTargetAngle);
                    }
                    isDodge = true;
                } else {
                    //移动到队友侧边
                    float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
                    float backBiggerAllyToTargetAngle = VectorUtils.getAngle(backBiggerAlly.ship.getLocation(), target.getLocation());

                    if (MathUtils.getShortestRotation(backBiggerAllyToTargetAngle, shipToTargetAngle) < 0) {
                        backBiggerAllyToTargetAngle = backBiggerAllyToTargetAngle - 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(backBiggerAlly.ship.getLocation(), (backBiggerAlly.ship.getCollisionRadius() * 2f + ship.getCollisionRadius() * 2f), backBiggerAllyToTargetAngle);
                        backBiggerAllyToTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        RC_BaseAIAction.move(ship, ship.getFacing(), backBiggerAllyToTargetAngle);
                    } else {
                        backBiggerAllyToTargetAngle = backBiggerAllyToTargetAngle + 90;
                        Vector2f targetPoint = MathUtils.getPointOnCircumference(backBiggerAlly.ship.getLocation(), (backBiggerAlly.ship.getCollisionRadius() * 2f + ship.getCollisionRadius() * 2f), backBiggerAllyToTargetAngle);
                        backBiggerAllyToTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetPoint);
                        RC_BaseAIAction.move(ship, ship.getFacing(), backBiggerAllyToTargetAngle);
                    }
                    isDodge = true;
                }
                //}
            }
        }

        //躲开快爆炸的
        if (nearestBiggerAlly != null&&!isDodge) {
            if ((nearestBiggerAlly.ship.getHullSize().compareTo(ship.getHullSize()) >= 0 && (nearestBiggerAlly.ship.getHitpoints() / nearestBiggerAlly.ship.getHitpoints() < 0.2F && nearestBiggerAlly.minDistance < (nearestBiggerAlly.ship.getCollisionRadius())))) {
                RC_BaseAIAction.move(ship, ship.getFacing(), VectorUtils.getAngle(nearestBiggerAlly.ship.getLocation(), ship.getLocation()));
                isDodge = true;
            }
        }
        if (nearestBiggerShip != null&&!isDodge) {
            if ((nearestBiggerShip.ship.getHullSize().compareTo(ship.getHullSize()) >= 0 && (nearestBiggerShip.ship.getHitpoints() / nearestBiggerShip.ship.getHitpoints() < 0.2F && nearestBiggerShip.minDistance < (nearestBiggerShip.ship.getCollisionRadius()))) || ship.getFluxTracker().isOverloaded() || disabledWeaponList.size()/ship.getAllWeapons().size()>0.5) {
                RC_BaseAIAction.move(ship, ship.getFacing(), VectorUtils.getAngle(nearestBiggerShip.ship.getLocation(), ship.getLocation()));
                isDodge = true;
            }
        }
        //如果 大船看自己
        if (nearestBiggerShip != null&&!isDodge) {
            if (nearestBiggerShip.ship.getHullSize().compareTo(ship.getHullSize())>0) {
                if (Math.abs(MathUtils.getShortestRotation(nearestBiggerShip.ship.getFacing(), VectorUtils.getAngle(nearestBiggerShip.ship.getLocation(), ship.getLocation()))) < 90) {
                    RC_BaseAIAction.move(ship, ship.getFacing(), VectorUtils.getAngle(nearestBiggerShip.ship.getLocation(), ship.getLocation()));
                    isDodge = true;
                }
            }
        }
    }

    public void turn(float amount){
        //距离目标太远敌人太近
        if (nearestBiggerShip!=null) {
            if (MathUtils.getDistance(ship,target)>minWingRange&&nearestBiggerShip.minDistance<minWeaponRange &&
                    //
                    Math.abs(MathUtils.getShortestRotation(nearestBiggerShip.ship.getFacing(),VectorUtils.getAngle(nearestBiggerShip.ship.getLocation(), ship.getLocation())))< 5f
            ) {
                faceLocation = nearestBiggerShip.ship.getLocation();
                RC_BaseAIAction.turn(ship, nearestBiggerShip.ship.getLocation(), amount);
                return;
            }
        }
        //面向OTHER 应该是 以目标自己 other自己的相对位置决定方向 跟 face无关
        if (other!=null) {
            if ((other.ship.getOwner()==ship.getOwner()||other.ship.getOwner()==100)&&MathUtils.getDistance(ship,other.ship)>other.ship.getCollisionRadius()+ship.getCollisionRadius()) {
                float shipToTarget = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
                float shipToOtherAngle = VectorUtils.getAngle(ship.getLocation(), other.ship.getLocation());
                if (MathUtils.getShortestRotation(shipToTarget, shipToOtherAngle) > 0) {
                    shipToOtherAngle = shipToOtherAngle - 90;
                    faceLocation = MathUtils.getPointOnCircumference(other.ship.getLocation(), (other.ship.getCollisionRadius() + ship.getCollisionRadius() * 2f) , shipToOtherAngle);
                } else {
                    shipToOtherAngle = shipToOtherAngle + 90;
                    faceLocation = MathUtils.getPointOnCircumference(other.ship.getLocation(), (other.ship.getCollisionRadius() + ship.getCollisionRadius() * 2f) , shipToOtherAngle);
                }
                RC_BaseAIAction.turn(ship, faceLocation, amount);
                return;
            }
        }
        //如果背后有船 并且 在射程范围外
        if (backBiggerAlly != null) {
            if (target != null) {
                //如果技能正在冷却
                //ship.getSystem().getCooldownRemaining()<2&&ship.getSystem().getCooldownRemaining()>0&&
                if (RC_AIContants.driveSystemId.contains(ship.getSystem().getId())) {
                    //if (backBiggerAlly.ship.getShipTarget() == target) {
                    if (ship.getHullSize().compareTo(target.getHullSize()) >= 0) {
                        //移动到目标侧边
                        float shipToTargetAngle = VectorUtils.getAngle(ship.getLocation(), target.getLocation());
                        float backBiggerAllyToTargetAngle = VectorUtils.getAngle(backBiggerAlly.ship.getLocation(), target.getLocation());

                        if (MathUtils.getShortestRotation(backBiggerAllyToTargetAngle, shipToTargetAngle) > 0) {
                            backBiggerAllyToTargetAngle = backBiggerAllyToTargetAngle - 90;
                            faceLocation = MathUtils.getPointOnCircumference(target.getLocation(), (target.getCollisionRadius() + ship.getCollisionRadius() * 2f), backBiggerAllyToTargetAngle);
                        } else {
                            backBiggerAllyToTargetAngle = backBiggerAllyToTargetAngle + 90;
                            faceLocation = MathUtils.getPointOnCircumference(target.getLocation(), (target.getCollisionRadius() + ship.getCollisionRadius() * 2f), backBiggerAllyToTargetAngle);
                        }
                        //if (MathUtils.getDistance(faceLocation,ship.getLocation())>minWeaponRange) {
                        RC_BaseAIAction.turn(ship, faceLocation, amount);
                        return;
                        //}
                    }
                    //}
                }
            }
        }
        faceLocation = null;
        RC_BaseAIAction.turn(ship, target.getLocation(), amount);
    }

    /**
     * 主引擎扫描添加
     *
     * 小队成员需要互相靠近 分布在队长两侧
     * 与非小队成员 保持 射程距离 保持距离方式为 与非小队成员目标
     *
     * 给屁股后面的队友船让道
     *
     * 向目标前进 当 目标大于自己且目标附近没有队友（队友加起来战斗力不如对方的2倍）时且不在屁股 停止往目标前进 退到射程最外
     * 给大队友让路
     *
     * 严格三船一小组 小组间保持距离
     *
     * 自己不在在最近的敌人最大射程内 面相目标点移动
     *
     * 开局没发现敌人的时候尽量分开 小队队长保持最大射程 但是不要是前后保持 改成保持角度吧 相同目标的 两个小队 角度不能小于最小射程弧度 船 左边 最小射程 获取一个点 ，三个点获取角度差 与 角度相比
     *
     * 如果角度不满足就 往错开位置移动
     *
     *
     * 不要离要爆炸的船太近
     * 最优先 不要里船太近
     * 向着目标进发 绕过 障碍
     * 没有队友时保持距离
     *
     * 保持距离 如果目标相同保持角度
     *
     * 面相阻挡 阻挡是敌人切比自己大
     * 面相最近且比自己大
     * 面相目标 目标射程范围内已经有自己
     */
    public static class Relationship{
        //目标 位置
        public Vector2f targetLocation;
        //目标 船
        public ShipAndDistance targetShip;
        //与目标之间的阻挡 这个要用位置去计算阻隔
        public ShipAndDistance other;
        //距离最近的船不包括飞机
        public ShipAndDistance nearestShip;
        public ShipAndDistance nearestBiggerShip;
        //距离最近的船包括hulk
        public ShipAndDistance nearestShipHulk;
        //距离最近的队友
        public ShipAndDistance nearestAlly;
        //距离最近且大于等于自己的队友
        public ShipAndDistance nearestBiggerAlly;
        //距离最近的敌人
        public ShipAndDistance nearestEnemy;
        //距离最近的敌人 不是飞机
        public ShipAndDistance nearestEnemyNotFighter;
        //距离最近且大于等于自己的敌人
        public ShipAndDistance nearestBiggerEnemy;
        //队长
        public ShipAndDistance leader;
        //
        public float minWingRange;
        //最大射程
        public float maxWeaponRange;
        //最小射程
        public float minWeaponRange;
        //最大射程范围内的船 敌人
        public Set<ShipAPI> enemyInMaxWeaponRange = new HashSet<ShipAPI>();
        //最小射程范围内的船 敌人
        public Set<ShipAPI> enemyInMinWeaponRange = new HashSet<ShipAPI>();
        //屁股后面的队友
        public ShipAndDistance back;
        //存放大于自己并且比目标近的船
        public Set<ShipAPI> biggerList = new HashSet<ShipAPI>();
        public Set<ShipAPI> biggerEnemyList = new HashSet<ShipAPI>();
        //被作为目标的次数
        public int beTargetCount;
        public Set<ShipAPI> myFighterList = new HashSet<ShipAPI>();
    }
}

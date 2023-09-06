package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.loading.WeaponGroupSpec;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

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
 */
public class RC_BaseShipAIBack implements ShipAIPlugin {
    public CombatEngineAPI engine = Global.getCombatEngine();
    public ShipAPI ship;
    public ShipAPI oldTarget = null;
    public ShipAPI target;
    public ShipAPI leader = null;
    public boolean isLeader = false;

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

    //一次循环找到所有目标
    //最近的目标
    ShipAndDistance nearestEnemy = null;
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
    List<ShipAPI> enemyList = new ArrayList<>();
    List<ShipAPI> fighterList = new ArrayList<>();
    List<ShipAPI> coverList = new ArrayList<>();

    List<ShipAPI> myFighterList = new ArrayList<>();
    Set<MissileAPI> missileList = new HashSet<>();
    Set<WeaponAPI> missileWeaponList = new HashSet<>();
    Set<WeaponAPI> disabledWeaponList = new HashSet<>();

    Set<BeamAPI> mayHitBeam = new HashSet<>();

    Set<CombatEntityAPI> mayHitProj = new HashSet<>();
    public RC_BaseShipAIBack(ShipAPI ship) {
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
        if (engine.isPaused()) {return;}
        if (!ship.isAlive()) {
            //刷新可用队友
            if (engine.getCustomData().get("RC_SmartAIEveryFrameCombatPluginenemyList")!=null) {
                List<ShipAPI> allyList = (List<ShipAPI>) engine.getCustomData().get("RC_SmartAIEveryFrameCombatPluginenemyList");
                allyList.remove(ship);
                allyList.removeAll(teamList);
                engine.getCustomData().put("RC_SmartAIEveryFrameCombatPluginenemyList", (List<ShipAPI>) engine.getCustomData().get("RC_SmartAIEveryFrameCombatPluginenemyList"));
            }
            if (ship.getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP))
            {
                target.removeCustomData("findBestTargetCAPITAL_SHIP");
            }
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
        try {
            if (ship.getCustomData().get("setWeaponGroup") == null) {
                setWeaponGroup();
                ship.getCustomData().put("setWeaponGroup", "setWeaponGroup");
            }
            if (ship.getCustomData().get("setWeaponGroup") != null) {
                weaponController();
            }
            vent();
            findBestTarget();
            if (target == null) {
                Global.getLogger(this.getClass()).info("怎么可能1");
            } else if (target.isHulk() || !target.isAlive()) {
                Global.getLogger(this.getClass()).info("怎么可能2");
            }
            boolean isDodge = false;
            //要分开写阻尼 TODO
            if (ship.getPhaseCloak() != null) {
                usePhase(amount);
            } else if (ship.getShield() != null) {
                useShield(amount);
            }
            //如果闪避了就不移动了 TODO
            else {
                if (!isDodge) {
                    float minDistance = ship.getMaxSpeed();
                    DamagingProjectileAPI targetProjectile = null;
                    List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
                    for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
                        float range = MathUtils.getDistance(ship, damagingProjectile);
                        if (range <= minDistance + damagingProjectile.getVelocity().length() && mayHit(damagingProjectile, ship.getCollisionRadius())) {
                            minDistance = range;
                            targetProjectile = damagingProjectile;
                        }
                    }
                    if (targetProjectile != null) {
                        if (target != null) {
                            if (dodge(targetProjectile, target, amount)) {
                                isDodge = true;
                            }
                        }
                    }
                }
            }
            //如果屁股后面有
            //
            if (!isDodge) {
                if (target != null) {
                    RC_BaseAIAction.turn(ship, target.getLocation(), amount);
                }
                if (backBiggerAlly != null) {
                    if (target != null) {
                        RC_BaseAIAction.shift(ship, VectorUtils.getAngle(backBiggerAlly.ship.getLocation(), ship.getLocation()), backBiggerAlly.ship.getFacing());
                        ship.giveCommand(ShipCommand.ACCELERATE_BACKWARDS, null, 0);
                        isDodge = true;
                    }
                }
                if (nearestBiggerShip != null) {
                    if ((nearestBiggerShip.ship.getHullSize().compareTo(ship.getHullSize()) >= 0 && (nearestBiggerShip.ship.getHitpoints() / nearestBiggerShip.ship.getHitpoints() < 0.3F && nearestBiggerShip.minDistance < (target.getCollisionRadius()))) || ship.getFluxTracker().isOverloaded()) {
                        RC_BaseAIAction.move(ship, ship.getFacing(), VectorUtils.getAngle(nearestBiggerShip.ship.getLocation(), ship.getLocation()));
                        isDodge = true;
                    } else if (nearestEnemy != null) {
                        if (nearestEnemy.minDistance > maxWeaponRange && nearestBiggerShip.minDistance < minWeaponRange) {
                            RC_BaseAIAction.move(ship, ship.getFacing(), VectorUtils.getAngle(nearestBiggerShip.ship.getLocation(), ship.getLocation()));
                            isDodge = true;
                        }
                    }
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
        }catch (Exception e) {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    /**
     * 优化算法 targetFacing 和 targetToSihp 差值
     * @param dodgeTarget 闪避目标
     * @param shipTarget 面相的目标
     * @param amount
     * @return
     */
    public boolean dodge(DamagingProjectileAPI dodgeTarget,ShipAPI shipTarget, float amount) {
        float dodgeTargetFacing = dodgeTarget.getFacing();
        float dodgeTargetToShip = VectorUtils.getAngle(dodgeTarget.getLocation(),ship.getLocation());
        float newAngle = dodgeTargetToShip - 90;
        if (MathUtils.getShortestRotation(dodgeTargetFacing, dodgeTargetToShip)>0) {
            newAngle = dodgeTargetToShip + 90;
        }
        if (mayHit(dodgeTarget,ship.getCollisionRadius()*3f)
        ) {
            //engine.addFloatingText(ship.getLocation(), "闪避", 25f, Color.BLUE, ship, 5f, 10f);
            //engine.addFloatingTextAlways(ship.getLocation(), "闪避", 25f, Color.BLUE, ship,
            //        1, 1, amount,0, 0, 1f);
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

            float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),shipTarget.getLocation());
            float needTurnAngle = Math.abs(MathUtils.getShortestRotation(shipFacing + ship.getAngularVelocity() * amount, toTargetAngle));
            RC_BaseAIAction.turn(ship, needTurnAngle, toTargetAngle, amount);
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
            if (((nearestBiggerShip.ship.getHitpoints()/nearestBiggerShip.ship.getHitpoints()<0.2f&&nearestBiggerShip.minDistance<(ship.getCollisionRadius()+target.getCollisionRadius())))) {
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
            if (((nearestBiggerShip.ship.getHitpoints()/nearestBiggerShip.ship.getHitpoints()<0.2f&&nearestBiggerShip.minDistance<(ship.getCollisionRadius()+target.getCollisionRadius())))) {
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
                    if (MathUtils.getShortestRotation(shield.getFacing(),VectorUtils.getAngle(ship.getLocation(),mouseTarget))<shieldArc/2) {
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
                if (!DamageType.KINETIC.equals(damagingProjectile.getDamageType())) {
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
        /*
        DamagingProjectileAPI leftDamagingProjectile = null;
        DamagingProjectileAPI rightDamagingProjectile = null;
        for (DamagingProjectileAPI eo:engineHitProjectiles) {
            for (DamagingProjectileAPI ei:engineHitProjectiles) {
                if (!ei.equals(eo)) {
                    float miToShip = VectorUtils.getAngle(ei.getLocation(),ship.getLocation());
                    float moToShip = VectorUtils.getAngle(eo.getLocation(),ship.getLocation());
                    float arc = Math.abs(MathUtils.getShortestRotation(miToShip,moToShip));
                    if (arc>maxArc) {
                        maxArc = arc;
                        leftDamagingProjectile = eo;
                        rightDamagingProjectile = ei;
                    }
                }
            }
        }
        if (rightDamagingProjectile==null&&leftDamagingProjectile==null) {
            for (DamagingProjectileAPI mo : mayHitProjectiles) {
                for (DamagingProjectileAPI mi : mayHitProjectiles) {
                    if (!mi.equals(mo)) {
                        float miToShip = VectorUtils.getAngle(mi.getLocation(), ship.getLocation());
                        float moToShip = VectorUtils.getAngle(mo.getLocation(), ship.getLocation());
                        float arc = Math.abs(MathUtils.getShortestRotation(miToShip, moToShip));
                        if (arc > maxArc) {
                            maxArc = arc;
                            leftDamagingProjectile = mo;
                            rightDamagingProjectile = mi;
                        }
                    }
                }
            }
        }
         */
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
            //原版会取消护盾重新开启 以maxProjectile 为基准
            //float distacne = MathUtils.getDistance(maxProjectile,ship.getLocation());
            //float remain = distacne/maxProjectile.getVelocity().length();
            // shield.getRingRotationRate()
            /*
            if (shieldActiveArc<maxArc) {
                if (Math.abs(MathUtils.getShortestRotation(shield.getFacing(),VectorUtils.getAngle(ship.getLocation(),mouseTarget)))
                        /shield.getInnerRotationRate()
                       >remain) {
                    if (shield.isOn()) {
                        if (shieldArc/3<=shieldActiveArc) {
                            ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,mouseTarget, 0);
                        }
                    }
                }
            }
            */
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
            /*
            if (!ship.getShield().isOn()) {
                shield.setActiveArc(0);
                shield.forceFacing(VectorUtils.getAngle(ship.getLocation(),mouseTarget));
                ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,mouseTarget, 0);
            }
             */
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
        /*
        ShipVariantAPI variant = ship.getVariant();
        variant.setSource(VariantSource.REFIT);
        List<WeaponGroupSpec> vg = ship.getVariant().getWeaponGroups();
        List<WeaponAPI> old = new ArrayList<>();
        for (WeaponGroupSpec g:vg) {
            if (weaponGroupSpec.get(g)==null) {
                weaponGroupSpec.put(g,g.getSlots());
            }
        }

        //for (WeaponGroupSpec g:vg) {
        //    if (weaponGroupSpec.get(g)!=null) {
        //        for (String slotId:weaponGroupSpec.get(g)) {
        //            g.removeSlot(slotId);
         //       }
        //    }
        //}

        vg.clear();
        variant.getWeaponGroups();
        WeaponGroupSpec pd = new WeaponGroupSpec(WeaponGroupType.LINKED);
        WeaponGroupSpec ship = new WeaponGroupSpec(WeaponGroupType.LINKED);
        WeaponGroupSpec kinetic = new WeaponGroupSpec(WeaponGroupType.LINKED);
        WeaponGroupSpec other = new WeaponGroupSpec(WeaponGroupType.LINKED);
        for (WeaponAPI w:ship.getAllWeapons()) {
            //pd武器一类
            if (w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD) || w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD_ALSO)) {
                pd.addSlot(w.getSlot().getId());
            }
            //无限导弹
            else if (WeaponAPI.WeaponType.ship.equals(w.getType())&&!w.usesAmmo()) {
                ship.addSlot(w.getSlot().getId());
            }
            //打盾
            else if (DamageType.KINETIC.equals(w.getDamageType())) {
                kinetic.addSlot(w.getSlot().getId());
            }
            //打甲
            else {
                other.addSlot(w.getSlot().getId());
            }
        }
        variant.addWeaponGroup(pd);
        variant.addWeaponGroup(ship);
        variant.addWeaponGroup(kinetic);
        variant.addWeaponGroup(other);
        */
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
                if (weaponGroup.get(g) == null) {
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
        /*
        if (other!=null&&target!=null){
            if (other.ship.getOwner()==100&&other.ship.) {
                for (WeaponGroupAPI g : ship.getWeaponGroupsCopy()) {
                    g.toggleOff();
                }
                return;
            }
        }
         */
        if (ship.getWeaponGroupsCopy().size()>=4) {
            List<WeaponGroupAPI> list = ship.getWeaponGroupsCopy();
            if (target != null) {
                float flux = ship.getFluxLevel();
                float targetFlux = target.getFluxLevel();
                //幅能充足 敌方幅能爆炸
                if (flux<0.5f||targetFlux>0.7f||target.getFluxTracker().isOverloadedOrVenting()) {
                    for (WeaponGroupAPI g : ship.getWeaponGroupsCopy()) {
                        for (WeaponAPI w:g.getWeaponsCopy()) {
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
    public void useDriveSystem(){
        if (target==null) {
            return;
        }
        ShipSystemAPI shipSystem = ship.getSystem();
        float distance = MathUtils.getDistance(ship.getLocation(),target.getLocation());
        if (other!=null&&distance<(minWeaponRange)) {
            if(other.ship.getOwner()==ship.getOwner())
            {
                if (shipSystem.isActive()) {
                    ship.giveCommand(ShipCommand.USE_SYSTEM,null,0);
                }
                return;
            }
        }
        if (distance<(ship.getCollisionRadius()*2+target.getCollisionRadius())) {
            if (shipSystem.isActive()) {
                ship.giveCommand(ShipCommand.USE_SYSTEM,null,0);
            }
            return;
        }
        if (ship.getHitpoints()/ship.getMaxHitpoints()<0.2f) {
            if (shipSystem.isActive()) {
                ship.giveCommand(ShipCommand.USE_SYSTEM,null,0);
            }
            return;
        }
        if (disabledWeaponList.size()/ship.getAllWeapons().size()>0.5) {
            return;
        }
        if (ship.getFluxLevel()>0.9f) {
            return;
        }
        if (shipSystem.isActive()) {
            return;
        }

        if (Math.abs(MathUtils.getShortestRotation(ship.getFacing(),VectorUtils.getAngle(ship.getLocation(),target.getLocation())))<5f) {
            ship.useSystem();
        }
    }

    public void vent() {
        if (target!=null) {
            if (!target.isAlive()) {
                if (ship.getFluxLevel()<0.3f) {
                    if (!ship.getFluxTracker().isVenting()) {
                        ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
                    }
                }
                //待测试
                if (target.getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP)) {
                    if (!ship.getFluxTracker().isVenting()&&nearestBiggerAlly!=null) {
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

    public class ShipAndDistance{
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

    public class MissileAndDistance{
        public MissileAPI missile;
        public float minDistance;
        public MissileAndDistance(MissileAPI missile,float minDistance)
        {
            this.missile = missile;
            this.minDistance = minDistance;
        }
    }

    public class DamagingProjectileAndDistance{
        public DamagingProjectileAPI projectile;
        public float minDistance;
        public DamagingProjectileAndDistance(DamagingProjectileAPI projectile,float minDistance)
        {
            this.projectile = projectile;
            this.minDistance = minDistance;
        }
    }

    public class BeamAndDistance{
        public BeamAPI beam;
        public float minDistance;
        public BeamAndDistance(BeamAPI beam,float minDistance)
        {
            this.beam = beam;
            this.minDistance = minDistance;
        }
    }

    public class BeamOrDamagingProjectileAndDistance{
        public BeamAPI beam;
        public DamagingProjectileAPI projectile;
        public float minDistance;
        public BeamOrDamagingProjectileAndDistance(BeamAPI beam,float minDistance)
        {
            this.beam = beam;
            this.minDistance = minDistance;
        }
        public BeamOrDamagingProjectileAndDistance(DamagingProjectileAPI projectile,float minDistance)
        {
            this.projectile = projectile;
            this.minDistance = minDistance;
        }
    }

    public BeamOrDamagingProjectileAndDistance findNearestBeamOrDamagingProjectile(BeamOrDamagingProjectileAndDistance beamOrDamagingProjectileAndDistance,DamagingProjectileAPI d,BeamAPI b) {
        if (d!=null) {
            float distance = MathUtils.getDistance(d, ship);
            if (beamOrDamagingProjectileAndDistance.minDistance > distance) {
                beamOrDamagingProjectileAndDistance.projectile = d;
                beamOrDamagingProjectileAndDistance.minDistance = distance;
            }
        }
        else if (b!=null) {
            float distance = MathUtils.getDistance(b.getTo(), ship.getLocation()) + ship.getCollisionRadius();
            if (beamOrDamagingProjectileAndDistance.minDistance > distance) {
                beamOrDamagingProjectileAndDistance.beam = b;
                beamOrDamagingProjectileAndDistance.minDistance = distance;
            }
        }
        return beamOrDamagingProjectileAndDistance;
    }

    public CombatEntityAndDistance findNearestCombatEntityAndDistance(CombatEntityAndDistance combatEntityAndDistance,CombatEntityAPI c) {
        float distance = MathUtils.getDistance(c, ship);
        if (combatEntityAndDistance.minDistance>distance)
        {
            combatEntityAndDistance.combatEntity = c;
            combatEntityAndDistance.minDistance = distance;
        }
        return combatEntityAndDistance;
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


    public boolean isOtherTrue(ShipAPI target, ShipAPI other) {
        float radius = other.getCollisionRadius();
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
        //绕开使用
        nearestEnemy = null;
        nearestShip = null;
        //最近的船 开盾使用
        nearestBiggerShip = null;
        //名字没改暂时使用
        nearestBiggerEnemy = null;
        nearestBiggerAlly = null;
        backBiggerAlly = null;
        //最近的超载目标
        ShipAndDistance minOverLoad = null;
        //最近的残血目标
        ShipAndDistance minHp = null;
        //最近的高幅能目标
        ShipAndDistance minFlux = null;
        //最近的比较弱的目标
        ShipAndDistance minWeak = null;
        //舰船级别比自己小的目标
        ShipAndDistance minHullSize = null;

        //最近的超载目标
        ShipAndDistance maxOverLoad = null;
        //最近的残血目标
        ShipAndDistance maxHp = null;
        //最近的高幅能目标
        ShipAndDistance maxFlux = null;
        //最近的比较弱的目标
        ShipAndDistance maxWeak = null;
        //舰船级别比自己小的目标
        ShipAndDistance maxHullSize = null;
        enemyList.clear();
        fighterList.clear();
        coverList.clear();
        myFighterList.clear();
        for (ShipAPI s :engine.getShips()) {
            if (s!=ship) {
                if (!s.isFighter()) {
                    if (nearestShip == null) {
                        nearestShip = new ShipAndDistance(s, MathUtils.getDistance(s, ship));
                    } else {
                        nearestShip = findNearestShip(nearestShip, s);
                    }
                    if ((s.getOwner()==ship.getOwner()||s.getOwner()==100)&&(s.getHullSize().compareTo(ship.getHullSize())>=0||s.getHullSize()==ship.getHullSize().smaller(false))) {
                        coverList.add(s);
                    }
                }
                else {
                    if (!s.isHulk() && s.isAlive() && s.getWing()!=null) {
                        if (s.getWing().getSourceShip()!=null) {
                            if (s.getWing().getSourceShip().equals(ship)) {
                                myFighterList.add(s);
                            }
                        }
                    }
                }
                if (!s.isHulk() && s.isAlive() && s.getOwner()!=100&&s.getOwner()!=ship.getOwner()) {
                    if (!s.isFighter()) {
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
                                &&Math.abs(MathUtils.getShortestRotation(s.getFacing(), ship.getFacing())) < 45
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
                    /*
                    //最近的过载目标
                    if (s.getFluxTracker().isOverloadedOrVenting()) {
                        if (minOverLoad == null) {
                            minOverLoad = new ShipAndDistance(s, distance);
                        }
                        else {
                            minOverLoad = findNearestShip(minOverLoad,s);
                        }
                    }

                    if (minOverLoad!=null) {
                        continue;
                    }

                    //最近的血量最少的
                    if (minHp == null) {
                        minHp = new ShipAndDistance(s, distance);
                    }
                    else if(s.getHitpoints()/s.getMaxHitpoints()<minHp.ship.getHitpoints()/minHp.ship.getMaxHitpoints()&&s.getHitpoints()/s.getMaxHitpoints()<0.5f){
                        minHp = new ShipAndDistance(s, distance);
                    }

                    if (minHp!=null) {
                        continue;
                    }

                    //幅能最高的
                    if (minFlux == null) {
                        minFlux = new ShipAndDistance(s, distance);
                    }
                    else if(s.getCurrFlux()>minFlux.ship.getCurrFlux()){
                        minFlux = new ShipAndDistance(s, distance);
                    }

                    if (minFlux!=null) {
                        continue;
                    }

                    /*
                    //比自己弱的
                    if (minWeak == null) {
                        minWeak = new ShipAndDistance(s, distance);
                    }
                    else if(s.getHullSpec().getFleetPoints()<minWeak.ship.getHullSpec().getFleetPoints()){
                        minWeak = new ShipAndDistance(s, distance);
                    }
                    else if(s.getHullSpec().getFleetPoints()==minWeak.ship.getHullSpec().getFleetPoints()){
                        minWeak = findNearestShip(minWeak,s);
                    }

                    if (minWeak!=null) {
                        continue;
                    }

                    //比自己舰船等级小的
                    if (ship.getHullSize() == s.getHullSize()) {
                        if (minHullSize == null) {
                            minHullSize = new ShipAndDistance(s, distance);
                        }
                        else
                        {
                            minHullSize = findNearestShip(minHullSize, s);
                        }
                    }
                    */
                }

                if (!s.isHulk() && s.isAlive() && distance<maxWeaponRange&&!s.isFighter()&&s.getOwner()!=100&&s.getOwner()!=ship.getOwner()&&!s.isFighter()) {
                    if (s.getHullSize().compareTo(ship.getHullSize())>=0) {
                        enemyList.add(s);
                    }
                    /*
                    if (minOverLoad!=null) {
                        continue;
                    }

                    //最近的过载目标
                    if (s.getFluxTracker().isOverloaded()) {
                        if (maxOverLoad == null) {
                            maxOverLoad = new ShipAndDistance(s, distance);
                        }
                        else {
                            maxOverLoad = findNearestShip(maxOverLoad,s);
                        }
                    }

                    if (maxOverLoad!=null) {
                        continue;
                    }

                    if (minHp!=null) {
                        continue;
                    }

                    //最近的血量最少的
                    if (maxHp == null) {
                        maxHp = new ShipAndDistance(s, distance);
                    }
                    else if(s.getHitpoints()/s.getMaxHitpoints()<maxHp.ship.getHitpoints()/maxHp.ship.getMaxHitpoints()&&s.getHitpoints()/s.getMaxHitpoints()<0.5f){
                        maxHp = new ShipAndDistance(s, distance);
                    }

                    if (maxHp!=null) {
                        continue;
                    }

                    if (minFlux!=null) {
                        continue;
                    }

                    //幅能最高的
                    if (maxFlux == null) {
                        maxFlux = new ShipAndDistance(s, distance);
                    }
                    else if(s.getFluxLevel()>maxFlux.ship.getFluxLevel()&&s.getFluxLevel()>0.5f){
                        maxFlux = new ShipAndDistance(s, distance);
                    }

                    if (maxFlux!=null) {
                        continue;
                    }
                    if (minWeak!=null) {
                        continue;
                    }

                    //比自己弱的
                    if (maxWeak == null) {
                        maxWeak = new ShipAndDistance(s, distance);
                    }
                    else if(s.getHullSpec().getFleetPoints()<maxWeak.ship.getHullSpec().getFleetPoints()){
                        maxWeak = new ShipAndDistance(s, distance);
                    }
                    else if(s.getHullSpec().getFleetPoints()==maxWeak.ship.getHullSpec().getFleetPoints()){
                        maxWeak = findNearestShip(maxWeak,s);
                    }

                    if (maxWeak!=null) {
                        continue;
                    }
                    if (minHullSize!=null) {
                        continue;
                    }

                    //比自己舰船等级小的
                    if (maxHullSize == null) {
                        maxHullSize = new ShipAndDistance(s, distance);
                    }
                    else if(s.getHullSize().compareTo(maxHullSize.ship.getHullSize())<0){
                        maxHullSize = new ShipAndDistance(s, distance);
                    }
                    else if(s.getHullSize().compareTo(maxHullSize.ship.getHullSize())==0){
                        maxHullSize = findNearestShip(maxHullSize,s);
                    }
                     */
                }
            }
        }

        if (target!=null) {
            int count = 0;
            if (ship.getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP))
            {
                target.removeCustomData("findBestTargetCAPITAL_SHIP");
            }

            if (target.getCustomData().get("findBestTarget") != null) {
                count = (int) target.getCustomData().get("findBestTarget");
                count--;
            }
            target.setCustomData("findBestTarget",count);
        }
        target = null;

        //跟随队长
        if (!isLeader) {
            if (ship.getCustomData().get("RC_BaseShipAI_lederTarget")!=null) {
                target = (ShipAPI) ship.getCustomData().get("RC_BaseShipAI_lederTarget");
            }
        }

        //如果目标不是最近的船也不是最近最大的船
        if (nearestShip!=null) {
            other = nearestShip;
            if (other.ship.isFighter()) {
                other = null;
            }
        }

        if (minOverLoad!=null) {
            target = minOverLoad.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }
            else if (getWeight(target)<=0){
                target = null;
            }
        }
        if (maxOverLoad!=null&&target==null) {
            target = maxOverLoad.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }
            else if (getWeight(target)<=0){
                target = null;
            }
        }

        if (minHp!=null&&target==null) {
            target = minHp.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }else if (getWeight(target)<=0){
                target = null;
            }
        }
        if (maxHp!=null&&target==null) {
            target = maxHp.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }else if (getWeight(target)<=0){
                target = null;
            }
        }

        if (minFlux!=null&&target==null) {
            target = minFlux.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }else if (getWeight(target)<=0){
                target = null;
            }
        }
        if (maxFlux!=null&&target==null) {
            target = maxFlux.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }else if (getWeight(target)<=0){
                target = null;
            }
        }

        if (minWeak!=null&&target==null) {
            target = minWeak.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }else if (getWeight(target)<=0){
                target = null;
            }
        }

        if (maxWeak!=null&&target==null) {
            target = maxWeak.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }else if (getWeight(target)<=0){
                target = null;
            }
        }

        if (minHullSize!=null&&target==null) {
            target = minHullSize.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }else if (getWeight(target)<=0){
                target = null;
            }
        }

        if (maxHullSize!=null&&target==null) {
            target = maxHullSize.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }else if (getWeight(target)<=0){
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

        if (nearestBiggerShip!=null&&target==null) {
            target = nearestBiggerShip.ship;
            if (target.isHulk()||!target.isAlive()) {
                target = null;
            }else if (getWeight(target)<=0){
                target = null;
            }
        }
        if (nearestEnemy!=null&&target==null){
            target = nearestEnemy.ship;
        }

        if (other!=null&&target!=null) {
            if (target.equals(other.ship)) {
                other = null;
            }
            else if (other.ship.isFighter()) {
                other = null;
            }
        }

        if (nearestEnemy==null) {
            Global.getLogger(this.getClass()).info("怎么可能");
        }

        if (other==null) {
            if (ship.getMouseTarget()!=null&&target!=null) {
                ship.getMouseTarget().set(target.getLocation());
            }
            ship.setShipTarget(target);
        }
        else if(other.ship.getOwner()!=ship.getOwner()&&other.ship.getOwner()!=100){
            //ship.setShipTarget(other.ship);
        }
        else {
            ship.setShipTarget(null);
        }

        if (target!=null) {
            ship.getMouseTarget().set(target.getLocation());
            if (ship.getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP))
            {
                target.setCustomData("findBestTargetCAPITAL_SHIP",1);
            }
            int count = 0;
            if (target.getCustomData().get("findBestTarget") != null) {
                count = (int) target.getCustomData().get("findBestTarget");
            }
            count++;
            target.setCustomData("findBestTarget",count);
        }
    }

    public float getWeight(ShipAPI target) {
        float weight = 1f;
        int count = 0;
        if (target.getCustomData().get("findBestTarget") != null) {
            count = (int) target.getCustomData().get("findBestTarget");
            if (count>1) {
                weight/=ship.getHullSpec().getFleetPoints()*target.getHullSize().ordinal();//count*2;
            }
            else {
                weight *= target.getHullSize().ordinal();
            }
        }
        if (ship.getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP))
        {
            if (target.getCustomData().get("findBestTargetCAPITAL_SHIP")!=null) {
                weight/=ship.getHullSpec().getFleetPoints()/target.getHullSize().ordinal();
            }
        }
        //是否过载或者散福能 + 福能
        if (target.getCurrFlux()!=0) {
            weight+=target.getCurrFlux();
            if (target.getFluxTracker().isOverloadedOrVenting()) {
                weight*=2;
            }
            if (target.getFluxLevel()>0.5f) {
                //舰船强度 处以
                weight += target.getHullSpec().getFleetPoints();
            }
        }
        //福能+
        //血量剩余 比例 处以
        if (target.getHitpoints()!=0) {
            weight = weight * target.getMaxHitpoints() / target.getHitpoints();
        }
        else {
            return 0;
        }

        //舰船屁股插值 处以
        float angle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(),target.getLocation()),target.getFacing()));
        if (angle!=0) {
            weight /= angle;
        }
        else {
            weight*=180;
        }
        //舰船转向 处以
        /*
        angle = Math.abs(MathUtils.getShortestRotation(ship.getFacing(),VectorUtils.getAngle(ship.getLocation(),target.getLocation())));
        if (angle!=0) {
            weight /= angle;
        }
        else {
            weight*=18;
        }
        */
        float distance = MathUtils.getDistance(ship,target);
        if (distance>minWeaponRange) {
            weight/=distance*2;
        }
        else if (distance>0) {
            weight/=distance;
        }

        if (target.getHullSize().equals(ship.getHullSize())) {
            weight *= ship.getHullSize().ordinal();
        }

        if (target.getHullSize().compareTo(ship.getHullSize())>0&&ship.getFluxLevel()>0.5) {
            weight *= 1+ship.getFluxLevel();
        }

        return weight;
    }

    public void useSystemRun() {

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
        //
        //
    }
}

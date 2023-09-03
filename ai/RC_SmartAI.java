package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.prototype.entities.Ship;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * 移动
 * 开火
 * 盾控
 * 技能
 */

public class RC_SmartAI {
    private final static String ID = "RC_SmartAI";
    private CombatEngineAPI engine = Global.getCombatEngine();
    private ShipwideAIFlags AIFlags = new ShipwideAIFlags();
    private ShipAPI ship;

    public RC_SmartAI(ShipAPI ship) {
        this.ship = ship;
    }
    /**
     * 重视玩家体验，命令最优先
     * 命令 1 护航 保证不遮挡炮轴 保证大船包小船
     * 命令 2 进攻 保证优先偷屁股
     * 命令 3 移动 有推进器用推进器 向无人区移动时专注移动
     * 命令 4 占点 有强大敌人直接取消命令
     *
     * 通用指令
     * 1 开火协同
     * 2 导弹节省
     * 3 安全V排
     * 当有指令的时候指令优先
     * 4 低战备血量后退
     * 5 智能逃跑
     * @param amount
     */

    //护航
    public void escortCommand(float amount) {
        //远离炮口

        //判断现在的方向

        //如果 中船 和 大船之间隔着小船则 挤过去 把小船挤出去
    }

    //进攻 绕着最远射程到达屁股
    public void attackCommand(float amount) {
        ShipAPI target = ship.getShipTarget();
        if (ship.getShipTarget()==null) {
            return;
        }
        //如果船比进攻目标快很多则绕屁股
        if(ship.getMaxSpeed()<=target.getMaxSpeed() + 50) {
            return;
        }
        //周围有敌人
        if (MathUtils.getDistance(AIUtils.getNearestEnemy(ship),ship) < 800f) {
            return;
        }
        float shipToTarget = VectorUtils.getAngle(ship.getLocation(),target.getLocation());
        //是否在目标最远射程内
        List<WeaponAPI> weaponList = target.getUsableWeapons();
        float maxWeaponRange = -1f;
        for (WeaponAPI w:weaponList) {
            if (MathUtils.getShortestRotation(w.getArcFacing(),VectorUtils.getAngle(w.getLocation(),ship.getLocation()))<=w.getArc()/2) {
                if (maxWeaponRange<w.getRange()) {
                    maxWeaponRange = w.getRange();
                }
            }
        }
        if (maxWeaponRange==-1f) {
            return;
        }

        //角度小于护盾缺角
        //float targetNoShield = 360f;
        //if (target.getShield()!=null) {
        //    targetNoShield = targetNoShield - target.getShield().getArc();
        //}
        if (MathUtils.getShortestRotation(shipToTarget,target.getFacing())<= 90f) {
            return;
        }

        //没有加速技能
        //面向目标
        float needTurn = Math.abs(MathUtils.getShortestRotation(ship.getFacing() + ship.getAngularVelocity() * amount,shipToTarget));
        turn(needTurn,shipToTarget,amount);
        drift(ship.getFacing(),shipToTarget);
        //ship.getSystem().getSpecAPI().getAIScript().advance(amount,new Vector2f(),new Vector2f(),target);
    }

    //移动
    public void moveCommand(float amount) {
        //目的地是否有敌舰 敌舰是否比自己强 否则有加速加速
    }

    //占领
    public void occupyCommand(float amount) {
        //如果占点有比自己强的 跑
        //一艘船强 跑
        //一堆船强两倍 跑
    }

    //开火协同
    public void firingSynergy() {
        if (ship == null) {
            return;
        }
        boolean hasKinetic = false;
        //动能武器是否CD中
        boolean isCoolDown = false;
        boolean targetFlux = true;
        boolean hasTarget = false;
        //福能是否充裕
        boolean shipFlux = true;
        if (ship.getCurrFlux()>ship.getMaxFlux()*0.8f) {
            shipFlux = false;
        }

        if (ship.getShipTarget()!=null) {
            hasTarget = true;
            if (ship.getShipTarget().getCurrFlux()>ship.getShipTarget().getMaxFlux()*0.8f) {
                targetFlux = false;
            }
        }

        //是否有动能武器
        for (WeaponAPI w:ship.getUsableWeapons()) {
            if (w.getDamageType() == DamageType.KINETIC) {
                hasKinetic = true;
                if (w.getCooldownRemaining()<0.1f) {
                    isCoolDown = true;
                }
            }
        }
        //有动能武器且动能武器冷却完毕
        if (hasKinetic&&!isCoolDown) {
            for (WeaponAPI w:ship.getUsableWeapons()) {
                if (w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD)||w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD_ALSO)||w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD_ONLY)) {
                    continue;
                }else {
                    //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"防空武器");
                }
                if (!hasTarget) {
                   continue;
                }else {
                    //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"没有目标");
                }
                if (targetFlux) {
                    continue;
                }else {
                    //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"火控敌方幅能");
                }
                if (!shipFlux) {
                    continue;
                }else {
                    //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"火控幅能太高");
                }
                if (w.getFluxCostToFire()>0) {
                    if ( ship.getWeaponGroupFor(w).getAutofirePlugin(w).shouldFire()) {
                        Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"智能火控");
                        ship.getWeaponGroupFor(w).getAutofirePlugin(w).forceOff();
                    }
                }
            }
        }
    }

    //思考 什么情况下需要使用导弹 这个导弹是干啥的
    //节省导弹
    public void saveMissile() {
        if (ship == null) {
            return;
        }
        for (WeaponAPI w:ship.getUsableWeapons()) {
            if (w.getType() == WeaponAPI.WeaponType.MISSILE) {

                if (w.usesAmmo()) {
                    //能量导弹 敌方导弹是否很接近玩家且很多
                    //破片导弹 敌方飞机多不多
                    if (w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD)||w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD_ALSO)||w.getSpec().getAIHints().contains(WeaponAPI.AIHints.PD_ONLY)) {
                       int wingCount = 0;
                       int missileCount = 0;
                       for (ShipAPI s:engine.getShips()){
                           if (MathUtils.getDistance(s,ship)<=w.getRange()&&s.getOwner()!=ship.getOwner()&&s.isFighter()) {
                               wingCount++;
                           }
                       }
                       for (MissileAPI m:engine.getMissiles())
                       {
                           if (MathUtils.getDistance(m,ship)<=w.getRange()&&m.getOwner()!=ship.getOwner()) {
                               missileCount++;
                           }
                       }
                       int mult = 0;
                       if (ship.getHullSize() == ShipAPI.HullSize.DESTROYER) {
                           mult = 1;
                       }
                       else if (ship.getHullSize() == ShipAPI.HullSize.CRUISER) {
                           mult = 1;
                       }
                       else if (ship.getHullSize() == ShipAPI.HullSize.CAPITAL_SHIP) {
                            mult = 1;
                       }
                       if (wingCount>=2+mult||missileCount>=2+mult) {
                           continue;
                       }
                       else {
                           //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"防空必须");
                       }
                    }
                    //导弹是否有备弹上限
                    if (!w.usesAmmo()) {
                        continue;
                    }
                    else {
                        //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"没有上限");
                    }
                    //自身幅能较高情况
                    if (ship.getCurrFlux()>ship.getMaxFlux()*0.8f) {
                        continue;
                    }
                    else {
                        //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"导弹我方幅能");
                    }
                    //攻击目标是否接近幅能上限
                    if (ship.getShipTarget()!=null) {
                        if (ship.getShipTarget().getCurrFlux()>ship.getShipTarget().getMaxFlux()*0.8f) {
                            continue;
                        }
                        else {
                            //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"导弹敌方快死");
                        }
                        //导弹是否比目标跑得快
                        if (ship.getShipTarget().getMaxSpeed()<w.getProjectileSpeed()) {
                            continue;
                        }
                        else {
                            //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"导弹导弹太慢");
                        }
                    }
                    if ( ship.getWeaponGroupFor(w).getAutofirePlugin(w).shouldFire()) {
                        Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"导弹智能导弹");
                        ship.getWeaponGroupFor(w).getAutofirePlugin(w).forceOff();
                    }
                }
            }
        }
    }

    //安全V排
    public void safeV() {
        if (ship == null) {
            return;
        }
        //幅能超过30%
        if (ship.getCurrFlux()<ship.getMaxFlux()*0.3f) {
            //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"不需要V排");
            return;
        }
        List<ShipAPI> enemyList = new ArrayList<>();
        for (ShipAPI s:engine.getShips()) {
            if (s.getOwner()!=ship.getOwner()&&s.getOwner()!=100 && s.getHullSize().compareTo(ship.getHullSize())>=0) {
                enemyList.add(s);
            }
        }
        //比自己等级大的船或同等级的船 两船之间是否 存在 己方 比自己等级大的船或同等级的船
        //先获取自己范围内敌方大船 连线 线段是否与范围内 己方大船相交 参考获取遮挡物
        //只要有一个没遮挡
        int count = 0;
        List<ShipAPI> moduleCopy = ship.getChildModulesCopy();
        for (ShipAPI e:enemyList) {
            float shipToEAngle = VectorUtils.getAngle(ship.getLocation(), e.getLocation());
            float eDistance = MathUtils.getDistance(ship,e);
            for (ShipAPI f:engine.getShips()) {
                if (moduleCopy.indexOf(f)<0) {
                    continue;
                }
                float fDistance = MathUtils.getDistance(ship,f);
                if (f.getOwner()==ship.getOwner()&& f.getHullSize().compareTo(ship.getHullSize())>=0&&eDistance>=fDistance) {
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
                Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"V排智能V排");
                ship.giveCommand(ShipCommand.VENT_FLUX, null, 0);
            }
        }
        //Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"无法V排");
    }

    //撤退
    public void retreatCommand() {
        if (ship == null) {
            return;
        }
        //战备40//血量30
        if (ship.getCurrentCR()>0.4f&&ship.getHitpoints()>ship.getMaxHitpoints()*0.3f) {
            return;
        }
        //命令前往地图底部
        if(!ship.isRetreating()) {
            Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"撤退智能撤退");
            ship.setRetreating(true,false);
            /*
            if (assignmentTarget!=null) {
                try {
                    /*
                    CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
                    CombatTaskManagerAPI task = manager.getTaskManager(false);
                    CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(ship);
                    float height = engine.getMapHeight();
                    Vector2f newLocation = new Vector2f(ship.getLocation().getX(), ship.getOwner() == 0 ? -height : height);
                    AssignmentTargetAPI assignmentTarget = manager.createWaypoint(newLocation,false);
                    task.orderRetreat()
                            .giveAssignment(
                            engine.getFleetManager(ship.getOwner()).getDeployedFleetMember(ship),
                            task.createAssignment(CombatAssignmentType.RETREAT, manager.createWaypoint(newLocation, false), false),
                            false);

                }
                catch (Exception e) {
                    Global.getLogger(this.getClass()).info("还是不行"+assignmentTarget.getLocation()+ assignmentTarget.getVelocity()+ assignmentTarget.getOwner());
                }
            }
             */
        }
        if (ship.getCurrentCR()<=0.2f||ship.getHitpoints()<=ship.getMaxHitpoints()*0.1f) {
            Global.getLogger(this.getClass()).info(ship.getHullSpec().getHullName()+"撤退智能撤退");
            ship.setRetreating(true,true);
        }
    }

    //逃跑
    public void escape(float amount) {
        //是否有烈焰去世 等离子 闪现
        ship.getSystem().getSpecAPI().getName();
    }

    public void advance(float amount) {
        try {

        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    public ShipwideAIFlags getAIFlags() {
        return this.AIFlags;
    }

    public void cancelCurrentManeuver() {}

    public ShipAIConfig getConfig() {
        return null;
    }

    private void turn(float needTurn,float shipToTarget, float amount){
        if( needTurn - Math.abs(ship.getAngularVelocity() * amount) > ship.getMaxTurnRate() * amount )
        {
            if (MathUtils.getShortestRotation(MathUtils.clampAngle(ship.getFacing()), shipToTarget) > 0) {
                ship.giveCommand(ShipCommand.TURN_LEFT,null,0);
            } else {
                ship.giveCommand(ShipCommand.TURN_RIGHT,null,0);
            }
        }
        else
        {
            if (ship.getAngularVelocity() > 1)
            {
                ship.giveCommand(ShipCommand.TURN_RIGHT,null,0);
            }
            else if((ship.getAngularVelocity() < -1))
            {
                ship.giveCommand(ShipCommand.TURN_LEFT,null,0);
            }
        }
    }

    private void drift(float shipFacing, float shipToTarget){
        if (MathUtils.getShortestRotation(shipFacing, shipToTarget) > 0) {
            ship.giveCommand(ShipCommand.STRAFE_RIGHT,null,0);

        } else {
            ship.giveCommand(ShipCommand.STRAFE_LEFT,null,0);
        }
    }
}

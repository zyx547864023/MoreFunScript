package real_combat.ai;

import com.fs.starfarer.api.combat.CombatAssignmentType;
import com.fs.starfarer.api.combat.CombatFleetManagerAPI;
import com.fs.starfarer.api.combat.CombatTaskManagerAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.HashSet;
import java.util.Set;

/**
 * 找寻落单目标
 * 大船 非全盾
 * 屁股后面没船
 *
 * 幅能大于XX并且 撤退用技能
 * 幅能小于XX 进攻用技能
 *
 * 没有目标就待在自己大船屁股后面
 *
 */
public class RC_HyperionAI extends RC_BaseShipAI {
    private final static String ID = "RC_HyperionAI";

    public RC_HyperionAI(ShipAPI ship) {
        super(ship);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
    }
    ShipAndDistance newTarget = null;
    @Override
    public void useSystem() {
        //撤退 瞬移到安全敌方
        if (ship.getFluxLevel()>0.8f) {
            //目标位置为自己大船的屁股后面
            //直接用
            if (nearestBiggerAlly!=null) {
                Vector2f targetLocation = MathUtils.getPoint(nearestBiggerAlly.ship.getLocation(),ship.getCollisionRadius()+nearestBiggerAlly.ship.getCollisionRadius(),nearestBiggerAlly.ship.getFacing()+180);
                ship.getMouseTarget().set(targetLocation);
                ship.useSystem();
            }
        }
        //进攻 瞬移到屁股
        else if (ship.getFluxLevel()<0.3f) {
            //目标为新target屁股后面
            float range = ship.getSystem().getSpecAPI().getRange(ship.getMutableStats());
            //如果在范围内才使用
            if (newTarget!=null) {
                Vector2f targetLocation = MathUtils.getPoint(newTarget.ship.getLocation(),ship.getCollisionRadius()+newTarget.ship.getCollisionRadius(),newTarget.ship.getFacing()+180);
                float distance = MathUtils.getDistance(ship,targetLocation);
                if (distance<=range) {
                    ship.getMouseTarget().set(targetLocation);
                    ship.useSystem();
                }
            }
        }
    }

    @Override
    public void findBestTarget() {
        super.findBestTarget();
        newTarget = null;
        //扫描所有船
        Set<ShipAPI> trueEnemyList = new HashSet<>();
        for (ShipAPI e:allEnemyList) {
            //非全盾 非相位
            boolean hasShield = true;
            if (e.getShield()==null&&e.getPhaseCloak()==null) {
                hasShield = false;
            }
            else if (e.getShield()!=null) {
                if (e.getShield().getArc()<360) {
                    hasShield = false;
                }
            }

            boolean hasEscort = false;
            if (!hasShield) {
                //落单 没有护卫
                for (ShipAPI m : allEnemyList) {
                    if (m!=e) {
                        CombatFleetManagerAPI manager = engine.getFleetManager(m.getOwner());
                        CombatTaskManagerAPI task = manager.getTaskManager(false);
                        CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(m);
                        if (mission!=null) {
                            if (mission.getType() == CombatAssignmentType.HEAVY_ESCORT || mission.getType() == CombatAssignmentType.LIGHT_ESCORT || mission.getType() == CombatAssignmentType.MEDIUM_ESCORT) {
                                if (mission.getTarget() != null) {
                                    if (mission.getTarget().getLocation() == e.getLocation()) {
                                        hasEscort = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                //找到target开始找other
                if (!hasEscort) {
                    Set<ShipAPI> others = new HashSet<>();
                    others.addAll(biggerList);
                    others.remove(e);
                    Set<ShipAPI> removes = new HashSet<>();
                    for (ShipAPI o:others) {
                        if(!isOtherTrue(e,o)) {
                            removes.add(o);
                        }
                    }
                    others.removeAll(removes);
                    if (others.size()==0) {
                        trueEnemyList.add(e);
                    }
                }
            }
        }

        //获得了循环最近的
        for (ShipAPI t:trueEnemyList) {
            if (newTarget==null) {
                newTarget = new ShipAndDistance(t,MathUtils.getDistance(t,ship));
            }
            else {
                newTarget = findNearestShip(newTarget,t);
            }
        }

        if (newTarget!=null) {
            if (isLeader&&target!=null) {
                int count = 0;
                if (target.getCustomData().get("findBestTarget") != null) {
                    count = (int) target.getCustomData().get("findBestTarget");
                    count--;
                }
                target.setCustomData("findBestTarget", count);
            }

            target = newTarget.ship;

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
    }
}

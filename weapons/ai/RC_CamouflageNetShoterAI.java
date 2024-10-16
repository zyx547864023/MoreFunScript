package real_combat.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

public class RC_CamouflageNetShoterAI implements AutofireAIPlugin {
    private static final float AIMING_RANGE = 2000f;
    private boolean shouldFire = false;
    private CombatEntityAPI target = null;
    private CombatEngineAPI engine = Global.getCombatEngine();
    private final WeaponAPI weapon;

    public RC_CamouflageNetShoterAI(WeaponAPI weapon) {
        this.weapon = weapon;
    }

    @Override
    public void advance(float amount) {
        ShipAPI ship = weapon.getShip();
        //如果船有命令目标
        CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
        CombatTaskManagerAPI task = manager.getTaskManager(false);
        CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(ship);
        Vector2f targetLocation = null;
        if (mission!=null) {
            if ((mission.getType() == CombatAssignmentType.ASSAULT)) {
                float distance = MathUtils.getDistance(ship,mission.getTarget().getLocation());
                if (distance < AIMING_RANGE) {
                    targetLocation = mission.getTarget().getLocation();
                }
            }
        }
        List<ShipAPI> enemies= AIUtils.getNearbyEnemies(ship,AIMING_RANGE);
        List<ShipAPI> fighterList = new ArrayList<>();
        ShipAPI nearestFighter = null;
        ShipAPI nearestShip = null;
        for (ShipAPI e: enemies)
        {
            if (e.isAlive()) {
                //如果攻击目标不为空 锁定攻击目标 如果不满足条件就不开火
                if (e.getLocation().equals(targetLocation)&&MathUtils.getDistance(e,ship)<AIMING_RANGE) {
                    target = e;
                    shouldFire = true;
                    return;
                }
                //打飞机
                if (e.isFighter()) {
                    if (MathUtils.getDistance(e,ship)<ship.getCollisionRadius()*2) {
                        fighterList.add(e);
                        if (nearestFighter==null) {
                            nearestFighter = e;
                        }
                        else if (MathUtils.getDistance(e,ship)<MathUtils.getDistance(nearestFighter,ship)) {
                            nearestFighter = e;
                        }
                    }
                }
            }
        }
        //打导弹
        List<MissileAPI> missileAPIList = AIUtils.getNearbyEnemyMissiles(ship,ship.getCollisionRadius()*2);
        if (missileAPIList.size()>3) {
            target = AIUtils.getNearestEnemyMissile(ship);
            shouldFire = true;
            return;
        }
        if (target==null&&nearestFighter!=null&&fighterList.size()>3) {
            target = nearestFighter;
            shouldFire = true;
            return;
        }
        shouldFire = false;
    }

    @Override
    public void forceOff() {
        shouldFire = false;
    }

    @Override
    public Vector2f getTarget() {
        if (target == null) {
            return null;
        } else {
            return target.getLocation();
        }
    }

    @Override
    public ShipAPI getTargetShip() {
        if (target != null) {
            if (target instanceof ShipAPI) {
                return (ShipAPI) target;
            }
        }
        return null;
    }

    @Override
    public WeaponAPI getWeapon() {
        return weapon;
    }

    @Override
    public boolean shouldFire() {
        return shouldFire;
    }

    @Override
    public MissileAPI getTargetMissile() {
        return null;
    }
}

package real_combat.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.combat.RC_MonsterBallEveryFrameCombatPlugin;

import java.util.List;

public class RC_MonsterBallShoterAI implements AutofireAIPlugin {
    private static final float AIMING_RANGE = 3000f;
    private static final float MIN_MULT = 2.5f;
    private boolean shouldFire = false;
    private ShipAPI target = null;
    private CombatEngineAPI engine = Global.getCombatEngine();
    private final WeaponAPI weapon;

    public RC_MonsterBallShoterAI(WeaponAPI weapon) {
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
        List<ShipAPI> enemies= AIUtils.getEnemiesOnMap(ship);
        float minMult = MIN_MULT;
        for (ShipAPI e: enemies)
        {
            float mult = RC_MonsterBallEveryFrameCombatPlugin.getMultWithOutMarines(e);
            //如果攻击目标不为空 锁定攻击目标 如果不满足条件就不开火
            if (e.getLocation().equals(targetLocation))
            {
                target = e;
                if (mult>MIN_MULT) {
                    shouldFire = true;
                }
                else {
                    shouldFire = false;
                }
                return;
            }
            //找范围内mult最大的
            else if(mult>minMult)
            {
                minMult = mult;
                target = e;
                shouldFire = true;
            }
        }
        if (minMult == MIN_MULT)
        {
            shouldFire = false;
        }
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
        return target;
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

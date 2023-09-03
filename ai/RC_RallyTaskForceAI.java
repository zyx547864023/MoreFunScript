package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 受到命令移动时候启动
 * 判断技能 闪现displacer相位传送phaseteleporter烈焰驱动器burndrive等离子爆裂驱动器microburn等离子爆裂驱动器 - 欧米伽microburn_omega等离子爆裂驱动器 - 欧米伽maneuveringjets
 * 烈焰喷射inferniuminjector等离子推进器plasmajets
 * 判断移动距离 移动距离较短不适用转向 相位传送不用转向
 * 移动距离长 转向目的地使用技能 或 0幅能加速
 *  护盾控制 保护屁股 对向敌人
 *  武器控制，幅能过高停止武器使用
 *  闪避？
 *  前面有队友直接撞开 有敌人判断量级
 */
public class RC_RallyTaskForceAI extends RC_BaseShipAI {
    private final static String ID = "RC_RallyTaskForceAI";

    public RC_RallyTaskForceAI(ShipAPI ship) {
        super(ship);
    }

    /**
     * @param amount
     */
    public void advance(float amount) {

            if (engine == null) return;
            if (engine.isPaused()) {return;}
            if (!ship.isAlive()) {return;}
            super.advance(amount);
            /*
            //护航任务
            CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
            CombatTaskManagerAPI task = manager.getTaskManager(false);
            CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(ship);
            Vector2f targetLocation = null;
            boolean missionClear = false;
            if (mission!=null) {
                if ((mission.getType() == CombatAssignmentType.RALLY_TASK_FORCE)) {
                    targetLocation = mission.getTarget().getLocation();
                    float distance = MathUtils.getDistance(ship, targetLocation);
                    if (distance < ship.getCollisionRadius()) {
                        //到达目的地
                        missionClear = true;
                    }
                }
                else {
                    missionClear = true;
                }
            }
            else {
                missionClear = true;
            }
            if (missionClear) {
                ship.removeCustomData(ID);
                ship.resetDefaultAI();
            }
            */
    }
}

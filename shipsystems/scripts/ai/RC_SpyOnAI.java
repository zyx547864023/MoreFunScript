package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.entity.RC_Escort;

import java.util.List;

public class RC_SpyOnAI implements ShipSystemAIScript {
    private final static String ID = "RC_spy";
    private CombatEngineAPI engine;
    private ShipAPI ship;
    float timer;
    private ShipwideAIFlags flags;
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine)
    {
        this.ship = ship;
        this.engine = engine;
        timer=0f;
        this.flags=flags;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (!ship.isAlive()) {
            return;
        }
        CombatFleetManagerAPI manager = engine.getFleetManager(ship.getOwner());
        if (manager!=null) {
            CombatTaskManagerAPI task = manager.getTaskManager(false);
            CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(ship);
            if (mission != null) {
                if (CombatAssignmentType.RECON.equals(mission.getType())||CombatAssignmentType.CAPTURE.equals(mission.getType())||CombatAssignmentType.CONTROL.equals(mission.getType()))
                {
                    //如果附近有自己的Spy
                    List<ShipAPI> ships= AIUtils.getNearbyAllies(ship,500f);
                    for (ShipAPI f:ships) {
                        if (ID.equals(f.getHullSpec().getBaseHullId())&&f.getWing()!=null) {
                            if (f.getCustomData().get(ID) == null&&ship.equals(f.getWing().getSourceShip())) {
                                if (!ship.getSystem().isActive()&&!ship.getSystem().isStateActive()&&!ship.getSystem().isCoolingDown()&&!ship.getSystem().isChargeup()&&!ship.getSystem().isChargedown()&&!ship.areAnyEnemiesInRange()) {
                                    ship.useSystem();
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

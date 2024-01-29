package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class RC_CaptureNetAI implements ShipSystemAIScript {
    private final static String WHO_SHOOT = "WHO_SHOOT";
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
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        if (AIUtils.getNearbyEnemies(ship,1500).size()==0) {
            if (ship.getSystem().isActive()) {
                ship.useSystem();
            }
            return;
        }
        if (!ship.getSystem().isActive()&&!ship.getSystem().isStateActive()&&!ship.getSystem().isCoolingDown()&&!ship.getSystem().isChargeup()&&!ship.getSystem().isChargedown()) {
            ship.useSystem();
        }
    }
}

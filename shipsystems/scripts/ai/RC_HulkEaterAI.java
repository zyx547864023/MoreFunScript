package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lwjgl.util.vector.Vector2f;

public class RC_HulkEaterAI implements ShipSystemAIScript {

    private CombatEngineAPI engine;
    private ShipAPI ship;
    float timer;
    private ShipwideAIFlags flags;
    protected IntervalUtil tracker = new IntervalUtil(0.4f, 0.5f);
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
        if (!ship.isAlive()) {
            return;
        }
        if (ship.getSystem().isActive() || ship.getSystem().isStateActive() || ship.getSystem().isCoolingDown() || ship.getSystem().isChargeup() || ship.getSystem().isChargedown()) {
            return;
        }
        //
        boolean isDestroyed = false;
        for (FighterWingAPI w : ship.getAllWings()) {
            if (w.getSpec().getNumFighters() > w.getWingMembers().size()) {
                isDestroyed = true;
                break;
            }
        }
        if (!ship.getSystem().isActive() && !ship.getSystem().isStateActive() && !ship.getSystem().isCoolingDown() && !ship.getSystem().isChargeup() && !ship.getSystem().isChargedown()
                && isDestroyed) {
            ship.useSystem();
        }
    }
}

package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class RC_TrinityForceSystemAI implements ShipSystemAIScript {

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

    }
}

package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lwjgl.util.vector.Vector2f;

public class AutoFixArmorAI implements ShipSystemAIScript {
    private static final float START_FIX_ARMOR = 0.1f;
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
        float nowArmor = 0;
        float maxArmor = 0;
        int count = 0;
        float[][] grid = ship.getArmorGrid().getGrid();
        for (int x = 0; x < grid.length; x++) {
            for (int y = 0; y < grid[x].length; y++) {
                nowArmor += ship.getArmorGrid().getArmorValue(x, y);
                count++;
            }
        }
        maxArmor = count * ship.getArmorGrid().getArmorRating() / 15f;
        //获取装甲最少
        if (maxArmor - nowArmor < START_FIX_ARMOR * maxArmor) {
            return;
        }
        if (!ship.getSystem().isActive()&&!ship.getSystem().isStateActive()&&!ship.getSystem().isCoolingDown()&&!ship.getSystem().isChargeup()&&!ship.getSystem().isChargedown()) {
            ship.useSystem();
        }
    }
}

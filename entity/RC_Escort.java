package real_combat.entity;

import com.fs.starfarer.api.combat.ShipAPI;
import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

/*
*如何玩家舰队组成合理
*当一艘船有下级船只护卫时候，移动速度增加 10%
*/
public class RC_Escort {
    public ShipAPI ship;
    public Vector2f targetLocation;

    public RC_Escort(ShipAPI ship,Vector2f targetLocation) {
        this.ship=ship;
        this.targetLocation=targetLocation;
    }

    public ShipAPI getShip() {
        return ship;
    }

    public void setShip(ShipAPI ship) {
        this.ship = ship;
    }

    public Vector2f getTargetLocation() {
        return targetLocation;
    }

    public void setTargetLocation(Vector2f targetLocation) {
        this.targetLocation = targetLocation;
    }
}

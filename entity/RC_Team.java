package real_combat.entity;

import com.fs.starfarer.api.combat.ShipAPI;

import java.util.ArrayList;
import java.util.List;

/*
*如何玩家舰队组成合理
*当一艘船有下级船只护卫时候，移动速度增加 10%
*/
public class RC_Team {
    ShipAPI flagShip;
    ShipAPI capitalShip;
    List<ShipAPI> cruiserList = new ArrayList<>();
    List<ShipAPI> destroyerList = new ArrayList<>();
    List<ShipAPI> frigateList = new ArrayList<>();
}

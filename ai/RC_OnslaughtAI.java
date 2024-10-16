package real_combat.ai;

import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipCommand;
import com.fs.starfarer.api.combat.WeaponAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 * 护卫舰AI
 * 不对着屁股不进攻
 */
public class RC_OnslaughtAI extends RC_BaseShipAI {
    private final static String ID = "RC_CarrirCombatAI";

    public RC_OnslaughtAI(ShipAPI ship) {
        super(ship);
    }

    @Override
    public void advance(float amount) {
        super.advance(amount);
    }

    @Override
    public void useSystem() {
        if(isWeaponDisabled()) {
            return;
        }
        super.useSystem();
    }

    public void turn(Vector2f targetLocation, float amount){
        if (isWeaponDisabled()) {
            //如果距离比较远就加速
            //距离很近那就减速和飞船同步
            float toTargetAngle = VectorUtils.getAngle(ship.getLocation(), targetLocation);
            if (MathUtils.getShortestRotation(MathUtils.clampAngle(ship.getFacing()), toTargetAngle) < 0) {
                ship.giveCommand(ShipCommand.TURN_LEFT, (Object) null, 0);
            } else {
                ship.giveCommand(ShipCommand.TURN_RIGHT, (Object) null, 0);
            }
        }
        else {
            RC_BaseAIAction.turn(ship, targetLocation, amount);
        }
    }

    public boolean isWeaponDisabled(){
        int count = 0;
        for (WeaponAPI w:ship.getAllWeapons()) {
            if ("tpc".equals(w.getSpec().getWeaponId())&&w.isDisabled()){
                count++;
            }
            if (count==2) {
                return true;
            }
        }
        return false;
    }
}

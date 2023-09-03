package real_combat.ai;

import com.fs.starfarer.api.combat.FighterWingAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

/**
 * 护卫舰AI
 * 不对着屁股不进攻
 */
public class RC_CarrirCombatAI extends RC_BaseShipAI {
    private final static String ID = "RC_CarrirCombatAI";

    public RC_CarrirCombatAI(ShipAPI ship) {
        super(ship);
    }
    @Override
    public void useDriveSystem(Vector2f targetLocation) {
        if (target!=null) {
            if (target.getHullSize().compareTo(ship.getHullSize()) >= 0) {
                int numFighters = 0;
                int maxNumFighters = 0;
                for (FighterWingAPI w : ship.getAllWings()) {
                    maxNumFighters += w.getSpec().getNumFighters();
                    numFighters += w.getWingMembers().size();
                }
                if (maxNumFighters != 0) {
                    if(numFighters / maxNumFighters<0.5f) {
                        return;
                    };
                }
            }
        }
        super.useDriveSystem(targetLocation);
    }
    @Override
    public float getWeight(ShipAPI target) {
        float weight = 1f;
        if (ship.getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP))
        {
            if (target.getCustomData().get("findBestTargetCAPITAL_SHIP")!=null) {
                weight/=ship.getFleetMember().getFleetData().getFleet().getNumShips();
            }
        }
        if (target.getCustomData().get("findBestTarget") != null) {
            int count = (int) target.getCustomData().get("findBestTarget");
            if (count>2) {
                weight/=ship.getHullSpec().getFleetPoints();//count*2;
            }
            else {
                weight*=(count+1);
            }
        }
        //是否过载或者散福能 + 福能
        if (target.getCurrFlux()!=0) {
            weight+=target.getCurrFlux();
            if (target.getFluxTracker().isOverloadedOrVenting()) {
                weight*=2;
            }
        }
        //福能+
        //血量剩余 比例 处以
        if (target.getHitpoints()!=0) {
            weight = weight * target.getMaxHitpoints() / target.getHitpoints();
        }
        else {
            return 0;
        }
        //舰船强度 处以
        weight *= target.getHullSpec().getFleetPoints();

        //舰船屁股插值 处以
        float angle = Math.abs(MathUtils.getShortestRotation(VectorUtils.getAngle(ship.getLocation(),target.getLocation()),target.getFacing()));
        if (angle!=0) {
            weight /= angle;
        }
        else {
            weight*=180;
        }
        //舰船转向 处以
        float distance = MathUtils.getDistance(ship,target);
        if (distance!=0) {
            weight/=distance;
        }

        if (target.getHullSize().equals(ship.getHullSize())) {
            weight *= 2;
        }

        if (target.getHullSize().compareTo(ship.getHullSize())>0&&ship.getFluxLevel()!=0) {
            weight *= ship.getFluxLevel();
        }
        if (target.getHullSize().compareTo(ship.getHullSize())>=0) {
            int numFighters = 0;
            int maxNumFighters = 0;
            for (FighterWingAPI w : ship.getAllWings()) {
                maxNumFighters += w.getSpec().getNumFighters();
                numFighters += w.getWingMembers().size();
            }
            if (maxNumFighters!=0) {
                weight *= numFighters / maxNumFighters;
            }
        }
        return weight;
    }
}

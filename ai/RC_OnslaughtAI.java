package real_combat.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.ui.P;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

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
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        if (!ship.isAlive()) {
            if (ship.getHullSize().equals(ShipAPI.HullSize.CAPITAL_SHIP))
            {
                target.removeCustomData("findBestTargetCAPITAL_SHIP");
            }
            if (target!=null) {
                int count = 0;
                if (target.getCustomData().get("findBestTarget") != null) {
                    count = (int) target.getCustomData().get("findBestTarget");
                    count--;
                }
                target.setCustomData("findBestTarget",count);
            }
            return;
        }

            /*
            for (BattleObjectiveAPI o:engine.getObjectives()) {
                Global.getLogger(this.getClass()).info(o.getOwner());
            }
            */
        if (ship.getCustomData().get("setWeaponGroup")==null) {
            setWeaponGroup();
            ship.getCustomData().put("setWeaponGroup","setWeaponGroup");
        }
        if (ship.getCustomData().get("setWeaponGroup")!=null) {
            weaponController();
        }
        vent();
        findBestTarget();
        if (target == null) {
            Global.getLogger(this.getClass()).info("怎么可能");
        }
        else if (target.isHulk()||!target.isAlive()) {
            Global.getLogger(this.getClass()).info("怎么可能");
        }
        boolean isDodge = false;
        //要分开写阻尼 TODO
        if (ship.getPhaseCloak()!=null) {
            usePhase(amount);
        }
        else if (ship.getShield()!=null) {
            useShield(amount);
        }
        //如果闪避了就不移动了 TODO
        else {
            if (!isDodge) {
                float minDistance = ship.getMaxSpeed();
                DamagingProjectileAPI targetProjectile = null;
                List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
                for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
                    float range = MathUtils.getDistance(ship, damagingProjectile);
                    if (range <= minDistance + damagingProjectile.getVelocity().length() && damagingProjectile.getOwner() != ship.getOwner() && !damagingProjectile.isFading() && !damagingProjectile.isExpired()) {
                        minDistance = range;
                        targetProjectile = damagingProjectile;
                    }
                }
                if (targetProjectile != null) {
                    if (target != null) {
                        if (dodge(targetProjectile, target, amount)) {
                            isDodge = true;
                        }
                    }
                }
            }
        }
        //
        if (!isDodge) {
            if (nearestBiggerShip!=null) {
                if ((nearestBiggerShip.ship.getHullSize().compareTo(ship.getHullSize())>=0&&(nearestBiggerShip.ship.getHitpoints()/nearestBiggerShip.ship.getHitpoints()<0.3F&&nearestBiggerShip.minDistance<(ship.getCollisionRadius()*2+target.getCollisionRadius())))||ship.getFluxTracker().isOverloaded()) {
                    move(ship.getFacing(),VectorUtils.getAngle(nearestBiggerShip.ship.getLocation(),ship.getLocation()));
                    isDodge = true;
                }
                else if(nearestEnemy!=null){
                    if (nearestEnemy.minDistance>maxWeaponRange&&nearestBiggerShip.minDistance<minWeaponRange) {
                        move(ship.getFacing(),VectorUtils.getAngle(nearestBiggerShip.ship.getLocation(),ship.getLocation()));
                        isDodge = true;
                    }
                }
            }
            if (target!=null&&!isDodge) {
                if (isWeaponDisabled()) {
                    turnNew(target.getLocation(), amount);
                }
                else {
                    turn(target.getLocation(), amount);
                }
                if (other==null) {
                    flyToTarget(target, null, amount);
                }
                else if(other.ship.isFighter()){
                    flyToTarget(target, null, amount);
                }
                else {
                    flyToTarget(target, other.ship, amount);
                }
            }
            else {
                Global.getLogger(this.getClass()).info("怎么可能");
            }
        }
        else {
            Global.getLogger(this.getClass()).info("怎么可能");
        }

        pullBackFighters();


        if (ship.getSystem()!=null&&!ship.getFluxTracker().isOverloaded()) {
            boolean hasAIScript = false;
            if (ship.getSystem().getSpecAPI()!=null) {
                if (ship.getSystem().getSpecAPI().getAIScript()!=null) {
                    hasAIScript = true;
                }
            }
            if (hasAIScript&&target!=null) {
                ship.getSystem().getSpecAPI().getAIScript().advance(amount, ship.getLocation(), ship.getLocation(), target);
            }
            else if(target!=null){
                useSystem(target.getLocation());
            }
        }


    }

    public void turnNew(Vector2f targetLocation, float amount){
        //如果距离比较远就加速
        //距离很近那就减速和飞船同步
        float toTargetAngle = VectorUtils.getAngle(ship.getLocation(),targetLocation);
        if (MathUtils.getShortestRotation(MathUtils.clampAngle(ship.getFacing()), toTargetAngle) < 0) {
            ship.giveCommand(ShipCommand.TURN_LEFT, (Object)null ,0);
        } else {
            ship.giveCommand(ShipCommand.TURN_RIGHT, (Object)null ,0);
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

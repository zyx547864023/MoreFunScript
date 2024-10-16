package real_combat.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.util.RC_Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class RC_CamouflageNetAI extends RC_BaseMissile {
    private static final float TARGET_ACQUISITION_RANGE = 2000f;
    private static final float SPEED_DIVIDE_2 = 2f;
    private CombatEngineAPI engine = Global.getCombatEngine();
    public RC_CamouflageNetAI(MissileAPI missile, ShipAPI launchingShip) {
        super(missile, launchingShip);
    }


    private static List<ShipAPI> getSortedDirectShipTargets(ShipAPI launchingShip) {
        List<ShipAPI> directTargets = RC_Util.getShipsWithinRange(launchingShip.getMouseTarget(), 300f);
        if (!directTargets.isEmpty()) {
            Collections.sort(directTargets, new CollectionUtils.SortEntitiesByDistance(launchingShip.getMouseTarget()));
        }
        return directTargets;
    }

    @Override
    public void advance(float amount) {
        float maxSpeed = missile.getMaxSpeed();

        assignMissileToShipTarget(launchingShip);

        //如果没有找到目标
        if (!acquireTarget(amount)) {
            if (missile.getVelocity().length() >= (maxSpeed / SPEED_DIVIDE_2)) {
                missile.giveCommand(ShipCommand.DECELERATE);
            } else {
                missile.giveCommand(ShipCommand.ACCELERATE);
            }
            return;
        }

        if (target!=null) {
            float missileToShipAngle = VectorUtils.getAngle(missile.getLocation(),target.getLocation());
            float missileFacing = MathUtils.clampAngle(missile.getFacing());
            float mi = Math.abs(MathUtils.getShortestRotation(missileFacing + missile.getAngularVelocity() * amount,missileToShipAngle));
            missileCommandNoSheild(mi, missileToShipAngle, amount);
            missile.giveCommand(ShipCommand.ACCELERATE);
        }
    }

    @Override
    protected void assignMissileToShipTarget(ShipAPI launchingShip) {
        engine = Global.getCombatEngine();

        //如果船有命令目标
        CombatFleetManagerAPI manager = engine.getFleetManager(launchingShip.getOwner());
        CombatTaskManagerAPI task = manager.getTaskManager(false);
        CombatFleetManagerAPI.AssignmentInfo mission = task.getAssignmentFor(launchingShip);
        Vector2f targetLocation = null;
        if (mission!=null) {
            if ((mission.getType() == CombatAssignmentType.ASSAULT)) {
                float distance = MathUtils.getDistance(launchingShip,mission.getTarget().getLocation());
                if (distance < TARGET_ACQUISITION_RANGE) {
                    targetLocation = mission.getTarget().getLocation();
                }
            }
        }
        List<ShipAPI> enemies= AIUtils.getNearbyEnemies(launchingShip,TARGET_ACQUISITION_RANGE);
        List<ShipAPI> fighterList = new ArrayList<>();
        ShipAPI nearestFighter = null;
        ShipAPI nearestShip = null;
        for (ShipAPI e: enemies)
        {
            if (e.isAlive()) {
                //如果攻击目标不为空 锁定攻击目标 如果不满足条件就不开火
                if (e.getLocation().equals(targetLocation)&&MathUtils.getDistance(e,launchingShip)<TARGET_ACQUISITION_RANGE) {
                    setTarget(e);
                    return;
                }
                //打飞机
                if (e.isFighter()) {
                    if (MathUtils.getDistance(e,launchingShip)<launchingShip.getCollisionRadius()*2) {
                        fighterList.add(e);
                        if (nearestFighter==null) {
                            nearestFighter = e;
                        }
                        else if (MathUtils.getDistance(e,launchingShip)<MathUtils.getDistance(nearestFighter,launchingShip)) {
                            nearestFighter = e;
                        }
                    }
                }
                else if (e.getHullSize().compareTo(ShipAPI.HullSize.CRUISER)>=0) {
                    if (nearestShip==null) {
                        nearestShip = e;
                    }
                    else if (MathUtils.getDistance(e,launchingShip)<MathUtils.getDistance(nearestShip,launchingShip)) {
                        nearestShip = e;
                    }
                }
            }
        }
        //打导弹
        List<MissileAPI> missileAPIList = AIUtils.getNearbyEnemyMissiles(launchingShip,launchingShip.getCollisionRadius()*2);
        if (missileAPIList.size()>3) {
            setTarget(AIUtils.getNearestEnemyMissile(launchingShip));
            return;
        }
        if (target==null&&nearestFighter!=null&&fighterList.size()>3) {
            setTarget(nearestFighter);
            return;
        }
        if (target==null&&nearestShip!=null) {
            if (TARGET_ACQUISITION_RANGE>MathUtils.getDistance(launchingShip,nearestShip)) {
                setTarget(nearestShip);
            }
        }
    }

    @Override
    protected CombatEntityAPI findBestTarget() {
        CombatEntityAPI closest = null;
        float distance, closestDistance = TARGET_ACQUISITION_RANGE;

        List<ShipAPI> ships = AIUtils.getNearbyEnemies(missile,missile.getMaxRange());
        int shipSize = ships.size();
        for (int i = 0; i < shipSize; i++) {
            ShipAPI tmp = ships.get(i);
            if (!isTargetValid(tmp)) {
                continue;
            }
            distance = MathUtils.getDistance(tmp, missile);
            if (distance < closestDistance) {
                closest = tmp;
                closestDistance = distance;
            }
        }
        return closest;
    }

    @Override
    protected CombatEntityAPI getMouseTarget(ShipAPI launchingShip) {
        ListIterator<ShipAPI> iter = getSortedDirectShipTargets(launchingShip).listIterator();
        while (iter.hasNext()) {
            ShipAPI tmp = iter.next();
            if (isTargetValid(tmp)) {
                return tmp;
            }
        }
        return null;
    }

    @Override
    protected boolean isTargetValid(CombatEntityAPI target) {
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            return !((ship.getOwner() == missile.getOwner())||!ship.isAlive());
        }
        return false;
    }

    private void missileCommandNoSheild(float mi,float missileToShipAngle, float amount){
        if( mi - Math.abs(missile.getAngularVelocity() * amount) > missile.getMaxTurnRate() * amount )
        {
            if (MathUtils.getShortestRotation(MathUtils.clampAngle(missile.getFacing()), missileToShipAngle) > 0) {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            } else {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            }
        }
        else
        {
            if (missile.getAngularVelocity() > 1)
            {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            }
            else if((missile.getAngularVelocity() < -1))
            {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            }
        }
    }
}

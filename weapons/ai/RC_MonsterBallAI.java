package real_combat.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import com.fs.starfarer.combat.CombatEngine;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import real_combat.util.RC_Util;

public class RC_MonsterBallAI extends RC_BaseMissile {
    private static final float TARGET_ACQUISITION_RANGE = 1800f;
    private final String ID="monster_ball_shooter_sec";
    public RC_MonsterBallAI(MissileAPI missile, ShipAPI launchingShip) {
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
        if (missile.isFizzling() || missile.isFading()) {
            CombatEngineAPI engine = Global.getCombatEngine();
            //登陆舱
            CombatEntityAPI landing = Global.getCombatEngine().spawnProjectile(
                    missile.getSource(),
                    null,
                    ID,
                    missile.getLocation(),
                    missile.getFacing(),
                    missile.getVelocity()
            );
            engine.removeObject(missile);
            return;
        }

        if (!acquireTarget(amount)) {
            if (missile.getVelocity().length() >= (maxSpeed / 2f)) {
                missile.giveCommand(ShipCommand.DECELERATE);
            } else {
                missile.giveCommand(ShipCommand.ACCELERATE);
            }
            return;
        }
        //如果导弹和目标之间有护盾就转向
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if(ship.getOwner()==missile.getOwner()) return;
            float shipToMissileAngle = VectorUtils.getAngle(ship.getLocation(),missile.getLocation());
            float MissileToshipAngle = VectorUtils.getAngle(missile.getLocation(),ship.getLocation());
            //当与目标距离小于护盾半径的时候时直线加速接近
            float distance = Math.abs(MathUtils.getDistance(ship,missile));
            float radius = ship.getCollisionRadius();
            if(ship.getShield()!=null)
            {
                radius = ship.getShield().getRadius();
            }
            float mi = Math.abs(MathUtils.getShortestRotation(missile.getFacing(),MissileToshipAngle));
            Global.getLogger(this.getClass()).info(mi);
            if(ship.getShield()!=null)
            {
                if(ship.getShield().isOn()) {
                    if(distance>radius*1.7)
                    {
                        //Global.getLogger(this.getClass()).info("ZJJJ");
                        missileCommandNoSheild(mi,MissileToshipAngle,20,90);
                    }
                    float shieldAngle = ship.getShield().getFacing();
                    float shieldAcc = ship.getShield().getActiveArc();
                    //如果面前有盾没有盾
                    if(Math.abs(MathUtils.getShortestRotation(shipToMissileAngle,shieldAngle))>(shieldAcc/2)+1)
                    {
                        //Global.getLogger(this.getClass()).info("QMMD");

                        if(mi>20)
                        if (MathUtils.getShortestRotation(missile.getFacing(), MissileToshipAngle) > 0) {
                            missile.giveCommand(ShipCommand.TURN_LEFT);
                            if (missile.getVelocity().length() >= (maxSpeed / 2f)) {
                                missile.giveCommand(ShipCommand.DECELERATE);
                            }
                        } else {
                            missile.giveCommand(ShipCommand.TURN_RIGHT);
                            if (missile.getVelocity().length() >= (maxSpeed / 2f)) {
                                missile.giveCommand(ShipCommand.DECELERATE);
                            }
                        }
                        if(mi<30)
                            missile.giveCommand(ShipCommand.ACCELERATE);
                        //missileCommandNoSheild(mi,MissileToshipAngle,20,30);
                    }
                    else {
                        //Global.getLogger(this.getClass()).info("QMYD");
                        //顺势转开
                        if(MathUtils.getShortestRotation(missile.getFacing(),MissileToshipAngle) > 0)
                        {
                            //转超过90度了要转回来
                            missileCommandExistsSheild(mi,MissileToshipAngle,maxSpeed,ShipCommand.TURN_RIGHT,ShipCommand.TURN_LEFT);
                        }
                        else {
                            missileCommandExistsSheild(mi,MissileToshipAngle,maxSpeed,ShipCommand.TURN_LEFT,ShipCommand.TURN_RIGHT);
                        }

                    }
                }
                else {
                    missileCommandNoSheild(mi,MissileToshipAngle,20,90);
                }
            }
            else {
                missileCommandNoSheild(mi,MissileToshipAngle,20,90);
            }
        }




        float distance = MathUtils.getDistance(target.getLocation(), missile.getLocation())
                - target.getCollisionRadius();

    }

    @Override
    protected void assignMissileToShipTarget(ShipAPI launchingShip) {
        /* Do nothing */
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
            distance = MathUtils.getDistance(tmp, missile.getLocation());
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
            return !((ship.getOwner() == missile.getOwner())||ship.isFighter());
        }
        return false;
    }

    private void missileCommandNoSheild(float mi,float missileToshipAngle,int turn,int notTurn){
        if(mi>turn)
            if (MathUtils.getShortestRotation(missile.getFacing(), missileToshipAngle) > 0) {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            } else {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            }
        if(mi<notTurn)
            missile.giveCommand(ShipCommand.ACCELERATE);
    }

    private void missileCommandExistsSheild(float mi,float missileToshipAngle,float maxSpeed,ShipCommand shipCommandS,ShipCommand shipCommandB){
        //转超过90度了要转回来
        if(mi<30) {
            missile.giveCommand(shipCommandS);
            if (missile.getVelocity().length() >= (maxSpeed / 4f)) {
                missile.giveCommand(ShipCommand.DECELERATE);
            }
        }
        else if (mi>=30&&mi<70){
            missile.giveCommand(ShipCommand.ACCELERATE);
        }
        else {
            missile.giveCommand(shipCommandB);
            if (missile.getVelocity().length() >= (maxSpeed / 4f)) {
                missile.giveCommand(ShipCommand.DECELERATE);
            }
        }
    }
}

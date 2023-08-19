package real_combat.weapons.ai.bak;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.VectorUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.util.RC_Util;
import real_combat.weapons.ai.RC_BaseMissile;

import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class RC_MonsterBallAIBack extends RC_BaseMissile {
    private static final float TARGET_ACQUISITION_RANGE = 1800f;
    private static final float RADIUS_DIVIDE_2 = 2f;
    private static final float SPEED_DIVIDE_2 = 2f;
    private static final float SPEED_DIVIDE_4 = 4f;
    private static final float RADIUS_MULTIPLY = 1.7f;
    private static final float SPURT_ANGLE = 90f;
    private static final float TURN_ANGLE = 20f;

    private static final float NO_TURN_ANGLE_MAX = 70f;
    private static final float NO_TURN_ANGLE_MIN = 30f;

    private ShipCommand shipCommand = null;
    private boolean isReady = false;
    private final String ID="monster_ball_shooter_sec";
    public RC_MonsterBallAIBack(MissileAPI missile, ShipAPI launchingShip) {
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
            if (missile.getVelocity().length() >= (maxSpeed / SPEED_DIVIDE_2)) {
                missile.giveCommand(ShipCommand.DECELERATE);
            } else {
                missile.giveCommand(ShipCommand.ACCELERATE);
            }
            return;
        }
        //如果导弹和目标之间有护盾就转向
        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if(ship.getOwner()==missile.getOwner()) {return;}
            float shipToMissileAngle = VectorUtils.getAngle(ship.getLocation(),missile.getLocation());
            float missileToshipAngle = VectorUtils.getAngle(missile.getLocation(),ship.getLocation());
            float missileFacing = MathUtils.clampAngle(missile.getFacing());
            float missileSpeedAngle = VectorUtils.getAngle(new Vector2f(0,0),missile.getVelocity());
            //当与目标距离小于护盾半径的时候时直线加速接近
            float distance = MathUtils.getDistance(ship,missile);
            float radius = ship.getCollisionRadius();
            if(ship.getShield()!=null)
            {
                radius = ship.getShield().getRadius();
            }
            float mi = Math.abs(MathUtils.getShortestRotation(missileFacing,missileToshipAngle));
            //Global.getLogger(this.getClass()).info(mi);
            if(ship.getShield()!=null)
            {
                if(ship.getShield().isOn()) {
                    if(distance>radius)
                    {
                        //Global.getLogger(this.getClass()).info("ZJJJ");
                        missileCommandNoSheild(mi, missileToshipAngle, 10, 90, amount);
                        isReady = false;
                        return;
                    }
                    float shieldAngle = ship.getShield().getFacing();
                    float shieldAcc = ship.getShield().getActiveArc();

                    //不管前面有没有盾
                    if(mi > missile.getAngularVelocity()*amount) {
                        if (MathUtils.getShortestRotation(missileFacing, missileToshipAngle) > 0) {
                            missile.giveCommand(ShipCommand.TURN_LEFT);
                            if (missile.getVelocity().length() >= (maxSpeed / SPEED_DIVIDE_4)) {
                                if(!isReady) {
                                    missile.giveCommand(ShipCommand.DECELERATE);
                                }
                            }
                        } else {
                            missile.giveCommand(ShipCommand.TURN_RIGHT);
                            if (missile.getVelocity().length() >= (maxSpeed / SPEED_DIVIDE_4)) {
                                if(!isReady) {
                                    missile.giveCommand(ShipCommand.DECELERATE);
                                }
                            }
                        }
                    }

                    //如果面前有盾没有盾
                    if(Math.abs(MathUtils.getShortestRotation(shipToMissileAngle,shieldAngle))>(shieldAcc/RADIUS_DIVIDE_2)+1)
                    {
                        shipCommand = null;
                        //Global.getLogger(this.getClass()).info("QMMD");
                        float sp = Math.abs(MathUtils.getShortestRotation(missileSpeedAngle,missileToshipAngle));
                        float speedDistance = MathUtils.getDistance(ship.getVelocity(),missile.getVelocity());
                        if(isReady)
                        {
                            missile.giveCommand(ShipCommand.ACCELERATE);
                            return;
                        }
                        if(mi <= 10 && speedDistance <= 200) {
                            //先减速再加速
                            missile.giveCommand(ShipCommand.ACCELERATE);
                            isReady = true;
                        }
                    }
                    else {
                        isReady = false;
                        if(distance<radius/2)
                        {
                            if (missile.getVelocity().length() >= (maxSpeed / SPEED_DIVIDE_2)) {
                                missile.giveCommand(ShipCommand.DECELERATE);
                            }
                        }
                        //Global.getLogger(this.getClass()).info("QMYD");
                        //missile.giveCommand(ShipCommand.STRAFE_LEFT);
                        //顺势转开
                        //float speedDistance = MathUtils.getDistance(ship.getVelocity(),missile.getVelocity());
                        if(shipCommand == null) {
                            if (MathUtils.getShortestRotation(missileFacing, missileToshipAngle) > 0) {
                                missile.giveCommand(ShipCommand.STRAFE_RIGHT);
                                shipCommand = ShipCommand.STRAFE_RIGHT;
                                //目标角度纠正为目标角度-90 mi 要重新计算
                                //missileToshipAngle = MathUtils.clampAngle(missileToshipAngle-(60-(speedDistance)*amount));
                                //mi = Math.abs(MathUtils.getShortestRotation(missileFacing,missileToshipAngle));
                                //missileCommandNoSheild(mi, missileToshipAngle, 5, 90, amount);
                                //转超过90度了要转回来
                                //missileCommandExistsSheild(mi, maxSpeed, ShipCommand.TURN_RIGHT, ShipCommand.TURN_LEFT, amount);
                            } else {
                                missile.giveCommand(ShipCommand.STRAFE_LEFT);
                                shipCommand = ShipCommand.STRAFE_LEFT;
                                //missile.giveCommand(ShipCommand.ACCELERATE);
                                //missileToshipAngle = MathUtils.clampAngle(missileToshipAngle+(60-(speedDistance)*amount));
                                //mi = Math.abs(MathUtils.getShortestRotation(missileFacing,missileToshipAngle));
                                //missileCommandNoSheild(mi, missileToshipAngle, 5, 90, amount);
                                //missileCommandExistsSheild(mi, maxSpeed, ShipCommand.TURN_LEFT, ShipCommand.TURN_RIGHT, amount);
                            }
                        }
                        else {
                            missile.giveCommand(shipCommand);
                        }
                    }
                }
                else {
                    missileCommandNoSheild(mi, missileToshipAngle, 10, 90, amount);
                }
            }
            else {
                missileCommandNoSheild(mi, missileToshipAngle, 10, 90, amount);
            }
        }
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

    private void missileCommandNoSheild(float mi,float missileToshipAngle,int turn,int notTurn,float amount){
        if(mi>turn && mi>missile.getAngularVelocity() * amount) {
            if (MathUtils.getShortestRotation(missile.getFacing(), missileToshipAngle) > 0) {
                missile.giveCommand(ShipCommand.TURN_LEFT);
            } else {
                missile.giveCommand(ShipCommand.TURN_RIGHT);
            }
        }
        if(mi<notTurn){
            missile.giveCommand(ShipCommand.ACCELERATE);
        }
    }

    private void missileCommandExistsSheild(float mi,float maxSpeed,ShipCommand shipCommandS,ShipCommand shipCommandB, float amount){
        //转超过90度了要转回来
        if(mi<NO_TURN_ANGLE_MIN) {
            missile.giveCommand(shipCommandS);
            if (missile.getVelocity().length() >= (maxSpeed / SPEED_DIVIDE_2)) {
                //missile.giveCommand(ShipCommand.DECELERATE);
            }
        }
        else if (mi>=NO_TURN_ANGLE_MIN&&mi<NO_TURN_ANGLE_MAX){
            //missile.giveCommand(ShipCommand.ACCELERATE);
        }
        else {
            missile.giveCommand(shipCommandB);
            if (missile.getVelocity().length() >= (maxSpeed / SPEED_DIVIDE_2)) {
                //missile.giveCommand(ShipCommand.DECELERATE);
            }
        }
        missile.giveCommand(ShipCommand.ACCELERATE);
    }
}

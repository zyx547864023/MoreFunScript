package real_combat.weapons.ai;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.CollectionUtils;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.ai.RC_BaseShipAI;
import real_combat.util.RC_Util;

import java.util.*;

public class RC_BaseMissile implements MissileAIPlugin, GuidedMissileAI {

    private static Vector2f quad(float a, float b, float c) {
        Vector2f solution = null;
        if (Float.compare(Math.abs(a), 0) == 0) {
            if (Float.compare(Math.abs(b), 0) == 0) {
                solution = (Float.compare(Math.abs(c), 0) == 0) ? new Vector2f(0, 0) : null;
            } else {
                solution = new Vector2f(-c / b, -c / b);
            }
        } else {
            float d = b * b - 4 * a * c;
            if (d >= 0) {
                d = (float) Math.sqrt(d);
                float e = 2 * a;
                solution = new Vector2f((-b - d) / e, (-b + d) / e);
            }
        }
        return solution;
    }

    static List<ShipAPI> getSortedDirectTargets(ShipAPI launchingShip) {
        List<ShipAPI> directTargets = RC_Util.getShipsWithinRange(launchingShip.getMouseTarget(), 300f);
        if (!directTargets.isEmpty()) {
            Collections.sort(directTargets, new CollectionUtils.SortEntitiesByDistance(launchingShip.getMouseTarget()));
        }
        return directTargets;
    }

    static Vector2f intercept(Vector2f point, float speed, Vector2f target, Vector2f targetVel) {
        final Vector2f difference = new Vector2f(target.x - point.x, target.y - point.y);

        final float a = targetVel.x * targetVel.x + targetVel.y * targetVel.y - speed * speed;
        final float b = 2 * (targetVel.x * difference.x + targetVel.y * difference.y);
        final float c = difference.x * difference.x + difference.y * difference.y;

        final Vector2f solutionSet = quad(a, b, c);

        Vector2f intercept = null;
        if (solutionSet != null) {
            float bestFit = Math.min(solutionSet.x, solutionSet.y);
            if (bestFit < 0) {
                bestFit = Math.max(solutionSet.x, solutionSet.y);
            }
            if (bestFit > 0) {
                intercept = new Vector2f(target.x + targetVel.x * bestFit, target.y + targetVel.y * bestFit);
            }
        }

        return intercept;
    }

    static Vector2f interceptAdvanced(Vector2f point, float speed, float acceleration, float maxspeed, Vector2f target,
            Vector2f targetVel) {
        Vector2f difference = new Vector2f(target.x - point.x, target.y - point.y);

        float s = speed;
        float a = acceleration / 2f;
        float b = speed;
        float c = difference.length();
        Vector2f solutionSet = quad(a, b, c);
        if (solutionSet != null) {
            float t = Math.min(solutionSet.x, solutionSet.y);
            if (t < 0) {
                t = Math.max(solutionSet.x, solutionSet.y);
            }
            if (t > 0) {
                s = acceleration * t;
                s = s / 2f + speed;
                s = Math.min(s, maxspeed);
            }
        }

        a = targetVel.x * targetVel.x + targetVel.y * targetVel.y - s * s;
        b = 2 * (targetVel.x * difference.x + targetVel.y * difference.y);
        c = difference.x * difference.x + difference.y * difference.y;

        solutionSet = quad(a, b, c);

        Vector2f intercept = null;
        if (solutionSet != null) {
            float bestFit = Math.min(solutionSet.x, solutionSet.y);
            if (bestFit < 0) {
                bestFit = Math.max(solutionSet.x, solutionSet.y);
            }
            if (bestFit > 0) {
                intercept = new Vector2f(target.x + targetVel.x * bestFit, target.y + targetVel.y * bestFit);
            }
        }

        return intercept;
    }

    protected ShipAPI launchingShip;
    protected MissileAPI missile;
    protected CombatEntityAPI target;

    public RC_BaseMissile(MissileAPI missile, ShipAPI launchingShip) {
        this.missile = missile;
        this.launchingShip = launchingShip;

        defaultInitialTargetingBehavior(launchingShip);
    }

    @Override
    public void advance(float amount) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public CombatEntityAPI getTarget() {
        return target;
    }

    @Override
    public final void setTarget(CombatEntityAPI target) {
        this.target = target;
    }

    private void defaultInitialTargetingBehavior(ShipAPI launchingShip) {
        assignMissileToShipTarget(launchingShip);

        if (target == null) {
            setTarget(getMouseTarget(launchingShip));
        }

        if (target == null) {
            setTarget(findBestTarget());
        }
    }

    protected boolean acquireTarget(float amount) {
        if (!isTargetValid(target)) {
            if (target instanceof ShipAPI) {
                ShipAPI ship = (ShipAPI) target;
                if (ship.isPhased() && ship.isAlive()) {
                    return false;
                }
            }
            setTarget(findBestTarget());
            if (target == null) {
                return false;
            }
        }
        return true;
    }

    protected void assignMissileToShipTarget(ShipAPI launchingShip) {
        if (isTargetValid(launchingShip.getShipTarget())) {
            setTarget(launchingShip.getShipTarget());
        }
    }

    protected CombatEntityAPI findBestTarget() {
        ShipAPI closest = null;
        float range = getRemainingRange() + missile.getMaxSpeed() * 2f;
        float closestDistance = getRemainingRange() + missile.getMaxSpeed() * 2f;
        //List<ShipAPI> ships = AIUtils.getEnemiesOnMap(missile);
        Set<ShipAPI> ships = new HashSet<>();
        ships = RC_BaseShipAI.getEnemiesOnMap(missile,ships);

        for (ShipAPI tmp: ships){
            float mod = 0f;
            if (tmp.isFighter() || tmp.isDrone()) {
                mod = range / 2f;
            }
            if (!isTargetValid(tmp)) {
                mod = range;
            }
            float distance = MathUtils.getDistance(tmp, missile.getLocation()) + mod;
            if (distance < closestDistance) {
                closest = tmp;
                closestDistance = distance;
            }
        }
        return closest;
    }

    protected CombatEntityAPI getMouseTarget(ShipAPI launchingShip) {
        ListIterator<ShipAPI> iter = getSortedDirectTargets(launchingShip).listIterator();
        while (iter.hasNext()) {
            ShipAPI tmp = iter.next();
            if (isTargetValid(tmp)) {
                return tmp;
            }
        }
        return null;
    }

    protected float getRange() {
        float maxTime = missile.getMaxFlightTime();
        float speed = missile.getMaxSpeed();
        return speed * maxTime;
    }

    protected float getRemainingRange() {
        float time = missile.getMaxFlightTime() - missile.getFlightTime();
        float speed = missile.getMaxSpeed();
        return speed * time;
    }

    protected boolean isTargetValid(CombatEntityAPI target) {
        if (target == null || (missile.getOwner() == target.getOwner()) || !Global.getCombatEngine().isEntityInPlay(
                target)) {
            return false;
        }

        if (target instanceof ShipAPI) {
            ShipAPI ship = (ShipAPI) target;
            if (ship.isPhased() || !ship.isAlive()) {
                return false;
            }
        } else {
            if (target.getCollisionClass() == CollisionClass.NONE) {
                return false;
            }
        }

        return true;
    }
}

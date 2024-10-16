package real_combat.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.entities.SimpleEntity;
import org.lwjgl.util.vector.Vector2f;

public class RC_FlyingThunderGod extends BaseShipSystemScript {
    public static final String ID="RC_FlyingThunderGod";
    private final static float THICKNESS = 10f;
    private final IntervalUtil Interval = new IntervalUtil(0.2f, 0.25f);
    private final java.awt.Color JITTER_COLOR = new java.awt.Color(255, 222, 90, 215);
    private final java.awt.Color JITTER_UNDER_COLOR = new java.awt.Color(252, 199, 0, 255);
    private static final String MISSILE_ID="heatseeker";
    private ShipAPI ship;
    public MissileAPI missile;
    CombatEngineAPI engine = Global.getCombatEngine();
    @Override
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        try {
            missile = (MissileAPI) ship.getCustomData().get(ID);
            if (missile == null) {
                /*
                if (effectLevel>=1) {
                    //发射一枚火蛇
                    missile = (MissileAPI) Global.getCombatEngine().spawnProjectile(
                            ship,
                            null,
                            MISSILE_ID,
                            ship.getLocation(),
                            ship.getFacing(),
                            ship.getVelocity()
                    );
                    ship.setCustomData(ID,missile);
                }
                */
            } else {
                /*
                if (missile.isFizzling() || missile.isFading() || missile.didDamage() || missile.isExpired()) {
                    missile = null;
                    ship.getCustomData().remove(ID);
                    return;
                }
                */
                float jitterLevel = effectLevel;
                if (state == State.OUT) {
                    //ship.setAlphaMult(1-effectLevel);
                    jitterLevel *= jitterLevel;
                    Vector2f newMissileLocation = new Vector2f(ship.getLocation());
                    //Vector2f newMissileVelocity = new Vector2f(ship.getVelocity());
                    float newMissileFacing = ship.getFacing();
                    ship.getLocation().set(new Vector2f(missile.getLocation()));
                    //ship.getVelocity().set(new Vector2f(missile.getVelocity()));
                    ship.setFacing(missile.getFacing());
                    ship.getSystem().setCooldownRemaining(1f);
                    missile.getLocation().set(newMissileLocation);
                    //missile.getVelocity().set(newMissileVelocity);
                    missile.setFacing(newMissileFacing);
                    if (ship.getShield()!=null) {
                        if (ship.getShield().isOn()) {
                            ship.getShield().toggleOff();
                        }
                    }
                    //engine.removeEntity(missile);
                    //missile = null;
                }

                float maxRangeBonus = 25f;
                float jitterRangeBonus = jitterLevel * maxRangeBonus;
                ship.setJitterUnder(this, JITTER_COLOR, jitterLevel, 11, 0f, 3f + jitterRangeBonus);
                ship.setJitter(this, JITTER_UNDER_COLOR, jitterLevel, 4, 0f, 0 + jitterRangeBonus);

                //给一个电弧
                Vector2f newShipRandomLocation = MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()*2);
                Vector2f mRandomLocation = MathUtils.getRandomPointInCircle(ship.getLocation(),ship.getCollisionRadius()*2);
                Interval.advance(engine.getElapsedInLastFrame());
                if (Interval.intervalElapsed()) {
                    engine.spawnEmpArcPierceShields(ship, newShipRandomLocation, new SimpleEntity(ship.getLocation()), new SimpleEntity(mRandomLocation), DamageType.ENERGY, 0f, 0f, 1000000f, null, MathUtils.getRandomNumberInRange(0.5f,1f)*THICKNESS, JITTER_UNDER_COLOR, JITTER_COLOR);
                }
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    @Override
    public void unapply(MutableShipStatsAPI stats, String id) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        ship = (ShipAPI) stats.getEntity();
        if (ship == null) {
            return;
        }
        try {
            if (ship.getAlphaMult()<1) {
                ship.setAlphaMult(ship.getAlphaMult()+engine.getElapsedInLastFrame());
            }
            else if(ship.getAlphaMult()>1){
                ship.setAlphaMult(1);
            }
            /*
            if (missile!=null) {
                if (missile.isFizzling() || missile.isFading() || missile.didDamage() || missile.isExpired()) {
                    missile = null;
                    ship.getCustomData().remove(ID);
                }
            }
             */
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
    }

    @Override
    public boolean isUsable(ShipSystemAPI system, ShipAPI ship) {
        if (engine == null) {return false;}
        if(engine.isPaused()) {return false;}
        if (ship == null) {
            return false;
        }
        try {
            missile = (MissileAPI) ship.getCustomData().get(ID);
            if (missile!=null) {
                return true;
            }
        }
        catch (Exception e)
        {
            Global.getLogger(this.getClass()).info(e);
        }
        return false;
    }

    @Override
    public String getInfoText(ShipSystemAPI system, ShipAPI ship) {
        missile = (MissileAPI) ship.getCustomData().get(ID);
        if (missile==null) {
            return "缺少苦无";
        }
        return null;
    }
}

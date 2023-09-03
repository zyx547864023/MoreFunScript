package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.Map;

public class RC_AsteroidArmAI implements ShipSystemAIScript {
    private final static String WHO_SHOOT = "WHO_SHOOT";
    private CombatEngineAPI engine;
    private ShipAPI ship;
    float timer;
    private ShipwideAIFlags flags;
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine)
    {
        this.ship = ship;
        this.engine = engine;
        timer=0f;
        this.flags=flags;
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine == null) return;
        if (engine.isPaused()) {return;}
        //如果武器或引擎坏了
        /*
        boolean isWeaponOrEngineDisabled = false;
        List<ShipEngineControllerAPI.ShipEngineAPI> shipEngines = ship.getEngineController().getShipEngines();
        for (int e = 0; e < shipEngines.size(); e++) {
            ShipEngineControllerAPI.ShipEngineAPI engine = shipEngines.get(e);
            if (engine.isDisabled()) {
                isWeaponOrEngineDisabled = true;
            }
        }
        List<WeaponAPI> shipWeapons = ship.getAllWeapons();
        for (int w = 0; w < shipWeapons.size(); w++) {
            WeaponAPI weapon = shipWeapons.get(w);
            if (weapon.isDisabled()) {
                isWeaponOrEngineDisabled = true;
            }
        }
        //如果周围很多子弹
        int count = 0;
        boolean isProjectileMany = false;
        List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
        for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
            float range = MathUtils.getDistance(ship, damagingProjectile);
            if(range<=ship.getCollisionRadius()*2&&damagingProjectile.getOwner()!=ship.getOwner()) {
                count++;
                if(count>5)
                {
                    isProjectileMany = true;
                }
            }
        }
        //如果周围没有敌人
        count = 0;
        boolean isNoEnemy = true;
        boolean isFlux = false;
        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI s : ships) {
            float range = MathUtils.getDistance(ship, s);
            if (range<=ship.getCollisionRadius()*6&&s.getOwner()!=ship.getOwner()&&s.isAlive()&!s.isFighter()) {
                isNoEnemy = false;
                count++;
                break;
            }
        }
        //如果幅能比较
        if (ship.getCurrFlux() > ship.getMaxFlux() / 2) {
            isFlux = true;
        }
        */
        if(ship.getSystem().isActive()&&ship.getCurrFlux() > ship.getMaxFlux() * 0.8f)
        {
            if (ship.getShield()!=null) {
                if (ship.getShield().isOn()) {
                    ship.giveCommand(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK,null,0);
                }
            }
            return;
        }
        //在范围内有陨石
        /*
        int count = 0;
        for (CombatEntityAPI a : engine.getAsteroids()) {
            float distance = MathUtils.getDistance(ship, a);
            Map<String, Object> customData = a.getCustomData();
            if (customData != null) {
                if (customData.get(WHO_SHOOT) == null&&distance < ship.getCollisionRadius() * 10f) {
                    count++;
                }
            }
        }
         */
        //
        /*
        if (count<=5&&ship.areAnyEnemiesInRange()) {
            ship.giveCommand(ShipCommand.VENT_FLUX,null,0);
            //ship.getSystem().deactivate();
            return;
        }
         */
        if (!ship.getSystem().isActive()&&!ship.getSystem().isStateActive()&&!ship.getSystem().isCoolingDown()&&!ship.getSystem().isChargeup()&&!ship.getSystem().isChargedown()) {
            ship.useSystem();
        }
    }
}

package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

public class RC_AsteroidArmAI implements ShipSystemAIScript {

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
        //如果武器或引擎坏了
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
                break;
            }
        }
        //如果幅能比较
        if (ship.getCurrFlux() > ship.getMaxFlux() / 2) {
            isFlux = true;
        }
        //
        if (!ship.getSystem().isActive()&&!ship.getSystem().isStateActive()&&!ship.getSystem().isCoolingDown()&&!ship.getSystem().isChargeup()&&!ship.getSystem().isChargedown()) {
            ship.useSystem();
            /*
            if (isWeaponOrEngineDisabled||isProjectileMany||isFlux||!isNoEnemy) {
                ship.useSystem();
            }
            else {
                //ship.giveCommand(ShipCommand.VENT_FLUX,null,0);
            }
             */
        }
        /*
        if(ship.getSystem().isActive()&&ship.getCurrFlux() < ship.getMaxFlux() / 4&&isNoEnemy)
        {
            ship.giveCommand(ShipCommand.VENT_FLUX,null,0);
        }
         */
    }
}

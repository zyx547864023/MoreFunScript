package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.constant.RC_ComboConstant;

import java.util.List;

public class RC_ComboSystemAi implements ShipSystemAIScript {

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
        float maxWeaponRange = 0;
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
            if(weapon.getRange()>maxWeaponRange)
            {
                maxWeaponRange = weapon.getRange();
            }
        }
        //如果周围很多子弹
        int count = 0;
        boolean isProjectileMany = false;
        List<DamagingProjectileAPI> damagingProjectiles = engine.getProjectiles();
        for (DamagingProjectileAPI damagingProjectile : damagingProjectiles) {
            float range = MathUtils.getDistance(ship, damagingProjectile);
            if(range<=maxWeaponRange&&damagingProjectile.getOwner()!=ship.getOwner()) {
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
        ShipAPI skillTarget = null;
        boolean isEnemy = false;
        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI s : ships) {
            float range = MathUtils.getDistance(ship, s);
            if (range<=maxWeaponRange&&s.getOwner()!=ship.getOwner()&&s.isAlive()&&!s.isPhased()&!s.isFighter()) {
                isNoEnemy = false;
                if (range<=400f) {
                    //如果幅能比较大且旁边有大船
                    if (ship.getCurrFlux() > ship.getMaxFlux() / 2) {
                        if (!target.getFluxTracker().isOverloadedOrVenting())
                        {
                            isFlux = true;
                            if(skillTarget == null)
                            {
                                skillTarget = s;
                            }
                            else if(s.getHullSize().compareTo(skillTarget.getHullSize())>0)
                            {
                                skillTarget = s;
                            }
                            ship.setShipTarget(s);
                        }
                    }
                    //如果幅能比较小且旁边有船
                    if (ship.getCurrFlux() <= ship.getMaxFlux() / 2) {
                        isEnemy = true;
                        if(skillTarget == null)
                        {
                            skillTarget = s;
                        }
                        else if(s.getHullSize().compareTo(skillTarget.getHullSize())>0)
                        {
                            skillTarget = s;
                        }
                        ship.setShipTarget(s);
                    }
                }
            }
        }

        //
        if (!ship.getSystem().isActive()&&!ship.getSystem().isStateActive()&&!ship.getSystem().isCoolingDown()&&!ship.getSystem().isChargeup()&&!ship.getSystem().isChargedown()) {
            if (isWeaponOrEngineDisabled) {
                ship.setCustomData("Combo", RC_ComboConstant.SKILL_DAMPER_OMEGA);
            } else if (isProjectileMany) {
                ship.setCustomData("Combo", RC_ComboConstant.SKILL_TEMPORALSHELL1);
            } else if (isNoEnemy) {
                ship.setCustomData("Combo", RC_ComboConstant.SKILL_MICROBURN);
            } else if (isFlux) {
                ship.setCustomData("Combo", RC_ComboConstant.SKILL_ACAUSALDISRUPTOR1);
            } else if (isEnemy) {
                ship.setCustomData("Combo", RC_ComboConstant.SKILL_ENTROPYAMPLIFIER1);
            }
            ship.setShipSystemDisabled(false);
            ship.useSystem();
        }
    }
}

package real_combat.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.*;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;
import real_combat.constant.RC_ComboConstant;

import java.util.List;

public class RC_ComboSystemAI implements ShipSystemAIScript {

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
        if (!ship.isAlive()) {
            return;
        }
        if (ship.getSystem().isActive() || ship.getSystem().isStateActive() || ship.getSystem().isCoolingDown() || ship.getSystem().isChargeup() || ship.getSystem().isChargedown()) {
            return;
        }
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
        boolean isNoEnemy = true;
        ShipAPI skillTarget = null;
        List<ShipAPI> ships = engine.getShips();
        for (ShipAPI s : ships) {
            float range = MathUtils.getDistance(ship, s);
            if (range<=maxWeaponRange&&s.getOwner()!=ship.getOwner()&&s.isAlive()&&!s.isFighter()) {
                isNoEnemy = false;
                //在技能范围内
                if (range<=500f) {
                    if(skillTarget == null)
                    {
                        skillTarget = s;
                    }
                    else if(s.getHullSize().compareTo(skillTarget.getHullSize())>0)
                    {
                        skillTarget = s;
                    }
                }
            }
        }

        boolean useAcausaldisruptor = false;
        boolean useEntropyamplifier = false;
        boolean useTemporalshell = false;
        if (!ship.getSystem().isActive()&&!ship.getSystem().isStateActive()&&!ship.getSystem().isCoolingDown()&&!ship.getSystem().isChargeup()&&!ship.getSystem().isChargedown()) {
            if(skillTarget!=null) {
                ship.setShipTarget(skillTarget);
                //如果目标护盾或者相位用量子
                if (skillTarget.isPhased()) {
                    useAcausaldisruptor = true;
                } else if(skillTarget.getShield()!=null){
                    if(skillTarget.getShield().isOn())
                    {
                        useAcausaldisruptor = true;
                    }
                    else {
                        //如果对方没有过载且自己幅能过高
                        if (!skillTarget.getFluxTracker().isOverloadedOrVenting() && ship.getCurrFlux() > ship.getMaxFlux() / 2) {
                            useAcausaldisruptor = true;
                        }
                        //幅能太大用时间之壳
                        else if (ship.getCurrFlux() > ship.getMaxFlux() / 2) {
                            useTemporalshell = true;
                        }
                        //
                        else {
                            useEntropyamplifier = true;
                        }
                    }
                }else {
                    //如果对方没有过载且自己幅能过高
                    if (!skillTarget.getFluxTracker().isOverloadedOrVenting() && ship.getCurrFlux() > ship.getMaxFlux() / 2) {
                        useAcausaldisruptor = true;
                    }
                    //幅能太大用时间之壳
                    else if (ship.getCurrFlux() > ship.getMaxFlux() / 2) {
                        useTemporalshell = true;
                    }
                    //
                    else {
                        useEntropyamplifier = true;
                    }
                }
            }
            if (isWeaponOrEngineDisabled) {
                ship.setCustomData("Combo", RC_ComboConstant.SKILL_DAMPER_OMEGA);
            } else if (isProjectileMany||useTemporalshell) {
                ship.setCustomData("Combo", RC_ComboConstant.SKILL_TEMPORALSHELL1);
            } else if (isNoEnemy) {
                ship.setCustomData("Combo", RC_ComboConstant.SKILL_MICROBURN);
            } else if (useAcausaldisruptor) {
                ship.setCustomData("Combo", RC_ComboConstant.SKILL_ACAUSALDISRUPTOR1);
            } else if (useEntropyamplifier) {
                ship.setCustomData("Combo", RC_ComboConstant.SKILL_ENTROPYAMPLIFIER1);
            }
            ship.setShipSystemDisabled(false);
            ship.useSystem();
        }
    }
}

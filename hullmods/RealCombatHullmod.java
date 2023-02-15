package real_combat.hullmods;
import java.util.HashMap;
import java.util.Map;

import org.lwjgl.util.vector.Vector2f;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize;
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import com.fs.starfarer.api.combat.listeners.WeaponRangeModifier;

public class RealCombatHullmod extends BaseHullMod {

    public static final Map<WeaponSize, Float> ballisticRangeUp = new HashMap<>(3);
    public static final Map<WeaponSize, Float> ballisticDamageDown = new HashMap<>(3);
    public static final int engineRangeUp = 100;
    public static final int missileRangeUp = 200;
    
    static {
    	ballisticRangeUp.put(WeaponSize.LARGE, 150f);
    	ballisticRangeUp.put(WeaponSize.MEDIUM, 100f);
    	ballisticRangeUp.put(WeaponSize.SMALL, 0f);
    	
    	ballisticDamageDown.put(WeaponSize.LARGE, -50f);
    	ballisticDamageDown.put(WeaponSize.MEDIUM, -50f);
    	ballisticDamageDown.put(WeaponSize.SMALL, 0f);
    }

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getEnergyWeaponRangeBonus().modifyPercent(id, engineRangeUp);
        stats.getMissileWeaponRangeBonus().modifyPercent(id, missileRangeUp);
    }
    /*
    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.SMALL_PD_MOD).modifyFlat(id, OP_INCREASE_SMALL);
        stats.getDynamic().getMod(Stats.MEDIUM_PD_MOD).modifyFlat(id, OP_INCREASE_MEDIUM);
        stats.getDynamic().getMod(Stats.LARGE_PD_MOD).modifyFlat(id, OP_INCREASE_LARGE);
    }
	*/
    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new DamageDealtMod());
        ship.addListener(new WeaponRangeMod());
    }
    
    
    private static class DamageDealtMod implements DamageDealtModifier {

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage, Vector2f point, boolean shieldHit) {
            WeaponAPI weapon = null;
            if (param instanceof DamagingProjectileAPI) {
                weapon = ((DamagingProjectileAPI) param).getWeapon();
            } else if (param instanceof BeamAPI) {
                weapon = ((BeamAPI) param).getWeapon();
            } else if (param instanceof MissileAPI) {
                weapon = ((MissileAPI) param).getWeapon();
            }
            String id = "real_combat_dam_mod";
            if (weapon == null) {
                if (damage.getStats() != null) {
                    damage.getStats().getHitStrengthBonus().unmodify(id);
                }
                return null;
            }
            if (weapon.getType()!=WeaponType.ENERGY&&weapon.getType()!=WeaponType.BALLISTIC&&weapon.getType()!=WeaponType.BUILT_IN) {
                if (damage.getStats() != null) {
                    damage.getStats().getHitStrengthBonus().unmodify(id);
                }
                return null;
            }
            if (weapon.getType()==WeaponType.ENERGY)
            {
	            //Vector2f weaponlocation = weapon.getLocation();
	            //double distance = Math.sqrt((weaponlocation.x - point.x)*(weaponlocation.x - point.x)+(weaponlocation.y - point.y)*(weaponlocation.y - point.y));
	            //如果距离大于射程威力减小，距离小于射程威力增加，最大为2
            	if (param instanceof BeamAPI) {
		            double percent = ((BeamAPI) param).getLength()/weapon.getRange();
		            damage.getModifier().modifyPercent(id, (float) ((1-percent)*100));
            	}
            	if (param instanceof DamagingProjectileAPI) {
            		Vector2f weaponlocation = ((DamagingProjectileAPI) param).getSpawnLocation();
    	            double distance = Math.sqrt((weaponlocation.x - point.x)*(weaponlocation.x - point.x)+(weaponlocation.y - point.y)*(weaponlocation.y - point.y));
    	            double percent = distance/weapon.getRange();
    	            damage.getModifier().modifyPercent(id, (float) ((1-percent)*100));
                } 
            }
            if (weapon.getType()==WeaponType.BALLISTIC)//||weapon.getType()==WeaponType.BUILT_IN
            {
            	damage.getModifier().modifyPercent(id, ballisticDamageDown.get(weapon.getSize()));
            }
            return id;
        }
    } 
    
    private static class WeaponRangeMod implements WeaponRangeModifier {

        @Override
        public float getWeaponRangePercentMod(ShipAPI ship, WeaponAPI weapon) {
            if (!(weapon.getType() == WeaponType.BALLISTIC)) {
                return 0f;
            }
            return (ballisticRangeUp.get(weapon.getSize())) / 100f;
        }

        @Override
        public float getWeaponRangeMultMod(ShipAPI ship, WeaponAPI weapon) {
            return 1f;
        }

        @Override
        public float getWeaponRangeFlatMod(ShipAPI ship, WeaponAPI weapon) {
            return 0f;
        }
    }
    
    //根据武器规格，增加能量武器{%s}的射程，根据命中距离衰减伤害，增加实弹武器{%s/%s/%s}的射程，同时减少实弹武器{%s/%s/%s}的伤害，增加导弹{%s}的射程。
    @Override
    public String getDescriptionParam(int index, HullSize hullSize) {
        if (index == 0) {
            return "" + engineRangeUp+"%";
        }
        if (index == 1) {
            return "" + ballisticRangeUp.get(WeaponSize.SMALL).intValue()+"%";
        }
        if (index == 2) {
            return "" + ballisticRangeUp.get(WeaponSize.MEDIUM).intValue()+"%";
        }
        if (index == 3) {
            return "" + ballisticRangeUp.get(WeaponSize.LARGE).intValue()+"%";
        }
        if (index == 4) {
            return "" + ballisticDamageDown.get(WeaponSize.SMALL).intValue()+"%";
        }
        if (index == 5) {
            return "" + ballisticDamageDown.get(WeaponSize.MEDIUM).intValue()+"%";
        }
        if (index == 6) {
            return "" + ballisticDamageDown.get(WeaponSize.LARGE).intValue()+"%";
        }
        if (index == 7) {
            return "" + missileRangeUp+"%";
        }
        return null;
    }
}
